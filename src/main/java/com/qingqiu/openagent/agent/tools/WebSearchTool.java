package com.qingqiu.openagent.agent.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 联网搜索工具：基于 Tavily Search API。
 * <p>
 * 仅当用户在前端勾选「联网搜索」时，{@code ChatAgentFactory} 会把它加入运行时工具集。
 * 默认 ToolType=OPTIONAL，但不会出现在 agent 配置的 allowed_tools 列表里，
 * 而是由用户在每一次对话临时启用，避免污染普通对话的工具空间。
 *
 * 文档：<a href="https://docs.tavily.com/documentation/api-reference/endpoint/search">Tavily Search API</a>
 */
@Component
@Slf4j
public class WebSearchTool implements ITool {

    @Value("${tavily.url}")
    private String url;

    @Value("${tavily.api-key}")
    private  String apiKey;

    @Override
    public String getName() {
        return "webSearchTool";
    }

    @Override
    public String getDescription() {
        return "Search the web with Tavily; results include title, url, content, score and images";
    }

    @Override
    public ToolType getType() {
        return ToolType.OPTIONAL;
    }

    @Tool(
            name = "webSearch",
            value = "Search the web in real-time when the user asks about news, recent events, or any topic that needs up-to-date information from the internet. Returns a JSON string with a 'results' array (each item: title, url, content, score) and an 'images' array."
    )
    public String webSearch(
            @P(value = "Search query in natural language; must be concise and specific, not null")
            String query
    ) {
        if (StrUtil.isBlank(query)) {
            return errorJson("query cannot be empty");
        }
        if (StrUtil.isBlank(apiKey)) {
            return errorJson("Tavily api key is not configured (tavily.api-key / TAVILY_API_KEY)");
        }

        try {
            JSONObject body = new JSONObject()
                    .set("query", query)
                    .set("search_depth", "basic")
                    .set("max_results", 5)
                    .set("include_answer", false)
                    .set("include_images", true)
                    .set("include_raw_content", false);

            log.info("[WebSearchTool] query={}", query);
            try (HttpResponse resp = HttpRequest.post(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(10_000)
                    .execute()) {

                String respBody = resp.body();
                if (!resp.isOk()) {
                    log.warn("[WebSearchTool] tavily error status={} body={}", resp.getStatus(), respBody);
                    return errorJson("tavily http " + resp.getStatus() + ": " + respBody);
                }

                JSONObject json = JSONUtil.parseObj(respBody);

                // 仅保留模型需要的字段，避免 token 浪费
                JSONArray simplifiedResults = new JSONArray();
                JSONArray rawResults = json.getJSONArray("results");
                if (rawResults != null) {
                    for (int i = 0; i < rawResults.size(); i++) {
                        JSONObject item = rawResults.getJSONObject(i);
                        simplifiedResults.add(new JSONObject()
                                .set("title", item.getStr("title", ""))
                                .set("url", item.getStr("url", ""))
                                .set("content", item.getStr("content", ""))
                                .set("score", item.getDouble("score", 0d)));
                    }
                }

                JSONArray images = json.getJSONArray("images");
                if (images == null) {
                    images = new JSONArray();
                }

                return new JSONObject()
                        .set("query", query)
                        .set("results", simplifiedResults)
                        .set("images", images)
                        .toString();
            }
        } catch (Exception e) {
            log.error("[WebSearchTool] error", e);
            return errorJson("web search failed: " + e.getMessage());
        }
    }

    private String errorJson(String message) {
        return new JSONObject()
                .set("error", message)
                .set("results", new JSONArray())
                .set("images", new JSONArray())
                .toString();
    }
}
