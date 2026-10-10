package com.csu.takeout.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 服务员实体，对应数据库表 server
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Server implements Serializable {

    /** 服务员编号（主键），数据库列名 service_id */
    private String serviceId;

    /** 服务员姓名，数据库列名 service_name */
    private String serviceName;

    /** 所属店铺名称，数据库列名 fastfood_shop_name */
    private String fastfoodShopName;
}
