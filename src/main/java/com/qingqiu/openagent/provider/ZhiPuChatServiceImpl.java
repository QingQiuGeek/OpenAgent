package com.qingqiu.openagent.provider;


import com.qingqiu.openagent.enums.ChatModeType;
import com.qingqiu.openagent.model.entity.Model;
import com.qingqiu.openagent.model.request.ChatRequest;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * 智谱 AI 服务调用。
 *
 * <p>当前 pom.xml 未引入 {@code langchain4j-community-zhipu-ai} 依赖；Step 6 会在
 * {@code DynamicChatModelService} 中统一处理 provider_type 分发，此处先占位。</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ZhiPuChatServiceImpl implements AbstractChatService {

    @Override
    public StreamingChatModel buildStreamingChatModel(Model chatModelVo, ChatRequest chatRequest) {
        throw new UnsupportedOperationException("ZhiPu streaming model not wired yet; handled in DynamicChatModelService (Step 6)");
    }

    @Override
    public String getProviderName() {
        return ChatModeType.ZHI_PU.getCode();
    }
}
