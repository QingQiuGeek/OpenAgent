package com.qingqiu.openagent.model.request;

import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/6 11:32
 * @description: CreateChatSession request payload
 */

@Data
public class CreateChatSessionRequest {
    private String agentId;
    private String title;
}
