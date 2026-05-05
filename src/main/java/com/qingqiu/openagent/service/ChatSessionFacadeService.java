package com.qingqiu.openagent.service;

import com.qingqiu.openagent.model.request.CreateChatSessionRequest;
import com.qingqiu.openagent.model.request.UpdateChatSessionRequest;
import com.qingqiu.openagent.model.response.CreateChatSessionResponse;
import com.qingqiu.openagent.model.response.GetChatSessionResponse;
import com.qingqiu.openagent.model.response.GetChatSessionsResponse;

public interface ChatSessionFacadeService {
    GetChatSessionsResponse getChatSessions();

    GetChatSessionResponse getChatSession(String chatSessionId);

    GetChatSessionsResponse getChatSessionsByAgentId(String agentId);

    CreateChatSessionResponse createChatSession(CreateChatSessionRequest request);

    void deleteChatSession(String chatSessionId);

    void updateChatSession(String chatSessionId, UpdateChatSessionRequest request);
}
