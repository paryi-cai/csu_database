package com.csu.takeout.mapper;

import com.csu.takeout.entity.Server;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 服务员维护持久层
 */
@Mapper
public interface ServerMapper {

    /**
     * 查询全部服务员（对应 SQL：{@code select * from server}）
     */
    List<Server> listAll();

    /**
     * 新增服务员前判重（既有接口按 service_id 查存在性）
     */
    int countByServiceId(@Param("serviceId") String serviceId);

    int insert(Server server);

    /**
     * 按编号解雇服务员
     */
    int deleteByServiceId(@Param("serviceId") String serviceId);
}
