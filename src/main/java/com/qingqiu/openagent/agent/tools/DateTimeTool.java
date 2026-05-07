package com.qingqiu.openagent.agent.tools;

import cn.hutool.core.date.DateTime;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class DateTimeTool implements ITool {

    @Override
    public String getName() {
        return "dateTimeTool";
    }

    @Override
    public String getDescription() {
        return "Get current dateTime";
    }

    @Override
    public ToolType getType() {
        return ToolType.FIXED;
    }

    @Tool(name = "dateTimeTool", value = "Get current dateTime")
    public String dateTimeTool() {
        return DateTime.now().toString();
    }
}
