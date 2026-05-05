package com.qingqiu.openagent.service;

import com.qingqiu.openagent.model.request.CreateAgentRequest;
import com.qingqiu.openagent.model.request.UpdateAgentRequest;
import com.qingqiu.openagent.model.response.CreateAgentResponse;
import com.qingqiu.openagent.model.response.GetAgentsResponse;

public interface AgentFacadeService {
    GetAgentsResponse getAgents();

    CreateAgentResponse createAgent(CreateAgentRequest request);

    void deleteAgent(String agentId);

    void updateAgent(String agentId, UpdateAgentRequest request);
}
