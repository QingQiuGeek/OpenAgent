package com.qingqiu.openagent.agent.tools;

import cn.hutool.core.util.StrUtil;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationOutput;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.qingqiu.openagent.enums.BizExceptionEnum;
import com.qingqiu.openagent.exception.BizException;
import com.qingqiu.openagent.util.OSSUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author: qingqiugeek
 * @date: 2026/5/8 08:25
 * @description: QwenGenImage agent tool
 */
@Slf4j
@Component
public class QwenGenImageTool implements ITool {

  /** 默认尺寸：DashScope qwen 系列使用 W*H 表示（注意中间是星号，不是 x）。 */
  private static final String DEFAULT_SIZE = "1024*1024";

  /** 文生图模型名，例如 qwen2.1-t2i-turbo / qwen2.1-t2i-plus / qwen-v1。 */
  @Value("${image-model.qwen.model-name}")
  private String imageModelName;

  /** DashScope API Key。可在 application-local.yaml 或环境变量 DASHSCOPE_API_KEY 中配置。 */
  @Value("${image-model.qwen.api-key}")
  private String apiKey;

  @Override
  public String getDescription() {
    return "调用阿里云通义千问生成高质量图片png，返回 markdown 图片语法。";
  }

  @Override
  public String getName() {
    return "qwenGenImageTool";
  }

  @Override
  public ToolType getType() {
    return ToolType.OPTIONAL;
  }

  @Tool(
      name = "qwenGenerateImage",
      value = "Generate a high-quality image with png. "
          + "Use this whenever the user asks to draw / paint / generate / produce an image, illustration, poster, "
          + "icon, avatar, or scene. Returns a Markdown image link `![alt](url)`. "
          + "IMPORTANT: in your final reply to the user, you MUST include the returned `![alt](url)` markdown VERBATIM "
          + "so the image is displayed; otherwise the user will only see text."
  )
  public String generateImage(
      @P(value = "Image description (prompt). 中英文均可，越具体越好；最多 800 个字符。")
      String prompt,
      @P(value = "image size, format must be `WIDTH*HEIGHT`!"
          + "Common: 16:9 is 2688*1536, 9:16 is 1536*2688, 1:1 is 2048*2048, 4:3 is 2368*1728, 3:4 is 1728*2368. Default 2048*2048",
          required = false)
      String size
  ) {
    if (StrUtil.isBlank(prompt)) {
      return "Error: prompt cannot be empty.";
    }
    if (StrUtil.isBlank(apiKey)) {
      log.warn("[QwenGenImageTool] apiKey is blank; configure image-model.api-key or DASHSCOPE_API_KEY env var");
      return "图片生成失败：DashScope API Key 未配置，请在 application-local.yaml 设置 image-model.api-key 或环境变量 DASHSCOPE_API_KEY。";
    }
    MultiModalConversation conv = new MultiModalConversation();

    MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
        .content(Arrays.asList(
            Collections.singletonMap("text", prompt)
        )).build();

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("watermark", false);
    parameters.put("prompt_extend", true);
    parameters.put("negative_prompt", "低分辨率，低画质，肢体畸形，手指畸形，画面过饱和，蜡像感，人脸无细节，过度光滑，画面具有AI感。构图混乱。文字模糊，扭曲。");

    MultiModalConversationParam param = MultiModalConversationParam.builder()
        .apiKey(apiKey)
        .model(imageModelName)
        .messages(Collections.singletonList(userMessage))
        .parameters(parameters)
        .build();
    try {
      MultiModalConversationResult result = conv.call(param);
      MultiModalConversationOutput output = result.getOutput();
      if (output == null) {
        throw new BizException(BizExceptionEnum.REQUEST_ERROR.getCode(),
            BizExceptionEnum.REQUEST_ERROR.getMessage());
      }
      String upstreamUrl = output.getChoices().get(0).getMessage().getContent().get(0)
          .get("image").toString();
      log.info("[QwenGenImageTool] upstream={}", upstreamUrl);

      // 上游 URL 是带签名的临时链接（24h 后失效），转存到自己的 OSS 拿永久 URL
      String ossUrl = transferToOssQuietly(upstreamUrl);
      String md = "![" + prompt.trim() + "](" + ossUrl + ")";
      return md
          + "\n\nINSTRUCTION FOR THE ASSISTANT: copy the line above (the `![...](...)` markdown) "
          + "verbatim into your final answer to the user, then add any extra description you like.";
    } catch (BizException be) {
      throw be;
    } catch (ApiException apiEx) {
      // DashScope SDK 的 ApiException.getMessage() 经常为 null，真实错误体藏在 toString() 里，
      // 形如：{"statusCode":400,"message":"Input data is suspected of being involved in IP infringement.","code":"IPInfringementSuspect",...}
      String friendly = parseDashScopeError(apiEx);
      log.error("[QwenGenImageTool] DashScope 拒绝：{}", friendly);
      // 不抛异常，直接返回友好文本给 LLM —— LLM 会把这条文字组装到 directAnswer 里展示给用户。
      // 这样比 throw 让 ChatAgent 拼 "Tool error: null" 体验好得多。
      return "图片生成失败：" + friendly;
    } catch (Exception e) {
      log.error("[QwenGenImageTool] generate failed", e);
      String msg = e.getMessage() != null ? e.getMessage()
              : e.getClass().getSimpleName() + ": " + e;
      return "图片生成失败：" + msg;
    }
  }

  /**
   * 解析 DashScope ApiException 中的真实错误码与消息。
   * 已知典型错误码 → 人话翻译，让用户看明白：
   * <ul>
   *   <li>IPInfringementSuspect：内容涉及受版权保护的 IP（如卡通形象），平台拒绝生成</li>
   *   <li>DataInspectionFailed：内容审查未通过</li>
   *   <li>InvalidApiKey / Throttling：配置/限速问题</li>
   * </ul>
   */
  private static String parseDashScopeError(ApiException apiEx) {
    String raw = apiEx.getMessage();
    if (raw == null) raw = apiEx.toString();
    String code = "";
    String message = raw;
    try {
      // 错误体可能含也可能不含 JSON；用 hutool 健壮解析
      int braceIdx = raw.indexOf('{');
      if (braceIdx >= 0 && JSONUtil.isTypeJSON(raw.substring(braceIdx))) {
        JSONObject json = JSONUtil.parseObj(raw.substring(braceIdx));
        code = json.getStr("code", "");
        message = json.getStr("message", message);
      }
    } catch (Exception ignore) { /* 解析失败用原文 */ }

    String hint = switch (code) {
      case "IPInfringementSuspect" -> "请求内容疑似涉及受版权保护的卡通 / 影视 / 商业 IP，平台依据版权审查拒绝生成。请改用通用描述（例如：'灰色拟人化狼角色，可爱风格'）。";
      case "DataInspectionFailed" -> "请求内容未通过内容安全审查。";
      case "InvalidApiKey", "AccessDenied" -> "DashScope API Key 无效或权限不足，请联系管理员检查 image-model.qwen.api-key。";
      case "Throttling", "Throttling.RateQuota" -> "调用过于频繁，已触发限速。请稍后再试。";
      default -> "";
    };
    return StrUtil.isBlank(hint) ? message : message + "（" + hint + "）";
  }

  /**
   * 把上游临时图片链接转存到自己的 OSS，路径规则：
   * <pre>upload/generate-image/yyyy-MM-dd-HH-mm-ss/{ts}_{random}.{ext}</pre>
   * 转存失败时降级返回原始 URL（仍可在有效期内访问），保证流程不中断。
   */
  static String transferToOssQuietly(String upstreamUrl) {
    try {
      String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
      return OSSUtil.uploadFromUrl(upstreamUrl, "generate-image/" + ts);
    } catch (Exception ex) {
      log.warn("[QwenGenImageTool] OSS 转存失败，降级返回上游 URL: {}", ex.getMessage());
      return upstreamUrl;
    }
  }
}
