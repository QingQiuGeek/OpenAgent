package com.qingqiu.openagent.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeBaseVO {
    private String id;
    private Long userId;
    private String name;
    private String description;
}

