package com.qingqiu.openagent.service;

import com.qingqiu.openagent.model.dto.ChatMessageDTO;
import com.qingqiu.openagent.model.request.CreateChatMessageRequest;
import com.qingqiu.openagent.model.request.UpdateChatMessageRequest;
import com.qingqiu.openagent.model.response.CreateChatMessageResponse;
import com.qingqiu.openagent.model.response.GetChatMessagesResponse;

import java.util.List;

/**
 * @author: qingqiugeek
 * @date: 2026/5/4 18:04
 * @description: ChatMessageFacade service
 */

public interface ChatMessageFacadeService {
    GetChatMessagesResponse getChatMessagesBySessionId(String sessionId);

    List<ChatMessageDTO> getChatMessagesBySessionIdRecently(String sessionId, int limit);

    CreateChatMessageResponse createChatMessage(CreateChatMessageRequest request);

    CreateChatMessageResponse createChatMessage(ChatMessageDTO chatMessageDTO);

    CreateChatMessageResponse agentCreateChatMessage(CreateChatMessageRequest request);

    CreateChatMessageResponse appendChatMessage(String chatMessageId, String appendContent);

    void deleteChatMessage(String chatMessageId);

    void updateChatMessage(String chatMessageId, UpdateChatMessageRequest request);
}
