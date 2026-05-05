package com.qingqiu.openagent.agent;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

/**
 * 用于跨线程通知 ChatAgent 停止当前运行循环。
 * key: chatSessionId
 */
@Component
public class AgentStopRegistry {

    private final Set<String> stopFlags = ConcurrentHashMap.newKeySet();

    /** 标记指定会话需要停止 */
    public void requestStop(String chatSessionId) {
        if (chatSessionId != null) {
            stopFlags.add(chatSessionId);
        }
    }

    /** 检查是否被请求停止 */
    public boolean isStopRequested(String chatSessionId) {
        return chatSessionId != null && stopFlags.contains(chatSessionId);
    }

    /** 清除停止标记，agent 结束运行时调用 */
    public void clear(String chatSessionId) {
        if (chatSessionId != null) {
            stopFlags.remove(chatSessionId);
        }
    }
}
