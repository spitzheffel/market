# Phase 1 补充：交易所 API 对接技术方案

## 技术选型分析

### 方案对比

| 维度 | 官方 SDK | OkHttp3 自实现 | 推荐方案 |
|------|---------|---------------|---------|
| **开发速度** | ⭐⭐⭐⭐⭐ 快速 | ⭐⭐⭐ 中等 | **混合方案** |
| **可控性** | ⭐⭐ 黑盒 | ⭐⭐⭐⭐⭐ 完全可控 | ⭐⭐⭐⭐ 高 |
| **维护成本** | ⭐⭐⭐ 依赖官方 | ⭐⭐ 需自己维护 | ⭐⭐⭐⭐ 可控 |
| **扩展性** | ⭐⭐ 受限 | ⭐⭐⭐⭐⭐ 灵活 | ⭐⭐⭐⭐⭐ 优秀 |
| **统一性** | ⭐ 各SDK不同 | ⭐⭐⭐⭐⭐ 统一接口 | ⭐⭐⭐⭐⭐ 统一 |

### 最终推荐：混合方案

**核心原则**：
1. **REST API**：使用 OkHttp3 自己实现（完全可控）
2. **WebSocket**：初期可参考 SDK，后期自己实现
3. **统一接口**：定义 `ExchangeAdapter` 抽象层
4. **灵活切换**：支持通过配置切换实现方式

---

## 架构设计

### 1. 统一接口层

```java
/**
 * 交易所适配器统一接口
 */
public interface ExchangeAdapter {

    /**
     * 获取交易所名称
     */
    String getExchangeName();

    /**
     * 健康检查
     */
    HealthStatus healthCheck();

    // ==================== 市场数据 ====================

    /**
     * 获取K线数据
     * @param symbol 交易对（如 "BTC/USDT"）
     * @param interval 时间周期（如 "1m", "5m", "1h"）
     * @param startTime 开始时间（毫秒时间戳）
     * @param endTime 结束时间（毫秒时间戳）
     * @param limit 数量限制（默认500，最大1000）
     */
    List<Kline> getKlines(String symbol, String interval, Long startTime, Long endTime, Integer limit);

    /**
     * 获取最新价格
     */
    Ticker getTicker(String symbol);

    /**
     * 获取订单簿深度
     */
    OrderBook getOrderBook(String symbol, Integer depth);

    /**
     * 获取最近成交
     */
    List<Trade> getRecentTrades(String symbol, Integer limit);

    // ==================== WebSocket 订阅 ====================

    /**
     * 订阅K线数据流
     */
    void subscribeKline(String symbol, String interval, Consumer<Kline> callback);

    /**
     * 订阅Ticker数据流
     */
    void subscribeTicker(String symbol, Consumer<Ticker> callback);

    /**
     * 取消订阅
     */
    void unsubscribe(String subscriptionId);

    /**
     * 关闭所有连接
     */
    void close();

    // ==================== 交易接口（需要API Key）====================

    /**
     * 创建订单
     */
    Order createOrder(OrderRequest request);

    /**
     * 取消订单
     */
    Order cancelOrder(String orderId);

    /**
     * 查询订单
     */
    Order getOrder(String orderId);

    /**
     * 查询当前挂单
     */
    List<Order> getOpenOrders(String symbol);

    // ==================== 账户接口 ====================

    /**
     * 获取账户信息
     */
    Account getAccount();

    /**
     * 获取余额
     */
    List<Balance> getBalances();
}
```

---

### 2. 基础 HTTP 客户端（OkHttp3 实现）

```java
/**
 * 基础 HTTP 客户端
 * 使用 OkHttp3 实现，提供统一的请求处理
 */
public abstract class BaseHttpClient {

    protected final OkHttpClient httpClient;
    protected final String baseUrl;
    protected final String apiKey;
    protected final String secretKey;
    protected final RateLimiter rateLimiter;

    public BaseHttpClient(String baseUrl, String apiKey, String secretKey) {
        this(baseUrl, apiKey, secretKey, null);
    }

    public BaseHttpClient(String baseUrl, String apiKey, String secretKey, ProxyConfig proxyConfig) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.rateLimiter = RateLimiter.create(10.0); // 每秒10个请求

        // 配置 OkHttp 客户端
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
            .addInterceptor(new LoggingInterceptor())
            .addInterceptor(new RetryInterceptor(3));

        // 配置代理
        if (proxyConfig != null && proxyConfig.isEnabled()) {
            configureProxy(builder, proxyConfig);
        }

        this.httpClient = builder.build();
    }

    /**
     * 配置代理
     */
    private void configureProxy(OkHttpClient.Builder builder, ProxyConfig config) {
        try {
            // 创建代理
            Proxy proxy = new Proxy(
                config.getType(),
                new InetSocketAddress(config.getHost(), config.getPort())
            );
            builder.proxy(proxy);

            // 如果需要代理认证
            if (config.getUsername() != null && config.getPassword() != null) {
                builder.proxyAuthenticator(new Authenticator() {
                    @Override
                    public Request authenticate(Route route, Response response) throws IOException {
                        // 避免无限重试
                        if (response.request().header("Proxy-Authorization") != null) {
                            return null; // 已经尝试过认证
                        }

                        String credential = Credentials.basic(
                            config.getUsername(),
                            config.getPassword()
                        );

                        return response.request().newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build();
                    }
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
        rateLimiter.acquire(); // 限流

        try {
            // 构建 URL
            HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + endpoint).newBuilder();
            if (params != null) {
                params.forEach(urlBuilder::addQueryParameter);
            }

            // 构建请求
            Request request = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .addHeader("Content-Type", "application/json")
                .build();

            // 执行请求
            try (Response response = httpClient.newCall(request).execute()) {
                return handleResponse(response, responseType);
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

            // 执行请求
            try (Response response = httpClient.newCall(request).execute()) {
                return handleResponse(response, responseType);
            }

        } catch (IOException e) {
            throw new ExchangeApiException("POST request failed: " + endpoint, e);
        }
    }

    /**
     * 处理响应
     */
    private <T> T handleResponse(Response response, Class<T> responseType) throws IOException {
        if (!response.isSuccessful()) {
            String errorBody = response.body() != null ? response.body().string() : "Unknown error";
            throw new ExchangeApiException(
                String.format("Request failed with code %d: %s", response.code(), errorBody)
            );
        }

        String body = response.body().string();
        return parseJson(body, responseType);
    }

    /**
     * 签名方法（子类实现）
     */
    protected abstract String sign(String payload);

    /**
     * 构建查询字符串
     */
    private String buildQueryString(Map<String, String> params) {
        return params.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining("&"));
    }

    /**
     * JSON 解析
     */
    private <T> T parseJson(String json, Class<T> clazz) {
        try {
            return new ObjectMapper().readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new ExchangeApiException("Failed to parse JSON response", e);
        }
    }
}

/**
 * 日志拦截器
 */
class LoggingInterceptor implements Interceptor {
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
 * 重试拦截器
 */
class RetryInterceptor implements Interceptor {
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

                // 关闭响应体
                response.close();

                // 等待后重试
                if (i < maxRetries) {
                    Thread.sleep((long) Math.pow(2, i) * 1000); // 指数退避
                }

            } catch (IOException e) {
                lastException = e;
                if (i < maxRetries) {
                    try {
                        Thread.sleep((long) Math.pow(2, i) * 1000);
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
```

---

### 3. Binance 适配器实现

```java
/**
 * Binance 交易所适配器
 */
@Service
public class BinanceAdapter extends BaseHttpClient implements ExchangeAdapter {

    private static final String BASE_URL = "https://api.binance.com";

    public BinanceAdapter(
            @Value("${exchange.binance.api-key}") String apiKey,
            @Value("${exchange.binance.secret-key}") String secretKey) {
        super(BASE_URL, apiKey, secretKey);
    }

    @Override
    public String getExchangeName() {
        return "Binance";
    }

    @Override
    public List<Kline> getKlines(String symbol, String interval, Long startTime, Long endTime, Integer limit) {
        Map<String, String> params = new HashMap<>();
        params.put("symbol", convertSymbol(symbol)); // BTC/USDT -> BTCUSDT
        params.put("interval", convertInterval(interval)); // 1m -> 1m

        if (startTime != null) {
            params.put("startTime", String.valueOf(startTime));
        }
        if (endTime != null) {
            params.put("endTime", String.valueOf(endTime));
        }
        if (limit != null) {
            params.put("limit", String.valueOf(limit));
        } else {
            params.put("limit", "500");
        }

        // Binance 返回的是数组格式
        List<List<Object>> response = get("/api/v3/klines", params, List.class);

        return response.stream()
            .map(this::convertToKline)
            .collect(Collectors.toList());
    }

    @Override
    public Ticker getTicker(String symbol) {
        Map<String, String> params = Map.of("symbol", convertSymbol(symbol));
        BinanceTickerResponse response = get("/api/v3/ticker/24hr", params, BinanceTickerResponse.class);
        return convertToTicker(response);
    }

    @Override
    public Order createOrder(OrderRequest request) {
        Map<String, String> params = new HashMap<>();
        params.put("symbol", convertSymbol(request.getSymbol()));
        params.put("side", request.getSide().toUpperCase()); // BUY/SELL
        params.put("type", request.getType().toUpperCase()); // MARKET/LIMIT
        params.put("quantity", request.getSize().toString());

        if ("LIMIT".equals(request.getType())) {
            params.put("price", request.getPrice().toString());
            params.put("timeInForce", "GTC");
        }

        BinanceOrderResponse response = post("/api/v3/order", params, BinanceOrderResponse.class);
        return convertToOrder(response);
    }

    @Override
    protected String sign(String payload) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);

            byte[] hash = sha256_HMAC.doFinal(payload.getBytes());
            return Hex.encodeHexString(hash);
        } catch (Exception e) {
            throw new ExchangeApiException("Failed to sign request", e);
        }
    }

    /**
     * 转换交易对格式
     * BTC/USDT -> BTCUSDT
     */
    private String convertSymbol(String symbol) {
        return symbol.replace("/", "");
    }

    /**
     * 转换时间周期
     */
    private String convertInterval(String interval) {
        // 1m, 5m, 15m, 30m, 1h, 4h, 1d 等格式相同
        return interval;
    }

    /**
     * 转换为统一的 Kline 格式
     * Binance 格式：[openTime, open, high, low, close, volume, closeTime, ...]
     */
    private Kline convertToKline(List<Object> data) {
        return Kline.builder()
            .timestamp((Long) data.get(0))
            .open(new BigDecimal((String) data.get(1)))
            .high(new BigDecimal((String) data.get(2)))
            .low(new BigDecimal((String) data.get(3)))
            .close(new BigDecimal((String) data.get(4)))
            .volume(new BigDecimal((String) data.get(5)))
            .build();
    }

    private Ticker convertToTicker(BinanceTickerResponse response) {
        return Ticker.builder()
            .symbol(response.getSymbol())
            .lastPrice(new BigDecimal(response.getLastPrice()))
            .volume(new BigDecimal(response.getVolume()))
            .high(new BigDecimal(response.getHighPrice()))
            .low(new BigDecimal(response.getLowPrice()))
            .timestamp(response.getCloseTime())
            .build();
    }

    private Order convertToOrder(BinanceOrderResponse response) {
        return Order.builder()
            .id(String.valueOf(response.getOrderId()))
            .symbol(response.getSymbol())
            .side(response.getSide())
            .type(response.getType())
            .price(new BigDecimal(response.getPrice()))
            .size(new BigDecimal(response.getOrigQty()))
            .filledSize(new BigDecimal(response.getExecutedQty()))
            .status(convertOrderStatus(response.getStatus()))
            .timestamp(response.getTransactTime())
            .build();
    }

    private OrderStatus convertOrderStatus(String status) {
        return switch (status) {
            case "NEW" -> OrderStatus.OPEN;
            case "PARTIALLY_FILLED" -> OrderStatus.PARTIALLY_FILLED;
            case "FILLED" -> OrderStatus.FILLED;
            case "CANCELED" -> OrderStatus.CANCELLED;
            case "REJECTED" -> OrderStatus.REJECTED;
            default -> OrderStatus.UNKNOWN;
        };
    }
}
```

---

### 4. WebSocket 实现（基于 OkHttp3）

```java
/**
 * WebSocket 客户端基类
 */
public abstract class BaseWebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(BaseWebSocketClient.class);

    protected final OkHttpClient client;
    protected WebSocket webSocket;
    protected final Map<String, Consumer<String>> subscriptions = new ConcurrentHashMap<>();
    protected final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private volatile boolean isConnected = false;
    private volatile boolean shouldReconnect = true;

    public BaseWebSocketClient() {
        this.client = new OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS) // 心跳间隔
            .build();
    }

    /**
     * 连接 WebSocket
     */
    public void connect(String url) {
        Request request = new Request.Builder()
            .url(url)
            .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.info("WebSocket connected: {}", url);
                isConnected = true;
                onConnected();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleMessage(text);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("WebSocket error", t);
                isConnected = false;

                if (shouldReconnect) {
                    scheduleReconnect(url);
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                log.info("WebSocket closed: {} {}", code, reason);
                isConnected = false;
            }
        });
    }

    /**
     * 发送订阅消息
     */
    protected void subscribe(String channel, Consumer<String> callback) {
        subscriptions.put(channel, callback);

        if (isConnected) {
            sendSubscribeMessage(channel);
        }
    }

    /**
     * 取消订阅
     */
    protected void unsubscribe(String channel) {
        subscriptions.remove(channel);
        sendUnsubscribeMessage(channel);
    }

    /**
     * 关闭连接
     */
    public void close() {
        shouldReconnect = false;
        if (webSocket != null) {
            webSocket.close(1000, "Client closing");
        }
        scheduler.shutdown();
    }

    /**
     * 重连调度
     */
    private void scheduleReconnect(String url) {
        scheduler.schedule(() -> {
            log.info("Attempting to reconnect...");
            connect(url);
        }, 5, TimeUnit.SECONDS);
    }

    /**
     * 连接成功后的处理（子类实现）
     */
    protected abstract void onConnected();

    /**
     * 发送订阅消息（子类实现）
     */
    protected abstract void sendSubscribeMessage(String channel);

    /**
     * 发送取消订阅消息（子类实现）
     */
    protected abstract void sendUnsubscribeMessage(String channel);

    /**
     * 处理接收到的消息（子类实现）
     */
    protected abstract void handleMessage(String message);
}

/**
 * Binance WebSocket 客户端
 */
public class BinanceWebSocketClient extends BaseWebSocketClient {

    private static final String WS_URL = "wss://stream.binance.com:9443/ws";

    @Override
    protected void onConnected() {
        // 重新订阅所有频道
        subscriptions.keySet().forEach(this::sendSubscribeMessage);
    }

    @Override
    protected void sendSubscribeMessage(String channel) {
        // Binance 使用 URL 路径订阅，不需要发送订阅消息
        // 订阅在连接时通过 URL 指定
    }

    @Override
    protected void sendUnsubscribeMessage(String channel) {
        // Binance 不支持动态取消订阅，需要关闭连接重新订阅
    }

    @Override
    protected void handleMessage(String message) {
        try {
            JsonNode json = new ObjectMapper().readTree(message);
            String stream = json.get("stream").asText();

            Consumer<String> callback = subscriptions.get(stream);
            if (callback != null) {
                callback.accept(message);
            }
        } catch (Exception e) {
            log.error("Failed to handle message", e);
        }
    }

    /**
     * 订阅 K线数据
     */
    public void subscribeKline(String symbol, String interval, Consumer<Kline> callback) {
        String stream = symbol.toLowerCase() + "@kline_" + interval;
        String url = WS_URL + "/" + stream;

        subscribe(stream, message -> {
            Kline kline = parseKlineMessage(message);
            callback.accept(kline);
        });

        connect(url);
    }

    private Kline parseKlineMessage(String message) {
        // 解析 Binance K线消息格式
        // 实现省略...
        return null;
    }
}
```

---

## 代理支持

### 代理配置类

```java
/**
 * 代理配置
 */
@Data
@Builder
public class ProxyConfig {

    /**
     * 是否启用代理
     */
    private boolean enabled;

    /**
     * 代理类型
     */
    private Proxy.Type type; // HTTP, SOCKS

    /**
     * 代理主机
     */
    private String host;

    /**
     * 代理端口
     */
    private int port;

    /**
     * 代理用户名（可选）
     */
    private String username;

    /**
     * 代理密码（可选）
     */
    private String password;

    /**
     * 从配置文件创建
     */
    public static ProxyConfig fromProperties(
            boolean enabled,
            String type,
            String host,
            Integer port,
            String username,
            String password) {

        if (!enabled) {
            return ProxyConfig.builder().enabled(false).build();
        }

        Proxy.Type proxyType = "SOCKS".equalsIgnoreCase(type)
            ? Proxy.Type.SOCKS
            : Proxy.Type.HTTP;

        return ProxyConfig.builder()
            .enabled(true)
            .type(proxyType)
            .host(host)
            .port(port != null ? port : 1080)
            .username(username)
            .password(password)
            .build();
    }
}
```

### 代理配置加载

```java
/**
 * 交易所配置
 */
@Configuration
@ConfigurationProperties(prefix = "exchange")
@Data
public class ExchangeProperties {

    private BinanceConfig binance;
    private OkxConfig okx;
    private ProxySettings proxy;

    @Data
    public static class BinanceConfig {
        private boolean enabled;
        private boolean useSdk;
        private String apiKey;
        private String secretKey;
        private String baseUrl;
        private String wsUrl;
    }

    @Data
    public static class OkxConfig {
        private boolean enabled;
        private boolean useSdk;
        private String apiKey;
        private String secretKey;
        private String passphrase;
        private String baseUrl;
    }

    @Data
    public static class ProxySettings {
        private boolean enabled;
        private String type; // HTTP, SOCKS
        private String host;
        private Integer port;
        private String username;
        private String password;
    }
}
```

### Binance 适配器（支持代理）

```java
/**
 * Binance 交易所适配器（支持代理）
 */
@Service
public class BinanceAdapter extends BaseHttpClient implements ExchangeAdapter {

    private static final String BASE_URL = "https://api.binance.com";

    public BinanceAdapter(ExchangeProperties properties) {
        super(
            BASE_URL,
            properties.getBinance().getApiKey(),
            properties.getBinance().getSecretKey(),
            createProxyConfig(properties.getProxy())
        );
    }

    private static ProxyConfig createProxyConfig(ExchangeProperties.ProxySettings proxy) {
        if (proxy == null || !proxy.isEnabled()) {
            return null;
        }

        return ProxyConfig.fromProperties(
            proxy.isEnabled(),
            proxy.getType(),
            proxy.getHost(),
            proxy.getPort(),
            proxy.getUsername(),
            proxy.getPassword()
        );
    }

    @Override
    public String getExchangeName() {
        return "Binance";
    }

    // ... 其他方法实现
}
```

### WebSocket 代理支持

```java
/**
 * WebSocket 客户端基类（支持代理）
 */
public abstract class BaseWebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(BaseWebSocketClient.class);

    protected final OkHttpClient client;
    protected WebSocket webSocket;
    protected final Map<String, Consumer<String>> subscriptions = new ConcurrentHashMap<>();
    protected final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private volatile boolean isConnected = false;
    private volatile boolean shouldReconnect = true;

    public BaseWebSocketClient() {
        this(null);
    }

    public BaseWebSocketClient(ProxyConfig proxyConfig) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS) // 心跳间隔
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS); // WebSocket 不设置读超时

        // 配置代理
        if (proxyConfig != null && proxyConfig.isEnabled()) {
            configureProxy(builder, proxyConfig);
        }

        this.client = builder.build();
    }

    /**
     * 配置代理
     */
    private void configureProxy(OkHttpClient.Builder builder, ProxyConfig config) {
        try {
            Proxy proxy = new Proxy(
                config.getType(),
                new InetSocketAddress(config.getHost(), config.getPort())
            );
            builder.proxy(proxy);

            // 代理认证
            if (config.getUsername() != null && config.getPassword() != null) {
                builder.proxyAuthenticator(new Authenticator() {
                    @Override
                    public Request authenticate(Route route, Response response) throws IOException {
                        if (response.request().header("Proxy-Authorization") != null) {
                            return null;
                        }

                        String credential = Credentials.basic(
                            config.getUsername(),
                            config.getPassword()
                        );

                        return response.request().newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build();
                    }
                });
            }

            log.info("WebSocket proxy configured: {}://{}:{}",
                config.getType(), config.getHost(), config.getPort());

        } catch (Exception e) {
            log.error("Failed to configure WebSocket proxy", e);
            throw new RuntimeException("Failed to configure WebSocket proxy", e);
        }
    }

    // ... 其他方法
}

/**
 * Binance WebSocket 客户端（支持代理）
 */
public class BinanceWebSocketClient extends BaseWebSocketClient {

    private static final String WS_URL = "wss://stream.binance.com:9443/ws";

    public BinanceWebSocketClient(ProxyConfig proxyConfig) {
        super(proxyConfig);
    }

    // ... 其他方法实现
}
```

---

## 配置文件（完整版）

```yaml
# application.yml
exchange:
  # 全局代理配置
  proxy:
    enabled: true  # 是否启用代理
    type: HTTP     # 代理类型：HTTP 或 SOCKS
    host: 127.0.0.1
    port: 7890
    username: null  # 代理用户名（可选）
    password: null  # 代理密码（可选）

  binance:
    enabled: true
    use-sdk: false
    api-key: ${BINANCE_API_KEY}
    secret-key: ${BINANCE_SECRET_KEY}
    base-url: https://api.binance.com
    ws-url: wss://stream.binance.com:9443/ws

  okx:
    enabled: true
    use-sdk: false
    api-key: ${OKX_API_KEY}
    secret-key: ${OKX_SECRET_KEY}
    passphrase: ${OKX_PASSPHRASE}
    base-url: https://www.okx.com

  bybit:
    enabled: false
    use-sdk: false
    api-key: ${BYBIT_API_KEY}
    secret-key: ${BYBIT_SECRET_KEY}
    base-url: https://api.bybit.com
```

### 环境变量配置

```bash
# .env 文件
# 交易所 API 密钥
BINANCE_API_KEY=your_binance_api_key
BINANCE_SECRET_KEY=your_binance_secret_key

OKX_API_KEY=your_okx_api_key
OKX_SECRET_KEY=your_okx_secret_key
OKX_PASSPHRASE=your_okx_passphrase

# 代理配置（可选）
PROXY_ENABLED=true
PROXY_TYPE=HTTP
PROXY_HOST=127.0.0.1
PROXY_PORT=7890
PROXY_USERNAME=
PROXY_PASSWORD=
```

### 多环境配置

```yaml
# application-dev.yml（开发环境 - 使用代理）
exchange:
  proxy:
    enabled: true
    type: HTTP
    host: 127.0.0.1
    port: 7890

# application-prod.yml（生产环境 - 不使用代理）
exchange:
  proxy:
    enabled: false
```

---

## 代理测试

### 单元测试

```java
/**
 * 代理配置测试
 */
@SpringBootTest
public class ProxyConfigTest {

    @Test
    public void testHttpProxy() {
        ProxyConfig config = ProxyConfig.builder()
            .enabled(true)
            .type(Proxy.Type.HTTP)
            .host("127.0.0.1")
            .port(7890)
            .build();

        BinanceAdapter adapter = new BinanceAdapter(
            "https://api.binance.com",
            "test_key",
            "test_secret",
            config
        );

        // 测试连接
        HealthStatus health = adapter.healthCheck();
        assertTrue(health.isHealthy());
    }

    @Test
    public void testSocksProxy() {
        ProxyConfig config = ProxyConfig.builder()
            .enabled(true)
            .type(Proxy.Type.SOCKS)
            .host("127.0.0.1")
            .port(1080)
            .build();

        BinanceAdapter adapter = new BinanceAdapter(
            "https://api.binance.com",
            "test_key",
            "test_secret",
            config
        );

        HealthStatus health = adapter.healthCheck();
        assertTrue(health.isHealthy());
    }

    @Test
    public void testProxyWithAuth() {
        ProxyConfig config = ProxyConfig.builder()
            .enabled(true)
            .type(Proxy.Type.HTTP)
            .host("127.0.0.1")
            .port(7890)
            .username("proxy_user")
            .password("proxy_pass")
            .build();

        BinanceAdapter adapter = new BinanceAdapter(
            "https://api.binance.com",
            "test_key",
            "test_secret",
            config
        );

        HealthStatus health = adapter.healthCheck();
        assertTrue(health.isHealthy());
    }

    @Test
    public void testWebSocketProxy() {
        ProxyConfig config = ProxyConfig.builder()
            .enabled(true)
            .type(Proxy.Type.HTTP)
            .host("127.0.0.1")
            .port(7890)
            .build();

        BinanceWebSocketClient wsClient = new BinanceWebSocketClient(config);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Kline> receivedKline = new AtomicReference<>();

        wsClient.subscribeKline("btcusdt", "1m", kline -> {
            receivedKline.set(kline);
            latch.countDown();
        });

        // 等待接收数据
        assertTrue(latch.await(30, TimeUnit.SECONDS));
        assertNotNull(receivedKline.get());

        wsClient.close();
    }
}
```

---

## 常见代理工具

### 1. Clash

**配置示例**：
```yaml
# Clash 配置
port: 7890
socks-port: 7891
allow-lan: false
mode: rule
log-level: info
```

**使用**：
```yaml
exchange:
  proxy:
    enabled: true
    type: HTTP
    host: 127.0.0.1
    port: 7890
```

### 2. V2Ray

**配置示例**：
```json
{
  "inbounds": [{
    "port": 1080,
    "protocol": "socks",
    "settings": {
      "auth": "noauth"
    }
  }]
}
```

**使用**：
```yaml
exchange:
  proxy:
    enabled: true
    type: SOCKS
    host: 127.0.0.1
    port: 1080
```

### 3. Shadowsocks

**使用**：
```yaml
exchange:
  proxy:
    enabled: true
    type: SOCKS
    host: 127.0.0.1
    port: 1080
```

---

## 代理性能优化

### 1. 连接池配置

```java
// 针对代理优化连接池
OkHttpClient.Builder builder = new OkHttpClient.Builder()
    .connectionPool(new ConnectionPool(
        20,  // 最大空闲连接数（代理时可以增加）
        5,   // 保持时间
        TimeUnit.MINUTES
    ))
    .connectTimeout(15, TimeUnit.SECONDS)  // 代理连接可能较慢
    .readTimeout(30, TimeUnit.SECONDS);
```

### 2. 代理健康检查

```java
/**
 * 代理健康检查
 */
public class ProxyHealthChecker {

    private final OkHttpClient client;

    public ProxyHealthChecker(ProxyConfig proxyConfig) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS);

        if (proxyConfig != null && proxyConfig.isEnabled()) {
            Proxy proxy = new Proxy(
                proxyConfig.getType(),
                new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getPort())
            );
            builder.proxy(proxy);
        }

        this.client = builder.build();
    }

    /**
     * 检查代理是否可用
     */
    public boolean isProxyHealthy() {
        try {
            Request request = new Request.Builder()
                .url("https://api.binance.com/api/v3/ping")
                .get()
                .build();

            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            log.error("Proxy health check failed", e);
            return false;
        }
    }

    /**
     * 测试代理延迟
     */
    public long getProxyLatency() {
        try {
            Request request = new Request.Builder()
                .url("https://api.binance.com/api/v3/ping")
                .get()
                .build();

            long startTime = System.currentTimeMillis();
            try (Response response = client.newCall(request).execute()) {
                long endTime = System.currentTimeMillis();
                return response.isSuccessful() ? (endTime - startTime) : -1;
            }
        } catch (Exception e) {
            return -1;
        }
    }
}
```

### 3. 自动切换代理

```java
/**
 * 代理管理器（支持多代理切换）
 */
@Service
public class ProxyManager {

    private final List<ProxyConfig> proxyList;
    private final AtomicInteger currentIndex = new AtomicInteger(0);
    private final ProxyHealthChecker healthChecker;

    public ProxyManager(List<ProxyConfig> proxyList) {
        this.proxyList = proxyList;
        this.healthChecker = new ProxyHealthChecker(getCurrentProxy());

        // 定期检查代理健康状态
        scheduleHealthCheck();
    }

    /**
     * 获取当前代理
     */
    public ProxyConfig getCurrentProxy() {
        if (proxyList.isEmpty()) {
            return null;
        }
        return proxyList.get(currentIndex.get() % proxyList.size());
    }

    /**
     * 切换到下一个代理
     */
    public ProxyConfig switchToNextProxy() {
        currentIndex.incrementAndGet();
        ProxyConfig proxy = getCurrentProxy();
        log.info("Switched to proxy: {}:{}", proxy.getHost(), proxy.getPort());
        return proxy;
    }

    /**
     * 定期健康检查
     */
    private void scheduleHealthCheck() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            ProxyConfig current = getCurrentProxy();
            if (current != null && !healthChecker.isProxyHealthy()) {
                log.warn("Current proxy unhealthy, switching...");
                switchToNextProxy();
            }
        }, 0, 1, TimeUnit.MINUTES);
    }
}
```

---

## 注意事项

### 1. 安全性

- ⚠️ **不要在代码中硬编码代理密码**
- ✅ 使用环境变量或加密配置
- ✅ 生产环境建议使用无认证代理或 VPN

### 2. 性能

- 代理会增加延迟（通常 50-200ms）
- WebSocket 通过代理可能不稳定，建议使用高质量代理
- 考虑使用连接池复用

### 3. 稳定性

- 实现代理健康检查
- 支持多代理自动切换
- 记录代理使用情况和错误日志

### 4. 合规性

- 确保代理使用符合当地法律法规
- 生产环境建议使用专线或 VPN
- 避免使用免费公共代理

---



## 依赖配置

```xml
<!-- pom.xml -->
<dependencies>
    <!-- OkHttp3 -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>4.12.0</version>
    </dependency>

    <!-- JSON 处理 -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>

    <!-- 限流 -->
    <dependency>
        <groupId>com.google.guava</groupId>
        <artifactId>guava</artifactId>
        <version>32.1.3-jre</version>
    </dependency>

    <!-- 加密 -->
    <dependency>
        <groupId>commons-codec</groupId>
        <artifactId>commons-codec</artifactId>
    </dependency>

    <!-- 可选：Binance SDK（作为参考或备用） -->
    <dependency>
        <groupId>com.binance.connector</groupId>
        <artifactId>binance-connector-java</artifactId>
        <version>3.0.0</version>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

## 实施计划

### Week 1-2：基础框架

1. ✅ 定义统一接口 `ExchangeAdapter`
2. ✅ 实现 `BaseHttpClient`（OkHttp3）
3. ✅ 实现 Binance 适配器（REST API）
4. ✅ 单元测试

### Week 3：WebSocket 实现

1. ✅ 实现 `BaseWebSocketClient`
2. ✅ 实现 Binance WebSocket 客户端
3. ✅ 测试实时数据订阅

### Week 4：扩展其他交易所

1. ✅ 实现 OKX 适配器
2. ✅ 实现 Bybit 适配器（可选）
3. ✅ 集成测试

---

## 优势总结

### 1. **完全可控**
- 统一的错误处理
- 统一的重试逻辑
- 统一的限流控制
- 统一的日志记录

### 2. **易于扩展**
- 添加新交易所只需实现 `ExchangeAdapter` 接口
- 不依赖第三方 SDK 的更新

### 3. **性能优化**
- 连接池复用
- 请求限流
- 自动重试
- 心跳保活

### 4. **灵活切换**
- 支持通过配置切换实现方式
- 可以保留 SDK 作为备用方案

---

## 参考资料

1. **OkHttp 官方文档**：https://square.github.io/okhttp/
2. **Binance API 文档**：https://binance-docs.github.io/apidocs/spot/en/
3. **OKX API 文档**：https://www.okx.com/docs-v5/en/
4. **Binance Java SDK**：https://github.com/binance/binance-connector-java

---

## 下一步

完成 API 对接后，进入数据同步和存储模块开发！
