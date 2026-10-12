package com.csu.takeout.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 数据一致性巡检结果条目（/api/manager/alert/patrol 响应体 data 字段中的 issues 元素）。
 *
 * <p>三种异常：行数不一致（{@code COUNT_MISMATCH}）、目标表缺失或查询失败
 * （{@code QUERY_FAILED}）、库不可达（{@code UNREACHABLE}）。库不可达时
 * {@code table} / 行数为 null，只保留 {@code detail} 描述。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"target", "table", "source_count", "target_count", "detail"})
public class ConsistencyIssueVO implements Serializable {

    /** 目标库名称（源库不可达时为"源库"） */
    private String target;

    /** 表名；库不可达时为 null */
    private String table;

    /** 源库行数；不可达时为 null */
    private Long sourceCount;

    /** 目标库行数；不可达时为 null */
    private Long targetCount;

    /** 异常描述 */
    private String detail;

    /** 行数不一致 */
    public static ConsistencyIssueVO mismatch(String target, String table, long sourceCount, long count) {
        return ConsistencyIssueVO.builder()
                .target(target)
                .table(table)
                .sourceCount(sourceCount)
                .targetCount(count)
                .detail("行数不一致")
                .build();
    }

    /** 库不可达 */
    public static ConsistencyIssueVO unreachable(String target, String message) {
        return ConsistencyIssueVO.builder()
                .target(target)
                .detail("库不可达：" + message)
                .build();
    }

    /** 单表查询失败（如表不存在） */
    public static ConsistencyIssueVO queryFailed(String target, String table, String message) {
        return ConsistencyIssueVO.builder()
                .target(target)
                .table(table)
                .detail("查询失败：" + message)
                .build();
    }
}
