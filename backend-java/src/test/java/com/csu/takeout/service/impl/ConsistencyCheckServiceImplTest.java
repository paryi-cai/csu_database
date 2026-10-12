package com.csu.takeout.service.impl;

import com.csu.takeout.properties.ConsistencyProperties;
import com.csu.takeout.service.AlertService;
import com.csu.takeout.vo.ConsistencyIssueVO;
import com.csu.takeout.vo.PatrolResultVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 一致性巡检单元测试（用两个 H2 内存库模拟源库与目标库，不依赖 Docker / MySQL）。
 */
class ConsistencyCheckServiceImplTest {

    /** 记录调用的假告警服务 */
    static class RecordingAlertService implements AlertService {

        final List<String> alertKeys = new ArrayList<>();

        @Override
        public boolean alert(String alertKey, String subject, String content) {
            alertKeys.add(alertKey);
            return true;
        }

        @Override
        public boolean alertNow(String subject, String content) {
            alertKeys.add("now");
            return true;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    }

    private final String suffix = UUID.randomUUID().toString().replace("-", "");

    private final String sourceUrl = "jdbc:h2:mem:cons_src_" + suffix + ";DB_CLOSE_DELAY=-1";

    private final String targetUrl = "jdbc:h2:mem:cons_tgt_" + suffix + ";DB_CLOSE_DELAY=-1";
    private final String target2Url = "jdbc:h2:mem:cons_tgt2_" + suffix + ";DB_CLOSE_DELAY=-1";

    @AfterEach
    void tearDown() throws Exception {
        // H2 内存库随 JVM 存活；主动清掉表避免同 JVM 内其它用例意外复用同名库
        for (String url : new String[]{sourceUrl, targetUrl, target2Url}) {
            try (Connection conn = DriverManager.getConnection(url, "sa", "");
                 Statement stmt = conn.createStatement()) {
                stmt.execute("drop all objects");
            }
        }
    }

    private void execute(String url, String... sqls) throws Exception {
        try (Connection conn = DriverManager.getConnection(url, "sa", "");
             Statement stmt = conn.createStatement()) {
            for (String sql : sqls) {
                stmt.execute(sql);
            }
        }
    }

    private ConsistencyProperties props() {
        ConsistencyProperties props = new ConsistencyProperties();
        ConsistencyProperties.DbEndpoint source = new ConsistencyProperties.DbEndpoint();
        source.setName("MySQL(源库)");
        source.setUrl(sourceUrl);
        source.setUsername("sa");
        source.setPassword("");
        props.setSource(source);

        ConsistencyProperties.DbEndpoint target = new ConsistencyProperties.DbEndpoint();
        target.setName("H2Target");
        target.setUrl(targetUrl);
        target.setUsername("sa");
        target.setPassword("");
        props.setTargets(List.of(target));
        props.setTables(List.of("probe"));
        return props;
    }

    @Test
    @DisplayName("行数一致：无任何异常")
    void consistentWhenCountsMatch() throws Exception {
        execute(sourceUrl, "create table probe(id int)", "insert into probe values (1), (2)");
        execute(targetUrl, "create table probe(id int)", "insert into probe values (1), (2)");

        ConsistencyCheckServiceImpl service = new ConsistencyCheckServiceImpl(props(), new RecordingAlertService());
        assertTrue(service.check().isEmpty());
    }

    @Test
    @DisplayName("行数不一致：给出目标库名、表名与两边行数")
    void mismatchReportedWithCounts() throws Exception {
        execute(sourceUrl, "create table probe(id int)", "insert into probe values (1), (2)");
        execute(targetUrl, "create table probe(id int)", "insert into probe values (1)");

        ConsistencyCheckServiceImpl service = new ConsistencyCheckServiceImpl(props(), new RecordingAlertService());
        List<ConsistencyIssueVO> issues = service.check();

        assertEquals(1, issues.size());
        ConsistencyIssueVO issue = issues.get(0);
        assertEquals("H2Target", issue.getTarget());
        assertEquals("probe", issue.getTable());
        assertEquals(2L, issue.getSourceCount());
        assertEquals(1L, issue.getTargetCount());
        assertEquals("行数不一致", issue.getDetail());
    }

    @Test
    @DisplayName("目标表缺失：报查询失败而非中断整轮巡检")
    void missingTableReportedAsQueryFailure() throws Exception {
        execute(sourceUrl, "create table probe(id int)", "insert into probe values (1)");
        // 目标库不建表

        ConsistencyCheckServiceImpl service = new ConsistencyCheckServiceImpl(props(), new RecordingAlertService());
        List<ConsistencyIssueVO> issues = service.check();

        assertEquals(1, issues.size());
        assertEquals("H2Target", issues.get(0).getTarget());
        assertTrue(issues.get(0).getDetail().startsWith("查询失败："));
    }

    @Test
    @DisplayName("目标库不可达：报不可达并保留源库统计")
    void unreachableTargetReported() throws Exception {
        execute(sourceUrl, "create table probe(id int)", "insert into probe values (1)");

        ConsistencyProperties props = props();
        ConsistencyProperties.DbEndpoint unreachable = new ConsistencyProperties.DbEndpoint();
        unreachable.setName("UnreachableTarget");
        // 指向一个没有服务监听的端口，连接被拒绝
        unreachable.setUrl("jdbc:h2:tcp://127.0.0.1:1/mem:nowhere");
        unreachable.setUsername("sa");
        unreachable.setPassword("");
        props.setTargets(List.of(unreachable));

        ConsistencyCheckServiceImpl service = new ConsistencyCheckServiceImpl(props, new RecordingAlertService());
        List<ConsistencyIssueVO> issues = service.check();

        assertEquals(1, issues.size());
        assertEquals("UnreachableTarget", issues.get(0).getTarget());
        assertTrue(issues.get(0).getDetail().startsWith("库不可达："));
    }

    @Test
    @DisplayName("patrol：发现异常触发告警；恢复后发送恢复通知")
    void patrolAlertsAndSendsRecovery() throws Exception {
        execute(sourceUrl, "create table probe(id int)", "insert into probe values (1), (2)");
        execute(targetUrl, "create table probe(id int)", "insert into probe values (1)");

        RecordingAlertService alerts = new RecordingAlertService();
        ConsistencyCheckServiceImpl service = new ConsistencyCheckServiceImpl(props(), alerts);

        PatrolResultVO first = service.patrol();
        assertFalse(first.getConsistent());
        assertEquals(List.of("consistency"), alerts.alertKeys);

        // 修复数据（补齐目标库行数）→ 再巡检应发恢复通知
        execute(targetUrl, "insert into probe values (2)");
        PatrolResultVO second = service.patrol();
        assertTrue(second.getConsistent());
        assertEquals(2, alerts.alertKeys.size());
        assertEquals("consistency-recovered", alerts.alertKeys.get(1));
    }
}
