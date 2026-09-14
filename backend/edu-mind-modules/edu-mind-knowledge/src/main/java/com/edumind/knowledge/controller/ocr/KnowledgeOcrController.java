package com.edumind.knowledge.controller.ocr;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.knowledge.dto.ocr.OcrPageUpdateDTO;
import com.edumind.knowledge.dto.ocr.OcrTaskCreateDTO;
import com.edumind.knowledge.service.ocr.KnowledgeOcrService;
import com.edumind.knowledge.vo.ocr.KnowledgeOcrPageVO;
import com.edumind.knowledge.vo.ocr.KnowledgeOcrTaskVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识库 OCR 识别与校对任务控制器
 */
@RestController
@RequestMapping("/api/knowledge/ocr-tasks")
@RequiredArgsConstructor
@SaCheckLogin
@SaCheckPermission("knowledge:ocr:use")
public class KnowledgeOcrController {

    private final KnowledgeOcrService knowledgeOcrService;

    @PostMapping
    public ApiResult<KnowledgeOcrTaskVO> createOcrTask(@Valid @RequestBody OcrTaskCreateDTO dto) {
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
    public ApiResult<Void> updatePageText(@PathVariable("pageId") Long pageId, @RequestBody OcrPageUpdateDTO dto) {
        knowledgeOcrService.updatePageText(pageId, dto.getProofreadText());
        return ApiResult.success();
    }

    @PostMapping("/{taskId}/confirm")
    public ApiResult<Void> confirmAndIngest(@PathVariable("taskId") Long taskId) {
        knowledgeOcrService.confirmAndIngest(taskId);
        return ApiResult.success();
    }
}
