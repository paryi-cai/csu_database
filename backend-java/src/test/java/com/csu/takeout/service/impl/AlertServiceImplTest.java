package com.csu.takeout.service.impl;

import com.csu.takeout.properties.AlertProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 告警服务单元测试（不启动 Spring、不连 SMTP）。
 *
 * <p>覆盖三件事：开关/收件人的降级行为、同一告警键的节流窗口、
 * 以及"发送失败不影响调用方"的兜底约定。</p>
 */
class AlertServiceImplTest {

    /** 记录发送内容的假 SMTP 实现 */
    static class RecordingMailSender extends JavaMailSenderImpl {

        final List<SimpleMailMessage> sent = new ArrayList<>();

        boolean fail = false;

        @Override
        public void send(SimpleMailMessage simpleMessage) {
            if (fail) {
                throw new org.springframework.mail.MailSendException("smtp down");
            }
            sent.add(simpleMessage);
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) {
            for (SimpleMailMessage message : simpleMessages) {
                send(message);
            }
        }
    }

    private static ObjectProvider<JavaMailSender> providerOf(JavaMailSender sender) {
        return new ObjectProvider<>() {
            @Override
            public JavaMailSender getObject() {
                return sender;
            }

            @Override
            public JavaMailSender getObject(Object... args) {
                return sender;
            }

            @Override
            public JavaMailSender getIfAvailable() {
                return sender;
            }

            @Override
            public JavaMailSender getIfUnique() {
                return sender;
            }
        };
    }

    private static AlertProperties enabledProps() {
        AlertProperties props = new AlertProperties();
        props.setEnabled(true);
        props.setFrom("bot@example.com");
        props.setTo("a@example.com, b@example.com");
        props.setThrottleMinutes(10);
        return props;
    }

    /** 用同步执行器 + 可控时钟构造，保证测试确定性 */
    private static AlertServiceImpl service(AlertProperties props, JavaMailSender sender, AtomicLong clock) {
        return new AlertServiceImpl(props, providerOf(sender), Runnable::run, clock::get);
    }

    @Test
    @DisplayName("未启用时：alertNow 返回 false 且不尝试发送")
    void disabledSkipsSend() {
        RecordingMailSender sender = new RecordingMailSender();
        AlertProperties props = enabledProps();
        props.setEnabled(false);

        AlertServiceImpl service = service(props, sender, new AtomicLong());
        assertFalse(service.alertNow("主题", "正文"));
        assertTrue(sender.sent.isEmpty());
    }

    @Test
    @DisplayName("未配置收件人时：视为未启用")
    void missingRecipientSkipsSend() {
        RecordingMailSender sender = new RecordingMailSender();
        AlertProperties props = enabledProps();
        props.setTo("  ");

        AlertServiceImpl service = service(props, sender, new AtomicLong());
        assertFalse(service.isEnabled());
        assertFalse(service.alertNow("主题", "正文"));
        assertTrue(sender.sent.isEmpty());
    }

    @Test
    @DisplayName("启用时：主题带前缀、收件人按逗号拆分、发件人取自配置")
    void sendsMailWithPrefixAndRecipients() {
        RecordingMailSender sender = new RecordingMailSender();
        AlertProperties props = enabledProps();
        props.setSubjectPrefix("[测试]");

        AlertServiceImpl service = service(props, sender, new AtomicLong());
        assertTrue(service.alertNow("巡检异常", "内容"));

        assertEquals(1, sender.sent.size());
        SimpleMailMessage message = sender.sent.get(0);
        assertEquals("bot@example.com", message.getFrom());
        assertEquals("[测试] 巡检异常", message.getSubject());
        assertEquals("内容", message.getText());
        assertEquals(List.of("a@example.com", "b@example.com"), List.of(message.getTo()));
    }

    @Test
    @DisplayName("节流：同一告警键在窗口内只发一次，窗口过后可再次发送；不同键互不影响")
    void throttlesSameKeyWithinWindow() {
        RecordingMailSender sender = new RecordingMailSender();
        AlertProperties props = enabledProps();
        props.setThrottleMinutes(10);
        AtomicLong clock = new AtomicLong(1_000_000L);

        AlertServiceImpl service = service(props, sender, clock);

        assertTrue(service.alert("job-a", "第 1 次", "x"));
        assertFalse(service.alert("job-a", "第 2 次", "x"), "窗口内应被节流");
        assertTrue(service.alert("job-b", "另一个键", "x"), "不同告警键不受影响");
        assertEquals(2, sender.sent.size());

        // 越过窗口（10 分钟）后同一键可再次发送
        clock.addAndGet(11 * 60_000L);
        assertTrue(service.alert("job-a", "第 3 次", "x"));
        assertEquals(3, sender.sent.size());
    }

    @Test
    @DisplayName("发送失败：不向外抛异常，只返回 false")
    void sendFailureIsSwallowed() {
        RecordingMailSender sender = new RecordingMailSender();
        sender.fail = true;

        AlertServiceImpl service = service(enabledProps(), sender, new AtomicLong());
        assertDoesNotThrow(() -> assertFalse(service.alertNow("主题", "正文")));
        assertTrue(sender.sent.isEmpty());
    }

    @Test
    @DisplayName("SMTP 未配置（容器里没有 JavaMailSender）：降级为只写日志")
    void missingSmtpBeanDegradesGracefully() {
        AlertServiceImpl service = new AlertServiceImpl(enabledProps(), providerOf(null), Runnable::run, System::currentTimeMillis);
        assertDoesNotThrow(() -> assertFalse(service.alertNow("主题", "正文")));
    }
}
