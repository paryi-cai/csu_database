package com.csu.takeout.mapper;

import com.csu.takeout.vo.DispatcherWorkloadVO;
import com.csu.takeout.vo.OrderSumVO;
import com.csu.takeout.vo.ShopRankVO;
import com.csu.takeout.vo.StatsCountVO;
import com.csu.takeout.vo.StatsDailyRowVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 管理端数据统计持久层（只读）。
 *
 * <p>职责边界（与项目既有约定保持一致）：</p>
 * <ul>
 *   <li>「派送中 / 已送达」的判定一律交给数据库视图 {@code sending_order} /
 *       {@code sended_order}，Java 侧不写 JOIN、不做 checked 过滤；</li>
 *   <li>「订餐方式构成」直接读 {@code orderway} 表——它的 {@code count} 由触发器
 *       order_insert / order_update / order_delete 维护，这里<b>不重算</b>；</li>
 *   <li>「店铺销量」读 {@code fastfood_shop.m_sale_v}，同样由触发器维护，不重算；</li>
 *   <li>本接口全部为 SELECT，不触碰任何写路径与触发器副作用。</li>
 * </ul>
 *
 * <p><b>SQL 可移植性</b>：契约测试跑在 H2 1.4.200 的 MySQL 兼容模式上，
 * 因此这里只用两边都支持的标准写法，并且刻意绕开了两个 H2 上不可用的写法：</p>
 * <ul>
 *   <li>日期聚合用 {@code substring(create_time, 1, 10)}，<b>不用</b> {@code date_format(...)}
 *       ——后者在 H2 1.4.200 上没有实现，会让契约测试覆盖不到这段 SQL；</li>
 *   <li>小时分桶用 {@code hour(create_time)}，但 {@code group by} 里必须重复
 *       <b>整个表达式</b>而不是只写别名（H2 把它当聚合函数，只写别名会报
 *       "Column create_time must be in the GROUP BY list"）；</li>
 *   <li>时间范围过滤用字符串比较（{@code create_time} 本身就是 varchar，
 *       格式固定为 {@code yyyy-MM-dd HH:mm:ss}，字典序与时间序一致）。</li>
 * </ul>
 * <p>具体写法与踩坑记录见 {@code ManagerStatsMapper.xml} 内对应 {@code <select>} 的注释。</p>
 */
@Mapper
public interface ManagerStatsMapper {

    /** 店铺数 */
    long countShops();

    /** 注册用户数 */
    long countUsers();

    /** 订单总量与总营业额（含未派发） */
    OrderSumVO sumOrders();

    /** 未派发订单数（checked = 0，直接查 oorder） */
    long countUnsend();

    /** 派送中订单数（视图 sending_order） */
    long countSending();

    /** 已送达订单数（视图 sended_order） */
    long countSended();

    /**
     * 按天聚合的订单量与营业额。
     *
     * @param beginDate 起始日期（含），yyyy-MM-dd
     * @param shopName  店铺名；为 null 时统计全部店铺
     */
    List<StatsDailyRowVO> listDailyTrend(@Param("beginDate") String beginDate,
                                         @Param("shopName") String shopName);

    /** 订单状态分布（checked 0/1/2 各自数量） */
    List<StatsCountVO> listStatusCount();

    /**
     * 店铺业绩榜。
     *
     * @param limit  取前 N 条
     * @param metric 排序口径：revenue（营业额）或 orderCount（订单量）
     */
    List<ShopRankVO> listShopRank(@Param("limit") int limit, @Param("metric") String metric);

    /** 单店铺的单价与销量（下钻时用于校验店铺是否存在并补齐静态信息） */
    ShopRankVO getShopBase(@Param("shopName") String shopName);

    /** 订餐方式构成（直接读 orderway 表） */
    List<StatsCountVO> listOrderWay();

    /** 配送员工作量（wuliu 按 disp_id 聚合 + dispatcher 信息） */
    List<DispatcherWorkloadVO> listDispatcherWorkload();

    /** 下单时段分布（按 create_time 的小时部分聚合） */
    List<StatsCountVO> listHourCount();
}
