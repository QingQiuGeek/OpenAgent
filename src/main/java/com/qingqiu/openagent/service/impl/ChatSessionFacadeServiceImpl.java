package com.qingqiu.openagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.qingqiu.openagent.converter.ChatSessionConverter;
import com.qingqiu.openagent.enums.BizExceptionEnum;
import com.qingqiu.openagent.exception.BizException;
import com.qingqiu.openagent.mapper.AgentMapper;
import com.qingqiu.openagent.mapper.ChatSessionMapper;
import com.qingqiu.openagent.model.dto.ChatSessionDTO;
import com.qingqiu.openagent.model.entity.Agent;
import com.qingqiu.openagent.model.entity.ChatSession;
import com.qingqiu.openagent.model.request.CreateChatSessionRequest;
import com.qingqiu.openagent.model.request.UpdateChatSessionRequest;
import com.qingqiu.openagent.model.response.CreateChatSessionResponse;
import com.qingqiu.openagent.model.response.GetChatSessionResponse;
import com.qingqiu.openagent.model.response.GetChatSessionsResponse;
import com.qingqiu.openagent.model.vo.ChatSessionVO;
import com.qingqiu.openagent.service.ChatSessionFacadeService;
import com.qingqiu.openagent.util.UserContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ChatSessionFacadeServiceImpl implements ChatSessionFacadeService {

    private final ChatSessionMapper chatSessionMapper;
    private final AgentMapper agentMapper;
    private final ChatSessionConverter chatSessionConverter;

    @Override
    public GetChatSessionsResponse getChatSessions() {
        Long userId = requireLoginUser();
        LambdaQueryWrapper<ChatSession> qw = new LambdaQueryWrapper<>();
        qw.eq(ChatSession::getUserId, userId).orderByDesc(ChatSession::getUpdatedAt);
        return buildResponse(chatSessionMapper.selectList(qw));
    }

    @Override
    public GetChatSessionResponse getChatSession(String chatSessionId) {
        ChatSession chatSession = requireOwnedSession(chatSessionId);
        try {
            return GetChatSessionResponse.builder()
                    .chatSession(chatSessionConverter.toVO(chatSession))
                    .build();
        } catch (JsonProcessingException e) {
            throw new BizException("解析聊天会话失败: " + e.getMessage());
        }
    }

    @Override
    public GetChatSessionsResponse getChatSessionsByAgentId(String agentId) {
        Long userId = requireLoginUser();
        // 先校验 agent 属主
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null) {
            throw new BizException(BizExceptionEnum.NOT_FOUND_ERROR.getCode(), "Agent 不存在: " + agentId);
        }
        if (!userId.equals(agent.getUserId())) {
            throw new BizException(BizExceptionEnum.FORBIDDEN_ERROR.getCode(),
                    BizExceptionEnum.FORBIDDEN_ERROR.getMessage());
        }
        LambdaQueryWrapper<ChatSession> qw = new LambdaQueryWrapper<>();
        qw.eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getAgentId, agentId)
                .orderByDesc(ChatSession::getUpdatedAt);
        return buildResponse(chatSessionMapper.selectList(qw));
    }

    @Override
    public CreateChatSessionResponse createChatSession(CreateChatSessionRequest request) {
        Long userId = requireLoginUser();
        if (request == null || request.getAgentId() == null) {
            throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(), "agentId 不能为空");
        }
        // agent 必须属于当前用户
        Agent agent = agentMapper.selectById(request.getAgentId());
        if (agent == null) {
            throw new BizException(BizExceptionEnum.NOT_FOUND_ERROR.getCode(),
                    "Agent 不存在: " + request.getAgentId());
        }
        if (!userId.equals(agent.getUserId())) {
            throw new BizException(BizExceptionEnum.FORBIDDEN_ERROR.getCode(),
                    BizExceptionEnum.FORBIDDEN_ERROR.getMessage());
        }
        try {
            ChatSessionDTO dto = chatSessionConverter.toDTO(request);
            dto.setUserId(userId);
            ChatSession chatSession = chatSessionConverter.toEntity(dto);
            LocalDateTime now = LocalDateTime.now();
            chatSession.setCreatedAt(now);
            chatSession.setUpdatedAt(now);
            chatSession.setIsDeleted(0);

            int rows = chatSessionMapper.insert(chatSession);
            if (rows <= 0) {
                throw new BizException(BizExceptionEnum.OPERATION_ERROR.getCode(), "创建聊天会话失败");
            }
            return CreateChatSessionResponse.builder()
                    .chatSessionId(chatSession.getId())
                    .build();
        } catch (JsonProcessingException e) {
            throw new BizException("创建聊天会话时发生序列化错误: " + e.getMessage());
        }
    }

    @Override
    public void deleteChatSession(String chatSessionId) {
        ChatSession existing = requireOwnedSession(chatSessionId);
        int rows = chatSessionMapper.deleteById(existing.getId());
        if (rows <= 0) {
            throw new BizException(BizExceptionEnum.OPERATION_ERROR.getCode(), "删除聊天会话失败");
        }
    }

    @Override
    public void updateChatSession(String chatSessionId, UpdateChatSessionRequest request) {
        ChatSession existing = requireOwnedSession(chatSessionId);
        try {
            ChatSessionDTO dto = chatSessionConverter.toDTO(existing);
            chatSessionConverter.updateDTOFromRequest(dto, request);

            ChatSession updated = chatSessionConverter.toEntity(dto);
            updated.setId(existing.getId());
            updated.setUserId(existing.getUserId());
            updated.setAgentId(existing.getAgentId());
            updated.setCreatedAt(existing.getCreatedAt());
            updated.setUpdatedAt(LocalDateTime.now());

            int rows = chatSessionMapper.updateById(updated);
            if (rows <= 0) {
                throw new BizException(BizExceptionEnum.OPERATION_ERROR.getCode(), "更新聊天会话失败");
            }
        } catch (JsonProcessingException e) {
            throw new BizException("更新聊天会话时发生序列化错误: " + e.getMessage());
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

    private ChatSession requireOwnedSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(), "sessionId 不能为空");
        }
        Long userId = requireLoginUser();
        ChatSession chatSession = chatSessionMapper.selectById(sessionId);
        if (chatSession == null) {
            throw new BizException(BizExceptionEnum.NOT_FOUND_ERROR.getCode(),
                    "聊天会话不存在: " + sessionId);
        }
        if (!userId.equals(chatSession.getUserId())) {
            throw new BizException(BizExceptionEnum.FORBIDDEN_ERROR.getCode(),
                    BizExceptionEnum.FORBIDDEN_ERROR.getMessage());
        }
        return chatSession;
    }

    private GetChatSessionsResponse buildResponse(List<ChatSession> chatSessions) {
        List<ChatSessionVO> result = new ArrayList<>();
        for (ChatSession chatSession : chatSessions) {
            try {
                result.add(chatSessionConverter.toVO(chatSession));
            } catch (JsonProcessingException e) {
                throw new BizException("解析聊天会话失败: " + e.getMessage());
            }
        }
        return GetChatSessionsResponse.builder()
                .chatSessions(result.toArray(new ChatSessionVO[0]))
                .build();
    }
}
