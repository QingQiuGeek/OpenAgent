package com.qingqiu.openagent.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: qingqiugeek
 * @description: 弹性文本分段策略，用于非 Markdown 的文档（pdf/docx/code/txt …）。
 *
 * 多层级回退策略：
 *   1. 优先按"段落"（连续空行）切分；
 *   2. 段落超过 maxChars 时退化按句子（中英文标点）切；
 *   3. 句子仍超长则按定长滑动窗口切（带 overlap 保留上下文）；
 *   4. 短段落贪心合并到接近 maxChars，避免 chunk 过碎。
 *
 * TODO: 1. 当前所有非 Markdown 文档共用同一套切分策略，后续应按文档类型定制：
 *          - PDF：优先按页面边界切分，保留页码信息
 *          - 代码文件（.java/.py/.js）：按函数/类/方法边界切分，保留语义完整性
 *          - Excel/CSV：按行组切分，保留表头作为上下文前缀
 *          - 长文本：当前策略基本够用，可微调 maxChars/overlap 参数
 *       2. 抽象 ChunkStrategy 接口（如 List<String> chunk(String text, ChunkContext ctx)），
 *          TextChunker 退化为默认实现，由 DocumentVectorizationService 按 filetype 选择策略。
 *       3. overlap 目前是固定 80 字符，可考虑按语义边界（句子开头）对齐重叠区域，
 *          避免在句子中间截断导致语义碎片化。
 */
@Service
public class TextChunker {

    /** 单个 chunk 目标字符上限（粗略对齐 bge-m3 的安全输入长度）。 */
    public static final int DEFAULT_MAX_CHARS = 800;
    /** 相邻 chunk 重叠字符数，便于跨段语义检索。 */
    public static final int DEFAULT_OVERLAP = 80;

    /** 按双换行（含 \r）切段；同时丢弃零长串。 */
    private static final Pattern PARAGRAPH = Pattern.compile("\\n{2,}|\\r\\n{2,}");
    /** 句子分隔符：中英文句号 / 问号 / 感叹号 / 分号 / 单换行。 */
    private static final Pattern SENTENCE = Pattern.compile(
            "(?<=[。！？!?；;\\n])"
    );

    public List<String> chunk(String text) {
        return chunk(text, DEFAULT_MAX_CHARS, DEFAULT_OVERLAP);
    }

    public List<String> chunk(String text, int maxChars, int overlap) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        String norm = text.replace("\r\n", "\n").trim();
        if (norm.isEmpty()) return out;
        if (maxChars <= 0) maxChars = DEFAULT_MAX_CHARS;
        if (overlap < 0) overlap = 0;
        if (overlap >= maxChars) overlap = maxChars / 5;

        StringBuilder buf = new StringBuilder();
        for (String paragraph : PARAGRAPH.split(norm)) {
            String p = paragraph.trim();
            if (p.isEmpty()) continue;

            if (p.length() > maxChars) {
                // 先 flush 缓冲区
                flush(buf, out);
                // 段落过长：按句子切，再按定长切
                for (String sent : splitBySentence(p, maxChars)) {
                    if (sent.length() > maxChars) {
                        out.addAll(splitByLength(sent, maxChars, overlap));
                    } else {
                        appendOrFlush(buf, sent, maxChars, out);
                    }
                }
                flush(buf, out);
                continue;
            }
            appendOrFlush(buf, p, maxChars, out);
        }
        flush(buf, out);
        return out;
    }

    /** 把 piece 追加到 buf；超过容量则先把 buf 输出再放入。 */
    private void appendOrFlush(StringBuilder buf, String piece, int maxChars,
                               List<String> out) {
        if (buf.length() == 0) {
            buf.append(piece);
            return;
        }
        if (buf.length() + 2 + piece.length() > maxChars) {
            flush(buf, out);
            buf.append(piece);
        } else {
            buf.append("\n\n").append(piece);
        }
    }

    private void flush(StringBuilder buf, List<String> out) {
        if (buf.length() == 0) return;
        out.add(buf.toString().trim());
        buf.setLength(0);
    }

    /** 用句子分隔符把段落切成若干小片，保留分隔符。短句仍会贪心合并到 ≤ maxChars。 */
    private List<String> splitBySentence(String paragraph, int maxChars) {
        List<String> result = new ArrayList<>();
        Matcher m = SENTENCE.matcher(paragraph);
        int last = 0;
        StringBuilder cur = new StringBuilder();
        while (m.find()) {
            String s = paragraph.substring(last, m.start()).trim();
            last = m.start();
            if (s.isEmpty()) continue;
            if (cur.length() + s.length() + 1 > maxChars) {
                if (cur.length() > 0) {
                    result.add(cur.toString());
                    cur.setLength(0);
                }
            }
            if (cur.length() > 0) cur.append(" ");
            cur.append(s);
        }
        // 尾巴
        String tail = paragraph.substring(last).trim();
        if (!tail.isEmpty()) {
            if (cur.length() + tail.length() + 1 > maxChars && cur.length() > 0) {
                result.add(cur.toString());
                cur.setLength(0);
            }
            if (cur.length() > 0) cur.append(" ");
            cur.append(tail);
        }
        if (cur.length() > 0) result.add(cur.toString());
        return result;
    }

    /** 兜底：纯按字符长度切，相邻块保留 overlap 字符。 */
    private List<String> splitByLength(String text, int maxChars, int overlap) {
        List<String> result = new ArrayList<>();
        int step = Math.max(1, maxChars - overlap);
        for (int i = 0; i < text.length(); i += step) {
            int end = Math.min(text.length(), i + maxChars);
            result.add(text.substring(i, end));
            if (end == text.length()) break;
        }
        return result;
    }
}
