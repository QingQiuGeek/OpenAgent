package com.qingqiu.openagent.service;

import com.qingqiu.openagent.model.request.CreateModelRequest;
import com.qingqiu.openagent.model.request.QueryModelRequest;
import com.qingqiu.openagent.model.request.UpdateModelRequest;
import com.qingqiu.openagent.model.response.CreateModelResponse;
import com.qingqiu.openagent.model.response.GetModelsResponse;

/**
 * @author: qingqiugeek
 * @date: 2026/5/12 18:34
 * @description: ModelFacade service
 */
public interface ModelFacadeService {

    GetModelsResponse getModels(QueryModelRequest request);

    CreateModelResponse createModel(CreateModelRequest request);

    void updateModel(Long modelId, UpdateModelRequest request);

    void deleteModel(Long modelId);
}
