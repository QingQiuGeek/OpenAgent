package com.qingqiu.openagent.agent.tools;

import cn.hutool.core.util.StrUtil;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGeneration;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationMessage;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationOutput;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationParam;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationResult;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationOutput;
import com.alibaba.dashscope.utils.JsonUtils;
import com.qingqiu.openagent.converter.ModelConverter;
import com.qingqiu.openagent.enums.BizExceptionEnum;
import com.qingqiu.openagent.exception.BizException;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.community.model.dashscope.WanxImageModel;
import dev.langchain4j.community.model.dashscope.WanxImageSize;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.output.Response;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 通义万相（DashScope wanx 系列）文生图工具。
 * <p>相较于免费的 Pollinations，万相在结构、中文语义与画面质量上一般更佳，适合付费场景。</p>
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
public class WanxGenImageTool implements ITool {

  /** 默认尺寸：DashScope wanx 系列使用 W*H 表示（注意中间是星号，不是 x）。 */
  private static final String DEFAULT_SIZE = "1024*1024";
  private final ModelConverter modelConverter;

  /** 文生图模型名，例如 wanx2.1-t2i-turbo / wanx2.1-t2i-plus / wanx-v1。 */
  @Value("${image-model.wanx.model-name}")
  private String imageModelName;

  /** DashScope API Key。可在 application-local.yaml 或环境变量 DASHSCOPE_API_KEY 中配置。 */
  @Value("${image-model.wanx.api-key}")
  private String apiKey;

  public WanxGenImageTool(ModelConverter modelConverter) {
    this.modelConverter = modelConverter;
  }

  @Override
  public String getDescription() {
    return "调用阿里云通义万相生成高质量图片，返回 markdown 图片语法。";
  }

  @Override
  public String getName() {
    return "wanxGenImageTool";
  }

  @Override
  public ToolType getType() {
    return ToolType.OPTIONAL;
  }

  @Tool(
      name = "wanxGenerateImage",
      value = "Generate a high-quality image with png. "
          + "Use this whenever the user asks to draw / paint / generate / produce an image, illustration, poster, "
          + "icon, avatar, or scene. Returns a Markdown image link `![alt](url)`. "
          + "IMPORTANT: in your final reply to the user, you MUST include the returned `![alt](url)` markdown VERBATIM "
          + "so the image is displayed; otherwise the user will only see text."
  )
  public String generateImage(
      @P(value = "Image description (prompt). 中英文均可，越具体越好；最多 800 个字符。")
      String prompt,
      @P(value = "image size, format must be `WIDTH*HEIGHT`! "
          + "Common: 1:1 is 1280*1280, 3:4 is 1104*1472, 4:3 is 1472*1104, 9:16 is 960*1696, 16:9 is 1696*960, 1280*720, 720*1280. Default 1280*1280.",
          required = false)
      String size
  ) {
    if (StrUtil.isBlank(prompt)) {
      return "Error: prompt cannot be empty.";
    }
    if (StrUtil.isBlank(apiKey)) {
      log.warn("[WanxGenImageTool] apiKey is blank; configure image-model.api-key or DASHSCOPE_API_KEY env var");
      return "图片生成失败：DashScope API Key 未配置，请在 application-local.yaml 设置 image-model.api-key 或环境变量 DASHSCOPE_API_KEY。";
    }
    // size 是可选参数，AI 不传时为 null/空，需要兜底；否则 WanxImageSize.of(null) 会 NPE。
    String chosenSize = StrUtil.isBlank(size) ? DEFAULT_SIZE : size.trim();

    ImageGenerationMessage message = ImageGenerationMessage.builder()
        .role("user")
        .content(Collections.singletonList(
            Collections.singletonMap("text",prompt)
        )).build();

    ImageGenerationParam param = ImageGenerationParam.builder()
        .apiKey(apiKey)
        .model(imageModelName)
        .n(1)
        .size(chosenSize)
        .negativePrompt("低分辨率，低画质，肢体畸形，手指畸形，画面过饱和，蜡像感，人脸无细节，过度光滑，画面具有AI感。构图混乱。文字模糊，扭曲。")
        .promptExtend(true)
        .watermark(false)
        .messages(Collections.singletonList(message))
        .build();

    ImageGeneration imageGeneration = new ImageGeneration();
    try {
      ImageGenerationResult result =  imageGeneration.call(param);
      log.info("[WanxGenImageTool] model={} size={} result={}", imageModelName, chosenSize, JsonUtils.toJson(result));
      ImageGenerationOutput output = result.getOutput();
      if(output == null){
        throw new BizException(BizExceptionEnum.REQUEST_ERROR.getCode(),BizExceptionEnum.REQUEST_ERROR.getMessage());
      }
      String imageUrl = output.getChoices().get(0).getMessage().getContent().get(0).get("image")
          .toString();

//      String md = "![" + prompt.trim() + "](" + imageUrl + ")";
//      return md
//          + "\n\nINSTRUCTION FOR THE ASSISTANT: copy the line above (the `![...](...)` markdown) "
//          + "verbatim into your final answer to the user, then add any extra description you like.";

      return imageUrl;
    } catch (Exception e) {
      log.error("[WanxGenImageTool] generate failed, model={} size={}", imageModelName, chosenSize, e);
      String msg = e.getMessage();
      return "图片生成异常：" + (StrUtil.isBlank(msg) ? e.getClass().getSimpleName() : msg);
    }
  }
}
