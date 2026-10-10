package com.csu.takeout.service;

import com.csu.takeout.vo.ShopVO;

import java.util.List;

/**
 * 店铺业务接口
 */
public interface ShopService {

    /**
     * 查询全部店铺
     */
    List<ShopVO> listAll();
}
