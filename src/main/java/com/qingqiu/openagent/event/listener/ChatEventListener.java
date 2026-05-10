package com.qingqiu.openagent.event.listener;

import com.qingqiu.openagent.agent.ChatAgent;
import com.qingqiu.openagent.agent.ChatAgentFactory;
import com.qingqiu.openagent.event.ChatEvent;
import lombok.AllArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @author: qingqiugeek
 * @date: 2026/5/1 19:39
 * @description: ChatEvent listener
 */

@Component
@AllArgsConstructor
public class ChatEventListener {

    private final ChatAgentFactory chatAgentFactory;

    @Async
    @EventListener
    public void handle(ChatEvent event) {
        // 创建一个 Agent 实例处理聊天事件
        ChatAgent chatAgent = chatAgentFactory.create(
                event.getAgentId(),
                event.getSessionId(),
                event.isWebSearch()
        );
        // ThreadLocal 在 @Async 线程中失效，这里用事件携带的 userId 覆盖一次
        if (event.getUserId() != null) {
            chatAgent.setUsageUserId(event.getUserId());
        }
        chatAgent.run();
    }
}
