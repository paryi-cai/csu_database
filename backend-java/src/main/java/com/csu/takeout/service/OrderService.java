package com.csu.takeout.service;

import com.csu.takeout.dto.OrderSubmitDTO;
import com.csu.takeout.dto.OrderUpdateDTO;
import com.csu.takeout.vo.SendingOrderVO;
import com.csu.takeout.vo.UnsendOrderVO;

import java.util.List;

/**
 * 用户端订单业务接口
 */
public interface OrderService {

    /**
     * 下单
     *
     * @param consPhone 顾客手机号（取自 JWT 载荷）
     * @param dto       下单参数
     */
    void addOrder(String consPhone, OrderSubmitDTO dto);

    /**
     * 查询某用户的未派发订单
     */
    List<UnsendOrderVO> listUnsend(String consPhone);

    /**
     * 查询某用户的派送中订单（数据来源：数据库视图 sending_order）
     */
    List<SendingOrderVO> listSending(String consPhone);

    /**
     * 查询某用户的已送达订单（数据来源：数据库视图 sended_order）
     */
    List<SendingOrderVO> listSended(String consPhone);

    /**
     * 修改未派发订单的收货信息。
     *
     * <p>实现会校验订单归属（同时匹配 order_id 与 JWT 里的手机号），
     * 不能修改他人订单。</p>
     *
     * @param consPhone 顾客手机号（取自 JWT 载荷）
     * @param dto       修改参数
     * @return 是否确实修改了一行（false = 订单不存在或不属于该用户）
     */
    boolean updateUnsend(String consPhone, OrderUpdateDTO dto);

    /**
     * 删除（取消）订单。
     *
     * <p>同样校验归属；删除后的统计/销量回退由数据库触发器负责。</p>
     *
     * @param consPhone 顾客手机号（取自 JWT 载荷）
     * @param orderId   订单号
     * @return 是否确实删除了一行（false = 订单不存在或不属于该用户）
     */
    boolean deleteOrder(String consPhone, Integer orderId);
}
