package com.qingqiu.openagent.model.response;

import com.qingqiu.openagent.model.vo.AgentVO;
import lombok.Builder;
import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/2 11:56
 * @description: GetAgents response payload
 */

@Data
@Builder
public class GetAgentsResponse {
    private AgentVO[] agents;
}
