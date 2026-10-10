package com.csu.takeout.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 店铺名称候选项，用于 /api/manager/server 的 {@code shop_range}。
 *
 * <p>Flask 版返回的元素形如 {@code {"shop_name": "大美鸡公煲"}}（只含名称）。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopNameVO implements Serializable {

    /** 响应键 shop_name */
    @JsonProperty("shop_name")
    private String shopName;
}
