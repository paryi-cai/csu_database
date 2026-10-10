package com.csu.takeout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 下单请求参数。
 *
 * <p>字段名与 Flask 版逐个对齐：{@code shop_name}、{@code order_money}、{@code order_way}、
 * {@code cons_name}、{@code cons_addre}；顾客手机号<b>不由前端传递</b>，
 * 而是从 JWT 载荷的 telephone 取得。</p>
 *
 * <p>注意：全局下划线命名策略只作用于序列化（响应），请求字段必须显式用
 * {@link JsonProperty} 声明，否则会解析失败。</p>
 */
@Data
@Schema(description = "用户下单参数")
public class OrderSubmitDTO implements Serializable {

    @Schema(description = "店铺名称", example = "大美鸡公煲")
    @JsonProperty("shop_name")
    private String shopName;

    @Schema(description = "订单金额（前端取店铺单价）", example = "15")
    @JsonProperty("order_money")
    private Integer orderMoney;

    @Schema(description = "订餐方式", example = "人工订餐")
    @JsonProperty("order_way")
    private String orderWay;

    @Schema(description = "顾客姓名", example = "张三")
    @JsonProperty("cons_name")
    private String consName;

    @Schema(description = "送餐地址", example = "15公寓")
    @JsonProperty("cons_addre")
    private String consAddre;
}
