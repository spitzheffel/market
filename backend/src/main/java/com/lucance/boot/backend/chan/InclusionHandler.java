package com.lucance.boot.backend.chan;

import com.lucance.boot.backend.chan.model.MergedKline;
import com.lucance.boot.backend.entity.Kline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 包含关系处理器
 * 参考: 02-phase2-supplement.md
 */
@Slf4j
@Component
public class InclusionHandler {

    /**
     * 处理K线序列，移除包含关系
     */
    public List<MergedKline> process(List<Kline> klines) {
        if (klines == null || klines.isEmpty()) {
            return new ArrayList<>();
        }

        List<MergedKline> result = new ArrayList<>();

        for (int i = 0; i < klines.size(); i++) {
            Kline kline = klines.get(i);
            MergedKline newBar = MergedKline.fromKline(kline, i);

            if (result.isEmpty()) {
                result.add(newBar);
                continue;
            }

            // 确定方向
            MergedKline.Direction direction = determineDirection(result);
            newBar.setDirection(direction);

            // 处理包含关系
            while (!result.isEmpty() && hasInclusion(result.get(result.size() - 1), newBar)) {
                MergedKline lastBar = result.remove(result.size() - 1);
                newBar = mergeKlines(lastBar, newBar, direction);
            }

            result.add(newBar);
        }

        // 更新索引
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setIndex(i);
        }

        return result;
    }

    /**
     * 判断两根K线是否存在包含关系
     */
    private boolean hasInclusion(MergedKline k1, MergedKline k2) {
        BigDecimal h1 = k1.getHigh();
        BigDecimal l1 = k1.getLow();
        BigDecimal h2 = k2.getHigh();
        BigDecimal l2 = k2.getLow();

        // k1 包含 k2
        boolean k1ContainsK2 = h1.compareTo(h2) >= 0 && l1.compareTo(l2) <= 0;
        // k2 包含 k1
        boolean k2ContainsK1 = h1.compareTo(h2) <= 0 && l1.compareTo(l2) >= 0;

        return k1ContainsK2 || k2ContainsK1;
    }

    /**
     * 合并包含关系的K线
     */
    private MergedKline mergeKlines(MergedKline k1, MergedKline k2, MergedKline.Direction direction) {
        BigDecimal newHigh, newLow;

        if (direction == MergedKline.Direction.UP) {
            // 向上：取两者中的最高点和最高的最低点
            newHigh = k1.getHigh().max(k2.getHigh());
            newLow = k1.getLow().max(k2.getLow());
        } else {
            // 向下：取两者中的最低点和最低的最高点
            newHigh = k1.getHigh().min(k2.getHigh());
            newLow = k1.getLow().min(k2.getLow());
        }

        // 合并元素列表（限制最大100个元素）
        List<Kline> elements = new ArrayList<>(k1.getElements());
        elements.addAll(k2.getElements());
        if (elements.size() > 100) {
            elements = elements.subList(elements.size() - 100, elements.size());
        }

        return MergedKline.builder()
                .index(k1.getIndex())
                .direction(direction)
                .open(k1.getOpen())
                .high(newHigh)
                .low(newLow)
                .close(k2.getClose())
                .time(k2.getTime())
                .volume(k1.getVolume().add(k2.getVolume()))
                .elements(elements)
                .build();
    }

    /**
     * 确定方向
     */
    private MergedKline.Direction determineDirection(List<MergedKline> bars) {
        if (bars.size() < 2) {
            return MergedKline.Direction.UP;
        }

        MergedKline k1 = bars.get(bars.size() - 2);
        MergedKline k2 = bars.get(bars.size() - 1);

        return k1.getHigh().compareTo(k2.getHigh()) < 0
                ? MergedKline.Direction.UP
                : MergedKline.Direction.DOWN;
    }
}
