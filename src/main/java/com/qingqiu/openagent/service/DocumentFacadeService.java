package com.qingqiu.openagent.service;

import com.qingqiu.openagent.model.request.CreateDocumentRequest;
import com.qingqiu.openagent.model.request.UpdateDocumentRequest;
import com.qingqiu.openagent.model.response.CreateDocumentResponse;
import com.qingqiu.openagent.model.response.GetDocumentsResponse;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentFacadeService {
    GetDocumentsResponse getDocuments();

    GetDocumentsResponse getDocumentsByKbId(String kbId);

    CreateDocumentResponse createDocument(CreateDocumentRequest request);

    CreateDocumentResponse uploadDocument(String kbId, MultipartFile file);

    void deleteDocument(String documentId);

    void updateDocument(String documentId, UpdateDocumentRequest request);
}
