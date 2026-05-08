package com.qingqiu.openagent.service;

import com.qingqiu.openagent.model.request.CreateAgentRequest;
import com.qingqiu.openagent.model.request.UpdateAgentRequest;
import com.qingqiu.openagent.model.response.CreateAgentResponse;
import com.qingqiu.openagent.model.response.GetAgentsResponse;

/**
 * @author: qingqiugeek
 * @date: 2026/5/3 18:05
 * @description: AgentFacade service
 */

public interface AgentFacadeService {
    GetAgentsResponse getAgents();

    CreateAgentResponse createAgent(CreateAgentRequest request);

    void deleteAgent(String agentId);

    void updateAgent(String agentId, UpdateAgentRequest request);
}
