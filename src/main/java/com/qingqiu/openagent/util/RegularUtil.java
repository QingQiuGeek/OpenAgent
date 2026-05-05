package com.qingqiu.openagent.util;

import java.util.regex.Pattern;

/**
 * 常用正则校验工具。方法语义：校验失败返回 true（配合 {@code !isXxx} 风格判空后取反，沿用历史风格）。
 */
public final class RegularUtil {

    private static final Pattern MAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    // 6~32 位，字母/数字/常见符号
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^[A-Za-z0-9!@#$%^&*()_+\\-=.,:;\\[\\]{}|~]{6,32}$"
    );

    private RegularUtil() {
    }

    /**
     * @return true 表示邮箱格式非法
     */
    public static boolean checkMail(String mail) {
        if (mail == null || mail.isBlank()) {
            return true;
        }
        return !MAIL_PATTERN.matcher(mail).matches();
    }

    /**
     * @return true 表示密码格式非法
     */
    public static boolean checkPassword(String password) {
        if (password == null || password.isBlank()) {
            return true;
        }
        return !PASSWORD_PATTERN.matcher(password).matches();
    }
}
