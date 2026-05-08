package com.qingqiu.openagent.provider;


import com.qingqiu.openagent.config.MyChatModelListener;
import com.qingqiu.openagent.enums.ChatModeType;
import com.qingqiu.openagent.model.entity.Model;
import com.qingqiu.openagent.model.request.ChatRequest;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * @author: qingqiugeek
 * @date: 2026/5/4 13:39
 * @description: QianWenChat service implementation
 */
@Service
@Slf4j
public class QianWenChatServiceImpl implements AbstractChatService {

    @Override
    public StreamingChatModel buildStreamingChatModel(Model chatModelVo,
        ChatRequest chatRequest) {
        return QwenStreamingChatModel.builder()
                .apiKey(chatModelVo.getApiKey())
                .modelName(chatModelVo.getModelName())
                .listeners(List.of(new MyChatModelListener()))
                .build();
    }

    @Override
    public String getProviderName() {
        return ChatModeType.QIAN_WEN.getCode();
    }

}
