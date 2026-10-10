package com.csu.takeout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 删除店铺 / 服务员 / 配送员的通用参数。
 *
 * <p>既有接口的删除接口统一从请求体的 {@code want_delete} 字段取值：
 * 店铺管理里它是店铺名，服务员与配送员管理里它是编号。</p>
 */
@Data
@Schema(description = "删除参数（店铺名或编号）")
public class WantDeleteDTO implements Serializable {

    @Schema(description = "要删除的店铺名称或人员编号")
    @JsonProperty("want_delete")
    private String wantDelete;
}
