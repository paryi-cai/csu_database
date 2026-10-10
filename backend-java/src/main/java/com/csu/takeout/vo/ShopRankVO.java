package com.csu.takeout.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 店铺业绩榜条目（/api/manager/stats/shops）。
 *
 * <p>同时带上 {@code price} 与 {@code sale}：单价来自 {@code fastfood_shop.price}，
 * {@code sale} 即数据库列 {@code m_sale_v}（销量，由触发器 order_insert_sale /
 * order_delete_sale 维护）。页面把它与实时聚合出来的 {@code order_count} 并列展示，
 * 用来直观说明"触发器维护的累计销量"与"oorder 实时聚合"的口径差异。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"shop_name", "order_count", "revenue", "price", "sale", "rank"})
public class ShopRankVO implements Serializable {

    /** 店铺名 */
    private String shopName;

    /** 区间订单量（oorder 实时聚合） */
    private Long orderCount;

    /** 区间营业额（oorder 实时聚合，单位：元） */
    private Long revenue;

    /** 产品单价（fastfood_shop.price） */
    private Integer price;

    /** 数据库销量（fastfood_shop.m_sale_v，触发器维护） */
    private Integer sale;

    /** 名次，从 1 开始 */
    private Integer rank;
}
