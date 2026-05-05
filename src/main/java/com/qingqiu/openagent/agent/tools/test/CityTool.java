package com.qingqiu.openagent.agent.tools.test;

import com.qingqiu.openagent.agent.tools.ITool;
import com.qingqiu.openagent.agent.tools.ToolType;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class CityTool implements ITool {

    @Override
    public String getName() {
        return "cityTool";
    }

    @Override
    public String getDescription() {
        return "Get current city";
    }

    @Override
    public ToolType getType() {
        return ToolType.OPTIONAL;
    }

    @Tool(name = "getCity", value = "Return current city")
    public String getCity() {
        return "Shanghai";
    }
}
