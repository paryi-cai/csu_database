package com.csu.takeout.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 快餐店铺实体，对应数据库表 fastfood_shop
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FastfoodShop implements Serializable {

    /** 店铺名称（主键） */
    private String shopName;

    /** 产品单价 */
    private Integer price;

    /** 月销量，数据库列名 m_sale_v */
    private Integer mSaleV;
}
