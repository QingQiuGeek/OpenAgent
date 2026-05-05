package com.qingqiu.openagent.controller;

import com.qingqiu.openagent.agent.AgentStopRegistry;
import com.qingqiu.openagent.model.common.R;
import com.qingqiu.openagent.model.request.CreateChatMessageRequest;
import com.qingqiu.openagent.model.request.UpdateChatMessageRequest;
import com.qingqiu.openagent.model.response.CreateChatMessageResponse;
import com.qingqiu.openagent.model.response.GetChatMessagesResponse;
import com.qingqiu.openagent.service.ChatMessageFacadeService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理聊天消息，会触发 ChatEvent 和 Agent 执行
 */
@RestController
@RequestMapping("/api/chat-messages")
@AllArgsConstructor
public class ChatMessageController {

    private final ChatMessageFacadeService chatMessageFacadeService;
    private final AgentStopRegistry agentStopRegistry;

    // 根据 sessionId 查询聊天消息
    @GetMapping("/session/{sessionId}")
    public R<GetChatMessagesResponse> getChatMessagesBySessionId(@PathVariable String sessionId) {
        return R.success(chatMessageFacadeService.getChatMessagesBySessionId(sessionId));
    }

    // 创建聊天消息
    @PostMapping
    public R<CreateChatMessageResponse> createChatMessage(@RequestBody CreateChatMessageRequest request) {
        return R.success(chatMessageFacadeService.createChatMessage(request));
    }

    // 删除聊天消息
    @DeleteMapping("/{chatMessageId}")
    public R<Boolean> deleteChatMessage(@PathVariable String chatMessageId) {
        chatMessageFacadeService.deleteChatMessage(chatMessageId);
        return R.success(true);
    }

    // 更新聊天消息
    @PatchMapping("/{chatMessageId}")
    public R<Boolean> updateChatMessage(@PathVariable String chatMessageId, @RequestBody UpdateChatMessageRequest request) {
        chatMessageFacadeService.updateChatMessage(chatMessageId, request);
        return R.success(true);
    }

    // 终止指定会话的 Agent 运行
    @PostMapping("/stop/{sessionId}")
    public R<Boolean> stopAgent(@PathVariable String sessionId) {
        agentStopRegistry.requestStop(sessionId);
        return R.success(true);
    }
}
