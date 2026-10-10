package com.csu.takeout.result;

import com.csu.takeout.vo.DispatcherIdVO;
import com.csu.takeout.vo.ShopNameVO;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 列表类接口的统一返回结构，对齐响应结构 {@code JSON(status=200, tabledata=Data, ...)}。
 *
 * <p>既有接口共 9 个接口使用 {@code status} + {@code tabledata} 结构，其中 2 个额外返回
 * 下拉候选数据（{@code disp_range} / {@code shop_range}）。为做到"不多输出任何一个键"，
 * 这里对 null 字段做忽略处理，并按需要填充额外字段。</p>
 *
 * @param <T> 列表元素类型
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TableResult<T> implements Serializable {

    /** 状态码：200 成功；1000 业务失败 */
    private Integer status;

    /** 提示信息（列表接口成功时不返回该字段） */
    private String msg;

    /** 数据列表 */
    private List<T> tabledata;

    /**
     * 配送员编号候选列表（仅 /api/manager/unsend 使用），元素形如 {"disp_id": "010100"}。
     *
     * <p>Java 属性名 {@code dispRange} 经全局下划线命名策略会变成 {@code disp_range}，
     * 这里显式标注以免依赖命名策略的细节。</p>
     */
    @JsonProperty("disp_range")
    private List<DispatcherIdVO> dispRange;

    /**
     * 店铺名称候选列表（仅 /api/manager/server 使用），元素形如 {"shop_name": "大美鸡公煲"}。
     */
    @JsonProperty("shop_range")
    private List<ShopNameVO> shopRange;

    /** 成功 + 列表数据 */
    public static <T> TableResult<T> success(List<T> tabledata) {
        TableResult<T> result = new TableResult<>();
        result.status = 200;
        result.tabledata = tabledata;
        return result;
    }

    /** 成功 + 提示信息（新增/修改/删除类接口） */
    public static <T> TableResult<T> success(String msg) {
        return message(200, msg);
    }

    /** 业务失败 + 提示信息 */
    public static <T> TableResult<T> error(String msg) {
        return message(1000, msg);
    }

    private static <T> TableResult<T> message(Integer status, String msg) {
        TableResult<T> result = new TableResult<>();
        result.status = status;
        result.msg = msg;
        return result;
    }
}
