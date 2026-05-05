package com.qingqiu.openagent.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.qingqiu.openagent.converter.ChatMessageConverter;
import com.qingqiu.openagent.enums.BizExceptionEnum;
import com.qingqiu.openagent.event.ChatEvent;
import com.qingqiu.openagent.exception.BizException;
import com.qingqiu.openagent.mapper.ChatMessageMapper;
import com.qingqiu.openagent.mapper.ChatSessionMapper;
import com.qingqiu.openagent.model.dto.ChatMessageDTO;
import com.qingqiu.openagent.model.entity.ChatMessage;
import com.qingqiu.openagent.model.entity.ChatSession;
import com.qingqiu.openagent.model.request.CreateChatMessageRequest;
import com.qingqiu.openagent.model.request.UpdateChatMessageRequest;
import com.qingqiu.openagent.model.response.CreateChatMessageResponse;
import com.qingqiu.openagent.model.response.GetChatMessagesResponse;
import com.qingqiu.openagent.model.vo.ChatMessageVO;
import com.qingqiu.openagent.service.ChatMessageFacadeService;
import com.qingqiu.openagent.util.UserContext;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class ChatMessageFacadeServiceImpl implements ChatMessageFacadeService {

    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageConverter chatMessageConverter;
    private final ApplicationEventPublisher publisher;

    @Override
    public GetChatMessagesResponse getChatMessagesBySessionId(String sessionId) {
        // 外部入口：校验 session 属于当前登录用户
        requireOwnedSession(sessionId);
        List<ChatMessage> chatMessages = chatMessageMapper.selectBySessionId(sessionId);
        List<ChatMessageVO> result = new ArrayList<>();

        for (ChatMessage chatMessage : chatMessages) {
            try {
                ChatMessageVO vo = chatMessageConverter.toVO(chatMessage);
                result.add(vo);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        return GetChatMessagesResponse.builder()
                .chatMessages(result.toArray(new ChatMessageVO[0]))
                .build();
    }

    @Override
    public List<ChatMessageDTO> getChatMessagesBySessionIdRecently(String sessionId, int limit) {
        List<ChatMessage> chatMessages = chatMessageMapper.selectBySessionIdRecently(sessionId, limit);
        List<ChatMessageDTO> result = new ArrayList<>();
        for (ChatMessage chatMessage : chatMessages) {
            try {
                ChatMessageDTO dto = chatMessageConverter.toDTO(chatMessage);
                result.add(dto);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    @Override
    public CreateChatMessageResponse createChatMessage(CreateChatMessageRequest request) {
        // 外部入口：校验 session 属于当前登录用户（agent 内部直接调用 doCreateChatMessage 或 agentCreateChatMessage，无需走这里）
        if (request == null || request.getSessionId() == null || request.getSessionId().isBlank()) {
            throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(), "sessionId 不能为空");
        }
        if (request.getAgentId() == null || request.getAgentId().isBlank()) {
            throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(), "agentId 不能为空");
        }
        requireOwnedSession(request.getSessionId());
        ChatMessage chatMessage = doCreateChatMessage(request);
        // 发布聊天通知事件
        publisher.publishEvent(new ChatEvent(
                        request.getAgentId(),
                        chatMessage.getSessionId(),
                        chatMessage.getContent()
                )
        );
        // 返回生成的 chatMessageId
        return CreateChatMessageResponse.builder()
                .chatMessageId(chatMessage.getId())
                .build();
    }

    @Override
    public CreateChatMessageResponse createChatMessage(ChatMessageDTO chatMessageDTO) {
        ChatMessage chatMessage = doCreateChatMessage(chatMessageDTO);
        return CreateChatMessageResponse.builder()
                .chatMessageId(chatMessage.getId())
                .build();
    }

    @Override
    public CreateChatMessageResponse agentCreateChatMessage(CreateChatMessageRequest request) {
        ChatMessage chatMessage = doCreateChatMessage(request);
        // 和 createChatMessage 的区别在于，Agent 创建的 chatMessage 不需要发布事件
        return CreateChatMessageResponse.builder()
                .chatMessageId(chatMessage.getId())
                .build();
    }

    private ChatMessage doCreateChatMessage(CreateChatMessageRequest request) {
        // 将 CreateChatMessageRequest 转换为 ChatMessageDTO
        ChatMessageDTO chatMessageDTO = chatMessageConverter.toDTO(request);
        // 将 ChatMessageDTO 转换为 ChatMessage 实体
        return doCreateChatMessage(chatMessageDTO);
    }

    private ChatMessage doCreateChatMessage(ChatMessageDTO chatMessageDTO) {
        try {
            // 将 ChatMessageDTO 转换为 ChatMessage 实体
            ChatMessage chatMessage = chatMessageConverter.toEntity(chatMessageDTO);

            // 设置创建时间和更新时间
            LocalDateTime now = LocalDateTime.now();
            chatMessage.setCreatedAt(now);
            chatMessage.setUpdatedAt(now);
            // 插入数据库，ID 由数据库自动生成
            int result = chatMessageMapper.insert(chatMessage);
            if (result <= 0) {
                throw new BizException("创建聊天消息失败");
            }
            return chatMessage;
        } catch (JsonProcessingException e) {
            throw new BizException("创建聊天消息时发生序列化错误: " + e.getMessage());
        }
    }

    @Override
    public CreateChatMessageResponse appendChatMessage(String chatMessageId, String appendContent) {
        // 查询现有的聊天消息
        ChatMessage existingChatMessage = chatMessageMapper.selectById(chatMessageId);
        if (existingChatMessage == null) {
            throw new BizException("聊天消息不存在: " + chatMessageId);
        }

        // 将追加内容添加到现有内容后面
        String currentContent = existingChatMessage.getContent() != null
                ? existingChatMessage.getContent()
                : "";
        String updatedContent = currentContent + appendContent;

        // 创建更新后的消息对象
        ChatMessage updatedChatMessage = ChatMessage.builder()
                .id(existingChatMessage.getId())
                .sessionId(existingChatMessage.getSessionId())
                .role(existingChatMessage.getRole())
                .content(updatedContent)
                .metadata(existingChatMessage.getMetadata())
                .createdAt(existingChatMessage.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        // 更新数据库
        int result = chatMessageMapper.updateById(updatedChatMessage);
        if (result <= 0) {
            throw new BizException("追加聊天消息内容失败");
        }

        // 返回聊天消息ID
        return CreateChatMessageResponse.builder()
                .chatMessageId(chatMessageId)
                .build();
    }

    @Override
    public void deleteChatMessage(String chatMessageId) {
        ChatMessage chatMessage = chatMessageMapper.selectById(chatMessageId);
        if (chatMessage == null) {
            throw new BizException(BizExceptionEnum.NOT_FOUND_ERROR.getCode(), "聊天消息不存在: " + chatMessageId);
        }
        requireOwnedSession(chatMessage.getSessionId());

        int result = chatMessageMapper.deleteById(chatMessageId);
        if (result <= 0) {
            throw new BizException(BizExceptionEnum.OPERATION_ERROR.getCode(), "删除聊天消息失败");
        }
    }

    @Override
    public void updateChatMessage(String chatMessageId, UpdateChatMessageRequest request) {
        try {
            ChatMessage existingChatMessage = chatMessageMapper.selectById(chatMessageId);
            if (existingChatMessage == null) {
                throw new BizException(BizExceptionEnum.NOT_FOUND_ERROR.getCode(), "聊天消息不存在: " + chatMessageId);
            }
            requireOwnedSession(existingChatMessage.getSessionId());

            // 将现有 ChatMessage 转换为 ChatMessageDTO
            ChatMessageDTO chatMessageDTO = chatMessageConverter.toDTO(existingChatMessage);

            // 使用 UpdateChatMessageRequest 更新 ChatMessageDTO
            chatMessageConverter.updateDTOFromRequest(chatMessageDTO, request);

            // 将更新后的 ChatMessageDTO 转换回 ChatMessage 实体
            ChatMessage updatedChatMessage = chatMessageConverter.toEntity(chatMessageDTO);

            // 保留原有的 ID、sessionId、role 和创建时间
            updatedChatMessage.setId(existingChatMessage.getId());
            updatedChatMessage.setSessionId(existingChatMessage.getSessionId());
            updatedChatMessage.setRole(existingChatMessage.getRole());
            updatedChatMessage.setCreatedAt(existingChatMessage.getCreatedAt());
            updatedChatMessage.setUpdatedAt(LocalDateTime.now());

            // 更新数据库
            int result = chatMessageMapper.updateById(updatedChatMessage);
            if (result <= 0) {
                throw new BizException("更新聊天消息失败");
            }
        } catch (JsonProcessingException e) {
            throw new BizException("更新聊天消息时发生序列化错误: " + e.getMessage());
        }
    }

    /**
     * 校验 sessionId 对应的 ChatSession 属于当前登录用户。Agent 内部流程（agentCreateChatMessage /
     * doCreateChatMessage / createChatMessage(ChatMessageDTO)）不会走这里。
     */
    private void requireOwnedSession(String sessionId) {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BizException(BizExceptionEnum.NOT_LOGIN_ERROR.getCode(),
                    BizExceptionEnum.NOT_LOGIN_ERROR.getMessage());
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(), "sessionId 不能为空");
        }
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BizException(BizExceptionEnum.NOT_FOUND_ERROR.getCode(),
                    "聊天会话不存在: " + sessionId);
        }
        if (!userId.equals(session.getUserId())) {
            throw new BizException(BizExceptionEnum.FORBIDDEN_ERROR.getCode(),
                    BizExceptionEnum.FORBIDDEN_ERROR.getMessage());
        }
    }
}

