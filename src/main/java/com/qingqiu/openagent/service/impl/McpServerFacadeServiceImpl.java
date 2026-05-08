package com.qingqiu.openagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qingqiu.openagent.converter.McpServerConverter;
import com.qingqiu.openagent.enums.BizExceptionEnum;
import com.qingqiu.openagent.exception.BizException;
import com.qingqiu.openagent.mapper.McpServerMapper;
import com.qingqiu.openagent.model.entity.McpServer;
import com.qingqiu.openagent.model.request.CreateMcpServerRequest;
import com.qingqiu.openagent.model.request.UpdateMcpServerRequest;
import com.qingqiu.openagent.model.response.CreateMcpServerResponse;
import com.qingqiu.openagent.model.response.GetMcpServersResponse;
import com.qingqiu.openagent.model.vo.McpServerVO;
import com.qingqiu.openagent.service.McpServerFacadeService;
import com.qingqiu.openagent.util.UserContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author: qingqiugeek
 * @date: 2026/5/8 21:34
 * @description: McpServerFacade service implementation
 */

@Service
@AllArgsConstructor
public class McpServerFacadeServiceImpl implements McpServerFacadeService {

    private final McpServerMapper mcpServerMapper;
    private final McpServerConverter mcpServerConverter;

    @Override
    public GetMcpServersResponse getMcpServers() {
        Long userId = requireLoginUser();
        LambdaQueryWrapper<McpServer> qw = new LambdaQueryWrapper<>();
        qw.eq(McpServer::getUserId, userId).orderByDesc(McpServer::getUpdatedAt);
        List<McpServer> list = mcpServerMapper.selectList(qw);
        List<McpServerVO> vos = list.stream().map(mcpServerConverter::toVO).collect(Collectors.toList());
        return GetMcpServersResponse.builder().mcpServers(vos).build();
    }

    @Override
    public CreateMcpServerResponse createMcpServer(CreateMcpServerRequest request) {
        Long userId = requireLoginUser();
        McpServer entity = mcpServerConverter.toEntity(request);
        entity.setUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setIsDeleted(0);
        int rows = mcpServerMapper.insert(entity);
        if (rows <= 0) {
            throw new BizException(BizExceptionEnum.OPERATION_ERROR.getCode(), "创建 MCP Server 失败");
        }
        return CreateMcpServerResponse.builder().mcpServerId(entity.getId()).build();
    }

    @Override
    public void updateMcpServer(Long mcpServerId, UpdateMcpServerRequest request) {
        McpServer existing = requireOwned(mcpServerId);
        mcpServerConverter.applyUpdate(existing, request);
        existing.setUpdatedAt(LocalDateTime.now());
        int rows = mcpServerMapper.updateById(existing);
        if (rows <= 0) {
            throw new BizException(BizExceptionEnum.OPERATION_ERROR.getCode(), "更新 MCP Server 失败");
        }
    }

    @Override
    public void deleteMcpServer(Long mcpServerId) {
        McpServer existing = requireOwned(mcpServerId);
        int rows = mcpServerMapper.deleteById(existing.getId());
        if (rows <= 0) {
            throw new BizException(BizExceptionEnum.OPERATION_ERROR.getCode(), "删除 MCP Server 失败");
        }
    }

    private Long requireLoginUser() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BizException(BizExceptionEnum.NOT_LOGIN_ERROR.getCode(),
                    BizExceptionEnum.NOT_LOGIN_ERROR.getMessage());
        }
        return userId;
    }

    private McpServer requireOwned(Long mcpServerId) {
        if (mcpServerId == null) {
            throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(), "mcpServerId 不能为空");
        }
        Long userId = requireLoginUser();
        McpServer entity = mcpServerMapper.selectById(mcpServerId);
        if (entity == null) {
            throw new BizException(BizExceptionEnum.NOT_FOUND_ERROR.getCode(),
                    "MCP Server 不存在: " + mcpServerId);
        }
        if (!userId.equals(entity.getUserId())) {
            throw new BizException(BizExceptionEnum.FORBIDDEN_ERROR.getCode(),
                    BizExceptionEnum.FORBIDDEN_ERROR.getMessage());
        }
        return entity;
    }
}
