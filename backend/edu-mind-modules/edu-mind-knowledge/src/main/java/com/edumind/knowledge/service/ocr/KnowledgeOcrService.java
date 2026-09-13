package com.edumind.knowledge.service.ocr;

import com.edumind.knowledge.vo.ocr.KnowledgeOcrPageVO;
import com.edumind.knowledge.vo.ocr.KnowledgeOcrTaskVO;

import java.util.List;

public interface KnowledgeOcrService {

    KnowledgeOcrTaskVO createOcrTask(Long documentId, String engine);

    KnowledgeOcrTaskVO getTaskStatus(Long taskId);

    List<KnowledgeOcrPageVO> getTaskPages(Long taskId);

    void updatePageText(Long pageId, String proofreadText);

    void confirmAndIngest(Long taskId);
}
