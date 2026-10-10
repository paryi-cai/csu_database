package com.csu.takeout.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 下单时段分布（/api/manager/stats/hours）。
 *
 * <p>固定返回 0–23 共 24 个桶（没有订单的小时补 0），前端不需要自己补齐横轴。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"hours", "order_counts", "peak_hour", "peak_count"})
public class StatsHourVO implements Serializable {

    /** 小时桶：0 ~ 23 */
    private List<Integer> hours;

    /** 各小时的订单量，与 hours 下标对应 */
    private List<Long> orderCounts;

    /** 订单量最高的小时（全为 0 时返回 0） */
    private Integer peakHour;

    /** 峰值小时的订单量 */
    private Long peakCount;
}
