package com.csu.takeout.utils;

import com.csu.takeout.dto.JwtUserData;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JWT 工具类测试，重点是<b>短密钥补零后仍与手工复算的 HMAC 完全一致</b>。
 *
 * <p>核心用例 {@link #signatureMatchesManualHmac()} 不依赖 JJWT 自身的实现：
 * 它按 RFC 7518 / RFC 2104 的定义手工拼接 {@code base64url(header).base64url(payload)}
 * 并用"短密钥补零到 64 字节分组长度"的方式独立计算 HMAC-SHA256，
 * 再与 {@link JwtUtil} 产出的 token 第三段比对，验证短密钥补零不改变签名结果。
 * 因此签名一致即证明：补零策略与"直接使用原始密钥"完全等价。</p>
 */
class JwtUtilTest {

    /** 与 application.yml 的默认密钥一致 */
    private static final String SECRET = "12asdffa2323";
    private static final String ISSUER = "wufang";
    private static final long TTL_MILLIS = 86400000L;

    private static final JwtUserData USER = JwtUserData.builder()
            .id(3)
            .username("YCX")
            .password("aA.460734909")
            .telephone("18873190398")
            .build();

    @Test
    @DisplayName("签名与手工复算的 HMAC 逐字节一致（按 RFC 2104）")
    void signatureMatchesManualHmac() throws Exception {
        String token = JwtUtil.createJWT(SECRET, TTL_MILLIS, ISSUER, USER);
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT 应为三段：" + token);

        // 独立复算：直接使用原始密钥字节（不补零）
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] expected = mac.doFinal((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        String expectedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(expected);

        assertEquals(expectedSignature, parts[2], "签名与手工复算不一致，短密钥处理有误");

        // 同时验证"短密钥补零到 64 字节"这一 RFC 2104 约定下的结果相同，
        // 说明补零到 32 字节并没有改变实际参与 HMAC 的密钥内容
        byte[] paddedKey = Arrays.copyOf(SECRET.getBytes(StandardCharsets.UTF_8), 64);
        Mac paddedMac = Mac.getInstance("HmacSHA256");
        paddedMac.init(new SecretKeySpec(paddedKey, "HmacSHA256"));
        String paddedSignature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(paddedMac.doFinal((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII)));
        assertEquals(expectedSignature, paddedSignature, "补零到分组长度不应改变 HMAC 结果");
    }

    @Test
    @DisplayName("头部为标准 HS256 JWT 头")
    void headerIsStandard() {
        String token = JwtUtil.createJWT(SECRET, TTL_MILLIS, ISSUER, USER);
        String header = new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]), StandardCharsets.UTF_8);
        assertEquals("{\"alg\":\"HS256\"}", header);
    }

    @Test
    @DisplayName("载荷包含 data/exp/iat/iss，且 iat 为一天前、有效期为一天")
    void claimsMatchContract() {
        long now = System.currentTimeMillis();
        String token = JwtUtil.createJWT(SECRET, TTL_MILLIS, ISSUER, USER);
        Claims claims = JwtUtil.parseJWT(SECRET, ISSUER, token);

        assertEquals("wufang", claims.getIssuer());
        long expDelta = claims.getExpiration().getTime() - now;
        long iatDelta = now - claims.getIssuedAt().getTime();
        assertTrue(Math.abs(expDelta - TTL_MILLIS) < 5000, "exp 应为签发时间 + 1 天，实际差 " + expDelta);
        assertTrue(Math.abs(iatDelta - TTL_MILLIS) < 5000, "iat 应为一天前，实际差 " + iatDelta);

        JwtUserData userData = JwtUtil.getUserData(claims);
        assertNotNull(userData);
        assertEquals(USER.getId(), userData.getId());
        assertEquals(USER.getUsername(), userData.getUsername());
        assertEquals(USER.getPassword(), userData.getPassword());
        assertEquals(USER.getTelephone(), userData.getTelephone());
    }

    @Test
    @DisplayName("载荷中的 data 字段键名与接口契约一致")
    void payloadContainsExpectedKeys() {
        String token = JwtUtil.createJWT(SECRET, TTL_MILLIS, ISSUER, USER);
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
        assertTrue(payload.startsWith("{\"data\":{"), "data 应为第一个声明，实际：" + payload);
        assertTrue(payload.contains("\"id\":3"), payload);
        assertTrue(payload.contains("\"username\":\"YCX\""), payload);
        assertTrue(payload.contains("\"password\":\"aA.460734909\""), payload);
        assertTrue(payload.contains("\"telephone\":\"18873190398\""), payload);
        assertTrue(payload.contains("\"iss\":\"wufang\""), payload);
    }

    @Test
    @DisplayName("签发者不匹配时验签失败（requireIssuer 强校验）")
    void issuerIsRequired() {
        String token = JwtUtil.createJWT(SECRET, TTL_MILLIS, "not-wufang", USER);
        assertThrows(JwtException.class, () -> JwtUtil.parseJWT(SECRET, ISSUER, token));
    }

    @Test
    @DisplayName("密钥错误时验签失败")
    void wrongSecretFails() {
        String token = JwtUtil.createJWT(SECRET, TTL_MILLIS, ISSUER, USER);
        assertThrows(JwtException.class, () -> JwtUtil.parseJWT("another-secret", ISSUER, token));
    }

    @Test
    @DisplayName("已过期的 token 验签失败")
    void expiredTokenFails() {
        String token = JwtUtil.createJWT(SECRET, -1000L, ISSUER, USER);
        assertThrows(ExpiredJwtException.class, () -> JwtUtil.parseJWT(SECRET, ISSUER, token));
    }
}
