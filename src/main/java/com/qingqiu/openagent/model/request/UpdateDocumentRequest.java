package com.qingqiu.openagent.model.request;

import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/12 10:31
 * @description: UpdateDocument request payload
 */

@Data
public class UpdateDocumentRequest {
    private String filename;
    private String filetype;
    private Long size;
}

