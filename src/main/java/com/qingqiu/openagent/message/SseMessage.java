package com.qingqiu.openagent.message;

import com.qingqiu.openagent.model.vo.ChatMessageVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/11 09:58
 * @description: Sse message
 */

@Data
@AllArgsConstructor
@Builder
public class SseMessage {

    private Type type;
    private Payload payload;
    private Metadata metadata;

    @Data
    @AllArgsConstructor
    @Builder
    public static class Payload {
        private ChatMessageVO message;
        private String statusText;
        private Boolean done;
        // 流式输出的内容增量
        private String delta;
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class Metadata {
        private String chatMessageId;
    }

    // 自定义消息类型
    // 1. AI 生成
    // 2. AI 规划中
    // 3. AI 思考中
    // 4. AI 执行中
    // 5. AI 完成
    public enum Type {
        AI_GENERATED_CONTENT,
        AI_MESSAGE_CHUNK,
        AI_PLANNING,
        AI_THINKING,
        AI_EXECUTING,
        AI_DONE,
        /** Agent 运行时异常：把 errorMsg 放入 payload.delta 推给前端，前端可显示错误气泡。 */
        AI_ERROR,
    }
}
