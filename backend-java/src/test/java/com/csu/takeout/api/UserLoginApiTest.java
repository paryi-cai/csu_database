package com.csu.takeout.api;

import com.csu.takeout.CsuTakeoutApplication;
import com.csu.takeout.dto.JwtUserData;
import com.csu.takeout.properties.JwtProperties;
import com.csu.takeout.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 登录接口与 JWT 拦截器的端到端测试。
 *
 * <p>启动完整 Web 环境（随机端口）+ H2（MySQL 兼容模式），断言的是<b>响应体原文</b>，
 * 因此同时验证了：路径、请求参数名、响应字段名与类型、Flask 兼容的 JSON 排版、
 * JWT 载荷结构、以及拦截器的拦截范围。</p>
 *
 * <p>两点测试约定：</p>
 * <ul>
 *   <li>必须显式指定 {@code classes = CsuTakeoutApplication.class}：否则 Spring Boot 会把
 *       测试类自身当作 {@code @SpringBootConfiguration}，从测试类所在包开始扫描，
 *       导致 config/controller/handler 等生产组件全部缺失；</li>
 *   <li>期望的中文按 Flask（ensure_ascii=True）规则转义，用 {@link #esc(String)} 在运行时
 *       生成，避免 Java 编译器把源码中的反斜杠-u 当成自己的转义序列。</li>
 * </ul>
 */
@SpringBootTest(classes = CsuTakeoutApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2test")
class UserLoginApiTest {

    private static final String LOGIN_URL = "/api/user/login";
    private static final String PROTECTED_URL = "/api/user/usermsg";
    private static final String TOKEN_HEADER = "token";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtProperties jwtProperties;

    /** 管理员账号登录：响应为 code=200 + msg + token + role 四个平铺字段 */
    @Test
    @DisplayName("管理员登录成功：字段、类型、排版与 Flask 一致")
    void loginAsAdmin() {
        ResponseEntity<String> response = postJson(LOGIN_URL,
                "{\"userortel\": \"18682412818\", \"password\": \"Aa.460734909\"}");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.startsWith("{\"code\": 200, \"msg\": \"" + esc("登录成功") + "\", \"token\": \""),
                "响应前缀不符：" + body);
        assertTrue(body.endsWith("\", \"role\": 1}\n"), "响应结尾不符：" + body);
        // 整个响应体必须是纯 ASCII（Flask jsonify 的 ensure_ascii=True 行为）
        assertTrue(body.chars().allMatch(c -> c < 128), "响应体应不含非 ASCII 字符：" + body);

        // JWT 载荷结构与 Flask auth.py 保持一致
        String token = extractToken(body);
        Claims claims = JwtUtil.parseJWT(jwtProperties.getSecretKey(), jwtProperties.getIssuer(), token);
        assertEquals("wufang", claims.getIssuer());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());

        // Flask 的写法是 exp = now + 1 天、iat = now - 1 天，因此 exp - iat = 2 天
        long spanSeconds = Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant()).getSeconds();
        assertEquals(172800L, spanSeconds, "exp - iat 应为 2 天（与 Flask auth.py 一致）");
        // exp 距当前约 1 天
        long expDelta = Duration.between(Instant.now(), claims.getExpiration().toInstant()).getSeconds();
        assertTrue(Math.abs(expDelta - 86400L) < 120, "exp 应约为一天后，实际相差 " + expDelta + " 秒");
        // iat 为"一天前"，与 Flask 一致
        long iatAge = Duration.between(claims.getIssuedAt().toInstant(), Instant.now()).getSeconds();
        assertTrue(Math.abs(iatAge - 86400L) < 120, "iat 应为一天前，实际相差 " + iatAge + " 秒");

        JwtUserData userData = JwtUtil.getUserData(claims);
        assertNotNull(userData);
        assertEquals(2, userData.getId());
        assertEquals("CMX", userData.getUsername());
        assertEquals("Aa.460734909", userData.getPassword());
        assertEquals("18682412818", userData.getTelephone());
    }

    /** 普通用户登录：role 必须为 0 */
    @Test
    @DisplayName("普通用户登录成功：role 为 0")
    void loginAsUser() {
        ResponseEntity<String> response = postJson(LOGIN_URL,
                "{\"userortel\": \"18873190398\", \"password\": \"aA.460734909\"}");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().endsWith("\", \"role\": 0}\n"), "响应结尾不符：" + response.getBody());
    }

    /** 密码错误：HTTP 仍为 200，体为 code=1000 + 错误提示 */
    @Test
    @DisplayName("登录失败：HTTP 200 + code=1000，无 token/role 字段")
    void loginWithWrongPassword() {
        ResponseEntity<String> response = postJson(LOGIN_URL,
                "{\"userortel\": \"18682412818\", \"password\": \"wrong-password\"}");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"code\": 1000, \"msg\": \"" + esc("用户名或密码错误") + "\"}\n", response.getBody());
    }

    /** 登录接口本身不在拦截范围内（Flask 版同样不校验）：不带 token 也能正常返回 */
    @Test
    @DisplayName("登录接口不校验 token")
    void loginIsNotIntercepted() {
        ResponseEntity<String> response = postJson(LOGIN_URL,
                "{\"userortel\": \"18873190398\", \"password\": \"aA.460734909\"}");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    /**
     * 入参首尾空白必须被去掉。
     *
     * <p>Flask 的 app.py 写的是 {@code request.json.get("userortel").strip()}，
     * 即手机号与密码都会先去空白。若新后端不 trim，"带空格的账号密码"在 Flask 能登录、
     * 在新后端会失败——这是必须锁住的兼容点。</p>
     */
    @Test
    @DisplayName("登录：手机号与密码的首尾空格被忽略（对齐 Flask 的 .strip()）")
    void loginTrimsWhitespace() {
        ResponseEntity<String> response = postJson(LOGIN_URL,
                "{\"userortel\": \"  18873190398  \", \"password\": \"  aA.460734909  \"}");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().startsWith("{\"code\": 200, \"msg\": \"" + esc("登录成功") + "\""),
                "带首尾空格应能登录成功，实际：" + response.getBody());
    }

    /** 拦截器：未携带 token 的受保护接口返回 401（已确认按 sky-take-out 风格处理） */
    @Test
    @DisplayName("受保护接口缺少 token：返回 401")
    void protectedEndpointWithoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(PROTECTED_URL, String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("{\"code\": 1000, \"msg\": \"" + esc("用户未登录") + "\"}\n", response.getBody());
    }

    /** 拦截器：非法 token 返回 401 */
    @Test
    @DisplayName("受保护接口 token 非法：返回 401")
    void protectedEndpointWithBadToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(TOKEN_HEADER, "not-a-jwt");
        ResponseEntity<String> response = restTemplate.exchange(
                PROTECTED_URL, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("{\"code\": 1000, \"msg\": \"" + esc("用户未登录") + "\"}\n", response.getBody());
    }

    /**
     * 个人信息在 user_msg 无记录时的真实响应。
     *
     * <p>Flask 版此时会因 {@code data[0]} 越界抛 IndexError（返回 500）；
     * 新实现返回 {@code {"status": 200}}——{@code data} 为 null 时被
     * {@code @JsonInclude(NON_NULL)} 整个省略。本用例锁定该行为，
     * 避免文档与实现再次不一致。</p>
     *
     * <p>构造方式：直接签发一个"手机号在 user_msg 里不存在"的 token，
     * 不需要真的存在这样的用户。</p>
     */
    @Test
    @DisplayName("GET /api/user/usermsg：user_msg 无记录时返回 {\"status\": 200}")
    void userMsgWithoutRecord() {
        String token = JwtUtil.createJWT(jwtProperties.getSecretKey(), jwtProperties.getTtl(),
                jwtProperties.getIssuer(), JwtUserData.builder()
                        .id(999).username("ghost").password("x").telephone("13000000000").build());

        HttpHeaders headers = new HttpHeaders();
        headers.add(TOKEN_HEADER, token);
        ResponseEntity<String> response = restTemplate.exchange(
                PROTECTED_URL, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\": 200}\n", response.getBody());
    }

    /** 拦截器：签发者不匹配的 token 返回 401 */
    @Test
    @DisplayName("受保护接口 token 签发者非法：返回 401")
    void protectedEndpointWithWrongIssuer() {
        String token = JwtUtil.createJWT(jwtProperties.getSecretKey(), jwtProperties.getTtl(),
                "someone-else", JwtUserData.builder().id(3).username("YCX")
                        .password("aA.460734909").telephone("18873190398").build());
        HttpHeaders headers = new HttpHeaders();
        headers.add(TOKEN_HEADER, token);
        ResponseEntity<String> response = restTemplate.exchange(
                PROTECTED_URL, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    /** 拦截器：用错误密钥签发的 token 返回 401 */
    @Test
    @DisplayName("受保护接口 token 密钥错误：返回 401")
    void protectedEndpointWithWrongSecret() {
        String token = JwtUtil.createJWT("another-secret", jwtProperties.getTtl(), jwtProperties.getIssuer(),
                JwtUserData.builder().id(3).username("YCX").password("aA.460734909").telephone("18873190398").build());
        HttpHeaders headers = new HttpHeaders();
        headers.add(TOKEN_HEADER, token);
        ResponseEntity<String> response = restTemplate.exchange(
                PROTECTED_URL, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    /** 拦截器 + BaseContext：携带合法 token 时，业务层能拿到手机号 */
    @Test
    @DisplayName("受保护接口携带合法 token：放行并进入业务层（返回该用户个人信息）")
    void protectedEndpointWithValidToken() {
        String token = extractToken(postJson(LOGIN_URL,
                "{\"userortel\": \"18873190398\", \"password\": \"aA.460734909\"}").getBody());

        HttpHeaders headers = new HttpHeaders();
        headers.add(TOKEN_HEADER, token);
        ResponseEntity<String> response = restTemplate.exchange(
                PROTECTED_URL, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        // 能查到 YCX 的个人信息，说明拦截器已把手机号写入 BaseContext 且业务层正确使用
        assertNotNull(response.getBody());
        assertTrue(response.getBody().startsWith("{\"status\": 200, \"data\": {"), "响应：" + response.getBody());
        assertTrue(response.getBody().contains("\"user_name\": \"YCX\""), "响应：" + response.getBody());
    }

    private ResponseEntity<String> postJson(String url, String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(json, headers), String.class);
    }

    private String extractToken(String body) {
        assertNotNull(body, "响应体为空");
        int start = body.indexOf("\"token\": \"");
        assertTrue(start > 0, "响应中没有 token 字段：" + body);
        start += "\"token\": \"".length();
        int end = body.indexOf('"', start);
        return body.substring(start, end);
    }

    /**
     * 按 Flask（Python json.dumps ensure_ascii=True）规则转义非 ASCII 字符。
     *
     * <p>Jackson 使用大写十六进制（{@code \u767B}），Python 使用小写（{@code \u767b}）——
     * 语义等价，本项目采用 Jackson 的大写形式。这里运行时生成，避免源码里出现
     * 被 Java 编译器抢先解析的 unicode 转义。</p>
     */
    private static String esc(String text) {
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
