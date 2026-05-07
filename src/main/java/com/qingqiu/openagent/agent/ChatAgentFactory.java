package com.qingqiu.openagent.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qingqiu.openagent.agent.tools.ITool;
import com.qingqiu.openagent.converter.AgentConverter;
import com.qingqiu.openagent.converter.ChatMessageConverter;
import com.qingqiu.openagent.converter.KnowledgeBaseConverter;
import com.qingqiu.openagent.mapper.AgentMapper;
import com.qingqiu.openagent.mapper.KnowledgeBaseMapper;
import com.qingqiu.openagent.model.dto.AgentDTO;
import com.qingqiu.openagent.model.dto.ChatMessageDTO;
import com.qingqiu.openagent.model.dto.KnowledgeBaseDTO;
import com.qingqiu.openagent.model.entity.Agent;
import com.qingqiu.openagent.model.entity.KnowledgeBase;
import com.qingqiu.openagent.service.ChatMessageFacadeService;
import com.qingqiu.openagent.service.DynamicChatModelService;
import com.qingqiu.openagent.service.SseService;
import com.qingqiu.openagent.service.ToolFacadeService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ChatAgentFactory {

    private static final Logger log = LoggerFactory.getLogger(ChatAgentFactory.class);
    private final DynamicChatModelService dynamicChatModelService;
    private final SseService sseService;
    private final AgentMapper agentMapper;
    private final AgentConverter agentConverter;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeBaseConverter knowledgeBaseConverter;
    private final ToolFacadeService toolFacadeService;
    private final ChatMessageFacadeService chatMessageFacadeService;
    private final ChatMessageConverter chatMessageConverter;
    private final ObjectMapper objectMapper;
    private final AgentStopRegistry agentStopRegistry;

    // 运行时 Agent 配置
    private AgentDTO agentConfig;

    public ChatAgentFactory(
            DynamicChatModelService dynamicChatModelService,
            SseService sseService,
            AgentMapper agentMapper,
            AgentConverter agentConverter,
            KnowledgeBaseMapper knowledgeBaseMapper,
            KnowledgeBaseConverter knowledgeBaseConverter,
            ToolFacadeService toolFacadeService,
            ChatMessageFacadeService chatMessageFacadeService,
            ChatMessageConverter chatMessageConverter,
            ObjectMapper objectMapper,
            AgentStopRegistry agentStopRegistry
    ) {
        this.dynamicChatModelService = dynamicChatModelService;
        this.sseService = sseService;
        this.agentMapper = agentMapper;
        this.agentConverter = agentConverter;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeBaseConverter = knowledgeBaseConverter;
        this.toolFacadeService = toolFacadeService;
        this.chatMessageFacadeService = chatMessageFacadeService;
        this.chatMessageConverter = chatMessageConverter;
        this.objectMapper = objectMapper;
        this.agentStopRegistry = agentStopRegistry;
    }

    private Agent loadAgent(String agentId) {
        return agentMapper.selectById(agentId);
    }

    /**
     * 将数据库中存储的记忆恢复成 List<Message> 结构
     */
    private List<ChatMessage> loadMemory(String chatSessionId) {
        int messageLength = agentConfig.getChatOptions().getMessageLength();
        List<ChatMessageDTO> chatMessages = chatMessageFacadeService.getChatMessagesBySessionIdRecently(chatSessionId, messageLength);
        List<ChatMessage> memory = new ArrayList<>();
        for (ChatMessageDTO chatMessageDTO : chatMessages) {
            switch (chatMessageDTO.getRole()) {
                case SYSTEM:
                    if (!StringUtils.hasLength(chatMessageDTO.getContent())) {
                        continue;
                    }
                    memory.add(0, SystemMessage.from(chatMessageDTO.getContent()));
                    break;
                case USER:
                    String userContent = buildUserContentWithAttachments(chatMessageDTO);
                    if (!StringUtils.hasLength(userContent)) {
                        continue;
                    }
                    memory.add(UserMessage.from(userContent));
                    break;
                case ASSISTANT:
                    List<ToolExecutionRequest> requests = new ArrayList<>();
                    if (chatMessageDTO.getMetadata() != null && chatMessageDTO.getMetadata().getToolCalls() != null) {
                        for (ChatMessageDTO.ToolCall toolCall : chatMessageDTO.getMetadata().getToolCalls()) {
                            requests.add(ToolExecutionRequest.builder()
                                    .id(toolCall.getId())
                                    .name(toolCall.getName())
                                    .arguments(toolCall.getArguments())
                                    .build());
                        }
                    }
                    memory.add(AiMessage.from(chatMessageDTO.getContent(), requests));
                    break;
                case TOOL:
                    if (chatMessageDTO.getMetadata() == null || chatMessageDTO.getMetadata().getToolResponse() == null) {
                        break;
                    }
                    ChatMessageDTO.ToolResponse response = chatMessageDTO.getMetadata().getToolResponse();
                    memory.add(ToolExecutionResultMessage.from(
                            response.getId(),
                            response.getName(),
                            response.getResponseData()
                    ));
                    break;
                default:
                    log.error("不支持的 Message 类型: {}, content = {}",
                            chatMessageDTO.getRole().getRole(),
                            chatMessageDTO.getContent()
                    );
                    throw new IllegalStateException("不支持的 Message 类型");
            }
        }
        return memory;
    }

    /**
     * 把用户消息的 metadata.attachments 拼到 content 后面，让 LLM 能感知到附件。
     * <p>展示层不变（数据库里 content 仍是用户原文），仅在喂给模型时拼接附件清单。
     *
     * <pre>
     * &lt;原始 content&gt;
     *
     * 附件:
     * - [文件名] (image/png, 1024B): https://oss.../foo.png
     * - [文件名] (text/plain, 200B): https://oss.../bar.txt
     * </pre>
     */
    private String buildUserContentWithAttachments(ChatMessageDTO dto) {
        String content = dto.getContent();
        if (dto.getMetadata() == null || dto.getMetadata().getAttachments() == null
                || dto.getMetadata().getAttachments().isEmpty()) {
            return content == null ? "" : content;
        }
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasLength(content)) {
            sb.append(content).append("\n\n");
        }
        sb.append("附件:");
        for (ChatMessageDTO.Attachment att : dto.getMetadata().getAttachments()) {
            sb.append("\n- [")
                    .append(att.getName() == null ? "未命名" : att.getName())
                    .append("]");
            if (StringUtils.hasLength(att.getContentType()) || att.getSize() != null) {
                sb.append(" (");
                if (StringUtils.hasLength(att.getContentType())) {
                    sb.append(att.getContentType());
                }
                if (att.getSize() != null) {
                    if (StringUtils.hasLength(att.getContentType())) {
                        sb.append(", ");
                    }
                    sb.append(att.getSize()).append("B");
                }
                sb.append(")");
            }
            if (StringUtils.hasLength(att.getUrl())) {
                sb.append(": ").append(att.getUrl());
            }
        }
        return sb.toString();
    }

    private AgentDTO toAgentConfig(Agent agent) {
        try {
            agentConfig = agentConverter.toDTO(agent);
            return agentConfig;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("解析 Agent 配置失败", e);
        }
    }

    private List<KnowledgeBaseDTO> resolveRuntimeKnowledgeBases(AgentDTO agentConfig) {
        List<String> allowedKbIds = agentConfig.getAllowedKbs();
        if (allowedKbIds == null || allowedKbIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<KnowledgeBase> knowledgeBases = knowledgeBaseMapper.selectByIdBatch(allowedKbIds);
        if (knowledgeBases.isEmpty()) {
            return Collections.emptyList();
        }
        List<KnowledgeBaseDTO> kbDTOs = new ArrayList<>();
        try {
            for (KnowledgeBase knowledgeBase : knowledgeBases) {
                KnowledgeBaseDTO kbDTO = knowledgeBaseConverter.toDTO(knowledgeBase);
                kbDTOs.add(kbDTO);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return kbDTOs;
    }

    private List<ITool> resolveRuntimeTools(AgentDTO agentConfig) {
        // 固定工具（系统强制）
        List<ITool> runtimeITools = new ArrayList<>(toolFacadeService.getFixedTools());

        // 可选工具（按 Agent 配置）
        List<String> allowedToolNames = agentConfig.getAllowedTools();
        if (allowedToolNames == null || allowedToolNames.isEmpty()) {
            return runtimeITools;
        }

        Map<String, ITool> optionalToolMap = toolFacadeService.getOptionalTools()
                .stream()
                .collect(Collectors.toMap(ITool::getName, Function.identity()));

        for (String toolName : allowedToolNames) {
            ITool iTool = optionalToolMap.get(toolName);
            if (iTool != null) {
                runtimeITools.add(iTool);
            }
        }
        return runtimeITools;
    }

    private LangChainToolExecutor buildToolExecutor(List<ITool> runtimeITools) {
        return new LangChainToolExecutor(runtimeITools, objectMapper);
    }

    private ChatAgent buildAgentRuntime(
            Agent agent,
            List<ChatMessage> memory,
            List<KnowledgeBaseDTO> knowledgeBases,
            LangChainToolExecutor toolExecutor,
            String chatSessionId
    ) {
        // 根据 agent.modelId 从 model 表动态构建；modelId 必填，未绑定或被删除直接抛错。
        ChatModel chatModel = dynamicChatModelService.resolve(agent.getModelId());
        StreamingChatModel streamingChatModel = dynamicChatModelService.resolveStreaming(agent.getModelId());
        return new ChatAgent(
                agent.getId(),
                agent.getName(),
                agent.getDescription(),
                agent.getSystemPrompt(),
                chatModel,
                streamingChatModel,
                agentConfig.getChatOptions().getMessageLength(),
                memory,
                toolExecutor,
                knowledgeBases,
                chatSessionId,
                sseService,
                chatMessageFacadeService,
                chatMessageConverter,
                agentStopRegistry
        );
    }

    /**
     * 创建一个 OpenAgent 实例。
     *
     * @param deepThink 是否启用深度思考（保留给后续接入推理模型用）
     * @param webSearch 是否启用联网搜索（true 时把 WebSearchTool 加入运行时工具集）
     */
    public ChatAgent create(String agentId, String chatSessionId, boolean deepThink, boolean webSearch) {
        Agent agent = loadAgent(agentId);
        AgentDTO agentConfig = toAgentConfig(agent);
        List<ChatMessage> memory = loadMemory(chatSessionId);

        // 解析 agent 的支持的知识库
        List<KnowledgeBaseDTO> knowledgeBases = resolveRuntimeKnowledgeBases(agentConfig);
        // 解析 agent 支持的工具调用
        List<ITool> runtimeITools = resolveRuntimeTools(agentConfig);

        // 联网搜索：动态注入 WebSearchTool（如果用户勾选且容器中存在该 bean）
        if (webSearch) {
            ITool webSearchTool = toolFacadeService.getAllTools().stream()
                    .filter(t -> "webSearchTool".equals(t.getName()))
                    .findFirst()
                    .orElse(null);
            if (webSearchTool != null && !runtimeITools.contains(webSearchTool)) {
                runtimeITools.add(webSearchTool);
                log.info("[Agent] 已启用联网搜索: sessionId={}", chatSessionId);
            } else if (webSearchTool == null) {
                log.warn("[Agent] webSearch=true 但未找到 WebSearchTool bean");
            }
        }

        // 构建 LangChain4j 工具执行器
        LangChainToolExecutor toolExecutor = buildToolExecutor(runtimeITools);

        ChatAgent chatAgent = buildAgentRuntime(
                agent,
                memory,
                knowledgeBases,
                toolExecutor,
                chatSessionId
        );
        chatAgent.setDeepThink(deepThink);
        chatAgent.setWebSearch(webSearch);
        return chatAgent;
    }

    /** 兼容旧签名 */
    public ChatAgent create(String agentId, String chatSessionId) {
        return create(agentId, chatSessionId, false, false);
    }
}
