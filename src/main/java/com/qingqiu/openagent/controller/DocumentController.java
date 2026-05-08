package com.qingqiu.openagent.controller;

import com.qingqiu.openagent.model.common.R;
import com.qingqiu.openagent.model.request.CreateDocumentRequest;
import com.qingqiu.openagent.model.request.UpdateDocumentRequest;
import com.qingqiu.openagent.model.response.CreateDocumentResponse;
import com.qingqiu.openagent.model.response.GetDocumentsResponse;
import com.qingqiu.openagent.service.DocumentFacadeService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author: qingqiugeek
 * @date: 2026/5/11 13:18
 * @description: Document controller
 */

@RestController
@RequestMapping("/api/documents")
@AllArgsConstructor
public class DocumentController {

    private final DocumentFacadeService documentFacadeService;

    // 查询所有文档
    @GetMapping
    public R<GetDocumentsResponse> getDocuments() {
        return R.success(documentFacadeService.getDocuments());
    }

    // 根据 kbId 查询文档
    @GetMapping("/kb/{kbId}")
    public R<GetDocumentsResponse> getDocumentsByKbId(@PathVariable String kbId) {
        return R.success(documentFacadeService.getDocumentsByKbId(kbId));
    }

    // 创建文档（仅创建记录，不上传文件）
    @PostMapping
    public R<CreateDocumentResponse> createDocument(@RequestBody CreateDocumentRequest request) {
        return R.success(documentFacadeService.createDocument(request));
    }

    // 上传文档（上传文件并创建记录）
    @PostMapping("/upload")
    public R<CreateDocumentResponse> uploadDocument(
            @RequestParam("kbId") String kbId,
            @RequestParam("file") MultipartFile file) {
        return R.success(documentFacadeService.uploadDocument(kbId, file));
    }

    // 删除文档
    @DeleteMapping("/{documentId}")
    public R<Boolean> deleteDocument(@PathVariable String documentId) {
        documentFacadeService.deleteDocument(documentId);
        return R.success(true);
    }

    // 更新文档
    @PatchMapping("/{documentId}")
    public R<Boolean> updateDocument(@PathVariable String documentId, @RequestBody UpdateDocumentRequest request) {
        documentFacadeService.updateDocument(documentId, request);
        return R.success(true);
    }
}
