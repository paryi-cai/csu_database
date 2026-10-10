package com.csu.takeout.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求参数，字段名固定为 {@code userortel}（user or telephone）与 {@code password}，与前端表单一致
 */
@Data
@Schema(description = "用户登录参数")
public class UserLoginDTO implements Serializable {

    /** 手机号（字段名沿用 既有接口的 userortel，即 user or telephone） */
    @Schema(description = "手机号", example = "18873190398")
    private String userortel;

    /** 密码 */
    @Schema(description = "密码", example = "aA.460734909")
    private String password;
}
