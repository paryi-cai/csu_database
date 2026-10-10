package com.csu.takeout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理端新增服务员参数。字段名与契约一致。
 */
@Data
@Schema(description = "新增服务员参数")
public class ServerAddDTO implements Serializable {

    @Schema(description = "服务员编号", example = "100012")
    @JsonProperty("service_id")
    private String serviceId;

    @Schema(description = "服务员姓名", example = "刀哥")
    @JsonProperty("service_name")
    private String serviceName;

    @Schema(description = "所属店铺名称", example = "小美烤盘饭")
    @JsonProperty("fastfood_shop_name")
    private String fastfoodShopName;
}
