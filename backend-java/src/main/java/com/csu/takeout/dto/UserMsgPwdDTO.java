package com.csu.takeout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 个人信息页的「修改密码」参数（PUT /api/user/usermsg/pwd）。
 *
 * <p>字段与既有的 {@code POST /api/user/pwd_chg} 完全一致（{@code old_pwd} /
 * {@code new_pwd}），校验逻辑也复用同一套「原始密码错误」语义；
 * 只是为了配合个人信息页改成两者共用一个提交入口，单独提供一个同语义的路径。</p>
 *
 * <p>{@code check_pwd} 是前端确认框的字段，后端不参与校验（与 Flask 版一致：
 * Flask 的 pwd_chg 也只读 old_pwd 与 new_pwd）。</p>
 */
@Data
@Schema(description = "个人信息页修改密码参数")
public class UserMsgPwdDTO implements Serializable {

    @Schema(description = "原密码", example = "Aa.460734909")
    @JsonProperty("old_pwd")
    private String oldPwd;

    @Schema(description = "新密码", example = "Aa.460734909")
    @JsonProperty("new_pwd")
    private String newPwd;
}
