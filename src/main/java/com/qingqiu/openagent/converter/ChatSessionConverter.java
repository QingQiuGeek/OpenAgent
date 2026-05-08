package com.qingqiu.openagent.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingqiu.openagent.model.dto.ChatSessionDTO;
import com.qingqiu.openagent.model.entity.ChatSession;
import com.qingqiu.openagent.model.request.CreateChatSessionRequest;
import com.qingqiu.openagent.model.request.UpdateChatSessionRequest;
import com.qingqiu.openagent.model.vo.ChatSessionVO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 17:24
 * @description: ChatSession converter
 */

@Component
@AllArgsConstructor
public class ChatSessionConverter {

    private final ObjectMapper objectMapper;

    public ChatSession toEntity(ChatSessionDTO chatSessionDTO) throws JsonProcessingException {
        Assert.notNull(chatSessionDTO, "ChatSessionDTO cannot be null");

        return ChatSession.builder()
                .id(chatSessionDTO.getId())
                .userId(chatSessionDTO.getUserId())
                .agentId(chatSessionDTO.getAgentId())
                .title(chatSessionDTO.getTitle())
                .metadata(chatSessionDTO.getMetadata() != null 
                        ? objectMapper.writeValueAsString(chatSessionDTO.getMetadata()) 
                        : null)
                .createdAt(chatSessionDTO.getCreatedAt())
                .updatedAt(chatSessionDTO.getUpdatedAt())
                .build();
    }

    public ChatSessionDTO toDTO(ChatSession chatSession) throws JsonProcessingException {
        Assert.notNull(chatSession, "ChatSession cannot be null");

        return ChatSessionDTO.builder()
                .id(chatSession.getId())
                .userId(chatSession.getUserId())
                .agentId(chatSession.getAgentId())
                .title(chatSession.getTitle())
                .metadata(chatSession.getMetadata() != null 
                        ? objectMapper.readValue(chatSession.getMetadata(), ChatSessionDTO.MetaData.class) 
                        : null)
                .createdAt(chatSession.getCreatedAt())
                .updatedAt(chatSession.getUpdatedAt())
                .build();
    }

    public ChatSessionVO toVO(ChatSessionDTO dto) {
        return ChatSessionVO.builder()
                .id(dto.getId())
                .userId(dto.getUserId())
                .agentId(dto.getAgentId())
                .title(dto.getTitle())
                .createdAt(dto.getCreatedAt())
                .build();
    }

    public ChatSessionVO toVO(ChatSession chatSession) throws JsonProcessingException {
        return toVO(toDTO(chatSession));
    }

    public ChatSessionDTO toDTO(CreateChatSessionRequest request) {
        Assert.notNull(request, "CreateChatSessionRequest cannot be null");
        Assert.notNull(request.getAgentId(), "AgentId cannot be null");

        return ChatSessionDTO.builder()
                .agentId(request.getAgentId())
                .title(request.getTitle())
                .build();
    }

    public void updateDTOFromRequest(ChatSessionDTO dto, UpdateChatSessionRequest request) {
        Assert.notNull(dto, "ChatSessionDTO cannot be null");
        Assert.notNull(request, "UpdateChatSessionRequest cannot be null");

        if (request.getTitle() != null) {
            dto.setTitle(request.getTitle());
        }
    }
}
