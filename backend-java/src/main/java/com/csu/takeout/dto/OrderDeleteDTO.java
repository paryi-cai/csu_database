package com.csu.takeout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户取消（删除）未派发订单。
 *
 * <p>字段名与 Flask 版一致：{@code delete_id}（注意不是 order_id）。</p>
 */
@Data
@Schema(description = "取消订单参数")
public class OrderDeleteDTO implements Serializable {

    @Schema(description = "要删除的订单编号", example = "3")
    @JsonProperty("delete_id")
    private Integer deleteId;
}
