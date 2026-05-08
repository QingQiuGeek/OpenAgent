package com.qingqiu.openagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.qingqiu.openagent.converter.AgentConverter;
import com.qingqiu.openagent.enums.BizExceptionEnum;
import com.qingqiu.openagent.exception.BizException;
import com.qingqiu.openagent.mapper.AgentMapper;
import com.qingqiu.openagent.mapper.ModelMapper;
import com.qingqiu.openagent.model.dto.AgentDTO;
import com.qingqiu.openagent.model.entity.Agent;
import com.qingqiu.openagent.model.entity.Model;
import com.qingqiu.openagent.model.request.CreateAgentRequest;
import com.qingqiu.openagent.model.request.UpdateAgentRequest;
import com.qingqiu.openagent.model.response.CreateAgentResponse;
import com.qingqiu.openagent.model.response.GetAgentsResponse;
import com.qingqiu.openagent.model.vo.AgentVO;
import com.qingqiu.openagent.service.AgentFacadeService;
import com.qingqiu.openagent.util.UserContext;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: qingqiugeek
 * @date: 2026/5/1 19:13
 * @description: AgentFacade service implementation
 */


@Service
@AllArgsConstructor
public class AgentFacadeServiceImpl implements AgentFacadeService {

    private final AgentMapper agentMapper;
    private final ModelMapper modelMapper;
    private final AgentConverter agentConverter;

    @Override
    public GetAgentsResponse getAgents() {
        Long userId = requireLoginUser();
        LambdaQueryWrapper<Agent> qw = new LambdaQueryWrapper<>();
        qw.eq(Agent::getUserId, userId);
        qw.orderByDesc(Agent::getUpdatedAt);
        List<Agent> agents = agentMapper.selectList(qw);
        List<AgentVO> result = new ArrayList<>();
        for (Agent agent : agents) {
            try {
                result.add(agentConverter.toVO(agent));
            } catch (JsonProcessingException e) {
                throw new BizException("解析 agent 失败: " + e.getMessage());
            }
        }
        return GetAgentsResponse.builder()
                .agents(result.toArray(new AgentVO[0]))
                .build();
    }

    @Override
    public CreateAgentResponse createAgent(CreateAgentRequest request) {
        Long userId = requireLoginUser();
        if (request == null || request.getModelId() == null) {
            throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(), "modelId 不能为空");
        }
        validateModelOwnership(request.getModelId(), userId);
        try {
            AgentDTO agentDTO = agentConverter.toDTO(request);
            agentDTO.setUserId(userId);

            Agent agent = agentConverter.toEntity(agentDTO);
            LocalDateTime now = LocalDateTime.now();
            agent.setCreatedAt(now);
            agent.setUpdatedAt(now);
            agent.setIsDeleted(0);

            int result = agentMapper.insert(agent);
            if (result <= 0) {
                throw new BizException(BizExceptionEnum.OPERATION_ERROR.getCode(), "创建 agent 失败");
            }
            return CreateAgentResponse.builder().agentId(agent.getId()).build();
        } catch (JsonProcessingException e) {
            throw new BizException("创建 agent 时发生序列化错误: " + e.getMessage());
        }
    }

    @Override
    public void deleteAgent(String agentId) {
        Agent existingAgent = requireOwnedAgent(agentId);
        int result = agentMapper.deleteById(existingAgent.getId());
        if (result <= 0) {
            throw new BizException(BizExceptionEnum.OPERATION_ERROR.getCode(), "删除 agent 失败");
        }
    }

    @Override
    public void updateAgent(String agentId, UpdateAgentRequest request) {
        Agent existingAgent = requireOwnedAgent(agentId);
        Long userId = existingAgent.getUserId();
        if (request.getModelId() != null && !request.getModelId().equals(existingAgent.getModelId())) {
            validateModelOwnership(request.getModelId(), userId);
        }
        try {
            AgentDTO agentDTO = agentConverter.toDTO(existingAgent);
            agentConverter.updateDTOFromRequest(agentDTO, request);

            Agent updatedAgent = agentConverter.toEntity(agentDTO);
            updatedAgent.setId(existingAgent.getId());
            updatedAgent.setUserId(existingAgent.getUserId());
            updatedAgent.setCreatedAt(existingAgent.getCreatedAt());
            updatedAgent.setUpdatedAt(LocalDateTime.now());

            int result = agentMapper.updateById(updatedAgent);
            if (result <= 0) {
                throw new BizException(BizExceptionEnum.OPERATION_ERROR.getCode(), "更新 agent 失败");
            }
        } catch (JsonProcessingException e) {
            throw new BizException("更新 agent 时发生序列化错误: " + e.getMessage());
        }
    }

    private Long requireLoginUser() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BizException(BizExceptionEnum.NOT_LOGIN_ERROR.getCode(),
                    BizExceptionEnum.NOT_LOGIN_ERROR.getMessage());
        }
        return userId;
    }

    private Agent requireOwnedAgent(String agentId) {
        if (agentId == null || agentId.isBlank()) {
            throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(), "agentId 不能为空");
        }
        Long userId = requireLoginUser();
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null) {
            throw new BizException(BizExceptionEnum.NOT_FOUND_ERROR.getCode(), "Agent 不存在: " + agentId);
        }
        if (!userId.equals(agent.getUserId())) {
            throw new BizException(BizExceptionEnum.FORBIDDEN_ERROR.getCode(),
                    BizExceptionEnum.FORBIDDEN_ERROR.getMessage());
        }
        return agent;
    }

    /**
     * 指定模型必须属于当前用户；modelId 为空时跳过（允许 agent 无默认模型）。
     */
    private void validateModelOwnership(Long modelId, Long userId) {
        if (modelId == null) {
            return;
        }
        Model model = modelMapper.selectById(modelId);
        if (model == null) {
            throw new BizException(BizExceptionEnum.NOT_FOUND_ERROR.getCode(), "模型不存在: " + modelId);
        }
        if (!userId.equals(model.getUserId())) {
            throw new BizException(BizExceptionEnum.FORBIDDEN_ERROR.getCode(),
                    "无权使用该模型: " + modelId);
        }
    }
}
