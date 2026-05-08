package com.qingqiu.openagent.enums;

import lombok.Getter;

/**
 * @author: qingqiugeek
 * @date: 2026/5/10 09:27
 * @description: UserType
 */
@Getter
public enum UserType {
    USER("user", "普通用户"),
    ADMIN("admin", "管理员"),
    TOURISTS("tourists", "游客");

    private final String code;
    private final String description;

    UserType(String code, String description) {
        this.code = code;
        this.description = description;
    }

}
