package com.qingqiu.openagent.service;

import com.qingqiu.openagent.agent.tools.ITool;

import java.util.List;

public interface ToolFacadeService {
    List<ITool> getAllTools();

    List<ITool> getOptionalTools();

    List<ITool> getFixedTools();
}
