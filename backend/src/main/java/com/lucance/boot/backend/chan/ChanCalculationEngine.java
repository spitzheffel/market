package com.lucance.boot.backend.chan;

import com.lucance.boot.backend.chan.model.Bi;
import com.lucance.boot.backend.chan.model.Fenxing;
import com.lucance.boot.backend.chan.model.MergedKline;
import com.lucance.boot.backend.entity.Kline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 缠论计算引擎
 * 统一入口：K线 -> 包含处理 -> 分型识别 -> 笔构建
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChanCalculationEngine {

    private final InclusionHandler inclusionHandler;
    private final FenxingIdentifier fenxingIdentifier;
    private final BiBuilder biBuilder;

    /**
     * 计算结果
     */
    public record ChanResult(
            List<MergedKline> mergedKlines,
            List<Fenxing> fenxings,
            List<Bi> bis) {
    }

    /**
     * 完整计算
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
}
