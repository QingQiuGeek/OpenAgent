package com.qingqiu.openagent.controller;

import com.qingqiu.openagent.model.common.R;
import com.qingqiu.openagent.model.request.CreateChatSessionRequest;
import com.qingqiu.openagent.model.request.UpdateChatSessionRequest;
import com.qingqiu.openagent.model.response.CreateChatSessionResponse;
import com.qingqiu.openagent.model.response.GetChatSessionResponse;
import com.qingqiu.openagent.model.response.GetChatSessionsResponse;
import com.qingqiu.openagent.service.ChatSessionFacadeService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 会话管理，不处理消息
 */
@RestController
@RequestMapping("/api/chat-sessions")
@AllArgsConstructor
public class ChatSessionController {

    private final ChatSessionFacadeService chatSessionFacadeService;

    // 查询所有聊天会话
    @GetMapping
    public R<GetChatSessionsResponse> getChatSessions() {
        return R.success(chatSessionFacadeService.getChatSessions());
    }

    // 查询单个聊天会话
    @GetMapping("/{chatSessionId}")
    public R<GetChatSessionResponse> getChatSession(@PathVariable String chatSessionId) {
        return R.success(chatSessionFacadeService.getChatSession(chatSessionId));
    }

    // 根据 agentId 查询聊天会话
    @GetMapping("/agent/{agentId}")
    public R<GetChatSessionsResponse> getChatSessionsByAgentId(@PathVariable String agentId) {
        return R.success(chatSessionFacadeService.getChatSessionsByAgentId(agentId));
    }

    // 创建聊天会话
    @PostMapping
    public R<CreateChatSessionResponse> createChatSession(@RequestBody CreateChatSessionRequest request) {
        return R.success(chatSessionFacadeService.createChatSession(request));
    }

    // 删除聊天会话
    @DeleteMapping("/{chatSessionId}")
    public R<Boolean> deleteChatSession(@PathVariable String chatSessionId) {
        chatSessionFacadeService.deleteChatSession(chatSessionId);
        return R.success(true);
    }

    // 更新聊天会话名
    @PatchMapping("/{chatSessionId}")
    public R<Boolean> updateChatSession(@PathVariable String chatSessionId, @RequestBody UpdateChatSessionRequest request) {
        chatSessionFacadeService.updateChatSession(chatSessionId, request);
        return R.success(true);
    }
}
