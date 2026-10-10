package com.csu.takeout.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 经营总览（/api/manager/stats/overview）。
 *
 * <p>六个数字全都来自聚合查询，其中「未派发」数直接查 {@code oorder.checked = 0}，
 * 「派送中」数查数据库视图 {@code sending_order}（视图内部已含 checked=1 与三表连接），
 * Java 侧不重复实现状态过滤。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "total_orders", "total_revenue", "shop_count", "user_count",
        "unsend_count", "sending_count", "sended_count"
})
public class StatsOverviewVO implements Serializable {

    /** 总订单数（含未派发） */
    private Long totalOrders;

    /** 总营业额（sum(order_money)，单位：元） */
    private Long totalRevenue;

    /** 店铺数（fastfood_shop 行数） */
    private Long shopCount;

    /** 注册用户数（user 行数） */
    private Long userCount;

    /** 未派发订单数（oorder.checked = 0） */
    private Long unsendCount;

    /** 派送中订单数（视图 sending_order 行数） */
    private Long sendingCount;

    /** 已送达订单数（视图 sended_order 行数） */
    private Long sendedCount;
}
