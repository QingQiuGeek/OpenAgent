package com.qingqiu.openagent.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 10:00
 * @description: Usage overview view object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageOverviewVO {

    private long totalCalls;
    private long totalTokens;
    private long totalPromptTokens;
    private long totalCompletionTokens;
    private long avgLatencyMs;
    private long errorCalls;
    /** 0~1 */
    private double errorRate;
}
