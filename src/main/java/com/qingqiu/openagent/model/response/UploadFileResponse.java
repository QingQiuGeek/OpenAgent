package com.qingqiu.openagent.model.response;

import lombok.Builder;
import lombok.Data;

/**
 * @author: qingqiugeek
 * @date: 2026/5/3 15:59
 * @description: UploadFile response payload
 */
@Data
@Builder
public class UploadFileResponse {
    /** OSS 公网访问 URL */
    private String url;
    /** 原始文件名 */
    private String name;
    /** 字节数 */
    private Long size;
    /** MIME 类型，如 image/png */
    private String contentType;
}
