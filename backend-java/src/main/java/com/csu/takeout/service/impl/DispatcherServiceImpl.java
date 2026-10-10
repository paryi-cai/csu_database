package com.csu.takeout.service.impl;

import com.csu.takeout.entity.Dispatcher;
import com.csu.takeout.mapper.DispatcherMapper;
import com.csu.takeout.service.DispatcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 配送员维护业务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DispatcherServiceImpl implements DispatcherService {

    private final DispatcherMapper dispatcherMapper;

    @Override
    public List<Dispatcher> listAll() {
        return dispatcherMapper.listAll();
    }

    @Override
    @Transactional
    public boolean add(String dispatcherId, String dispatcherName, String dispatcherPhone) {
        if (dispatcherMapper.countByDispatcherId(dispatcherId) > 0) {
            return false;
        }
        dispatcherMapper.insert(Dispatcher.builder()
                .dispatcherId(dispatcherId)
                .dispatcherName(dispatcherName)
                .dispatcherPhone(dispatcherPhone)
                .build());
        return true;
    }

    @Override
    @Transactional
    public void delete(String dispatcherId) {
        dispatcherMapper.deleteByDispatcherId(dispatcherId);
    }
}
