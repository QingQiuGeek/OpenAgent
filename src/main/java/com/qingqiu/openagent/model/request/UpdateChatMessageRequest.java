package com.qingqiu.openagent.model.request;

import com.qingqiu.openagent.model.dto.ChatMessageDTO;
import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/7 18:17
 * @description: UpdateChatMessage request payload
 */

@Data
public class UpdateChatMessageRequest {
    private String content;
    private ChatMessageDTO.MetaData metadata;
}

