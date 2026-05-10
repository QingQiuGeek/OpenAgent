package com.qingqiu.openagent.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qingqiu.openagent.agent.tools.ITool;
import com.qingqiu.openagent.util.AttachmentContentExtractor;
import com.qingqiu.openagent.converter.AgentConverter;
import com.qingqiu.openagent.converter.ChatMessageConverter;
import com.qingqiu.openagent.converter.KnowledgeBaseConverter;
import com.qingqiu.openagent.agent.mcp.McpClientPool;
import com.qingqiu.openagent.mapper.AgentMapper;
import com.qingqiu.openagent.mapper.KnowledgeBaseMapper;
import com.qingqiu.openagent.mapper.McpServerMapper;
import com.qingqiu.openagent.model.entity.McpServer;
import com.qingqiu.openagent.model.dto.AgentDTO;
import com.qingqiu.openagent.model.dto.ChatMessageDTO;
import com.qingqiu.openagent.model.dto.KnowledgeBaseDTO;
import com.qingqiu.openagent.model.entity.Agent;
import com.qingqiu.openagent.model.entity.KnowledgeBase;
import com.qingqiu.openagent.service.AgentUsageLogService;
import com.qingqiu.openagent.service.ChatMessageFacadeService;
import com.qingqiu.openagent.service.DynamicChatModelService;
import com.qingqiu.openagent.service.SseService;
import com.qingqiu.openagent.service.ToolFacadeService;
import com.qingqiu.openagent.util.UserContext;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.mcp.client.McpClient;
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

/**
 * @author: qingqiugeek
 * @date: 2026/5/7 22:26
 * @description: ChatAgent factory
 */

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
    private final AttachmentContentExtractor attachmentExtractor;
    private final AgentUsageLogService agentUsageLogService;
    private final McpServerMapper mcpServerMapper;
    private final McpClientPool mcpClientPool;

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
            AgentStopRegistry agentStopRegistry,
            AttachmentContentExtractor attachmentExtractor,
            AgentUsageLogService agentUsageLogService,
            McpServerMapper mcpServerMapper,
            McpClientPool mcpClientPool
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
        this.attachmentExtractor = attachmentExtractor;
        this.agentUsageLogService = agentUsageLogService;
        this.mcpServerMapper = mcpServerMapper;
        this.mcpClientPool = mcpClientPool;
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
     * 把用户消息的附件内容提取后拼到 content，让 LLM 能理解文件实际内容。
     *
     * <p>策略（按文件类型）：
     * <ul>
     *   <li>图片：只告诉 LLM 有图片附件及 URL（无法提取像素含义）</li>
     *   <li>可解析文档/代码：用 Tika 提取文本，截断到 50,000 字符后内联给 LLM</li>
     *   <li>解析失败：降级为只给 URL，告知 LLM 无法读取内容</li>
     * </ul>
     *
     * <p>展示层不变：数据库里 content 仍是用户原文，只在送给模型时拼接附件内容。
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
        sb.append("以下是用户上传的附件内容：");
        for (ChatMessageDTO.Attachment att : dto.getMetadata().getAttachments()) {
            String name = att.getName() == null ? "未命名" : att.getName();
            String ct   = att.getContentType();
            sb.append("\n\n--- 附件: ").append(name);
            if (StringUtils.hasLength(ct)) {
                sb.append(" (").append(ct).append(")");
            }
            sb.append(" ---");

            // 图片无可提取文本，只告知 LLM 有图片
            if (ct != null && ct.startsWith("image/")) {
                sb.append("\n[图片附件，URL: ").append(att.getUrl()).append("]");
                continue;
            }

            // 尝试用 Tika 提取文件文本内容
            String extracted = attachmentExtractor.extract(att.getUrl(), ct);
            if (StringUtils.hasLength(extracted)) {
                sb.append("\n").append(extracted);
            } else {
                // 降级：Tika 无法解析或文件为空，只给 URL
                sb.append("\n[无法提取文本内容，文件 URL: ").append(att.getUrl()).append("]");
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

    /** 扫描 allowed_tools 中以 "mcp:{id}" 开头的条目，加载该用户已启用的 MCP 客户端。 */
    private List<McpClient> resolveMcpClients(AgentDTO agentConfig, Long ownerUserId) {
        List<String> allowed = agentConfig.getAllowedTools();
        if (allowed == null || allowed.isEmpty()) return Collections.emptyList();
        List<McpClient> clients = new ArrayList<>();
        for (String name : allowed) {
            if (name == null || !name.startsWith("mcp:")) continue;
            String idStr = name.substring("mcp:".length());
            Long mcpId;
            try {
                mcpId = Long.parseLong(idStr.trim());
            } catch (NumberFormatException e) {
                log.warn("[Agent] 非法 mcp 引用: {}", name);
                continue;
            }
            try {
                McpServer server = mcpServerMapper.selectById(mcpId);
                if (server == null) {
                    log.warn("[Agent] mcp#{} 不存在，跳过", mcpId);
                    continue;
                }
                if (server.getEnabled() != null && server.getEnabled() == 0) {
                    log.info("[Agent] mcp#{} 已禁用，跳过", mcpId);
                    continue;
                }
                if (ownerUserId != null && server.getUserId() != null
                        && !ownerUserId.equals(server.getUserId())) {
                    log.warn("[Agent] mcp#{} 不属于 agent 所有者 (user={})，跳过", mcpId, ownerUserId);
                    continue;
                }
                McpClient client = mcpClientPool.acquire(server);
                clients.add(client);
            } catch (Exception e) {
                log.warn("[Agent] 加载 mcp#{} 失败，跳过: {}", mcpId, e.getMessage());
            }
        }
        return clients;
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
     * @param webSearch 是否启用联网搜索（true 时把 WebSearchTool 加入运行时工具集）
     */
    public ChatAgent create(String agentId, String chatSessionId, boolean webSearch) {
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

        // MCP 工具注入：从 allowed_tools 中识别 mcp:{id} 条目
        List<McpClient> mcpClients = resolveMcpClients(agentConfig, agent.getUserId());
        if (!mcpClients.isEmpty()) {
            toolExecutor.addMcpTools(mcpClients);
        }

        ChatAgent chatAgent = buildAgentRuntime(
                agent,
                memory,
                knowledgeBases,
                toolExecutor,
                chatSessionId
        );
        chatAgent.setWebSearch(webSearch);

        // ===== 用量埋点：注入 service + 关键上下文 =====
        chatAgent.setAgentUsageLogService(agentUsageLogService);
        chatAgent.setUsageUserId(UserContext.getUser());
        chatAgent.setUsageModelId(agent.getModelId());
        chatAgent.setChatMode(webSearch ? "web_search" : "normal");

        return chatAgent;
    }

    /** 兼容旧签名 */
    public ChatAgent create(String agentId, String chatSessionId) {
        return create(agentId, chatSessionId, false);
    }
}
