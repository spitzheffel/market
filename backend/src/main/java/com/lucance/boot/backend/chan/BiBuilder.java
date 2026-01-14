package com.lucance.boot.backend.chan;

import com.lucance.boot.backend.chan.model.Bi;
import com.lucance.boot.backend.chan.model.Fenxing;
import com.lucance.boot.backend.chan.model.MergedKline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 笔构建器
 * 参考: 02-phase2-supplement.md
 */
@Slf4j
@Component
public class BiBuilder {

    // 最小笔长度（K线数量）
    private static final int MIN_BI_LENGTH = 5;

    /**
     * 从分型序列中构建笔
     */
    public List<Bi> build(List<Fenxing> fenxings, List<MergedKline> bars) {
        List<Bi> bis = new ArrayList<>();

        if (fenxings.size() < 2) {
            return bis;
        }

        int i = 0;
        while (i < fenxings.size() - 1) {
            Fenxing startFx = fenxings.get(i);

            // 寻找下一个有效的结束分型
            Optional<Fenxing> endFxOpt = findValidEndFenxing(startFx, fenxings, i + 1, bars);

            if (endFxOpt.isPresent()) {
                Fenxing endFx = endFxOpt.get();

                // 获取笔中的K线
                List<MergedKline> biKlines = getBiKlines(startFx, endFx, bars);

                // 创建笔对象
                Bi bi = Bi.builder()
                        .id(UUID.randomUUID().toString())
                        .startFenxing(startFx)
                        .endFenxing(endFx)
                        .direction(startFx.getType() == Fenxing.FenxingType.BOTTOM
                                ? MergedKline.Direction.UP
                                : MergedKline.Direction.DOWN)
                        .klines(biKlines)
                        .klineCount(biKlines.size())
                        .startPrice(startFx.getPrice())
                        .endPrice(endFx.getPrice())
                        .startTime(startFx.getTimestamp())
                        .endTime(endFx.getTimestamp())
                        .confirmed(true)
                        .build();

                bis.add(bi);

                // 移动到结束分型
                i = fenxings.indexOf(endFx);
            } else {
                i++;
            }
        }

        return bis;
    }

    /**
     * 寻找有效的结束分型
     */
    private Optional<Fenxing> findValidEndFenxing(
            Fenxing startFx,
            List<Fenxing> fenxings,
            int startIndex,
            List<MergedKline> bars) {

        for (int i = startIndex; i < fenxings.size(); i++) {
            Fenxing endFx = fenxings.get(i);

            // 1. 检查类型是否相反
            if (startFx.getType() == endFx.getType()) {
                continue;
            }

            // 2. 检查价格关系
            if (startFx.getType() == Fenxing.FenxingType.BOTTOM) {
                // 向上笔：结束分型必须高于起始分型
                if (endFx.getPrice().compareTo(startFx.getPrice()) <= 0) {
                    continue;
                }
            } else {
                // 向下笔：结束分型必须低于起始分型
                if (endFx.getPrice().compareTo(startFx.getPrice()) >= 0) {
                    continue;
                }
            }

            // 3. 检查K线数量
            List<MergedKline> biKlines = getBiKlines(startFx, endFx, bars);
            if (biKlines.size() < MIN_BI_LENGTH) {
                continue;
            }

            return Optional.of(endFx);
        }

        return Optional.empty();
    }

    /**
     * 获取笔中的K线序列
     */
    private List<MergedKline> getBiKlines(Fenxing startFx, Fenxing endFx, List<MergedKline> bars) {
        long startTime = startFx.getTimestamp();
        long endTime = endFx.getTimestamp();

        return bars.stream()
                .filter(k -> k.getTimestamp() >= startTime && k.getTimestamp() <= endTime)
                .toList();
    }
}
