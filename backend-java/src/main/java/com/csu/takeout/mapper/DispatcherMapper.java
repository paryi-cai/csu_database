package com.csu.takeout.mapper;

import com.csu.takeout.entity.Dispatcher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 配送员维护持久层
 */
@Mapper
public interface DispatcherMapper {

    /**
     * 查询全部配送员（对应 Flask 版 {@code select * from dispatcher}）
     */
    List<Dispatcher> listAll();

    /**
     * 新增配送员前判重（Flask 版按 dispatcher_id 查存在性）
     */
    int countByDispatcherId(@Param("dispatcherId") String dispatcherId);

    int insert(Dispatcher dispatcher);

    /**
     * 按编号解雇配送员
     */
    int deleteByDispatcherId(@Param("dispatcherId") String dispatcherId);
}
