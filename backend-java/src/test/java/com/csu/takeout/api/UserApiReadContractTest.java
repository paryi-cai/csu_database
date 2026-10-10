package com.csu.takeout.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 用户端「只读」接口的契约测试。
 *
 * <p>断言的是响应体原文（含空格排版、末尾换行、unicode 转义），数据来自 testdb/data.sql。
 * 这些用例不修改数据，可以共用一个 Spring 上下文；会产生数据变更的用例在
 * {@link UserApiMutationTest} 中单独执行。</p>
 */
class UserApiReadContractTest extends ApiContractTestSupport {

    /** Flask: jsonify(status=200, tabledata=[{shop_name, price, sale}]) */
    @Test
    @DisplayName("GET /api/user/shop：店铺列表字段与排版")
    void listShop() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/user/shop", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\": 200, \"tabledata\": ["
                        + "{\"shop_name\": \"" + esc("大美鸡公煲") + "\", \"price\": 15, \"sale\": 100}, "
                        + "{\"shop_name\": \"" + esc("天美自选饭") + "\", \"price\": 10, \"sale\": 200}]}\n",
                response.getBody());
    }

    /** 未派发订单：order_money 改为 price、order_way 改为 orderway */
    @Test
    @DisplayName("GET /api/user/unsend：字段改名（price / orderway）与内容")
    void listUnsend() {
        ResponseEntity<String> response = authorizedGet("/api/user/unsend", loginAsUser());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\": 200, \"tabledata\": ["
                        + "{\"order_id\": 1, \"shop_name\": \"" + esc("大美鸡公煲") + "\", \"price\": 15, "
                        + "\"orderway\": \"" + esc("人工订餐") + "\", \"cons_name\": \"" + esc("吴方") + "\", "
                        + "\"cons_addre\": \"" + esc("15公寓") + "\", \"create_time\": \"2022-12-16 14:35:17\"}]}\n",
                response.getBody());
    }

    /** 派送中订单：数据来自视图 sending_order，视图列 dispatcher_phone 输出为 disp_phone */
    @Test
    @DisplayName("GET /api/user/sending：来自视图 sending_order，含 disp_phone")
    void listSending() {
        ResponseEntity<String> response = authorizedGet("/api/user/sending", loginAsUser());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\": 200, \"tabledata\": ["
                        + "{\"order_id\": 2, \"shop_name\": \"" + esc("天美自选饭") + "\", "
                        + "\"order_money\": 10, \"order_way\": \"" + esc("人工订餐") + "\", "
                        + "\"cons_phone\": \"" + YCX_PHONE + "\", \"cons_name\": \"" + esc("吴方") + "\", "
                        + "\"cons_addre\": \"" + esc("8公寓") + "\", \"disp_id\": \"010100\", "
                        + "\"deliver_time\": \"20" + esc("分钟") + "\", \"disp_phone\": \"13365789765\"}]}\n",
                response.getBody());
    }

    /** 已送达订单：数据来自视图 sended_order */
    @Test
    @DisplayName("GET /api/user/sended：来自视图 sended_order，含 disp_phone")
    void listSended() {
        ResponseEntity<String> response = authorizedGet("/api/user/sended", loginAsUser());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\": 200, \"tabledata\": ["
                        + "{\"order_id\": 3, \"shop_name\": \"" + esc("大美鸡公煲") + "\", "
                        + "\"order_money\": 15, \"order_way\": \"" + esc("网上订餐") + "\", "
                        + "\"cons_phone\": \"" + YCX_PHONE + "\", \"cons_name\": \"" + esc("吴方") + "\", "
                        + "\"cons_addre\": \"" + esc("10公寓") + "\", \"disp_id\": \"010100\", "
                        + "\"deliver_time\": \"15" + esc("分钟") + "\", \"disp_phone\": \"13365789765\"}]}\n",
                response.getBody());
    }

    /** 个人信息：status + data（data 是对象），不含 id 字段 */
    @Test
    @DisplayName("GET /api/user/usermsg：data 为对象且字段名与 Flask 一致")
    void getUserMsg() {
        ResponseEntity<String> response = authorizedGet("/api/user/usermsg", loginAsUser());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\": 200, \"data\": {\"real_name\": \"ycx\", \"sex\": \"" + esc("男")
                        + "\", \"age\": 20, \"mail\": \"787898@qq.com\", \"phone\": \"" + YCX_PHONE
                        + "\", \"user_name\": \"YCX\"}}\n",
                response.getBody());
    }

    /** 该用户的订单为空时，tabledata 必须输出 []（无空格） */
    @Test
    @DisplayName("空列表：CMX 无订单，tabledata 输出 []")
    void emptyLists() {
        String token = login(CMX_PHONE, CMX_PASSWORD);

        assertEquals("{\"status\": 200, \"tabledata\": []}\n", authorizedGet("/api/user/unsend", token).getBody());
        assertEquals("{\"status\": 200, \"tabledata\": []}\n", authorizedGet("/api/user/sending", token).getBody());
        assertEquals("{\"status\": 200, \"tabledata\": []}\n", authorizedGet("/api/user/sended", token).getBody());
    }

    /** 受保护接口必须都要求 token */
    @Test
    @DisplayName("用户端受保护接口缺少 token 均返回 401")
    void protectedEndpointsRequireToken() {
        for (String url : new String[]{"/api/user/unsend", "/api/user/sending", "/api/user/sended",
                "/api/user/usermsg"}) {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), url + " 应要求 token");
        }
    }

    /** 店铺列表接口在 Flask 版不校验 token，这里保持一致 */
    @Test
    @DisplayName("GET /api/user/shop 不要求 token")
    void shopListDoesNotRequireToken() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/user/shop", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().startsWith("{\"status\": 200, \"tabledata\": ["), response.getBody());
    }
}
