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
import java.util.Iterator;
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
    private static final Integer MAX_STEPS = 20;

    private static final Integer DEFAULT_MAX_MESSAGES = 20;

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
        while (this.chatMemory.size() > this.maxMessages) {
            this.chatMemory.removeFirst();
        }
    }

    // 打印工具调用信息
    private void logToolCalls(List<ToolExecutionRequest> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            log.info("\n\n[ToolCalling] 无工具调用");
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
        log.info("\n\n========== Tool Calling ==========\n{}\n=================================\n", logMessage);
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

    // 使用流式模型获取响应，并将文本增量通过 SSE 推送给前端
    private ChatResponse streamChat(ChatRequest request) {
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

    // thinkPrompt 应该放到 system 中还是
    private boolean think() {
        String webSearchHint = webSearch
                ? "\n- 用户已开启「联网搜索」，遇到时效性、新闻、最新数据等问题时，**必须**调用 webSearch 工具获取实时信息，再基于搜索结果回答；不要凭训练记忆作答。"
                : "\n- 用户未开启联网搜索；如遇明显时效性问题（今天/最近/最新/当前/x 年 x 月以来等），请提示用户开启联网搜索或调用相关工具。";
        // 把当前时间注入 prompt，避免模型用 2024 等过期数据回答时效性问题
        String nowText = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String thinkPrompt = """
                现在你是OpenAgent智能体的具体「决策模块」
                请根据当前对话上下文，决定下一步的动作。
                                \s
                【额外信息】
                - 当前系统时间（**真实当前时间，请以此为准，不要使用训练数据中的旧时间**）：%s
                - 你目前拥有的知识库列表以及描述：%s
                - 如果有缺失的上下文时，优先从知识库中进行搜索%s

                【输出规则（务必遵守）】
                - 当上一步刚刚有「返回了具体内容」的工具（例如 webSearch、知识库检索、qwenGenerateImage 等）时，
                  必须用 `directAnswer` 工具，把工具返回的关键信息（图片链接、引用、答案）原样写进 `message` 参数交给用户；
                  不要直接 `terminate`，否则用户看不到任何输出。
                - 调用 `directAnswer` 时，`message` 参数必须非空，且包含完整的最终答案；不要传空串或 "ok"。
                - 仅当确实无内容可回（如已经在上一条回复里说完）才用 `terminate`。
                - 涉及"今天/最近/最新/当前/x 年 x 月以来"等时效性问题时，先用上面的当前时间锚定时间范围；
                  必要时调用 webSearch（用户已开启联网搜索）或时间工具校准，再回答。
                """.formatted(nowText, this.availableKbs, webSearchHint);

            List<ChatMessage> requestMessages = new ArrayList<>(this.chatMemory);
            requestMessages.add(SystemMessage.from(thinkPrompt));

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

        List<ToolExecutionResultMessage> toolResults = new ArrayList<>();
        for (ToolExecutionRequest request : this.lastAiMessage.toolExecutionRequests()) {
            String result;
            try {
                result = toolExecutor.execute(request);
            } catch (Exception e) {
                // 工具执行失败：把错误回传给模型，让它有机会重试或放弃；不要中断整个 agent
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                log.warn("工具调用失败: tool={} args={} reason={}",
                        request.name(), request.arguments(), cause.getMessage());
                result = "Tool error: " + cause.getMessage();
            }
            // langchain4j 要求 text 不能为空，兜底为 "ok"
            if (result == null || result.isBlank()) {
                result = "ok";
            }
            // 提取来源引用
            try {
                extractSources(request.name(), result);
            } catch (Exception e) {
                log.warn("提取 sources 失败: tool={}", request.name(), e);
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
        }

        String collect = toolResults
            .stream()
            .map(resp -> "工具" + resp.toolName() + "的返回结果为：" + resp.text())
            .collect(Collectors.joining("\n"));

        log.info("工具调用结果：{}", collect);

        refreshPendingMessages();

        // directAnswer / terminate 都是“任务已完成”信号：
        // - terminate：明确的结束信号
        // - directAnswer：模型表示本轮已给出答案、不需要再调其他工具
        // 以前仅 terminate 才结束，导致“你是谁”这种简单问题也会多次 think 后才结束。
        boolean shouldFinish = toolResults.stream()
                .map(ToolExecutionResultMessage::toolName)
                .anyMatch(name -> "terminate".equals(name) || "directAnswer".equals(name));
        if (shouldFinish) {
            // 兜底：模型直接 terminate 但本轮 AI 正文为空（没复述上一轮工具结果），
            // 把最近一个有内容的工具返回（如 qwenGenerateImage 的 markdown）作为最终回复推给用户。
            boolean onlyTerminate = toolResults.stream()
                    .map(ToolExecutionResultMessage::toolName)
                    .allMatch("terminate"::equals);
            if (onlyTerminate
                    && (this.lastAiMessage == null || !StringUtils.hasText(this.lastAiMessage.text()))) {
                String fallback = findRecentToolResultText();
                if (StringUtils.hasText(fallback)) {
                    log.info("[ChatAgent] terminate 兜底：使用上一轮工具结果作为最终回复");
                    AiMessage synthetic = AiMessage.from(fallback);
                    saveMessage(synthetic);
                    refreshPendingMessages();
                }
            }
            this.agentState = AgentState.FINISHED;
            log.info("任务结束");
        }
    }

    /**
     * 从 chatMemory 倒序找最近一个「非 terminate / 非 directAnswer」工具的返回文本。
     * 用于 terminate 时模型没复述工具结果的兜底。
     */
    private String findRecentToolResultText() {
        // chatMemory 是 Deque，没有 get(int)；用 descendingIterator 从尾到头遍历
        Iterator<ChatMessage> it = this.chatMemory.descendingIterator();
        while (it.hasNext()) {
            ChatMessage cm = it.next();
            if (cm instanceof ToolExecutionResultMessage tr) {
                String name = tr.toolName();
                if ("terminate".equals(name) || "directAnswer".equals(name)) continue;
                String text = tr.text();
                if (StringUtils.hasText(text) && !"ok".equals(text)) {
                    return text;
                }
            }
        }
        return null;
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
