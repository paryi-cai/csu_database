package com.csu.takeout.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户端「派送中 / 已送达」订单列表项（数据来自数据库视图 sending_order / sended_order）。
 *
 * <p>字段映射关系：视图列 {@code dispatcher_phone} → 响应键 {@code disp_phone}
 * （需单独覆盖）；其余字段走全局下划线命名策略。</p>
 * <p>注意：管理端同类接口（/api/manager/sending、/api/manager/sended）<b>不返回</b>
 * {@code disp_phone}，届时用另一个 VO 控制。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"order_id", "shop_name", "order_money", "order_way", "cons_phone", "cons_name",
        "cons_addre", "disp_id", "deliver_time", "disp_phone"})
public class SendingOrderVO implements Serializable {

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

    /** 响应键 disp_phone（视图列 dispatcher_phone） */
    @JsonProperty("disp_phone")
    private String dispPhone;
}
