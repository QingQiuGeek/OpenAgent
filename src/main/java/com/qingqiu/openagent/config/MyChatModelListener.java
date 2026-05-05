package com.qingqiu.openagent.config;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 底层 provider 的流式聊天日志监听器（占位实现）。
 */
@Slf4j
public class MyChatModelListener implements ChatModelListener {

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        log.debug("[ChatModel] onRequest");
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        log.debug("[ChatModel] onResponse");
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        log.warn("[ChatModel] onError", errorContext.error());
    }
}
