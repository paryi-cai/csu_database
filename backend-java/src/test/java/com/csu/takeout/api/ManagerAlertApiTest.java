package com.csu.takeout.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 异常告警接口的契约测试。
 *
 * <p>覆盖：未携带 token 一律 401（新增接口纳入鉴权）；测试告警在未启用时的业务失败响应；
 * 一致性巡检接口的两种结果（一致 / 不一致）。巡检用两个 H2 内存库模拟源库与目标库
 * （URL 见 application-h2test.yml），断言的是响应体原文。</p>
 */
class ManagerAlertApiTest extends ApiContractTestSupport {

    private static final String TEST_ALERT_URL = "/api/manager/alert/test";
    private static final String PATROL_URL = "/api/manager/alert/patrol";

    private static final String SOURCE_URL = "jdbc:h2:mem:consistency_src;DB_CLOSE_DELAY=-1";
    private static final String TARGET_URL = "jdbc:h2:mem:consistency_tgt;DB_CLOSE_DELAY=-1";

    @AfterEach
    void cleanupProbe() throws Exception {
        dropProbe(SOURCE_URL);
        dropProbe(TARGET_URL);
    }

    private void dropProbe(String url) throws Exception {
        try (Connection conn = DriverManager.getConnection(url, "sa", "");
             Statement stmt = conn.createStatement()) {
            stmt.execute("drop table if exists probe");
        }
    }

    private void createProbe(String url, int rows) throws Exception {
        try (Connection conn = DriverManager.getConnection(url, "sa", "");
             Statement stmt = conn.createStatement()) {
            stmt.execute("create table probe(id int)");
            for (int i = 1; i <= rows; i++) {
                stmt.execute("insert into probe values (" + i + ")");
            }
        }
    }

    @Test
    @DisplayName("两个告警接口未携带 token 均返回 401")
    void requiresToken() {
        ResponseEntity<String> test = postJson(TEST_ALERT_URL, "{}");
        assertEquals(HttpStatus.UNAUTHORIZED, test.getStatusCode());
        assertEquals("{\"code\": 1000, \"msg\": \"" + esc("用户未登录") + "\"}\n", test.getBody());

        ResponseEntity<String> patrol = postJson(PATROL_URL, "{}");
        assertEquals(HttpStatus.UNAUTHORIZED, patrol.getStatusCode());
        assertEquals("{\"code\": 1000, \"msg\": \"" + esc("用户未登录") + "\"}\n", patrol.getBody());
    }

    @Test
    @DisplayName("POST /api/manager/alert/test：告警未启用时返回 1000 与配置提示")
    void testAlertDisabled() {
        ResponseEntity<String> response = authorizedPost(TEST_ALERT_URL, loginAsUser(), "{}");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\": 1000, \"msg\": \"" + esc("告警未启用或发送失败：请检查 csu.alert.* 与 spring.mail.* 配置") + "\"}\n",
                response.getBody());
    }

    @Test
    @DisplayName("POST /api/manager/alert/patrol：行数一致时 consistent=true、无异常")
    void patrolConsistent() throws Exception {
        createProbe(SOURCE_URL, 2);
        createProbe(TARGET_URL, 2);

        ResponseEntity<String> response = authorizedPost(PATROL_URL, loginAsUser(), "{}");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\": 200, \"data\": {\"consistent\": true, \"alert_sent\": false, \"issues\": []}}\n",
                response.getBody());
    }

    @Test
    @DisplayName("POST /api/manager/alert/patrol：行数不一致时输出目标库、表名与两边行数")
    void patrolMismatch() throws Exception {
        createProbe(SOURCE_URL, 3);
        createProbe(TARGET_URL, 1);

        ResponseEntity<String> response = authorizedPost(PATROL_URL, loginAsUser(), "{}");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("{\"status\": 200, \"data\": {\"consistent\": false, \"alert_sent\": false, \"issues\": ["
                        + "{\"target\": \"H2Target\", \"table\": \"probe\", \"source_count\": 3, "
                        + "\"target_count\": 1, \"detail\": \"" + esc("行数不一致") + "\"}]}}\n",
                response.getBody());
    }

    @Test
    @DisplayName("POST /api/manager/alert/patrol：目标表缺失时给出查询失败原因")
    void patrolMissingTargetTable() throws Exception {
        createProbe(SOURCE_URL, 1);
        // 目标库不建表（库存在但无 probe 表）→ 单表查询失败
        ResponseEntity<String> response = authorizedPost(PATROL_URL, loginAsUser(), "{}");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("\"target\": \"H2Target\""), response.getBody());
        assertTrue(response.getBody().contains("\"" + esc("查询失败：")), response.getBody());
    }
}
