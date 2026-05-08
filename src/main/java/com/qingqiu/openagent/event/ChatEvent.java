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
    /** 是否启用深度思考 */
    private boolean deepThink;
    /** 是否启用联网搜索 */
    private boolean webSearch;
}
