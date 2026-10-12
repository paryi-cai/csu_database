package com.csu.takeout.service;

import com.csu.takeout.CsuTakeoutApplication;
import com.csu.takeout.constant.RedisConstant;
import com.csu.takeout.dto.OrderSubmitDTO;
import com.csu.takeout.vo.StatsOverviewVO;
import com.csu.takeout.vo.StatsTrendVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 看板统计缓存（@Cacheable / @CacheEvict）的集成测试。
 *
 * <p>h2test profile 把缓存切换为内存实现（{@code spring.cache.type=simple}），
 * 因此无需 Redis 即可验证三件事：同参数命中同一缓存对象、key 按「接口 + 参数」隔离、
 * 写路径（店铺维护 / 下单）触发全量失效。</p>
 */
@SpringBootTest(classes = CsuTakeoutApplication.class)
@ActiveProfiles("h2test")
class ManagerStatsCacheTest {

    @Autowired
    private ManagerStatsService statsService;

    @Autowired
    private ManagerShopService managerShopService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CacheManager cacheManager;

    private Cache statsCache() {
        Cache cache = cacheManager.getCache(RedisConstant.STATS_CACHE);
        assertNotNull(cache, "缓存管理器里应存在 stats 缓存");
        return cache;
    }

    @Test
    @DisplayName("同参数二次读取命中缓存：返回同一对象；不同参数各自成 key")
    void secondReadHitsCache() {
        StatsOverviewVO first = statsService.overview();
        assertSame(first, statsService.overview(), "第二次读取应命中缓存（对象同一）");
        assertSame(first, statsCache().get("overview").get());

        StatsTrendVO trend7 = statsService.trend(7);
        assertSame(trend7, statsService.trend(7));
        statsService.trend(30);
        assertNotNull(statsCache().get("trend:7"), "参数不同的调用应各自成 key");
        assertNotNull(statsCache().get("trend:30"));

        statsService.shops(10, "money");
        assertNotNull(statsCache().get("shops:10:money"));
    }

    @Test
    @DisplayName("店铺新增 / 删除：全量失效统计缓存")
    void shopMutationEvictsStatsCache() {
        statsService.overview();
        assertNotNull(statsCache().get("overview"));

        assertTrue(managerShopService.add("缓存测试店", 10, 0));
        assertNull(statsCache().get("overview"), "新增店铺后统计缓存应被清空");

        managerShopService.delete("缓存测试店");
    }

    @Test
    @DisplayName("下单：全量失效统计缓存")
    void orderMutationEvictsStatsCache() {
        statsService.status();
        assertNotNull(statsCache().get("status"));

        OrderSubmitDTO dto = new OrderSubmitDTO();
        dto.setShopName("大美鸡公煲");
        dto.setOrderMoney(15);
        dto.setOrderWay("人工订餐");
        dto.setConsName("缓存测试");
        dto.setConsAddre("15公寓");
        orderService.addOrder("18873190398", dto);

        assertNull(statsCache().get("status"), "下单后统计缓存应被清空");
    }
}
