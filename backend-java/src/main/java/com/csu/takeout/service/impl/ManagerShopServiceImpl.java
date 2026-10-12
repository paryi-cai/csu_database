package com.csu.takeout.service.impl;

import com.csu.takeout.constant.RedisConstant;
import com.csu.takeout.mapper.ManagerShopMapper;
import com.csu.takeout.mapper.ShopMapper;
import com.csu.takeout.service.ManagerShopService;
import com.csu.takeout.vo.ShopVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 管理端店铺维护业务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ManagerShopServiceImpl implements ManagerShopService {

    private final ShopMapper shopMapper;
    private final ManagerShopMapper managerShopMapper;

    @Override
    public List<ShopVO> listAll() {
        // 与用户端共用同一条查询（响应键：shop_name / price / sale）
        return shopMapper.listAll();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = RedisConstant.STATS_CACHE, allEntries = true)
    public boolean add(String shopName, Integer price, Integer mSaleV) {
        if (managerShopMapper.countByShopName(shopName) > 0) {
            return false;
        }
        managerShopMapper.insert(shopName, price, mSaleV);
        return true;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = RedisConstant.STATS_CACHE, allEntries = true)
    public void change(String shopName, Integer price, Integer mSaleV) {
        managerShopMapper.update(shopName, price, mSaleV);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = RedisConstant.STATS_CACHE, allEntries = true)
    public void delete(String shopName) {
        // 若店铺被 server.fastfood_shop_name 外键引用，数据库会拒绝删除并抛出约束异常
        managerShopMapper.deleteByShopName(shopName);
    }
}
