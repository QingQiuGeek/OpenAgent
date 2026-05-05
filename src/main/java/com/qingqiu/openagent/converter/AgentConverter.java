package com.qingqiu.openagent.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingqiu.openagent.mapper.ModelMapper;
import com.qingqiu.openagent.model.dto.AgentDTO;
import com.qingqiu.openagent.model.entity.Agent;
import com.qingqiu.openagent.model.entity.Model;
import com.qingqiu.openagent.model.request.CreateAgentRequest;
import com.qingqiu.openagent.model.request.UpdateAgentRequest;
import com.qingqiu.openagent.model.vo.AgentVO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@AllArgsConstructor
public class AgentConverter {

    private final ObjectMapper objectMapper;
    private final ModelMapper modelMapper;

    public Agent toEntity(AgentDTO agentDTO) throws JsonProcessingException {
        Assert.notNull(agentDTO, "AgentDTO cannot be null");
        Assert.notNull(agentDTO.getAllowedTools(), "Allowed tools cannot be null");
        Assert.notNull(agentDTO.getAllowedKbs(), "Allowed kbs cannot be null");
        Assert.notNull(agentDTO.getChatOptions(), "Chat options cannot be null");

        return Agent.builder()
                .id(agentDTO.getId())
                .userId(agentDTO.getUserId())
                .name(agentDTO.getName())
                .description(agentDTO.getDescription())
                .systemPrompt(agentDTO.getSystemPrompt())
                .modelId(agentDTO.getModelId())
                .allowedTools(objectMapper.writeValueAsString(agentDTO.getAllowedTools()))
                .allowedKbs(objectMapper.writeValueAsString(agentDTO.getAllowedKbs()))
                .chatOptions(objectMapper.writeValueAsString(agentDTO.getChatOptions()))
                .createdAt(agentDTO.getCreatedAt())
                .updatedAt(agentDTO.getUpdatedAt())
                .build();
    }

    public AgentDTO toDTO(Agent agent) throws JsonProcessingException {
        Assert.notNull(agent, "Agent cannot be null");
        Assert.notNull(agent.getAllowedTools(), "Allowed tools cannot be null");
        Assert.notNull(agent.getAllowedKbs(), "Allowed kbs cannot be null");
        Assert.notNull(agent.getChatOptions(), "Chat options cannot be null");

        return AgentDTO.builder()
                .id(agent.getId())
                .userId(agent.getUserId())
                .name(agent.getName())
                .description(agent.getDescription())
                .systemPrompt(agent.getSystemPrompt())
                .modelId(agent.getModelId())
                .allowedTools(objectMapper.readValue(agent.getAllowedTools(), new TypeReference<>(){}))
                .allowedKbs(objectMapper.readValue(agent.getAllowedKbs(), new TypeReference<>(){}))
                .chatOptions(objectMapper.readValue(agent.getChatOptions(), AgentDTO.ChatOptions.class))
                .createdAt(agent.getCreatedAt())
                .updatedAt(agent.getUpdatedAt())
                .build();
    }

    public AgentVO toVO(AgentDTO dto) {
        String modelName = null;
        if (dto.getModelId() != null) {
            Model model = modelMapper.selectById(dto.getModelId());
            if (model != null) {
                modelName = model.getModelName();
            }
        }
        return AgentVO.builder()
                .id(dto.getId())
                .userId(dto.getUserId())
                .name(dto.getName())
                .description(dto.getDescription())
                .systemPrompt(dto.getSystemPrompt())
                .modelId(dto.getModelId())
                .modelName(modelName)
                .allowedTools(dto.getAllowedTools())
                .allowedKbs(dto.getAllowedKbs())
                .chatOptions(dto.getChatOptions())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }

    public AgentVO toVO(Agent agent) throws JsonProcessingException {
        return toVO(toDTO(agent));
    }

    public AgentDTO toDTO(CreateAgentRequest request) {
        Assert.notNull(request, "CreateAgentRequest cannot be null");
        return AgentDTO.builder()
                .name(request.getName())
                .description(request.getDescription())
                .systemPrompt(request.getSystemPrompt())
                .modelId(request.getModelId())
                .allowedTools(request.getAllowedTools() == null
                        ? java.util.Collections.emptyList() : request.getAllowedTools())
                .allowedKbs(request.getAllowedKbs() == null
                        ? java.util.Collections.emptyList() : request.getAllowedKbs())
                .chatOptions(request.getChatOptions() == null
                        ? AgentDTO.ChatOptions.defaultOptions() : request.getChatOptions())
                .build();
    }

    public void updateDTOFromRequest(AgentDTO dto, UpdateAgentRequest request) {
        Assert.notNull(dto, "AgentDTO cannot be null");
        Assert.notNull(request, "UpdateAgentRequest cannot be null");

        if (request.getName() != null) {
            dto.setName(request.getName());
        }
        if (request.getDescription() != null) {
            dto.setDescription(request.getDescription());
        }
        if (request.getSystemPrompt() != null) {
            dto.setSystemPrompt(request.getSystemPrompt());
        }
        if (request.getModelId() != null) {
            dto.setModelId(request.getModelId());
        }
        if (request.getAllowedTools() != null) {
            dto.setAllowedTools(request.getAllowedTools());
        }
        if (request.getAllowedKbs() != null) {
            dto.setAllowedKbs(request.getAllowedKbs());
        }
        if (request.getChatOptions() != null) {
            dto.setChatOptions(request.getChatOptions());
        }
    }
}
