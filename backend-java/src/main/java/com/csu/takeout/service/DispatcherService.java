package com.csu.takeout.service;

import com.csu.takeout.entity.Dispatcher;

import java.util.List;

/**
 * 配送员维护业务接口
 */
public interface DispatcherService {

    List<Dispatcher> listAll();

    /** 新增配送员；编号已存在时返回 false */
    boolean add(String dispatcherId, String dispatcherName, String dispatcherPhone);

    /** 解雇配送员 */
    void delete(String dispatcherId);
}
