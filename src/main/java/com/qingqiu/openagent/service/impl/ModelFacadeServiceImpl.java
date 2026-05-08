package com.qingqiu.openagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingqiu.openagent.converter.ModelConverter;
import com.qingqiu.openagent.enums.BizExceptionEnum;
import com.qingqiu.openagent.exception.BizException;
import com.qingqiu.openagent.mapper.AgentMapper;
import com.qingqiu.openagent.mapper.ModelMapper;
import com.qingqiu.openagent.model.dto.ModelDTO;
import com.qingqiu.openagent.model.entity.Agent;
import com.qingqiu.openagent.model.entity.Model;
import com.qingqiu.openagent.model.request.CreateModelRequest;
import com.qingqiu.openagent.model.request.QueryModelRequest;
import com.qingqiu.openagent.model.request.UpdateModelRequest;
import com.qingqiu.openagent.model.response.CreateModelResponse;
import com.qingqiu.openagent.model.response.GetModelsResponse;
import com.qingqiu.openagent.model.vo.ModelVO;
import com.qingqiu.openagent.service.ModelFacadeService;
import com.qingqiu.openagent.util.UserContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author: qingqiugeek
 * @date: 2026/5/10 09:20
 * @description: ModelFacade service implementation
 */

@Service
@AllArgsConstructor
public class ModelFacadeServiceImpl implements ModelFacadeService {

    private final ModelMapper modelMapper;
    private final ModelConverter modelConverter;
    private final AgentMapper agentMapper;

    @Override
    public GetModelsResponse getModels(QueryModelRequest request) {
        Long userId = requireLoginUser();
        LambdaQueryWrapper<Model> qw = new LambdaQueryWrapper<>();
        qw.eq(Model::getUserId, userId);
        if (request != null) {
            if (request.getModelName() != null && !request.getModelName().isBlank()) {
                qw.like(Model::getModelName, request.getModelName());
            }
            if (request.getProviderType() != null && !request.getProviderType().isBlank()) {
                qw.eq(Model::getProviderType, request.getProviderType());
            }
        }
        qw.orderByDesc(Model::getUpdatedAt);
        List<Model> models = modelMapper.selectList(qw);
        List<ModelVO> vos = models.stream().map(modelConverter::toVO).collect(Collectors.toList());
        return GetModelsResponse.builder().models(vos).build();
    }

    @Override
    public CreateModelResponse createModel(CreateModelRequest request) {
        Long userId = requireLoginUser();
        ModelDTO dto = modelConverter.toDTO(request);
        dto.setUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        dto.setCreatedAt(now);
        dto.setUpdatedAt(now);
        Model entity = modelConverter.toEntity(dto);
        entity.setIsDeleted(0);
        int rows = modelMapper.insert(entity);
        if (rows <= 0) {
            throw new BizException(BizExceptionEnum.OPERATION_ERROR.getCode(), "创建模型失败");
        }
        return CreateModelResponse.builder().modelId(entity.getId()).build();
    }

    @Override
    public void updateModel(Long modelId, UpdateModelRequest request) {
        Model existing = requireOwnedModel(modelId);
        ModelDTO dto = modelConverter.toDTO(existing);
        modelConverter.updateDTOFromRequest(dto, request);
        Model updated = modelConverter.toEntity(dto);
        updated.setId(existing.getId());
        updated.setUserId(existing.getUserId());
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setUpdatedAt(LocalDateTime.now());
        int rows = modelMapper.updateById(updated);
        if (rows <= 0) {
            throw new BizException(BizExceptionEnum.OPERATION_ERROR.getCode(), "更新模型失败");
        }
    }

    @Override
    public void deleteModel(Long modelId) {
        Model existing = requireOwnedModel(modelId);
        // 阻断：仍有 Agent 引用该模型
        LambdaQueryWrapper<Agent> refQw = new LambdaQueryWrapper<>();
        refQw.eq(Agent::getModelId, existing.getId());
        Long refCount = agentMapper.selectCount(refQw);
        if (refCount != null && refCount > 0) {
            throw new BizException(BizExceptionEnum.OPERATION_ERROR.getCode(),
                    "该模型仍被 " + refCount + " 个 Agent 引用，无法删除");
        }
        int rows = modelMapper.deleteById(existing.getId());
        if (rows <= 0) {
            throw new BizException(BizExceptionEnum.OPERATION_ERROR.getCode(), "删除模型失败");
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

    private Model requireOwnedModel(Long modelId) {
        if (modelId == null) {
            throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(), "modelId 不能为空");
        }
        Long userId = requireLoginUser();
        Model existing = modelMapper.selectById(modelId);
        if (existing == null) {
            throw new BizException(BizExceptionEnum.NOT_FOUND_ERROR.getCode(), "模型不存在: " + modelId);
        }
        if (!userId.equals(existing.getUserId())) {
            throw new BizException(BizExceptionEnum.FORBIDDEN_ERROR.getCode(),
                    BizExceptionEnum.FORBIDDEN_ERROR.getMessage());
        }
        return existing;
    }
}
