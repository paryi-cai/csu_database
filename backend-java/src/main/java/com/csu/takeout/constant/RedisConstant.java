package com.csu.takeout.constant;

/**
 * Redis key 命名常量。
 *
 * <p>key 命名约定：{@code valid_code:{手机号}}，
 * 值为短信验证码，TTL 600 秒。当前 前后端 两版均未启用短信验证码功能
 * （对应接口未实现），此处仅登记命名约定，<b>不新增任何 key</b>。</p>
 */
public class RedisConstant {

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
