package com.csu.takeout.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * {@code {"status": 200, "data": {...}}} 形式的响应，专用于 /api/user/usermsg。
 *
 * <p>既有接口该接口的响应键是 {@code status} 与 {@code data}（data 是对象而不是数组），
 * 与列表接口的 {@code tabledata} 不同，因此单独建类以保证响应体不多输出任何键。</p>
 *
 * @param <T> data 的类型
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataResult<T> implements Serializable {

    /** 状态码：200 成功 */
    private Integer status;

    /** 数据对象 */
    private T data;

    public static <T> DataResult<T> success(T data) {
        DataResult<T> result = new DataResult<>();
        result.status = 200;
        result.data = data;
        return result;
    }
}
