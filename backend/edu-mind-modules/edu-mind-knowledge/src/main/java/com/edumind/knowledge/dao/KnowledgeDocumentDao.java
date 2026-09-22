package com.edumind.knowledge.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.common.context.TenantContext;
import com.edumind.knowledge.entity.KnowledgeDocumentEntity;
import com.edumind.knowledge.mapper.KnowledgeDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class KnowledgeDocumentDao {

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    public KnowledgeDocumentEntity findById(Long id) {
        return knowledgeDocumentMapper.selectById(id);
    }

    /**
     * 忽略租户过滤按主键查询（仅供异步流水线/调度器在缺少租户上下文时反查文档归属租户使用）。
     * 调用方必须自行完成租户校验，禁止直接用于对外查询接口。
     */
    public KnowledgeDocumentEntity findByIdIgnoreTenant(Long id) {
        if (id == null) {
            return null;
        }
        KnowledgeDocumentEntity[] holder = new KnowledgeDocumentEntity[1];
        TenantContext.runWithoutTenant(() -> holder[0] = knowledgeDocumentMapper.selectById(id));
        return holder[0];
    }

    public List<KnowledgeDocumentEntity> findByKnowledgeBaseId(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            return Collections.emptyList();
        }
        return knowledgeDocumentMapper.selectList(
                new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                        .eq(KnowledgeDocumentEntity::getKnowledgeBaseId, knowledgeBaseId)
                        .orderByDesc(KnowledgeDocumentEntity::getCreateTime)
        );
    }

    public int insert(KnowledgeDocumentEntity entity) {
        return knowledgeDocumentMapper.insert(entity);
    }

    public int updateById(KnowledgeDocumentEntity entity) {
        return knowledgeDocumentMapper.updateById(entity);
    }

    public int deleteById(Long id) {
        return knowledgeDocumentMapper.deleteById(id);
    }

    public long countByKnowledgeBaseId(Long knowledgeBaseId) {
        return knowledgeDocumentMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                        .eq(KnowledgeDocumentEntity::getKnowledgeBaseId, knowledgeBaseId)
        );
    }

    public KnowledgeDocumentEntity findByCourseResourceId(Long courseResourceId) {
        if (courseResourceId == null) {
            return null;
        }
        return knowledgeDocumentMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                        .eq(KnowledgeDocumentEntity::getCourseResourceId, courseResourceId)
                        .last("LIMIT 1")
        );
    }

    public KnowledgeDocumentEntity findLessonDocument(Long courseId, Long lessonChapterId) {
        if (courseId == null || lessonChapterId == null) {
            return null;
        }
        return knowledgeDocumentMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeDocumentEntity>()
                        .eq(KnowledgeDocumentEntity::getSourceType, "LESSON")
                        .eq(KnowledgeDocumentEntity::getCourseId, courseId)
                        .eq(KnowledgeDocumentEntity::getLessonChapterId, lessonChapterId)
                        .last("LIMIT 1")
        );
    }
}
