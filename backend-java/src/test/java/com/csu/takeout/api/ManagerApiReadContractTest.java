package com.csu.takeout.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 管理端「只读」接口的契约测试。
 *
 * <p>既有接口管理端接口<b>不校验 token</b>，这里保持同样的行为（不带 token 直接调用）。</p>
 * <p>数据来自 testdb/data.sql；本类不修改数据。</p>
 *
 * <p><b>顺序上的一条真实约束</b>：本类与 {@link ManagerApiMutationTest} 共享同一个 Spring
 * 上下文（后者没有类级 {@code @DirtiesContext}），而那个类里的 {@code deleteShop} 会真的
 * 删掉一家店铺。因此<b>只能在本类中断言店铺列表</b>（例如 {@code listShop} 的逐字节比对），
 * 且不能把店铺列表断言放到会跑在 {@code ManagerApiMutationTest} 之后的地方。
 * 注意 {@code @Order} 只约束类内顺序，跨类顺序由测试框架的扫描顺序决定，不可依赖。</p>
 */
class ManagerApiReadContractTest extends ApiContractTestSupport {

    /** 店铺列表：与用户端 /api/user/shop 响应结构完全一致 */
    @Test
    @DisplayName("GET /api/manager/shop：店铺列表")
    void listShop() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/manager/shop", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\": 200, \"tabledata\": ["
                        + "{\"shop_name\": \"" + esc("大美鸡公煲") + "\", \"price\": 15, \"sale\": 100}, "
                        + "{\"shop_name\": \"" + esc("天美自选饭") + "\", \"price\": 10, \"sale\": 200}]}\n",
                response.getBody());
    }

    /** 服务员列表：额外返回 shop_range 供下拉选择 */
    @Test
    @DisplayName("GET /api/manager/server：列表 + shop_range")
    void listServer() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/manager/server", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\": 200, \"tabledata\": ["
                        + "{\"service_id\": \"100011\", \"service_name\": \"" + esc("虎哥") + "\", "
                        + "\"fastfood_shop_name\": \"" + esc("大美鸡公煲") + "\"}], "
                        + "\"shop_range\": [{\"shop_name\": \"" + esc("大美鸡公煲") + "\"}, "
                        + "{\"shop_name\": \"" + esc("天美自选饭") + "\"}]}\n",
                response.getBody());
    }

    /** 配送员列表 */
    @Test
    @DisplayName("GET /api/manager/dispatcher：配送员列表")
    void listDispatcher() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/manager/dispatcher", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\": 200, \"tabledata\": ["
                        + "{\"dispatcher_id\": \"010100\", \"dispatcher_name\": \"" + esc("摇摆羊")
                        + "\", \"dispatcher_phone\": \"13365789765\"}, "
                        + "{\"dispatcher_id\": \"010101\", \"dispatcher_name\": \"" + esc("小亮")
                        + "\", \"dispatcher_phone\": \"15878977898\"}]}\n",
                response.getBody());
    }

    /** 未派发订单：额外返回 disp_range（只含编号） */
    @Test
    @DisplayName("GET /api/manager/unsend：列表 + disp_range")
    void listUnsend() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/manager/unsend", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\": 200, \"tabledata\": ["
                        + "{\"order_id\": 1, \"shop_name\": \"" + esc("大美鸡公煲") + "\", \"price\": 15, "
                        + "\"orderway\": \"" + esc("人工订餐") + "\", \"cons_phone\": \"" + YCX_PHONE + "\", "
                        + "\"cons_name\": \"" + esc("吴方") + "\", \"cons_addre\": \"" + esc("15公寓")
                        + "\", \"create_time\": \"2022-12-16 14:35:17\"}], "
                        + "\"disp_range\": [{\"disp_id\": \"010100\"}, {\"disp_id\": \"010101\"}]}\n",
                response.getBody());
    }

    /** 派送中订单：来自视图，且不返回 disp_phone（与用户端不同） */
    @Test
    @DisplayName("GET /api/manager/sending：来自视图，无 disp_phone")
    void listSending() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/manager/sending", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\": 200, \"tabledata\": ["
                        + "{\"order_id\": 2, \"shop_name\": \"" + esc("天美自选饭") + "\", \"order_money\": 10, "
                        + "\"order_way\": \"" + esc("人工订餐") + "\", \"cons_phone\": \"" + YCX_PHONE + "\", "
                        + "\"cons_name\": \"" + esc("吴方") + "\", \"cons_addre\": \"" + esc("8公寓")
                        + "\", \"disp_id\": \"010100\", \"deliver_time\": \"20" + esc("分钟") + "\"}]}\n",
                response.getBody());
    }

    /** 已送达订单：来自视图，且不返回 disp_phone */
    @Test
    @DisplayName("GET /api/manager/sended：来自视图，无 disp_phone")
    void listSended() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/manager/sended", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\": 200, \"tabledata\": ["
                        + "{\"order_id\": 3, \"shop_name\": \"" + esc("大美鸡公煲") + "\", \"order_money\": 15, "
                        + "\"order_way\": \"" + esc("网上订餐") + "\", \"cons_phone\": \"" + YCX_PHONE + "\", "
                        + "\"cons_name\": \"" + esc("吴方") + "\", \"cons_addre\": \"" + esc("10公寓")
                        + "\", \"disp_id\": \"010100\", \"deliver_time\": \"15" + esc("分钟") + "\"}]}\n",
                response.getBody());
    }

    /** 物流：id=0 返回未结束；id=1 与缺省都返回已结束（既有行为） */
    @Test
    @DisplayName("GET /api/manager/wuliu：id=0 未结束，其余取值 已结束")
    void listWuliu() {
        assertEquals("{\"status\": 200, \"tabledata\": ["
                        + "{\"order_id\": 2, \"cons_phone\": \"" + YCX_PHONE + "\", \"disp_id\": \"010100\", "
                        + "\"deliver_time\": \"20" + esc("分钟") + "\"}]}\n",
                restTemplate.getForEntity("/api/manager/wuliu?id=0", String.class).getBody());

        String endedExpected = "{\"status\": 200, \"tabledata\": ["
                + "{\"order_id\": 3, \"cons_phone\": \"" + YCX_PHONE + "\", \"disp_id\": \"010100\", "
                + "\"deliver_time\": \"15" + esc("分钟") + "\"}]}\n";
        assertEquals(endedExpected, restTemplate.getForEntity("/api/manager/wuliu?id=1", String.class).getBody());
        // 不传 id 时 不传 id 时走默认分支，同样返回已结束记录
        assertEquals(endedExpected, restTemplate.getForEntity("/api/manager/wuliu", String.class).getBody());
        // 非 0 的任意值同样如此
        assertEquals(endedExpected, restTemplate.getForEntity("/api/manager/wuliu?id=abc", String.class).getBody());
    }

    /** 管理端接口在 既有接口不校验 token，这里保持一致 */
    @Test
    @DisplayName("管理端接口不要求 token")
    void managerEndpointsDoNotRequireToken() {
        for (String url : new String[]{"/api/manager/shop", "/api/manager/server", "/api/manager/dispatcher",
                "/api/manager/unsend", "/api/manager/sending", "/api/manager/sended", "/api/manager/wuliu?id=0"}) {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            assertEquals(HttpStatus.OK, response.getStatusCode(), url);
            assertTrue(response.getBody() != null && response.getBody().startsWith("{\"status\": 200"),
                    url + " -> " + response.getBody());
        }
    }
}
