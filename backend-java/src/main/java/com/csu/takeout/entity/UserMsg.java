package com.csu.takeout.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户详细信息实体，对应数据库表 user_msg
 *
 * <p>表中没有主键，通过 {@code id}（外键关联 user.id）与用户关联；
 * Flask 版是按 {@code phone} 字段查询的，这里保持一致。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMsg implements Serializable {

    /** 用户 id，外键关联 user.id */
    private Integer id;

    /** 真实姓名 */
    private String realName;

    /** 性别 */
    private String sex;

    /** 年龄 */
    private Integer age;

    /** 邮箱 */
    private String mail;

    /** 手机号 */
    private String phone;

    /** 用户名 */
    private String userName;
}
