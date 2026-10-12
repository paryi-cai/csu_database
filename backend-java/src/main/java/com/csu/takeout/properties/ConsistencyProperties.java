package com.csu.takeout.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据一致性巡检配置，对应 application.yml 中的 csu.consistency.*
 *
 * <p>巡检逻辑：把「源库（业务 MySQL）」各表的数据行数与每个「目标库
 * （DataX 迁移后的 MariaDB / PostgreSQL）」逐一对比，出现行数不一致、
 * 表缺失或目标库不可达时触发告警。目标库只在跨库迁移演示时才需要启动，
 * 因此巡检默认关闭（{@code enabled=false}），手动触发接口不受该开关限制。</p>
 */
@Component
@ConfigurationProperties(prefix = "csu.consistency")
@Data
public class ConsistencyProperties {

    /** 定时巡检开关（手动触发 /api/manager/alert/patrol 不受此开关限制） */
    private boolean enabled = false;

    /** 巡检间隔（毫秒），默认 5 分钟 */
    private long intervalMs = 300_000L;

    /** 首次执行延迟（毫秒）：给应用与容器留出启动时间 */
    private long initialDelayMs = 60_000L;

    /** 源库（业务 MySQL） */
    private DbEndpoint source = new DbEndpoint();

    /** 目标库列表（MariaDB / PostgreSQL） */
    private List<DbEndpoint> targets = new ArrayList<>();

    /** 参与比对的表 */
    private List<String> tables = new ArrayList<>();

    /** 是否已配置好源库与至少一个目标库 */
    public boolean isConfigured() {
        return source != null && StringUtils.hasText(source.getUrl()) && !targets.isEmpty();
    }

    /** 单个数据库连接配置 */
    @Data
    public static class DbEndpoint {

        /** 展示名称（告警文案中显示） */
        private String name;

        /** JDBC URL（建议带 connectTimeout，巡检失败要快速返回） */
        private String url;

        private String username;

        private String password;
    }
}
