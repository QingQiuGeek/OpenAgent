package com.qingqiu.openagent.model.request;

import lombok.Data;

@Data
public class UpdateModelRequest {

    private String modelName;

    private String providerType;

    private String baseUrl;

    private String apiKey;

    private Integer maxTokens;
}
