package com.edumind.teaching.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.teaching.entity.ExamQuestionEntity;
import com.edumind.teaching.mapper.ExamQuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ExamQuestionDao {

    /** 批量插入单条 SQL 的最大行数，避免超出 MySQL max_allowed_packet */
    private static final int INSERT_BATCH_SIZE = 500;

    private final ExamQuestionMapper examQuestionMapper;

    public List<ExamQuestionEntity> listByExamId(Long examId) {
        if (examId == null) {
            return Collections.emptyList();
        }
        return examQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamQuestionEntity>()
                        .eq(ExamQuestionEntity::getExamId, examId)
                        .orderByAsc(ExamQuestionEntity::getSortOrder)
        );
    }

    public int insert(ExamQuestionEntity entity) {
        return examQuestionMapper.insert(entity);
    }

    /** 单条 SQL 分片批量插入（参与当前事务），用于替代循环内逐条 insert */
    public int insertBatch(List<ExamQuestionEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        int affected = 0;
        for (int i = 0; i < entities.size(); i += INSERT_BATCH_SIZE) {
            int end = Math.min(i + INSERT_BATCH_SIZE, entities.size());
            affected += examQuestionMapper.insertBatch(entities.subList(i, end));
        }
        return affected;
    }

    public int deleteByExamId(Long examId) {
        return examQuestionMapper.delete(
                new LambdaQueryWrapper<ExamQuestionEntity>()
                        .eq(ExamQuestionEntity::getExamId, examId)
        );
    }
}
