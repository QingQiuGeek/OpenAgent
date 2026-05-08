package com.qingqiu.openagent.enums;

import lombok.Getter;

/**
 * @author: qingqiugeek
 * @date: 2026/5/12 18:08
 * @description: ChatModeType
 */
@Getter
public enum ChatModeType {
    OLLAMA("ollama", "ollama本地部署模型"),
    ZHI_PU("zhipu", "智谱清言"),
    DEEP_SEEK("deepseek", "深度求索"),
    QIAN_WEN("qianwen", "通义千问"),
    OPEN_AI("openai", "openai");
    private final String code;
    private final String description;

    ChatModeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

}
