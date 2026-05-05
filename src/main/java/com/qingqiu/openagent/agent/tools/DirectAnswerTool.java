package com.qingqiu.openagent.agent.tools;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class DirectAnswerTool implements ITool {

    @Override
    public String getName() {
        return "directAnswer";
    }

    @Override
    public String getDescription() {
        return "Directly answer user without further tools";
    }

    @Override
    public ToolType getType() {
        return ToolType.FIXED;
    }

    @Tool(name = "directAnswer", value = "Use this when no additional tool is needed")
    public void directAnswer() {
    }
}
