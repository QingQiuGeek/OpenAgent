package com.qingqiu.openagent.provider;

import com.qingqiu.openagent.model.entity.Model;
import com.qingqiu.openagent.model.request.ChatRequest;
import dev.langchain4j.model.chat.StreamingChatModel;

/**
 * @author: qingqiugeek
 * @date: 2026/5/4 14:54
 * @description: AbstractChat service
 */
public interface AbstractChatService {

    /**
     * 创建流式聊天模型
     *
     * @param chatModelVo 模型配置
     * @param chatRequest 聊天请求
     * @return 流式聊天模型实例
     */
    StreamingChatModel buildStreamingChatModel(Model chatModelVo, ChatRequest chatRequest);

    /**
     * 获取服务提供商名称
     */
    String getProviderName();
}
