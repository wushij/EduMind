package com.edumind.teaching.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.teaching.entity.GradingResultEntity;
import com.edumind.teaching.mapper.GradingResultMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class GradingResultDao {

    private final GradingResultMapper gradingResultMapper;

    public List<GradingResultEntity> listBySubmissionId(Long submissionId) {
        if (submissionId == null) {
            return Collections.emptyList();
        }
        return gradingResultMapper.selectList(
                new LambdaQueryWrapper<GradingResultEntity>()
                        .eq(GradingResultEntity::getSubmissionId, submissionId)
        );
    }

    public int insert(GradingResultEntity entity) {
        return gradingResultMapper.insert(entity);
    }

    public int updateById(GradingResultEntity entity) {
        return gradingResultMapper.updateById(entity);
    }

    public GradingResultEntity findBySubmissionIdAndQuestionId(Long submissionId, Long questionId) {
        if (submissionId == null || questionId == null) {
            return null;
        }
        return gradingResultMapper.selectOne(
                new LambdaQueryWrapper<GradingResultEntity>()
                        .eq(GradingResultEntity::getSubmissionId, submissionId)
                        .eq(GradingResultEntity::getQuestionId, questionId)
        );
    }

    public int deleteBySubmissionId(Long submissionId) {
        if (submissionId == null) {
            return 0;
        }
        return gradingResultMapper.delete(
                new LambdaQueryWrapper<GradingResultEntity>()
                        .eq(GradingResultEntity::getSubmissionId, submissionId)
        );
    }
}
