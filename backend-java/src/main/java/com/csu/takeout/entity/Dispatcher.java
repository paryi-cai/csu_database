package com.csu.takeout.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 配送员实体，对应数据库表 dispatcher
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dispatcher implements Serializable {

    /** 配送员编号（主键），数据库列名 dispatcher_id */
    private String dispatcherId;

    /** 配送员姓名，数据库列名 dispatcher_name */
    private String dispatcherName;

    /** 配送员电话，数据库列名 dispatcher_phone */
    private String dispatcherPhone;
}
