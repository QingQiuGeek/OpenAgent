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
 * @author: qingqiugeek
 * @date: 2026/5/8 14:28
 * @description: ScrapeWebPage agent tool
 */
@Component
@Slf4j
public class ScrapeWebPageTool implements ITool {

    @Value("${tavily.extract-url}")
    private String extractUrl;

    @Value("${tavily.api-key}")
    private String apiKey;

    @Override
    public String getName() {
        return "scrapeWebPageTool";
    }

    @Override
    public String getDescription() {
        return "Scrape one or more web pages with Tavily Extract; returns clean markdown content per URL.";
    }

    @Override
    public ToolType getType() {
        return ToolType.OPTIONAL;
    }

    @Tool(
            name = "scrapeWebPage",
            value = "Extract the main readable content of a web page via Tavily. Pass a single full URL (with http/https). "
                    + "Returns a JSON string with a 'results' array (each item: url, raw_content) and 'failed_results'. "
                    + "Use 'advanced' depth only when the page is content-heavy (e.g. long articles, tables)."
    )
    public String scrapeWebPage(
            @P(value = "Full URL of the web page to extract, including http/https scheme")
            String url,
            @P(value = "Extraction depth, must be 'basic' (fast, default) or 'advanced' (more complete, costs 2 credits)")
            String extractDepth
    ) {
        if (StrUtil.isBlank(url)) {
            return errorJson("url cannot be empty");
        }
        if (StrUtil.isBlank(apiKey)) {
            return errorJson("Tavily api key is not configured (tavily.api-key / TAVILY_API_KEY)");
        }
        String depth = StrUtil.isBlank(extractDepth) ? "basic" : extractDepth.trim().toLowerCase();
        if (!"basic".equals(depth) && !"advanced".equals(depth)) {
            depth = "basic";
        }

        try {
            JSONObject body = new JSONObject()
                    .set("urls", new JSONArray().set(url))
                    .set("extract_depth", depth)
                    .set("format", "markdown")
                    .set("include_images", false)
                    .set("include_favicon", false);

            log.info("[ScrapeWebPageTool] url={} depth={}", url, depth);
            try (HttpResponse resp = HttpRequest.post(extractUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(30_000)
                    .execute()) {

                String respBody = resp.body();
                if (!resp.isOk()) {
                    log.warn("[ScrapeWebPageTool] tavily error status={} body={}", resp.getStatus(), respBody);
                    return errorJson("tavily http " + resp.getStatus() + ": " + respBody);
                }

                JSONObject json = JSONUtil.parseObj(respBody);

                // 仅保留模型需要的字段，避免 token 浪费
                JSONArray simplified = new JSONArray();
                JSONArray rawResults = json.getJSONArray("results");
                if (rawResults != null) {
                    for (int i = 0; i < rawResults.size(); i++) {
                        JSONObject item = rawResults.getJSONObject(i);
                        simplified.add(new JSONObject()
                                .set("url", item.getStr("url", ""))
                                .set("raw_content", item.getStr("raw_content", "")));
                    }
                }

                JSONArray failed = json.getJSONArray("failed_results");
                if (failed == null) {
                    failed = new JSONArray();
                }

                return new JSONObject()
                        .set("url", url)
                        .set("results", simplified)
                        .set("failed_results", failed)
                        .toString();
            }
        } catch (Exception e) {
            log.error("[ScrapeWebPageTool] error", e);
            return errorJson("scrape failed: " + e.getMessage());
        }
    }

    private String errorJson(String message) {
        return new JSONObject()
                .set("error", message)
                .set("results", new JSONArray())
                .set("failed_results", new JSONArray())
                .toString();
    }
}
