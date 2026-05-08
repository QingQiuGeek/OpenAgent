package com.qingqiu.openagent.controller;

import com.qingqiu.openagent.model.common.R;
import com.qingqiu.openagent.model.request.CreateModelRequest;
import com.qingqiu.openagent.model.request.QueryModelRequest;
import com.qingqiu.openagent.model.request.UpdateModelRequest;
import com.qingqiu.openagent.model.response.CreateModelResponse;
import com.qingqiu.openagent.model.response.GetModelsResponse;
import com.qingqiu.openagent.service.ModelFacadeService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: qingqiugeek
 * @date: 2026/5/12 12:36
 * @description: Model controller
 */
@RestController
@RequestMapping("/api/models")
@AllArgsConstructor
public class ModelController {

    private final ModelFacadeService modelFacadeService;

    // 查询当前用户的模型列表
    @GetMapping
    public R<GetModelsResponse> getModels(QueryModelRequest request) {
        return R.success(modelFacadeService.getModels(request));
    }

    // 新建模型
    @PostMapping
    public R<CreateModelResponse> createModel(@RequestBody CreateModelRequest request) {
        return R.success(modelFacadeService.createModel(request));
    }

    // 修改模型（部分字段）
    @PatchMapping("/{modelId}")
    public R<Boolean> updateModel(@PathVariable Long modelId, @RequestBody UpdateModelRequest request) {
        modelFacadeService.updateModel(modelId, request);
        return R.success(true);
    }

    // 删除模型
    @DeleteMapping("/{modelId}")
    public R<Boolean> deleteModel(@PathVariable Long modelId) {
        modelFacadeService.deleteModel(modelId);
        return R.success(true);
    }
}
