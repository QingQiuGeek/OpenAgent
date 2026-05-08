package com.qingqiu.openagent.agent.tools;

/**
 * @author: qingqiugeek
 * @date: 2026/5/1 09:37
 * @description: I agent tool
 */

public interface ITool {
    String getName();

    String getDescription();

    ToolType getType();
}
