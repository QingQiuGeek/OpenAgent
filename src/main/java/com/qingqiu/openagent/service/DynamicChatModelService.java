package com.qingqiu.openagent.service;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

/**
 * 按 {@code model.id} 动态解析出一个 {@link ChatModel} 实例。
 *
 * <p>从 {@code model} 表读用户自定义配置并构建；带轻量缓存，{@code updatedAt} 变化时自动失效重建。
 * <strong>modelId 必填</strong>，未绑定或查不到则抛 {@link IllegalStateException}。</p>
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
