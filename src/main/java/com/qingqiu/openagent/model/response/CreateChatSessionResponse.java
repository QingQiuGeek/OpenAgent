package com.qingqiu.openagent.model.response;

import lombok.Builder;
import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/10 09:23
 * @description: CreateChatSession response payload
 */

@Data
@Builder
public class CreateChatSessionResponse {
    private String chatSessionId;
}
