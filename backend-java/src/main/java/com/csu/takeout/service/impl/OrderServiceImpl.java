package com.csu.takeout.service.impl;

import com.csu.takeout.dto.OrderSubmitDTO;
import com.csu.takeout.dto.OrderUpdateDTO;
import com.csu.takeout.entity.Order;
import com.csu.takeout.mapper.OrderMapper;
import com.csu.takeout.service.OrderService;
import com.csu.takeout.vo.SendingOrderVO;
import com.csu.takeout.vo.UnsendOrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 用户端订单业务实现
 *
 * <p>职责边界（与契约一致）：</p>
 * <ul>
 *   <li>订单状态 {@code checked} <b>不</b>由本类维护，派生逻辑全部在数据库触发器里
 *       （派发时 {@code wuliu_insert} 置 1）；</li>
 *   <li>派送中 / 已送达订单直接查数据库视图，本类不做 JOIN、不做状态过滤；</li>
 *   <li>下单会连带触发 {@code order_insert}（orderway.count+1）与
 *       {@code order_insert_sale}（店铺销量+1），删除会触发对应的反向触发器，
 *       因此写库后如需最新统计值必须重新查询。</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    /** 与契约完全一致：datetime.now().strftime('%Y-%m-%d %H:%M:%S') */
    private static final DateTimeFormatter CREATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public void addOrder(String consPhone, OrderSubmitDTO dto) {
        Order order = Order.builder()
                .shopName(dto.getShopName())
                .orderMoney(dto.getOrderMoney())
                .orderWay(dto.getOrderWay())
                // 顾客手机号来自 JWT，不由前端传入（与契约一致）
                .consPhone(consPhone)
                .consName(dto.getConsName())
                .consAddre(dto.getConsAddre())
                .createTime(LocalDateTime.now().format(CREATE_TIME_FORMATTER))
                .build();
        orderMapper.insert(order);
        log.info("下单成功：shop={}, phone={}", order.getShopName(), order.getConsPhone());
    }

    @Override
    public List<UnsendOrderVO> listUnsend(String consPhone) {
        return orderMapper.listUnsendByPhone(consPhone);
    }

    @Override
    public List<SendingOrderVO> listSending(String consPhone) {
        return orderMapper.listSendingByPhone(consPhone);
    }

    @Override
    public List<SendingOrderVO> listSended(String consPhone) {
        return orderMapper.listSendedByPhone(consPhone);
    }

    @Override
    @Transactional
    public boolean updateUnsend(String consPhone, OrderUpdateDTO dto) {
        // 与 既有接口的差异：SQL 里多一个 cons_phone 谓词（越权修复）。
        // 返回受影响行数，让 Controller 能把"订单不存在/不属于你"如实告诉调用方，
        // 而不是无论改没改成都回"修改成功"。
        int rows = orderMapper.updateConsInfo(
                dto.getOrderId(), consPhone, dto.getConsName(), dto.getConsAddre());
        log.info("修改订单：orderId={}, phone={}, 影响行数={}", dto.getOrderId(), consPhone, rows);
        return rows > 0;
    }

    @Override
    @Transactional
    public boolean deleteOrder(String consPhone, Integer orderId) {
        // 删除后的统计/销量回退由数据库触发器负责；归属校验同时避免了
        // 越权删除误减他人店铺的 m_sale_v 与 orderway.count。
        int rows = orderMapper.deleteByOrderId(orderId, consPhone);
        log.info("取消订单：orderId={}, phone={}, 影响行数={}", orderId, consPhone, rows);
        return rows > 0;
    }
}
