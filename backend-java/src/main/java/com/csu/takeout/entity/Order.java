package com.csu.takeout.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单实体，对应数据库表 oorder
 *
 * <p>注意：{@code checked} 字段（0 未派发 / 1 派送中 / 2 已送达）完全由数据库触发器维护
 * （wuliu 表插入时由触发器 {@code wuliu_insert} 置为 1），业务代码<b>不得</b>直接修改。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order implements Serializable {

    /** 订单编号，自增主键 */
    private Integer orderId;

    /** 店铺名称 */
    private String shopName;

    /** 订单金额，数据库列名 order_money */
    private Integer orderMoney;

    /** 订餐方式，数据库列名 order_way */
    private String orderWay;

    /** 顾客手机号，取自 JWT 载荷 */
    private String consPhone;

    /** 顾客姓名 */
    private String consName;

    /** 送餐地址 */
    private String consAddre;

    /** 订单状态：0 未派发、1 派送中、2 已送达（由数据库触发器维护） */
    private Integer checked;

    /** 下单时间，数据库列名 create_time；库中为 varchar(50)，格式 yyyy-MM-dd HH:mm:ss */
    private String createTime;
}
