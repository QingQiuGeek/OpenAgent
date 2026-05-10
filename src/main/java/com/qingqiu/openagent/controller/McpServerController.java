package com.qingqiu.openagent.controller;

import com.qingqiu.openagent.model.common.R;
import com.qingqiu.openagent.model.request.CreateMcpServerRequest;
import com.qingqiu.openagent.model.request.UpdateMcpServerRequest;
import com.qingqiu.openagent.model.response.CreateMcpServerResponse;
import com.qingqiu.openagent.model.response.GetMcpServersResponse;
import com.qingqiu.openagent.service.McpServerFacadeService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** MCP Server 管理，多租户；Agent 通过 allowed_tools 中的 {@code mcp:{id}} 引用。 */

/**
 * @author: qingqiugeek
 * @date: 2026/5/6 17:30
 * @description: McpServer controller
 */
@RestController
@RequestMapping("/api/mcp-servers")
@AllArgsConstructor
public class McpServerController {

    private final McpServerFacadeService mcpServerFacadeService;

    @GetMapping
    public R<GetMcpServersResponse> getMcpServers() {
        return R.success(mcpServerFacadeService.getMcpServers());
    }

    @PostMapping
    public R<CreateMcpServerResponse> createMcpServer(@RequestBody CreateMcpServerRequest request) {
        return R.success(mcpServerFacadeService.createMcpServer(request));
    }

    @PatchMapping("/{mcpServerId}")
    public R<Boolean> updateMcpServer(@PathVariable Long mcpServerId,
                                      @RequestBody UpdateMcpServerRequest request) {
        mcpServerFacadeService.updateMcpServer(mcpServerId, request);
        return R.success(true);
    }

    @DeleteMapping("/{mcpServerId}")
    public R<Boolean> deleteMcpServer(@PathVariable Long mcpServerId) {
        mcpServerFacadeService.deleteMcpServer(mcpServerId);
        return R.success(true);
    }

    /** 连接测试：返回 MCP server 暴露的工具名列表；失败抛 BizException 由全局异常处理 */
    @PostMapping("/{mcpServerId}/test")
    public R<java.util.List<String>> testConnection(@PathVariable Long mcpServerId) {
        return R.success(mcpServerFacadeService.testConnection(mcpServerId));
    }
}
