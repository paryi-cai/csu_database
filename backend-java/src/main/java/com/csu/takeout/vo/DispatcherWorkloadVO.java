package com.csu.takeout.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 配送员工作量条目（/api/manager/stats/dispatchers）。
 *
 * <p>数据来自 {@code wuliu}（配送记录）按 {@code disp_id} 聚合，再关联
 * {@code dispatcher} 取姓名与电话。用 {@code left join} 保证"新入职还没派过单"
 * 的配送员也会出现在对比里（数量为 0），否则管理者看不到"谁一直没接单"。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"disp_id", "disp_name", "phone", "sending_count", "sended_count", "total"})
public class DispatcherWorkloadVO implements Serializable {

    /** 配送员编号 */
    private String dispId;

    /** 配送员姓名 */
    private String dispName;

    /** 联系电话 */
    private String phone;

    /** 在途（wuliu.ended = 0）单量 */
    private Long sendingCount;

    /** 已完成（wuliu.ended = 1）单量 */
    private Long sendedCount;

    /** 合计单量 */
    private Long total;
}
