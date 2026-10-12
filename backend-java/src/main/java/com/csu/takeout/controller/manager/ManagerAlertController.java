package com.csu.takeout.controller.manager;

import com.csu.takeout.constant.MessageConstant;
import com.csu.takeout.result.DataResult;
import com.csu.takeout.result.TableResult;
import com.csu.takeout.service.AlertService;
import com.csu.takeout.service.ConsistencyCheckService;
import com.csu.takeout.vo.PatrolResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 —— 异常检测与告警。
 *
 * <p>两个接口都在 {@code WebMvcConfiguration.JWT_PATH_PATTERNS} 内（新增功能按新要求纳入鉴权）：</p>
 * <ul>
 *   <li>{@code POST /test}：发送一封测试告警邮件，用于验证 SMTP 配置链路；</li>
 *   <li>{@code POST /patrol}：立即执行一次数据一致性巡检并返回明细，发现异常会触发告警。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/manager/alert")
@Tag(name = "管理端-异常告警")
@Slf4j
@RequiredArgsConstructor
public class ManagerAlertController {

    private final AlertService alertService;

    private final ConsistencyCheckService consistencyCheckService;

    /**
     * 发送测试告警邮件。
     *
     * <pre>
     * 成功：   {"status": 200, "msg": "测试告警已发送"}
     * 未启用： {"status": 1000, "msg": "告警未启用或发送失败：请检查 csu.alert.* 与 spring.mail.* 配置"}
     * </pre>
     */
    @PostMapping("/test")
    @Operation(summary = "发送测试告警邮件", description = "验证 SMTP 配置；未启用或发送失败时返回 status=1000")
    public TableResult<String> testAlert() {
        boolean sent = alertService.alertNow("测试告警",
                "这是一封来自校园快餐订餐系统的测试告警邮件。\n收到即说明 SMTP 告警链路已配置成功。");
        if (!sent) {
            return TableResult.error(MessageConstant.ALERT_NOT_SENT);
        }
        return TableResult.success(MessageConstant.ALERT_TEST_SENT);
    }

    /**
     * 立即执行一次数据一致性巡检。
     *
     * <pre>
     * 全部一致：{"status": 200, "data": {"consistent": true, "alert_sent": false, "issues": []}}
     * 发现异常：{"status": 200, "data": {"consistent": false, "alert_sent": true|false, "issues": [...]}}
     * </pre>
     */
    @PostMapping("/patrol")
    @Operation(summary = "立即执行数据一致性巡检", description = "对比源库与各目标库行数；发现异常按节流发送告警，恢复时发送恢复通知")
    public DataResult<PatrolResultVO> patrol() {
        PatrolResultVO result = consistencyCheckService.patrol();
        if (result.getConsistent()) {
            log.info("手动巡检完成：全部一致");
        } else {
            log.info("手动巡检完成：发现 {} 处异常，告警发送={}", result.getIssues().size(), result.getAlertSent());
        }
        return DataResult.success(result);
    }
}
