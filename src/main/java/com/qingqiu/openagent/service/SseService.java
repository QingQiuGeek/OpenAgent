package com.qingqiu.openagent.service;

import com.qingqiu.openagent.message.SseMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author: qingqiugeek
 * @date: 2026/5/2 13:17
 * @description: Sse service
 */

public interface SseService {
    // 没有用户系统，使用 chatSessionId 作为连接标识
    SseEmitter connect(String chatSessionId);

    void send(String chatSessionId, SseMessage message);
}
