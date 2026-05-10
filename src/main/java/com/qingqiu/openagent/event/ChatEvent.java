package com.qingqiu.openagent.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/11 10:13
 * @description: ChatEvent
 */

@Data
@Builder
@AllArgsConstructor
public class ChatEvent {
    private String agentId;
    private String sessionId;
    private String userInput;
    /** 是否启用联网搜索 */
    private boolean webSearch;
    /** 发起对话的用户 ID，跨线程传递（@Async listener 收不到 ThreadLocal） */
    private Long userId;
}
