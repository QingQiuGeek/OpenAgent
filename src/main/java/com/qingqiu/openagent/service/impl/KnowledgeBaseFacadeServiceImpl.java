package com.qingqiu.openagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.qingqiu.openagent.converter.KnowledgeBaseConverter;
import com.qingqiu.openagent.enums.BizExceptionEnum;
import com.qingqiu.openagent.exception.BizException;
import com.qingqiu.openagent.mapper.KnowledgeBaseMapper;
import com.qingqiu.openagent.model.dto.KnowledgeBaseDTO;
import com.qingqiu.openagent.model.entity.KnowledgeBase;
import com.qingqiu.openagent.model.request.CreateKnowledgeBaseRequest;
import com.qingqiu.openagent.model.request.UpdateKnowledgeBaseRequest;
import com.qingqiu.openagent.model.response.CreateKnowledgeBaseResponse;
import com.qingqiu.openagent.model.response.GetKnowledgeBasesResponse;
import com.qingqiu.openagent.model.vo.KnowledgeBaseVO;
import com.qingqiu.openagent.service.KnowledgeBaseFacadeService;
import com.qingqiu.openagent.util.UserContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class KnowledgeBaseFacadeServiceImpl implements KnowledgeBaseFacadeService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeBaseConverter knowledgeBaseConverter;

    @Override
    public GetKnowledgeBasesResponse getKnowledgeBases() {
        Long userId = requireLoginUser();
        LambdaQueryWrapper<KnowledgeBase> qw = new LambdaQueryWrapper<>();
        qw.eq(KnowledgeBase::getUserId, userId).orderByDesc(KnowledgeBase::getUpdatedAt);
        List<KnowledgeBase> knowledgeBases = knowledgeBaseMapper.selectList(qw);
        List<KnowledgeBaseVO> result = new ArrayList<>();
        for (KnowledgeBase knowledgeBase : knowledgeBases) {
            try {
                result.add(knowledgeBaseConverter.toVO(knowledgeBase));
            } catch (JsonProcessingException e) {
                throw new BizException("解析知识库失败: " + e.getMessage());
            }
        }
        return GetKnowledgeBasesResponse.builder()
                .knowledgeBases(result.toArray(new KnowledgeBaseVO[0]))
                .build();
    }

    @Override
    public CreateKnowledgeBaseResponse createKnowledgeBase(CreateKnowledgeBaseRequest request) {
        Long userId = requireLoginUser();
        try {
            KnowledgeBaseDTO dto = knowledgeBaseConverter.toDTO(request);
            dto.setUserId(userId);
            KnowledgeBase knowledgeBase = knowledgeBaseConverter.toEntity(dto);
            LocalDateTime now = LocalDateTime.now();
            knowledgeBase.setCreatedAt(now);
            knowledgeBase.setUpdatedAt(now);
            knowledgeBase.setIsDeleted(0);

            int rows = knowledgeBaseMapper.insert(knowledgeBase);
            if (rows <= 0) {
                throw new BizException(BizExceptionEnum.OPERATION_ERROR.getCode(), "创建知识库失败");
            }
            return CreateKnowledgeBaseResponse.builder()
                    .knowledgeBaseId(knowledgeBase.getId())
                    .build();
        } catch (JsonProcessingException e) {
            throw new BizException("创建知识库时发生序列化错误: " + e.getMessage());
        }
    }

    @Override
    public void deleteKnowledgeBase(String knowledgeBaseId) {
        KnowledgeBase existing = requireOwnedKb(knowledgeBaseId);
        int rows = knowledgeBaseMapper.deleteById(existing.getId());
        if (rows <= 0) {
            throw new BizException(BizExceptionEnum.OPERATION_ERROR.getCode(), "删除知识库失败");
        }
    }

    @Override
    public void updateKnowledgeBase(String knowledgeBaseId, UpdateKnowledgeBaseRequest request) {
        KnowledgeBase existing = requireOwnedKb(knowledgeBaseId);
        try {
            KnowledgeBaseDTO dto = knowledgeBaseConverter.toDTO(existing);
            knowledgeBaseConverter.updateDTOFromRequest(dto, request);

            KnowledgeBase updated = knowledgeBaseConverter.toEntity(dto);
            updated.setId(existing.getId());
            updated.setUserId(existing.getUserId());
            updated.setCreatedAt(existing.getCreatedAt());
            updated.setUpdatedAt(LocalDateTime.now());

            int rows = knowledgeBaseMapper.updateById(updated);
            if (rows <= 0) {
                throw new BizException(BizExceptionEnum.OPERATION_ERROR.getCode(), "更新知识库失败");
            }
        } catch (JsonProcessingException e) {
            throw new BizException("更新知识库时发生序列化错误: " + e.getMessage());
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

    private KnowledgeBase requireOwnedKb(String kbId) {
        if (kbId == null || kbId.isBlank()) {
            throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(), "kbId 不能为空");
        }
        Long userId = requireLoginUser();
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            throw new BizException(BizExceptionEnum.NOT_FOUND_ERROR.getCode(), "知识库不存在: " + kbId);
        }
        if (!userId.equals(kb.getUserId())) {
            throw new BizException(BizExceptionEnum.FORBIDDEN_ERROR.getCode(),
                    BizExceptionEnum.FORBIDDEN_ERROR.getMessage());
        }
        return kb;
    }
}
