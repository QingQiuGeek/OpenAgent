package com.qingqiu.openagent.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: qingqiugeek
 * @date: 2026/5/3 20:35
 * @description: CreateMcpServer response payload
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMcpServerResponse {
    private Long mcpServerId;
}
