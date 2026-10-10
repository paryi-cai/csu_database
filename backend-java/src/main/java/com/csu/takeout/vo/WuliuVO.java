package com.csu.takeout.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 物流记录列表项（对应 /api/manager/wuliu）。
 *
 * <p>响应只包含四个键，<b>不返回</b> {@code ended}。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"order_id", "cons_phone", "disp_id", "deliver_time"})
public class WuliuVO implements Serializable {

    /** 响应键 order_id */
    private Integer orderId;

    /** 响应键 cons_phone */
    private String consPhone;

    /** 响应键 disp_id */
    private String dispId;

    /** 响应键 deliver_time */
    private String deliverTime;
}
