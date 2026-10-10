package com.csu.takeout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改密码请求参数。
 *
 * <p>字段名与 Flask 版一致：{@code old_pwd}、{@code new_pwd}、{@code check_pwd}。
 * 其中 {@code check_pwd} 前端已自行校验两次输入是否一致，
 * 后端与 Flask 版一样<b>忽略</b>该字段。</p>
 */
@Data
@Schema(description = "修改密码参数")
public class PasswordChangeDTO implements Serializable {

    @Schema(description = "原密码")
    @JsonProperty("old_pwd")
    private String oldPwd;

    @Schema(description = "新密码")
    @JsonProperty("new_pwd")
    private String newPwd;

    @Schema(description = "确认新密码（后端忽略，与 Flask 版一致）")
    @JsonProperty("check_pwd")
    private String checkPwd;
}
