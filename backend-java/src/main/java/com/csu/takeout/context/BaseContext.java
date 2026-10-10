package com.csu.takeout.context;

/**
 * 基于 ThreadLocal 保存当前请求的登录用户信息。
 *
 * <p>代码组织与 sky-take-out 的 BaseContext 一致；区别在于本项目 Flask 版是
 * <b>以手机号</b>为检索键（所有订单/用户信息查询都用 {@code cons_phone = token 中的 telephone}），
 * 因此这里保存的是手机号字符串，而不是用户 id。</p>
 */
public class BaseContext {

    public static ThreadLocal<String> threadLocal = new ThreadLocal<>();

    public static void setCurrentTelephone(String telephone) {
        threadLocal.set(telephone);
    }

    public static String getCurrentTelephone() {
        return threadLocal.get();
    }

    public static void removeCurrentTelephone() {
        threadLocal.remove();
    }
}
