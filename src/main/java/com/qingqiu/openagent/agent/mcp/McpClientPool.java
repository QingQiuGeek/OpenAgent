package com.qingqiu.openagent.agent.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingqiu.openagent.model.entity.McpServer;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 16:00
 * @description: MCP Client connection pool —— 按 mcp_server.id 缓存活跃 McpClient 实例
 */
@Slf4j
@Component
@AllArgsConstructor
public class McpClientPool {

    private final ObjectMapper objectMapper;

    /** key = mcp_server.id */
    private final Map<Long, McpClient> cache = new ConcurrentHashMap<>();

    /**
     * 获取或新建一个 McpClient。失败时抛 RuntimeException。
     * 调用方负责确保 cfg.getEnabled()==1（这里不再校验）。
     */
    public McpClient acquire(McpServer cfg) {
        if (cfg == null || cfg.getId() == null) {
            throw new IllegalArgumentException("McpServer 不能为空");
        }
        return cache.computeIfAbsent(cfg.getId(), k -> build(cfg));
    }

    /** 关闭并移除指定 server 对应的 client（CRUD 后调用，避免 stale 配置）。 */
    public void invalidate(Long mcpServerId) {
        if (mcpServerId == null) return;
        McpClient existing = cache.remove(mcpServerId);
        if (existing != null) {
            try {
                existing.close();
            } catch (Exception e) {
                log.warn("[McpClientPool] close mcp#{} failed: {}", mcpServerId, e.getMessage());
            }
        }
    }

    private McpClient build(McpServer cfg) {
        String transportType = cfg.getTransport() == null ? "stdio" : cfg.getTransport().toLowerCase();
        McpTransport transport;
        switch (transportType) {
            case "stdio" -> transport = buildStdio(cfg);
            case "sse", "http" -> transport = buildHttp(cfg);
            default -> throw new IllegalStateException("未知的 mcp transport: " + cfg.getTransport());
        }
        try {
            return new DefaultMcpClient.Builder()
                    .key(String.valueOf(cfg.getId()))
                    .clientName("OpenAgent")
                    .transport(transport)
                    .build();
        } catch (Exception e) {
            log.warn("[McpClientPool] 构建 mcp#{} 失败: {}", cfg.getId(), e.getMessage());
            throw new RuntimeException("构建 MCP 客户端失败: " + e.getMessage(), e);
        }
    }

    private McpTransport buildStdio(McpServer cfg) {
        String cmd = cfg.getCommand();
        if (cmd == null || cmd.isBlank()) {
            throw new IllegalStateException("stdio 模式必须填写 command");
        }
        List<String> commandList = Arrays.stream(cmd.trim().split("\\s+")).toList();
        Map<String, String> env = parseJsonStringMap(cfg.getEnv());
        return new StdioMcpTransport.Builder()
                .command(commandList)
                .environment(env == null ? Collections.emptyMap() : env)
                .logEvents(false)
                .build();
    }

    private McpTransport buildHttp(McpServer cfg) {
        String url = cfg.getUrl();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("sse / http 模式必须填写 url");
        }
        // 注意：当前依赖版本 (langchain4j-mcp 1.1.0-beta7) 的 HttpMcpTransport.Builder
        // 不支持自定义 headers；如有鉴权需求请等后续升级或用 stdio 包装代理。
        return new HttpMcpTransport.Builder()
                .sseUrl(url)
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    private Map<String, String> parseJsonStringMap(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("[McpClientPool] env JSON 解析失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
