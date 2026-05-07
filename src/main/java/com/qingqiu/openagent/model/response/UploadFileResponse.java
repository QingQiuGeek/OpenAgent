package com.qingqiu.openagent.model.response;

import lombok.Builder;
import lombok.Data;

/**
 * 文件上传成功后的元数据，前端拿到后写入 message.metadata.attachments。
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
