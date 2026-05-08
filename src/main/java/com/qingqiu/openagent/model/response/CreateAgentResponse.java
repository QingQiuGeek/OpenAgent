package com.qingqiu.openagent.model.response;

import lombok.Builder;
import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/11 18:07
 * @description: CreateAgent response payload
 */

@Data
@Builder
public class CreateAgentResponse {
    private String agentId;
}
