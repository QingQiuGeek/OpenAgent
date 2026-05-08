package com.qingqiu.openagent.agent.guardrail;


import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
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
   * 检测用户输入是否安全
   */
  @Override
  public InputGuardrailResult validate(UserMessage userMessage) {
    // 获取用户输入并转换为小写以确保大小写不敏感
    String inputText = userMessage.singleText().toLowerCase();
    // 使用正则表达式分割输入文本为单词
    String[] words = inputText.split("\\W+");
    // 遍历所有单词，检查是否存在敏感词
    for (String word : words) {
      if (sensitiveWords.contains(word)) {
        return fatal("触发敏感词: "+ word);
      }
    }
    return success();
  }

  /**
   * 从资源文件中加载敏感词
   */
  private static Set<String> loadSensitiveWords() {
    Set<String> words = new HashSet<>();
    URL directoryUrl = SensitiveWordsInputGuardrail.class.getClassLoader()
        .getResource("sensitiveLexicon");
    if (directoryUrl != null) {
      // 如果资源在文件系统中
      if (directoryUrl.getProtocol().equals("file")) {
        File directory = null;
        try {
          directory = new File(directoryUrl.toURI());
        } catch (URISyntaxException e) {
          throw new RuntimeException(e);
        }
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".txt"));
        if (files != null) {
          for (File file : files) {
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
              String line;
              while (true) {
                if ((line = reader.readLine()) == null) {
                  break;
                }
                words.add(line.trim().toLowerCase());
              }
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        }
      }

//    try (InputStream inputStream = SensitiveWordsInputGuardrail.class.getClassLoader().getResourceAsStream("sensitiveLexicon/xxx.txt");
//        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
//      String line;
//      while ((line = reader.readLine()) != null) {
//        words.add(line.trim().toLowerCase());
//      }
//    } catch (Exception e) {
//      throw new RuntimeException("Failed to load sensitive words from resource file", e);
//    }

    }
    return words;
  }
}
