package com.edumind.knowledge.service.rag;

import com.edumind.knowledge.vo.rag.KnowledgeRagDashboardVO;
import com.edumind.knowledge.vo.rag.KnowledgeRagPurgeResultVO;
import com.edumind.knowledge.vo.rag.KnowledgeRagSyncResultVO;

public interface KnowledgeRagDashboardService {

    KnowledgeRagDashboardVO getDashboard();

    KnowledgeRagSyncResultVO syncAll();

    KnowledgeRagSyncResultVO syncDocument(Long documentId);

    KnowledgeRagPurgeResultVO purgeOrphanChunks();
}
