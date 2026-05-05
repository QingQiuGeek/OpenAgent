package com.qingqiu.openagent.service.impl;

import com.qingqiu.openagent.enums.ChatModeType;
import com.qingqiu.openagent.mapper.ModelMapper;
import com.qingqiu.openagent.model.entity.Model;
import com.qingqiu.openagent.service.DynamicChatModelService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 按 {@code model.id} 动态构建 {@link ChatModel} 的实现。带轻量缓存，当 {@code model.updatedAt}
 * 变化时自动重建。
 */
@Service
@Slf4j
@AllArgsConstructor
public class DynamicChatModelServiceImpl implements DynamicChatModelService {

    private final ModelMapper modelMapper;

    /** 缓存：modelId → (updatedAt 时间戳, ChatModel)。updatedAt 变更则失效。 */
    private final ConcurrentHashMap<Long, Entry> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, StreamingEntry> streamingCache = new ConcurrentHashMap<>();

    @Override
    public ChatModel resolve(Long modelId) {
        if (modelId == null) {
            throw new IllegalStateException("Agent 未绑定 modelId，无法构建 ChatModel");
        }
        Model model = modelMapper.selectById(modelId);
        if (model == null) {
            throw new IllegalStateException("modelId=" + modelId + " 在 model 表中不存在或已被删除");
        }
        return resolveFromModel(model);
    }

    @Override
    public StreamingChatModel resolveStreaming(Long modelId) {
        if (modelId == null) {
            throw new IllegalStateException("Agent 未绑定 modelId，无法构建 StreamingChatModel");
        }
        Model model = modelMapper.selectById(modelId);
        if (model == null) {
            throw new IllegalStateException("modelId=" + modelId + " 在 model 表中不存在或已被删除");
        }
        Long id = model.getId();
        LocalDateTime updatedAt = model.getUpdatedAt();
        StreamingEntry cached = streamingCache.get(id);
        if (cached != null && Objects.equals(cached.updatedAt, updatedAt)) {
            return cached.streamingChatModel;
        }
        StreamingChatModel built = buildStreamingChatModel(model);
        streamingCache.put(id, new StreamingEntry(updatedAt, built));
        return built;
    }

    private ChatModel resolveFromModel(Model model) {
        Long id = model.getId();
        LocalDateTime updatedAt = model.getUpdatedAt();
        Entry cached = cache.get(id);
        if (cached != null && Objects.equals(cached.updatedAt, updatedAt)) {
            return cached.chatModel;
        }
        ChatModel built = buildChatModel(model);
        cache.put(id, new Entry(updatedAt, built));
        return built;
    }

    private ChatModel buildChatModel(Model model) {
        String providerType = model.getProviderType();
        // 当前 pom 仅 OpenAI-compatible 可用（Deepseek/ZhiPu/通义也可走 OpenAI 兼容协议）。
        // 后续加入 Ollama/ZhiPu 独立依赖后再按 providerType 分支。
        if (providerType == null
                || providerType.isBlank()
                || ChatModeType.OPEN_AI.getCode().equalsIgnoreCase(providerType)
                || ChatModeType.DEEP_SEEK.getCode().equalsIgnoreCase(providerType)
                || ChatModeType.QIAN_WEN.getCode().equalsIgnoreCase(providerType)
                || ChatModeType.ZHI_PU.getCode().equalsIgnoreCase(providerType)) {
            OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                    .baseUrl(model.getBaseUrl())
                    .apiKey(model.getApiKey())
                    .modelName(model.getModelName());
            if (model.getMaxTokens() != null) {
                builder.maxTokens(model.getMaxTokens());
            }
            return builder.build();
        }
        throw new IllegalStateException("暂不支持的 provider_type: " + providerType);
    }

    private StreamingChatModel buildStreamingChatModel(Model model) {
        String providerType = model.getProviderType();
        if (providerType == null
                || providerType.isBlank()
                || ChatModeType.OPEN_AI.getCode().equalsIgnoreCase(providerType)
                || ChatModeType.DEEP_SEEK.getCode().equalsIgnoreCase(providerType)
                || ChatModeType.QIAN_WEN.getCode().equalsIgnoreCase(providerType)
                || ChatModeType.ZHI_PU.getCode().equalsIgnoreCase(providerType)) {
            OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                    .baseUrl(model.getBaseUrl())
                    .apiKey(model.getApiKey())
                    .modelName(model.getModelName());
            if (model.getMaxTokens() != null) {
                builder.maxTokens(model.getMaxTokens());
            }
            return builder.build();
        }
        throw new IllegalStateException("暂不支持的 provider_type: " + providerType);
    }

    private record Entry(LocalDateTime updatedAt, ChatModel chatModel) {
    }

    private record StreamingEntry(LocalDateTime updatedAt, StreamingChatModel streamingChatModel) {
    }
}
