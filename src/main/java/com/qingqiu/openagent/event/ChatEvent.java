package com.qingqiu.openagent.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

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
