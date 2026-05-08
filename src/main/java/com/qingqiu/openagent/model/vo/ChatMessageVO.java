package com.qingqiu.openagent.model.vo;

import com.qingqiu.openagent.model.dto.ChatMessageDTO;
import lombok.Builder;
import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/5 18:30
 * @description: ChatMessage view object
 */

@Data
@Builder
public class ChatMessageVO {
    private String id;
    private String sessionId;
    private ChatMessageDTO.RoleType role;
    private String content;
    private ChatMessageDTO.MetaData metadata;
}
