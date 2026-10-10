package com.csu.takeout.service;

import com.csu.takeout.dto.UserLoginDTO;
import com.csu.takeout.result.UserLoginResult;

/**
 * 用户相关业务接口
 */
public interface UserService {

    /**
     * 用户登录：校验手机号与密码，成功后签发 JWT
     *
     * @param userLoginDTO 登录参数
     * @return 登录结果（含 token 与角色），失败时 status = 1000
     */
    UserLoginResult login(UserLoginDTO userLoginDTO);

    /**
     * 修改密码
     *
     * @param telephone 当前登录用户手机号
     * @param oldPwd    原密码
     * @param newPwd    新密码
     * @return true 修改成功；false 原密码错误
     */
    boolean changePassword(String telephone, String oldPwd, String newPwd);
}
