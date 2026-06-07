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
import com.qingqiu.openagent.model.entity.Document;
import com.qingqiu.openagent.model.entity.KnowledgeBase;
import com.qingqiu.openagent.model.request.CreateDocumentRequest;
import com.qingqiu.openagent.model.request.UpdateDocumentRequest;
import com.qingqiu.openagent.model.response.CreateDocumentResponse;
import com.qingqiu.openagent.model.response.GetDocumentsResponse;
import com.qingqiu.openagent.model.vo.DocumentVO;
import com.qingqiu.openagent.service.DocumentFacadeService;
import com.qingqiu.openagent.service.DocumentStorageService;
import com.qingqiu.openagent.util.UserContext;
import java.io.IOException;
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
    private final ChunkBgeM3Mapper chunkBgeM3Mapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentConverter documentConverter;
    private final DocumentStorageService documentStorageService;
    private final DocumentVectorizationService documentVectorizationService;

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

            // 计算文件内容 SHA-256 摘要，用于变更检测
            String contentHash = cn.hutool.crypto.digest.DigestUtil.sha256Hex(file.getInputStream());

            // 检查同知识库下是否已存在同名文档（支持覆盖更新）
            LambdaQueryWrapper<Document> existQw = new LambdaQueryWrapper<>();
            existQw.eq(Document::getKbId, kbId)
                    .eq(Document::getFilename, originalFilename)
                    .eq(Document::getUserId, userId)
                    .eq(Document::getIsDeleted, 0);
            Document existingDoc = documentMapper.selectOne(existQw);

            LocalDateTime now = LocalDateTime.now();
            String documentId;
            boolean isUpdate = existingDoc != null;

            // 内容 hash 变更检测：同名文件内容未变则跳过向量化
            if (isUpdate && contentHash.equals(existingDoc.getContentHash())) {
                log.info("文档内容未变化，跳过向量化: documentId={}, filename={}", existingDoc.getId(), originalFilename);
                return CreateDocumentResponse.builder().documentId(existingDoc.getId()).build();
            }

            if (isUpdate) {
                // 覆盖更新：先清理旧 chunks，再复用已有文档记录
                documentId = existingDoc.getId();
                int deletedChunks = chunkBgeM3Mapper.deleteByDocId(documentId);
                log.info("覆盖更新文档，已清理旧 chunks: documentId={}, deletedChunks={}", documentId, deletedChunks);

                // 删除旧文件
                try {
                    DocumentDTO oldDto = documentConverter.toDTO(existingDoc);
                    if (oldDto.getMetadata() != null && oldDto.getMetadata().getFilePath() != null) {
                        documentStorageService.deleteFile(oldDto.getMetadata().getFilePath());
                    }
                } catch (Exception e) {
                    log.warn("删除旧文件失败，继续更新: documentId={}, error={}", documentId, e.getMessage());
                }

                // 更新文档记录
                existingDoc.setFiletype(filetype);
                existingDoc.setSize(fileSize);
                existingDoc.setContentHash(contentHash);
                existingDoc.setStatus("uploading");
                existingDoc.setErrorMsg(null);
                existingDoc.setUpdatedAt(now);
                documentMapper.updateById(existingDoc);
            } else {
                // 新建文档记录
                DocumentDTO documentDTO = DocumentDTO.builder()
                        .userId(userId)
                        .kbId(kbId)
                        .filename(originalFilename)
                        .filetype(filetype)
                        .size(fileSize)
                        .build();

                Document document = documentConverter.toEntity(documentDTO);
                document.setCreatedAt(now);
                document.setUpdatedAt(now);
                document.setIsDeleted(0);
                document.setContentHash(contentHash);
                document.setStatus("uploading");

                int rows = documentMapper.insert(document);
                if (rows <= 0) {
                    throw new BizException(BizExceptionEnum.OPERATION_ERROR.getCode(), "创建文档记录失败");
                }
                documentId = document.getId();
            }

            // 保存文件到 OSS / 本地
            String filePath = documentStorageService.saveFile(kbId, documentId, file);

            // 写入 metadata.filePath，把状态切到「向量化中」/「已完成（跳过）」
            DocumentDTO.MetaData metadata = new DocumentDTO.MetaData();
            metadata.setFilePath(filePath);

            boolean isImage = isImageType(filetype);
            boolean needVectorize = !isImage;

            Document statusUpdate = new Document();
            statusUpdate.setId(documentId);
            statusUpdate.setStatus(needVectorize ? "vectorizing" : "skipped");
            statusUpdate.setUpdatedAt(LocalDateTime.now());
            // 通过 metadata JSON 字符串写入 filePath
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                statusUpdate.setMetadata(om.writeValueAsString(metadata));
            } catch (Exception e) {
                log.warn("序列化 metadata 失败: {}", e.getMessage());
            }
            documentMapper.updateById(statusUpdate);

            log.info("文档{}成功: userId={}, kbId={}, documentId={}, filename={}, status={}",
                    isUpdate ? "覆盖更新" : "上传", userId, kbId, documentId, originalFilename,
                    needVectorize ? "vectorizing" : "skipped");

            // 异步向量化（md 走标题切段，其他走 Tika + 弹性分段），失败回写状态
            if (needVectorize) {
                documentVectorizationService.processAsync(kbId, documentId, filePath, filetype);
            } else {
                log.warn("图片类型已跳过向量化: filetype={}", filetype);
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
        // 清理该文档对应的所有 chunks，避免"僵尸数据"污染检索结果
        int deletedChunks = chunkBgeM3Mapper.deleteByDocId(documentId);
        log.info("已清理文档关联 chunks: documentId={}, deletedChunks={}", documentId, deletedChunks);
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

    private static boolean isImageType(String filetype) {
        if (filetype == null) return false;
        switch (filetype.toLowerCase()) {
            case "png":
            case "jpg":
            case "jpeg":
            case "gif":
            case "webp":
            case "bmp":
            case "svg":
                return true;
            default:
                return false;
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
