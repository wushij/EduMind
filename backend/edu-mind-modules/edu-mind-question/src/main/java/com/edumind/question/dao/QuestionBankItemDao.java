package com.edumind.question.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.question.entity.QuestionBankItemEntity;
import com.edumind.question.mapper.QuestionBankItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class QuestionBankItemDao {

    /** 批量插入单条 SQL 的最大行数，避免超出 MySQL max_allowed_packet */
    private static final int INSERT_BATCH_SIZE = 500;

    private final QuestionBankItemMapper questionBankItemMapper;

    public List<Long> listQuestionIdsByBankId(Long bankId) {
        if (bankId == null) {
            return Collections.emptyList();
        }
        return questionBankItemMapper.selectList(
                new LambdaQueryWrapper<QuestionBankItemEntity>()
                        .eq(QuestionBankItemEntity::getBankId, bankId)
        ).stream().map(QuestionBankItemEntity::getQuestionId).collect(Collectors.toList());
    }

    public int countByBankId(Long bankId) {
        return Math.toIntExact(questionBankItemMapper.selectCount(
                new LambdaQueryWrapper<QuestionBankItemEntity>()
                        .eq(QuestionBankItemEntity::getBankId, bankId)
        ));
    }

    public int insert(QuestionBankItemEntity entity) {
        return questionBankItemMapper.insert(entity);
    }

    /** 单条 SQL 分片批量插入（参与当前事务），用于替代循环内逐条 insert */
    public int insertBatch(List<QuestionBankItemEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        int affected = 0;
        for (int i = 0; i < entities.size(); i += INSERT_BATCH_SIZE) {
            int end = Math.min(i + INSERT_BATCH_SIZE, entities.size());
            affected += questionBankItemMapper.insertBatch(entities.subList(i, end));
        }
        return affected;
    }

    public int deleteByBankIdAndQuestionId(Long bankId, Long questionId) {
        return questionBankItemMapper.delete(
                new LambdaQueryWrapper<QuestionBankItemEntity>()
                        .eq(QuestionBankItemEntity::getBankId, bankId)
                        .eq(QuestionBankItemEntity::getQuestionId, questionId)
        );
    }

    public boolean exists(Long bankId, Long questionId) {
        return questionBankItemMapper.selectCount(
                new LambdaQueryWrapper<QuestionBankItemEntity>()
                        .eq(QuestionBankItemEntity::getBankId, bankId)
                        .eq(QuestionBankItemEntity::getQuestionId, questionId)
        ) > 0;
    }
}
