package com.qingqiu.openagent.service.impl;

import com.qingqiu.openagent.mapper.ChunkBgeM3Mapper;
import com.qingqiu.openagent.mapper.DocumentMapper;
import com.qingqiu.openagent.model.entity.ChunkBgeM3;
import com.qingqiu.openagent.model.entity.Document;
import com.qingqiu.openagent.service.DocumentStorageService;
import com.qingqiu.openagent.service.MarkdownParserService;
import com.qingqiu.openagent.service.RagService;
import com.qingqiu.openagent.service.TextChunker;
import com.qingqiu.openagent.service.TikaTextExtractor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: qingqiugeek
 * @description: 文档异步向量化服务（独立 bean，确保 @Async 走代理生效）。
 *
 * 入口 {@link #processAsync} 根据 filetype 分发：
 *   - md → MarkdownParserService 按标题层级切段（每段 title 做 embedding，正文做 content）
 *   - 其他文档（pdf/docx/xlsx/code/txt …）→ TikaTextExtractor 提取纯文本 + TextChunker 弹性分段
 *
 * 入向量库的字段：chunk_bge_m3(kb_id, doc_id, content, embedding VECTOR(1024))
 */
@Service
@AllArgsConstructor
@Slf4j
public class DocumentVectorizationService {

    private final DocumentMapper documentMapper;
    private final ChunkBgeM3Mapper chunkBgeM3Mapper;
    private final DocumentStorageService documentStorageService;
    private final MarkdownParserService markdownParserService;
    private final RagService ragService;
    private final TikaTextExtractor tikaTextExtractor;
    private final TextChunker textChunker;

    /**
     * 异步执行向量化；成功 → status=done，失败 → status=failed + error_msg。
     */
    @Async
    public void processAsync(String kbId, String documentId, String filePath, String filetype) {
        try {
            String type = filetype == null ? "" : filetype.toLowerCase();
            int chunkCount;
            if ("md".equals(type) || "markdown".equals(type)) {
                chunkCount = doProcessMarkdown(kbId, documentId, filePath);
            } else {
                chunkCount = doProcessGeneric(kbId, documentId, filePath);
            }
            log.info("向量化完成: documentId={}, type={}, chunks={}", documentId, type, chunkCount);
            updateStatus(documentId, "done", null);
        } catch (Exception e) {
            log.error("异步向量化失败: documentId={}", documentId, e);
            updateStatus(documentId, "failed", truncate(e.getMessage(), 1000));
        }
    }

    /** Markdown：按标题层级切段，每段 title 做 embedding，正文存 content。 */
    private int doProcessMarkdown(String kbId, String documentId, String filePath) throws Exception {
        log.info("开始处理 Markdown 文档: kbId={}, documentId={}", kbId, documentId);
        Path path = documentStorageService.getFilePath(filePath);
        try (InputStream inputStream = Files.newInputStream(path)) {
            List<MarkdownParserService.MarkdownSection> sections =
                    markdownParserService.parseMarkdown(inputStream);
            if (sections.isEmpty()) {
                log.warn("Markdown 文档解析后没有任何章节: documentId={}", documentId);
                return 0;
            }
            LocalDateTime now = LocalDateTime.now();
            int chunkCount = 0;
            for (MarkdownParserService.MarkdownSection section : sections) {
                String title = section.getTitle();
                String content = section.getContent();
                if (title == null || title.trim().isEmpty()) {
                    continue;
                }
                float[] embedding = ragService.embed(title);
                ChunkBgeM3 chunk = ChunkBgeM3.builder()
                        .kbId(kbId)
                        .docId(documentId)
                        .content(content != null ? content : "")
                        .metadata(null)
                        .embedding(embedding)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                if (chunkBgeM3Mapper.insert(chunk) > 0) {
                    chunkCount++;
                }
            }
            return chunkCount;
        }
    }

    /** 通用文档：Tika 提取文本 → 弹性分段 → 每段 chunk 做 embedding 入库。 */
    private int doProcessGeneric(String kbId, String documentId, String filePath) throws Exception {
        log.info("开始处理通用文档: kbId={}, documentId={}, filePath={}", kbId, documentId, filePath);
        Path path = documentStorageService.getFilePath(filePath);
        String text;
        try (InputStream inputStream = Files.newInputStream(path)) {
            text = tikaTextExtractor.extract(inputStream);
        }
        if (text == null || text.trim().isEmpty()) {
            log.warn("文档抽取后内容为空: documentId={}", documentId);
            return 0;
        }
        List<String> chunks = textChunker.chunk(text);
        if (chunks.isEmpty()) {
            log.warn("分段后没有任何 chunk: documentId={}", documentId);
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        int chunkCount = 0;
        for (String content : chunks) {
            if (content == null || content.isBlank()) continue;
            float[] embedding = ragService.embed(content);
            ChunkBgeM3 chunk = ChunkBgeM3.builder()
                    .kbId(kbId)
                    .docId(documentId)
                    .content(content)
                    .metadata(null)
                    .embedding(embedding)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            if (chunkBgeM3Mapper.insert(chunk) > 0) {
                chunkCount++;
            }
        }
        return chunkCount;
    }

    private void updateStatus(String documentId, String status, String errorMsg) {
        Document update = new Document();
        update.setId(documentId);
        update.setStatus(status);
        update.setErrorMsg(errorMsg);
        update.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(update);
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
