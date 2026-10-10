package com.csu.takeout.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 管理端未派发订单列表项。
 *
 * <p>键名与用户端未派发列表一致（{@code order_money} → {@code price}、
 * {@code order_way} → {@code orderway}），比用户端多一个 {@code cons_phone}。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"order_id", "shop_name", "price", "orderway", "cons_phone", "cons_name",
        "cons_addre", "create_time"})
public class ManagerUnsendOrderVO implements Serializable {

    /** 响应键 order_id */
    private Integer orderId;

    /** 响应键 shop_name */
    private String shopName;

    /** 响应键 price（数据库列 order_money） */
    private Integer price;

    /** 响应键 orderway（数据库列 order_way，键名无下划线） */
    @JsonProperty("orderway")
    private String orderway;

    /** 响应键 cons_phone */
    private String consPhone;

    /** 响应键 cons_name */
    private String consName;

    /** 响应键 cons_addre */
    private String consAddre;

    /** 响应键 create_time */
    private String createTime;
}
