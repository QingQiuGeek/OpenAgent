package com.qingqiu.openagent.util;

/**
 * @author: qingqiugeek
 * @date: 2026/5/12 08:41
 * @description: UserContext
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
