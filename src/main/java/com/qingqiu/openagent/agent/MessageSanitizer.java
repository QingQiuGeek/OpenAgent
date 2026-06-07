package com.qingqiu.openagent.agent;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 消息流防破损清洗器（不做"压缩"，只做"清洗"）。
 *
 * <p>每次 think 把 chatMemory 喂给 LLM 之前过一遍，确保：
 * <ul>
 *   <li>messages[0] 必须是 SystemMessage 或 UserMessage（OpenAI 兼容端硬要求）</li>
 *   <li>每个 {@code AiMessage(hasToolCalls)} 后面紧跟数量对齐的 {@code ToolExecutionResultMessage}，
 *       否则整轮丢弃，避免 langchain4j 因 toolCall/toolResult 配对不全报 400</li>
 *   <li>{@code AiMessage.text() == null} 强制改为空串，避免某些 SDK 版本把 null 序列化成 {} 对象</li>
 * </ul>
 *
 * <p>常见的"破损来源"：会话首条消息被滑窗淘汰、MAX_STEPS 兜底退出导致最后一轮工具结果缺失、
 * loadMemory 从 DB 边界恢复时切到一半的工具轮次。
 *https://mp.weixin.qq.com/s/9FVBngl6E36sAiZgtbdP_A
 * TODO: 滑动窗口（evictUntilWithinLimit）因 AiMessage 与 ToolExecutionResultMessage 配对关系导致的问题：
 *       1. 问题根因：滑窗按消息条数淘汰，但一条 AiMessage(toolCalls=N) 逻辑上关联 N 条 ToolResult，
 *          淘汰时必须整组删除，否则产生 orphan（孤立的 ToolResult 或缺少 ToolResult 的 AiMessage）。
 *       2. 当前解决方案：evictUntilWithinLimit 中按工具轮次整体淘汰，MessageSanitizer 二次兜底清洗。
 *       3. 后续优化方向：
 *          a) 将滑窗的淘汰单位从"单条消息"改为"逻辑轮次"（一个 user→ai→tool→ai 完整交互算一轮），
 *             从根本上避免配对断裂；
 *          b) MAX_STEPS 退出时，若最后一步 AiMessage 带 toolCalls 但未执行，
 *             应在退出前补全 ToolExecutionResultMessage 占位（而非依赖 MessageSanitizer 丢弃），
 *             这样即使中间轮次被裁剪也不会产生 orphan；
 *          c) DB 恢复时 loadMemory 按 toolCall.id 与 toolResult.id 严格配对校验，
 *             而非仅靠数量对齐，提升边界恢复的健壮性。
 */
@Slf4j
public final class MessageSanitizer {

    private MessageSanitizer() {}

    public static List<ChatMessage> sanitize(Collection<ChatMessage> source) {
        if (source == null || source.isEmpty()) return List.of();
        List<ChatMessage> input = (source instanceof List<ChatMessage> l) ? l : new ArrayList<>(source);

        List<ChatMessage> out = new ArrayList<>(input.size());
        int dropped = 0;
        int i = 0;

        // 步骤 1：剔除头部所有"非法开头"消息（必须以 system / user 起头）
        while (i < input.size()) {
            ChatMessage m = input.get(i);
            if (m instanceof SystemMessage || m instanceof UserMessage) break;
            if (m instanceof AiMessage am && am.hasToolExecutionRequests()) {
                int j = i + 1;
                while (j < input.size() && input.get(j) instanceof ToolExecutionResultMessage) j++;
                dropped += (j - i);
                i = j;
            } else {
                dropped++;
                i++;
            }
        }

        // 步骤 2：扫描剩余消息，校验每个含 toolCalls 的 AiMessage 后续 ToolResult 配对完整
        while (i < input.size()) {
            ChatMessage m = input.get(i);
            if (m instanceof AiMessage am && am.hasToolExecutionRequests()) {
                List<ToolExecutionRequest> reqs = am.toolExecutionRequests();
                int j = i + 1;
                List<ToolExecutionResultMessage> matched = new ArrayList<>();
                while (j < input.size() && input.get(j) instanceof ToolExecutionResultMessage tr) {
                    matched.add(tr);
                    j++;
                }
                // 按数量判完整性：MCP/DB 往返中 toolResult.id 不一定与 toolCall.id 严格相同，
                // 但只要数量对齐就是正常轮次。OpenAI 端会自行二次校验 id。
                if (matched.size() >= reqs.size()) {
                    out.add(normalizeAiMessage(am));
                    out.addAll(matched);
                } else {
                    dropped += 1 + matched.size();
                }
                i = j;
            } else if (m instanceof ToolExecutionResultMessage) {
                dropped++;
                i++;
            } else if (m instanceof AiMessage am) {
                out.add(normalizeAiMessage(am));
                i++;
            } else {
                out.add(m);
                i++;
            }
        }
        if (dropped > 0) {
            log.info("[MessageSanitizer] 清洗剔除 {} 条不合法/破损消息（含头部 ai/tool、未配对轮次）", dropped);
        }
        return out;
    }

    /**
     * 修正 AiMessage：text 为 null 时强制为空串。
     * 部分 langchain4j-openai 版本会把 null content 序列化成 {} 对象，
     * 触发 OpenAI 兼容端 "got an object" 400 错误。
     */
    private static AiMessage normalizeAiMessage(AiMessage am) {
        if (am.text() != null) return am;
        if (am.hasToolExecutionRequests()) {
            return AiMessage.from("", am.toolExecutionRequests());
        }
        return AiMessage.from("");
    }
}
