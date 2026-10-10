package com.csu.takeout.handler;

import com.csu.takeout.constant.MessageConstant;
import com.csu.takeout.exception.BaseException;
import com.csu.takeout.exception.UnauthorizedException;
import com.csu.takeout.result.Result;
import com.csu.takeout.result.TableResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，代码组织对齐 sky-takeout 的 GlobalExceptionHandler。
 *
 * <p>关于响应体字段名：除了登录接口用 {@code code} 之外，本项目其余接口的成功与失败
 * 响应都用 {@code status}（接口契约）。因此这里的兜底错误统一使用
 * {@link TableResult}（输出 {@code status}），避免错误响应出现 {@code code} 而
 * 成功响应是 {@code status} 的不一致——前端只判断 {@code status}，
 * 若错误时返回 {@code code}，用户会看不到任何提示。</p>
 *
 * <p><b>关于数据库异常的真实类型</b>：MyBatis-Spring 会通过
 * {@code MyBatisExceptionTranslator} 把 {@code SQLException} 翻译成 Spring 的
 * {@code DataAccessException} 家族，所以实际能捕获到的是
 * {@link DuplicateKeyException}（重复主键/唯一键）与
 * {@link DataIntegrityViolationException}（外键约束等），
 * <b>不是</b>原始的 {@link SQLIntegrityConstraintViolationException}——
 * 后者仅作为兜底保留，正常不会命中。</p>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 未登录 / 令牌非法：返回 HTTP 401。
     *
     * <p>说明：拦截器（{@code preHandle}）中抛出的异常不会进入本处理器，因此
     * {@code JwtTokenInterceptor} 是自己直接写出 401 响应的；本方法用于控制器或
     * 业务层后续可能抛出的同类异常。</p>
     */
    @ExceptionHandler
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> unauthorizedExceptionHandler(UnauthorizedException ex) {
        log.warn("未授权访问：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 捕获业务异常：HTTP 200 + {"status": 1000, "msg": "..."}
     */
    @ExceptionHandler
    public TableResult<Void> baseExceptionHandler(BaseException ex) {
        log.error("业务异常：{}", ex.getMessage());
        return TableResult.error(ex.getMessage());
    }

    /**
     * 唯一键 / 主键冲突（MyBatis-Spring 翻译后的实际类型）。
     *
     * <p>复刻既有的提示思路：既有实现直接执行 INSERT，靠数据库报
     * {@code Duplicate entry 'xxx' for key 'yyy'}；这里从异常信息里取出冲突值拼成提示。
     * 注意：本项目的新增接口都会"先查重再插入"，正常情况下不会走到这里。</p>
     */
    @ExceptionHandler
    public TableResult<Void> duplicateKeyExceptionHandler(DuplicateKeyException ex) {
        log.error("唯一键冲突：{}", ex.getMessage());
        return TableResult.error(duplicateMessage(ex.getMessage()));
    }

    /**
     * 数据库完整性约束异常（外键引用等），由 MyBatis-Spring 翻译而来。
     *
     * <p>典型场景：删除仍被 {@code server.fastfood_shop_name} 外键引用的店铺。
     * 既有接口此时直接返回 500；新实现收敛为 HTTP 200 + 业务失败响应，
     * 前端不会因为 500 而丢失提示。</p>
     */
    @ExceptionHandler
    public TableResult<Void> dataIntegrityViolationExceptionHandler(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        log.error("数据库完整性约束异常：{}", message);

        if (message != null && message.contains("Duplicate entry")) {
            return TableResult.error(duplicateMessage(message));
        }
        return TableResult.error(MessageConstant.UNKNOWN_ERROR);
    }

    /**
     * 兜底：未经 Spring 翻译的原始 JDBC 约束异常（保留以防直连 JDBC 的场景）
     */
    @ExceptionHandler
    public TableResult<Void> sqlExceptionHandler(SQLIntegrityConstraintViolationException ex) {
        log.error("JDBC 约束异常：{}", ex.getMessage());
        if (ex.getMessage() != null && ex.getMessage().contains("Duplicate entry")) {
            return TableResult.error(duplicateMessage(ex.getMessage()));
        }
        return TableResult.error(MessageConstant.UNKNOWN_ERROR);
    }

    /**
     * 请求体缺失或格式错误（例如请求字段名与后端不一致）
     */
    @ExceptionHandler
    public TableResult<Void> httpMessageNotReadableExceptionHandler(HttpMessageNotReadableException ex) {
        log.error("请求体解析失败：{}", ex.getMessage());
        return TableResult.error(MessageConstant.UNKNOWN_ERROR);
    }

    /* ==================== 框架级错误：返回真实 HTTP 状态码 ==================== *
     *
     * 为什么单独处理：下面的 {@link #exceptionHandler(Exception)} 会把**所有**未捕获异常
     * 收成 HTTP 200 + {"status":1000,"msg":"未知错误"}。对业务异常这是刻意的（既有接口也是
     * 200 + msg，前端只读 status），但 Spring MVC 自己抛出的框架级异常（路径不存在、
     * 方法不允许、参数类型不匹配）也走同一条路，就会出现下面的怪现象：
     *
     *   GET /definitely-not-exist                → 200 + {"status":1000,...}
     *   GET /api/user/register/test（已注释路由）  → 200 + {"status":1000,...}
     *   POST /api/user/shop（方法不允许）          → 200 + {"status":1000,...}
     *   GET /api/manager/stats/trend?days=abc    → 200 + {"status":1000,...}
     *
     * 后果是反向代理、浏览器 devtools、监控和调用方全都无法区分"接口不存在"与
     * "业务失败"，且 README 声称"既有接口为 404，行为一致"（未注册的路由确实
     * 返回 404）与实际不符。这里为三类框架级错误返回真实状态码。
     *
     * 响应体选择空体（<b>不</b>再包一层 status/msg）：
     *   1. 这三类错误的响应本来就不是业务契约的一部分，不需要给前端解析；
     *   2. 避免与"HTTP 200 + status"的既有契约混淆——看到 4xx 就是调用姿势不对，
     *      看到 200 就一定是业务响应，判定规则变得单一。
     * 现有前端不受影响：它只对这些路径发正确的方法，不会命中这三类分支。
     *
     * 注意：请求体解析失败（字段名写错、多传未声明字段）**仍然**返回
     * 200 + {"status":1000,"msg":"未知错误"}（见上面的 handler）——那是请求"打到了"
     * 接口但内容不合契约，属于业务响应的一种，已被 UserMsgApiTest 等用例锁定。
     */

    /** 路径不存在 → 404 */
    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void noResourceFoundExceptionHandler(NoResourceFoundException ex) {
        log.warn("路径不存在：{}", ex.getResourcePath());
    }

    /** HTTP 方法不允许 → 405（并带上 Allow 响应头，方便调用方自查） */
    @ExceptionHandler
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ResponseEntity<Void> httpRequestMethodNotSupportedExceptionHandler(
            HttpRequestMethodNotSupportedException ex) {
        log.warn("HTTP 方法不支持：{}", ex.getMessage());
        HttpHeaders headers = new HttpHeaders();
        if (ex.getSupportedHttpMethods() != null) {
            headers.setAllow(ex.getSupportedHttpMethods());
        }
        return new ResponseEntity<>(headers, HttpStatus.METHOD_NOT_ALLOWED);
    }

    /** 查询参数类型不匹配（例如 days=abc）→ 400，而不是静默当成默认值 */
    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void methodArgumentTypeMismatchExceptionHandler(MethodArgumentTypeMismatchException ex) {
        log.warn("参数类型不匹配：name={}, value={}", ex.getName(), ex.getValue());
    }

    /**
     * 兜底异常处理，避免向前端返回 HTML 错误页
     */
    @ExceptionHandler
    public TableResult<Void> exceptionHandler(Exception ex) {
        log.error("系统异常", ex);
        return TableResult.error(MessageConstant.UNKNOWN_ERROR);
    }

    /**
     * 从 {@code Duplicate entry 'xxx' for key 'yyy'} 中取出冲突值，拼成提示信息
     */
    private String duplicateMessage(String message) {
        if (message == null) {
            return MessageConstant.UNKNOWN_ERROR;
        }
        int start = message.indexOf("Duplicate entry '");
        if (start < 0) {
            return MessageConstant.UNKNOWN_ERROR;
        }
        start += "Duplicate entry '".length();
        int end = message.indexOf('\'', start);
        if (end < 0) {
            return MessageConstant.UNKNOWN_ERROR;
        }
        return message.substring(start, end) + MessageConstant.UNKNOWN_ERROR;
    }
}
