package com.csu.takeout.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务开关。
 *
 * <p>目前只有数据一致性巡检一个定时任务（{@link com.csu.takeout.task.ConsistencyCheckTask}），
 * 它默认不执行（{@code csu.consistency.enabled=false}），开启后才按配置间隔运行。</p>
 */
@Configuration
@EnableScheduling
public class SchedulingConfiguration {
}
