package com.qingqiu.openagent.agent.tools;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 文生图工具。基于 Pollinations.ai（免 API key），把 prompt 拼成图片直链返回，
 * 以 Markdown 图片语法返回，方便前端 markdown 渲染直接显示。
 */
@Component
@Slf4j
public class GenImageTool implements ITool {

    private static final String IMAGE_BASE_URL = "https://image.pollinations.ai/prompt/";

    @Override
    public String getName() {
        return "genImageTool";
    }

    @Override
    public String getDescription() {
        return "Generate an image from a text prompt and return its URL";
    }

    @Override
    public ToolType getType() {
        return ToolType.OPTIONAL;
    }

    @Tool(
            name = "generateImage",
            value = "Generate an image based on a text description. Returns a Markdown image link that can be embedded in the reply."
    )
    public String genImage(
            @P(value = "Text description (prompt) of the image to generate, in English for best quality")
            String prompt,
            @P(value = "Image width in pixels, default 1024 if blank or invalid")
            String width,
            @P(value = "Image height in pixels, default 1024 if blank or invalid")
            String height
    ) {
        try {
            if (StrUtil.isBlank(prompt)) {
                return "Error: prompt cannot be empty.";
            }
            int w = parseSizeOrDefault(width, 1024);
            int h = parseSizeOrDefault(height, 1024);

            String encoded = URLEncoder.encode(prompt.trim(), StandardCharsets.UTF_8);
            String imageUrl = IMAGE_BASE_URL + encoded
                    + "?width=" + w
                    + "&height=" + h
                    + "&nologo=true";

            log.info("[GenImageTool] prompt={}, url={}", prompt, imageUrl);
            // 直接返回 Markdown 图片，可被前端 XMarkdown 渲染显示
            return "![" + prompt + "](" + imageUrl + ")";
        } catch (Exception e) {
            log.error("genImageTool error", e);
            return "Error generating image: " + e.getMessage();
        }
    }

    private int parseSizeOrDefault(String value, int defaultValue) {
        if (StrUtil.isBlank(value)) {
            return defaultValue;
        }
        try {
            int n = Integer.parseInt(value.trim());
            if (n < 64 || n > 2048) {
                return defaultValue;
            }
            return n;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
