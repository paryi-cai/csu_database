package com.csu.takeout.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 相关配置，对应 application.yml 中的 csu.jwt.*
 *
 * <p>与 sky-take-out 的 JwtProperties 组织方式一致，但本项目只有一个端（前端只有一套
 * token）；密钥、有效期、签发者保持默认值稳定，以保证历史上签发的 token 始终可解析
 * （短密钥的补零策略见 {@code JwtUtil}）。</p>
 */
@Component
@ConfigurationProperties(prefix = "csu.jwt")
@Data
public class JwtProperties {

    /** JWT 签名密钥（HS256），默认值见 application.yml（生产用环境变量 JWT_SECRET 覆盖） */
    private String secretKey;

    /** JWT 过期时间（毫秒），默认 24 小时（86400000） */
    private long ttl;

    /** 前端传递令牌的请求头名称，对应前端的自定义头 token（不是 Authorization） */
    private String tokenName;

    /** 签发者，解析时强校验（默认 wufang） */
    private String issuer;
}
