package com.csu.takeout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理端派发订单参数（POST /api/manager/unsend）。
 *
 * <p>字段名与契约一致，注意请求键是 {@code dispatcher_id}，
 * 而写入 wuliu 表时对应 {@code disp_id} 列。</p>
 */
@Data
@Schema(description = "派发订单参数")
public class OrderDispatchDTO implements Serializable {

    @Schema(description = "订单编号")
    @JsonProperty("order_id")
    private Integer orderId;

    @Schema(description = "配送员编号")
    @JsonProperty("dispatcher_id")
    private String dispatcherId;

    @Schema(description = "预计送达时间")
    @JsonProperty("deliver_time")
    private String deliverTime;
}
