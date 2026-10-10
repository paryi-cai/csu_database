package com.csu.takeout.service;

import com.csu.takeout.vo.ShopVO;

import java.util.List;

/**
 * 管理端店铺维护业务接口
 */
public interface ManagerShopService {

    /** 查询全部店铺（与用户端 /api/user/shop 同一份数据、同一套响应键） */
    List<ShopVO> listAll();

    /** 新增店铺；已存在同名店铺时返回 false */
    boolean add(String shopName, Integer price, Integer mSaleV);

    /** 修改店铺价格与销量 */
    void change(String shopName, Integer price, Integer mSaleV);

    /** 删除店铺 */
    void delete(String shopName);
}
