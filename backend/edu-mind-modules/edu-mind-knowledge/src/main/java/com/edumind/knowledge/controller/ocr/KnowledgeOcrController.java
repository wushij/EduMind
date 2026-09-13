package com.edumind.knowledge.controller.ocr;

import com.edumind.common.api.ApiResult;
import com.edumind.knowledge.service.ocr.KnowledgeOcrService;
import com.edumind.knowledge.vo.ocr.KnowledgeOcrPageVO;
import com.edumind.knowledge.vo.ocr.KnowledgeOcrTaskVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge/ocr-tasks")
@RequiredArgsConstructor
public class KnowledgeOcrController {

    private final KnowledgeOcrService knowledgeOcrService;

    @PostMapping
    public ApiResult<KnowledgeOcrTaskVO> createOcrTask(@RequestBody OcrTaskCreateDTO dto) {
        return ApiResult.success(knowledgeOcrService.createOcrTask(dto.getDocumentId(), dto.getEngine()));
    }

    @GetMapping("/{taskId}")
    public ApiResult<KnowledgeOcrTaskVO> getTaskStatus(@PathVariable("taskId") Long taskId) {
        return ApiResult.success(knowledgeOcrService.getTaskStatus(taskId));
    }

    @GetMapping("/{taskId}/pages")
    public ApiResult<List<KnowledgeOcrPageVO>> getTaskPages(@PathVariable("taskId") Long taskId) {
        return ApiResult.success(knowledgeOcrService.getTaskPages(taskId));
    }

    @PutMapping("/pages/{pageId}")
    public ApiResult<Void> updatePageText(@PathVariable("pageId") Long pageId, @RequestBody PageUpdateDTO dto) {
        knowledgeOcrService.updatePageText(pageId, dto.getProofreadText());
        return ApiResult.success();
    }

    @PostMapping("/{taskId}/confirm")
    public ApiResult<Void> confirmAndIngest(@PathVariable("taskId") Long taskId) {
        knowledgeOcrService.confirmAndIngest(taskId);
        return ApiResult.success();
    }

    @Data
    public static class OcrTaskCreateDTO {
        private Long documentId;
        private String engine;
    }

    @Data
    public static class PageUpdateDTO {
        private String proofreadText;
    }
}
