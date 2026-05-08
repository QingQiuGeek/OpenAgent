package com.qingqiu.openagent.agent.tools;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.qingqiu.openagent.service.RagService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author: qingqiugeek
 * @date: 2026/5/6 18:47
 * @description: KnowledgeTools
 */

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

    @Tool(name = "KnowledgeTool", value = "Semantic search a knowledge base by id, returns relevant text chunks as JSON. Use the 'content' field of each result when answering the user.")
    public String knowledgeQuery(
            @P(value = "Knowledge base id to search in. Pick from the list provided in the system prompt.")
            String kbsId,
            @P(value = "User natural language query to search semantically in the knowledge base")
            String query) {
        List<String> results = ragService.similaritySearch(kbsId, query);
        // 返回 JSON，便于上游 Agent 提取 sources
        JSONArray arr = new JSONArray();
        for (String content : results) {
            arr.add(new JSONObject().set("content", content));
        }
        return new JSONObject()
                .set("kbId", kbsId)
                .set("query", query)
                .set("results", arr)
                .toString();
    }
}
