package com.qingqiu.openagent.model.request;

import com.qingqiu.openagent.model.dto.AgentDTO;
import lombok.Data;

import java.util.List;

/**
 * @author: qingqiugeek
 * @date: 2026/5/10 11:39
 * @description: UpdateAgent request payload
 */

@Data
public class UpdateAgentRequest {
    private String name;
    private String description;
    private String systemPrompt;
    private Long modelId;
    private List<String> allowedTools;
    private List<String> allowedKbs;
    private AgentDTO.ChatOptions chatOptions;
}
