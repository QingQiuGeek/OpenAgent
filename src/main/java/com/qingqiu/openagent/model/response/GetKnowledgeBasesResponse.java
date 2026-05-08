package com.qingqiu.openagent.model.response;

import com.qingqiu.openagent.model.vo.KnowledgeBaseVO;
import lombok.Builder;
import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/7 19:16
 * @description: GetKnowledgeBases response payload
 */

@Data
@Builder
public class GetKnowledgeBasesResponse {
    private KnowledgeBaseVO[] knowledgeBases;
}

