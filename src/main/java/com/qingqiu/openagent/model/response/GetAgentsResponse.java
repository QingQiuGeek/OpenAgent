package com.qingqiu.openagent.model.response;

import com.qingqiu.openagent.model.vo.AgentVO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetAgentsResponse {
    private AgentVO[] agents;
}
