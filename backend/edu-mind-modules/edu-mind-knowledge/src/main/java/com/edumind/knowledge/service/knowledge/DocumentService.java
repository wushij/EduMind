package com.edumind.knowledge.service.knowledge;

import com.edumind.knowledge.vo.knowledge.KnowledgeDocumentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    KnowledgeDocumentVO upload(Long knowledgeBaseId, MultipartFile file);

    List<KnowledgeDocumentVO> list(Long knowledgeBaseId);

    void delete(Long documentId);

    void triggerParse(Long documentId);

    KnowledgeDocumentVO getById(Long documentId);
}
