package com.csu.takeout.task;

import com.csu.takeout.properties.ConsistencyProperties;
import com.csu.takeout.service.ConsistencyCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

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

    /** 配置不完整只提示一次，避免每轮巡检重复刷日志 */
    private final AtomicBoolean configWarned = new AtomicBoolean();

    @Scheduled(fixedDelayString = "${csu.consistency.interval-ms:300000}",
            initialDelayString = "${csu.consistency.initial-delay-ms:60000}")
    public void scheduledPatrol() {
        if (!properties.isEnabled()) {
            return;
        }
        if (!properties.isConfigured()) {
            // 只开开关没配源库/目标库时，比对循环会空转并误报"全部一致"，这里直接跳过
            if (configWarned.compareAndSet(false, true)) {
                log.warn("定时巡检已启用，但源库或目标库未配置完整，跳过本轮（配置齐备后自动恢复执行）");
            }
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
