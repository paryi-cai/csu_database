package com.csu.takeout.mapper;

import com.csu.takeout.vo.ShopVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 店铺相关持久层
 */
@Mapper
public interface ShopMapper {

    /**
     * 查询全部店铺（对应 Flask 版 {@code select * from fastfood_shop}）
     *
     * <p>无排序条件，与 Flask 版一致，顺序由数据库决定。</p>
     *
     * @return 店铺列表，元素键为 shop_name / price / sale
     */
    List<ShopVO> listAll();
}
