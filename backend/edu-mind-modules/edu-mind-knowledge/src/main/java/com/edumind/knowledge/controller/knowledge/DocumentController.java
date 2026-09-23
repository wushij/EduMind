package com.edumind.knowledge.controller.knowledge;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.knowledge.service.knowledge.DocumentPipelineService;
import com.edumind.knowledge.service.knowledge.DocumentService;
import com.edumind.knowledge.vo.knowledge.KnowledgeDocumentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases/{knowledgeBaseId}/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentPipelineService documentPipelineService;

    @SaCheckPermission("knowledge:edit")
    @PostMapping
    public ApiResult<KnowledgeDocumentVO> upload(
            @PathVariable("knowledgeBaseId") Long knowledgeBaseId,
            @RequestParam("file") MultipartFile file) {
        KnowledgeDocumentVO document = documentService.upload(knowledgeBaseId, file);
        documentPipelineService.requestPipeline(document.getId());
        return ApiResult.success(document);
    }

    @SaCheckPermission("knowledge:view")
    @GetMapping
    public ApiResult<List<KnowledgeDocumentVO>> list(@PathVariable("knowledgeBaseId") Long knowledgeBaseId) {
        return ApiResult.success(documentService.list(knowledgeBaseId));
    }

    @SaCheckPermission("knowledge:edit")
    @DeleteMapping("/{documentId}")
    public ApiResult<Void> delete(@PathVariable("documentId") Long documentId) {
        documentService.delete(documentId);
        return ApiResult.success();
    }

    @SaCheckPermission("knowledge:edit")
    @PostMapping("/{documentId}/parse")
    public ApiResult<Void> triggerParse(@PathVariable("documentId") Long documentId) {
        documentService.triggerParse(documentId);
        return ApiResult.success();
    }
}
