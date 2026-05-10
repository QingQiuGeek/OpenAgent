package com.qingqiu.openagent.service;

import com.qingqiu.openagent.agent.tools.ITool;
import com.qingqiu.openagent.model.vo.McpToolGroupVO;

import java.util.List;

/**
 * @author: qingqiugeek
 * @date: 2026/5/8 20:28
 * @description: ToolFacade service
 */

public interface ToolFacadeService {
    List<ITool> getAllTools();

    List<ITool> getOptionalTools();

    List<ITool> getFixedTools();

    /** 当前用户已启用的 MCP server 暴露的工具，按 server 分组。 */
    List<McpToolGroupVO> listMyMcpToolGroups();
}
