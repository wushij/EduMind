package com.edumind.knowledge.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.knowledge.entity.KnowledgeIndexTaskEntity;
import com.edumind.knowledge.mapper.KnowledgeIndexTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class KnowledgeIndexTaskDao {

    private final KnowledgeIndexTaskMapper knowledgeIndexTaskMapper;

    public KnowledgeIndexTaskEntity findLatestByKnowledgeBaseId(Long knowledgeBaseId) {
        return knowledgeIndexTaskMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeIndexTaskEntity>()
                        .eq(KnowledgeIndexTaskEntity::getKnowledgeBaseId, knowledgeBaseId)
                        .orderByDesc(KnowledgeIndexTaskEntity::getId)
                        .last("LIMIT 1")
        );
    }

    public int insert(KnowledgeIndexTaskEntity entity) {
        return knowledgeIndexTaskMapper.insert(entity);
    }

    public int updateById(KnowledgeIndexTaskEntity entity) {
        return knowledgeIndexTaskMapper.updateById(entity);
    }

    public KnowledgeIndexTaskEntity findLatestGlobal() {
        return knowledgeIndexTaskMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeIndexTaskEntity>()
                        .orderByDesc(KnowledgeIndexTaskEntity::getId)
                        .last("LIMIT 1")
        );
    }

    /**
     * 取当前仍然处于 INDEXING 的最新任务。
     * 大盘进度应以「真正在执行的任务」为准，而不是最近一条（可能是终态）任务。
     */
    public KnowledgeIndexTaskEntity findLatestIndexing() {
        return knowledgeIndexTaskMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeIndexTaskEntity>()
                        .eq(KnowledgeIndexTaskEntity::getStatus, "INDEXING")
                        .orderByDesc(KnowledgeIndexTaskEntity::getId)
                        .last("LIMIT 1")
        );
    }

    /** 将任务直接置为终态（如 SUPERSEDED），避免老任务永久停留在 INDEXING。 */
    public int markTerminalStatus(Long taskId, String status, String errorMessage) {
        return knowledgeIndexTaskMapper.updateTerminalStatus(taskId, status, errorMessage);
    }

    /** 查询超时仍无进展的僵死任务归属知识库（去重）。 */
    public List<Long> findStaleIndexingKnowledgeBaseIds(LocalDateTime before) {
        return knowledgeIndexTaskMapper.selectStaleIndexingKnowledgeBaseIds(before);
    }

    /** 批量把超时仍无进展的僵死 INDEXING 任务复位为失败。 */
    public int failStaleIndexingTasks(LocalDateTime before, String errorMessage) {
        return knowledgeIndexTaskMapper.failStaleIndexing(before, errorMessage);
    }
}
