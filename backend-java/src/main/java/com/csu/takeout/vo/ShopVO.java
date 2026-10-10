package com.csu.takeout.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户端店铺列表项。
 *
 * <p>响应键为 {@code shop_name} / {@code price} / {@code sale}。
 * 注意销量字段在数据库中叫 {@code m_sale_v}，在响应里叫 {@code sale}。</p>
 *
 * <p>提示：{@code @JsonPropertyOrder} 的取值必须是<b>序列化后的键名</b>（下划线风格），
 * 而不是 Java 属性名，否则与全局命名策略不匹配、注解会失效。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"shop_name", "price", "sale"})
public class ShopVO implements Serializable {

    /** 响应键 shop_name */
    @JsonProperty("shop_name")
    private String shopName;

    private Integer price;

    /** 响应键 sale（数据库列 m_sale_v） */
    private Integer sale;
}
