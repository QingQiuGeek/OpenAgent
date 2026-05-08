package com.qingqiu.openagent.model.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: qingqiugeek
 * @date: 2026/5/10 10:48
 * @description: Model DTO
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelDTO {

    private Long id;

    private Long userId;

    private String modelName;

    private String providerType;

    private String baseUrl;

    private String apiKey;

    private Integer maxTokens;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
