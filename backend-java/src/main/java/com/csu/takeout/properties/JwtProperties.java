package com.csu.takeout.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 相关配置，对应 application.yml 中的 csu.jwt.*
 *
 * <p>与 sky-take-out 的 JwtProperties 组织方式一致，但本项目只有一个端（前端只有一套
 * token），且签名密钥、过期时间、签发者均与 Flask 版 auth.py 保持一致，
 * 以便 Flask 与 Spring Boot 两个后端可以互相解析对方签发的 token。</p>
 */
@Component
@ConfigurationProperties(prefix = "csu.jwt")
@Data
public class JwtProperties {

    /** JWT 签名密钥（HS256），对应 Flask auth.py 中的 secret */
    private String secretKey;

    /** JWT 过期时间（毫秒），对应 Flask 的 datetime.timedelta(days=1) */
    private long ttl;

    /** 前端传递令牌的请求头名称，对应前端的自定义头 token（不是 Authorization） */
    private String tokenName;

    /** 签发者，解析时强校验，对应 Flask PyJWT 的 issuer='wufang' */
    private String issuer;
}
