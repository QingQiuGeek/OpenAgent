package com.qingqiu.openagent.model.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelVO {

    private Long id;

    private Long userId;

    private String modelName;

    private String providerType;

    private String baseUrl;

    /** API Key 对外返回时做脱敏，避免泄露。 */
    private String apiKey;

    private Integer maxTokens;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
