package com.csu.takeout.exception;

/**
 * 未登录 / 令牌非法异常，由全局异常处理器映射为 HTTP 401
 */
public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String msg) {
        super(msg);
    }
}
