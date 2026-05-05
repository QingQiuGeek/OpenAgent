package com.qingqiu.openagent.model.request;

import lombok.Data;

/**
 * 面向底层 provider 的流式聊天请求参数（预留占位，Step 6 的动态模型会进一步拓展）。
 */
@Data
public class ChatRequest {

    private String sessionId;

    private String userMessage;

    private Boolean enableThinking;
}
