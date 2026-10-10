package com.csu.takeout.exception;

/**
 * 登录失败异常（用户名或密码错误）
 */
public class LoginFailedException extends BaseException {

    public LoginFailedException(String msg) {
        super(msg);
    }
}
