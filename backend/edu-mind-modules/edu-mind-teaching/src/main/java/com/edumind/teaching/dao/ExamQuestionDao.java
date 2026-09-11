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

    public int deleteByExamId(Long examId) {
        return examQuestionMapper.delete(
                new LambdaQueryWrapper<ExamQuestionEntity>()
                        .eq(ExamQuestionEntity::getExamId, examId)
        );
    }
}
