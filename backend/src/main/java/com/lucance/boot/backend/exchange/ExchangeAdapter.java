package com.lucance.boot.backend.exchange;

import com.lucance.boot.backend.entity.Kline;
import com.lucance.boot.backend.exchange.model.*;

import java.util.List;
import java.util.function.Consumer;

/**
 * 交易所适配器统一接口
 * 参考: 01-phase1-api-integration.md
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
     *
     * @param symbol    交易对（如 "BTC/USDT"）
     * @param interval  时间周期（如 "1m", "5m", "1h"）
     * @param startTime 开始时间（毫秒时间戳）
     * @param endTime   结束时间（毫秒时间戳）
     * @param limit     数量限制（默认500，最大1000）
     */
    List<Kline> getKlines(String symbol, String interval, Long startTime, Long endTime, int limit);

    /**
     * 获取最新价格
     */
    Ticker getTicker(String symbol);

    // ==================== WebSocket 订阅 ====================

    /**
     * 订阅K线数据流
     */
    void subscribeKline(String symbol, String interval, Consumer<Kline> callback);

    /**
     * 取消K线订阅
     */
    void unsubscribeKline(String symbol, String interval);

    // ==================== 交易接口（需要API Key）====================

    /**
     * 创建订单
     */
    Order placeOrder(OrderRequest request);

    /**
     * 取消订单
     */
    Order cancelOrder(String orderId, String symbol);

    /**
     * 查询订单
     */
    Order getOrder(String orderId, String symbol);

    // ==================== 账户接口 ====================

    /**
     * 获取账户余额
     */
    List<Balance> getBalances();
}
