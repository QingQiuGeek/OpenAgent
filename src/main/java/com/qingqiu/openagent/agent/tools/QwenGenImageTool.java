package com.qingqiu.openagent.agent.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationOutput;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationOutput.Choice;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.qingqiu.openagent.enums.BizExceptionEnum;
import com.qingqiu.openagent.exception.BizException;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.community.model.dashscope.WanxImageModel;
import dev.langchain4j.community.model.dashscope.WanxImageSize;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.output.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 通义千问（DashScope qwen 系列）文生图工具。
 * <p>相较于免费的 Pollinations，千问在结构、中文语义与画面质量上一般更佳，适合付费场景。</p>
 *
 * <h3>API Key 配置</h3>
 * <pre>
 * # application.yaml 或 application-local.yaml
 * dashscope:
 *   api-key: ${DASHSCOPE_API_KEY:}   # 推荐走环境变量
 * </pre>
 * 代码会优先用上述配置；为空时退化到 SDK 默认（读环境变量 {@code DASHSCOPE_API_KEY}）。
 * 申请 Key：https://bailian.console.aliyun.com/?tab=model#/api-key
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
      if(output == null){
        throw new BizException(BizExceptionEnum.REQUEST_ERROR.getCode(),BizExceptionEnum.REQUEST_ERROR.getMessage());
      }
      return output.getChoices().get(0).getMessage().getContent().get(0).get("image").toString();
    } catch (Exception e) {
      throw new BizException(BizExceptionEnum.REQUEST_ERROR.getCode(),e.getMessage());
    }
  }
}
