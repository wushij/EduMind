package com.edumind.teaching.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.teaching.entity.SubmissionAnswerEntity;
import com.edumind.teaching.mapper.SubmissionAnswerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SubmissionAnswerDao {

    private final SubmissionAnswerMapper submissionAnswerMapper;

    public List<SubmissionAnswerEntity> listBySubmissionId(Long submissionId) {
        if (submissionId == null) {
            return Collections.emptyList();
        }
        return submissionAnswerMapper.selectList(
                new LambdaQueryWrapper<SubmissionAnswerEntity>()
                        .eq(SubmissionAnswerEntity::getSubmissionId, submissionId)
        );
    }

    public int insert(SubmissionAnswerEntity entity) {
        return submissionAnswerMapper.insert(entity);
    }

    public int deleteBySubmissionId(Long submissionId) {
        return submissionAnswerMapper.delete(
                new LambdaQueryWrapper<SubmissionAnswerEntity>()
                        .eq(SubmissionAnswerEntity::getSubmissionId, submissionId)
        );
    }
}
