package com.qingqiu.openagent.model.request;

import com.qingqiu.openagent.model.dto.AgentDTO;
import lombok.Data;

import java.util.List;

/**
 * @author: qingqiugeek
 * @date: 2026/5/2 12:27
 * @description: CreateAgent request payload
 */

@Data
public class CreateAgentRequest {
    private String name;
    private String description;
    private String systemPrompt;
    private Long modelId;
    private List<String> allowedTools;
    private List<String> allowedKbs;
    private AgentDTO.ChatOptions chatOptions;
}
