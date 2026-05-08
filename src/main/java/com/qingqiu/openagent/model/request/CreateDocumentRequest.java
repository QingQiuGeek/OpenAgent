package com.qingqiu.openagent.model.request;

import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/7 11:55
 * @description: CreateDocument request payload
 */

@Data
public class CreateDocumentRequest {
    private String kbId;
    private String filename;
    private String filetype;
    private Long size;
}

