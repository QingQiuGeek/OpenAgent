package com.qingqiu.openagent.controller;

import com.qingqiu.openagent.model.common.R;
import com.qingqiu.openagent.model.request.CreateKnowledgeBaseRequest;
import com.qingqiu.openagent.model.request.UpdateKnowledgeBaseRequest;
import com.qingqiu.openagent.model.response.CreateKnowledgeBaseResponse;
import com.qingqiu.openagent.model.response.GetKnowledgeBasesResponse;
import com.qingqiu.openagent.service.KnowledgeBaseFacadeService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/knowledge-bases")
@AllArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseFacadeService knowledgeBaseFacadeService;

    // 查询所有知识库
    @GetMapping
    public R<GetKnowledgeBasesResponse> getKnowledgeBases() {
        return R.success(knowledgeBaseFacadeService.getKnowledgeBases());
    }

    // 创建知识库
    @PostMapping
    public R<CreateKnowledgeBaseResponse> createKnowledgeBase(@RequestBody CreateKnowledgeBaseRequest request) {
        return R.success(knowledgeBaseFacadeService.createKnowledgeBase(request));
    }

    // 删除知识库
    @DeleteMapping("/{knowledgeBaseId}")
    public R<Boolean> deleteKnowledgeBase(@PathVariable String knowledgeBaseId) {
        knowledgeBaseFacadeService.deleteKnowledgeBase(knowledgeBaseId);
        return R.success(true);
    }

    // 更新知识库的名称、描述，不涉及文档或文件
    @PatchMapping("/{knowledgeBaseId}")
    public R<Boolean> updateKnowledgeBase(@PathVariable String knowledgeBaseId, @RequestBody UpdateKnowledgeBaseRequest request) {
        knowledgeBaseFacadeService.updateKnowledgeBase(knowledgeBaseId, request);
        return R.success(true);
    }
}
