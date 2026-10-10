package com.csu.takeout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户修改未派发订单的收货信息。
 *
 * <p>字段名与 Flask 版一致：{@code order_id}、{@code cons_name}、{@code cons_addre}。</p>
 */
@Data
@Schema(description = "修改未派发订单的收货信息")
public class OrderUpdateDTO implements Serializable {

    @Schema(description = "订单编号", example = "3")
    @JsonProperty("order_id")
    private Integer orderId;

    @Schema(description = "顾客姓名")
    @JsonProperty("cons_name")
    private String consName;

    @Schema(description = "送餐地址")
    @JsonProperty("cons_addre")
    private String consAddre;
}
