package com.qingqiu.openagent.agent.guardrail;


import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * @author: qingqiugeek
 * @date: 2026/5/2 13:12
 * @description: SensitiveWordsInputGuardrail
 */
@Slf4j
@Component
public class SensitiveWordsInputGuardrail implements InputGuardrail {

  private static final Set<String> sensitiveWords;

  // 静态初始化块，用于加载敏感词
  static {
    sensitiveWords = loadSensitiveWords();
    log.info("敏感词加载成功！");
  }

  /**
   * 检测用户输入是否安全。
   * <p>采用「子串包含」判定而非分词相等：中文之间没有 \\W 分隔符，
   * 原先的 split("\\W+") 会把整段中文当成一个 token，导致 "恐怖分子很危险" 这种长句无法命中
   * "恐怖分子"。改为遍历词库，任何敏感词作为子串出现即拦截。
   */
  @Override
  public InputGuardrailResult validate(UserMessage userMessage) {
    if (sensitiveWords.isEmpty()) {
      return success();
    }
    String inputText = userMessage.singleText().toLowerCase();
    for (String word : sensitiveWords) {
      if (word == null || word.isEmpty()) continue;
      if (inputText.contains(word)) {
        return fatal("触发敏感词: " + word);
      }
    }
    return success();
  }

  /**
   * 从 classpath 加载敏感词。
   * <p>用 Spring 的 {@link PathMatchingResourcePatternResolver} 扫描
   * {@code classpath*:sensitiveLexicon/*.txt}，同时支持开发环境的 file: 协议
   * 和打包后 jar: 协议，避免上线后词库丢失。每个文件按行读取，每行一词。
   * 空行 / null 行会被跳过，统一 trim + toLowerCase。
   */
  private static Set<String> loadSensitiveWords() {
    Set<String> words = new HashSet<>();
    try {
      Resource[] resources = new PathMatchingResourcePatternResolver()
          .getResources("classpath*:sensitiveLexicon/*.txt");
      for (Resource resource : resources) {
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
          String line;
          while ((line = reader.readLine()) != null) {
            String word = line.trim().toLowerCase();
            if (!word.isEmpty()) {
              words.add(word);
            }
          }
        } catch (IOException e) {
          log.warn("[SensitiveWords] 读取词库失败: {}", resource.getDescription(), e);
        }
      }
      log.info("[SensitiveWords] 共加载 {} 个敏感词，来自 {} 个文件",
          words.size(), resources.length);
    } catch (IOException e) {
      log.warn("[SensitiveWords] 扫描词库目录失败", e);
    }
    return words;
  }

  /** 测试 / 调试可用：当前词库大小。 */
  public static int wordCount() {
    return sensitiveWords.size();
  }
}
