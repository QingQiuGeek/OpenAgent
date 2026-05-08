package com.qingqiu.openagent.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author: qingqiugeek
 * @date: 2026/5/5 09:50
 * @description: Document DTO
 */

@Data
@Builder
public class DocumentDTO {
    private String id;

    private Long userId;

    private String kbId;

    private String filename;

    private String filetype;

    private Long size;

    private MetaData metadata;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Data
    public static class MetaData {
        private String filePath; // 文件存储路径
    }
}
