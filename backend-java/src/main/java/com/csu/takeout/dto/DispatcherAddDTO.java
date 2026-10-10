package com.csu.takeout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理端新增配送员参数。字段名与契约一致。
 */
@Data
@Schema(description = "新增配送员参数")
public class DispatcherAddDTO implements Serializable {

    @Schema(description = "配送员编号", example = "010102")
    @JsonProperty("dispatcher_id")
    private String dispatcherId;

    @Schema(description = "配送员姓名", example = "小亮")
    @JsonProperty("dispatcher_name")
    private String dispatcherName;

    @Schema(description = "配送员电话", example = "15878977898")
    @JsonProperty("dispatcher_phone")
    private String dispatcherPhone;
}
