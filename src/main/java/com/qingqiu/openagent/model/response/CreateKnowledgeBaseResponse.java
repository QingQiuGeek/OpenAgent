package com.qingqiu.openagent.model.response;

import lombok.Builder;
import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/11 20:45
 * @description: CreateKnowledgeBase response payload
 */

@Data
@Builder
public class CreateKnowledgeBaseResponse {
    private String knowledgeBaseId;
}

