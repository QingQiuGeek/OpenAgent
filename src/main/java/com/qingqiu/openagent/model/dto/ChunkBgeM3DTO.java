package com.qingqiu.openagent.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author: qingqiugeek
 * @date: 2026/5/3 20:47
 * @description: ChunkBgeM3 DTO
 */

@Data
@Builder
public class ChunkBgeM3DTO {
    private String id;

    private String kbId;

    private String docId;

    private String content;

    private MetaData metadata;

    private float[] embedding;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Data
    public static class MetaData {
    }
}
