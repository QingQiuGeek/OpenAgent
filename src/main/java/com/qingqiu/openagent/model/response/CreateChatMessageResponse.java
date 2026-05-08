package com.qingqiu.openagent.model.response;

import lombok.Builder;
import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/10 13:45
 * @description: CreateChatMessage response payload
 */

@Data
@Builder
public class CreateChatMessageResponse {
    private String chatMessageId;
}

