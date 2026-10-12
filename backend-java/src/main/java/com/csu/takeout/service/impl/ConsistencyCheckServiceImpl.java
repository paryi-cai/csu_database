package com.csu.takeout.service.impl;

import com.csu.takeout.properties.ConsistencyProperties;
import com.csu.takeout.service.AlertService;
import com.csu.takeout.service.ConsistencyCheckService;
import com.csu.takeout.vo.ConsistencyIssueVO;
import com.csu.takeout.vo.PatrolResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 数据一致性巡检实现。
 *
 * <p>设计要点（面试要点）：</p>
 * <ol>
 *   <li><b>纯 JDBC 直连</b>：巡检目标是多个异构数据库（MySQL / MariaDB / PostgreSQL），
 *       不属于 MyBatis 的映射范围，因此用 {@link DriverManager} + {@code select count(*)}
 *       直接比对，不经过业务连接池（避免占用业务资源）；</li>
 *   <li><b>快速失败</b>：连接与查询超时在 JDBC URL 上配置（如 {@code connectTimeout=3000}），
 *       目标库没启动时巡检也要在数秒内返回而不是长时间挂起；</li>
 *   <li><b>双维度检测</b>：连接可达性（库不可达 / 表被删）与行数一致性，分别给出具体原因；</li>
 *   <li><b>恢复通知</b>：上次有异常、本次恢复一致时发一条恢复邮件，形成"告警 → 修复 → 确认"闭环。</li>
 * </ol>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ConsistencyCheckServiceImpl implements ConsistencyCheckService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 源库不可达时在告警中的展示名 */
    private static final String SOURCE_LABEL = "源库";

    private final ConsistencyProperties properties;

    private final AlertService alertService;

    /** 上一轮巡检是否存在异常（用于"恢复通知"的状态转换判断） */
    private final AtomicBoolean lastRunHadIssues = new AtomicBoolean(false);

    @Override
    public List<ConsistencyIssueVO> check() {
        List<ConsistencyIssueVO> issues = new ArrayList<>();
        ConsistencyProperties.DbEndpoint source = properties.getSource();

        // 1. 先数源库。源库整体不可达则没有比对基准，直接返回；
        //    单表查询失败（如表缺失）记录为异常，其它表继续
        Map<String, Long> sourceCounts = new LinkedHashMap<>();
        try (Connection conn = open(source)) {
            for (String table : properties.getTables()) {
                try {
                    sourceCounts.put(table, count(conn, table, source.getUrl()));
                } catch (SQLException ex) {
                    issues.add(ConsistencyIssueVO.queryFailed(SOURCE_LABEL, table, ex.getMessage()));
                }
            }
        } catch (SQLException ex) {
            log.warn("巡检：源库不可达 - {}", ex.getMessage());
            issues.add(ConsistencyIssueVO.unreachable(SOURCE_LABEL, ex.getMessage()));
            return issues;
        }

        // 2. 逐目标库比对
        for (ConsistencyProperties.DbEndpoint target : properties.getTargets()) {
            try (Connection conn = open(target)) {
                for (String table : properties.getTables()) {
                    Long sourceCount = sourceCounts.get(table);
                    if (sourceCount == null) {
                        continue;
                    }
                    try {
                        long targetCount = count(conn, table, target.getUrl());
                        if (targetCount != sourceCount) {
                            issues.add(ConsistencyIssueVO.mismatch(target.getName(), table, sourceCount, targetCount));
                        }
                    } catch (SQLException ex) {
                        // 单表查询失败（如表缺失）不影响其它表继续比对
                        issues.add(ConsistencyIssueVO.queryFailed(target.getName(), table, ex.getMessage()));
                    }
                }
            } catch (SQLException ex) {
                log.warn("巡检：目标库 {} 不可达 - {}", target.getName(), ex.getMessage());
                issues.add(ConsistencyIssueVO.unreachable(target.getName(), ex.getMessage()));
            }
        }
        return issues;
    }

    @Override
    public PatrolResultVO patrol() {
        List<ConsistencyIssueVO> issues = check();
        boolean alertSent = false;
        if (!issues.isEmpty()) {
            alertSent = alertService.alert("consistency", "数据一致性巡检发现异常", buildAlertContent(issues));
            lastRunHadIssues.set(true);
        } else if (lastRunHadIssues.compareAndSet(true, false)) {
            // 上次异常、本次恢复：发一条恢复通知（不同的告警键，不受异常告警的节流影响）
            alertService.alert("consistency-recovered", "数据一致性已恢复",
                    "巡检时间：" + LocalDateTime.now().format(TIME_FMT) + "\n"
                            + "源库与各目标库行数已恢复一致，此前的异常已消除。");
        }
        return PatrolResultVO.builder()
                .consistent(issues.isEmpty())
                .alertSent(alertSent)
                .issues(issues)
                .build();
    }

    private Connection open(ConsistencyProperties.DbEndpoint endpoint) throws SQLException {
        // 全局登录超时兜底：URL 上没配 connectTimeout 的驱动也不会无限等待
        DriverManager.setLoginTimeout(5);
        return DriverManager.getConnection(endpoint.getUrl(), endpoint.getUsername(), endpoint.getPassword());
    }

    private long count(Connection conn, String table, String url) throws SQLException {
        String quoted = quoteIdentifier(table, url);
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("select count(*) from " + quoted)) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    /**
     * 按方言给表名加引号：PostgreSQL 的 {@code user} 是保留字必须加双引号；
     * MySQL / MariaDB / H2(MySQL 模式) 可直接使用裸表名。
     */
    private String quoteIdentifier(String table, String url) {
        if (url != null && url.startsWith("jdbc:postgresql")) {
            return "\"" + table.replace("\"", "\"\"") + "\"";
        }
        return table;
    }

    private String buildAlertContent(List<ConsistencyIssueVO> issues) {
        StringBuilder sb = new StringBuilder();
        sb.append("巡检时间：").append(LocalDateTime.now().format(TIME_FMT)).append('\n');
        sb.append("源库：").append(properties.getSource().getUrl()).append('\n');
        sb.append("发现 ").append(issues.size()).append(" 处异常：\n");
        for (ConsistencyIssueVO issue : issues) {
            sb.append("  - [").append(issue.getTarget()).append(']');
            if (issue.getTable() != null) {
                sb.append(' ').append(issue.getTable());
            }
            sb.append("：").append(issue.getDetail());
            if (issue.getSourceCount() != null && issue.getTargetCount() != null) {
                sb.append("（源库 ").append(issue.getSourceCount())
                        .append(" 行 / 目标库 ").append(issue.getTargetCount()).append(" 行）");
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
