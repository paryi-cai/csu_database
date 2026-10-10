package com.csu.takeout.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 管理端数据统计接口契约测试（H2 MySQL 兼容模式）。
 *
 * <p>覆盖点：</p>
 * <ol>
 *   <li>8 个统计接口的响应结构与 Flask 兼容排版（含末尾换行、非 ASCII 转义）；</li>
 *   <li>JWT 校验：统计接口未携带 token 必须 401（既有管理端接口的行为不受影响）；</li>
 *   <li>日期/小时补零：区间内没有订单的日期要出现在序列里，小时的桶固定 24 个；</li>
 *   <li>参数收敛与非法入参（days 越界、店铺不存在、limit 越界）；</li>
 *   <li>只读性：统计接口不写库（连查两次结果一致、订单数不变）。</li>
 * </ol>
 *
 * <p><b>为什么在用例内插数据而不是写进 data.sql</b>：
 * {@code data.sql} 里的 3 条订单是既有列表类契约测试<b>逐字节断言</b>的依赖，
 * 而统计接口按 [today-(days-1), today] 聚合，需要有订单落在"最近几天"这个窗口内。
 * 两者冲突，因此在每个用例前用 {@link JdbcTemplate} 插入一条<b>当天</b>的订单
 * （order_id = 100，不占用既有编号），用例后删除，既有夹具保持一行不改。</p>
 *
 * <p><b>为什么本类结束后要 @DirtiesContext</b>：同层的写操作测试类
 * （如 {@code UserApiMutationTest}）会新增订单，Spring 的上下文缓存按"配置"共享，
 * 于是本类的断言会看到被污染的订单数。标了 AFTER_CLASS 之后，本类结束时丢弃上下文，
 * 后续测试类拿到的是重新初始化过的干净库（库名带随机后缀，见 application-h2test.yml），
 * 从而保持"逐字节断言"类测试的数据前提。</p>
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ManagerStatsApiTest extends ApiContractTestSupport {

    private static final String STATS_OVERVIEW = "/api/manager/stats/overview";
    private static final String STATS_TREND = "/api/manager/stats/trend";
    private static final String STATS_STATUS = "/api/manager/stats/status";
    private static final String STATS_SHOPS = "/api/manager/stats/shops";
    private static final String STATS_SHOP_TREND = "/api/manager/stats/shop/trend";
    private static final String STATS_ORDERWAY = "/api/manager/stats/orderway";
    private static final String STATS_DISPATCHERS = "/api/manager/stats/dispatchers";
    private static final String STATS_HOURS = "/api/manager/stats/hours";

    /** 当天订单的编号与金额：不占用 data.sql 的 1..3 */
    private static final int TODAY_ORDER_ID = 100;
    private static final int TODAY_ORDER_MONEY = 20;
    private static final String TODAY_ORDER_PHONE = "13525188888";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 每个用例前插入一条"当天 14:20:05"的未派发订单（店铺：大美鸡公煲）。
     *
     * <p>累积效果（data.sql 的 3 条 + 本条的 1 条）：</p>
     * <pre>
     * 订单总数 4、总营业额 60
     * 未派发 2 条（order1 + 本条）、派送中 1 条、已送达 1 条
     * 大美鸡公煲 50 元 3 单（15+15+20）、天美自选饭 10 元 1 单
     * 下单时段：14 点 2 单（本条的 14:20:05 + order1 的 14:35:17）为峰值
     * 7 天走势：只有当天 1 单 20 元（另外两条 2022-12-16 在窗口外）
     * </pre>
     */
    @BeforeEach
    void insertTodayOrder() {
        jdbcTemplate.update("delete from oorder where order_id = ?", TODAY_ORDER_ID);
        jdbcTemplate.update(
                "insert into oorder (order_id, shop_name, order_money, order_way, cons_phone, "
                        + "cons_name, cons_addre, checked, create_time) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                TODAY_ORDER_ID, "大美鸡公煲", TODAY_ORDER_MONEY, "人工订餐", TODAY_ORDER_PHONE,
                "老三", "15公寓", 0, LocalDate.now() + " 14:20:05");
    }

    /* ==================== 1. 鉴权 ==================== */

    @Test
    @DisplayName("统计接口未携带 token：401 + 用户未登录（既有管理端接口不受影响）")
    void statsRequireToken() {
        String[] urls = {STATS_OVERVIEW, STATS_TREND, STATS_STATUS, STATS_SHOPS,
                STATS_ORDERWAY, STATS_DISPATCHERS, STATS_HOURS};

        for (String url : urls) {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            assertThat(response.getStatusCode()).as(url).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).as(url).isEqualTo("{\"code\": 1000, \"msg\": \"" + esc("用户未登录") + "\"}\n");
        }
    }

    @Test
    @DisplayName("既有管理端接口仍然不校验 token（零回归）")
    void existingManagerApiStillOpen() {
        ResponseEntity<String> shop = restTemplate.getForEntity("/api/manager/shop", String.class);
        assertThat(shop.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(shop.getBody()).contains("\"status\": 200");
    }

    /* ==================== 2. 总览 ==================== */

    @Test
    @DisplayName("overview：总订单数/营业额/店铺数/用户数/未派发/派送中/已送达")
    void overview() {
        String body = get(STATS_OVERVIEW);

        // 3 条（data.sql）+ 1 条（当天）= 4 条；金额 15+10+15+20 = 60
        assertThat(body).contains("\"total_orders\": 4");
        assertThat(body).contains("\"total_revenue\": 60");
        assertThat(body).contains("\"shop_count\": 2");
        assertThat(body).contains("\"user_count\": 2");
        // 未派发 2 条（data.sql 的 order1 + 本条）；派送中 / 已送达走数据库视图，各 1 条
        assertThat(body).contains("\"unsend_count\": 2");
        assertThat(body).contains("\"sending_count\": 1");
        assertThat(body).contains("\"sended_count\": 1");
        assertThat(body).endsWith("}\n");
    }

    /* ==================== 3. 走势与补零 ==================== */

    @Test
    @DisplayName("trend：默认 7 天，日期序列连续，窗口外的历史订单不计入")
    void trendFillsMissingDays() {
        String body = get(STATS_TREND);

        assertThat(body).contains("\"days\": 7");
        // 前三条订单是 2022-12-16（远在窗口外），只有当天那条落在窗口内
        assertThat(body).contains("\"order_counts\": [0, 0, 0, 0, 0, 0, 1]");
        assertThat(body).contains("\"revenues\": [0, 0, 0, 0, 0, 0, 20]");
        assertThat(body).contains("\"order_total\": 1");
        assertThat(body).contains("\"revenue_total\": 20");
        // 日均按"窗口天数"分摊（区间已补零，长度恒为 days）：20 ÷ 7 = 2.857 -> 2.9
        assertThat(body).contains("\"order_avg\": 0.1");
        assertThat(body).contains("\"revenue_avg\": 2.9");
    }

    @Test
    @DisplayName("trend：days 越界时服务端收敛到 [1,90]")
    void trendClampsDays() {
        assertThat(get(STATS_TREND + "?days=0")).contains("\"days\": 1");
        assertThat(get(STATS_TREND + "?days=-5")).contains("\"days\": 1");
        assertThat(get(STATS_TREND + "?days=1000")).contains("\"days\": 90");
        assertThat(get(STATS_TREND + "?days=30")).contains("\"days\": 30");
        // 30 天窗口仍然覆盖不到 2022-12-16 的历史订单
        assertThat(get(STATS_TREND + "?days=30")).contains("\"order_total\": 1");
    }

    /* ==================== 4. 状态分布 ==================== */

    @Test
    @DisplayName("status：三种状态齐全，label 与前端标签一致")
    void statusDistribution() {
        String body = get(STATS_STATUS);

        assertThat(body).contains("\"status\": 0");
        assertThat(body).contains("\"label\": \"" + esc("待派单") + "\"");
        assertThat(body).contains("\"label\": \"" + esc("配送中") + "\"");
        assertThat(body).contains("\"label\": \"" + esc("已完成") + "\"");
        assertThat(body).contains("\"count\": 1");
        assertThat(body).contains("\"total\": 4");
    }

    /* ==================== 5. 店铺榜 ==================== */

    @Test
    @DisplayName("shops：按营业额排序、rank 连续、带单价与 m_sale_v")
    void shopRank() {
        String body = get(STATS_SHOPS);

        assertThat(body).contains("\"metric\": \"money\"");
        assertThat(body).contains("\"unit\": \"" + esc("元") + "\"");
        // 大美鸡公煲 15+15+20=50 元 3 单；天美自选饭 10 元 1 单
        assertThat(body).contains("\"shop_name\": \"" + esc("大美鸡公煲") + "\"");
        assertThat(body).contains("\"order_count\": 3");
        assertThat(body).contains("\"revenue\": 50");
        assertThat(body).contains("\"rank\": 1");
        assertThat(body).contains("\"rank\": 2");
        assertThat(body).contains("\"revenue\": 10");
    }

    @Test
    @DisplayName("shops：metric=count 时改按订单量排序，非法 metric 回落为 money")
    void shopRankMetric() {
        assertThat(get(STATS_SHOPS + "?metric=count")).contains("\"metric\": \"count\"");
        assertThat(get(STATS_SHOPS + "?metric=count")).contains("\"unit\": \"" + esc("单") + "\"");
        assertThat(get(STATS_SHOPS + "?metric=whatever")).contains("\"metric\": \"money\"");
    }

    @Test
    @DisplayName("shops：limit 越界收敛到 [1,20]")
    void shopRankLimit() {
        assertThat(get(STATS_SHOPS + "?limit=0")).contains("\"rank\": 1");
        assertThat(get(STATS_SHOPS + "?limit=0")).doesNotContain("\"rank\": 2");
        assertThat(get(STATS_SHOPS + "?limit=999")).contains("\"rank\": 2");
    }

    /* ==================== 6. 店铺下钻 ==================== */

    @Test
    @DisplayName("shop/trend：存在时返回店铺名 + 走势；不存在时 status=1000")
    void shopTrend() {
        // 直接传中文，由 TestRestTemplate 负责百分号编码；
        // 若自行 URLEncoder.encode 再交给它，百分号会被二次编码（% -> %25），服务端解不回来。
        String ok = get(STATS_SHOP_TREND + "?shop_name=大美鸡公煲&days=7");
        assertThat(ok).contains("\"status\": 200");
        assertThat(ok).contains("\"shop_name\": \"" + esc("大美鸡公煲") + "\"");
        assertThat(ok).contains("\"order_total\": 1");
        assertThat(ok).contains("\"revenue_total\": 20");

        String notFound = get(STATS_SHOP_TREND + "?shop_name=不存在的店");
        assertThat(notFound).isEqualTo("{\"status\": 1000, \"msg\": \"" + esc("该店铺不存在") + "\"}\n");

        String blank = get(STATS_SHOP_TREND);
        assertThat(blank).contains("\"status\": 1000");
    }

    /* ==================== 7. 订餐方式（读 orderway 表） ==================== */

    @Test
    @DisplayName("orderway：直接读表，ratio 按合计计算")
    void orderWay() {
        String body = get(STATS_ORDERWAY);

        // 测试数据：人工订餐 108、网上订餐 51，合计 159 -> 67.9% / 32.1%
        assertThat(body).contains("\"name\": \"" + esc("人工订餐") + "\"");
        assertThat(body).contains("\"count\": 108");
        assertThat(body).contains("\"ratio\": 67.9");
        assertThat(body).contains("\"count\": 51");
        assertThat(body).contains("\"ratio\": 32.1");
        assertThat(body).contains("\"total\": 159");
    }

    /* ==================== 8. 配送员工作量 ==================== */

    @Test
    @DisplayName("dispatchers：left join 保证没派过单的配送员也在列表里（数量 0）")
    void dispatcherWorkload() {
        String body = get(STATS_DISPATCHERS);

        // 010100 摇摆羊：wuliu 两条（ended=0 一条、ended=1 一条）
        assertThat(body).contains("\"disp_id\": \"010100\"");
        assertThat(body).contains("\"sending_count\": 1");
        assertThat(body).contains("\"sended_count\": 1");
        assertThat(body).contains("\"total\": 2");
        // 010101 小亮没有任何配送记录，仍应出现
        assertThat(body).contains("\"disp_id\": \"010101\"");
        assertThat(body).contains("\"total\": 0");
    }

    /* ==================== 9. 时段分布 ==================== */

    @Test
    @DisplayName("hours：固定 24 个桶，峰值小时正确")
    void hourDistribution() {
        String body = get(STATS_HOURS);

        // 下单时间：14:35、14:35（历史两条）+ 14:20（当天）-> 14 点 3 单为峰值（15 点 1 单）
        assertThat(body).contains("\"peak_hour\": 14");
        assertThat(body).contains("\"peak_count\": 3");
        assertThat(body).contains("\"hours\": [0, 1, 2");
        assertThat(body).contains("23]");
        assertThat(countOccurrences(body, "\"hours\": [")).isEqualTo(1);
    }

    /* ==================== 10. 只读性 ==================== */

    @Test
    @DisplayName("只读：连续调用两次统计接口，结果完全一致；统计不改变订单数据")
    void statsAreReadOnly() {
        String first = get(STATS_OVERVIEW);
        String second = get(STATS_OVERVIEW);
        assertThat(second).isEqualTo(first);

        // 统计接口只读：再查一次店铺榜与趋势，金额与条数保持用例开始时的状态
        assertThat(get(STATS_SHOPS)).contains("\"revenue\": 50");
        assertThat(get(STATS_STATUS)).contains("\"total\": 4");
        assertThat(get(STATS_TREND)).contains("\"order_total\": 1");

        Integer orders = jdbcTemplate.queryForObject("select count(*) from oorder", Integer.class);
        assertThat(orders).isEqualTo(4);
    }

    /* ==================== 工具方法 ==================== */

    private String get(String url) {
        ResponseEntity<String> response = authorizedGet(url, loginAsUser());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private static int countOccurrences(String text, String token) {
        int count = 0;
        int index = text.indexOf(token);
        while (index >= 0) {
            count++;
            index = text.indexOf(token, index + token.length());
        }
        return count;
    }

    private static String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
