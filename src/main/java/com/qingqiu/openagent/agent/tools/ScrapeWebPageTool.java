package com.qingqiu.openagent.agent.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

/**
 * 网页抓取工具
 */
@Component
public class ScrapeWebPageTool implements ITool {

    @Override
    public String getDescription() {
        return "Scrape the content of a web page given its URL.";
    }

    @Override
    public String getName() {
        return "scrapeWebPageTool";
    }

    @Override
    public ToolType getType() {
        return ToolType.OPTIONAL;
    }

    @Tool("Scrape the content of a web page")
    public String scrapeWebPageTool(@P(value = "URL of the web page to scrape") String url) {
        try {
            Document document = Jsoup.connect(url).get();
            return document.html();
        } catch (Exception e) {
            return "Error scraping web page: " + e.getMessage();
        }
    }
}
