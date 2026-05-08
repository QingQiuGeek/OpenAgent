package com.qingqiu.openagent.agent.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * @author: qingqiugeek
 * @date: 2026/5/9 12:13
 * @description: DirectAnswer agent tool
 */

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

    @Tool(
            name = "directAnswer",
            value = "Call this EXACTLY ONCE when you can fully answer the user's question without any other tool. "
                    + "Put the COMPLETE final answer for the user into the `message` argument (Markdown supported). "
                    + "After this call, the conversation is finished — do not call directAnswer or terminate again."
    )
    public String directAnswer(
            @P("The full final answer shown to the user. Required. Use Markdown if needed.")
            String message
    ) {
        return message == null ? "" : message;
    }
}
