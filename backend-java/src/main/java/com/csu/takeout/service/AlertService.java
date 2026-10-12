package com.csu.takeout.service;

/**
 * 异常告警服务。
 *
 * <p>两条使用路径：</p>
 * <ul>
 *   <li>{@link #alert(String, String, String)}：自动告警（运行期异常、巡检异常）。
 *       带<b>节流</b>（同一告警键在配置窗口内只发一封）与<b>异步发送</b>（不阻塞业务线程）；</li>
 *   <li>{@link #alertNow(String, String)}：手动测试告警，不受节流限制、同步返回发送结果。</li>
 * </ul>
 *
 * <p>告警全程不抛异常：未启用、SMTP 未配置或发送失败都只写日志，保证告警链路
 * 永远不会影响业务流程。</p>
 */
public interface AlertService {

    /**
     * 发送自动告警（带节流，异步）。
     *
     * @param alertKey 告警键：相同键在节流窗口内只发送一次，例如 "unexpected" / "consistency:MariaDB"
     * @param subject  主题（会自动加上配置的主题前缀）
     * @param content  正文
     * @return 是否已受理发送（false = 未启用 / 未配置收件人 / 处于节流窗口）
     */
    boolean alert(String alertKey, String subject, String content);

    /**
     * 立即发送告警（手动测试专用：不做节流，同步返回真实发送结果）。
     */
    boolean alertNow(String subject, String content);

    /** 告警是否已启用（开关打开且配置了收件人） */
    boolean isEnabled();
}
