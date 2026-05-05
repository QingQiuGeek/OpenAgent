package com.qingqiu.openagent.util;

/**
 * 登录用户上下文（线程局部变量）。由 {@link com.qingqiu.openagent.interceptor.SaTokenInterceptor} 注入，
 * 业务层使用 {@link #getUser()} 获取当前登录用户 id。
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void saveUser(Long userId) {
        USER_HOLDER.set(userId);
    }

    public static Long getUser() {
        return USER_HOLDER.get();
    }

    public static void removeUser() {
        USER_HOLDER.remove();
    }
}
