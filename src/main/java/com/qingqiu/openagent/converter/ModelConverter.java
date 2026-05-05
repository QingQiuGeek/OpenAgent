package com.qingqiu.openagent.converter;

import com.qingqiu.openagent.model.dto.ModelDTO;
import com.qingqiu.openagent.model.entity.Model;
import com.qingqiu.openagent.model.request.CreateModelRequest;
import com.qingqiu.openagent.model.request.UpdateModelRequest;
import com.qingqiu.openagent.model.vo.ModelVO;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * {@link Model} 实体 ↔ DTO/VO/Request 的转换器。遵循项目 Converter 规范。
 */
@Component
public class ModelConverter {

    public Model toEntity(ModelDTO dto) {
        Assert.notNull(dto, "ModelDTO cannot be null");
        return Model.builder()
                .id(dto.getId())
                .userId(dto.getUserId())
                .modelName(dto.getModelName())
                .providerType(dto.getProviderType())
                .baseUrl(dto.getBaseUrl())
                .apiKey(dto.getApiKey())
                .maxTokens(dto.getMaxTokens())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }

    public ModelDTO toDTO(Model entity) {
        Assert.notNull(entity, "Model entity cannot be null");
        return ModelDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .modelName(entity.getModelName())
                .providerType(entity.getProviderType())
                .baseUrl(entity.getBaseUrl())
                .apiKey(entity.getApiKey())
                .maxTokens(entity.getMaxTokens())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ModelDTO toDTO(CreateModelRequest request) {
        Assert.notNull(request, "CreateModelRequest cannot be null");
        Assert.hasText(request.getModelName(), "modelName is required");
        Assert.hasText(request.getBaseUrl(), "baseUrl is required");
        Assert.hasText(request.getApiKey(), "apiKey is required");
        return ModelDTO.builder()
                .modelName(request.getModelName())
                .providerType(request.getProviderType())
                .baseUrl(request.getBaseUrl())
                .apiKey(request.getApiKey())
                .maxTokens(request.getMaxTokens())
                .build();
    }

    public void updateDTOFromRequest(ModelDTO dto, UpdateModelRequest request) {
        Assert.notNull(dto, "ModelDTO cannot be null");
        Assert.notNull(request, "UpdateModelRequest cannot be null");
        if (request.getModelName() != null) {
            dto.setModelName(request.getModelName());
        }
        if (request.getProviderType() != null) {
            dto.setProviderType(request.getProviderType());
        }
        if (request.getBaseUrl() != null) {
            dto.setBaseUrl(request.getBaseUrl());
        }
        if (request.getApiKey() != null) {
            dto.setApiKey(request.getApiKey());
        }
        if (request.getMaxTokens() != null) {
            dto.setMaxTokens(request.getMaxTokens());
        }
    }

    public ModelVO toVO(Model entity) {
        if (entity == null) {
            return null;
        }
        return ModelVO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .modelName(entity.getModelName())
                .providerType(entity.getProviderType())
                .baseUrl(entity.getBaseUrl())
                .apiKey(maskApiKey(entity.getApiKey()))
                .maxTokens(entity.getMaxTokens())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
