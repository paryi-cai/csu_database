package com.csu.takeout.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.util.Map;

/**
 * 统一 JSON 序列化器（全站响应的唯一出口）。
 *
 * <p>约定的响应格式形如：</p>
 * <pre>
 *   {"status": 200, "msg": "成功下单"}
 *   {"status": 200, "tabledata": [{"shop_name": "大美鸡公煲", "price": 15, "sale": 100}]}
 * </pre>
 * <p>排版规则：<b>单行输出、冒号后一个空格、逗号后一个空格、空数组紧凑为 []、整体以换行结尾</b>。
 * Jackson 默认输出紧凑格式 {@code {"status":200,...}}，存在字节差异。本类通过自定义
 * PrettyPrinter 精确复刻上述排版，保证前端依赖的响应体逐字节稳定
 * （见 {@code UnifiedJsonObjectMapperTest}，断言的就是响应体原文）。</p>
 *
 * <p>实现要点（均已通过测试验证）：</p>
 * <ol>
 *   <li>以 {@link MinimalPrettyPrinter} 为基类：它本身不换行，但硬编码冒号为 {@code ':'}
 *       且不使用 {@code Spacing} 配置，因此需要自行覆写分隔符方法；</li>
 *   <li>{@code writeArrayValueSeparator} 由 Jackson 在数组元素<b>之间</b>调用，
 *       正好对应约定的 {@code ", "}，不会产生首元素前的多余逗号；</li>
 *   <li>空数组无需特判即输出 {@code []}（基类的 {@code beforeArrayValues} 是空实现）；</li>
 *   <li>末尾换行不能依赖 {@code writeRootValueSeparator}（Jackson 只在写下一个根值前调用它，
 *       单个响应体场景永远不会触发），改为在根层对象/数组闭合时补齐。</li>
 * </ol>
 *
 * <p>注意：{@code INDENT_OUTPUT} 必须显式开启，否则 Spring MVC 的
 * {@code MappingJackson2HttpMessageConverter} 不会启用 PrettyPrinter。</p>
 */
public class UnifiedJsonObjectMapper extends ObjectMapper {

    public UnifiedJsonObjectMapper() {
        super();
        // 空对象输出 {}、空集合输出 []，与既有契约一致
        this.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // 开启格式化输出，使自定义 PrettyPrinter 生效
        this.enable(SerializationFeature.INDENT_OUTPUT);
        // 响应键统一为下划线风格：Java 属性 camelCase，响应键 snake_case
        // （order_id、cons_name、disp_phone ...）。个别键名不规则的字段
        // （orderway / price / sale / dispPhone）用 @JsonProperty 单独覆盖。
        this.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        // 约定的响应格式要求：非 ASCII 字符一律转义输出（形如 U+767B 的转义序列）。
        //
        // 关于这里的取舍（已实测确认，jackson-core 2.18.3）：
        //   ObjectMapper._configAndWriteValue() 内部会执行
        //     if (!isEnabled(JsonGenerator.Feature.ESCAPE_NON_ASCII)) generator.setCharacterEscapes(null);
        //   即"未开启该特性时会把自定义转义表清空"。因此要保留自定义转义就必须开启该特性。
        //   开启后 Jackson 走内置分支输出【大写】十六进制（\u767B）；部分其他实现习惯输出
        //   【小写】（\u767b）。两者是等价的 JSON 转义，任何解析器（浏览器 / Vue / Axios）
        //   结果完全一致，这里统一采用大写形式，语义不受影响。
        this.getFactory().configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), true);
        this.setDefaultPrettyPrinter(new UnifiedPrettyPrinter());
    }

    /**
     * 统一响应排版的 PrettyPrinter
     */
    static class UnifiedPrettyPrinter extends MinimalPrettyPrinter {

        private static final long serialVersionUID = 1L;

        /**
         * 当前嵌套深度，由本类自行维护。
         *
         * <p>不能用 {@code getOutputContext().inRoot()} 或 {@code getNestingDepth()} 判断根层：
         * Jackson 在序列化 POJO 时（BeanSerializer.serialize → gen.writeStartObject(Object)）
         * 会先压入一个额外上下文，此时根对象的 depth 为 1、{@code inRoot()} 为 false，
         * 而序列化 Map 时又是 depth 0 —— 两种情况不一致，无法据此识别根层。
         * 因此改为在 start/end 回调中自行计数，计数归零即为根层结束。</p>
         */
        private int depth;

        @Override
        public void writeStartObject(JsonGenerator g) throws IOException {
            ++depth;
            g.writeRaw('{');
        }

        @Override
        public void writeStartArray(JsonGenerator g) throws IOException {
            ++depth;
            g.writeRaw('[');
        }

        /** 对象字段名与值之间：{@code ": "} */
        @Override
        public void writeObjectFieldValueSeparator(JsonGenerator g) throws IOException {
            g.writeRaw(": ");
        }

        /** 对象字段之间：{@code ", "} */
        @Override
        public void writeObjectEntrySeparator(JsonGenerator g) throws IOException {
            g.writeRaw(", ");
        }

        /** 数组元素之间：{@code ", "}（由 Jackson 在元素之间调用，首元素前不会调用） */
        @Override
        public void writeArrayValueSeparator(JsonGenerator g) throws IOException {
            g.writeRaw(", ");
        }

        @Override
        public void writeEndArray(JsonGenerator g, int nrOfValues) throws IOException {
            g.writeRaw(']');
            appendTrailingNewLineIfRoot(g);
        }

        @Override
        public void writeEndObject(JsonGenerator g, int nrOfEntries) throws IOException {
            g.writeRaw('}');
            appendTrailingNewLineIfRoot(g);
        }

        /** 根值结束后追加换行，等价于 响应体末尾的换行 */
        private void appendTrailingNewLineIfRoot(JsonGenerator g) throws IOException {
            if (depth > 0) {
                --depth;
            }
            if (depth == 0) {
                g.writeRaw('\n');
            }
        }
    }

    /**
     * 便于调试与测试：返回 统一格式 JSON 字符串
     *
     * <p>与 HTTP 响应走同一条写出路径（{@link UnifiedJsonWriter}），因此测试断言的就是
     * 真实响应体的内容：排版（冒号/逗号后的空格、末尾换行、空数组）由本类的
     * PrettyPrinter 决定，非 ASCII 字符的转义由本类构造器开启的工厂特性
     * {@code JsonWriteFeature.ESCAPE_NON_ASCII} 决定。</p>
     */
    public String toUnifiedJson(Object value) throws IOException {
        return UnifiedJsonWriter.writeToString(this, value);
    }

    /**
     * 便于调试与测试：统一格式 JSON 字符串转回 Map
     */
    public Map<?, ?> fromJson(String json) throws IOException {
        return this.readValue(json, Map.class);
    }
}
