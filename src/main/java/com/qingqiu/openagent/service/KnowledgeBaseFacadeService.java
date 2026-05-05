package com.qingqiu.openagent.service;

import com.qingqiu.openagent.model.request.CreateKnowledgeBaseRequest;
import com.qingqiu.openagent.model.request.UpdateKnowledgeBaseRequest;
import com.qingqiu.openagent.model.response.CreateKnowledgeBaseResponse;
import com.qingqiu.openagent.model.response.GetKnowledgeBasesResponse;

public interface KnowledgeBaseFacadeService {
    GetKnowledgeBasesResponse getKnowledgeBases();

    CreateKnowledgeBaseResponse createKnowledgeBase(CreateKnowledgeBaseRequest request);

    void deleteKnowledgeBase(String knowledgeBaseId);

    void updateKnowledgeBase(String knowledgeBaseId, UpdateKnowledgeBaseRequest request);
}

