package com.csu.takeout.service.impl;

import com.csu.takeout.mapper.ShopMapper;
import com.csu.takeout.service.ShopService;
import com.csu.takeout.vo.ShopVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 店铺业务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopMapper shopMapper;

    @Override
    public List<ShopVO> listAll() {
        // 销量由数据库触发器维护，这里只做查询
        return shopMapper.listAll();
    }
}
