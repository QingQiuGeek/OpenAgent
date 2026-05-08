package com.qingqiu.openagent.model.request;

import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/5 19:07
 * @description: UpdateKnowledgeBase request payload
 */

@Data
public class UpdateKnowledgeBaseRequest {
    private String name;
    private String description;
}

