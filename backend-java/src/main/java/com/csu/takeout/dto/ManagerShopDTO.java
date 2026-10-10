package com.csu.takeout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理端店铺维护参数（同一接口按 {@code action} 区分新增与修改）。
 *
 * <p>字段名与 Flask 版一致：{@code action}、{@code shop_name}、{@code price}、{@code m_sale_v}。</p>
 *
 * <p>注意：全局下划线命名策略只作用于<b>序列化</b>（响应），不作用于<b>反序列化</b>（请求）。
 * 因此请求里的 {@code shop_name}、{@code m_sale_v} 必须用 {@link JsonProperty} 显式声明，
 * 否则会因"Unrecognized field"导致 400/解析失败。</p>
 *
 * <p>{@code price} 与 {@code m_sale_v} 声明为 {@link String}，是为了复刻 Flask 版的
 * {@code int(price)} 转换语义——前端 Element UI 的输入框提交的是字符串，
 * 而 Flask 会把它转成整数后写库。由 controller 显式转换，转换失败时按业务失败返回。</p>
 */
@Data
@Schema(description = "店铺维护参数（action=add/change）")
public class ManagerShopDTO implements Serializable {

    @Schema(description = "操作类型：add 新增、change 修改", example = "add")
    private String action;

    @Schema(description = "店铺名称", example = "小美麻辣烫")
    @JsonProperty("shop_name")
    private String shopName;

    @Schema(description = "产品单价", example = "15")
    private String price;

    @Schema(description = "月销量", example = "103")
    @JsonProperty("m_sale_v")
    private String mSaleV;
}
