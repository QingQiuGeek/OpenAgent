package com.qingqiu.websearchmcpserver.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Tavily 网络搜索工具。
 * 文档：<a href="https://docs.tavily.com/documentation/api-reference/endpoint/search">Tavily Search API</a>
 *
 * 通过 application-*.yml 中 {@code tavily.api-key} 注入 API Key，
 * 也可通过环境变量 {@code TAVILY_API_KEY} 覆盖（yml 中已配置占位符）。
 */
@Slf4j
@Service
public class WebSearchTool {

    @Value("${tavily.url:}")
    private String API_URL;

    @Value("${tavily.api-key:}")
    private  String apiKey;

    @Tool(
            name = "webSearch",
            description = "Search the web with Tavily. Returns a JSON string with a 'results' array (each item has title, url, content, score) and a top-level 'images' array."
    )
    public String webSearch(
            @ToolParam(description = "Search query keyword in natural language") String query
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
                    .set("max_results", 6)
                    .set("include_answer", false)
                    .set("include_images", true)
                    .set("include_raw_content", false);

            log.info("[WebSearchTool] query={}", query);
            try (HttpResponse resp = HttpRequest.post(API_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(15_000)
                    .execute()) {

                String respBody = resp.body();
                if (!resp.isOk()) {
                    log.warn("[WebSearchTool] tavily error status={} body={}", resp.getStatus(), respBody);
                    return errorJson("tavily http " + resp.getStatus() + ": " + respBody);
                }

                JSONObject json = JSONUtil.parseObj(respBody);

                JSONArray rawResults = json.getJSONArray("results");
                List<JSONObject> results = new ArrayList<>();
                if (rawResults != null) {
                    for (int i = 0; i < rawResults.size(); i++) {
                        JSONObject item = rawResults.getJSONObject(i);
                        JSONObject simplified = new JSONObject()
                                .set("title", item.getStr("title", ""))
                                .set("url", item.getStr("url", ""))
                                .set("content", item.getStr("content", ""))
                                .set("score", item.getDouble("score", 0d));
                        results.add(simplified);
                    }
                }

                JSONArray images = json.getJSONArray("images");
                if (images == null) {
                    images = new JSONArray();
                }

                JSONObject out = new JSONObject()
                        .set("query", query)
                        .set("results", results)
                        .set("images", images);
                return out.toString();
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
