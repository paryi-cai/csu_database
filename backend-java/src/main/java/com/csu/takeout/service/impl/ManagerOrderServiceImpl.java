package com.csu.takeout.service.impl;

import com.csu.takeout.dto.OrderDispatchDTO;
import com.csu.takeout.entity.Wuliu;
import com.csu.takeout.mapper.OrderMapper;
import com.csu.takeout.mapper.WuliuMapper;
import com.csu.takeout.service.ManagerOrderService;
import com.csu.takeout.vo.ManagerOrderVO;
import com.csu.takeout.vo.ManagerUnsendOrderVO;
import com.csu.takeout.vo.WuliuVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 管理端订单与物流业务实现
 *
 * <p>职责边界：派发订单只向 wuliu 表插入一条记录，
 * 订单状态 {@code checked} 0→1 由数据库触发器 {@code wuliu_insert} 完成。</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ManagerOrderServiceImpl implements ManagerOrderService {

    private final OrderMapper orderMapper;
    private final WuliuMapper wuliuMapper;

    @Override
    public List<ManagerUnsendOrderVO> listUnsend() {
        return orderMapper.listAllUnsend();
    }

    @Override
    public List<ManagerOrderVO> listSending() {
        return orderMapper.listAllSending();
    }

    @Override
    public List<ManagerOrderVO> listSended() {
        return orderMapper.listAllSended();
    }

    @Override
    @Transactional
    public void dispatch(OrderDispatchDTO dto) {
        // 与 Flask 版一致：先从订单表取出顾客手机号，再写入物流记录
        String consPhone = orderMapper.getConsPhoneByOrderId(dto.getOrderId());
        wuliuMapper.insert(Wuliu.builder()
                .orderId(dto.getOrderId())
                .consPhone(consPhone)
                // 请求键为 dispatcher_id，写入 wuliu.disp_id
                .dispId(dto.getDispatcherId())
                .deliverTime(dto.getDeliverTime())
                .build());
        log.info("订单已派发：orderId={}, dispId={}", dto.getOrderId(), dto.getDispatcherId());
    }

    @Override
    public List<WuliuVO> listWuliu(Integer ended) {
        return wuliuMapper.listByEnded(ended);
    }
}
