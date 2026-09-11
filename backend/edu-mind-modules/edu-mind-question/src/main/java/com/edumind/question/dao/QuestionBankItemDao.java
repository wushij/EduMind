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
