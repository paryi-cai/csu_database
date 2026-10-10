package com.csu.takeout.service;

import com.csu.takeout.vo.DispatcherWorkloadVO;
import com.csu.takeout.vo.ShopRankVO;
import com.csu.takeout.vo.StatsHourVO;
import com.csu.takeout.vo.StatsOverviewVO;
import com.csu.takeout.vo.StatsTrendVO;

import java.util.List;
import java.util.Map;

/**
 * 管理端数据统计业务接口（全部为只读查询）。
 *
 * <p>返回 {@code Map} 的两个方法（状态分布、订餐方式）需要额外的比率计算，
 * 且字段随维度而异，用 Map 组装后交给统一的 JSON 序列化器输出，
 * 避免为两三个字段各造一个一次性 VO。</p>
 */
public interface ManagerStatsService {

    /** 经营总览 KPI */
    StatsOverviewVO overview();

    /**
     * 订单量 / 营业额走势
     *
     * @param days 统计天数（服务端会做区间收敛，见实现）
     */
    StatsTrendVO trend(int days);

    /**
     * 单店铺走势（Top 榜下钻）
     *
     * @return 店铺不存在时返回 null
     */
    StatsTrendVO shopTrend(String shopName, int days);

    /** 订单状态分布：items 元素为 {status, label, count} */
    Map<String, Object> status();

    /** 店铺业绩榜 */
    Map<String, Object> shops(int limit, String metric);

    /** 订餐方式构成：items 元素为 {name, count, ratio} */
    Map<String, Object> orderWay();

    /** 配送员工作量对比 */
    List<DispatcherWorkloadVO> dispatchers();

    /** 下单时段分布（24 个桶） */
    StatsHourVO hours();
}
