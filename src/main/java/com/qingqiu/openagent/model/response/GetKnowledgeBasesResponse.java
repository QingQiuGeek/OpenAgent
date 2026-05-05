package com.qingqiu.openagent.model.response;

import com.qingqiu.openagent.model.vo.KnowledgeBaseVO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetKnowledgeBasesResponse {
    private KnowledgeBaseVO[] knowledgeBases;
}

