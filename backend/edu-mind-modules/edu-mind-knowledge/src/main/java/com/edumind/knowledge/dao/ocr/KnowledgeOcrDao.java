package com.edumind.knowledge.dao.ocr;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.knowledge.entity.ocr.KnowledgeOcrPageEntity;
import com.edumind.knowledge.entity.ocr.KnowledgeOcrTaskEntity;
import com.edumind.knowledge.mapper.ocr.KnowledgeOcrPageMapper;
import com.edumind.knowledge.mapper.ocr.KnowledgeOcrTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class KnowledgeOcrDao {

    private final KnowledgeOcrTaskMapper taskMapper;
    private final KnowledgeOcrPageMapper pageMapper;

    public KnowledgeOcrTaskEntity findTaskById(Long id) {
        return taskMapper.selectById(id);
    }

    /**
     * 按 ID 加载任务（忽略租户 SQL 拦截），仅用于 IDOR 归属校验。
     * 禁止在普通列表/分页查询中使用。
     */
    public KnowledgeOcrTaskEntity findTaskByIdIgnoreTenant(Long id) {
        if (id == null) {
            return null;
        }
        final KnowledgeOcrTaskEntity[] holder = new KnowledgeOcrTaskEntity[1];
        com.edumind.common.context.TenantContext.runWithoutTenant(() -> holder[0] = taskMapper.selectById(id));
        return holder[0];
    }

    public List<KnowledgeOcrTaskEntity> listTasksByDocument(Long tenantId, Long documentId) {
        return taskMapper.selectList(new LambdaQueryWrapper<KnowledgeOcrTaskEntity>()
                .eq(KnowledgeOcrTaskEntity::getTenantId, tenantId)
                .eq(KnowledgeOcrTaskEntity::getDocumentId, documentId)
                .orderByDesc(KnowledgeOcrTaskEntity::getCreateTime));
    }

    public int insertTask(KnowledgeOcrTaskEntity entity) {
        return taskMapper.insert(entity);
    }

    public int updateTask(KnowledgeOcrTaskEntity entity) {
        return taskMapper.updateById(entity);
    }

    public List<KnowledgeOcrPageEntity> listPagesByTaskId(Long taskId) {
        return pageMapper.selectList(new LambdaQueryWrapper<KnowledgeOcrPageEntity>()
                .eq(KnowledgeOcrPageEntity::getTaskId, taskId)
                .orderByAsc(KnowledgeOcrPageEntity::getPageNo));
    }

    public KnowledgeOcrPageEntity findPageById(Long id) {
        return pageMapper.selectById(id);
    }

    public int insertPage(KnowledgeOcrPageEntity entity) {
        return pageMapper.insert(entity);
    }

    public int updatePage(KnowledgeOcrPageEntity entity) {
        return pageMapper.updateById(entity);
    }
}
