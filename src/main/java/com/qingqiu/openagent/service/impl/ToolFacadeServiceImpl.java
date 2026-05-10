package com.qingqiu.openagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingqiu.openagent.agent.mcp.McpClientPool;
import com.qingqiu.openagent.agent.tools.ITool;
import com.qingqiu.openagent.agent.tools.ToolType;
import com.qingqiu.openagent.mapper.McpServerMapper;
import com.qingqiu.openagent.model.entity.McpServer;
import com.qingqiu.openagent.model.vo.McpToolGroupVO;
import com.qingqiu.openagent.service.ToolFacadeService;
import com.qingqiu.openagent.util.UserContext;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author: qingqiugeek
 * @date: 2026/5/10 16:23
 * @description: ToolFacade service implementation
 */

@Slf4j
@Service
@AllArgsConstructor
public class ToolFacadeServiceImpl implements ToolFacadeService {

    private final List<ITool> iTools;
    private final McpServerMapper mcpServerMapper;
    private final McpClientPool mcpClientPool;

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

    @Override
    public List<McpToolGroupVO> listMyMcpToolGroups() {
        Long userId = UserContext.getUser();
        if (userId == null) return Collections.emptyList();
        List<McpServer> servers = mcpServerMapper.selectList(new LambdaQueryWrapper<McpServer>()
                .eq(McpServer::getUserId, userId)
                .eq(McpServer::getEnabled, 1)
                .orderByAsc(McpServer::getId));
        List<McpToolGroupVO> result = new ArrayList<>();
        for (McpServer s : servers) {
            McpToolGroupVO.McpToolGroupVOBuilder b = McpToolGroupVO.builder()
                    .mcpServerId(s.getId())
                    .mcpServerName(s.getName())
                    .transport(s.getTransport());
            try {
                McpClient client = mcpClientPool.acquire(s);
                List<ToolSpecification> tools = client.listTools();
                List<String> names = tools == null
                        ? Collections.emptyList()
                        : tools.stream().map(ToolSpecification::name).toList();
                result.add(b.toolNames(names).ok(true).build());
            } catch (Exception e) {
                log.warn("[ToolFacade] mcp#{} listTools 失败: {}", s.getId(), e.getMessage());
                result.add(b.toolNames(Collections.emptyList()).ok(false).errorMsg(e.getMessage()).build());
            }
        }
        return result;
    }

    private List<ITool> getToolsByType(ToolType type) {
        return iTools.stream()
                .filter(tool -> tool.getType().equals(type))
                .toList();
    }
}
