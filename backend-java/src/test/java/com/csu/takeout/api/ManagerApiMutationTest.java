package com.csu.takeout.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 管理端「写操作」接口的契约测试（店铺 / 服务员 / 配送员）。
 *
 * <p>关于隔离方式：这里<b>不能</b>用 {@code @Transactional} 做回滚。
 * 测试方法的事务与内嵌容器的 HTTP 请求线程使用的是不同连接，请求看不到未提交的数据
 * （实测表现为 {@code {"code":1000,"msg":"未知错误"}} 与 NullPointerException）。
 * 因此改为"每个用例自行清理数据"，并把会破坏初始数据的派发用例隔离到
 * {@link DispatchOrder} 嵌套类。</p>
 *
 * <p><b>类级 {@code @DirtiesContext} 是必需的，不是保守起见</b>：本类无法把数据改回原状
 * （例如 {@code deleteShop} 删掉的店铺补不回 {@code m_sale_v}——H2 测试库没有维护该计数的
 * 触发器），而 {@code ManagerApiReadContractTest} 对店铺列表做的是<b>逐字节</b>断言。
 * 两个类之间没有类级隔离时，"读契约能否通过"就取决于测试框架的扫描顺序——
 * 那是实现细节，不是保证。加上类级隔离后，无论谁先跑，后续类都拿到干净的夹具。</p>
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ManagerApiMutationTest extends ApiContractTestSupport {

    private static final String NEW_SHOP = "测试新店铺";

    /** deleteShop 用：自建的"被服务员引用"的店铺与服务员编号（不用夹具数据，用完即清） */
    private static final String REFERENCED_SHOP = "测试被引用店铺";
    private static final String REFERENCED_SERVICE_ID = "100098";

    /** 新增店铺 → 列表可见；重复新增 → 提示已存在 */
    @Test
    @DisplayName("POST /api/manager/shop?action=add：新增与重复新增")
    void addShop() {
        ResponseEntity<String> ok = postJson("/api/manager/shop",
                "{\"action\": \"add\", \"shop_name\": \"" + esc(NEW_SHOP)
                        + "\", \"price\": \"18\", \"m_sale_v\": \"66\"}");
        assertEquals(HttpStatus.OK, ok.getStatusCode());
        assertEquals("{\"status\": 200, \"msg\": \"" + esc("添加成功") + "\"}\n", ok.getBody());

        String list = restTemplate.getForEntity("/api/manager/shop", String.class).getBody();
        assertTrue(list != null && list.contains("\"shop_name\": \"" + esc(NEW_SHOP)
                + "\", \"price\": 18, \"sale\": 66"), "新店铺未出现在列表中：" + list);

        ResponseEntity<String> again = postJson("/api/manager/shop",
                "{\"action\": \"add\", \"shop_name\": \"" + esc(NEW_SHOP)
                        + "\", \"price\": \"18\", \"m_sale_v\": \"66\"}");
        assertEquals("{\"status\": 1000, \"msg\": \"" + esc("该店铺已存在") + "\"}\n", again.getBody());
    }

    /** 修改店铺 → 价格与销量变化（action=change） */
    @Test
    @DisplayName("POST /api/manager/shop?action=change：修改价格与销量")
    void changeShop() {
        ResponseEntity<String> response = postJson("/api/manager/shop",                "{\"action\": \"change\", \"shop_name\": \"" + esc("大美鸡公煲")
                        + "\", \"price\": \"20\", \"m_sale_v\": \"123\"}");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\": 200, \"msg\": \"" + esc("修改成功") + "\"}\n", response.getBody());

        String list = restTemplate.getForEntity("/api/manager/shop", String.class).getBody();
        assertTrue(list != null && list.contains("\"shop_name\": \"" + esc("大美鸡公煲")
                + "\", \"price\": 20, \"sale\": 123"), "修改未生效：" + list);
    }

    /**
     * 删除店铺 → 列表消失；<b>被服务员引用的店铺同样删得掉</b>（生产库没有那个外键）。
     *
     * <p>生产库（{@code database/init.sql}）的 {@code server} 表<b>没有</b>指向
     * {@code fastfood_shop} 的外键（实测 information_schema 只有 {@code user_msg.id → user.id}
     * 一条），所以删店铺不会被引用阻止，并会留下 {@code server} 里指向不存在店铺的孤儿行。
     * 夹具曾自造这个外键，使测试锁住了一个生产上不成立的行为；现已与生产对齐。</p>
     *
     * <p><b>为什么自建被引用的店铺，而不是删夹具里的店铺</b>：夹具里确实有一条
     * {@code 100011 虎哥 → 大美鸡公煲}，但直接删它有两个问题：
     * ① 会永久破坏夹具——{@code ManagerApiReadContractTest#listShop} 对店铺列表做<b>逐字节</b>
     * 断言（含 {@code "sale": 100}），而 H2 测试库没有维护 {@code m_sale_v} 的触发器，
     * 删掉就补不回原销量；两个类共享 Spring 上下文时，"读契约能否通过"就取决于测试框架的
     * 扫描顺序（实现细节，不是保证）；
     * ② 夹具里只有那一条 {@code server} 记录，删了它"服务员变孤儿"就没法验证了。
     * 因此这里自己造一对「店铺 + 引用它的服务员」，验证完即可全部清干净，<b>不依赖任何跨类顺序</b>。</p>
     *
     * <p>该孤儿行是既有的遗留行为，未在本次改造中修复（修复会改变既有接口语义），
     * 已登记在 README 的「已知差异」。</p>
     */
    @Test
    @DisplayName("DELETE /api/manager/shop：删除成功，且被服务员引用的店铺不被阻止（生产无外键）")
    void deleteShop() {
        // 1) 不被引用的店铺：新增后删除成功
        postJson("/api/manager/shop",
                "{\"action\": \"add\", \"shop_name\": \"" + esc(NEW_SHOP)
                        + "\", \"price\": \"18\", \"m_sale_v\": \"66\"}");
        ResponseEntity<String> deleted = deleteJson("/api/manager/shop", "{\"want_delete\": \"" + esc(NEW_SHOP) + "\"}");
        assertEquals(HttpStatus.OK, deleted.getStatusCode());
        assertEquals("{\"status\": 200, \"msg\": \"" + esc("删除成功") + "\"}\n", deleted.getBody());

        String list = restTemplate.getForEntity("/api/manager/shop", String.class).getBody();
        assertFalse(list != null && list.contains(NEW_SHOP), "店铺应已被删除：" + list);

        // 2) 造一家"被服务员引用"的店铺：店铺 + 指向它的服务员
        postJson("/api/manager/shop",
                "{\"action\": \"add\", \"shop_name\": \"" + esc(REFERENCED_SHOP)
                        + "\", \"price\": \"12\", \"m_sale_v\": \"3\"}");
        ResponseEntity<String> serverAdded = postJson("/api/manager/server",
                "{\"service_id\": \"" + REFERENCED_SERVICE_ID + "\", \"service_name\": \"" + esc("测试服务员")
                        + "\", \"fastfood_shop_name\": \"" + esc(REFERENCED_SHOP) + "\"}");
        assertEquals("{\"status\": 200, \"msg\": \"" + esc("添加成功") + "\"}\n", serverAdded.getBody());

        // 3) 删除这家被引用的店铺：生产无外键 → 删除成功
        ResponseEntity<String> referenced = deleteJson("/api/manager/shop",
                "{\"want_delete\": \"" + esc(REFERENCED_SHOP) + "\"}");
        assertEquals(HttpStatus.OK, referenced.getStatusCode());
        assertEquals("{\"status\": 200, \"msg\": \"" + esc("删除成功") + "\"}\n", referenced.getBody());

        String after = restTemplate.getForEntity("/api/manager/shop", String.class).getBody();
        assertFalse(after != null && after.contains(esc(REFERENCED_SHOP)), "店铺应已被删除：" + after);

        // 4) 证实孤儿行：被删店铺已不在 shop_range 里，但服务员仍指向它
        String serverList = restTemplate.getForEntity("/api/manager/server", String.class).getBody();
        assertTrue(serverList != null
                        && serverList.contains("\"fastfood_shop_name\": \"" + esc(REFERENCED_SHOP) + "\""),
                "应留下指向已删除店铺的服务员行（孤儿行）：" + serverList);
        assertFalse(serverList.contains("\"shop_name\": \"" + esc(REFERENCED_SHOP) + "\""),
                "已删除的店铺不应再出现在 shop_range 候选里：" + serverList);

        // 5) 清理自建的服务员，保持夹具与其它用例干净
        ResponseEntity<String> serverDeleted = deleteJson("/api/manager/server",
                "{\"want_delete\": \"" + REFERENCED_SERVICE_ID + "\"}");
        assertEquals(HttpStatus.OK, serverDeleted.getStatusCode());
        assertFalse(restTemplate.getForEntity("/api/manager/server", String.class).getBody()
                .contains(REFERENCED_SERVICE_ID), "自建服务员应已被清理");
    }



    /** 服务员：新增 → 列表可见；重复新增 → 已存在；解雇 → 列表消失 */
    @Test
    @DisplayName("POST/DELETE /api/manager/server：新增、重复、解雇")
    void serverCrud() {
        ResponseEntity<String> ok = postJson("/api/manager/server",
                "{\"service_id\": \"100099\", \"service_name\": \"" + esc("测试服务员")
                        + "\", \"fastfood_shop_name\": \"" + esc("天美自选饭") + "\"}");
        assertEquals("{\"status\": 200, \"msg\": \"" + esc("添加成功") + "\"}\n", ok.getBody());

        String list = restTemplate.getForEntity("/api/manager/server", String.class).getBody();
        assertTrue(list != null && list.contains("\"service_id\": \"100099\""), "新增服务员未出现：" + list);

        ResponseEntity<String> again = postJson("/api/manager/server",
                "{\"service_id\": \"100099\", \"service_name\": \"" + esc("测试服务员")
                        + "\", \"fastfood_shop_name\": \"" + esc("天美自选饭") + "\"}");
        assertEquals("{\"status\": 1000, \"msg\": \"" + esc("该编号已存在") + "\"}\n", again.getBody());

        ResponseEntity<String> deleted = deleteJson("/api/manager/server", "{\"want_delete\": \"100099\"}");
        assertEquals("{\"status\": 200, \"msg\": \"" + esc("解雇成功") + "\"}\n", deleted.getBody());

        String after = restTemplate.getForEntity("/api/manager/server", String.class).getBody();
        assertFalse(after != null && after.contains("100099"), "服务员应已被解雇：" + after);
    }

    /** 配送员：新增 → 列表可见；重复新增 → 已存在；解雇 → 列表消失 */
    @Test
    @DisplayName("POST/DELETE /api/manager/dispatcher：新增、重复、解雇")
    void dispatcherCrud() {
        ResponseEntity<String> ok = postJson("/api/manager/dispatcher",
                "{\"dispatcher_id\": \"010199\", \"dispatcher_name\": \"" + esc("测试配送员")
                        + "\", \"dispatcher_phone\": \"13900000000\"}");
        assertEquals("{\"status\": 200, \"msg\": \"" + esc("添加成功") + "\"}\n", ok.getBody());

        String list = restTemplate.getForEntity("/api/manager/dispatcher", String.class).getBody();
        assertTrue(list != null && list.contains("\"dispatcher_id\": \"010199\""), "新增配送员未出现：" + list);

        ResponseEntity<String> again = postJson("/api/manager/dispatcher",
                "{\"dispatcher_id\": \"010199\", \"dispatcher_name\": \"" + esc("测试配送员")
                        + "\", \"dispatcher_phone\": \"13900000000\"}");
        assertEquals("{\"status\": 1000, \"msg\": \"" + esc("该编号已存在") + "\"}\n", again.getBody());

        ResponseEntity<String> deleted = deleteJson("/api/manager/dispatcher", "{\"want_delete\": \"010199\"}");
        assertEquals("{\"status\": 200, \"msg\": \"" + esc("解雇成功") + "\"}\n", deleted.getBody());

        String after = restTemplate.getForEntity("/api/manager/dispatcher", String.class).getBody();
        assertFalse(after != null && after.contains("010199"), "配送员应已被解雇：" + after);
    }

    /**
     * 派发订单会新增物流记录，（在真实 MySQL 上）还会把订单移出未派发列表，
     * 会破坏只读测试依赖的初始数据，因此单独放在这个嵌套类里，
     * 并在整个嵌套类执行完后丢弃 Spring 上下文。
     */
    @Nested
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
    class DispatchOrder {

        /** 派发订单：写入物流记录后，该订单出现在物流查询中 */
        @Test
        @DisplayName("POST /api/manager/unsend：派发订单并写入物流记录")
        void dispatchOrder() {
            // 订单 1（YCX 的未派发订单）
            ResponseEntity<String> response = postJson("/api/manager/unsend",
                    "{\"order_id\": 1, \"dispatcher_id\": \"010101\", \"deliver_time\": \"" + esc("30分钟") + "\"}");
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("{\"status\": 200, \"msg\": \"" + esc("成功派发") + "\"}\n", response.getBody());

            // 物流记录已写入（ended 默认 0，因此出现在 id=0 的查询里）
            String wuliu = restTemplate.getForEntity("/api/manager/wuliu?id=0", String.class).getBody();
            assertTrue(wuliu != null && wuliu.contains("\"order_id\": 1, \"cons_phone\": \"" + YCX_PHONE
                    + "\", \"disp_id\": \"010101\", \"deliver_time\": \"" + esc("30分钟") + "\""),
                    "物流记录未写入：" + wuliu);

            // 说明：订单状态 checked 由触发器 wuliu_insert 置为 1，
            // 该触发器无法在 H2 上重建，需在真实 MySQL 上验证"订单从未派发列表消失"。
        }
    }
}
