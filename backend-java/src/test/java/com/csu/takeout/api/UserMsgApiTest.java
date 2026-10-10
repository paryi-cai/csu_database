package com.csu.takeout.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 个人信息「查看 / 修改资料 / 修改密码」的接口契约测试。
 *
 * <p>覆盖三类行为：</p>
 * <ol>
 *   <li>查询：响应键与值（沿用 Flask 契约，data 为对象且不含 id）；</li>
 *   <li>修改资料：写库生效、{@code phone} 不可改、无记录时返回业务失败、未登录 401；</li>
 *   <li>修改密码：原密码错误 → 1000，原密码正确 → 200 且新密码能登录。</li>
 * </ol>
 *
 * <p><b>为什么改密码放在嵌套类里</b>：测试库是共享的（同一 Spring 上下文内多个测试类复用），
 * 改掉 YCX 的密码会让依赖 {@code aA.460734909} 登录的其它用例失败。
 * 嵌套类标了 AFTER_CLASS 的 {@link DirtiesContext}，执行完即丢弃上下文，
 * 后续测试类会拿到重新初始化过的干净库（库名带随机后缀，见 application-h2test.yml）。
 * 同理，"修改资料"用例改完数据后会在用例内自行还原。</p>
 */
class UserMsgApiTest extends ApiContractTestSupport {

    private static final String USERMSG = "/api/user/usermsg";
    private static final String USERMSG_EDIT = "/api/user/usermsg/edit";
    private static final String USERMSG_PWD = "/api/user/usermsg/pwd";

    /** data.sql 里 YCX（id=3）的明细，作为断言与还原的基准值 */
    private static final String YCX_REAL_NAME = "ycx";
    private static final String YCX_SEX = "男";
    private static final int YCX_AGE = 20;
    private static final String YCX_MAIL = "787898@qq.com";

    /* ==================== 1. 查询 ==================== */

    @Test
    @DisplayName("GET /api/user/usermsg：响应键为 Flask 契约的六个键，且不含 id")
    void getUserMsg() {
        String body = getAsUser(USERMSG);

        assertThat(body).startsWith("{\"status\": 200, \"data\": {");
        assertThat(body).contains("\"real_name\": \"" + esc(YCX_REAL_NAME) + "\"");
        assertThat(body).contains("\"sex\": \"" + esc(YCX_SEX) + "\"");
        assertThat(body).contains("\"age\": " + YCX_AGE);
        assertThat(body).contains("\"mail\": \"" + YCX_MAIL + "\"");
        assertThat(body).contains("\"phone\": \"" + YCX_PHONE + "\"");
        assertThat(body).contains("\"user_name\": \"YCX\"");
        assertThat(body).doesNotContain("\"id\"");
    }

    @Test
    @DisplayName("三个个人信息接口未携带 token 一律 401")
    void requiresToken() {
        ResponseEntity<String> get = restTemplate.getForEntity(USERMSG, String.class);
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get.getBody()).isEqualTo("{\"code\": 1000, \"msg\": \"" + esc("用户未登录") + "\"}\n");

        ResponseEntity<String> edit = putJson(USERMSG_EDIT, "{}");
        assertThat(edit.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(edit.getBody()).isEqualTo("{\"code\": 1000, \"msg\": \"" + esc("用户未登录") + "\"}\n");

        ResponseEntity<String> pwd = putJson(USERMSG_PWD, "{}");
        assertThat(pwd.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(pwd.getBody()).isEqualTo("{\"code\": 1000, \"msg\": \"" + esc("用户未登录") + "\"}\n");
    }

    /* ==================== 2. 修改资料 ==================== */

    @Test
    @DisplayName("PUT /api/user/usermsg/edit：五个字段都能改，且写库生效")
    void updateProfile() {
        String token = loginAsUser();
        String payload = "{"
                + "\"user_name\": \"小璇\", "
                + "\"real_name\": \"" + esc("杨楚璇") + "\", "
                + "\"sex\": \"" + esc("女") + "\", "
                + "\"age\": 22, "
                + "\"mail\": \"ycx@csu.edu.cn\"}";

        ResponseEntity<String> response = authorizedPut(USERMSG_EDIT, token, payload);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isEqualTo("{\"status\": 200, \"msg\": \"" + esc("修改成功") + "\"}\n");

        // 重新查询，确认真的落库（而不是只返回成功提示）
        String body = getAsUser(USERMSG);
        assertThat(body).contains("\"user_name\": \"" + esc("小璇") + "\"");
        assertThat(body).contains("\"real_name\": \"" + esc("杨楚璇") + "\"");
        assertThat(body).contains("\"sex\": \"" + esc("女") + "\"");
        assertThat(body).contains("\"age\": 22");
        assertThat(body).contains("\"mail\": \"ycx@csu.edu.cn\"");
        // 手机号不受影响
        assertThat(body).contains("\"phone\": \"" + YCX_PHONE + "\"");

        // 还原基准数据：本类不做上下文隔离，改了必须改回去
        restoreYcxProfile();
        assertThat(getAsUser(USERMSG)).contains("\"real_name\": \"" + esc(YCX_REAL_NAME) + "\"");
    }

    @Test
    @DisplayName("PUT /api/user/usermsg/edit：请求体没有任何可改字段时被提前拦下，不落到数据库约束")
    void updateProfileWithNothingToChange() {
        // user_msg 的 5 个列都是 NOT NULL，若直接把全 null 的 DTO 下发会撞约束、
        // 被全局兜底转成"未知错误"；控制器已提前拦截并给出明确原因。
        ResponseEntity<String> empty = authorizedPut(USERMSG_EDIT, loginAsUser(), "{}");
        assertThat(empty.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(empty.getBody())
                .isEqualTo("{\"status\": 1000, \"msg\": \"" + esc("没有需要修改的字段") + "\"}\n");

        // 数据未被破坏
        assertThat(getAsUser(USERMSG)).contains("\"real_name\": \"" + esc(YCX_REAL_NAME) + "\"");
    }

    @Test
    @DisplayName("PUT /api/user/usermsg/edit：只传部分字段时，其余字段用现有记录补齐（不全写 NULL）")
    void partialUpdateKeepsOtherFields() {
        String token = loginAsUser();
        int beforeAge = 20;
        try {
            // 只改年龄：其余 4 列必须保持原值，而不是被写成 NULL
            ResponseEntity<String> response = authorizedPut(USERMSG_EDIT, token, "{\"age\": 33}");
            assertThat(response.getBody())
                    .isEqualTo("{\"status\": 200, \"msg\": \"" + esc("修改成功") + "\"}\n");

            String body = getAsUser(USERMSG);
            assertThat(body).contains("\"age\": 33");
            assertThat(body).contains("\"user_name\": \"YCX\"");
            assertThat(body).contains("\"real_name\": \"" + esc(YCX_REAL_NAME) + "\"");
            assertThat(body).contains("\"sex\": \"" + esc(YCX_SEX) + "\"");
            assertThat(body).contains("\"mail\": \"" + YCX_MAIL + "\"");
        } finally {
            // 还原（本类不做上下文隔离）
            String restore = "{"
                    + "\"user_name\": \"YCX\", "
                    + "\"real_name\": \"" + esc(YCX_REAL_NAME) + "\", "
                    + "\"sex\": \"" + esc(YCX_SEX) + "\", "
                    + "\"age\": " + beforeAge + ", "
                    + "\"mail\": \"" + YCX_MAIL + "\"}";
            authorizedPut(USERMSG_EDIT, token, restore);
        }
        assertThat(getAsUser(USERMSG)).contains("\"age\": " + beforeAge);
    }

    @Test
    @DisplayName("PUT /api/user/usermsg/edit：把必填字段显式传成空白时给出明确提示，而不是数据库约束错误")
    void blankFieldRejected() {
        // 显式传空字符串（不是"未提供"）→ 明确拒绝；若放行会撞 NOT NULL 约束
        ResponseEntity<String> response = authorizedPut(USERMSG_EDIT, loginAsUser(),
                "{\"user_name\": \"  \", \"real_name\": \"ycx\", \"sex\": \"" + esc("男")
                        + "\", \"age\": 20, \"mail\": \"a@b.com\"}");
        assertThat(response.getBody())
                .isEqualTo("{\"status\": 1000, \"msg\": \""
                        + esc("用户名、真实姓名、性别、年龄、邮箱都不能为空") + "\"}\n");

        // 数据未被破坏
        assertThat(getAsUser(USERMSG)).contains("\"user_name\": \"YCX\"");
    }

    @Test
    @DisplayName("PUT /api/user/usermsg/edit：请求体里多带 phone 会被直接拒绝（联系电话只读）")
    void phoneIsNotEditable() {
        // 定位方案刻意做成"白名单"：UserMsgUpdateDTO 只声明 5 个可改字段，
        // Jackson 开着 FAIL_ON_UNKNOWN_PROPERTIES，多传任何未声明字段都会解析失败
        // → 全局异常处理器返回 {"status":1000,"msg":"未知错误"}。
        // 好处是"想改什么"与"能改什么"完全一致，不会出现"传了但被静默忽略"的错觉。
        String withPhone = "{"
                + "\"user_name\": \"YCX\", "
                + "\"real_name\": \"" + esc(YCX_REAL_NAME) + "\", "
                + "\"sex\": \"" + esc(YCX_SEX) + "\", "
                + "\"age\": " + YCX_AGE + ", "
                + "\"mail\": \"" + YCX_MAIL + "\", "
                + "\"phone\": \"13000000000\"}";
        ResponseEntity<String> rejected = authorizedPut(USERMSG_EDIT, loginAsUser(), withPhone);
        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rejected.getBody())
                .isEqualTo("{\"status\": 1000, \"msg\": \"" + esc("未知错误") + "\"}\n");

        // 只传 5 个合法字段时正常成功
        restoreYcxProfile();

        // 手机号未被改动：仍能按原号码查到记录，按伪造成的新号码查不到
        assertThat(getAsUser(USERMSG)).contains("\"phone\": \"" + YCX_PHONE + "\"");
        assertThat(authorizedGet(USERMSG, issueToken("13000000000")).getBody())
                .isEqualTo("{\"status\": 200}\n");
    }

    @Test
    @DisplayName("PUT /api/user/usermsg/edit：user_msg 无记录时返回业务失败而不是 500")
    void updateProfileWithoutRecord() {
        String payload = "{"
                + "\"user_name\": \"ghost\", "
                + "\"real_name\": \"" + esc("幽灵") + "\", "
                + "\"sex\": \"" + esc("男") + "\", "
                + "\"age\": 1, "
                + "\"mail\": \"ghost@x.com\"}";

        ResponseEntity<String> response = authorizedPut(USERMSG_EDIT, issueToken("13000000000"), payload);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isEqualTo("{\"status\": 1000, \"msg\": \"" + esc("用户信息不存在") + "\"}\n");
    }

    /* ==================== 3. 修改密码 ==================== */

    @Nested
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
    class ChangePassword {

        @Test
        @DisplayName("PUT /api/user/usermsg/pwd：原密码错误 → 1000；正确 → 200 且新密码可登录")
        void changePassword() {
            ResponseEntity<String> wrong = authorizedPut(USERMSG_PWD, loginAsUser(),
                    "{\"old_pwd\": \"wrong-pwd\", \"new_pwd\": \"newPwd123\"}");
            assertThat(wrong.getBody())
                    .isEqualTo("{\"status\": 1000, \"msg\": \"" + esc("原始密码错误") + "\"}\n");

            ResponseEntity<String> ok = authorizedPut(USERMSG_PWD, loginAsUser(),
                    "{\"old_pwd\": \"" + YCX_PASSWORD + "\", \"new_pwd\": \"newPwd123\"}");
            assertThat(ok.getBody())
                    .isEqualTo("{\"status\": 200, \"msg\": \"" + esc("修改成功") + "\"}\n");

            // 新密码能登录、旧密码不能
            assertThat(login(YCX_PHONE, "newPwd123")).isNotBlank();
            ResponseEntity<String> oldPwd = postJson("/api/user/login",
                    "{\"userortel\": \"" + YCX_PHONE + "\", \"password\": \"" + YCX_PASSWORD + "\"}");
            assertThat(oldPwd.getBody()).contains("\"code\": 1000");
        }

        @Test
        @DisplayName("PUT /api/user/usermsg/pwd 与既有 POST /api/user/pwd_chg 行为一致")
        void sameAsPwdChg() {
            String body = "{\"old_pwd\": \"not-the-pwd\", \"new_pwd\": \"x\"}";
            ResponseEntity<String> viaUsermsg = authorizedPut(USERMSG_PWD, loginAsUser(), body);
            ResponseEntity<String> viaPwdChg = authorizedPost("/api/user/pwd_chg", loginAsUser(), body);
            assertThat(viaUsermsg.getStatusCode()).isEqualTo(viaPwdChg.getStatusCode());
            assertThat(viaUsermsg.getBody()).isEqualTo(viaPwdChg.getBody());
        }
    }

    /* ==================== 工具方法 ==================== */

    /** 以 YCX 身份 GET，并断言 HTTP 200 */
    private String getAsUser(String url) {
        ResponseEntity<String> response = authorizedGet(url, loginAsUser());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    /** 把 YCX 的明细还原为 data.sql 的基准值（用例之间互相不污染） */
    private void restoreYcxProfile() {
        String payload = "{"
                + "\"user_name\": \"YCX\", "
                + "\"real_name\": \"" + esc(YCX_REAL_NAME) + "\", "
                + "\"sex\": \"" + esc(YCX_SEX) + "\", "
                + "\"age\": " + YCX_AGE + ", "
                + "\"mail\": \"" + YCX_MAIL + "\"}";
        ResponseEntity<String> response = authorizedPut(USERMSG_EDIT, loginAsUser(), payload);
        assertThat(response.getBody()).contains("\"status\": 200");
    }
}
