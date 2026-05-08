package com.qingqiu.openagent.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author: qingqiugeek
 * @date: 2026/5/10 12:13
 * @description: KnowledgeBase DTO
 */

@Data
@Builder
public class KnowledgeBaseDTO {
    private String id;

    private Long userId;

    private String name;

    private String description;

    private MetaData metadata;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Data
    public static class MetaData {
        private String version;
    }

    @Override
    public String toString() {
        return "{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
