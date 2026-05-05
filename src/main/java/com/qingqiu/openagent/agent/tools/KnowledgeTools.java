package com.qingqiu.openagent.agent.tools;

import com.qingqiu.openagent.service.RagService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KnowledgeTools implements ITool {

    private final RagService ragService;

    public KnowledgeTools(RagService ragService) {
        this.ragService = ragService;
    }

    @Override
    public String getName() {
        return "KnowledgeTool";
    }

    @Override
    public String getDescription() {
        return "Semantic search over knowledge base";
    }

    @Override
    public ToolType getType() {
        return ToolType.FIXED;
    }

    @Tool(name = "KnowledgeTool", value = "Search by kbId and query")
    public String knowledgeQuery(String kbsId, String query) {
        List<String> results = ragService.similaritySearch(kbsId, query);
        return String.join("\n", results);
    }
}
