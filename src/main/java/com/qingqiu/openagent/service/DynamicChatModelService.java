package com.qingqiu.openagent.service;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

/**
 * @author: qingqiugeek
 * @date: 2026/5/11 21:58
 * @description: DynamicChatModel service
 */
public interface DynamicChatModelService {

    /**
     * @param modelId {@link com.qingqiu.openagent.model.entity.Model#getId()}，必填
     * @return 非空 ChatModel
     * @throws IllegalStateException modelId 为空 / 记录不存在 / provider 不支持
     */
    ChatModel resolve(Long modelId);

    /**
     * 解析支持流式输出的 ChatModel
     */
    StreamingChatModel resolveStreaming(Long modelId);
}
