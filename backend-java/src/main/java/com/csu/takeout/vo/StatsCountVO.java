package com.csu.takeout.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统计接口的通用「名称 + 累计数量」条目。
 *
 * <p>用于字段很少的维度统计（订单状态分布、订餐方式构成、下单时段分布），
 * 与 {@link ShopRankVO}、{@link DispatcherWorkloadVO} 这类字段较多的维度分开，
 * 避免一个"万能 VO"塞进十几个用不到的字段。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"name", "count"})
public class StatsCountVO implements Serializable {

    /** 维度名称（订餐方式名 / 小时标签等） */
    private String name;

    /** 该维度的累计数量 */
    private Long count;
}
