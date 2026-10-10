package com.csu.takeout.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * 订单表聚合结果（内部使用，不直接作为响应体）。
 *
 * <p>原先这里用的是 {@code Map<String, Object>}，实测在 H2 + MyBatis 下聚合列没被映射进来
 * （{@code get("totalOrders")} 为 null，导致总订单数与总营业额恒为 0）。
 * 改用带字段的 POJO 后由 MyBatis 按列别名走 setter 注入，行为稳定可测。</p>
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderSumVO implements Serializable {

    /** 订单总数（SQL 别名 totalOrders） */
    private Long totalOrders;

    /** 订单总金额（SQL 别名 totalRevenue） */
    private Long totalRevenue;
}
