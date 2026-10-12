package com.csu.takeout.service;

import com.csu.takeout.vo.ConsistencyIssueVO;
import com.csu.takeout.vo.PatrolResultVO;

import java.util.List;

/**
 * 数据一致性巡检服务：对比源库与各目标库（DataX 迁移结果）的数据行数。
 */
public interface ConsistencyCheckService {

    /**
     * 执行一次纯比对（不发送任何告警）。
     *
     * @return 异常明细列表；空列表表示全部一致
     */
    List<ConsistencyIssueVO> check();

    /**
     * 执行一次巡检并处理告警：
     * <ul>
     *   <li>发现异常 → 触发告警（带节流）；</li>
     *   <li>由"上次异常"转为"本次一致" → 发送一条恢复通知（帮助确认修复生效）。</li>
     * </ul>
     */
    PatrolResultVO patrol();
}
