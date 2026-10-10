package com.csu.takeout.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 用户端「写操作」接口的契约测试（下单 / 改单 / 取消订单 / 改密）。
 *
 * <p>用例按 {@link Order} 顺序执行，并且整个类执行完后丢弃 Spring 上下文，
 * 让依赖初始数据的其它测试类拿到干净的数据库。</p>
 *
 * <p>重点验证<b>数据库副作用</b>：下单后店铺销量由触发器 {@code order_insert_sale} 自增、
 * 取消订单后由 {@code order_delete_sale} 回退——Java 侧不重复实现这些逻辑。</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserApiMutationTest extends ApiContractTestSupport {

    private static final DateTimeFormatter CREATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 第一个用例新增的订单号，供后续用例使用 */
    private static Integer createdOrderId;

    @Test
    @Order(1)
    @DisplayName("POST /api/user/addorder：返回成功提示，订单入库且 checked 取数据库默认值 0")
    void addOrder() {
        String token = loginAsUser();

        String before = LocalDateTime.now().format(CREATE_TIME_FORMATTER);
        ResponseEntity<String> response = authorizedPost("/api/user/addorder", token,
                "{\"shop_name\": \"" + esc("大美鸡公煲") + "\", \"order_money\": 15, "
                        + "\"order_way\": \"" + esc("人工订餐") + "\", \"cons_name\": \"" + esc("测试顾客")
                        + "\", \"cons_addre\": \"" + esc("20公寓") + "\"}");
        String after = LocalDateTime.now().format(CREATE_TIME_FORMATTER);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\": 200, \"msg\": \"" + esc("成功下单") + "\"}\n", response.getBody());

        // 订单已入库：未派发列表中应出现新建订单，create_time 为 yyyy-MM-dd HH:mm:ss 且落在调用区间内
        String unsend = authorizedGet("/api/user/unsend", token).getBody();
        assertTrue(unsend.contains("\"cons_name\": \"" + esc("测试顾客") + "\""), unsend);
        assertTrue(unsend.contains("\"cons_addre\": \"" + esc("20公寓") + "\""), unsend);
        assertTrue(unsend.contains("\"orderway\": \"" + esc("人工订餐") + "\""), unsend);
        assertTrue(unsend.contains("\"price\": 15"), unsend);

        createdOrderId = firstOrderIdOfNewOrder(unsend);

        // 新订单的 create_time 必须落在本次调用区间内（在"测试顾客"那段 JSON 内匹配）
        String newOrderRow = rowOf(unsend, esc("测试顾客"));
        Matcher timeMatcher = Pattern.compile("\"create_time\": \"([^\"]+)\"").matcher(newOrderRow);
        assertTrue(timeMatcher.find(), "找不到新建订单的 create_time：" + unsend);
        String createTime = timeMatcher.group(1);
        assertTrue(createTime.compareTo(before) >= 0 && createTime.compareTo(after) <= 0,
                "create_time " + createTime + " 不在 [" + before + ", " + after + "] 之间");

        // 验证数据库侧默认值：Java 的 insert 语句刻意不写 checked 列，
        // 订单能出现在"未派发"（checked = 0）列表中，说明默认值生效。
        // 说明：生产库中销量递增由触发器 order_insert_sale 完成，H2 无法表达
        // MySQL 的 begin...end 触发器体，该副作用需在真实 MySQL 上验证。
        assertTrue(unsend.contains("\"order_id\": " + createdOrderId), "新订单应处于未派发状态：" + unsend);
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/user/unsend：修改收货信息")
    void updateOrder() {
        String token = loginAsUser();

        ResponseEntity<String> response = authorizedPost("/api/user/unsend", token,
                "{\"order_id\": " + createdOrderId + ", \"cons_name\": \"" + esc("改名后")
                        + "\", \"cons_addre\": \"" + esc("21公寓") + "\"}");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\": 200, \"msg\": \"" + esc("修改成功") + "\"}\n", response.getBody());

        String unsend = authorizedGet("/api/user/unsend", token).getBody();
        assertTrue(unsend.contains("\"cons_name\": \"" + esc("改名后") + "\""), unsend);
        assertTrue(unsend.contains("\"cons_addre\": \"" + esc("21公寓") + "\""), unsend);
    }

    @Test
    @Order(3)
    @DisplayName("DELETE /api/user/unsend：取消订单后订单从列表消失")
    void deleteOrder() {
        String token = loginAsUser();

        ResponseEntity<String> response = authorizedDelete("/api/user/unsend", token,
                "{\"delete_id\": " + createdOrderId + "}");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\": 200, \"msg\": \"" + esc("删除成功") + "\"}\n", response.getBody());

        String unsend = authorizedGet("/api/user/unsend", token).getBody();
        assertFalse(unsend.contains("\"order_id\": " + createdOrderId), "订单应已被删除：" + unsend);

        // 说明：生产库中销量回退由触发器 order_delete_sale 完成，需在真实 MySQL 上验证。
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/user/pwd_chg：原密码错误与修改成功")
    void changePassword() {
        // 原密码错误
        ResponseEntity<String> wrong = authorizedPost("/api/user/pwd_chg", loginAsUser(),
                "{\"old_pwd\": \"wrong-password\", \"new_pwd\": \"aA.460734909\", \"check_pwd\": \"aA.460734909\"}");
        assertEquals(HttpStatus.OK, wrong.getStatusCode());
        assertEquals("{\"status\": 1000, \"msg\": \"" + esc("原始密码错误") + "\"}\n", wrong.getBody());

        // 修改成功后可用新密码登录（改回原密码，避免影响其它用例）
        ResponseEntity<String> ok = authorizedPost("/api/user/pwd_chg", loginAsUser(),
                "{\"old_pwd\": \"" + YCX_PASSWORD + "\", \"new_pwd\": \"aA.111111\", \"check_pwd\": \"aA.111111\"}");
        assertEquals(HttpStatus.OK, ok.getStatusCode());
        assertEquals("{\"status\": 200, \"msg\": \"" + esc("修改成功") + "\"}\n", ok.getBody());

        assertTrue(login(YCX_PHONE, "aA.111111").length() > 20, "应能用新密码登录");

        // 用新密码对应的 token 改回原密码
        ResponseEntity<String> restore = authorizedPost("/api/user/pwd_chg", login(YCX_PHONE, "aA.111111"),
                "{\"old_pwd\": \"aA.111111\", \"new_pwd\": \"" + YCX_PASSWORD + "\", \"check_pwd\": \""
                        + YCX_PASSWORD + "\"}");
        assertEquals("{\"status\": 200, \"msg\": \"" + esc("修改成功") + "\"}\n", restore.getBody());
        assertTrue(login(YCX_PHONE, YCX_PASSWORD).length() > 20, "应能用原密码登录");
    }

    @Test
    @Order(5)
    @DisplayName("越权修复：不能修改/删除他人订单（归属校验下推到 SQL）")
    void cannotTouchOthersOrder() {
        // 用 YCX 建一条自己的订单，然后换 CMX 的 token 去改它 / 删它。
        // 注意 CMX 是 role=1 的管理端账号，但 JWT 里同样带手机号，
        // 因此它在这里就是一个"另一个普通用户"，用来验证归属校验。
        String ownerToken = loginAsUser();
        authorizedPost("/api/user/addorder", ownerToken,
                "{\"shop_name\": \"" + esc("大美鸡公煲") + "\", \"order_money\": 15, "
                        + "\"order_way\": \"" + esc("人工订餐") + "\", \"cons_name\": \"" + esc("越权目标")
                        + "\", \"cons_addre\": \"" + esc("22公寓") + "\"}");

        String unsend = authorizedGet("/api/user/unsend", ownerToken).getBody();
        Integer orderId = newestOrderId(unsend);

        String intruderToken = issueToken(CMX_PHONE);

        // 1) 改：应被拒绝，且原值不变
        ResponseEntity<String> update = authorizedPost("/api/user/unsend", intruderToken,
                "{\"order_id\": " + orderId + ", \"cons_name\": \"" + esc("被篡改")
                        + "\", \"cons_addre\": \"" + esc("黑客公寓") + "\"}");
        assertEquals(HttpStatus.OK, update.getStatusCode());
        assertEquals("{\"status\": 1000, \"msg\": \"" + esc("订单不存在或不属于当前用户") + "\"}\n",
                update.getBody());

        String afterUpdate = authorizedGet("/api/user/unsend", ownerToken).getBody();
        assertTrue(afterUpdate.contains("\"order_id\": " + orderId), "订单应仍在：" + afterUpdate);
        assertFalse(afterUpdate.contains(esc("被篡改")), "不应被他人改写：" + afterUpdate);
        assertTrue(afterUpdate.contains("\"cons_name\": \"" + esc("越权目标") + "\""), afterUpdate);

        // 2) 删：应被拒绝，订单仍存在
        ResponseEntity<String> delete = authorizedDelete("/api/user/unsend", intruderToken,
                "{\"delete_id\": " + orderId + "}");
        assertEquals(HttpStatus.OK, delete.getStatusCode());
        assertEquals("{\"status\": 1000, \"msg\": \"" + esc("订单不存在或不属于当前用户") + "\"}\n",
                delete.getBody());
        assertTrue(authorizedGet("/api/user/unsend", ownerToken).getBody()
                .contains("\"order_id\": " + orderId), "订单不应被他人删除");

        // 3) 换回本人：应正常修改、正常删除（确认归属校验没有误伤正常路径）
        assertEquals("{\"status\": 200, \"msg\": \"" + esc("修改成功") + "\"}\n",
                authorizedPost("/api/user/unsend", ownerToken,
                        "{\"order_id\": " + orderId + ", \"cons_name\": \"" + esc("本人可改")
                                + "\", \"cons_addre\": \"" + esc("23公寓") + "\"}").getBody());
        assertEquals("{\"status\": 200, \"msg\": \"" + esc("删除成功") + "\"}\n",
                authorizedDelete("/api/user/unsend", ownerToken,
                        "{\"delete_id\": " + orderId + "}").getBody());
        assertFalse(authorizedGet("/api/user/unsend", ownerToken).getBody()
                .contains("\"order_id\": " + orderId), "本人应能删除");
    }

    @Test
    @Order(6)
    @DisplayName("改单/取消订单：订单号不存在时返回业务失败而不是「修改成功」")
    void unknownOrderId() {
        String token = loginAsUser();
        assertEquals("{\"status\": 1000, \"msg\": \"" + esc("订单不存在或不属于当前用户") + "\"}\n",
                authorizedPost("/api/user/unsend", token,
                        "{\"order_id\": 99999999, \"cons_name\": \"x\", \"cons_addre\": \"y\"}").getBody());
        assertEquals("{\"status\": 1000, \"msg\": \"" + esc("订单不存在或不属于当前用户") + "\"}\n",
                authorizedDelete("/api/user/unsend", token, "{\"delete_id\": 99999999}").getBody());
    }

    /** 取出"测试顾客"那条订单的 order_id */
    private Integer firstOrderIdOfNewOrder(String unsendBody) {
        String row = rowOf(unsendBody, esc("测试顾客"));
        Matcher matcher = Pattern.compile("\"order_id\": (\\d+)").matcher(row);
        if (!matcher.find()) {
            throw new AssertionError("找不到新建订单的 order_id：" + unsendBody);
        }
        return Integer.valueOf(matcher.group(1));
    }

    /**
     * 取出响应中最大的 order_id（= 最新插入的那条）。
     *
     * <p>用于"刚下完单"的场景：不依赖 cons_name 文本匹配，也不假设列表顺序，
     * 因为自增主键最大的那条必然是本次新建的订单。</p>
     */
    private Integer newestOrderId(String unsendBody) {
        Matcher matcher = Pattern.compile("\"order_id\": (\\d+)").matcher(unsendBody);
        int max = -1;
        while (matcher.find()) {
            max = Math.max(max, Integer.parseInt(matcher.group(1)));
        }
        if (max < 0) {
            throw new AssertionError("响应中没有任何 order_id：" + unsendBody);
        }
        return max;
    }

    /**
     * 从 JSON 数组响应中截取包含指定片段的那一条元素。
     *
     * <p>响应是"单行 + 空格分隔"的格式，元素之间以 ", {" 分隔，因此可以据此切分，
     * 避免正则跨元素误匹配。</p>
     */
    private String rowOf(String json, String fragment) {
        int index = json.indexOf(fragment);
        if (index < 0) {
            throw new AssertionError("响应中找不到片段 " + fragment + "：" + json);
        }
        int start = json.lastIndexOf(", {", index);
        start = (start < 0) ? json.indexOf('{', json.indexOf('[')) : start + 2;
        int end = json.indexOf('}', index);
        if (end < 0) {
            end = json.length();
        }
        return json.substring(start, end + 1);
    }
}
