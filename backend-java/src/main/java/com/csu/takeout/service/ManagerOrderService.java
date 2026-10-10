package com.csu.takeout.service;

import com.csu.takeout.dto.OrderDispatchDTO;
import com.csu.takeout.vo.ManagerOrderVO;
import com.csu.takeout.vo.ManagerUnsendOrderVO;
import com.csu.takeout.vo.WuliuVO;

import java.util.List;

/**
 * 管理端订单与物流业务接口
 */
public interface ManagerOrderService {

    /** 查询全部未派发订单 */
    List<ManagerUnsendOrderVO> listUnsend();

    /** 查询全部派送中订单 */
    List<ManagerOrderVO> listSending();

    /** 查询全部已送达订单 */
    List<ManagerOrderVO> listSended();

    /**
     * 派发订单：写入物流记录
     *
     * <p>订单状态 checked 由数据库触发器 {@code wuliu_insert} 置为 1，本方法不做该更新。</p>
     */
    void dispatch(OrderDispatchDTO dto);

    /**
     * 按是否结束查询物流记录
     *
     * @param ended 0 进行中、1 已完成
     */
    List<WuliuVO> listWuliu(Integer ended);
}
