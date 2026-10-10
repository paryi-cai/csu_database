package com.csu.takeout.exception;

/**
 * 业务异常（与 sky-take-out 的 BaseException 一致）
 */
public class BaseException extends RuntimeException {

    public BaseException() {
    }

    public BaseException(String msg) {
        super(msg);
    }
}
