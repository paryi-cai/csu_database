package com.csu.takeout.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 个人信息（对应 Flask 版 {@code jsonify(status=200, data=Data)} 中的 data 对象）。
 *
 * <p>响应键与 Flask 版完全一致：{@code real_name}、{@code sex}、{@code age}、
 * {@code mail}、{@code phone}、{@code user_name}（前两者与最后一项由全局下划线命名策略生成）；
 * <b>不含</b> {@code id}。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"real_name", "sex", "age", "mail", "phone", "user_name"})
public class UserMsgVO implements Serializable {

    /** 响应键 real_name */
    private String realName;

    private String sex;

    private Integer age;

    private String mail;

    private String phone;

    /** 响应键 user_name */
    private String userName;
}
