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
import java.util.ArrayList;
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
 *
 * TODO: 1. metadata 字段当前写入 null，后续应记录 chunk 元数据（文档文件名、文件类型、
 *          chunk 在原文中的页码/段落序号、切分层级等），便于检索后的引用溯源。
 *       2. 不同格式文档应制定不同的切块策略（如 PDF 按页、代码按函数/类、表格按行），
 *          抽象 ChunkStrategy 接口，通过工厂按 filetype 分发，替换当前 if-else 硬编码。
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

    /** Markdown：按标题层级切段，批量 embedding，批量入库。 */
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

            // 过滤有效章节，收集标题用于批量 embedding
            List<MarkdownParserService.MarkdownSection> validSections = sections.stream()
                    .filter(s -> s.getTitle() != null && !s.getTitle().trim().isEmpty())
                    .toList();
            if (validSections.isEmpty()) {
                log.warn("Markdown 文档没有有效章节: documentId={}", documentId);
                return 0;
            }

            List<String> titles = validSections.stream()
                    .map(MarkdownParserService.MarkdownSection::getTitle)
                    .toList();

            // 批量 Embedding
            List<float[]> embeddings = ragService.batchEmbed(titles);

            // 构建 chunk 实体列表
            LocalDateTime now = LocalDateTime.now();
            List<ChunkBgeM3> chunkEntities = new ArrayList<>();
            for (int i = 0; i < validSections.size(); i++) {
                MarkdownParserService.MarkdownSection section = validSections.get(i);
                String content = section.getContent();
                chunkEntities.add(ChunkBgeM3.builder()
                        .kbId(kbId)
                        .docId(documentId)
                        .content(content != null ? content : "")
                        .embedding(embeddings.get(i))
                        .createdAt(now)
                        .updatedAt(now)
                        .build());
            }

            // 批量插入
            int inserted = chunkBgeM3Mapper.batchInsert(chunkEntities);
            log.info("批量插入 Markdown chunks: documentId={}, count={}", documentId, inserted);
            return inserted;
        }
    }

    /** 通用文档：Tika 提取文本 → 弹性分段 → 批量 embedding → 批量入库。 */
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
        List<String> chunks = textChunker.chunk(text).stream()
                .filter(t -> t != null && !t.isBlank())
                .toList();
        if (chunks.isEmpty()) {
            log.warn("分段后没有任何 chunk: documentId={}", documentId);
            return 0;
        }

        // 批量 Embedding（内部自动分批，每批 ≤10 条）
        List<float[]> embeddings = ragService.batchEmbed(chunks);

        // 构建 chunk 实体列表
        LocalDateTime now = LocalDateTime.now();
        List<ChunkBgeM3> chunkEntities = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            chunkEntities.add(ChunkBgeM3.builder()
                    .kbId(kbId)
                    .docId(documentId)
                    .content(chunks.get(i))
                    .embedding(embeddings.get(i))
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        }

        // 批量插入
        int inserted = chunkBgeM3Mapper.batchInsert(chunkEntities);
        log.info("批量插入 chunks: documentId={}, count={}", documentId, inserted);
        return inserted;
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
