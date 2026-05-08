package com.qingqiu.openagent.model.response;

import com.qingqiu.openagent.model.vo.DocumentVO;
import lombok.Builder;
import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/8 21:33
 * @description: GetDocuments response payload
 */

@Data
@Builder
public class GetDocumentsResponse {
    private DocumentVO[] documents;
}

