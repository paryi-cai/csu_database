package com.csu.takeout.interceptor;

import com.csu.takeout.constant.MessageConstant;
import com.csu.takeout.context.BaseContext;
import com.csu.takeout.dto.JwtUserData;
import com.csu.takeout.json.UnifiedJsonObjectMapper;
import com.csu.takeout.properties.JwtProperties;
import com.csu.takeout.result.Result;
import com.csu.takeout.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * JWT 令牌校验拦截器（jakarta.servlet.*）。
 *
 * <p>结构与 sky-take-out 的 JwtTokenUserInterceptor 一致，差异点：</p>
 * <ul>
 *   <li>令牌从自定义请求头 {@code token} 中取（不是 {@code Authorization: Bearer}）；</li>
 *   <li>BaseContext 中保存的是<b>手机号</b>（既有接口所有业务查询都以手机号为键）；</li>
 *   <li>校验失败时直接写出 401 响应（与 sky 的 {@code response.setStatus(401); return false;}
 *       同一个思路），而不是抛异常。</li>
 * </ul>
 *
 * <p><b>为什么校验失败不抛异常：</b>实测确认，在 {@code preHandle} 中抛出的异常
 * <b>不会</b>被 {@code @RestControllerAdvice} 的全局异常处理器捕获——它会绕过
 * DispatcherServlet 的异常解析链，最终落到 {@code BasicErrorController}，
 * 导致返回 200 + "未知错误"（因为兜底的 {@code @ExceptionHandler(Exception.class)}
 * 只对控制器方法内部抛出的异常生效）。因此这里显式写响应，行为可控且与 sky 一致。</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtTokenInterceptor implements HandlerInterceptor {

    private final JwtProperties jwtProperties;

    /** 401 响应体使用与业务响应一致的 统一响应排版 */
    private final UnifiedJsonObjectMapper jsonMapper = new UnifiedJsonObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        // 判断当前拦截到的是 Controller 的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            // 当前拦截到的不是动态方法，直接放行（如 doc.html、静态资源）
            return true;
        }

        // 1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getTokenName());
        if (!StringUtils.hasText(token)) {
            log.info("请求头 {} 中未携带令牌，拒绝访问：{}", jwtProperties.getTokenName(), request.getRequestURI());
            return reject(response, MessageConstant.USER_NOT_LOGIN);
        }

        // 2、校验令牌（签名、过期时间、签发者）
        JwtUserData userData;
        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getSecretKey(), jwtProperties.getIssuer(), token);
            userData = JwtUtil.getUserData(claims);
        } catch (Exception ex) {
            log.info("令牌校验失败（{}），拒绝访问：{}", ex.getMessage(), request.getRequestURI());
            return reject(response, MessageConstant.USER_NOT_LOGIN);
        }

        if (userData == null || !StringUtils.hasText(userData.getTelephone())) {
            log.info("令牌载荷缺少 data.telephone，拒绝访问：{}", request.getRequestURI());
            return reject(response, MessageConstant.USER_NOT_LOGIN);
        }

        // 3、通过，将手机号放入 ThreadLocal 供业务层使用
        BaseContext.setCurrentTelephone(userData.getTelephone());
        log.info("jwt 校验通过，当前用户手机号：{}", userData.getTelephone());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束清理 ThreadLocal，避免线程复用导致的串号
        BaseContext.removeCurrentTelephone();
    }

    /**
     * 返回 401（HTTP 状态码 401 + JSON 错误体）
     */
    private boolean reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonMapper.toUnifiedJson(Result.error(message)));
        return false;
    }
}
