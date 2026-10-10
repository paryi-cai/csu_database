package com.csu.takeout.mapper;

import com.csu.takeout.entity.Order;
import com.csu.takeout.vo.ManagerOrderVO;
import com.csu.takeout.vo.ManagerUnsendOrderVO;
import com.csu.takeout.vo.SendingOrderVO;
import com.csu.takeout.vo.UnsendOrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单相关持久层
 *
 * <p>重要：所有涉及订单状态的查询都直接依赖数据库侧的定义——</p>
 * <ul>
 *   <li>{@code checked = 0} 的未派发订单直接查 {@code oorder} 表；</li>
 *   <li>派送中 / 已送达订单查数据库<b>视图</b> {@code sending_order} / {@code sended_order}
 *       （视图内部已包含 {@code checked = 1/2} 条件与三表连接），
 *       Java 侧<b>不做</b>任何 JOIN 或状态过滤。</li>
 * </ul>
 */
@Mapper
public interface OrderMapper {

    /**
     * 查询某用户的未派发订单（对应 Flask 版
     * {@code select * from oorder where checked=0 and cons_phone=?}）
     *
     * @param consPhone 顾客手机号（取自 JWT）
     * @return 订单列表，元素键为 order_id / shop_name / price / orderway / cons_name / cons_addre / create_time
     */
    List<UnsendOrderVO> listUnsendByPhone(@Param("consPhone") String consPhone);

    /**
     * 查询某用户的派送中订单（直接查视图 sending_order）
     */
    List<SendingOrderVO> listSendingByPhone(@Param("consPhone") String consPhone);

    /**
     * 查询某用户的已送达订单（直接查视图 sended_order）
     */
    List<SendingOrderVO> listSendedByPhone(@Param("consPhone") String consPhone);

    /**
     * 新增订单
     *
     * <p>只插入业务字段，{@code checked} 由数据库默认值 0 填充；
     * 订单数量统计与销量递增由数据库触发器完成，此处不处理。</p>
     *
     * @param order 订单（createTime 由业务层按 Flask 的 yyyy-MM-dd HH:mm:ss 生成）
     * @return 影响行数
     */
    int insert(Order order);

    /**
     * 修改未派发订单的收货信息。
     *
     * <p>SQL 同时匹配 {@code order_id} 与 {@code consPhone}，即只允许改自己的订单
     * （Flask 版只按 order_id，存在越权；此处补上归属条件）。</p>
     *
     * @return 受影响行数；0 表示订单不存在**或不属于该用户**
     */
    int updateConsInfo(@Param("orderId") Integer orderId,
                       @Param("consPhone") String consPhone,
                       @Param("consName") String consName,
                       @Param("consAddre") String consAddre);

    /**
     * 删除订单（对应 Flask 版 {@code delete from oorder where order_id=?}，另加归属校验）。
     *
     * <p>删除后由数据库触发器 {@code order_delete} / {@code order_delete_sale}
     * 自动维护 orderway.count 与 fastfood_shop.m_sale_v；因此归属校验也顺带避免了
     * 越权删除误减他人店铺的销量。</p>
     *
     * @return 受影响行数；0 表示订单不存在**或不属于该用户**
     */
    int deleteByOrderId(@Param("orderId") Integer orderId, @Param("consPhone") String consPhone);

    /* ==================== 管理端 ==================== */

    /**
     * 查询全部未派发订单（对应 Flask 版 {@code select * from oorder where checked=0}）
     */
    List<ManagerUnsendOrderVO> listAllUnsend();

    /**
     * 查询全部派送中订单（直接查视图 sending_order）
     */
    List<ManagerOrderVO> listAllSending();

    /**
     * 查询全部已送达订单（直接查视图 sended_order）
     */
    List<ManagerOrderVO> listAllSended();

    /**
     * 按订单号查询顾客手机号（派发订单时用于填充 wuliu.cons_phone，
     * 与 Flask 版 {@code select cons_phone from oorder where order_id=?} 一致）
     */
    String getConsPhoneByOrderId(@Param("orderId") Integer orderId);
}
