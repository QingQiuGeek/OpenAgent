package com.qingqiu.openagent.provider;


import com.qingqiu.openagent.enums.ChatModeType;
import com.qingqiu.openagent.model.entity.Model;
import com.qingqiu.openagent.model.request.ChatRequest;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * @author: qingqiugeek
 * @date: 2026/5/4 13:51
 * @description: ZhiPuChat service implementation
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
