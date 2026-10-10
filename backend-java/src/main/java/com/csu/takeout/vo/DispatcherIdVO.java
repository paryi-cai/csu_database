package com.csu.takeout.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 配送员编号候选项，用于 /api/manager/unsend 的 {@code disp_range}。
 *
 * <p>Flask 版返回的元素形如 {@code {"disp_id": "010100"}}（只含编号，不含姓名）。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatcherIdVO implements Serializable {

    /** 响应键 disp_id */
    @JsonProperty("disp_id")
    private String dispId;
}
