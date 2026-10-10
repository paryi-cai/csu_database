package com.csu.takeout.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 未派发订单列表项。
 *
 * <p>字段映射关系（容易写错，务必对照）：</p>
 * <ul>
 *   <li>数据库 {@code order_money} → 响应键 {@code price}</li>
 *   <li>数据库 {@code order_way} → 响应键 {@code orderway}（无下划线，需单独覆盖）</li>
 *   <li>其余字段走全局下划线命名策略：{@code order_id}、{@code shop_name}、
 *       {@code cons_name}、{@code cons_addre}、{@code create_time}</li>
 * </ul>
 * <p>不返回 {@code cons_phone} 与 {@code checked}。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"order_id", "shop_name", "price", "orderway", "cons_name", "cons_addre", "create_time"})
public class UnsendOrderVO implements Serializable {

    /** 响应键 order_id */
    private Integer orderId;

    /** 响应键 shop_name */
    private String shopName;

    /** 响应键 price（数据库列 order_money） */
    private Integer price;

    /** 响应键 orderway（数据库列 order_way，键名无下划线） */
    @JsonProperty("orderway")
    private String orderway;

    /** 响应键 cons_name */
    private String consName;

    /** 响应键 cons_addre */
    private String consAddre;

    /** 响应键 create_time */
    private String createTime;
}
