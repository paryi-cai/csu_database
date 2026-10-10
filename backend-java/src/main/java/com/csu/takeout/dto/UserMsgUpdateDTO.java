package com.csu.takeout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改个人信息请求参数（PUT /api/user/usermsg/edit）。
 *
 * <p><b>可改 5 项</b>：用户名（{@code user_name}）、真实姓名（{@code real_name}）、
 * 性别（{@code sex}）、年龄（{@code age}）、邮箱（{@code mail}）。</p>
 *
 * <p><b>刻意不含 {@code phone}</b>：{@code user_msg.phone} 是既有接口
 * {@code GET /api/user/usermsg} 的查询键（前后端 都是
 * {@code select * from user_msg where phone = ?}），一旦允许修改，用户改完就再也查不到
 * 自己的记录（接口会返回没有 data 键的 {"status":200}）。因此联系电话在页面上只读，
 * 请求体里也不提供该字段。</p>
 *
 * <p><b>多传字段会被直接拒绝，而不是被忽略</b>：本项目 Jackson 保持
 * {@code FAIL_ON_UNKNOWN_PROPERTIES} 开启（默认值，未关闭），请求体里出现未声明属性时
 * 反序列化直接失败，经全局异常处理器返回 {@code {"status":1000,"msg":"未知错误"}}。
 * 行为由 {@code UserMsgApiTest#phoneIsNotEditable} 锁定。好处是"想改什么"与"能改什么"
 * 完全一致；代价是前端多传字段不会静默成功（本项目前端只提交下面这 5 个字段）。</p>
 *
 * <p>全局下划线命名策略只作用于序列化（响应），请求字段必须显式标注 {@link JsonProperty}。</p>
 */
@Data
@Schema(description = "修改个人信息参数")
public class UserMsgUpdateDTO implements Serializable {

    @Schema(description = "用户名", example = "CMX")
    @JsonProperty("user_name")
    private String userName;

    @Schema(description = "真实姓名", example = "蔡明轩")
    @JsonProperty("real_name")
    private String realName;

    @Schema(description = "性别", example = "男")
    @JsonProperty("sex")
    private String sex;

    @Schema(description = "年龄", example = "21")
    @JsonProperty("age")
    private Integer age;

    @Schema(description = "邮箱", example = "320836@qq.com")
    @JsonProperty("mail")
    private String mail;
}
