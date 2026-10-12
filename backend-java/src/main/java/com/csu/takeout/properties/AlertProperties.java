package com.csu.takeout.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 异常告警配置，对应 application.yml 中的 csu.alert.*
 *
 * <p>默认关闭：只有显式开启并配好收件人后才发送邮件，未配置时所有告警只写日志，
 * 不影响启动与业务。SMTP 连接参数走 Spring 标准配置（spring.mail.*，可用
 * MAIL_HOST / MAIL_USERNAME / MAIL_PASSWORD 环境变量注入）。</p>
 */
@Component
@ConfigurationProperties(prefix = "csu.alert")
@Data
public class AlertProperties {

    /** 总开关：关闭时所有告警只写日志、不发送邮件 */
    private boolean enabled = false;

    /** 发件人；留空时取 SMTP 账号（spring.mail.username） */
    private String from;

    /** 收件人，支持逗号分隔多个 */
    private String to;

    /** 同一告警键的抑制窗口（分钟）：防止异常风暴把邮箱打爆 */
    private int throttleMinutes = 10;

    /** 邮件主题前缀 */
    private String subjectPrefix = "[校园快餐系统]";
}
