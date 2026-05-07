package com.qingqiu.websearchmcpserver;

import com.qingqiu.websearchmcpserver.tools.WebSearchTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WebSearchMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebSearchMcpServerApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider webSearchTool(WebSearchTool webSearchTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(webSearchTool)
                .build();
    }

}
