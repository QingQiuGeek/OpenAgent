package com.qingqiu.openagent.controller;

import com.qingqiu.openagent.agent.tools.ITool;
import com.qingqiu.openagent.model.common.R;
import com.qingqiu.openagent.model.vo.McpToolGroupVO;
import com.qingqiu.openagent.service.ToolFacadeService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author: qingqiugeek
 * @date: 2026/5/8 09:17
 * @description: Tool controller
 */

@RestController
@RequestMapping("/api/tools")
@AllArgsConstructor
public class ToolController {

    private final ToolFacadeService toolFacadeService;

    // 给前端提供的工具列表（包含 FIXED 与 OPTIONAL）
    // 前端按 type 渲染：FIXED 置灰、强制已选、不可取消；OPTIONAL 可勾选
    @GetMapping
    public R<List<ITool>> getAllTools() {
        return R.success(toolFacadeService.getAllTools());
    }

    /** 当前用户已启用的 MCP 服务暴露的工具，按 server 分组 */
    @GetMapping("/mcp")
    public R<List<McpToolGroupVO>> getMyMcpToolGroups() {
        return R.success(toolFacadeService.listMyMcpToolGroups());
    }
}
