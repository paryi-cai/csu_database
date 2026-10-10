package com.csu.takeout.service.impl;

import com.csu.takeout.entity.Server;
import com.csu.takeout.mapper.ServerMapper;
import com.csu.takeout.service.ServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 服务员维护业务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServerServiceImpl implements ServerService {

    private final ServerMapper serverMapper;

    @Override
    public List<Server> listAll() {
        return serverMapper.listAll();
    }

    @Override
    @Transactional
    public boolean add(String serviceId, String serviceName, String fastfoodShopName) {
        if (serverMapper.countByServiceId(serviceId) > 0) {
            return false;
        }
        serverMapper.insert(Server.builder()
                .serviceId(serviceId)
                .serviceName(serviceName)
                .fastfoodShopName(fastfoodShopName)
                .build());
        return true;
    }

    @Override
    @Transactional
    public void delete(String serviceId) {
        serverMapper.deleteByServiceId(serviceId);
    }
}
