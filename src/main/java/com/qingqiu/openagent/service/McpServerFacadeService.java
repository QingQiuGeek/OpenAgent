package com.qingqiu.openagent.service;

import com.qingqiu.openagent.model.request.CreateMcpServerRequest;
import com.qingqiu.openagent.model.request.UpdateMcpServerRequest;
import com.qingqiu.openagent.model.response.CreateMcpServerResponse;
import com.qingqiu.openagent.model.response.GetMcpServersResponse;

/** MCP Server 管理 Facade，多租户隔离。 */

/**
 * @author: qingqiugeek
 * @date: 2026/5/3 11:37
 * @description: McpServerFacade service
 */
public interface McpServerFacadeService {

    GetMcpServersResponse getMcpServers();

    CreateMcpServerResponse createMcpServer(CreateMcpServerRequest request);

    void updateMcpServer(Long mcpServerId, UpdateMcpServerRequest request);

    void deleteMcpServer(Long mcpServerId);

    /** 连接测试：返回工具名列表；失败抛 BizException。 */
    java.util.List<String> testConnection(Long mcpServerId);
}
