package com.csu.takeout.config;

import com.csu.takeout.vo.DispatcherWorkloadVO;
import com.csu.takeout.vo.StatsOverviewVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Spring Cache + Redis 的配置断言（不启动 Spring、不连 Redis）。
 *
 * <p>锁定三个契约：TTL 60 秒、统一 key 前缀、值用 JSON 序列化且缓存类型可原样回读。</p>
 */
class RedisCacheConfigurationTest {

    @Test
    @DisplayName("缓存默认配置：TTL 60 秒、key 前缀 csu:cache:{缓存名}::")
    void cacheDefaults() {
        RedisCacheConfiguration config = RedisConfiguration.cacheConfiguration();

        assertEquals(Duration.ofSeconds(60), config.getTtl());
        assertEquals("csu:cache:stats::", config.getKeyPrefixFor("stats"));
    }

    @Test
    @DisplayName("值序列化器 JSON 往返：VO 与列表原样回读（Redis 里存的是 JSON 文本）")
    void valueSerializerRoundTripsCachedTypes() {
        var serializer = RedisConfiguration.cacheValueSerializer();

        StatsOverviewVO overview = new StatsOverviewVO(1L, 2L, 3L, 4L, 5L, 6L, 7L);
        assertEquals(overview, serializer.deserialize(serializer.serialize(overview)));

        List<DispatcherWorkloadVO> workload = new ArrayList<>();
        workload.add(new DispatcherWorkloadVO("D1", "张三", "13800000000", 2L, 3L, 5L));
        assertEquals(workload, serializer.deserialize(serializer.serialize(workload)));
    }
}
