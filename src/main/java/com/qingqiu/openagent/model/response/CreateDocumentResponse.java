package com.qingqiu.openagent.model.response;

import lombok.Builder;
import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 13:31
 * @description: CreateDocument response payload
 */

@Data
@Builder
public class CreateDocumentResponse {
    private String documentId;
}

