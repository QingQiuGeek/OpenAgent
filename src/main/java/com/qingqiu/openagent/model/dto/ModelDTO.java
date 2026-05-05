package com.qingqiu.openagent.model.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
