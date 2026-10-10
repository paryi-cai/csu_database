package com.csu.takeout.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * 按天聚合的原始行（内部使用）。
 *
 * <p>{@code day} 由 SQL 的 {@code substring(create_time, 1, 10)} 得到：
 * {@code create_time} 是 varchar 且格式固定为 {@code yyyy-MM-dd HH:mm:ss}，
 * 取前 10 位即日期。之所以不用 {@code date_format(...)}，是因为该函数
 * 在 H2 1.4.200（契约测试库）上没有对应实现，会抛 SQLException；
 * 字符串截取在 MySQL 8 与 H2 上行为完全一致。</p>
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatsDailyRowVO implements Serializable {

    /** 日期 yyyy-MM-dd */
    private String day;

    /** 当日订单量 */
    private Long orderCount;

    /** 当日营业额 */
    private Long revenue;
}
