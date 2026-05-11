package com.qingqiu.openagent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingqiu.openagent.message.SseMessage;
import com.qingqiu.openagent.service.SseService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SSE 服务实现：维护 chatSessionId → SseEmitter 的映射，并周期性发送 heartbeat
 * 防止反向代理 / 浏览器把空闲连接当作死连接关闭。
 *
 * @author qingqiugeek
 * @date 2026/5/8 18:56
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class SseServiceImpl implements SseService {

    /** 心跳间隔：15s。够小避免 Nginx 默认 60s read timeout，够大不浪费带宽。 */
    private static final long HEARTBEAT_INTERVAL_SECONDS = 15;

    private final ConcurrentMap<String, SseEmitter> clients = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    /** 单线程定时器，足以处理几百条心跳。 */
    private ScheduledExecutorService heartbeatScheduler;

    @PostConstruct
    public void init() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sse-heartbeat");
            t.setDaemon(true);
            return t;
        });
        heartbeatScheduler.scheduleAtFixedRate(this::broadcastHeartbeat,
                HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdownNow();
        }
    }

    /**
     * 广播心跳。SseEmitter.event().comment(...) 会发送形如 `:heartbeat\n\n` 的 SSE 注释行，
     * 浏览器 EventSource 不会触发 onmessage，但能维持 TCP 连接活着。
     * 任何发送失败的 emitter 直接移除，让客户端走标准重连。
     */
    private void broadcastHeartbeat() {
        for (Map.Entry<String, SseEmitter> entry : clients.entrySet()) {
            String sessionId = entry.getKey();
            SseEmitter emitter = entry.getValue();
            try {
                emitter.send(SseEmitter.event().comment("hb"));
            } catch (Exception e) {
                // 写失败说明对端已经断开（或代理超时），清理掉
                log.debug("[SSE] heartbeat 失败，移除 session={}, reason={}", sessionId, e.getMessage());
                clients.remove(sessionId);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignore) {
                    /* 忽略二次失败 */
                }
            }
        }
    }

    @Override
    public SseEmitter connect(String chatSessionId) {
        // 超时 30 分钟：30min 内必须有 send / heartbeat 活动，否则连接被标记 timeout
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        // 用户重连时旧 emitter 残留：先清理避免内存泄漏
        SseEmitter previous = clients.put(chatSessionId, emitter);
        if (previous != null) {
            try { previous.complete(); } catch (Exception ignore) { /* ignore */ }
        }

        try {
            // 和前端协商：建链成功立即 push 一条 init，前端 onopen 之外能再确认一次
            emitter.send(SseEmitter.event()
                    .name("init")
                    .data("connected")
            );
        } catch (IOException e) {
            // 建链就失败：直接清理
            clients.remove(chatSessionId);
            throw new RuntimeException(e);
        }

        emitter.onCompletion(() -> clients.remove(chatSessionId));
        emitter.onTimeout(() -> {
            clients.remove(chatSessionId);
            emitter.complete();
        });
        emitter.onError((error) -> clients.remove(chatSessionId));

        return emitter;
    }

    @Override
    public void send(String chatSessionId, SseMessage message) {
        SseEmitter emitter = clients.get(chatSessionId);
        if (emitter == null) {
            log.warn("No SSE client found for chatSessionId: {}, message dropped", chatSessionId);
            return;
        }
        try {
            // 将消息转换为字符串
            String sseMessageStr = objectMapper.writeValueAsString(message);
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(sseMessageStr)
            );
        } catch (IOException e) {
            // 发送失败说明对端已经掉线，清理 + 不抛出，避免影响业务流程
            log.warn("[SSE] send 失败，移除 session={}, reason={}", chatSessionId, e.getMessage());
            clients.remove(chatSessionId);
            try {
                emitter.completeWithError(e);
            } catch (Exception ignore) {
                /* ignore */
            }
        }
    }
}
