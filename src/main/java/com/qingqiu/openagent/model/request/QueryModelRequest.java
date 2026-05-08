package com.qingqiu.openagent.model.request;

import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 19:34
 * @description: QueryModel request payload
 */

@Data
public class QueryModelRequest {

    private String modelName;

    private String providerType;
}
