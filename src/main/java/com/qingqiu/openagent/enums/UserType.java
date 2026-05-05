package com.qingqiu.openagent.enums;

import lombok.Getter;

/**
 * 模型分类
 *
 * @author
 * @date 2025-12-14
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
