package com.qingqiu.openagent.model.request;

import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/2 09:52
 * @description: CreateKnowledgeBase request payload
 */

@Data
public class CreateKnowledgeBaseRequest {
    private String name;
    private String description;
}

