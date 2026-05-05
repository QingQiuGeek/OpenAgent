package com.qingqiu.openagent.service.impl;

import com.qingqiu.openagent.agent.tools.ITool;
import com.qingqiu.openagent.agent.tools.ToolType;
import com.qingqiu.openagent.service.ToolFacadeService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ToolFacadeServiceImpl implements ToolFacadeService {

    private final List<ITool> iTools;

    @Override
    public List<ITool> getAllTools() {
        return iTools;
    }

    @Override
    public List<ITool> getOptionalTools() {
        return getToolsByType(ToolType.OPTIONAL);
    }

    @Override
    public List<ITool> getFixedTools() {
        return getToolsByType(ToolType.FIXED);
    }

    private List<ITool> getToolsByType(ToolType type) {
        return iTools.stream()
                .filter(tool -> tool.getType().equals(type))
                .toList();
    }

    // -------------------------------------------------------------------------
    // TODO（Step 7 后续扩展）：识别 agent.allowed_tools 中形如 "mcp:{id}" 的条目，
    // 注入 McpServerMapper，按 id 查出 McpServer 记录，用 langchain4j-mcp 的
    // McpClient 构建 ToolSpecification 列表并合并入运行时工具集合。
    // 目前仅提供 MCP Server 的 CRUD；实际工具挂载待 Step 6/Step 7 的后续任务接入。
    // -------------------------------------------------------------------------
}
