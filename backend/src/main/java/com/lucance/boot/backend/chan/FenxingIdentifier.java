package com.lucance.boot.backend.chan;

import com.lucance.boot.backend.chan.model.Fenxing;
import com.lucance.boot.backend.chan.model.MergedKline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 分型识别器
 * 参考: 02-phase2-supplement.md
 */
@Slf4j
@Component
public class FenxingIdentifier {

    /**
     * 从K线序列中识别所有分型
     */
    public List<Fenxing> identify(List<MergedKline> bars) {
        List<Fenxing> fenxings = new ArrayList<>();

        if (bars.size() < 3) {
            return fenxings;
        }

        for (int i = 1; i < bars.size() - 1; i++) {
            Optional<Fenxing> fx = checkFenxing(bars.get(i - 1), bars.get(i), bars.get(i + 1), i);

            if (fx.isPresent()) {
                Fenxing fenxing = fx.get();

                // 验证分型交替（顶底必须交替出现）
                if (!fenxings.isEmpty()) {
                    Fenxing lastFx = fenxings.get(fenxings.size() - 1);

                    if (lastFx.getType() == fenxing.getType()) {
                        // 保留更极端的分型
                        if (fenxing.getType() == Fenxing.FenxingType.TOP) {
                            if (fenxing.getPrice().compareTo(lastFx.getPrice()) > 0) {
                                fenxings.set(fenxings.size() - 1, fenxing);
                            }
                        } else {
                            if (fenxing.getPrice().compareTo(lastFx.getPrice()) < 0) {
                                fenxings.set(fenxings.size() - 1, fenxing);
                            }
                        }
                        continue;
                    }
                }

                fenxings.add(fenxing);
            }
        }

        return fenxings;
    }

    /**
     * 检查三根K线是否构成分型
     */
    public Optional<Fenxing> checkFenxing(MergedKline k1, MergedKline k2, MergedKline k3, int centerIndex) {
        // 顶分型：k2的高低点都高于k1和k3
        boolean isTop = k1.getHigh().compareTo(k2.getHigh()) < 0 &&
                k2.getHigh().compareTo(k3.getHigh()) > 0 &&
                k1.getLow().compareTo(k2.getLow()) < 0 &&
                k2.getLow().compareTo(k3.getLow()) > 0;

        // 底分型：k2的高低点都低于k1和k3
        boolean isBottom = k1.getLow().compareTo(k2.getLow()) > 0 &&
                k2.getLow().compareTo(k3.getLow()) < 0 &&
                k1.getHigh().compareTo(k2.getHigh()) > 0 &&
                k2.getHigh().compareTo(k3.getHigh()) < 0;

        if (isTop) {
            return Optional.of(Fenxing.builder()
                    .id(UUID.randomUUID().toString())
                    .type(Fenxing.FenxingType.TOP)
                    .centerIndex(centerIndex)
                    .leftIndex(centerIndex - 1)
                    .rightIndex(centerIndex + 1)
                    .price(k2.getHigh())
                    .time(k2.getTime())
                    .klines(Arrays.asList(k1, k2, k3))
                    .confirmed(true)
                    .build());
        } else if (isBottom) {
            return Optional.of(Fenxing.builder()
                    .id(UUID.randomUUID().toString())
                    .type(Fenxing.FenxingType.BOTTOM)
                    .centerIndex(centerIndex)
                    .leftIndex(centerIndex - 1)
                    .rightIndex(centerIndex + 1)
                    .price(k2.getLow())
                    .time(k2.getTime())
                    .klines(Arrays.asList(k1, k2, k3))
                    .confirmed(true)
                    .build());
        }

        return Optional.empty();
    }
}
