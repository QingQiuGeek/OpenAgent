package com.qingqiu.openagent.model.request;

import com.qingqiu.openagent.model.dto.ChatMessageDTO;
import lombok.Builder;
import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/8 22:54
 * @description: CreateChatMessage request payload
 */

@Data
@Builder
public class CreateChatMessageRequest {
    private String agentId;
    private String sessionId;
    private ChatMessageDTO.RoleType role;
    private String content;
    /** 元数据：toolCalls / toolResponse / sources / attachments 等 */
    private ChatMessageDTO.MetaData metadata;
    /** 是否启用深度思考（前端控制，后端目前透传给 agent） */
    private Boolean deepThink;
    /** 是否启用联网搜索 */
    private Boolean webSearch;
}
