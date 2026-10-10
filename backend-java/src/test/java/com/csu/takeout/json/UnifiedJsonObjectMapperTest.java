package com.csu.takeout.json;

import com.csu.takeout.result.Result;
import com.csu.takeout.result.TableResult;
import com.csu.takeout.vo.DispatcherIdVO;
import com.csu.takeout.vo.UserLoginVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Flask jsonify 兼容性测试。
 *
 * <p>断言的是"逐字节相等"：期望字符串就是 Flask 版 app.py 会产出的响应体原文。
 * 排版规则来自 Werkzeug 2.3 的 jsonify：</p>
 * <ul>
 *   <li>{@code json.dumps(..., ensure_ascii=True)}：中文等非 ASCII 字符输出为
 *       unicode 转义序列（本文件用 {@link #esc(String)} 在运行时生成，避免 Java 编译器
 *       把源码中的反斜杠-u 当成自己的转义）；</li>
 *   <li>{@code separators=(", ", ": ")}：逗号与冒号后各有一个空格；</li>
 *   <li>空集合输出 {@code []}；</li>
 *   <li>响应体末尾有一个换行。</li>
 * </ul>
 */
class FlaskJsonObjectMapperTest {

    private final FlaskJsonObjectMapper mapper = new FlaskJsonObjectMapper();

    /** Flask: jsonify(status=200, msg="成功下单") */
    @Test
    @DisplayName("单条提示：字段顺序、空格排版、中文转义均与 Flask 一致")
    void messageBody() throws Exception {
        TableResult<Object> body = TableResult.success("成功下单");
        assertEquals("{\"status\": 200, \"msg\": \"" + esc("成功下单") + "\"}\n", mapper.toFlaskJson(body));
    }

    /** Flask: jsonify({"code": 1000, "msg": "用户名或密码错误"}) */
    @Test
    @DisplayName("登录失败：code/msg 两字段")
    void loginFailedBody() throws Exception {
        Result<UserLoginVO> body = Result.error("用户名或密码错误");
        assertEquals("{\"code\": 1000, \"msg\": \"" + esc("用户名或密码错误") + "\"}\n", mapper.toFlaskJson(body));
    }

    /** Flask: jsonify({"code": 200, "msg": "登录成功", "token": ..., "role": 1}) */
    @Test
    @DisplayName("登录成功：code/msg/token/role 平铺在根层级")
    void loginSuccessBody() throws Exception {
        UserLoginVO vo = UserLoginVO.builder()
                .code(200).msg("登录成功").token("eyJhbGciOiJIUzI1NiJ9.abc.def").role(1).build();
        assertEquals("{\"code\": 200, \"msg\": \"" + esc("登录成功") + "\","
                        + " \"token\": \"eyJhbGciOiJIUzI1NiJ9.abc.def\", \"role\": 1}\n",
                mapper.toFlaskJson(vo));
    }

    /** 登录失败：token/role 为 null，必须不出现在响应体中 */
    @Test
    @DisplayName("登录失败：null 字段被忽略，只输出 code 与 msg")
    void loginFailedWithNullFields() throws Exception {
        UserLoginVO vo = UserLoginVO.builder().code(1000).msg("用户名或密码错误").build();
        assertEquals("{\"code\": 1000, \"msg\": \"" + esc("用户名或密码错误") + "\"}\n", mapper.toFlaskJson(vo));
    }

    /** 未登录：拦截器直接写出的 401 响应体 */
    @Test
    @DisplayName("401 响应体：code=1000 与未登录提示")
    void unauthorizedBody() throws Exception {
        assertEquals("{\"code\": 1000, \"msg\": \"" + esc("用户未登录") + "\"}\n",
                mapper.toFlaskJson(Result.error("用户未登录")));
    }

    /** Flask: jsonify(status=200, tabledata=[{...}]) —— 店铺列表，含中文、整数 */
    @Test
    @DisplayName("列表响应：tabledata 元素字段名与类型")
    void shopListBody() throws Exception {
        List<Map<String, Object>> data = new ArrayList<>();
        data.add(orderedMap("shop_name", "大美鸡公煲", "price", 15, "sale", 100));
        data.add(orderedMap("shop_name", "天美自选饭", "price", 10, "sale", 200));

        TableResult<Map<String, Object>> body = TableResult.success(data);
        assertEquals("{\"status\": 200, \"tabledata\": ["
                        + "{\"shop_name\": \"" + esc("大美鸡公煲") + "\", \"price\": 15, \"sale\": 100}, "
                        + "{\"shop_name\": \"" + esc("天美自选饭") + "\", \"price\": 10, \"sale\": 200}]}\n",
                mapper.toFlaskJson(body));
    }

    /** Flask 空列表输出为 []（无空格），本映射器必须复刻 */
    @Test
    @DisplayName("空列表：输出 [] 而不是 [ ]")
    void emptyListBody() throws Exception {
        TableResult<Object> body = TableResult.success(new ArrayList<>());
        assertEquals("{\"status\": 200, \"tabledata\": []}\n", mapper.toFlaskJson(body));
    }

    /** Flask: jsonify(status=200, tabledata=[...], disp_range=[{"disp_id": "010100"}]) */
    @Test
    @DisplayName("带候选项的列表响应：额外字段 disp_range")
    void listWithRangeBody() throws Exception {
        List<Map<String, Object>> data = new ArrayList<>();
        data.add(orderedMap("order_id", "1", "price", 12, "orderway", "人工订餐"));

        List<DispatcherIdVO> dispRange = List.of(
                new DispatcherIdVO("010100"),
                new DispatcherIdVO("010101"));

        TableResult<Map<String, Object>> body = TableResult.success(data);
        body.setDispRange(dispRange);

        assertEquals("{\"status\": 200, \"tabledata\": ["
                        + "{\"order_id\": \"1\", \"price\": 12, \"orderway\": \"" + esc("人工订餐") + "\"}], "
                        + "\"disp_range\": [{\"disp_id\": \"010100\"}, {\"disp_id\": \"010101\"}]}\n",
                mapper.toFlaskJson(body));
    }

    /** Flask: jsonify(status=200, data={...}) —— 个人信息，data 是对象且键顺序固定 */
    @Test
    @DisplayName("个人信息：data 为对象，键顺序与 Flask 字典插入顺序一致")
    void userMsgBody() throws Exception {
        Map<String, Object> data = orderedMap(
                "real_name", "ycx", "sex", "男", "age", 20,
                "mail", "787898@qq.com", "phone", "18873190398", "user_name", "YCX");

        String json = mapper.toFlaskJson(orderedMap("status", 200, "data", data));
        assertEquals("{\"status\": 200, \"data\": {\"real_name\": \"ycx\", \"sex\": \"" + esc("男")
                        + "\", \"age\": 20, \"mail\": \"787898@qq.com\", \"phone\": \"18873190398\","
                        + " \"user_name\": \"YCX\"}}\n",
                json);
    }

    /** 反斜杠、引号等特殊字符的转义规则必须与 Python json.dumps 一致 */
    @Test
    @DisplayName("特殊字符转义：反斜杠、引号、制表符")
    void specialCharacters() throws Exception {
        String value = "a" + (char) 92 + "b" + (char) 34 + "c" + (char) 9 + "d";
        assertEquals("{\"status\": 200, \"msg\": \"a" + (char) 92 + (char) 92 + "b" + (char) 92 + (char) 34
                        + "c" + (char) 92 + "td\"}\n",
                mapper.toFlaskJson(TableResult.success(value)));
    }

    /**
     * 单独校验非 ASCII 转义的形态：整个响应体为纯 ASCII，中文以 unicode 转义输出（Jackson 使用大写十六进制）
     * （与 Python json.dumps(ensure_ascii=True) 的字节输出一致）。
     */
    @Test
    @DisplayName("非 ASCII 转义：纯 ASCII 响应体 + unicode 转义")
    void nonAsciiUsesUnicodeEscape() throws Exception {
        String json = mapper.toFlaskJson(TableResult.success("登录成功"));
        assertEquals("{\"status\": 200, \"msg\": \"" + esc("登录成功") + "\"}\n", json);
        assertTrue(json.chars().allMatch(c -> c < 128), "响应体应只含 ASCII：" + json);
        // Jackson 使用大写十六进制；与小写形式语义等价（对比脚本会归一化）
        assertTrue(json.contains(esc("登")), "应包含 unicode 转义：" + json);
        assertFalse(json.contains(esc("登").toLowerCase().replace("\\u", "\\U")),
                "不应输出与预期不同的转义形式：" + json);
    }

    /** 转义后的 JSON 必须仍能被解析回原始中文，语义不变 */
    @Test
    @DisplayName("转义后仍可正确解析回中文")
    void escapedJsonRoundTrips() throws Exception {
        Map<String, Object> data = orderedMap("shop_name", "大美鸡公煲", "orderway", "人工订餐", "cons_name", "吴方");
        String json = mapper.toFlaskJson(TableResult.success(List.of(data)));

        Map<?, ?> parsed = mapper.fromJson(json);
        assertEquals(200, parsed.get("status"));
        List<?> rows = (List<?>) parsed.get("tabledata");
        assertEquals(1, rows.size());
        Map<?, ?> row = (Map<?, ?>) rows.get(0);
        assertEquals("大美鸡公煲", row.get("shop_name"));
        assertEquals("人工订餐", row.get("orderway"));
        assertEquals("吴方", row.get("cons_name"));
    }

    /** 保留插入顺序的 Map 构造工具（Jackson 对 Map 按插入顺序输出） */
    private Map<String, Object> orderedMap(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }

    /**
     * 按 Flask（Python json.dumps ensure_ascii=True）规则转义非 ASCII 字符。
     *
     * <p>Jackson 使用大写十六进制（{@code \u767B}），Python 使用小写（{@code \u767b}）——
     * 语义等价，本项目采用 Jackson 的大写形式。这里运行时生成，避免源码里出现
     * 被 Java 编译器抢先解析的 unicode 转义。</p>
     */
    static String esc(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c > 127) {
                sb.append((char) 92).append('u').append(String.format("%04X", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
