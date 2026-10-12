package com.csu.takeout.constant;

/**
 * Redis key / 缓存名 命名常量。
 *
 * <p>两类用途：</p>
 * <ul>
 *   <li><b>看板缓存</b>（已启用）：缓存名 {@link #STATS_CACHE}，实际 Redis key 形如
 *       {@code csu:cache:stats::overview}（{@code csu:cache:{缓存名}::} 前缀在
 *       {@code RedisConfiguration} 统一拼接）；</li>
 *   <li><b>短信验证码</b>（预留）：key 形如 {@code valid_code:18682412818}，TTL 600 秒；
 *       对应接口未实现，此处只登记命名约定。</li>
 * </ul>
 */
public class RedisConstant {

    /** 管理端数据看板统计缓存名（@Cacheable / @CacheEvict 共用） */
    public static final String STATS_CACHE = "stats";

    /** 短信验证码 key 前缀，完整 key 形如 valid_code:18682412818 */
    public static final String VALID_CODE_PREFIX = "valid_code:";

    /** 验证码有效期（秒），对应 Redis 键的 600 秒过期 */
    public static final long VALID_CODE_TTL_SECONDS = 600L;

    private RedisConstant() {
    }

    /**
     * 拼接验证码 key
     */
    public static String validCodeKey(String telephone) {
        return VALID_CODE_PREFIX + telephone;
    }
}
