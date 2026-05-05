package com.qingqiu.openagent.service;

import com.qingqiu.openagent.model.request.CreateModelRequest;
import com.qingqiu.openagent.model.request.QueryModelRequest;
import com.qingqiu.openagent.model.request.UpdateModelRequest;
import com.qingqiu.openagent.model.response.CreateModelResponse;
import com.qingqiu.openagent.model.response.GetModelsResponse;

/**
 * 模型管理 Facade。所有操作按登录用户隔离。
 */
public interface ModelFacadeService {

    GetModelsResponse getModels(QueryModelRequest request);

    CreateModelResponse createModel(CreateModelRequest request);

    void updateModel(Long modelId, UpdateModelRequest request);

    void deleteModel(Long modelId);
}
