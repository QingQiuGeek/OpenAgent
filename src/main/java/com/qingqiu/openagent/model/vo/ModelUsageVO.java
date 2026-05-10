package com.qingqiu.openagent.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 10:01
 * @description: Per-model usage aggregation view object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelUsageVO {

    private Long modelId;
    private String modelName;
    private long calls;
    private long totalTokens;
    private long avgLatencyMs;
    private long errorCalls;
}
