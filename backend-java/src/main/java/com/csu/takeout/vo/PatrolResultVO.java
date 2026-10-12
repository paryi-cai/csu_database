package com.csu.takeout.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 一次巡检的完整结果（/api/manager/alert/patrol 的 data）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"consistent", "alert_sent", "issues"})
public class PatrolResultVO implements Serializable {

    /** 是否全部一致 */
    private Boolean consistent;

    /** 本次是否实际发送了告警邮件（未启用 / SMTP 未配置 / 处于节流窗口时为 false） */
    private Boolean alertSent;

    /** 异常明细，一致时为空列表 */
    private List<ConsistencyIssueVO> issues;
}
