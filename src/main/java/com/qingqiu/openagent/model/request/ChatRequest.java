package com.qingqiu.openagent.model.request;

import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/10 17:37
 * @description: Chat request payload
 */
@Data
public class ChatRequest {

    private String sessionId;

    private String userMessage;

    private Boolean enableThinking;
}
