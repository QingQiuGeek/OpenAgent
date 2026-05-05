package com.qingqiu.openagent.controller;

import com.qingqiu.openagent.agent.tools.ITool;
import com.qingqiu.openagent.model.common.R;
import com.qingqiu.openagent.service.ToolFacadeService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tools")
@AllArgsConstructor
public class ToolController {

    private final ToolFacadeService toolFacadeService;

    // 给前端提供的可选的工具列表
    @GetMapping
    public R<List<ITool>> getOptionalTools() {
        return R.success(toolFacadeService.getOptionalTools());
    }
}
