package com.qingqiu.openagent.agent.tools;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class TerminateTool implements ITool {

    @Override
    public String getName() {
        return "terminateTool";
    }

    @Override
    public String getDescription() {
        return "Terminate current task";
    }

    @Override
    public ToolType getType() {
        return ToolType.FIXED;
    }

    @Tool(name = "terminate", value = "Call when task is fully completed")
    public void terminate() {
    }
}
