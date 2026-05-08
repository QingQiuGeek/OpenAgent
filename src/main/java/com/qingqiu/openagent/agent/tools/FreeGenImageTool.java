package com.qingqiu.openagent.agent.tools;

import cn.hutool.core.util.StrUtil;
import com.qingqiu.openagent.util.OSSUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author: qingqiugeek
 * @date: 2026/5/4 18:12
 * @description: FreeGenImage agent tool
 */
@Component
@Slf4j
public class FreeGenImageTool implements ITool {

    private static final String IMAGE_BASE_URL = "https://image.pollinations.ai/prompt/";

    @Override
    public String getName() {
        return "freeGenImageTool";
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
            value = "Generate an image based on a text description. Returns a Markdown image link `![alt](url)`. "
                    + "IMPORTANT: in your final reply to the user, you MUST include the returned `![alt](url)` "
                    + "markdown VERBATIM so the image is displayed; otherwise the user will only see text."
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

            log.info("[GenImageTool] prompt={}, upstream={}", prompt, imageUrl);

            // 转存到自己的 OSS，避免 pollinations 链接被墙 / 接口变更
            String ossUrl = transferToOssQuietly(imageUrl);

            // 工具返回不仅给出 markdown，还要再附一段提示，强制模型把 markdown 复制到最终回复里。
            // 否则有些模型只会用自然语言描述图片而不嵌入链接，导致前端无法渲染图片本体。
            String md = "![" + prompt + "](" + ossUrl + ")";
            return md
                    + "\n\nINSTRUCTION FOR THE ASSISTANT: copy the line above (the `![...](...)` markdown) "
                    + "verbatim into your final answer to the user, then add any extra description you like.";
        } catch (Exception e) {
            log.error("genImageTool error", e);
            return "Error generating image: " + e.getMessage();
        }
    }

    /** 把上游临时 URL 转存到自己的 OSS；失败时降级返回上游 URL 保证流程不中断。 */
    static String transferToOssQuietly(String upstreamUrl) {
        try {
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
            return OSSUtil.uploadFromUrl(upstreamUrl, "generate-image/" + ts);
        } catch (Exception ex) {
            log.warn("[FreeGenImageTool] OSS 转存失败，降级返回上游 URL: {}", ex.getMessage());
            return upstreamUrl;
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
