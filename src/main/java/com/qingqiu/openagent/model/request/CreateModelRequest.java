package com.qingqiu.openagent.model.request;

import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/11 09:47
 * @description: CreateModel request payload
 */

@Data
public class CreateModelRequest {

    private String modelName;

    private String providerType;

    private String baseUrl;

    private String apiKey;

    private Integer maxTokens;
}
