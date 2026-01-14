# Phase 2 补充：缠论计算引擎优化

## Week 7-8: 性能优化 + 测试框架

### 2.5 增量计算引擎

#### 设计目标

基于 czsc 库的实现经验，实现高性能的增量计算引擎：
- 单次计算延迟 < 10ms
- 支持实时数据流更新
- 内存占用可控
- 状态可持久化

#### 核心数据结构

```java
/**
 * 缠论计算状态
 * 参考 czsc 库的设计，维护完整的计算状态
 */
public class ChanState {
    // 原始K线序列
    private List<Kline> rawKlines;

    // 处理包含关系后的K线序列
    private List<MergedKline> mergedKlines;

    // 已确认的分型
    private List<Fenxing> confirmedFenxing;

    // 待确认的分型（最近3根K线）
    private List<Fenxing> pendingFenxing;

    // 已确认的笔
    private List<Bi> confirmedBi;

    // 未完成的笔（bars_ubi）
    private List<MergedKline> unfinishedBiBars;

    // 线段列表
    private List<Xianduan> xianduanList;

    // 中枢列表
    private List<Zhongshu> zhongshuList;

    // 最后更新时间戳
    private long lastTimestamp;

    // MACD 缓存
    private Map<String, List<MACDResult>> macdCache;
}
```

#### 包含关系处理

基于 czsc 库的 `remove_include` 实现：

```java
/**
 * 处理K线包含关系
 * 参考: czsc/analyze.py remove_include()
 */
public class InclusionHandler {

    /**
     * 判断两根K线是否存在包含关系
     */
    private boolean hasInclusion(MergedKline k1, MergedKline k2) {
        return (k1.getHigh() >= k2.getHigh() && k1.getLow() <= k2.getLow()) ||
               (k1.getHigh() <= k2.getHigh() && k1.getLow() >= k2.getLow());
    }

    /**
     * 合并包含关系的K线
     * @param k1 第一根K线
     * @param k2 第二根K线（包含在k1中或包含k1）
     * @param direction 当前方向（基于前一根K线确定）
     */
    public MergedKline mergeKlines(MergedKline k1, MergedKline k2, Direction direction) {
        BigDecimal newHigh, newLow;

        if (direction == Direction.UP) {
            // 向上：取两者中的最高点和最高的最低点
            newHigh = k1.getHigh().max(k2.getHigh());
            newLow = k1.getLow().max(k2.getLow());
        } else {
            // 向下：取两者中的最低点和最低的最高点
            newHigh = k1.getHigh().min(k2.getHigh());
            newLow = k1.getLow().min(k2.getLow());
        }

        // 合并元素列表（限制最大100个元素，防止内存溢出）
        List<Kline> elements = new ArrayList<>(k1.getElements());
        elements.addAll(k2.getElements());
        if (elements.size() > 100) {
            elements = elements.subList(elements.size() - 100, elements.size());
        }

        return MergedKline.builder()
            .symbol(k1.getSymbol())
            .timestamp(k2.getTimestamp()) // 使用最新的时间戳
            .open(k1.getOpen())
            .high(newHigh)
            .low(newLow)
            .close(k2.getClose())
            .volume(k1.getVolume().add(k2.getVolume()))
            .elements(elements)
            .build();
    }

    /**
     * 处理新K线，移除包含关系
     */
    public List<MergedKline> processNewKline(List<MergedKline> existingBars, Kline newKline) {
        List<MergedKline> result = new ArrayList<>(existingBars);

        // 将新K线转换为 MergedKline
        MergedKline newBar = MergedKline.fromKline(newKline);

        if (result.isEmpty()) {
            result.add(newBar);
            return result;
        }

        // 确定方向
        Direction direction = determineDirection(result);

        // 处理包含关系
        while (result.size() >= 1 && hasInclusion(result.get(result.size() - 1), newBar)) {
            MergedKline lastBar = result.remove(result.size() - 1);
            newBar = mergeKlines(lastBar, newBar, direction);
        }

        result.add(newBar);
        return result;
    }

    private Direction determineDirection(List<MergedKline> bars) {
        if (bars.size() < 2) {
            return Direction.UP;
        }
        MergedKline k1 = bars.get(bars.size() - 2);
        MergedKline k2 = bars.get(bars.size() - 1);
        return k1.getHigh().compareTo(k2.getHigh()) < 0 ? Direction.UP : Direction.DOWN;
    }
}
```

---

### 2.6 分型识别优化

基于 czsc 库的 `check_fx` 和 `check_fxs` 实现：

```java
/**
 * 分型识别器
 * 参考: czsc/analyze.py check_fx() 和 check_fxs()
 */
public class FenxingIdentifier {

    /**
     * 检查三根K线是否构成分型
     */
    public Optional<Fenxing> checkFenxing(MergedKline k1, MergedKline k2, MergedKline k3) {
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
                .type(FenxingType.TOP)
                .centerIndex(1) // k2的索引
                .leftIndex(0)
                .rightIndex(2)
                .price(k2.getHigh())
                .timestamp(k2.getTimestamp())
                .klines(Arrays.asList(k1, k2, k3))
                .confirmed(false)
                .build());
        } else if (isBottom) {
            return Optional.of(Fenxing.builder()
                .type(FenxingType.BOTTOM)
                .centerIndex(1)
                .leftIndex(0)
                .rightIndex(2)
                .price(k2.getLow())
                .timestamp(k2.getTimestamp())
                .klines(Arrays.asList(k1, k2, k3))
                .confirmed(false)
                .build());
        }

        return Optional.empty();
    }

    /**
     * 从K线序列中识别所有分型
     */
    public List<Fenxing> identifyFenxings(List<MergedKline> bars) {
        List<Fenxing> fenxings = new ArrayList<>();

        if (bars.size() < 3) {
            return fenxings;
        }

        for (int i = 1; i < bars.size() - 1; i++) {
            Optional<Fenxing> fx = checkFenxing(bars.get(i - 1), bars.get(i), bars.get(i + 1));

            if (fx.isPresent()) {
                Fenxing fenxing = fx.get();

                // 验证分型交替（顶底必须交替出现）
                if (!fenxings.isEmpty()) {
                    Fenxing lastFx = fenxings.get(fenxings.size() - 1);
                    if (lastFx.getType() == fenxing.getType()) {
                        log.warn("检测到连续相同类型的分型，这不应该发生！位置: {}", i);
                        // 保留更极端的分型
                        if (fenxing.getType() == FenxingType.TOP) {
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
}
```

---

### 2.7 笔识别与破坏处理

基于 czsc 库的 `check_bi` 和笔破坏检测实现：

```java
/**
 * 笔识别器
 * 参考: czsc/analyze.py check_bi() 和 CZSC.__update_bi()
 */
public class BiIdentifier {

    private static final int MIN_BI_LENGTH = 5; // 最小笔长度（K线数量）

    /**
     * 从分型序列中识别笔
     */
    public List<Bi> identifyBis(List<Fenxing> fenxings, List<MergedKline> bars) {
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
                    .direction(startFx.getType() == FenxingType.BOTTOM ? Direction.UP : Direction.DOWN)
                    .klines(biKlines)
                    .klineCount(biKlines.size())
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
     * 必须满足：
     * 1. 类型相反（顶底交替）
     * 2. 价格关系正确（向上笔：结束分型高于起始分型）
     * 3. 没有包含关系
     * 4. K线数量 >= MIN_BI_LENGTH
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
            if (startFx.getType() == FenxingType.BOTTOM) {
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

            // 3. 检查是否有包含关系
            boolean hasInclusion = checkInclusion(startFx, endFx);
            if (hasInclusion) {
                continue;
            }

            // 4. 检查K线数量
            List<MergedKline> biKlines = getBiKlines(startFx, endFx, bars);
            if (biKlines.size() < MIN_BI_LENGTH) {
                continue;
            }

            return Optional.of(endFx);
        }

        return Optional.empty();
    }

    /**
     * 检查两个分型是否存在包含关系
     */
    private boolean checkInclusion(Fenxing fx1, Fenxing fx2) {
        BigDecimal fx1High = fx1.getKlines().stream()
            .map(MergedKline::getHigh)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);

        BigDecimal fx1Low = fx1.getKlines().stream()
            .map(MergedKline::getLow)
            .min(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);

        BigDecimal fx2High = fx2.getKlines().stream()
            .map(MergedKline::getHigh)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);

        BigDecimal fx2Low = fx2.getKlines().stream()
            .map(MergedKline::getLow)
            .min(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);

        return (fx1High.compareTo(fx2High) >= 0 && fx1Low.compareTo(fx2Low) <= 0) ||
               (fx1High.compareTo(fx2High) <= 0 && fx1Low.compareTo(fx2Low) >= 0);
    }

    /**
     * 获取笔中的K线序列
     */
    private List<MergedKline> getBiKlines(Fenxing startFx, Fenxing endFx, List<MergedKline> bars) {
        long startTime = startFx.getTimestamp();
        long endTime = endFx.getTimestamp();

        return bars.stream()
            .filter(k -> k.getTimestamp() >= startTime && k.getTimestamp() <= endTime)
            .collect(Collectors.toList());
    }

    /**
     * 检查笔是否被破坏
     * 参考: czsc/analyze.py CZSC.__update_bi()
     */
    public boolean isBiDestroyed(Bi bi, List<MergedKline> newBars) {
        if (newBars.isEmpty()) {
            return false;
        }

        MergedKline lastBar = newBars.get(newBars.size() - 1);

        if (bi.getDirection() == Direction.UP) {
            // 向上笔：如果新K线的高点超过笔的终点，笔被破坏
            return lastBar.getHigh().compareTo(bi.getEndFenxing().getPrice()) > 0;
        } else {
            // 向下笔：如果新K线的低点低于笔的终点，笔被破坏
            return lastBar.getLow().compareTo(bi.getEndFenxing().getPrice()) < 0;
        }
    }

    /**
     * 处理笔破坏后的K线合并
     * 参考: czsc 库的处理逻辑
     */
    public List<MergedKline> mergeBarsAfterDestruction(Bi destroyedBi, List<MergedKline> newBars) {
        List<MergedKline> result = new ArrayList<>();

        // 保留被破坏笔的前n-2根K线
        List<MergedKline> biKlines = destroyedBi.getKlines();
        if (biKlines.size() >= 2) {
            result.addAll(biKlines.subList(0, biKlines.size() - 2));
        }

        // 添加新K线中时间戳 >= 倒数第二根K线的所有K线
        if (biKlines.size() >= 2) {
            long thresholdTime = biKlines.get(biKlines.size() - 2).getTimestamp();
            result.addAll(newBars.stream()
                .filter(k -> k.getTimestamp() >= thresholdTime)
                .collect(Collectors.toList()));
        } else {
            result.addAll(newBars);
        }

        return result;
    }
}
```

---

### 2.8 线段识别算法（增强版）

基于缠论理论和 czsc 库的实践经验，实现完整的线段识别算法：

```java
/**
 * 线段识别器（增强版）
 * 包含完整的边界情况处理和缺口处理
 */
public class XianduanIdentifier {

    /**
     * 从笔序列中识别线段
     */
    public List<Xianduan> identifyXianduan(List<Bi> biList) {
        if (biList.size() < 3) {
            return new ArrayList<>();
        }

        // 1. 构建特征序列
        List<FeatureElement> features = buildFeatureSequence(biList);

        // 2. 识别线段
        return identifySegmentsFromFeatures(features, biList);
    }

    /**
     * 构建特征序列
     * 特征序列：相邻笔的重叠部分
     */
    private List<FeatureElement> buildFeatureSequence(List<Bi> biList) {
        List<FeatureElement> features = new ArrayList<>();

        for (int i = 0; i < biList.size() - 1; i++) {
            Bi bi1 = biList.get(i);
            Bi bi2 = biList.get(i + 1);

            // 计算重叠区间
            BigDecimal high1 = bi1.getDirection() == Direction.UP ?
                bi1.getEndFenxing().getPrice() : bi1.getStartFenxing().getPrice();
            BigDecimal low1 = bi1.getDirection() == Direction.UP ?
                bi1.getStartFenxing().getPrice() : bi1.getEndFenxing().getPrice();

            BigDecimal high2 = bi2.getDirection() == Direction.UP ?
                bi2.getEndFenxing().getPrice() : bi2.getStartFenxing().getPrice();
            BigDecimal low2 = bi2.getDirection() == Direction.UP ?
                bi2.getStartFenxing().getPrice() : bi2.getEndFenxing().getPrice();

            // 重叠区间
            BigDecimal overlapHigh = high1.min(high2);
            BigDecimal overlapLow = low1.max(low2);

            // 检查是否有缺口
            boolean hasGap = overlapHigh.compareTo(overlapLow) < 0;

            FeatureElement feature = FeatureElement.builder()
                .index(i)
                .bi1(bi1)
                .bi2(bi2)
                .high(overlapHigh)
                .low(overlapLow)
                .hasGap(hasGap)
                .gapSize(hasGap ? overlapLow.subtract(overlapHigh) : BigDecimal.ZERO)
                .build();

            features.add(feature);
        }

        return features;
    }

    /**
     * 从特征序列识别线段
     */
    private List<Xianduan> identifySegmentsFromFeatures(
            List<FeatureElement> features,
            List<Bi> biList) {

        List<Xianduan> segments = new ArrayList<>();

        if (features.isEmpty()) {
            return segments;
        }

        int segmentStart = 0;

        for (int i = 0; i < features.size(); i++) {
            // 检查线段是否被破坏
            if (isSegmentBroken(features, segmentStart, i)) {
                // 创建线段
                Xianduan segment = createSegment(biList, segmentStart, i);
                segments.add(segment);

                // 开始新线段
                segmentStart = i;
            }
        }

        // 处理最后一个未完成的线段
        if (segmentStart < biList.size() - 1) {
            Xianduan segment = createSegment(biList, segmentStart, biList.size() - 1);
            segment.setConfirmed(false); // 未完成的线段
            segments.add(segment);
        }

        return segments;
    }

    /**
     * 判断线段是否被破坏
     *
     * 线段破坏的条件：
     * 1. 标准破坏：特征序列被破坏（后续笔突破前面的特征区间）
     * 2. 缺口破坏：出现缺口且缺口被回补
     * 3. 九笔规则：至少需要5笔才能构成线段
     */
    private boolean isSegmentBroken(
            List<FeatureElement> features,
            int start,
            int current) {

        if (current - start < 2) {
            return false; // 至少需要3个特征元素（对应4笔）
        }

        // 1. 检查标准破坏
        if (checkStandardBreakdown(features, start, current)) {
            return true;
        }

        // 2. 检查缺口破坏
        if (checkGapBreakdown(features, start, current)) {
            return true;
        }

        // 3. 检查九笔规则（至少5笔）
        if (current - start >= 4) {
            return checkNineBiRule(features, start, current);
        }

        return false;
    }

    /**
     * 检查标准破坏：后续笔突破前面的特征区间
     */
    private boolean checkStandardBreakdown(
            List<FeatureElement> features,
            int start,
            int current) {

        if (current <= start + 1) {
            return false;
        }

        FeatureElement currentFeature = features.get(current);
        Bi currentBi = currentFeature.getBi2();

        // 获取前面的特征区间
        for (int i = start; i < current - 1; i++) {
            FeatureElement prevFeature = features.get(i);

            // 检查当前笔是否突破了前面的特征区间
            if (currentBi.getDirection() == Direction.UP) {
                BigDecimal currentHigh = currentBi.getEndFenxing().getPrice();
                if (currentHigh.compareTo(prevFeature.getHigh()) > 0) {
                    return true;
                }
            } else {
                BigDecimal currentLow = currentBi.getEndFenxing().getPrice();
                if (currentLow.compareTo(prevFeature.getLow()) < 0) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 检查缺口破坏
     */
    private boolean checkGapBreakdown(
            List<FeatureElement> features,
            int start,
            int current) {

        // 查找缺口
        for (int i = start; i < current; i++) {
            FeatureElement feature = features.get(i);
            if (feature.isHasGap()) {
                // 检查缺口是否被后续笔回补
                for (int j = i + 1; j <= current; j++) {
                    FeatureElement laterFeature = features.get(j);
                    Bi laterBi = laterFeature.getBi2();

                    // 判断缺口是否被回补
                    BigDecimal biHigh = laterBi.getDirection() == Direction.UP ?
                        laterBi.getEndFenxing().getPrice() : laterBi.getStartFenxing().getPrice();
                    BigDecimal biLow = laterBi.getDirection() == Direction.UP ?
                        laterBi.getStartFenxing().getPrice() : laterBi.getEndFenxing().getPrice();

                    // 缺口在 [feature.high, feature.low] 之间
                    // 如果后续笔的区间覆盖了缺口，则缺口被回补
                    if (biLow.compareTo(feature.getHigh()) <= 0 &&
                        biHigh.compareTo(feature.getLow()) >= 0) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 检查九笔规则
     * 至少需要5笔才能构成一个完整的线段
     */
    private boolean checkNineBiRule(
            List<FeatureElement> features,
            int start,
            int current) {

        int biCount = current - start + 1;
        return biCount >= 5;
    }

    /**
     * 创建线段对象
     */
    private Xianduan createSegment(List<Bi> biList, int startIndex, int endIndex) {
        List<Bi> segmentBis = biList.subList(startIndex, endIndex + 1);

        Bi firstBi = segmentBis.get(0);
        Bi lastBi = segmentBis.get(segmentBis.size() - 1);

        return Xianduan.builder()
            .id(UUID.randomUUID().toString())
            .startBi(firstBi)
            .endBi(lastBi)
            .direction(firstBi.getDirection())
            .bis(segmentBis)
            .biCount(segmentBis.size())
            .confirmed(true)
            .build();
    }
}

/**
 * 特征序列元素
 */
@Data
@Builder
class FeatureElement {
    private int index;
    private Bi bi1;
    private Bi bi2;
    private BigDecimal high;
    private BigDecimal low;
    private boolean hasGap;
    private BigDecimal gapSize;
}
```

---

### 2.9 多级别时间对齐算法

多级别分析的核心是精确的时间对齐。参考 czsc 库的实践经验：

```java
/**
 * 多级别时间对齐器
 * 实现不同周期K线之间的精确时间映射
 */
public class MultiLevelTimeAligner {

    /**
     * 级别关系映射
     * 例如：1分钟 -> 5分钟 -> 15分钟 -> 30分钟 -> 60分钟 -> 日线
     */
    private static final Map<String, Integer> LEVEL_MULTIPLIERS = Map.of(
        "1m", 1,
        "5m", 5,
        "15m", 15,
        "30m", 30,
        "1h", 60,
        "4h", 240,
        "1d", 1440
    );

    /**
     * 将低级别K线对齐到高级别
     * @param lowLevelKlines 低级别K线列表
     * @param lowLevel 低级别周期（如 "1m"）
     * @param highLevel 高级别周期（如 "5m"）
     * @return 对齐后的映射关系
     */
    public Map<Long, List<Kline>> alignToHigherLevel(
            List<Kline> lowLevelKlines,
            String lowLevel,
            String highLevel) {

        int multiplier = LEVEL_MULTIPLIERS.get(highLevel) / LEVEL_MULTIPLIERS.get(lowLevel);

        if (multiplier <= 1) {
            throw new IllegalArgumentException("高级别必须大于低级别");
        }

        Map<Long, List<Kline>> alignment = new TreeMap<>();

        for (Kline kline : lowLevelKlines) {
            // 计算该K线所属的高级别时间戳
            long highLevelTimestamp = alignTimestamp(
                kline.getTimestamp(),
                LEVEL_MULTIPLIERS.get(lowLevel),
                LEVEL_MULTIPLIERS.get(highLevel)
            );

            alignment.computeIfAbsent(highLevelTimestamp, k -> new ArrayList<>())
                .add(kline);
        }

        return alignment;
    }

    /**
     * 时间戳对齐算法
     * 将低级别时间戳对齐到高级别的起始时间
     */
    private long alignTimestamp(long timestamp, int lowLevelMinutes, int highLevelMinutes) {
        // 转换为分钟数
        long minutes = timestamp / (60 * 1000);

        // 对齐到高级别的起始分钟
        long alignedMinutes = (minutes / highLevelMinutes) * highLevelMinutes;

        // 转换回毫秒
        return alignedMinutes * 60 * 1000;
    }

    /**
     * 构建多级别缠论结果的映射关系
     */
    public MultiLevelMapping buildMultiLevelMapping(
            Map<String, ChanResult> levelResults) {

        MultiLevelMapping mapping = new MultiLevelMapping();

        // 按级别从小到大排序
        List<String> sortedLevels = levelResults.keySet().stream()
            .sorted(Comparator.comparingInt(LEVEL_MULTIPLIERS::get))
            .collect(Collectors.toList());

        // 构建相邻级别之间的映射
        for (int i = 0; i < sortedLevels.size() - 1; i++) {
            String lowLevel = sortedLevels.get(i);
            String highLevel = sortedLevels.get(i + 1);

            ChanResult lowResult = levelResults.get(lowLevel);
            ChanResult highResult = levelResults.get(highLevel);

            // 构建笔的映射关系
            Map<String, List<String>> biMapping = buildBiMapping(
                lowResult.getBis(),
                highResult.getBis(),
                lowLevel,
                highLevel
            );

            mapping.addBiMapping(lowLevel, highLevel, biMapping);

            // 构建线段的映射关系
            Map<String, List<String>> xianduanMapping = buildXianduanMapping(
                lowResult.getXianduans(),
                highResult.getXianduans(),
                lowLevel,
                highLevel
            );

            mapping.addXianduanMapping(lowLevel, highLevel, xianduanMapping);
        }

        return mapping;
    }

    /**
     * 构建笔的映射关系
     * 一根高级别的笔可能对应多根低级别的笔
     */
    private Map<String, List<String>> buildBiMapping(
            List<Bi> lowLevelBis,
            List<Bi> highLevelBis,
            String lowLevel,
            String highLevel) {

        Map<String, List<String>> mapping = new HashMap<>();

        for (Bi highBi : highLevelBis) {
            List<String> correspondingLowBis = new ArrayList<>();

            long highBiStart = highBi.getStartFenxing().getTimestamp();
            long highBiEnd = highBi.getEndFenxing().getTimestamp();

            // 找到时间范围重叠的低级别笔
            for (Bi lowBi : lowLevelBis) {
                long lowBiStart = lowBi.getStartFenxing().getTimestamp();
                long lowBiEnd = lowBi.getEndFenxing().getTimestamp();

                // 检查时间范围是否有重叠
                if (lowBiEnd >= highBiStart && lowBiStart <= highBiEnd) {
                    correspondingLowBis.add(lowBi.getId());
                }
            }

            mapping.put(highBi.getId(), correspondingLowBis);
        }

        return mapping;
    }

    /**
     * 构建线段的映射关系
     */
    private Map<String, List<String>> buildXianduanMapping(
            List<Xianduan> lowLevelXianduans,
            List<Xianduan> highLevelXianduans,
            String lowLevel,
            String highLevel) {

        Map<String, List<String>> mapping = new HashMap<>();

        for (Xianduan highXd : highLevelXianduans) {
            List<String> correspondingLowXds = new ArrayList<>();

            long highXdStart = highXd.getStartBi().getStartFenxing().getTimestamp();
            long highXdEnd = highXd.getEndBi().getEndFenxing().getTimestamp();

            for (Xianduan lowXd : lowLevelXianduans) {
                long lowXdStart = lowXd.getStartBi().getStartFenxing().getTimestamp();
                long lowXdEnd = lowXd.getEndBi().getEndFenxing().getTimestamp();

                if (lowXdEnd >= highXdStart && lowXdStart <= highXdEnd) {
                    correspondingLowXds.add(lowXd.getId());
                }
            }

            mapping.put(highXd.getId(), correspondingLowXds);
        }

        return mapping;
    }
}

/**
 * 多级别映射关系
 */
@Data
public class MultiLevelMapping {
    // 笔的映射：高级别笔ID -> 低级别笔ID列表
    private Map<String, Map<String, List<String>>> biMappings = new HashMap<>();

    // 线段的映射：高级别线段ID -> 低级别线段ID列表
    private Map<String, Map<String, List<String>>> xianduanMappings = new HashMap<>();

    public void addBiMapping(String lowLevel, String highLevel, Map<String, List<String>> mapping) {
        String key = lowLevel + "->" + highLevel;
        biMappings.put(key, mapping);
    }

    public void addXianduanMapping(String lowLevel, String highLevel, Map<String, List<String>> mapping) {
        String key = lowLevel + "->" + highLevel;
        xianduanMappings.put(key, mapping);
    }

    /**
     * 获取高级别笔对应的低级别笔
     */
    public List<String> getLowLevelBis(String lowLevel, String highLevel, String highBiId) {
        String key = lowLevel + "->" + highLevel;
        Map<String, List<String>> mapping = biMappings.get(key);
        return mapping != null ? mapping.getOrDefault(highBiId, new ArrayList<>()) : new ArrayList<>();
    }
}
```

---

### 2.10 测试框架

#### 单元测试

```java
/**
 * 包含关系处理测试
 */
@SpringBootTest
public class InclusionHandlerTest {

    @Autowired
    private InclusionHandler inclusionHandler;

    @Test
    public void testMergeKlines_UpDirection() {
        // 准备测试数据
        MergedKline k1 = createMergedKline("2024-01-01 10:00", "100", "110", "95", "105");
        MergedKline k2 = createMergedKline("2024-01-01 10:01", "105", "108", "98", "102");

        // 执行合并（向上方向）
        MergedKline merged = inclusionHandler.mergeKlines(k1, k2, Direction.UP);

        // 验证结果
        assertEquals(new BigDecimal("110"), merged.getHigh()); // 取最高的高点
        assertEquals(new BigDecimal("98"), merged.getLow());   // 取最高的低点
        assertEquals(2, merged.getElements().size());
    }

    @Test
    public void testMergeKlines_DownDirection() {
        MergedKline k1 = createMergedKline("2024-01-01 10:00", "100", "110", "95", "105");
        MergedKline k2 = createMergedKline("2024-01-01 10:01", "105", "108", "98", "102");

        MergedKline merged = inclusionHandler.mergeKlines(k1, k2, Direction.DOWN);

        assertEquals(new BigDecimal("108"), merged.getHigh()); // 取最低的高点
        assertEquals(new BigDecimal("95"), merged.getLow());   // 取最低的低点
    }

    @Test
    public void testElementsLimit() {
        // 测试元素数量限制（最多100个）
        MergedKline k1 = createMergedKlineWithElements(101);
        MergedKline k2 = createMergedKline("2024-01-01 10:01", "105", "108", "98", "102");

        MergedKline merged = inclusionHandler.mergeKlines(k1, k2, Direction.UP);

        assertTrue(merged.getElements().size() <= 100);
    }
}

/**
 * 分型识别测试
 */
@SpringBootTest
public class FenxingIdentifierTest {

    @Autowired
    private FenxingIdentifier fenxingIdentifier;

    @Test
    public void testTopFenxing() {
        // 构造顶分型：k2的高低点都高于k1和k3
        MergedKline k1 = createMergedKline("2024-01-01 10:00", "100", "105", "95", "102");
        MergedKline k2 = createMergedKline("2024-01-01 10:01", "102", "115", "100", "110");
        MergedKline k3 = createMergedKline("2024-01-01 10:02", "110", "108", "98", "100");

        Optional<Fenxing> fx = fenxingIdentifier.checkFenxing(k1, k2, k3);

        assertTrue(fx.isPresent());
        assertEquals(FenxingType.TOP, fx.get().getType());
        assertEquals(new BigDecimal("115"), fx.get().getPrice());
    }

    @Test
    public void testBottomFenxing() {
        MergedKline k1 = createMergedKline("2024-01-01 10:00", "100", "105", "95", "102");
        MergedKline k2 = createMergedKline("2024-01-01 10:01", "102", "100", "85", "90");
        MergedKline k3 = createMergedKline("2024-01-01 10:02", "90", "108", "88", "100");

        Optional<Fenxing> fx = fenxingIdentifier.checkFenxing(k1, k2, k3);

        assertTrue(fx.isPresent());
        assertEquals(FenxingType.BOTTOM, fx.get().getType());
        assertEquals(new BigDecimal("85"), fx.get().getPrice());
    }

    @Test
    public void testNoFenxing() {
        // 不构成分型的情况
        MergedKline k1 = createMergedKline("2024-01-01 10:00", "100", "105", "95", "102");
        MergedKline k2 = createMergedKline("2024-01-01 10:01", "102", "110", "100", "108");
        MergedKline k3 = createMergedKline("2024-01-01 10:02", "108", "115", "105", "112");

        Optional<Fenxing> fx = fenxingIdentifier.checkFenxing(k1, k2, k3);

        assertFalse(fx.isPresent());
    }

    @Test
    public void testFenxingAlternation() {
        // 测试分型交替
        List<MergedKline> bars = createTestBars();
        List<Fenxing> fenxings = fenxingIdentifier.identifyFenxings(bars);

        // 验证分型交替（顶底交替）
        for (int i = 1; i < fenxings.size(); i++) {
            assertNotEquals(fenxings.get(i - 1).getType(), fenxings.get(i).getType());
        }
    }
}

/**
 * 笔识别测试
 */
@SpringBootTest
public class BiIdentifierTest {

    @Autowired
    private BiIdentifier biIdentifier;

    @Test
    public void testValidBi() {
        // 构造有效的笔：底分型 -> 顶分型，满足所有条件
        List<Fenxing> fenxings = createValidFenxings();
        List<MergedKline> bars = createTestBars();

        List<Bi> bis = biIdentifier.identifyBis(fenxings, bars);

        assertTrue(bis.size() > 0);
        assertEquals(Direction.UP, bis.get(0).getDirection());
        assertTrue(bis.get(0).getKlineCount() >= 5);
    }

    @Test
    public void testBiDestruction() {
        // 测试笔破坏
        Bi bi = createTestBi(Direction.UP, "50000");
        List<MergedKline> newBars = createBarsBreakingBi("50100");

        boolean destroyed = biIdentifier.isBiDestroyed(bi, newBars);

        assertTrue(destroyed);
    }

    @Test
    public void testInsufficientLength() {
        // 测试K线数量不足的情况
        List<Fenxing> fenxings = createFenxingsWithShortDistance();
        List<MergedKline> bars = createTestBars();

        List<Bi> bis = biIdentifier.identifyBis(fenxings, bars);

        // 应该过滤掉长度不足的笔
        assertTrue(bis.stream().allMatch(bi -> bi.getKlineCount() >= 5));
    }
}

/**
 * 线段识别测试
 */
@SpringBootTest
public class XianduanIdentifierTest {

    @Autowired
    private XianduanIdentifier xianduanIdentifier;

    @Test
    public void testStandardBreakdown() {
        // 测试标准破坏
        List<Bi> bis = createBisForStandardBreakdown();

        List<Xianduan> xianduans = xianduanIdentifier.identifyXianduan(bis);

        assertTrue(xianduans.size() > 0);
        assertTrue(xianduans.get(0).getBiCount() >= 3);
    }

    @Test
    public void testGapBreakdown() {
        // 测试缺口破坏
        List<Bi> bis = createBisWithGap();

        List<Xianduan> xianduans = xianduanIdentifier.identifyXianduan(bis);

        // 验证缺口被正确识别和处理
        assertTrue(xianduans.size() > 0);
    }

    @Test
    public void testNineBiRule() {
        // 测试九笔规则
        List<Bi> bis = createNineBis();

        List<Xianduan> xianduans = xianduanIdentifier.identifyXianduan(bis);

        // 至少5笔才能构成线段
        assertTrue(xianduans.stream().allMatch(xd -> xd.getBiCount() >= 5));
    }
}
```

#### 集成测试

```java
/**
 * 完整流程集成测试
 */
@SpringBootTest
public class ChanEngineIntegrationTest {

    @Autowired
    private ChanCalculationEngine engine;

    @Test
    public void testCompleteFlow() {
        // 1. 准备测试数据
        List<Kline> klines = loadTestKlines("BTC/USDT", "1m", 1000);

        // 2. 执行完整计算
        ChanResult result = engine.calculate(klines);

        // 3. 验证结果
        assertNotNull(result);
        assertTrue(result.getMergedKlines().size() > 0);
        assertTrue(result.getFenxings().size() > 0);
        assertTrue(result.getBis().size() > 0);

        // 4. 验证数据一致性
        validateDataConsistency(result);
    }

    @Test
    public void testIncrementalUpdate() {
        // 测试增量更新
        List<Kline> initialKlines = loadTestKlines("BTC/USDT", "1m", 500);
        ChanResult initialResult = engine.calculate(initialKlines);

        // 添加新K线
        Kline newKline = createNewKline();
        ChanResult updatedResult = engine.incrementalUpdate(initialResult, newKline);

        // 验证增量更新的正确性
        assertNotNull(updatedResult);
        assertEquals(initialResult.getMergedKlines().size() + 1,
                    updatedResult.getMergedKlines().size());
    }

    @Test
    public void testPerformance() {
        // 性能测试：单次计算 < 10ms
        List<Kline> klines = loadTestKlines("BTC/USDT", "1m", 100);

        long startTime = System.currentTimeMillis();
        ChanResult result = engine.calculate(klines);
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;
        assertTrue(duration < 10, "计算耗时: " + duration + "ms");
    }

    private void validateDataConsistency(ChanResult result) {
        // 验证分型交替
        List<Fenxing> fenxings = result.getFenxings();
        for (int i = 1; i < fenxings.size(); i++) {
            assertNotEquals(fenxings.get(i - 1).getType(), fenxings.get(i).getType());
        }

        // 验证笔的方向
        List<Bi> bis = result.getBis();
        for (Bi bi : bis) {
            if (bi.getStartFenxing().getType() == FenxingType.BOTTOM) {
                assertEquals(Direction.UP, bi.getDirection());
            } else {
                assertEquals(Direction.DOWN, bi.getDirection());
            }
        }
    }
}
```

---

### 2.11 性能基准测试

```java
/**
 * 性能基准测试
 */
@SpringBootTest
public class PerformanceBenchmarkTest {

    @Autowired
    private ChanCalculationEngine engine;

    @Test
    public void benchmarkCalculationSpeed() {
        int[] dataSizes = {100, 500, 1000, 5000, 10000};

        for (int size : dataSizes) {
            List<Kline> klines = loadTestKlines("BTC/USDT", "1m", size);

            long totalTime = 0;
            int iterations = 10;

            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                engine.calculate(klines);
                long endTime = System.nanoTime();
                totalTime += (endTime - startTime);
            }

            long avgTime = totalTime / iterations / 1_000_000; // 转换为毫秒

            System.out.println(String.format(
                "数据量: %d, 平均耗时: %d ms",
                size, avgTime
            ));

            // 验收标准：< 10ms
            if (size <= 1000) {
                assertTrue(avgTime < 10);
            }
        }
    }

    @Test
    public void benchmarkMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();

        // 执行GC
        System.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // 执行计算
        List<Kline> klines = loadTestKlines("BTC/USDT", "1m", 10000);
        ChanResult result = engine.calculate(klines);

        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = (memoryAfter - memoryBefore) / 1024 / 1024; // MB

        System.out.println("内存使用: " + memoryUsed + " MB");

        // 验收标准：< 100MB
        assertTrue(memoryUsed < 100);
    }
}
```

---

## Phase 2 交付物

### 代码交付物
- [x] 增量计算引擎
- [x] 包含关系处理
- [x] 分型识别器
- [x] 笔识别器（含破坏处理）
- [x] 线段识别器（增强版，含缺口处理）
- [x] 多级别时间对齐器
- [x] 完整的单元测试
- [x] 集成测试
- [x] 性能基准测试

### 测试覆盖率目标
- [ ] 单元测试覆盖率 > 80%
- [ ] 集成测试覆盖率 > 70%
- [ ] 核心算法覆盖率 > 90%

### 性能指标
- [ ] 单次计算延迟 < 10ms（1000根K线）
- [ ] 内存占用 < 100MB（10000根K线）
- [ ] 支持实时数据流更新

---

## 参考资料

1. **czsc 库**：https://github.com/waditu/czsc
   - 查看 v0.9.69 版本的纯 Python 实现
   - 核心文件：`czsc/analyze.py`、`czsc/objects.py`

2. **缠论原理**
   - 包含关系处理
   - 分型、笔、线段的定义和识别规则
   - 特征序列和线段破坏

3. **算法优化**
   - 增量计算
   - 状态管理
   - 内存控制

---

## 下一步

完成 Phase 2 后，进入 Phase 3：策略和回测系统开发！
