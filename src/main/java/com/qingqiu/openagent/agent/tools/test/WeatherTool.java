package com.qingqiu.openagent.agent.tools.test;

import com.qingqiu.openagent.agent.tools.ITool;
import com.qingqiu.openagent.agent.tools.ToolType;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class WeatherTool implements ITool {

    @Override
    public String getName() {
        return "weatherTool";
    }

    @Override
    public String getDescription() {
        return "Get weather by city and date";
    }

    @Override
    public ToolType getType() {
        return ToolType.OPTIONAL;
    }

    @Tool(name = "weather", value = "Return weather by city and date")
    public String getWeather(String city, String date) {
        return "Sunny in " + city + " on " + date;
    }
}
