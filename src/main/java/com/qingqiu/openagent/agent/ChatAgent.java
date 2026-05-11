package com.qingqiu.openagent.agent;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.qingqiu.openagent.converter.ChatMessageConverter;
import com.qingqiu.openagent.message.SseMessage;
import com.qingqiu.openagent.model.dto.ChatMessageDTO;
import com.qingqiu.openagent.model.dto.KnowledgeBaseDTO;
import com.qingqiu.openagent.model.entity.AgentUsageLog;
import com.qingqiu.openagent.model.response.CreateChatMessageResponse;
import com.qingqiu.openagent.model.vo.ChatMessageVO;
import com.qingqiu.openagent.service.AgentUsageLogService;
import com.qingqiu.openagent.service.ChatMessageFacadeService;
import com.qingqiu.openagent.service.SseService;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author: qingqiugeek
 * @date: 2026/5/4 09:14
 * @description: Chat agent
 */

@Slf4j
public class ChatAgent {
    // 智能体 ID
    private String agentId;

    // 名称
    private String name;

    // 描述
    private String description;

    // 默认系统提示词
    private String systemPrompt;

    // 交互实例
    private ChatModel chatModel;

    // 流式交互实例
    private StreamingChatModel streamingChatModel;

    // 状态
    private AgentState agentState;

    // 可用的工具
    private LangChainToolExecutor toolExecutor;

    // 可访问的知识库
    private List<KnowledgeBaseDTO> availableKbs;

    // 模型的聊天记录
    private Deque<ChatMessage> chatMemory;

    private int maxMessages;

    // 模型的聊天会话 ID
    private String chatSessionId;

    // 最多循环次数
    private static final Integer MAX_STEPS = 10;

    private static final Integer DEFAULT_MAX_MESSAGES = 50;

    // SSE 服务, 用于发送消息给前端
    private SseService sseService;

    private ChatMessageConverter chatMessageConverter;

    private ChatMessageFacadeService chatMessageFacadeService;

    private AgentStopRegistry stopRegistry;

    // 最后一次的 AiMessage
    private AiMessage lastAiMessage;

    // AI 返回的，已经持久化，但是需要 sse 发给前端的消息
    private final List<ChatMessageDTO> pendingChatMessages = new ArrayList<>();

    // 是否启用联网搜索（仅作日志/标记用，工具注入逻辑在 ChatAgentFactory）
    @Setter
    private boolean webSearch = false;

    // 累计的来源引用（web 搜索 + 知识库召回）。每一轮 execute 后追加，下一轮 think 的最终回复会带上。
    private final List<ChatMessageDTO.Source> accumulatedSources = new ArrayList<>();

    // ========== 用量埋点（agent_usage_log） ==========
    /** 由 ChatAgentFactory 注入；为 null 表示不埋点。 */
    @Setter
    private AgentUsageLogService agentUsageLogService;

    @Setter
    private Long usageUserId;

    @Setter
    private Long usageModelId;

    /** normal / agent / web_search ... */
    @Setter
    private String chatMode;

    private long usageStartMs;
    private int usagePromptTokens;
    private int usageCompletionTokens;
    private int usageTotalTokens;
    private String usageStatus = "success";
    private String usageErrorMsg;

    public ChatAgent() {
    }

    public ChatAgent(String agentId,
                     String name,
                     String description,
                     String systemPrompt,
                     ChatModel chatModel,
                     StreamingChatModel streamingChatModel,
                     Integer maxMessages,
                     List<ChatMessage> memory,
                     LangChainToolExecutor toolExecutor,
                     List<KnowledgeBaseDTO> availableKbs,
                     String chatSessionId,
                     SseService sseService,
                     ChatMessageFacadeService chatMessageFacadeService,
                     ChatMessageConverter chatMessageConverter,
                     AgentStopRegistry stopRegistry
    ) {
        this.agentId = agentId;
        this.name = name;
        this.description = description;
        this.systemPrompt = systemPrompt;

        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;

        this.toolExecutor = toolExecutor;
        this.availableKbs = availableKbs;
        this.maxMessages = maxMessages == null ? DEFAULT_MAX_MESSAGES : maxMessages;

        this.chatSessionId = chatSessionId;
        this.sseService = sseService;

        this.chatMessageFacadeService = chatMessageFacadeService;
        this.chatMessageConverter = chatMessageConverter;
        this.stopRegistry = stopRegistry;

        this.agentState = AgentState.IDLE;

        this.chatMemory = new ArrayDeque<>();
        if (memory != null) {
            for (ChatMessage message : memory) {
                addToMemory(message);
            }
        }

        // 添加系统提示
        if (StringUtils.hasLength(systemPrompt)) {
            addToMemory(SystemMessage.from(systemPrompt));
        }
    }

    private void addToMemory(ChatMessage message) {
        this.chatMemory.addLast(message);
        evictUntilWithinLimit();
    }

    /**
     * 智能滑窗淘汰：在保证不破坏 OpenAI 协议配对的前提下裁到 maxMessages 以内。
     *
     * <p>规则（按优先级）：
     * <ol>
     *   <li><b>永远保留首条 SystemMessage</b>：缺它会让 messages[0] 变成 assistant/tool，多数 OpenAI 兼容端会 400</li>
     *   <li><b>按工具轮次整体淘汰</b>：要删 {@code AiMessage(toolCalls=N)} 就必须把它后面紧跟的 N 条 ToolResult 一起删，
     *       否则会产生 orphan ToolResult，langchain4j / OpenAI 端会因找不到对应 tool_call_id 而 400</li>
     *   <li><b>开头是 ToolResult 也必须删</b>：会话首条不可能是 tool，必删</li>
     * </ol>
     *
     * <p>极端情况：若全是 SystemMessage 撑爆 maxMessages（理论上不会），循环退出避免死循环。
     */
    private void evictUntilWithinLimit() {
        int safetyGuard = this.chatMemory.size() * 2;
        while (this.chatMemory.size() > this.maxMessages && safetyGuard-- > 0) {
            // 拿出"待考察"的最老消息——通常就是 first，但若 first 是 system 则考察 second
            java.util.Iterator<ChatMessage> it = this.chatMemory.iterator();
            ChatMessage first = it.hasNext() ? it.next() : null;
            ChatMessage candidate;
            boolean skipSystem;
            if (first instanceof SystemMessage && it.hasNext()) {
                candidate = it.next();
                skipSystem = true;
            } else {
                candidate = first;
                skipSystem = false;
            }
            if (candidate == null) break;

            // 临时摘下 SystemMessage（保留首位地位）
            SystemMessage savedSystem = skipSystem ? (SystemMessage) this.chatMemory.pollFirst() : null;

            if (candidate instanceof AiMessage am && am.hasToolExecutionRequests()) {
                // 删整轮：AiMessage + 紧跟的 N 条 ToolResult
                this.chatMemory.pollFirst();
                int n = am.toolExecutionRequests().size();
                for (int k = 0; k < n; k++) {
                    if (this.chatMemory.peekFirst() instanceof ToolExecutionResultMessage) {
                        this.chatMemory.pollFirst();
                    } else break;
                }
            } else {
                // 普通消息（user / ai-no-tool / 罕见的 orphan tool）直接删
                this.chatMemory.pollFirst();
            }

            // SystemMessage 复位
            if (savedSystem != null) {
                this.chatMemory.addFirst(savedSystem);
            }
        }
    }

    // 打印工具调用信息
    private void logToolCalls(List<ToolExecutionRequest> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            log.debug("[ToolCalling] 无工具调用");
            return;
        }
        String logMessage = IntStream.range(0, toolCalls.size())
                .mapToObj(i -> {
                    ToolExecutionRequest call = toolCalls.get(i);
                    return String.format(
                            "[ToolCalling #%d]\n- name      : %s\n- arguments : %s",
                            i + 1,
                            call.name(),
                            call.arguments()
                    );
                })
                .collect(Collectors.joining("\n\n"));
        if (log.isDebugEnabled()) {
            log.debug("========== Tool Calling ==========\n{}\n=================================", logMessage);
        }
    }

    // 持久化 Message, 返回 chatMessageId
    // 需要 Agent 持久化的 Message 子类有以下两类
    // AssistantMessage
    // ToolResponseMessage

    // SystemMessage 不需要持久化
    // UserMessage 在每次用户发送问题之间就已经持久化过了
        private void saveMessage(ChatMessage message) {
        ChatMessageDTO.ChatMessageDTOBuilder builder = ChatMessageDTO.builder();
        if (message instanceof AiMessage assistantMessage) {
            List<ChatMessageDTO.ToolCall> toolCalls = assistantMessage.toolExecutionRequests() == null
                ? List.of()
                : assistantMessage.toolExecutionRequests().stream()
                .map(req -> ChatMessageDTO.ToolCall.builder()
                    .id(req.id())
                    .name(req.name())
                    .arguments(req.arguments())
                    .build())
                .toList();

            // 仅在「终态回复」（没有 toolCalls）才附带累计的 sources
            List<ChatMessageDTO.Source> sourcesForMessage =
                    assistantMessage.hasToolExecutionRequests()
                            ? null
                            : (accumulatedSources.isEmpty() ? null : new ArrayList<>(accumulatedSources));

            ChatMessageDTO chatMessageDTO = builder.role(ChatMessageDTO.RoleType.ASSISTANT)
                .content(assistantMessage.text())
                    .sessionId(this.chatSessionId)
                    .metadata(ChatMessageDTO.MetaData.builder()
                            .toolCalls(toolCalls)
                            .sources(sourcesForMessage)
                            .build())
                    .build();
            CreateChatMessageResponse chatMessage = chatMessageFacadeService.createChatMessage(chatMessageDTO);
            chatMessageDTO.setId(chatMessage.getChatMessageId());
            pendingChatMessages.add(chatMessageDTO);
        } else if (message instanceof ToolExecutionResultMessage toolResponseMessage) {
            ChatMessageDTO chatMessageDTO = builder.role(ChatMessageDTO.RoleType.TOOL)
                .content(toolResponseMessage.text())
                .sessionId(this.chatSessionId)
                .metadata(ChatMessageDTO.MetaData.builder()
                    .toolResponse(ChatMessageDTO.ToolResponse.builder()
                        .id(toolResponseMessage.id())
                        .name(toolResponseMessage.toolName())
                        .responseData(toolResponseMessage.text())
                        .build())
                    .build())
                .build();
            CreateChatMessageResponse chatMessage = chatMessageFacadeService.createChatMessage(chatMessageDTO);
            chatMessageDTO.setId(chatMessage.getChatMessageId());
            pendingChatMessages.add(chatMessageDTO);
        } else {
            throw new IllegalArgumentException("不支持的 Message 类型: " + message.getClass().getName());
        }
    }

    // 刷新 pendingMessages, 将数据通过 sse 发送给前端
    private void refreshPendingMessages() {
        for (ChatMessageDTO message : pendingChatMessages) {
            ChatMessageVO vo = chatMessageConverter.toVO(message);
            SseMessage sseMessage = SseMessage.builder()
                    .type(SseMessage.Type.AI_GENERATED_CONTENT)
                    .payload(SseMessage.Payload.builder()
                            .message(vo)
                            .build())
                    .metadata(SseMessage.Metadata.builder()
                            .chatMessageId(message.getId())
                            .build())
                    .build();
            sseService.send(this.chatSessionId, sseMessage);
        }
        pendingChatMessages.clear();
    }

    /** 流式聊天最多重试次数（不含首次）。 */
    private static final int STREAM_MAX_RETRIES = 2;
    /** 第 n 次重试前的退避基数（毫秒），实际等待 = base * (1 << n)。 */
    private static final long STREAM_RETRY_BASE_MS = 400L;

    // 使用流式模型获取响应，并将文本增量通过 SSE 推送给前端；遇到网络抖动自动重试。
    private ChatResponse streamChat(ChatRequest request) {
        int attempt = 0;
        while (true) {
            try {
                return streamChatOnce(request);
            } catch (RuntimeException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                if (attempt >= STREAM_MAX_RETRIES || !isRetryable(cause)) {
                    throw e;
                }
                long backoff = STREAM_RETRY_BASE_MS * (1L << attempt);
                attempt++;
                log.warn("[ChatAgent] streamChat 第 {} 次重试 (backoff={}ms), 原因: {}",
                        attempt, backoff, cause.getMessage());
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    /** 判断异常是否值得重试：仅网络层瞬时错误（连接重置 / SocketTimeout / IOException）。 */
    private boolean isRetryable(Throwable cause) {
        if (cause instanceof java.net.SocketTimeoutException) return true;
        if (cause instanceof java.net.SocketException) return true;
        if (cause instanceof java.io.IOException) {
            String msg = cause.getMessage();
            return msg != null && (msg.contains("Connection reset")
                    || msg.contains("Connection closed")
                    || msg.contains("timeout"));
        }
        return false;
    }

    private ChatResponse streamChatOnce(ChatRequest request) {
        if (streamingChatModel == null) {
            ChatResponse resp = chatModel.chat(request);
            accumulateTokens(resp);
            return resp;
        }
        CompletableFuture<ChatResponse> future = new CompletableFuture<>();
        streamingChatModel.chat(request, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                if (partialResponse == null || partialResponse.isEmpty()) return;
                try {
                    SseMessage chunk = SseMessage.builder()
                            .type(SseMessage.Type.AI_MESSAGE_CHUNK)
                            .payload(SseMessage.Payload.builder()
                                    .delta(partialResponse)
                                    .build())
                            .build();
                    sseService.send(chatSessionId, chunk);
                } catch (Exception e) {
                    log.warn("发送流式片段失败: {}", e.getMessage());
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                accumulateTokens(completeResponse);
                future.complete(completeResponse);
            }

            @Override
            public void onError(Throwable error) {
                future.completeExceptionally(error);
            }
        });
        try {
            return future.get();
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new RuntimeException("流式聊天失败: " + cause.getMessage(), cause);
        }
    }

    // ============================================================
    // 工具循环保护：防止模型在同一工具上死磕
    // ------------------------------------------------------------
    // 触发场景：tavily 返回 answer=null、检索结果空、工具失败等情况下，
    // 模型容易换 query 反复重试，永远不 directAnswer。
    // 双层保护：
    //   L1 (≥WARN_THRESHOLD): 往 thinkPrompt 注入 urgent_override，警告模型必须收尾
    //   L2 (≥HARD_THRESHOLD): ChatAgent 直接合成兜底 AiMessage，强制 FINISHED
    // ============================================================
    private static final int LOOP_WARN_THRESHOLD = 3;
    private static final int LOOP_HARD_THRESHOLD = 4;

    /**
     * 根据当前 toolCallCounts 构造 L1 警告段。某工具 ≥ LOOP_WARN_THRESHOLD
     * 但 < LOOP_HARD_THRESHOLD 时插入；否则返回空串。
     */
    private static String buildLoopOverride(java.util.Map<String, Integer> counts) {
        String hotTool = null;
        int hotCount = 0;
        for (var e : counts.entrySet()) {
            if (e.getValue() >= LOOP_WARN_THRESHOLD && e.getValue() > hotCount) {
                hotCount = e.getValue();
                hotTool = e.getKey();
            }
        }
        if (hotTool == null) return "";
        return String.format("""

                <urgent_override>
                ⚠️ 检测到工具 `%s` 在本会话中已被调用 %d 次。**本轮你必须立即调用 `directAnswer`**：
                把已收集到的任何信息（即使不完整）整合成最终答案交付用户；如确实无法回答就坦诚告知。
                **禁止**再以任何参数调用 `%s` 或其它检索/查询工具。
                </urgent_override>
                """, hotTool, hotCount, hotTool);
    }

    /** 统计本会话内每个非 directAnswer 工具被调用了多少次。 */
    private java.util.Map<String, Integer> countToolCallsInMemory() {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        for (ChatMessage m : this.chatMemory) {
            if (m instanceof AiMessage am && am.hasToolExecutionRequests()) {
                for (ToolExecutionRequest req : am.toolExecutionRequests()) {
                    if (!"directAnswer".equals(req.name())) {
                        counts.merge(req.name(), 1, Integer::sum);
                    }
                }
            }
        }
        return counts;
    }

    // thinkPrompt 应该放到 system 中还是
    private boolean think() {
        // 当前真实时间，用于锚定时效性问题。
        String nowText = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // L2 硬兜底：某工具被调用次数 ≥ LOOP_HARD_THRESHOLD，直接强制结束。
        // 不再 think，避免无意义的额外 LLM 调用与 token 浪费。
        java.util.Map<String, Integer> toolCallCounts = countToolCallsInMemory();
        String overflowTool = null;
        int overflowCount = 0;
        for (var e : toolCallCounts.entrySet()) {
            if (e.getValue() > overflowCount) {
                overflowCount = e.getValue();
                overflowTool = e.getKey();
            }
        }
        if (overflowTool != null && overflowCount >= LOOP_HARD_THRESHOLD) {
            log.warn("[ChatAgent] 检测到工具循环 tool={} count={}，强制收尾", overflowTool, overflowCount);
            String fallback = String.format(
                    "我已多次尝试通过 `%s` 工具获取信息（共 %d 次），但仍未能拿到足够明确的结果。" +
                    "可能原因：该工具暂时不可用、查询条件过于具体、或目标信息确实难以从公开渠道实时获得。" +
                    "建议你换个角度提问，或稍后再试。",
                    overflowTool, overflowCount);
            AiMessage synthetic = AiMessage.from(fallback);
            addToMemory(synthetic);
            saveMessage(synthetic);
            refreshPendingMessages();
            return false; // step() 看到 false 会把状态置为 FINISHED
        }

        // 工具卡片：name + description。LLM 仅依据描述判断能力，不要在代码里替它做分类。
        // description 截断 200 字，避免 prompt 失衡（个别 MCP 工具 desc 超长）。
        List<ToolSpecification> specs = this.toolExecutor.getToolSpecifications();
        String availableToolsText;
        if (specs == null || specs.isEmpty()) {
            availableToolsText = "（当前无任何工具可用）";
        } else {
            availableToolsText = specs.stream()
                    .map(s -> {
                        String desc = s.description() == null ? "" : s.description().trim();
                        if (desc.length() > 200) desc = desc.substring(0, 200) + "...";
                        return "- " + s.name() + (desc.isEmpty() ? "" : ": " + desc);
                    })
                    .collect(Collectors.joining("\n"));
        }

        // 知识库卡片：仅给 id + name + description，避免 LLM 过度依赖 toString 输出格式。
        String availableKbsText;
        if (this.availableKbs == null || this.availableKbs.isEmpty()) {
            availableKbsText = "（当前无任何知识库）";
        } else {
            availableKbsText = this.availableKbs.stream()
                    .map(kb -> "- id=" + kb.getId() + " | " + kb.getName()
                            + (kb.getDescription() == null ? "" : " | " + kb.getDescription()))
                    .collect(Collectors.joining("\n"));
        }

        String thinkPrompt = """
                你负责思考决策，本轮必须以**工具调用**的形式输出决策动作，不要输出自由文本。
                - 当任务天然可以并行（例如同时检索多个知识库、同时联网搜索 + 查知识库）且各工具之间**没有前后依赖**时，
                  允许在本轮一次性发起多个工具调用，平台会并行执行并把所有结果一起交给下一轮决策；
                - 有前后依赖（A 的输出作为 B 的输入）时，本轮只发 A，等 A 的结果回来下一轮再发 B。

                <context>
                - 当前时间: %s
                </context>

                <available_knowledge_bases>
                %s
                </available_knowledge_bases>

                <available_tools>
                %s
                </available_tools>

                <decision_procedure>
                按下列顺序判定用户消息所属类别，**取第一个匹配项**作为本轮动作来源：

                (A) **元对话 / 闲聊 / 能力询问**
                    判定：消息无需任何外部数据、计算或检索，仅凭通识或对自己身份的认知就能给出令人满意的回答。
                    例如：寒暄、问候、自我介绍、询问你是谁/能做什么、表达情绪、闲聊话题。
                    动作：直接调用 `directAnswer`，`message` 中给出自然友好的最终回复。

                (B) **后处理（上一步工具刚返回了实质内容）**
                    判定：对话历史中**最近一条 ToolExecutionResult** 含有图片链接 / 列表 / 数据 / 文本答案等可读内容。
                    动作：调用 `directAnswer`，把这些关键信息整合进 `message` 交给用户。**禁止**重复调同一个工具。

                (C) **需要外部能力的任务**
                    判定：任务依赖以下任意一种能力——最新资讯 / 特定领域数据 / 数学或代码计算 / 生成图像或音频 /
                    抓取或解析网页 / 数据库或知识库检索 / 任何超出你训练截止的事实。
                    特别地：消息含"今天 / 最近 / 最新 / 当前 / 本周 / 本月 / x 年 x 月以来 / 现在"等时效性表述时，
                    先用 <context> 当前时间锚定时间范围。
                    动作：在 <available_tools> 中**逐个阅读工具描述**，挑选语义最匹配的工具调用。
                    举例：要联网就找描述里提到 search / 搜索 / web / 联网 / news / 资讯 / 抓取等概念的工具；
                    要查知识库就找 knowledge / 召回 / retrieve 类工具并配合 <available_knowledge_bases> 选 id。

                (D) **任务需要某能力，但 <available_tools> 中没有匹配工具**
                    判定：阅读完所有工具描述后，找不到能完成该任务的工具。
                    动作：调用 `directAnswer`，**坦诚告知**用户当前 Agent 不具备该能力。"
                    **禁止**凭训练记忆编造时效性事实。

                (E) **可凭通识回答且无时效性**
                    判定：(C)(D) 都不成立，问题属于稳定知识（数学定理、历史事件、概念解释等）。
                    动作：直接 `directAnswer`，给出完整准确的最终答案。
                </decision_procedure>

                <hard_constraints>
                1. 工具能力**只能**从 <available_tools> 中真实列出的条目识别。不要假设存在未列出的工具，
                   不要把"用户没开启 X"作为回答理由——如果列表里有等价工具就直接用。
                2. `directAnswer.message` 必须非空、必须是给用户看的完整最终答案，禁止 "ok"/"好的"/"已收到"/空串。
                3. 每个会话最终必须以一次 `directAnswer` 收尾。不要在会话未给用户交付最终答案前处于“何也不做”的状态。
                4. 同一轮决策只调一个工具。需要多步（先检索再总结）时，本轮先调检索工具，下一轮再 `directAnswer`。
                5. **重复定义**：判断"是否重复调用"以**工具名**为准，不论参数。`tavily_search(q=A)` 与
                   `tavily_search(q=B)` 视为同一工具的两次调用。同名工具在本会话中累计调用 ≥ 3 次后，
                   **必须 directAnswer**，基于已收集到的任何信息给出最终答案，**禁止**再换 query 重试。
                6. **"信息够用"判断**：工具返回 JSON 含 `results`/`items`/`data`/`content` 等数组字段且非空时，
                   即视为获得了实质内容，**应当 directAnswer**。不要因 `answer`/`summary` 字段为 null 就再次重试。
                </hard_constraints>
                %s
                """.formatted(nowText, availableKbsText, availableToolsText, buildLoopOverride(toolCallCounts));

            // 清洗 chatMemory（仅针对本次请求的快照，不改 chatMemory 本身）：
            // 保证 messages[0] 是 system/user、工具轮次配对完整、AiMessage.text 非 null。
            List<ChatMessage> baseMessages = MessageSanitizer.sanitize(this.chatMemory);
            // 关键：thinkPrompt 必须放在 messages **最前面**作为首条 SystemMessage。
            // 之前放在末尾时，某些 OpenAI 兼容端（DeepSeek/Qwen 等）会把它当成"用户上下文延续"，
            // 导致模型直接 echo thinkPrompt 内容当作回答（看到过 "你负责思考决策..." 被原文输出）。
            // 放在最前面后，它就是清晰的"系统指令"，模型不会再回显。
            List<ChatMessage> requestMessages = new ArrayList<>();
            requestMessages.add(SystemMessage.from(thinkPrompt));
            requestMessages.addAll(baseMessages);

            ChatRequest request = ChatRequest.builder()
                .messages(requestMessages)
                .toolSpecifications(this.toolExecutor.getToolSpecifications())
                .build();

            ChatResponse response = streamChat(request);
            Assert.notNull(response, "Chat response cannot be null");

            AiMessage output = response.aiMessage();
            Assert.notNull(output, "AI message cannot be null");

            List<ToolExecutionRequest> toolCalls = output.toolExecutionRequests();

            this.lastAiMessage = output;
            addToMemory(output);

        // 保存
        saveMessage(output);
        // 刷新消息，将 AI 回复通过 SSE 发送给前端
        refreshPendingMessages();

        // 打印工具调用
        logToolCalls(toolCalls);

        // 如果工具调用不为空，则进入执行阶段
        return output.hasToolExecutionRequests();
    }

    // 执行
    private void execute() {
        Assert.notNull(this.lastAiMessage, "Last AI message cannot be null");

        if (!this.lastAiMessage.hasToolExecutionRequests()) {
            return;
        }

        List<ToolExecutionRequest> requests = this.lastAiMessage.toolExecutionRequests();

        // ============================================================
        // 同轮 dedup（方案 B）：相同工具名只真正执行 1 次，重复者共享首个结果
        // ------------------------------------------------------------
        // 触发场景：模型在同一轮 think 里发起多个 knowledgeTool / tavily_search 等
        // 同名调用（不同 query 但语义相近）。直接执行会浪费 token、产生重复引用来源。
        // 实现：建立 name -> 首次出现 idx 的映射，N 个相同 name 的 toolCall 都共用
        //       同一个 future 的结果；toolCall ↔ toolResult 仍按 1:1 顺序对齐，
        //       不破坏 langchain4j 配对校验。
        // ============================================================
        java.util.Map<String, Integer> nameToFirstIdx = new java.util.LinkedHashMap<>();
        int[] sharedIdx = new int[requests.size()];
        for (int i = 0; i < requests.size(); i++) {
            String name = requests.get(i).name();
            Integer first = nameToFirstIdx.get(name);
            if (first == null) {
                nameToFirstIdx.put(name, i);
                sharedIdx[i] = i;
            } else {
                sharedIdx[i] = first;
                log.info("[ChatAgent] 同轮内重复工具 dedup: name={} idx={} 复用 idx={} 的结果",
                        name, i, first);
            }
        }

        // 仅对首次出现的请求真正并行执行
        List<CompletableFuture<String>> futures = new ArrayList<>(requests.size());
        for (int i = 0; i < requests.size(); i++) {
            if (sharedIdx[i] != i) {
                futures.add(null); // 占位，后面按 sharedIdx 映射读取
                continue;
            }
            final ToolExecutionRequest request = requests.get(i);
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return toolExecutor.execute(request);
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    String reason = cause.getMessage() != null
                            ? cause.getMessage()
                            : cause.getClass().getSimpleName();
                    log.warn("工具调用失败: tool={} args={} cause={} reason={}",
                            request.name(), request.arguments(),
                            cause.getClass().getSimpleName(), reason);
                    return "Tool error: " + reason;
                }
            }));
        }

        // 按请求顺序串行处理结果：保持 toolCall ↔ toolResult 的顺序对齐，
        // 避免 chatMemory 顺序错乱导致下一轮 LLM 请求报错。
        List<ToolExecutionResultMessage> toolResults = new ArrayList<>(requests.size());
        int processedCount = 0;
        try {
        for (int i = 0; i < requests.size(); i++) {
            ToolExecutionRequest request = requests.get(i);
            String result;
            try {
                // dedup：重复 toolCall 复用首个 idx 的执行结果
                result = futures.get(sharedIdx[i]).get();
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                result = "Tool error: " + cause.getMessage();
            }
            // langchain4j 要求 text 不能为空，兜底为 "ok"
            if (result == null || result.isBlank()) {
                result = "ok";
            }
            // 提取来源引用（dedup：重复 toolCall 共享同一 result，只对首次 idx 提取一次）
            if (sharedIdx[i] == i) {
                try {
                    extractSources(request.name(), result);
                } catch (Exception e) {
                    log.warn("提取 sources 失败: tool={}", request.name(), e);
                }
            }
            ToolExecutionResultMessage resultMessage = ToolExecutionResultMessage.from(request, result);
            toolResults.add(resultMessage);
            addToMemory(resultMessage);

            // directAnswer 是「最终答案」工具：不应作为工具卡片展示给用户，
            // 而应该把它的内容作为 AI 正文消息呈现。
            // - tool 结果仍要保留在 chatMemory 中（保持 toolCall/toolResult 配对，避免下次请求报错）
            // - 但不持久化 tool 消息、不通过 SSE 推 tool 卡片
            // - 取 directAnswer 入参 `message`（优先）或返回值，作为 AI 文本消息保存+推送
            if ("directAnswer".equals(request.name())) {
                String answerText = extractDirectAnswerText(request.arguments(), result);
                if (StringUtils.hasText(answerText)) {
                    AiMessage synthetic = AiMessage.from(answerText);
                    saveMessage(synthetic);
                }
            } else {
                saveMessage(resultMessage);
            }
            processedCount = i + 1;
        }
        } finally {
            // 兜底：若执行中途抛异常 / 被中断（stopRegistry），把未处理的 toolCall 补占位 ToolResult。
            // 不补的话 chatMemory 会留下 orphan AiMessage(toolCalls)，下一轮请求必 400。
            // 占位文本明确写明"aborted"，下一轮 think 模型能感知并相应地 directAnswer。
            for (int i = processedCount; i < requests.size(); i++) {
                ToolExecutionRequest req = requests.get(i);
                ToolExecutionResultMessage placeholder =
                        ToolExecutionResultMessage.from(req, "Tool execution aborted: interrupted or error");
                addToMemory(placeholder);
                log.warn("[ChatAgent] execute 中断，补占位 ToolResult: idx={} tool={}", i, req.name());
            }
        }

        if (log.isDebugEnabled()) {
            String collect = toolResults
                .stream()
                .map(resp -> "工具" + resp.toolName() + "的返回结果为：" + resp.text())
                .collect(Collectors.joining("\n"));
            log.debug("工具调用结果：{}", collect);
        }

        refreshPendingMessages();

        // directAnswer 是唯一的会话结束信号：模型主动决定“本轮已给出最终回复”。
        // terminate 已废弃：之前它的唯一价值“只结束不说话”反而会造成用户看不到内容，
        // 现在强制模型始终用 directAnswer.message 交付结果，体验更一致。
        boolean shouldFinish = toolResults.stream()
                .map(ToolExecutionResultMessage::toolName)
                .anyMatch("directAnswer"::equals);
        if (shouldFinish) {
            this.agentState = AgentState.FINISHED;
            log.debug("任务结束");
        }
    }

    /**
     * 从 directAnswer 工具的入参 / 返回值中解析出最终答案文本。
     * 优先取入参 `message`（模型应把答案放这里），失败/为空时回退到工具返回字符串。
     */
    private String extractDirectAnswerText(String argumentsJson, String fallbackResult) {
        try {
            if (StringUtils.hasText(argumentsJson) && JSONUtil.isTypeJSONObject(argumentsJson)) {
                JSONObject obj = JSONUtil.parseObj(argumentsJson);
                String msg = obj.getStr("message");
                if (StringUtils.hasText(msg)) {
                    return msg;
                }
            }
        } catch (Exception e) {
            log.warn("解析 directAnswer 入参失败: {}", argumentsJson, e);
        }
        // 回退：工具方法体本身返回的就是 message
        if (fallbackResult != null && !"ok".equals(fallbackResult)) {
            return fallbackResult;
        }
        return "";
    }

    /**
     * 从工具执行结果中提取「来源引用」累积到 accumulatedSources。
     * 支持两类工具：
     *  - webSearch：JSON 形如 {"results":[{"title","url","content","score"}], "images":[...]}
     *  - KnowledgeTool：JSON 形如 {"kbId","results":[{"content"}]}
     */
    private void extractSources(String toolName, String resultJson) {
        if (toolName == null || resultJson == null) return;
        if (!JSONUtil.isTypeJSONObject(resultJson)) return;
        JSONObject json = JSONUtil.parseObj(resultJson);

        if ("webSearch".equals(toolName)) {
            JSONArray results = json.getJSONArray("results");
            if (results == null) return;
            for (int i = 0; i < results.size(); i++) {
                JSONObject item = results.getJSONObject(i);
                String url = item.getStr("url", "");
                if (url.isBlank()) continue;
                accumulatedSources.add(ChatMessageDTO.Source.builder()
                        .type("web")
                        .title(item.getStr("title", url))
                        .url(url)
                        .content(item.getStr("content", ""))
                        .score(item.getDouble("score", 0d))
                        .build());
            }
        } else if ("KnowledgeTool".equals(toolName)) {
            String kbId = json.getStr("kbId", "");
            String kbName = lookupKbName(kbId);
            JSONArray results = json.getJSONArray("results");
            if (results == null) return;
            for (int i = 0; i < results.size(); i++) {
                JSONObject item = results.getJSONObject(i);
                String content = item.getStr("content", "");
                if (content.isBlank()) continue;
                String title = content.length() > 30 ? content.substring(0, 30) + "..." : content;
                accumulatedSources.add(ChatMessageDTO.Source.builder()
                        .type("kb")
                        .title("片段 " + (i + 1) + "：" + title)
                        .content(content)
                        .kbName(kbName)
                        .build());
            }
        }
    }

    private final Map<String, String> kbNameCache = new LinkedHashMap<>();

    private String lookupKbName(String kbId) {
        if (kbId == null || kbId.isBlank()) return "";
        if (kbNameCache.containsKey(kbId)) {
            return kbNameCache.get(kbId);
        }
        String name = "";
        if (availableKbs != null) {
            for (KnowledgeBaseDTO kb : availableKbs) {
                if (kbId.equals(kb.getId())) {
                    name = kb.getName() == null ? "" : kb.getName();
                    break;
                }
            }
        }
        kbNameCache.put(kbId, name);
        return name;
    }

    // 单个步骤模板
    private void step() {
        if (think()) {
            execute();
        } else { // 没有工具调用
            agentState = AgentState.FINISHED;
        }
    }

    // 运行
    public void run() {
        if (agentState != AgentState.IDLE) {
            throw new IllegalStateException("Agent is not idle");
        }
        this.usageStartMs = System.currentTimeMillis();
        this.usageStatus = "success";
        this.usageErrorMsg = null;

        try {
            for (int i = 0; i < MAX_STEPS && agentState != AgentState.FINISHED; i++) {
                // 用户主动终止
                if (stopRegistry != null && stopRegistry.isStopRequested(this.chatSessionId)) {
                    log.info("收到用户终止请求，停止 Agent 循环, sessionId={}", this.chatSessionId);
                    agentState = AgentState.FINISHED;
                    break;
                }
                // 当前步骤，用于实现 Agent Loop
                int currentStep = i + 1;
                step();
                if (currentStep >= MAX_STEPS) {
                    agentState = AgentState.FINISHED;
                    log.warn("Max steps reached, stopping agent");
                }
            }
            agentState = AgentState.FINISHED;
        } catch (Exception e) {
            agentState = AgentState.ERROR;
            this.usageStatus = "error";
            this.usageErrorMsg = e.getMessage();
            log.error("Error running agent", e);
            throw new RuntimeException("Error running agent", e);
        } finally {
            // 通知前端 agent 已完成
            try {
                SseMessage doneMsg = SseMessage.builder()
                        .type(SseMessage.Type.AI_DONE)
                        .payload(SseMessage.Payload.builder().done(true).build())
                        .build();
                sseService.send(this.chatSessionId, doneMsg);
            } catch (Exception ignore) {
                // ignore
            }
            if (stopRegistry != null) {
                stopRegistry.clear(this.chatSessionId);
            }
            flushUsageLog();
        }
    }

    /** 累加一次 ChatResponse 的 token。 */
    private void accumulateTokens(ChatResponse resp) {
        if (resp == null) return;
        TokenUsage usage = resp.tokenUsage();
        if (usage == null) return;
        if (usage.inputTokenCount() != null) usagePromptTokens += usage.inputTokenCount();
        if (usage.outputTokenCount() != null) usageCompletionTokens += usage.outputTokenCount();
        if (usage.totalTokenCount() != null) usageTotalTokens += usage.totalTokenCount();
    }

    /** 写一条 agent_usage_log（异步，吞掉异常）。 */
    private void flushUsageLog() {
        if (agentUsageLogService == null || usageUserId == null) return;
        try {
            int latency = (int) (System.currentTimeMillis() - usageStartMs);
            AgentUsageLog entry = AgentUsageLog.builder()
                    .userId(usageUserId)
                    .agentId(this.agentId)
                    .sessionId(this.chatSessionId)
                    .modelId(usageModelId)
                    .chatMode(chatMode)
                    .promptTokens(usagePromptTokens)
                    .completionTokens(usageCompletionTokens)
                    .totalTokens(usageTotalTokens)
                    .latencyMs(latency)
                    .status(usageStatus)
                    .errorMsg(usageErrorMsg)
                    .build();
            agentUsageLogService.recordAsync(entry);
        } catch (Exception e) {
            log.warn("[AgentUsageLog] flush failed: {}", e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "OpenAgent {" +
                "name = " + name + ",\n" +
                "description = " + description + ",\n" +
                "agentId = " + agentId + ",\n" +
                "systemPrompt = " + systemPrompt + "}";
    }
}
