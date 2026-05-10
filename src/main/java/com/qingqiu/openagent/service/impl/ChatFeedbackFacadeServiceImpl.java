package com.qingqiu.openagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingqiu.openagent.enums.BizExceptionEnum;
import com.qingqiu.openagent.exception.BizException;
import com.qingqiu.openagent.mapper.ChatFeedbackMapper;
import com.qingqiu.openagent.model.entity.ChatFeedback;
import com.qingqiu.openagent.model.request.SubmitFeedbackRequest;
import com.qingqiu.openagent.model.vo.ChatFeedbackVO;
import com.qingqiu.openagent.service.ChatFeedbackFacadeService;
import com.qingqiu.openagent.util.UserContext;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 12:08
 * @description: ChatFeedback facade service implementation
 */
@Slf4j
@Service
@AllArgsConstructor
public class ChatFeedbackFacadeServiceImpl implements ChatFeedbackFacadeService {

    private final ChatFeedbackMapper chatFeedbackMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Long submit(SubmitFeedbackRequest request) {
        Long userId = requireLoginUser();
        if (request == null || request.getMessageId() == null || request.getMessageId().isBlank()) {
            throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(), "messageId 不能为空");
        }
        if (request.getRating() == null || (request.getRating() != 1 && request.getRating() != -1)) {
            throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(), "rating 必须为 1 或 -1");
        }
        ChatFeedback existing = chatFeedbackMapper.selectOne(new LambdaQueryWrapper<ChatFeedback>()
                .eq(ChatFeedback::getUserId, userId)
                .eq(ChatFeedback::getMessageId, request.getMessageId()));
        String reasonTagsJson = serializeTags(request.getReasonTags());
        if (existing != null) {
            existing.setRating(request.getRating());
            existing.setReasonTags(reasonTagsJson);
            existing.setComment(request.getComment());
            chatFeedbackMapper.updateById(existing);
            return existing.getId();
        }
        ChatFeedback entity = ChatFeedback.builder()
                .userId(userId)
                .messageId(request.getMessageId())
                .rating(request.getRating())
                .reasonTags(reasonTagsJson)
                .comment(request.getComment())
                .createdAt(LocalDateTime.now())
                .build();
        chatFeedbackMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void withdraw(String messageId) {
        Long userId = requireLoginUser();
        chatFeedbackMapper.delete(new LambdaQueryWrapper<ChatFeedback>()
                .eq(ChatFeedback::getUserId, userId)
                .eq(ChatFeedback::getMessageId, messageId));
    }

    @Override
    public ChatFeedbackVO get(String messageId) {
        Long userId = requireLoginUser();
        ChatFeedback existing = chatFeedbackMapper.selectOne(new LambdaQueryWrapper<ChatFeedback>()
                .eq(ChatFeedback::getUserId, userId)
                .eq(ChatFeedback::getMessageId, messageId));
        return toVO(existing);
    }

    @Override
    public List<ChatFeedbackVO> batchGet(List<String> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) return Collections.emptyList();
        Long userId = requireLoginUser();
        List<ChatFeedback> list = chatFeedbackMapper.selectList(new LambdaQueryWrapper<ChatFeedback>()
                .eq(ChatFeedback::getUserId, userId)
                .in(ChatFeedback::getMessageId, messageIds));
        return list.stream().map(this::toVO).toList();
    }

    // ---- helpers ----

    private Long requireLoginUser() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BizException(BizExceptionEnum.NOT_LOGIN_ERROR.getCode(),
                    BizExceptionEnum.NOT_LOGIN_ERROR.getMessage());
        }
        return userId;
    }

    private ChatFeedbackVO toVO(ChatFeedback entity) {
        if (entity == null) return null;
        return ChatFeedbackVO.builder()
                .id(entity.getId())
                .messageId(entity.getMessageId())
                .rating(entity.getRating())
                .reasonTags(deserializeTags(entity.getReasonTags()))
                .comment(entity.getComment())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private String serializeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            log.warn("[ChatFeedback] reasonTags 序列化失败: {}", e.getMessage());
            return null;
        }
    }

    private List<String> deserializeTags(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
