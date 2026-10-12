package com.csu.takeout.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 配置。
 *
 * <p>Redis 在本项目承担<b>管理端数据看板的统计缓存</b>（真实业务使用）：</p>
 * <ul>
 *   <li>{@code @EnableCaching} 打开注解缓存，由 Spring Boot 自动装配 RedisCacheManager；</li>
 *   <li>统一缓存策略（{@link #cacheConfiguration()}）：TTL 60 秒（看板可容忍 1 分钟内的陈旧）、
 *       key 用 String 序列化（redis-cli 里可读）、值用 JSON 序列化
 *       （不用 JDK 序列化：可读、跨版本稳、无反序列化安全顾虑）；</li>
 *   <li>缓存故障降级（{@link #errorHandler()}）：Redis 不可用时只写日志、回落到直查数据库，
 *       与告警链路同一原则——旁路设施不能影响主业务；</li>
 *   <li>{@code RedisTemplate} 保留给后续非注解场景（如短信验证码，key 命名见
 *       {@code com.csu.takeout.constant.RedisConstant}）。</li>
 * </ul>
 */
@Configuration
@EnableCaching
@Slf4j
public class RedisConfiguration implements CachingConfigurer {

    /** 看板缓存 TTL：仪表盘允许 1 分钟内的陈旧数据，短 TTL 同时兜底失效遗漏 */
    static final Duration CACHE_TTL = Duration.ofSeconds(60);

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // 设置 redis 的连接工厂对象
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // 设置 redis key 的序列化器
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        return redisTemplate;
    }

    /** 看板缓存统一配置（包可见，便于单测直接断言 TTL / key 前缀 / 序列化行为） */
    static RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(CACHE_TTL)
                // key 前缀统一收口，便于在 redis-cli 里按 csu:cache:* 定位与清理
                .computePrefixWith(cacheName -> "csu:cache:" + cacheName + "::")
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(cacheValueSerializer()));
    }

    /** 缓存值的 JSON 序列化器（带类型信息，反序列化后仍是原 VO 类型） */
    static RedisSerializer<Object> cacheValueSerializer() {
        return new GenericJackson2JsonRedisSerializer();
    }

    /** 把统一缓存策略挂到 Boot 自动装配的 RedisCacheManager 上 */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return builder -> builder.cacheDefaults(cacheConfiguration());
    }

    /** 缓存故障降级：读失败视为未命中（回落数据库），写/失效失败只记日志 */
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }
}
