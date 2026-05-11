package com.qingqiu.openagent.event.listener;

import com.qingqiu.openagent.agent.ChatAgent;
import com.qingqiu.openagent.agent.ChatAgentFactory;
import com.qingqiu.openagent.event.ChatEvent;
import com.qingqiu.openagent.message.SseMessage;
import com.qingqiu.openagent.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 监听 {@link ChatEvent}，异步驱动 Agent 运行；异常会被捕获并通过 SSE 推送给前端，
 * 避免前端表现为"无回复 + SSE 中断"导致用户困惑。
 *
 * @author qingqiugeek
 * @date 2026/5/1 19:39
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatEventListener {

    private final ChatAgentFactory chatAgentFactory;
    private final SseService sseService;

    @Async
    @EventListener
    public void handle(ChatEvent event) {
        try {
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
        } catch (Exception e) {
            // Agent 运行失败：日志 + 推 SSE 错误事件，让前端能展示"系统繁忙，请重试"
            log.error("[ChatEventListener] Agent 运行失败 sessionId={}, agentId={}",
                    event.getSessionId(), event.getAgentId(), e);
            try {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                String msg = cause.getMessage() == null ? e.getClass().getSimpleName() : cause.getMessage();
                sseService.send(event.getSessionId(), SseMessage.builder()
                        .type(SseMessage.Type.AI_ERROR)
                        .payload(SseMessage.Payload.builder()
                                .delta("智能体运行失败：" + msg)
                                .build())
                        .build());
            } catch (Exception inner) {
                log.warn("[ChatEventListener] 推送错误事件失败: {}", inner.getMessage());
            }
        }
    }
}
