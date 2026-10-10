package com.csu.takeout.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 管理端「派送中 / 已送达」订单列表项（数据来自视图 sending_order / sended_order）。
 *
 * <p>与用户端的区别：管理端接口<b>不返回</b> {@code disp_phone}，
 * 因此单独建类而不是复用 {@link SendingOrderVO}。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"order_id", "shop_name", "order_money", "order_way", "cons_phone", "cons_name",
        "cons_addre", "disp_id", "deliver_time"})
public class ManagerOrderVO implements Serializable {

    /** 响应键 order_id */
    private Integer orderId;

    /** 响应键 shop_name */
    private String shopName;

    /** 响应键 order_money */
    private Integer orderMoney;

    /** 响应键 order_way */
    private String orderWay;

    /** 响应键 cons_phone */
    private String consPhone;

    /** 响应键 cons_name */
    private String consName;

    /** 响应键 cons_addre */
    private String consAddre;

    /** 响应键 disp_id */
    private String dispId;

    /** 响应键 deliver_time */
    private String deliverTime;
}
