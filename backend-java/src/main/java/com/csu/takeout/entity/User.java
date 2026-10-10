package com.csu.takeout.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户实体，对应数据库表 user
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    /** 主键 id */
    private Integer id;

    /** 用户名 */
    private String username;

    /** 密码（明文存储，沿用原库设计） */
    private String password;

    /** 手机号，登录后作为 JWT 载荷与业务查询键 */
    private String telephone;

    /** 角色：0 普通用户，1 管理员 */
    private Integer role;
}
