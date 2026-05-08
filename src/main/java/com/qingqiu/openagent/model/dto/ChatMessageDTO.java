package com.qingqiu.openagent.model.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: qingqiugeek
 * @date: 2026/5/2 21:04
 * @description: ChatMessage DTO
 */

@Data
@Builder
public class ChatMessageDTO {
    private String id;

    private String sessionId;

    private RoleType role;

    private String content;

    private MetaData metadata;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class MetaData {
        private ToolResponse toolResponse;
        private List<ToolCall> toolCalls;
        /** 来源引用：assistant 终态消息会附带 web 搜索 / 知识库召回结果 */
        private List<Source> sources;
        /** 用户消息附件：上传到 OSS 后的元数据，加载历史时直接用于渲染文件卡片 */
        private List<Attachment> attachments;
    }

    @Data
    @Builder
    public static class Attachment {
        /** OSS 公网访问 URL */
        private String url;
        /** 原始文件名 */
        private String name;
        /** 字节数 */
        private Long size;
        /** MIME 类型，如 image/png */
        private String contentType;
    }

    @Data
    @Builder
    public static class Source {
        /** "web" 或 "kb" */
        private String type;
        /** 标题：网页标题 / 文件名 / 分块标题 */
        private String title;
        /** url：网页链接（kb 类型可空） */
        private String url;
        /** 内容摘要 / 分块文本 */
        private String content;
        /** 网络搜索得分 */
        private Double score;
        /** 知识库名（kb 类型用） */
        private String kbName;
    }

    @Data
    @Builder
    public static class ToolCall {
        private String id;
        private String name;
        private String arguments;
    }

    @Data
    @Builder
    public static class ToolResponse {
        private String id;
        private String name;
        private String responseData;
    }

    @Getter
    @AllArgsConstructor
    public enum RoleType {
        USER("user"),
        ASSISTANT("assistant"),
        SYSTEM("system"),
        TOOL("tool");

        @JsonValue
        private final String role;

        public static RoleType fromRole(String role) {
            for (RoleType value : values()) {
                if (value.role.equals(role)) {
                    return value;
                }
            }
            throw new IllegalArgumentException("Invalid role: " + role);
        }
    }
}
