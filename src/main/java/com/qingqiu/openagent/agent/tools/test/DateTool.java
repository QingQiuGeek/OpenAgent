package com.qingqiu.openagent.agent.tools.test;

import com.qingqiu.openagent.agent.tools.ITool;
import com.qingqiu.openagent.agent.tools.ToolType;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DateTool implements ITool {

    @Override
    public String getName() {
        return "dateTool";
    }

    @Override
    public String getDescription() {
        return "Get current date";
    }

    @Override
    public ToolType getType() {
        return ToolType.OPTIONAL;
    }

    @Tool(name = "getDate", value = "Return current date")
    public String getDate() {
        return LocalDate.now().toString();
    }
}
