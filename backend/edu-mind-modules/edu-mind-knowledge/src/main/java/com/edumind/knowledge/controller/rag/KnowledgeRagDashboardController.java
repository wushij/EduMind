package com.edumind.knowledge.controller.rag;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.knowledge.service.rag.KnowledgeRagDashboardService;
import com.edumind.knowledge.vo.rag.KnowledgeRagDashboardVO;
import com.edumind.knowledge.vo.rag.KnowledgeRagPurgeResultVO;
import com.edumind.knowledge.vo.rag.KnowledgeRagSyncResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/knowledge/rag-dashboard")
@RequiredArgsConstructor
public class KnowledgeRagDashboardController {

    private final KnowledgeRagDashboardService knowledgeRagDashboardService;

    @SaCheckPermission("knowledge:view")
    @GetMapping("/stats")
    public ApiResult<KnowledgeRagDashboardVO> stats() {
        return ApiResult.success(knowledgeRagDashboardService.getDashboard());
    }

    @SaCheckPermission("knowledge:edit")
    @PostMapping("/sync-all")
    public ApiResult<KnowledgeRagSyncResultVO> syncAll() {
        return ApiResult.success(knowledgeRagDashboardService.syncAll());
    }

    @SaCheckPermission("knowledge:edit")
    @PostMapping("/sync-document/{documentId}")
    public ApiResult<KnowledgeRagSyncResultVO> syncDocument(@PathVariable Long documentId) {
        return ApiResult.success(knowledgeRagDashboardService.syncDocument(documentId));
    }

    @SaCheckPermission("knowledge:edit")
    @PostMapping("/purge-orphans")
    public ApiResult<KnowledgeRagPurgeResultVO> purgeOrphans() {
        return ApiResult.success(knowledgeRagDashboardService.purgeOrphanChunks());
    }
}
