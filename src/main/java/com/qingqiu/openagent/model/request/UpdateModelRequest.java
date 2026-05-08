package com.qingqiu.openagent.model.request;

import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 11:55
 * @description: UpdateModel request payload
 */

@Data
public class UpdateModelRequest {

    private String modelName;

    private String providerType;

    private String baseUrl;

    private String apiKey;

    private Integer maxTokens;
}
