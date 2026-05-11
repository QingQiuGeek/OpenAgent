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
            value = "Deliver the FINAL answer to the user and end the conversation turn. "
                    + "Put the COMPLETE final answer for the user into the `message` argument (Markdown supported). "
                    + "This is the ONLY way to finish a turn — always call this exactly once at the end of the turn, "
                    + "do NOT leave the turn without calling it. After this call, the conversation is finished."
    )
    public String directAnswer(
            @P("The full final answer shown to the user. Required. Use Markdown if needed.")
            String message
    ) {
        return message == null ? "" : message;
    }
}
