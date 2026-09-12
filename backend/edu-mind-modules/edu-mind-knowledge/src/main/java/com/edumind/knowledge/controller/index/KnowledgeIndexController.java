package com.edumind.knowledge.controller.index;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.knowledge.dto.knowledge.IndexRequestDTO;
import com.edumind.knowledge.service.index.IndexingService;
import com.edumind.knowledge.service.knowledge.KnowledgeAccessService;
import com.edumind.knowledge.vo.knowledge.IndexStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class KnowledgeIndexController {

    private final IndexingService indexingService;
    private final KnowledgeAccessService knowledgeAccessService;

    @SaCheckPermission("knowledge:edit")
    @PostMapping("/api/knowledge-bases/{id}/index")
    public ApiResult<Void> triggerIndex(@PathVariable("id") Long knowledgeBaseId,
                                        @RequestBody(required = false) IndexRequestDTO request) {
        String mode = request != null ? request.getMode() : "FULL";
        indexingService.triggerIndex(knowledgeBaseId, mode);
        return ApiResult.success();
    }

    @SaCheckPermission("knowledge:view")
    @GetMapping("/api/knowledge-bases/{id}/index/status")
    public ApiResult<IndexStatusVO> getIndexStatus(@PathVariable("id") Long knowledgeBaseId) {
        knowledgeAccessService.assertAccessible(knowledgeBaseId);
        return ApiResult.success(indexingService.getIndexStatus(knowledgeBaseId));
    }

    @SaCheckPermission("knowledge:edit")
    @PostMapping("/api/documents/{documentId}/reindex")
    public ApiResult<Void> reindexDocument(@PathVariable("documentId") Long documentId) {
        indexingService.reindexDocument(documentId);
        return ApiResult.success();
    }
}
