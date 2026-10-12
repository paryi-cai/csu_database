package com.csu.takeout.service.impl;

import com.csu.takeout.properties.AlertProperties;
import com.csu.takeout.service.AlertService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.LongSupplier;

/**
 * 异常告警服务实现。
 *
 * <p>三个关键设计（面试要点）：</p>
 * <ol>
 *   <li><b>节流</b>：以告警键为粒度记录上次发送时间，窗口内重复出现的异常只发第一封，
 *       避免"数据库抖动导致每秒十个异常"把邮箱打爆；</li>
 *   <li><b>异步</b>：邮件发送走单线程守护线程池，SMTP 抖动（连接超时可达数秒）
 *       不会阻塞请求线程或定时巡检线程；</li>
 *   <li><b>降级不报错</b>：未启用 / 未配置收件人 / SMTP 不可用 / 发送失败，
 *       全部只写日志并返回 false，绝不向上抛异常——告警是旁路，不能影响主业务。</li>
 * </ol>
 */
@Service
@Slf4j
public class AlertServiceImpl implements AlertService {

    private final AlertProperties properties;

    /** SMTP 客户端；未配置 spring.mail.host 时容器里不存在该 Bean，用 ObjectProvider 优雅降级 */
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    private final Executor executor;

    /** 自建的线程池（测试注入同步 Executor 时为 null） */
    private final ExecutorService ownedExecutor;

    /** 可注入时钟（测试可控）；生产用系统时间 */
    private final LongSupplier clock;

    /** 告警键 -> 上次发送时间（毫秒）。键的数量与业务规模相关且极少，无需清理策略 */
    private final ConcurrentHashMap<String, Long> lastSentAt = new ConcurrentHashMap<>();

    @Autowired
    public AlertServiceImpl(AlertProperties properties, ObjectProvider<JavaMailSender> mailSenderProvider) {
        this(properties, mailSenderProvider, null, System::currentTimeMillis);
    }

    /** 测试友好构造器：注入同步 Executor（如 {@code Runnable::run}）与可控时钟即可获得确定性行为 */
    AlertServiceImpl(AlertProperties properties, ObjectProvider<JavaMailSender> mailSenderProvider,
                     Executor executor, LongSupplier clock) {
        this.properties = properties;
        this.mailSenderProvider = mailSenderProvider;
        this.clock = clock;
        if (executor != null) {
            this.executor = executor;
            this.ownedExecutor = null;
        } else {
            this.ownedExecutor = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "alert-mailer");
                thread.setDaemon(true);
                return thread;
            });
            this.executor = ownedExecutor;
        }
    }

    @PreDestroy
    public void shutdown() {
        if (ownedExecutor != null) {
            ownedExecutor.shutdown();
        }
    }

    @Override
    public boolean alert(String alertKey, String subject, String content) {
        if (!isEnabled()) {
            log.info("告警未启用（csu.alert.enabled=false 或未配置收件人），仅记录：{}", subject);
            return false;
        }
        if (throttled(alertKey)) {
            log.info("告警处于 {} 分钟抑制窗口内，跳过：{}", properties.getThrottleMinutes(), subject);
            return false;
        }
        try {
            executor.execute(() -> doSend(subject, content));
            return true;
        } catch (RejectedExecutionException ex) {
            // 应用关闭期间线程池已停止：丢弃本次告警，绝不能把异常抛给调用方
            // （调用方之一是 GlobalExceptionHandler 的兜底分支，那里抛异常会掩盖原始异常）
            log.warn("告警线程池已关闭，本次告警丢弃：{}", subject);
            return false;
        }
    }

    @Override
    public boolean alertNow(String subject, String content) {
        if (!isEnabled()) {
            log.info("告警未启用（csu.alert.enabled=false 或未配置收件人），仅记录：{}", subject);
            return false;
        }
        return doSend(subject, content);
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled() && StringUtils.hasText(properties.getTo());
    }

    /**
     * 节流判定：同一告警键在窗口内只放行一次。
     * 用 {@code compute} 保证并发下的"检查 + 记录"是原子的（多线程同时告警不会双发）。
     */
    private boolean throttled(String alertKey) {
        long now = clock.getAsLong();
        long windowMs = Math.max(0, properties.getThrottleMinutes()) * 60_000L;
        boolean[] proceed = {false};
        lastSentAt.compute(alertKey, (key, last) -> {
            if (last != null && now - last < windowMs) {
                return last;
            }
            proceed[0] = true;
            return now;
        });
        return !proceed[0];
    }

    /** 实际发送邮件。所有异常就地消化，不向外抛 */
    private boolean doSend(String subject, String content) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.warn("SMTP 未配置（spring.mail.host / MAIL_HOST 为空），告警未发送：{}", subject);
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            String from = StringUtils.hasText(properties.getFrom())
                    ? properties.getFrom()
                    : (sender instanceof JavaMailSenderImpl impl ? impl.getUsername() : null);
            if (StringUtils.hasText(from)) {
                message.setFrom(from);
            }
            message.setTo(properties.getTo().split("\\s*,\\s*"));
            message.setSubject(properties.getSubjectPrefix() + " " + subject);
            message.setText(content);
            sender.send(message);
            log.info("告警邮件已发送：{} -> {}", subject, properties.getTo());
            return true;
        } catch (Exception ex) {
            log.warn("告警邮件发送失败（已忽略，不影响业务）：{}", ex.getMessage());
            return false;
        }
    }
}
