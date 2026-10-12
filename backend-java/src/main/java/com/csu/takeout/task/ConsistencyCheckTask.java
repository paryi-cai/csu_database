package com.csu.takeout.task;

import com.csu.takeout.properties.ConsistencyProperties;
import com.csu.takeout.service.ConsistencyCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 数据一致性定时巡检任务。
 *
 * <p>由 {@code csu.consistency.enabled} 控制开关（默认关闭，因为目标库只在
 * 跨库迁移演示时才启动）；间隔与首次延迟均可在 application.yml 中调整。
 * 手动巡检走 {@code POST /api/manager/alert/patrol}，不受该开关限制。</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConsistencyCheckTask {

    private final ConsistencyProperties properties;

    private final ConsistencyCheckService consistencyCheckService;

    @Scheduled(fixedDelayString = "${csu.consistency.interval-ms:300000}",
            initialDelayString = "${csu.consistency.initial-delay-ms:60000}")
    public void scheduledPatrol() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            var result = consistencyCheckService.patrol();
            log.info("定时巡检完成：{}，告警发送：{}", result.getConsistent() ? "全部一致" : "发现异常", result.getAlertSent());
        } catch (Exception ex) {
            // 定时任务里的异常不能让调度线程"暗中死亡"，记录后等下一轮
            log.error("定时巡检执行失败：{}", ex.getMessage());
        }
    }
}
