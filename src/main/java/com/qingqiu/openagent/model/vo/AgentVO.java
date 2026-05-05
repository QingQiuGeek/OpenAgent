package com.qingqiu.openagent.model.vo;

import com.qingqiu.openagent.model.dto.AgentDTO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AgentVO {
    private String id;

    private Long userId;

    private String name;

    private String description;

    private String systemPrompt;

    private Long modelId;

    /** 展示用的模型名，由 Converter 通过 {@code model} 表补齐（可能为空）。 */
    private String modelName;

    private List<String> allowedTools;

    private List<String> allowedKbs;

    private AgentDTO.ChatOptions chatOptions;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
