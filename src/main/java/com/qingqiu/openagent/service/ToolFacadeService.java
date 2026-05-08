package com.qingqiu.openagent.service;

import com.qingqiu.openagent.agent.tools.ITool;

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
}
