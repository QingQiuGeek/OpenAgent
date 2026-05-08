package com.qingqiu.openagent.provider;


import com.qingqiu.openagent.config.MyChatModelListener;
import com.qingqiu.openagent.enums.ChatModeType;
import com.qingqiu.openagent.model.entity.Model;
import com.qingqiu.openagent.model.request.ChatRequest;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * @author: qingqiugeek
 * @date: 2026/5/7 14:55
 * @description: OpenAI service implementation
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OpenAIServiceImpl implements AbstractChatService {

    @Override
    public StreamingChatModel buildStreamingChatModel(Model chatModelVo,
        ChatRequest chatRequest) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(chatModelVo.getBaseUrl())
                .apiKey(chatModelVo.getApiKey())
                .modelName(chatModelVo.getModelName())
                .listeners(List.of(new MyChatModelListener()))
                .build();
    }

    @Override
    public String getProviderName() {
        return ChatModeType.OPEN_AI.getCode();
    }

}
