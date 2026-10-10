package com.csu.takeout.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置，与 sky-take-out 的 RedisConfiguration 一致。
 *
 * <p>说明：Flask 版中 Redis 仅被"发送短信验证码"相关代码使用，而该部分代码在
 * app.py 中处于注释状态，因此当前没有任何接口依赖 Redis。此处保留依赖与配置，
 * 是为保持技术栈一致并便于后续启用验证码功能（key 命名见
 * {@code com.csu.takeout.constant.RedisConstant}）。</p>
 */
@Configuration
@Slf4j
public class RedisConfiguration {

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
}
