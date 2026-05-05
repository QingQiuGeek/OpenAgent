package com.qingqiu.openagent.controller;

import com.qingqiu.openagent.model.common.R;
import com.qingqiu.openagent.model.request.CreateAgentRequest;
import com.qingqiu.openagent.model.request.UpdateAgentRequest;
import com.qingqiu.openagent.model.response.CreateAgentResponse;
import com.qingqiu.openagent.model.response.GetAgentsResponse;
import com.qingqiu.openagent.service.AgentFacadeService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agents")
@AllArgsConstructor
public class AgentController {

    private final AgentFacadeService agentFacadeService;

    // 查询 agents
    @GetMapping
    public R<GetAgentsResponse> getAgents() {
        return R.success(agentFacadeService.getAgents());
    }

    // 创建 agent
    @PostMapping
    public R<CreateAgentResponse> createAgent(@RequestBody CreateAgentRequest request) {
        return R.success(agentFacadeService.createAgent(request));
    }

    // 删除 agent
    @DeleteMapping("/{agentId}")
    public R<Boolean> deleteAgent(@PathVariable String agentId) {
        agentFacadeService.deleteAgent(agentId);
        return R.success(true);
    }

    // 更新 agent
    @PatchMapping("/{agentId}")
    public R<Boolean> updateAgent(@PathVariable String agentId, @RequestBody UpdateAgentRequest request) {
        agentFacadeService.updateAgent(agentId, request);
        return R.success(true);
    }
}
