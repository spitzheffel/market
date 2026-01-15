package com.lucance.boot.backend.chan;

import com.lucance.boot.backend.chan.model.Bi;
import com.lucance.boot.backend.chan.model.Fenxing;
import com.lucance.boot.backend.chan.model.MergedKline;
import com.lucance.boot.backend.chan.model.TradingPoint;
import com.lucance.boot.backend.chan.model.Xianduan;
import com.lucance.boot.backend.chan.model.Zhongshu;
import com.lucance.boot.backend.entity.Kline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 缠论计算引擎
 * 统一入口：K线 -> 包含处理 -> 分型识别 -> 笔构建 -> 线段识别 -> 中枢识别
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChanCalculationEngine {

    private final InclusionHandler inclusionHandler;
    private final FenxingIdentifier fenxingIdentifier;
    private final BiBuilder biBuilder;
    private final XianduanIdentifier xianduanIdentifier;
    private final ZhongshuIdentifier zhongshuIdentifier;
    private final TradingPointIdentifier tradingPointIdentifier;

    /**
     * 计算结果（完整版，包含线段和中枢）
     */
    public record ChanResultFull(
            List<MergedKline> mergedKlines,
            List<Fenxing> fenxings,
            List<Bi> bis,
            List<Xianduan> xianduans,
            List<Zhongshu> zhongshus,
            List<TradingPoint> tradingPoints) {
    }

    /**
     * 计算结果（基础版，兼容旧接口）
     */
    public record ChanResult(
            List<MergedKline> mergedKlines,
            List<Fenxing> fenxings,
            List<Bi> bis) {
    }

    /**
     * 完整计算（包含线段和中枢）
     */
    public ChanResultFull calculateFull(List<Kline> klines) {
        log.info("Starting full Chan calculation with {} klines", klines.size());

        // 1. 处理包含关系
        List<MergedKline> mergedKlines = inclusionHandler.process(klines);
        log.debug("After inclusion processing: {} merged klines", mergedKlines.size());

        // 2. 识别分型
        List<Fenxing> fenxings = fenxingIdentifier.identify(mergedKlines);
        log.debug("Identified {} fenxings", fenxings.size());

        // 3. 构建笔
        List<Bi> bis = biBuilder.build(fenxings, mergedKlines);
        log.debug("Built {} bis", bis.size());

        // 4. 识别线段
        List<Xianduan> xianduans = xianduanIdentifier.identify(bis);
        log.debug("Identified {} xianduans", xianduans.size());

        // 5. 识别中枢（笔中枢 + 线段中枢）
        List<Zhongshu> biZhongshus = zhongshuIdentifier.identifyBiZhongshu(bis);
        List<Zhongshu> xianduanZhongshus = zhongshuIdentifier.identifyXianduanZhongshu(xianduans);
        List<Zhongshu> zhongshus = new java.util.ArrayList<>(biZhongshus);
        zhongshus.addAll(xianduanZhongshus);
        log.debug("Identified {} zhongshus (bi: {}, xianduan: {})", zhongshus.size(), biZhongshus.size(),
                xianduanZhongshus.size());

        // 6. 识别买卖点（只用已确认的线段）
        List<Xianduan> confirmedXianduans = xianduans.stream()
                .filter(Xianduan::isConfirmed)
                .toList();
        List<TradingPoint> tradingPoints = tradingPointIdentifier.identify(bis, confirmedXianduans, zhongshus, klines);
        log.debug("Identified {} trading points", tradingPoints.size());

        log.info(
                "Full Chan calculation completed: {} merged klines, {} fenxings, {} bis, {} xianduans, {} zhongshus, {} trading points",
                mergedKlines.size(), fenxings.size(), bis.size(), xianduans.size(), zhongshus.size(),
                tradingPoints.size());

        return new ChanResultFull(mergedKlines, fenxings, bis, xianduans, zhongshus, tradingPoints);
    }

    /**
     * 基础计算（兼容旧接口，仅到笔）
     */
    public ChanResult calculate(List<Kline> klines) {
        log.info("Starting Chan calculation with {} klines", klines.size());

        // 1. 处理包含关系
        List<MergedKline> mergedKlines = inclusionHandler.process(klines);
        log.debug("After inclusion processing: {} merged klines", mergedKlines.size());

        // 2. 识别分型
        List<Fenxing> fenxings = fenxingIdentifier.identify(mergedKlines);
        log.debug("Identified {} fenxings", fenxings.size());

        // 3. 构建笔
        List<Bi> bis = biBuilder.build(fenxings, mergedKlines);
        log.debug("Built {} bis", bis.size());

        log.info("Chan calculation completed: {} merged klines, {} fenxings, {} bis",
                mergedKlines.size(), fenxings.size(), bis.size());

        return new ChanResult(mergedKlines, fenxings, bis);
    }

    /**
     * 仅计算到分型
     */
    public List<Fenxing> calculateFenxings(List<Kline> klines) {
        List<MergedKline> mergedKlines = inclusionHandler.process(klines);
        return fenxingIdentifier.identify(mergedKlines);
    }

    /**
     * 仅处理包含关系
     */
    public List<MergedKline> processMergedKlines(List<Kline> klines) {
        return inclusionHandler.process(klines);
    }

    /**
     * 仅计算笔
     */
    public List<Bi> calculateBis(List<Kline> klines) {
        List<MergedKline> mergedKlines = inclusionHandler.process(klines);
        List<Fenxing> fenxings = fenxingIdentifier.identify(mergedKlines);
        return biBuilder.build(fenxings, mergedKlines);
    }

    /**
     * 仅计算线段（需要先有笔）
     */
    public List<Xianduan> calculateXianduans(List<Bi> bis) {
        return xianduanIdentifier.identify(bis);
    }

    /**
     * 仅计算中枢（需要先有笔）
     */
    public List<Zhongshu> calculateZhongshus(List<Bi> bis) {
        return zhongshuIdentifier.identifyBiZhongshu(bis);
    }
}
