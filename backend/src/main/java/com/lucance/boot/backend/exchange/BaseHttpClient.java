package com.lucance.boot.backend.exchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import com.lucance.boot.backend.service.LatencyStatsService;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 基础 HTTP 客户端
 * 使用 OkHttp3 实现，提供统一的请求处理
 * 参考: 01-phase1-api-integration.md
 */
public abstract class BaseHttpClient {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final OkHttpClient httpClient;
    protected final String baseUrl;
    protected final String apiKey;
    protected final String secretKey;
    protected final RateLimiter rateLimiter;
    protected final ObjectMapper objectMapper;
    protected LatencyStatsService latencyStatsService; // Optional

    public BaseHttpClient(String baseUrl, String apiKey, String secretKey) {
        this(baseUrl, apiKey, secretKey, null, 10.0);
    }

    public BaseHttpClient(String baseUrl, String apiKey, String secretKey, ProxyConfig proxyConfig) {
        this(baseUrl, apiKey, secretKey, proxyConfig, 10.0);
    }

    public BaseHttpClient(String baseUrl, String apiKey, String secretKey,
            ProxyConfig proxyConfig, double requestsPerSecond) {
        this(baseUrl, apiKey, secretKey, proxyConfig, requestsPerSecond, 10, 5);
    }

    public BaseHttpClient(String baseUrl, String apiKey, String secretKey,
            ProxyConfig proxyConfig, double requestsPerSecond,
            int maxIdleConnections, int keepAliveDurationMinutes) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.rateLimiter = RateLimiter.create(requestsPerSecond);
        this.objectMapper = new ObjectMapper();

        // 配置 OkHttp 客户端
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(maxIdleConnections, keepAliveDurationMinutes, TimeUnit.MINUTES))
                .addInterceptor(new LoggingInterceptor())
                .addInterceptor(new LatencyTrackingInterceptor())
                .addInterceptor(new RetryInterceptor(3));

        // 配置代理
        if (proxyConfig != null && proxyConfig.isEnabled()) {
            configureProxy(builder, proxyConfig);
        }

        this.httpClient = builder.build();
    }

    /**
     * 设置延迟统计服务（可选）
     */
    public void setLatencyStatsService(LatencyStatsService latencyStatsService) {
        this.latencyStatsService = latencyStatsService;
    }

    /**
     * 配置代理
     */
    private void configureProxy(OkHttpClient.Builder builder, ProxyConfig config) {
        try {
            Proxy proxy = new Proxy(
                    config.getType(),
                    new InetSocketAddress(config.getHost(), config.getPort()));
            builder.proxy(proxy);

            // 如果需要代理认证
            if (config.getUsername() != null && config.getPassword() != null) {
                builder.proxyAuthenticator((route, response) -> {
                    if (response.request().header("Proxy-Authorization") != null) {
                        return null; // 已经尝试过认证
                    }

                    String credential = Credentials.basic(
                            config.getUsername(),
                            config.getPassword());

                    return response.request().newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build();
                });
            }

            log.info("Proxy configured: {}://{}:{}",
                    config.getType(), config.getHost(), config.getPort());

        } catch (Exception e) {
            log.error("Failed to configure proxy", e);
            throw new ExchangeApiException("Failed to configure proxy", e);
        }
    }

    /**
     * GET 请求
     */
    protected <T> T get(String endpoint, Map<String, String> params, Class<T> responseType) {
        return get(endpoint, params, responseType, false);
    }

    /**
     * GET 请求（支持签名）
     */
    protected <T> T get(String endpoint, Map<String, String> params, Class<T> responseType, boolean signed) {
        rateLimiter.acquire();

        try {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + endpoint).newBuilder();
            if (params != null) {
                params.forEach(urlBuilder::addQueryParameter);
            }

            Request.Builder requestBuilder = new Request.Builder()
                    .url(urlBuilder.build())
                    .get()
                    .addHeader("Content-Type", "application/json");

            // 需要签名的请求添加 API Key
            if (signed && apiKey != null) {
                requestBuilder.addHeader("X-MBX-APIKEY", apiKey);
            }

            try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
                return handleResponse(response, responseType);
            }

        } catch (IOException e) {
            throw new ExchangeApiException("GET request failed: " + endpoint, e);
        }
    }

    /**
     * GET 请求（返回泛型类型）
     */
    protected <T> T get(String endpoint, Map<String, String> params, TypeReference<T> typeRef) {
        return get(endpoint, params, typeRef, false);
    }

    /**
     * GET 请求（返回泛型类型，支持签名）
     */
    protected <T> T get(String endpoint, Map<String, String> params, TypeReference<T> typeRef, boolean signed) {
        rateLimiter.acquire();

        try {
            HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + endpoint).newBuilder();
            if (params != null) {
                params.forEach(urlBuilder::addQueryParameter);
            }

            Request.Builder requestBuilder = new Request.Builder()
                    .url(urlBuilder.build())
                    .get()
                    .addHeader("Content-Type", "application/json");

            if (signed && apiKey != null) {
                requestBuilder.addHeader("X-MBX-APIKEY", apiKey);
            }

            try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
                return handleResponse(response, typeRef);
            }

        } catch (IOException e) {
            throw new ExchangeApiException("GET request failed: " + endpoint, e);
        }
    }

    /**
     * POST 请求（需要签名）
     */
    protected <T> T post(String endpoint, Map<String, String> params, Class<T> responseType) {
        rateLimiter.acquire();

        try {
            // 添加时间戳
            params.put("timestamp", String.valueOf(System.currentTimeMillis()));

            // 生成签名
            String queryString = buildQueryString(params);
            String signature = sign(queryString);
            params.put("signature", signature);

            // 构建请求体
            FormBody.Builder formBuilder = new FormBody.Builder();
            params.forEach(formBuilder::add);

            // 构建请求
            Request request = new Request.Builder()
                    .url(baseUrl + endpoint)
                    .post(formBuilder.build())
                    .addHeader("X-MBX-APIKEY", apiKey)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return handleResponse(response, responseType);
            }

        } catch (IOException e) {
            throw new ExchangeApiException("POST request failed: " + endpoint, e);
        }
    }

    /**
     * POST 请求（需要签名，返回泛型类型）
     */
    protected <T> T post(String endpoint, Map<String, String> params, TypeReference<T> typeRef) {
        rateLimiter.acquire();

        try {
            // 添加时间戳
            params.put("timestamp", String.valueOf(System.currentTimeMillis()));

            // 生成签名
            String queryString = buildQueryString(params);
            String signature = sign(queryString);
            params.put("signature", signature);

            // 构建请求体
            FormBody.Builder formBuilder = new FormBody.Builder();
            params.forEach(formBuilder::add);

            // 构建请求
            Request request = new Request.Builder()
                    .url(baseUrl + endpoint)
                    .post(formBuilder.build())
                    .addHeader("X-MBX-APIKEY", apiKey)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return handleResponse(response, typeRef);
            }

        } catch (IOException e) {
            throw new ExchangeApiException("POST request failed: " + endpoint, e);
        }
    }

    /**
     * DELETE 请求（需要签名）
     */
    protected <T> T delete(String endpoint, Map<String, String> params, TypeReference<T> typeRef) {
        rateLimiter.acquire();

        try {
            // 添加时间戳
            params.put("timestamp", String.valueOf(System.currentTimeMillis()));

            // 生成签名
            String queryString = buildQueryString(params);
            String signature = sign(queryString);
            params.put("signature", signature);

            // 构建 URL 参数
            HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + endpoint).newBuilder();
            params.forEach(urlBuilder::addQueryParameter);

            // 构建请求
            Request request = new Request.Builder()
                    .url(urlBuilder.build())
                    .delete()
                    .addHeader("X-MBX-APIKEY", apiKey)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return handleResponse(response, typeRef);
            }

        } catch (IOException e) {
            throw new ExchangeApiException("DELETE request failed: " + endpoint, e);
        }
    }

    /**
     * 处理响应
     */
    private <T> T handleResponse(Response response, Class<T> responseType) throws IOException {
        if (!response.isSuccessful()) {
            String errorBody = response.body() != null ? response.body().string() : "Unknown error";
            throw new ExchangeApiException(
                    String.format("Request failed with code %d: %s", response.code(), errorBody));
        }

        String body = response.body().string();
        return parseJson(body, responseType);
    }

    /**
     * 处理响应（泛型）
     */
    private <T> T handleResponse(Response response, TypeReference<T> typeRef) throws IOException {
        if (!response.isSuccessful()) {
            String errorBody = response.body() != null ? response.body().string() : "Unknown error";
            throw new ExchangeApiException(
                    String.format("Request failed with code %d: %s", response.code(), errorBody));
        }

        String body = response.body().string();
        return objectMapper.readValue(body, typeRef);
    }

    /**
     * 签名方法（子类实现）
     */
    protected abstract String sign(String payload);

    /**
     * 构建查询字符串
     */
    protected String buildQueryString(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    /**
     * JSON 解析
     */
    private <T> T parseJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new ExchangeApiException("Failed to parse JSON response", e);
        }
    }

    /**
     * 关闭客户端
     */
    public void close() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    /**
     * 日志拦截器
     */
    static class LoggingInterceptor implements Interceptor {
        private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();

            long startTime = System.currentTimeMillis();
            log.debug("Request: {} {}", request.method(), request.url());

            Response response = chain.proceed(request);

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Response: {} {} ({}ms)", response.code(), request.url(), duration);

            return response;
        }
    }

    /**
     * 延迟跟踪拦截器
     */
    class LatencyTrackingInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            long startTime = System.currentTimeMillis();
            boolean success = false;

            try {
                Response response = chain.proceed(request);
                success = response.isSuccessful();
                return response;
            } catch (IOException e) {
                success = false;
                throw e;
            } finally {
                long latency = System.currentTimeMillis() - startTime;

                // 记录延迟统计（如果服务已注入）
                if (latencyStatsService != null) {
                    String exchangeName = getExchangeName();
                    String endpoint = request.url().encodedPath();
                    latencyStatsService.recordLatency(exchangeName, endpoint, latency, success);
                }
            }
        }
    }

    /**
     * 获取交易所名称（子类实现）
     */
    protected String getExchangeName() {
        return this.getClass().getSimpleName().replace("Adapter", "");
    }

    /**
     * 重试拦截器
     */
    static class RetryInterceptor implements Interceptor {
        private static final Logger log = LoggerFactory.getLogger(RetryInterceptor.class);
        private final int maxRetries;

        public RetryInterceptor(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = null;
            IOException lastException = null;

            for (int i = 0; i <= maxRetries; i++) {
                try {
                    response = chain.proceed(request);

                    // 如果是服务器错误（5xx）或限流（429），重试
                    if (response.isSuccessful() ||
                            (response.code() != 429 && response.code() < 500)) {
                        return response;
                    }

                    response.close();

                    if (i < maxRetries) {
                        long delay = (long) Math.pow(2, i) * 1000;
                        log.warn("Retrying request after {}ms (attempt {}/{})", delay, i + 1, maxRetries);
                        Thread.sleep(delay);
                    }

                } catch (IOException e) {
                    lastException = e;
                    if (i < maxRetries) {
                        try {
                            long delay = (long) Math.pow(2, i) * 1000;
                            log.warn("Request failed, retrying after {}ms: {}", delay, e.getMessage());
                            Thread.sleep(delay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw e;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Retry interrupted", e);
                }
            }

            if (lastException != null) {
                throw lastException;
            }

            return response;
        }
    }
}
