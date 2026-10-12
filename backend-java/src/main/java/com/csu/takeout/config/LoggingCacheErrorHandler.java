package com.csu.takeout.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * 缓存故障降级处理器：Redis 不可用时只写日志，绝不向上抛异常。
 *
 * <ul>
 *   <li>{@code get} 失败：不抛异常后缓存层视为未命中，调用方回落到数据库查询（接口仍可用）；</li>
 *   <li>{@code put / evict / clear} 失败：忽略本次缓存操作，业务结果不受影响（只是没有缓存增益）。</li>
 * </ul>
 *
 * <p>与告警链路同一设计原则：旁路设施（缓存 / 邮件）的故障不能影响主业务。</p>
 */
@Slf4j
public class LoggingCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("缓存读取失败，降级为直查数据库：cache={}, key={}, cause={}",
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("缓存写入失败（已忽略）：cache={}, key={}, cause={}",
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("缓存失效失败（已忽略，等待 TTL 到期）：cache={}, key={}, cause={}",
                cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("缓存清空失败（已忽略，等待 TTL 到期）：cache={}, cause={}",
                cache.getName(), exception.getMessage());
    }
}
