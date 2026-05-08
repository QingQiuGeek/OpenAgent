package com.qingqiu.openagent.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author: qingqiugeek
 * @date: 2026/5/3 14:39
 * @description: KnowledgeBase view object
 */

@Data
@Builder
public class KnowledgeBaseVO {
    private String id;
    private Long userId;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

