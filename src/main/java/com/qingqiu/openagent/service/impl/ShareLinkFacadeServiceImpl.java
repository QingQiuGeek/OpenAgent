package com.qingqiu.openagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.qingqiu.openagent.enums.BizExceptionEnum;
import com.qingqiu.openagent.exception.BizException;
import com.qingqiu.openagent.mapper.AgentMapper;
import com.qingqiu.openagent.mapper.ChatMessageMapper;
import com.qingqiu.openagent.mapper.ChatSessionMapper;
import com.qingqiu.openagent.mapper.ShareLinkMapper;
import com.qingqiu.openagent.model.entity.Agent;
import com.qingqiu.openagent.model.entity.ChatMessage;
import com.qingqiu.openagent.model.entity.ChatSession;
import com.qingqiu.openagent.model.entity.ShareLink;
import com.qingqiu.openagent.model.request.CreateShareLinkRequest;
import com.qingqiu.openagent.model.vo.ShareLinkVO;
import com.qingqiu.openagent.model.vo.ShareSnapshotVO;
import com.qingqiu.openagent.service.ShareLinkFacadeService;
import com.qingqiu.openagent.util.UserContext;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 14:10
 * @description: ShareLink facade service implementation
 */
@Slf4j
@Service
@AllArgsConstructor
public class ShareLinkFacadeServiceImpl implements ShareLinkFacadeService {

    private static final String SLUG_ALPHABET = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ShareLinkMapper shareLinkMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final AgentMapper agentMapper;
    private final ObjectMapper objectMapper;

    @Override
    public ShareLinkVO create(CreateShareLinkRequest request) {
        Long userId = requireLoginUser();
        if (request == null || request.getSessionId() == null || request.getSessionId().isBlank()) {
            throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(), "sessionId 不能为空");
        }
        ChatSession session = chatSessionMapper.selectById(request.getSessionId());
        if (session == null) {
            throw new BizException(BizExceptionEnum.NOT_FOUND_ERROR.getCode(), "会话不存在");
        }
        if (!userId.equals(session.getUserId())) {
            throw new BizException(BizExceptionEnum.FORBIDDEN_ERROR.getCode(),
                    BizExceptionEnum.FORBIDDEN_ERROR.getMessage());
        }

        // 构造快照
        String snapshotJson = buildSnapshotJson(session);

        LocalDateTime expireAt = null;
        if (request.getExpireDays() != null && request.getExpireDays() > 0) {
            expireAt = LocalDateTime.now().plusDays(request.getExpireDays());
        }

        ShareLink entity = ShareLink.builder()
                .userId(userId)
                .sessionId(session.getId())
                .slug(generateUniqueSlug())
                .snapshot(snapshotJson)
                .expireAt(expireAt)
                .viewCount(0)
                .createdAt(LocalDateTime.now())
                .isDeleted(0)
                .build();
        shareLinkMapper.insert(entity);
        return toVO(entity);
    }

    @Override
    public List<ShareLinkVO> myLinks() {
        Long userId = requireLoginUser();
        List<ShareLink> list = shareLinkMapper.selectList(new LambdaQueryWrapper<ShareLink>()
                .eq(ShareLink::getUserId, userId)
                .orderByDesc(ShareLink::getCreatedAt));
        return list.stream().map(this::toVO).toList();
    }

    @Override
    public void revoke(String shareId) {
        Long userId = requireLoginUser();
        if (shareId == null || shareId.isBlank()) {
            throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(), "shareId 不能为空");
        }
        ShareLink existing = shareLinkMapper.selectById(shareId);
        if (existing == null) {
            throw new BizException(BizExceptionEnum.NOT_FOUND_ERROR.getCode(), "分享不存在");
        }
        if (!userId.equals(existing.getUserId())) {
            throw new BizException(BizExceptionEnum.FORBIDDEN_ERROR.getCode(),
                    BizExceptionEnum.FORBIDDEN_ERROR.getMessage());
        }
        shareLinkMapper.deleteById(shareId);
    }

    @Override
    public ShareSnapshotVO viewBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new BizException(BizExceptionEnum.NOT_FOUND_ERROR.getCode(), "分享不存在");
        }
        ShareLink existing = shareLinkMapper.selectOne(new LambdaQueryWrapper<ShareLink>()
                .eq(ShareLink::getSlug, slug));
        if (existing == null) {
            throw new BizException(BizExceptionEnum.NOT_FOUND_ERROR.getCode(), "分享不存在或已撤销");
        }
        if (existing.getExpireAt() != null && existing.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BizException(BizExceptionEnum.NOT_FOUND_ERROR.getCode(), "分享链接已过期");
        }
        // view_count++（不强一致；忽略并发竞争）
        try {
            existing.setViewCount((existing.getViewCount() == null ? 0 : existing.getViewCount()) + 1);
            shareLinkMapper.updateById(existing);
        } catch (Exception ignore) {
            // 忽略并发更新冲突
        }

        // 反序列化快照
        try {
            JsonNode root = objectMapper.readTree(existing.getSnapshot() == null ? "{}" : existing.getSnapshot());
            return ShareSnapshotVO.builder()
                    .slug(existing.getSlug())
                    .title(getText(root, "title"))
                    .agentName(getText(root, "agentName"))
                    .messages(root.has("messages") ? root.get("messages") : objectMapper.createArrayNode())
                    .createdAt(existing.getCreatedAt())
                    .viewCount(existing.getViewCount())
                    .build();
        } catch (JsonProcessingException e) {
            throw new BizException(BizExceptionEnum.OPERATION_ERROR.getCode(), "快照解析失败");
        }
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

    private String generateUniqueSlug() {
        for (int i = 0; i < 5; i++) {
            String candidate = randomSlug(8);
            Long count = shareLinkMapper.selectCount(new LambdaQueryWrapper<ShareLink>()
                    .eq(ShareLink::getSlug, candidate));
            if (count == null || count == 0) {
                return candidate;
            }
        }
        // 极小概率多次冲突 → 兜底用 12 位
        return randomSlug(12);
    }

    private String randomSlug(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(SLUG_ALPHABET.charAt(RANDOM.nextInt(SLUG_ALPHABET.length())));
        }
        return sb.toString();
    }

    private String buildSnapshotJson(ChatSession session) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("title", session.getTitle());
        // 取 agent 名（如果会话绑定了 agent）
        String agentName = null;
        if (session.getAgentId() != null && !session.getAgentId().isBlank()) {
            try {
                Agent agent = agentMapper.selectById(session.getAgentId());
                if (agent != null) agentName = agent.getName();
            } catch (Exception ignore) {
                // ignore
            }
        }
        root.put("agentName", agentName);

        // 加载该 session 全部未删除消息
        List<ChatMessage> messages = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, session.getId())
                .orderByAsc(ChatMessage::getCreatedAt));
        ArrayNode arr = objectMapper.createArrayNode();
        for (ChatMessage m : messages) {
            ObjectNode n = objectMapper.createObjectNode();
            n.put("id", m.getId());
            n.put("role", m.getRole());
            n.put("content", m.getContent());
            // metadata 是已经序列化好的 JSON 字符串，原样塞回为 JsonNode
            if (m.getMetadata() != null && !m.getMetadata().isBlank()) {
                try {
                    n.set("metadata", objectMapper.readTree(m.getMetadata()));
                } catch (JsonProcessingException e) {
                    n.put("metadata", m.getMetadata());
                }
            }
            n.put("createdAt", m.getCreatedAt() == null ? null : m.getCreatedAt().toString());
            arr.add(n);
        }
        root.set("messages", arr);

        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            log.warn("[ShareLink] snapshot 序列化失败: {}", e.getMessage());
            return "{}";
        }
    }

    private ShareLinkVO toVO(ShareLink entity) {
        return ShareLinkVO.builder()
                .id(entity.getId())
                .slug(entity.getSlug())
                .sessionId(entity.getSessionId())
                .expireAt(entity.getExpireAt())
                .viewCount(entity.getViewCount())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private static String getText(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) return null;
        return node.get(field).asText();
    }
}
