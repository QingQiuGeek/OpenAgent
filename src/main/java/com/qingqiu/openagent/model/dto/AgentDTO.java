package com.qingqiu.openagent.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: qingqiugeek
 * @date: 2026/5/1 21:14
 * @description: Agent DTO
 */

@Data
@Builder
public class AgentDTO {
    private String id;

    private Long userId;

    private String name;

    private String description;

    private String systemPrompt;

    /** 关联 {@code model.id}，由用户在 /api/models 下自己维护。 */
    private Long modelId;

    private List<String> allowedTools;

    private List<String> allowedKbs;

    private ChatOptions chatOptions;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Data
    @AllArgsConstructor
    @Builder
    public static class ChatOptions {
        private Double temperature;
        private Double topP;
        private Integer messageLength; // 聊天消息窗口长度

        private static final Double DEFAULT_TEMPERATURE = 0.7;
        private static final Double DEFAULT_TOP_P = 1.0;
        private static final Integer DEFAULT_MESSAGE_LENGTH = 10;

        public static ChatOptions defaultOptions() {
            return ChatOptions.builder()
                    .temperature(DEFAULT_TEMPERATURE)
                    .topP(DEFAULT_TOP_P)
                    .messageLength(DEFAULT_MESSAGE_LENGTH)
                    .build();
        }
    }
}
