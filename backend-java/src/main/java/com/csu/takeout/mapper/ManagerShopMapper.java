package com.csu.takeout.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 管理端店铺维护持久层
 *
 * <p>查询复用 {@link ShopMapper#listAll()}，这里只放新增 / 修改 / 删除。</p>
 */
@Mapper
public interface ManagerShopMapper {

    /**
     * 按店铺名统计记录数（Flask 版用 {@code select * ... fetchall()} 判存在，
     * 这里用 count 达到同样的判重语义）
     */
    int countByShopName(@Param("shopName") String shopName);

    /**
     * 新增店铺，对应 Flask 版
     * {@code insert fastfood_shop(shop_name,price,m_sale_v) value(?,?,?)}
     */
    int insert(@Param("shopName") String shopName,
               @Param("price") Integer price,
               @Param("mSaleV") Integer mSaleV);

    /**
     * 修改店铺价格与销量，对应 Flask 版
     * {@code update fastfood_shop set price=?, m_sale_v=? where shop_name=?}
     */
    int update(@Param("shopName") String shopName,
               @Param("price") Integer price,
               @Param("mSaleV") Integer mSaleV);

    /**
     * 删除店铺，对应 Flask 版 {@code delete from fastfood_shop where shop_name=?}
     *
     * <p>若该店铺仍被 server.fastfood_shop_name 外键引用，数据库会拒绝删除。</p>
     */
    int deleteByShopName(@Param("shopName") String shopName);
}
