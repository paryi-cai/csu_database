package com.csu.takeout.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 订单量与营业额走势（/api/manager/stats/trend 与 /stats/shop/trend）。
 *
 * <p>三个列表等长且下标一一对应，服务端已把区间内没有订单的日期补零，
 * 前端拿到即可直接画图，不需要自己做日期补齐（否则折线会断点或横轴错位）。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "shop_name", "days", "dates", "order_counts", "revenues",
        "order_total", "revenue_total", "order_avg", "revenue_avg"
})
public class StatsTrendVO implements Serializable {

    /** 仅店铺下钻接口返回：店铺名 */
    private String shopName;

    /** 统计区间天数 */
    private Integer days;

    /** 日期序列（yyyy-MM-dd，升序） */
    private List<String> dates;

    /** 每日订单量，与 dates 下标对应 */
    private List<Long> orderCounts;

    /** 每日营业额，与 dates 下标对应 */
    private List<Long> revenues;

    /** 区间订单总量 */
    private Long orderTotal;

    /** 区间营业额总量 */
    private Long revenueTotal;

    /** 日均订单量（保留一位小数） */
    private Double orderAvg;

    /** 日均营业额（保留一位小数） */
    private Double revenueAvg;
}
