package com.csu.takeout.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户登录响应数据（对应 SQL JSON({"code": 200, "msg": ..., "token": ..., "role": ...})）
 *
 * <p>字段名必须与契约一致：{@code code}、{@code msg}、{@code token}、{@code role}，
 * 其中 {@code code} 与 {@code role} 都是 JSON 数字。登录失败时仅返回 code 与 msg
 * 两个字段，因此这里忽略 null 字段（token/role 为 null 时不输出）。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserLoginVO implements Serializable {

    /** 与既有契约一致的 code（固定为 200） */
    private Integer code;

    /** 提示信息 */
    private String msg;

    /** JWT 令牌 */
    private String token;

    /** 角色：0 用户端，1 管理端 */
    private Integer role;
}
