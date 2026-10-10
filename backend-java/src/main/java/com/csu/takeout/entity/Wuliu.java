package com.csu.takeout.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 物流配送记录实体，对应数据库表 wuliu
 *
 * <p>重要：向本表<b>插入</b>记录会触发数据库触发器 {@code wuliu_insert}，
 * 把对应订单的 {@code checked} 置为 1（派送中）。业务代码不得自行修改订单状态。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wuliu implements Serializable {

    /** 订单编号（主键），数据库列名 order_id */
    private Integer orderId;

    /** 顾客手机号，数据库列名 cons_phone */
    private String consPhone;

    /** 配送员编号，数据库列名 disp_id */
    private String dispId;

    /** 预计送达时间，数据库列名 deliver_time（库中为 varchar） */
    private String deliverTime;

    /** 是否结束：0 未结束、1 已结束（当前无接口可修改） */
    private Integer ended;
}
