package com.csu.takeout.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * 后端统一返回结果。
 *
 * <p>与 sky-take-out 的代码组织方式一致（Lombok + 静态工厂 + 泛型 data），
 * 但字段名与取值遵循 Flask 版的响应契约：</p>
 * <ul>
 *   <li>登录接口：{"code": 200|1000, "msg": "..."} —— 注意是 {@code code}，且是数字</li>
 *   <li>其余接口：{"status": 200|1000, "msg": "..."} 或 {"status": 200, "tabledata": [...]}</li>
 * </ul>
 * 数据列表接口使用 {@link TableResult}，登录接口使用本类。
 *
 * @param <T> 数据体类型
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    /** 状态码：200 成功；1000 业务失败（与 Flask 版一致，注意是数字而非字符串） */
    private Integer code;

    /** 提示信息 */
    private String msg;

    /** 数据 */
    private T data;

    /** 成功，状态码 200 */
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.code = 200;
        return result;
    }

    /** 成功并携带数据体 */
    public static <T> Result<T> success(T object) {
        Result<T> result = new Result<>();
        result.code = 200;
        result.data = object;
        return result;
    }

    /** 业务失败，状态码 1000 */
    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.code = 1000;
        result.msg = msg;
        return result;
    }
}
