package com.csu.takeout.utils;

import com.csu.takeout.constant.JwtClaimsConstant;
import com.csu.takeout.dto.JwtUserData;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类。
 *
 * <p>方法与参数组织方式对齐 sky-take-out 的 JwtUtil（静态 createJWT / parseJWT），
 * 算法与载荷结构则与 Flask 版 auth.py 完全一致：</p>
 * <pre>
 *   {
 *     "data": {"id": 3, "username": "YCX", "password": "aA.460734909", "telephone": "18873190398"},
 *     "exp": &lt;now + 1 天&gt;,
 *     "iat": &lt;now - 1 天&gt;,
 *     "iss": "wufang"
 *   }
 * </pre>
 * <p>注意 {@code iat} 为"昨天"，这是 Flask 版原有的写法，此处刻意保持一致，
 * 以保证两边签发的 token 结构相同、可互相解析。</p>
 */
public class JwtUtil {

    /** HS256 对应的 JCE 算法名 */
    private static final String HMAC_SHA256 = "HmacSHA256";

    /** HS256 要求的最小密钥字节数（SHA-256 摘要长度，256 位） */
    private static final int MIN_KEY_BYTES = 32;

    /**
     * 生成 JWT
     *
     * @param secretKey 签名密钥
     * @param ttlMillis 过期时间（毫秒）
     * @param issuer    签发者
     * @param userData  用户信息，写入 data 声明
     * @return JWT 字符串
     */
    public static String createJWT(String secretKey, long ttlMillis, String issuer, JwtUserData userData) {
        Map<String, Object> data = new HashMap<>();
        data.put(JwtClaimsConstant.ID, userData.getId());
        data.put(JwtClaimsConstant.USERNAME, userData.getUsername());
        data.put(JwtClaimsConstant.PASSWORD, userData.getPassword());
        data.put(JwtClaimsConstant.TELEPHONE, userData.getTelephone());

        // 与 Flask 一致：过期时间 = 签发时间 + 1 天
        Date expiration = new Date(System.currentTimeMillis() + ttlMillis);

        JwtBuilder builder = Jwts.builder()
                // 自定义声明 data
                .claim(JwtClaimsConstant.DATA, data)
                // 签发时间：与 Flask 一致，设置为"一天前"
                .issuedAt(new Date(System.currentTimeMillis() - ttlMillis))
                // 签发者
                .issuer(issuer)
                // 过期时间
                .expiration(expiration)
                // 签名算法使用固定密钥
                .signWith(secretKey(secretKey), Jwts.SIG.HS256);

        return builder.compact();
    }

    /**
     * 解析并校验 JWT（签名、过期时间、签发者）
     *
     * @param secretKey 签名密钥
     * @param issuer    期望的签发者
     * @param token     JWT 字符串
     * @return 载荷
     */
    public static Claims parseJWT(String secretKey, String issuer, String token) {
        return Jwts.parser()
                .verifyWith(secretKey(secretKey))
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从载荷中取出 data 中的用户信息
     */
    @SuppressWarnings("unchecked")
    public static JwtUserData getUserData(Claims claims) {
        Object data = claims.get(JwtClaimsConstant.DATA);
        if (!(data instanceof Map)) {
            return null;
        }
        return JwtUserData.fromMap((Map<String, Object>) data);
    }

    /**
     * 由字符串密钥构造 HMAC-SHA256 密钥。
     *
     * <p>这里刻意不使用 {@code Keys.hmacShaKeyFor(...)}：Flask 版 auth.py 使用的密钥是
     * {@code "12asdffa2323"}（仅 96 位），而 JJWT 会按 RFC 7518 校验
     * "HMAC-SHA256 密钥不得短于 256 位"，在<b>签名与验签时都会</b>抛出
     * {@code WeakKeyException}。直接构造 {@link SecretKeySpec} 可以完全绕开该校验。</p>
     *
     * <p>为什么补零到 32 字节不影响与 Flask 的互通：RFC 2104 规定 HMAC 使用
     * <b>长度小于分组长度</b>（SHA-256 为 64 字节）的密钥时，需在末尾补零至分组长度。
     * PyJWT 把原始字节交给 Python 的 {@code hmac} 模块，JCE 的 {@code HmacSHA256} 收到
     * 短于分组长度的 {@link SecretKeySpec} 时同样按 RFC 2104 补零，
     * 因此两边算出的 HMAC 完全一致——补零到 32 字节只是为了让 JJWT 的长度校验通过，
     * <b>实际参与 HMAC 的密钥内容没有任何变化</b>。</p>
     *
     * <p>关于更长的密钥：只有当密钥<b>超过分组长度 64 字节</b>时，RFC 2104 才要求先对密钥
     * 做一次哈希；60/64/100 字节这类长度并不会触发。因此只要密钥不超过 64 字节，
     * PyJWT 与 JCE 的计算方式就完全一致、token 双向互通。本项目默认密钥仅 12 字节，
     * 补零到 32 字节后仍远小于 64，不涉及该分支。</p>
     */
    private static SecretKey secretKey(String secretKey) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_KEY_BYTES) {
            keyBytes = Arrays.copyOf(keyBytes, MIN_KEY_BYTES);
        }
        return new SecretKeySpec(keyBytes, HMAC_SHA256);
    }
}
