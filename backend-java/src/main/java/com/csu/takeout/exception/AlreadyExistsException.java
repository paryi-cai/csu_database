package com.csu.takeout.exception;

/**
 * 业务冲突异常（如编号/名称已存在）
 */
public class AlreadyExistsException extends BaseException {

    public AlreadyExistsException(String msg) {
        super(msg);
    }
}
