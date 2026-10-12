package com.csu.takeout.task;

import com.csu.takeout.properties.ConsistencyProperties;
import com.csu.takeout.service.ConsistencyCheckService;
import com.csu.takeout.vo.ConsistencyIssueVO;
import com.csu.takeout.vo.PatrolResultVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 定时巡检任务的开关与配置门禁测试（不启动 Spring、不连数据库）。
 *
 * <p>重点覆盖"假绿"防护：开关打开但源库/目标库未配置完整时必须跳过，
 * 而不是空转比对后误报"全部一致"。</p>
 */
class ConsistencyCheckTaskTest {

    /** 记录调用次数的假巡检服务 */
    static class CountingService implements ConsistencyCheckService {

        int patrolCalls;

        @Override
        public List<ConsistencyIssueVO> check() {
            return List.of();
        }

        @Override
        public PatrolResultVO patrol() {
            patrolCalls++;
            return PatrolResultVO.builder().consistent(true).alertSent(false).issues(List.of()).build();
        }
    }

    private static ConsistencyProperties enabledProps() {
        ConsistencyProperties props = new ConsistencyProperties();
        props.setEnabled(true);
        return props;
    }

    @Test
    @DisplayName("开关关闭：不执行巡检")
    void disabledTaskSkipsPatrol() {
        ConsistencyProperties props = enabledProps();
        props.setEnabled(false);
        CountingService service = new CountingService();

        new ConsistencyCheckTask(props, service).scheduledPatrol();

        assertEquals(0, service.patrolCalls);
    }

    @Test
    @DisplayName("开关打开但源库/目标库未配置：跳过且不误报（防空转假绿），重复调用不抛异常")
    void enabledButUnconfiguredSkipsPatrol() {
        // 默认值：source.url 为空、targets 为空列表 → isConfigured()=false
        ConsistencyProperties props = enabledProps();
        CountingService service = new CountingService();
        ConsistencyCheckTask task = new ConsistencyCheckTask(props, service);

        assertDoesNotThrow(() -> {
            task.scheduledPatrol();
            task.scheduledPatrol();
        });
        assertEquals(0, service.patrolCalls);
    }

    @Test
    @DisplayName("开关打开且配置完整：正常执行一次巡检")
    void configuredTaskPatrols() {
        ConsistencyProperties props = enabledProps();
        props.getSource().setUrl("jdbc:mysql://localhost:3307/caimingxuan");
        props.getTargets().add(new ConsistencyProperties.DbEndpoint());
        CountingService service = new CountingService();

        new ConsistencyCheckTask(props, service).scheduledPatrol();

        assertEquals(1, service.patrolCalls);
    }
}
