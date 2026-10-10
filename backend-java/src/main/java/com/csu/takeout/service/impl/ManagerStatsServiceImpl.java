package com.csu.takeout.service.impl;

import com.csu.takeout.mapper.ManagerStatsMapper;
import com.csu.takeout.service.ManagerStatsService;
import com.csu.takeout.vo.DispatcherWorkloadVO;
import com.csu.takeout.vo.OrderSumVO;
import com.csu.takeout.vo.ShopRankVO;
import com.csu.takeout.vo.StatsCountVO;
import com.csu.takeout.vo.StatsDailyRowVO;
import com.csu.takeout.vo.StatsHourVO;
import com.csu.takeout.vo.StatsOverviewVO;
import com.csu.takeout.vo.StatsTrendVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端数据统计实现。
 *
 * <p>只读：{@code @Transactional(readOnly = true)}，不会触发任何触发器写路径。</p>
 *
 * <p>在 Java 侧做三件事（放进 SQL 反而不可移植或不可读）：</p>
 * <ol>
 *   <li><b>日期补零</b>：区间内没有订单的日期补 0，保证折线连续、横轴等距；</li>
 *   <li><b>小时补零</b>：固定输出 0–23 共 24 个桶；</li>
 *   <li><b>比率计算</b>：占比、日均等在 Java 里算，避免 SQL 里出现除零风险。</li>
 * </ol>
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManagerStatsServiceImpl implements ManagerStatsService {

    /** 趋势接口允许的天数区间：过小没意义，过大聚合结果没人看 */
    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 90;

    /** Top 榜允许的条数区间 */
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 20;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** checked -> 展示文案，与前端列表页的状态标签保持一致 */
    private static final Map<Integer, String> STATUS_LABELS = Map.of(
            0, "待派单",
            1, "配送中",
            2, "已完成"
    );

    private final ManagerStatsMapper statsMapper;

    @Override
    public StatsOverviewVO overview() {
        OrderSumVO sums = statsMapper.sumOrders();
        return new StatsOverviewVO(
                toLong(sums == null ? null : sums.getTotalOrders()),
                toLong(sums == null ? null : sums.getTotalRevenue()),
                statsMapper.countShops(),
                statsMapper.countUsers(),
                statsMapper.countUnsend(),
                statsMapper.countSending(),
                statsMapper.countSended()
        );
    }

    @Override
    public StatsTrendVO trend(int days) {
        int range = clamp(days, MIN_DAYS, MAX_DAYS);
        LocalDate today = LocalDate.now();
        List<String> dates = buildDateRange(today, range);
        return buildTrend(null, range, dates);
    }

    @Override
    public StatsTrendVO shopTrend(String shopName, int days) {
        if (shopName == null || shopName.isBlank()) {
            return null;
        }
        ShopRankVO shop = statsMapper.getShopBase(shopName);
        if (shop == null) {
            // 店铺不存在：交给 Controller 转成业务失败响应
            return null;
        }
        int range = clamp(days, MIN_DAYS, MAX_DAYS);
        LocalDate today = LocalDate.now();
        List<String> dates = buildDateRange(today, range);

        StatsTrendVO vo = buildTrend(shopName, range, dates);
        vo.setShopName(shop.getShopName());
        return vo;
    }

    @Override
    public Map<String, Object> status() {
        Map<Integer, Long> counted = new LinkedHashMap<>();
        for (StatsCountVO row : statsMapper.listStatusCount()) {
            counted.put(toInt(row.getName()), toLong(row.getCount()));
        }

        List<Map<String, Object>> items = new ArrayList<>();
        long total = 0L;
        for (Integer status : new int[]{0, 1, 2}) {
            long count = counted.getOrDefault(status, 0L);
            total += count;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("status", status);
            item.put("label", STATUS_LABELS.get(status));
            item.put("count", count);
            items.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("total", total);
        return data;
    }

    @Override
    public Map<String, Object> shops(int limit, String metric) {
        String by = "count".equalsIgnoreCase(metric) ? "count" : "money";
        List<ShopRankVO> rows = statsMapper.listShopRank(clamp(limit, MIN_LIMIT, MAX_LIMIT), by);

        // 名次在 Java 侧补：SQL 里用窗口函数(row_number)在 H2 1.4.200 上不可用
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setRank(i + 1);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("metric", by);
        data.put("unit", "count".equals(by) ? "单" : "元");
        data.put("items", rows);
        return data;
    }

    @Override
    public Map<String, Object> orderWay() {
        List<StatsCountVO> rows = statsMapper.listOrderWay();
        long total = 0L;
        for (StatsCountVO row : rows) {
            total += toLong(row.getCount());
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (StatsCountVO row : rows) {
            long count = toLong(row.getCount());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", row.getName());
            item.put("count", count);
            item.put("ratio", total == 0 ? 0.0 : round1(count * 100.0 / total));
            items.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items);
        data.put("total", total);
        return data;
    }

    @Override
    public List<DispatcherWorkloadVO> dispatchers() {
        return statsMapper.listDispatcherWorkload();
    }

    @Override
    public StatsHourVO hours() {
        Map<Integer, Long> counted = new LinkedHashMap<>();
        for (StatsCountVO row : statsMapper.listHourCount()) {
            counted.put(toInt(row.getName()), toLong(row.getCount()));
        }

        List<Integer> hours = new ArrayList<>(24);
        List<Long> counts = new ArrayList<>(24);
        int peakHour = 0;
        long peakCount = -1L;
        for (int h = 0; h < 24; h++) {
            long count = counted.getOrDefault(h, 0L);
            hours.add(h);
            counts.add(count);
            // 严格大于：并列时取更早的小时，结果稳定
            if (count > peakCount) {
                peakCount = count;
                peakHour = h;
            }
        }
        if (peakCount < 0) {
            peakCount = 0L;
        }
        return new StatsHourVO(hours, counts, peakHour, peakCount);
    }

    /* ==================== 内部工具 ==================== */

    /**
     * 组装配零后的走势数据。
     *
     * @param shopName 为 null 表示统计全部店铺
     * @param dates    已经排好序的完整日期序列（含无订单的日期）
     */
    private StatsTrendVO buildTrend(String shopName, int days, List<String> dates) {
        Map<String, Long> orderMap = new LinkedHashMap<>();
        Map<String, Long> revenueMap = new LinkedHashMap<>();
        for (StatsDailyRowVO row : statsMapper.listDailyTrend(dates.get(0), shopName)) {
            orderMap.put(row.getDay(), toLong(row.getOrderCount()));
            revenueMap.put(row.getDay(), toLong(row.getRevenue()));
        }

        List<Long> orderCounts = new ArrayList<>(dates.size());
        List<Long> revenues = new ArrayList<>(dates.size());
        long orderTotal = 0L;
        long revenueTotal = 0L;
        for (String date : dates) {
            long count = orderMap.getOrDefault(date, 0L);
            long revenue = revenueMap.getOrDefault(date, 0L);
            orderCounts.add(count);
            revenues.add(revenue);
            orderTotal += count;
            revenueTotal += revenue;
        }

        StatsTrendVO vo = new StatsTrendVO();
        vo.setDays(days);
        vo.setDates(dates);
        vo.setOrderCounts(orderCounts);
        vo.setRevenues(revenues);
        vo.setOrderTotal(orderTotal);
        vo.setRevenueTotal(revenueTotal);
        vo.setOrderAvg(round1(orderTotal * 1.0 / dates.size()));
        vo.setRevenueAvg(round1(revenueTotal * 1.0 / dates.size()));
        return vo;
    }

    /** 生成 [today - (days-1), today] 的日期序列（升序），days 已收敛 */
    private List<String> buildDateRange(LocalDate today, int days) {
        List<String> dates = new ArrayList<>(days);
        LocalDate start = today.minusDays(days - 1L);
        for (int i = 0; i < days; i++) {
            dates.add(start.plusDays(i).format(DATE_FMT));
        }
        return dates;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /** null 安全的数值转换：聚合查询在没有数据时可能返回 null */
    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            log.warn("统计结果无法转换为数值：{}", value);
            return 0L;
        }
    }

    private int toInt(Object value) {
        return (int) toLong(value);
    }

    /** 保留一位小数，避免前端拿到 61.53846153846154 这种脏数据 */
    private double round1(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
