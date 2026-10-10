package com.csu.takeout.service;

import com.csu.takeout.entity.Server;

import java.util.List;

/**
 * 服务员维护业务接口
 */
public interface ServerService {

    List<Server> listAll();

    /** 新增服务员；编号已存在时返回 false */
    boolean add(String serviceId, String serviceName, String fastfoodShopName);

    /** 解雇服务员 */
    void delete(String serviceId);
}
