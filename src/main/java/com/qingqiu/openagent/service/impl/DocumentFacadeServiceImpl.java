package com.qingqiu.openagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.qingqiu.openagent.converter.DocumentConverter;
import com.qingqiu.openagent.enums.BizExceptionEnum;
import com.qingqiu.openagent.exception.BizException;
import com.qingqiu.openagent.mapper.ChunkBgeM3Mapper;
import com.qingqiu.openagent.mapper.DocumentMapper;
import com.qingqiu.openagent.mapper.KnowledgeBaseMapper;
import com.qingqiu.openagent.model.dto.DocumentDTO;
import com.qingqiu.openagent.model.entity.ChunkBgeM3;
import com.qingqiu.openagent.model.entity.Document;
import com.qingqiu.openagent.model.entity.KnowledgeBase;
import com.qingqiu.openagent.model.request.CreateDocumentRequest;
import com.qingqiu.openagent.model.request.UpdateDocumentRequest;
import com.qingqiu.openagent.model.response.CreateDocumentResponse;
import com.qingqiu.openagent.model.response.GetDocumentsResponse;
import com.qingqiu.openagent.model.vo.DocumentVO;
import com.qingqiu.openagent.service.DocumentFacadeService;
import com.qingqiu.openagent.service.DocumentStorageService;
import com.qingqiu.openagent.service.MarkdownParserService;
import com.qingqiu.openagent.service.RagService;
import com.qingqiu.openagent.util.UserContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author: qingqiugeek
 * @date: 2026/5/5 10:41
 * @description: DocumentFacade service implementation
 */

@Service
@AllArgsConstructor
@Slf4j
public class DocumentFacadeServiceImpl implements DocumentFacadeService {

    private final DocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentConverter documentConverter;
    private final DocumentStorageService documentStorageService;
    private final MarkdownParserService markdownParserService;
    private final RagService ragService;
    private final ChunkBgeM3Mapper chunkBgeM3Mapper;

    @Override
    public GetDocumentsResponse getDocuments() {
        Long userId = requireLoginUser();
        LambdaQueryWrapper<Document> qw = new LambdaQueryWrapper<>();
        qw.eq(Document::getUserId, userId).orderByDesc(Document::getUpdatedAt);
        return buildResponse(documentMapper.selectList(qw));
    }

    @Override
    public GetDocumentsResponse getDocumentsByKbId(String kbId) {
        requireOwnedKb(kbId);
        LambdaQueryWrapper<Document> qw = new LambdaQueryWrapper<>();
        qw.eq(Document::getKbId, kbId).orderByDesc(Document::getUpdatedAt);
        return buildResponse(documentMapper.selectList(qw));
    }

    @Override
    public CreateDocumentResponse createDocument(CreateDocumentRequest request) {
        Long userId = requireLoginUser();
        if (request == null || request.getKbId() == null) {
            throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(), "kbId 不能为空");
        }
        requireOwnedKb(request.getKbId());
        try {
            DocumentDTO dto = documentConverter.toDTO(request);
            dto.setUserId(userId);

            Document document = documentConverter.toEntity(dto);
            LocalDateTime now = LocalDateTime.now();
            document.setCreatedAt(now);
            document.setUpdatedAt(now);
            document.setIsDeleted(0);

            int rows = documentMapper.insert(document);
            if (rows <= 0) {
                throw new BizException(BizExceptionEnum.OPERATION_ERROR.getCode(), "创建文档失败");
            }
            return CreateDocumentResponse.builder()
                    .documentId(document.getId())
                    .build();
        } catch (JsonProcessingException e) {
            throw new BizException("创建文档时发生序列化错误: " + e.getMessage());
        }
    }

    @Override
    public CreateDocumentResponse uploadDocument(String kbId, MultipartFile file) {
        Long userId = requireLoginUser();
        requireOwnedKb(kbId);
        try {
            if (file.isEmpty()) {
                throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(), "上传的文件为空");
            }

            String originalFilename = file.getOriginalFilename();
            String filetype = getFileType(originalFilename);
            long fileSize = file.getSize();

            DocumentDTO documentDTO = DocumentDTO.builder()
                    .userId(userId)
                    .kbId(kbId)
                    .filename(originalFilename)
                    .filetype(filetype)
                    .size(fileSize)
                    .build();

            Document document = documentConverter.toEntity(documentDTO);
            LocalDateTime now = LocalDateTime.now();
            document.setCreatedAt(now);
            document.setUpdatedAt(now);
            document.setIsDeleted(0);

            int rows = documentMapper.insert(document);
            if (rows <= 0) {
                throw new BizException(BizExceptionEnum.OPERATION_ERROR.getCode(), "创建文档记录失败");
            }

            String documentId = document.getId();
            String filePath = documentStorageService.saveFile(kbId, documentId, file);

            DocumentDTO.MetaData metadata = new DocumentDTO.MetaData();
            metadata.setFilePath(filePath);
            documentDTO.setMetadata(metadata);
            documentDTO.setId(documentId);
            documentDTO.setCreatedAt(now);
            documentDTO.setUpdatedAt(now);

            Document updatedDocument = documentConverter.toEntity(documentDTO);
            updatedDocument.setId(documentId);
            updatedDocument.setCreatedAt(now);
            updatedDocument.setUpdatedAt(now);

            documentMapper.updateById(updatedDocument);
            log.info("文档上传成功: userId={}, kbId={}, documentId={}, filename={}",
                    userId, kbId, documentId, originalFilename);

            if ("md".equalsIgnoreCase(filetype) || "markdown".equalsIgnoreCase(filetype)) {
                processMarkdownDocument(kbId, documentId, filePath);
            } else {
                log.warn("待新增处理的文件类型: {}", filetype);
            }

            return CreateDocumentResponse.builder().documentId(documentId).build();
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BizException("文件保存失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteDocument(String documentId) {
        Document document = requireOwnedDocument(documentId);
        // 删文件
        try {
            DocumentDTO documentDTO = documentConverter.toDTO(document);
            if (documentDTO.getMetadata() != null && documentDTO.getMetadata().getFilePath() != null) {
                documentStorageService.deleteFile(documentDTO.getMetadata().getFilePath());
            }
        } catch (Exception e) {
            log.warn("删除文件失败，继续删除文档记录: documentId={}, error={}", documentId, e.getMessage());
        }
        int rows = documentMapper.deleteById(documentId);
        if (rows <= 0) {
            throw new BizException(BizExceptionEnum.OPERATION_ERROR.getCode(), "删除文档失败");
        }
    }

    @Override
    public void updateDocument(String documentId, UpdateDocumentRequest request) {
        Document existing = requireOwnedDocument(documentId);
        try {
            DocumentDTO dto = documentConverter.toDTO(existing);
            documentConverter.updateDTOFromRequest(dto, request);

            Document updated = documentConverter.toEntity(dto);
            updated.setId(existing.getId());
            updated.setUserId(existing.getUserId());
            updated.setKbId(existing.getKbId());
            updated.setCreatedAt(existing.getCreatedAt());
            updated.setUpdatedAt(LocalDateTime.now());

            int rows = documentMapper.updateById(updated);
            if (rows <= 0) {
                throw new BizException(BizExceptionEnum.OPERATION_ERROR.getCode(), "更新文档失败");
            }
        } catch (JsonProcessingException e) {
            throw new BizException("更新文档时发生序列化错误: " + e.getMessage());
        }
    }

    private void processMarkdownDocument(String kbId, String documentId, String filePath) {
        try {
            log.info("开始处理 Markdown 文档: kbId={}, documentId={}, filePath={}", kbId, documentId, filePath);
            Path path = documentStorageService.getFilePath(filePath);
            try (InputStream inputStream = Files.newInputStream(path)) {
                List<MarkdownParserService.MarkdownSection> sections = markdownParserService.parseMarkdown(inputStream);
                if (sections.isEmpty()) {
                    log.warn("Markdown 文档解析后没有找到任何章节: documentId={}", documentId);
                    return;
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
                log.info("Markdown 文档处理完成: documentId={}, 共生成 {} 个 chunks", documentId, chunkCount);
            }
        } catch (Exception e) {
            log.error("处理 Markdown 文档失败: documentId={}", documentId, e);
        }
    }

    private String getFileType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private Long requireLoginUser() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BizException(BizExceptionEnum.NOT_LOGIN_ERROR.getCode(),
                    BizExceptionEnum.NOT_LOGIN_ERROR.getMessage());
        }
        return userId;
    }

    private void requireOwnedKb(String kbId) {
        if (kbId == null || kbId.isBlank()) {
            throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(), "kbId 不能为空");
        }
        Long userId = requireLoginUser();
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            throw new BizException(BizExceptionEnum.NOT_FOUND_ERROR.getCode(), "知识库不存在: " + kbId);
        }
        if (!userId.equals(kb.getUserId())) {
            throw new BizException(BizExceptionEnum.FORBIDDEN_ERROR.getCode(),
                    BizExceptionEnum.FORBIDDEN_ERROR.getMessage());
        }
    }

    private Document requireOwnedDocument(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            throw new BizException(BizExceptionEnum.PARAMS_ERROR.getCode(), "documentId 不能为空");
        }
        Long userId = requireLoginUser();
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BizException(BizExceptionEnum.NOT_FOUND_ERROR.getCode(), "文档不存在: " + documentId);
        }
        if (!userId.equals(document.getUserId())) {
            throw new BizException(BizExceptionEnum.FORBIDDEN_ERROR.getCode(),
                    BizExceptionEnum.FORBIDDEN_ERROR.getMessage());
        }
        return document;
    }

    private GetDocumentsResponse buildResponse(List<Document> documents) {
        List<DocumentVO> result = new ArrayList<>();
        for (Document document : documents) {
            try {
                result.add(documentConverter.toVO(document));
            } catch (JsonProcessingException e) {
                throw new BizException("解析文档失败: " + e.getMessage());
            }
        }
        return GetDocumentsResponse.builder()
                .documents(result.toArray(new DocumentVO[0]))
                .build();
    }
}
