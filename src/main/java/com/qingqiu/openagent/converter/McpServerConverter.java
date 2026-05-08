package com.qingqiu.openagent.converter;

import com.qingqiu.openagent.model.entity.McpServer;
import com.qingqiu.openagent.model.request.CreateMcpServerRequest;
import com.qingqiu.openagent.model.request.UpdateMcpServerRequest;
import com.qingqiu.openagent.model.vo.McpServerVO;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author: qingqiugeek
 * @date: 2026/5/4 17:03
 * @description: McpServer converter
 */

@Component
public class McpServerConverter {

    public McpServer toEntity(CreateMcpServerRequest request) {
        Assert.notNull(request, "CreateMcpServerRequest cannot be null");
        Assert.hasText(request.getName(), "name is required");
        Assert.hasText(request.getTransport(), "transport is required");
        return McpServer.builder()
                .name(request.getName())
                .transport(request.getTransport())
                .command(request.getCommand())
                .url(request.getUrl())
                .headers(request.getHeaders())
                .env(request.getEnv())
                .enabled(request.getEnabled() == null ? 1 : request.getEnabled())
                .build();
    }

    public void applyUpdate(McpServer entity, UpdateMcpServerRequest request) {
        Assert.notNull(entity, "McpServer cannot be null");
        Assert.notNull(request, "UpdateMcpServerRequest cannot be null");
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getTransport() != null) entity.setTransport(request.getTransport());
        if (request.getCommand() != null) entity.setCommand(request.getCommand());
        if (request.getUrl() != null) entity.setUrl(request.getUrl());
        if (request.getHeaders() != null) entity.setHeaders(request.getHeaders());
        if (request.getEnv() != null) entity.setEnv(request.getEnv());
        if (request.getEnabled() != null) entity.setEnabled(request.getEnabled());
    }

    public McpServerVO toVO(McpServer entity) {
        if (entity == null) return null;
        return McpServerVO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .name(entity.getName())
                .transport(entity.getTransport())
                .command(entity.getCommand())
                .url(entity.getUrl())
                .headers(entity.getHeaders())
                .env(entity.getEnv())
                .enabled(entity.getEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
