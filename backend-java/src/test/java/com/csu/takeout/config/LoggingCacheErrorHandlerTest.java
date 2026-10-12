package com.csu.takeout.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * 缓存故障降级处理器测试：四类缓存错误都必须只写日志、不抛异常
 * （Redis 挂掉时看板接口仍可用，只是失去缓存增益）。
 */
class LoggingCacheErrorHandlerTest {

    @Test
    @DisplayName("get/put/evict/clear 四类错误全部吞掉，不影响业务")
    void neverThrowsOnAnyCacheError() {
        LoggingCacheErrorHandler handler = new LoggingCacheErrorHandler();
        Cache cache = new ConcurrentMapCache("stats");
        RuntimeException cause = new RuntimeException("redis down");

        assertDoesNotThrow(() -> {
            handler.handleCacheGetError(cause, cache, "overview");
            handler.handleCachePutError(cause, cache, "overview", new Object());
            handler.handleCacheEvictError(cause, cache, "overview");
            handler.handleCacheClearError(cause, cache);
        });
    }
}
