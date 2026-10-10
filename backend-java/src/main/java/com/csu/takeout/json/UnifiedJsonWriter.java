package com.csu.takeout.json;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 统一格式 JSON 的写出工具（测试与调试用）。
 *
 * <p>实际转义与排版配置都在 {@link UnifiedJsonObjectMapper} 上：</p>
 * <ul>
 *   <li>排版（空格、末尾换行、空数组）由 {@code UnifiedPrettyPrinter} 决定；</li>
 *   <li>非 ASCII 字符转义由工厂特性 {@code ESCAPE_NON_ASCII} 决定。</li>
 * </ul>
 *
 * <p>本方法存在的意义是让测试走<b>与 HTTP 响应完全相同</b>的序列化路径，
 * 从而保证测试断言的就是真实响应体内容。</p>
 */
public final class UnifiedJsonWriter {

    private UnifiedJsonWriter() {
    }

    /**
     * 用 约定规则把对象序列化为字符串（含末尾换行）
     */
    public static String writeToString(ObjectMapper mapper, Object value) throws java.io.IOException {
        return mapper.writeValueAsString(value);
    }
}
