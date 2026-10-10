package com.csu.takeout.controller.manager;

import com.csu.takeout.constant.MessageConstant;
import com.csu.takeout.result.DataResult;
import com.csu.takeout.service.ManagerStatsService;
import com.csu.takeout.vo.DispatcherWorkloadVO;
import com.csu.takeout.vo.StatsHourVO;
import com.csu.takeout.vo.StatsOverviewVO;
import com.csu.takeout.vo.StatsTrendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端 —— 数据统计（只读）。
 *
 * <p>响应形态统一为 {@code {"status": 200, "data": {...}}}（{@link DataResult}），
 * 与既有接口一致；失败时为 {@code {"status": 1000, "msg": "..."}}。</p>
 *
 * <p><b>与既有管理端接口的差别</b>：本组接口在
 * {@code WebMvcConfiguration.JWT_PATH_PATTERNS} 中注册了 JWT 校验
 * （其余管理端接口沿用 Flask 原样不校验，未做任何改动）。
 * 前端本来就在每个请求头携带 token，因此对现有页面零影响。</p>
 *
 * <p><b>只读保证</b>：全部为 GET + 聚合查询，不写任何表，也就不可能触发
 * {@code order_insert_sale} / {@code orderway} 等触发器的副作用。</p>
 */
@RestController
@RequestMapping("/api/manager/stats")
@Tag(name = "管理端-数据统计")
@Slf4j
@RequiredArgsConstructor
public class ManagerStatsController {

    private final ManagerStatsService managerStatsService;

    /**
     * 经营总览 KPI。
     *
     * <pre>
     * {"status": 200, "data": {"total_orders": 358, "total_revenue": 4720,
     *                          "shop_count": 8, "user_count": 2,
     *                          "unsend_count": 63, "sending_count": 76, "sended_count": 219}}
     * </pre>
     */
    @GetMapping("/overview")
    @Operation(summary = "经营总览", description = "总订单数 / 总营业额 / 店铺数 / 注册用户数 / 未派发 / 派送中 / 已送达")
    public DataResult<StatsOverviewVO> overview() {
        return DataResult.success(managerStatsService.overview());
    }

    /**
     * 订单量与营业额走势（按天聚合，服务端补零）。
     *
     * <pre>
     * {"status": 200, "data": {"days": 7, "dates": ["2026-10-04", ...],
     *                          "order_counts": [33, ...], "revenues": [486, ...],
     *                          "order_total": 243, "revenue_total": 3580,
     *                          "order_avg": 34.7, "revenue_avg": 511.4}}
     * </pre>
     *
     * @param days 统计天数，默认 7；服务端收敛到 [1, 90]
     */
    @GetMapping("/trend")
    @Operation(summary = "订单量与营业额走势", description = "days 默认 7，服务端收敛到 [1,90]；无订单的日期补零")
    public DataResult<StatsTrendVO> trend(
            @Parameter(description = "统计天数，默认 7") @RequestParam(value = "days", required = false) Integer days) {
        int range = days == null ? 7 : days;
        return DataResult.success(managerStatsService.trend(range));
    }

    /**
     * 单店铺走势（Top 榜点击下钻）。
     *
     * <p>返回类型为 {@code Map} 而非 {@code DataResult}：店铺不存在时需要返回
     * {@code {"status": 1000, "msg": "该店铺不存在"}} 这种带 msg 的业务失败结构，
     * 而 {@code DataResult} 只有 status/data 两个键（其定义以 /api/user/usermsg 为准，
     * 不为了统计接口去改它）。</p>
     */
    @GetMapping("/shop/trend")
    @Operation(summary = "单店铺走势", description = "店铺不存在时返回 status=1000")
    public Map<String, Object> shopTrend(
            @Parameter(description = "店铺名") @RequestParam(value = "shop_name", required = false) String shopName,
            @Parameter(description = "统计天数，默认 7") @RequestParam(value = "days", required = false) Integer days) {

        int range = days == null ? 7 : days;
        StatsTrendVO trend = managerStatsService.shopTrend(shopName, range);

        Map<String, Object> body = new LinkedHashMap<>();
        if (trend == null) {
            log.info("店铺下钻：店铺不存在或店铺名为空，shopName={}", shopName);
            body.put("status", 1000);
            body.put("msg", MessageConstant.SHOP_NOT_FOUND);
            return body;
        }
        body.put("status", 200);
        body.put("data", trend);
        return body;
    }

    /**
     * 订单状态分布（checked 0/1/2）。
     *
     * <pre>
     * {"status": 200, "data": {"items": [{"status": 0, "label": "待派单", "count": 63}, ...],
     *                          "total": 358}}
     * </pre>
     */
    @GetMapping("/status")
    @Operation(summary = "订单状态分布", description = "checked = 0 / 1 / 2 三种状态的数量")
    public DataResult<Map<String, Object>> status() {
        return DataResult.success(managerStatsService.status());
    }

    /**
     * 店铺业绩榜。
     *
     * @param limit  取前 N 条，默认 8，服务端收敛到 [1, 20]
     * @param metric 排序口径：money（营业额，默认）/ count（订单量）
     */
    @GetMapping("/shops")
    @Operation(summary = "店铺业绩榜", description = "metric=money|count，limit 默认 8，收敛到 [1,20]")
    public DataResult<Map<String, Object>> shops(
            @Parameter(description = "取前 N 条，默认 8") @RequestParam(value = "limit", required = false) Integer limit,
            @Parameter(description = "排序口径 money|count") @RequestParam(value = "metric", required = false) String metric) {
        return DataResult.success(managerStatsService.shops(limit == null ? 8 : limit, metric));
    }

    /**
     * 订餐方式构成（直接读 orderway 表，count 由触发器维护）。
     *
     * <pre>
     * {"status": 200, "data": {"items": [{"name": "人工订餐", "count": 214, "ratio": 61.5}, ...],
     *                          "total": 347}}
     * </pre>
     */
    @GetMapping("/orderway")
    @Operation(summary = "订餐方式构成", description = "读 orderway 表，不重算触发器维护的 count")
    public DataResult<Map<String, Object>> orderWay() {
        return DataResult.success(managerStatsService.orderWay());
    }

    /**
     * 配送员工作量对比（wuliu 按 disp_id 聚合 + dispatcher 信息）。
     */
    @GetMapping("/dispatchers")
    @Operation(summary = "配送员工作量对比", description = "left join 保证未接单的配送员也在列表中（数量为 0）")
    public DataResult<List<DispatcherWorkloadVO>> dispatchers() {
        return DataResult.success(managerStatsService.dispatchers());
    }

    /**
     * 下单时段分布（固定 0–23 共 24 个桶）。
     */
    @GetMapping("/hours")
    @Operation(summary = "下单时段分布", description = "按 create_time 的小时部分聚合，输出 24 个桶与峰值小时")
    public DataResult<StatsHourVO> hours() {
        return DataResult.success(managerStatsService.hours());
    }
}
