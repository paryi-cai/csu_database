package com.csu.takeout.api;

import com.csu.takeout.CsuTakeoutApplication;
import com.csu.takeout.dto.JwtUserData;
import com.csu.takeout.properties.JwtProperties;
import com.csu.takeout.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * 接口契约测试基类。
 *
 * <p>提供两件事：</p>
 * <ul>
 *   <li>{@link #login}：调用真实登录接口拿 token（顺便验证登录链路）；</li>
 *   <li>{@link #esc}：按 Flask（Python json.dumps ensure_ascii=True）规则生成 unicode 转义，
 *       用于断言<b>响应体原文</b>。运行时生成可避免 Java 编译器抢先解析源码里的反斜杠-u。</li>
 * </ul>
 *
 * <p>注意 {@code classes = CsuTakeoutApplication.class} 是必须的：否则 Spring Boot 会把测试类
 * 自身当作 {@code @SpringBootConfiguration}，导致生产组件全部扫描不到。</p>
 */
@SpringBootTest(classes = CsuTakeoutApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2test")
abstract class ApiContractTestSupport {

    static final String TOKEN_HEADER = "token";
    static final String YCX_PHONE = "18873190398";
    static final String YCX_PASSWORD = "aA.460734909";
    static final String CMX_PHONE = "18682412818";
    static final String CMX_PASSWORD = "Aa.460734909";

    @Autowired
    protected TestRestTemplate restTemplate;

    /** JWT 配置（签发测试用 token 时需要密钥/签发者） */
    @Autowired
    protected JwtProperties jwtProperties;

    /** 登录并返回 JWT */
    protected String login(String telephone, String password) {
        ResponseEntity<String> response = postJson("/api/user/login",
                "{\"userortel\": \"" + telephone + "\", \"password\": \"" + password + "\"}");
        String body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("登录失败，响应体为空");
        }
        int start = body.indexOf("\"token\": \"");
        if (start < 0) {
            throw new IllegalStateException("登录响应中没有 token：" + body);
        }
        start += "\"token\": \"".length();
        return body.substring(start, body.indexOf('"', start));
    }

    /** 登录普通用户 YCX */
    protected String loginAsUser() {
        return login(YCX_PHONE, YCX_PASSWORD);
    }

    protected ResponseEntity<String> postJson(String url, String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(json, headers), String.class);
    }

    protected ResponseEntity<String> authorizedPost(String url, String token, String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(TOKEN_HEADER, token);
        return restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(json, headers), String.class);
    }

    protected ResponseEntity<String> authorizedGet(String url, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(TOKEN_HEADER, token);
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    protected ResponseEntity<String> authorizedDelete(String url, String token, String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(TOKEN_HEADER, token);
        return restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(json, headers), String.class);
    }

    /**
     * 带请求体的 DELETE，且不携带 token。
     *
     * <p>管理端接口在 Flask 版不校验 token，因此这里不加 token 头
     * （若加 {@code null} 值会触发 HttpClient 的 NullPointerException）。</p>
     */
    protected ResponseEntity<String> deleteJson(String url, String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(json, headers), String.class);
    }

    /** 带请求体与 token 的 PUT（个人信息编辑类接口使用） */
    protected ResponseEntity<String> authorizedPut(String url, String token, String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(TOKEN_HEADER, token);
        return restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(json, headers), String.class);
    }

    /** 不带 token 的 PUT（用于验证鉴权拦截） */
    protected ResponseEntity<String> putJson(String url, String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(json, headers), String.class);
    }

    /**
     * 按当前 h2test 配置自行签发一个 JWT。
     *
     * <p>用途：构造"手机号在 user_msg 里查不到记录"这类场景——
     * 这种 token 无法通过真实登录接口拿到（登录会先校验 user 表），只能自己签。
     * 密钥/签发者一律取自 {@code JwtProperties}，不在测试里硬编码字面量。</p>
     */
    protected String issueToken(String telephone) {
        return JwtUtil.createJWT(
                jwtProperties.getSecretKey(),
                jwtProperties.getTtl(),
                jwtProperties.getIssuer(),
                JwtUserData.builder()
                        .id(999).username("ghost").password("x").telephone(telephone).build());
    }

    /**
     * 按 Flask（Python json.dumps ensure_ascii=True）规则转义非 ASCII 字符。
     *
     * <p>Jackson 输出大写十六进制（{@code \u767B}），Python 输出小写（{@code \u767b}），
     * 两者语义等价；本项目采用 Jackson 的大写形式，对比脚本会做归一化。</p>
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
