package com.csu.takeout.constant;

/**
 * Redis key 命名常量。
 *
 * <p>与 Flask 版 app.py 中的 key 命名保持一致：{@code valid_code:{手机号}}，
 * 值为短信验证码，TTL 600 秒。当前 Flask 与 Java 两版均未启用短信验证码功能
 * （app.py 中相关路由处于注释状态），此处仅登记既有命名，<b>不新增任何 key</b>。</p>
 */
public class RedisConstant {

    /** 短信验证码 key 前缀，完整 key 形如 valid_code:18682412818 */
    public static final String VALID_CODE_PREFIX = "valid_code:";

    /** 验证码有效期（秒），对应 Flask 版 redis_store.set(..., 600) */
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
