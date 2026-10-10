package com.csu.takeout.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 统一 JSON 消息转换器。
 *
 * <p>把 {@link UnifiedJsonObjectMapper} 挂到 Spring MVC 上，使响应体与统一排版
 * 逐字节一致（空格、末尾换行、空数组、非 ASCII 转义）。</p>
 *
 * <p>相比直接 {@code setObjectMapper}，这里额外做了两件事：</p>
 * <ol>
 *   <li>固定字符集为 UTF-8：本转换器直接以 UTF-8 写字节，若响应头仍按平台默认字符集声明，
 *       客户端会解码错误导致中文乱码；</li>
 *   <li>排除了 {@code String} 返回值：保持与 Spring 默认行为一致，让
 *       {@code StringHttpMessageConverter} 处理纯文本，避免给文本加上 JSON 引号。</li>
 * </ol>
 */
public class UnifiedJsonMessageConverter extends MappingJackson2HttpMessageConverter {

    public UnifiedJsonMessageConverter(ObjectMapper objectMapper) {
        super(objectMapper);
        setDefaultCharset(StandardCharsets.UTF_8);
    }

    @Override
    public Charset getDefaultCharset() {
        return StandardCharsets.UTF_8;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        if (CharSequence.class.isAssignableFrom(clazz)) {
            return false;
        }
        return super.canWrite(clazz, MediaType.APPLICATION_JSON);
    }
}
