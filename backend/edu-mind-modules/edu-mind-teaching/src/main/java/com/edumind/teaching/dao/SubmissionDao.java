package com.edumind.teaching.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.teaching.entity.SubmissionEntity;
import com.edumind.teaching.mapper.SubmissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SubmissionDao {

    private final SubmissionMapper submissionMapper;

    public SubmissionEntity findById(Long id) {
        return submissionMapper.selectById(id);
    }

    public SubmissionEntity findByAssignmentAndStudent(Long assignmentId, Long studentId) {
        return submissionMapper.selectOne(
                new LambdaQueryWrapper<SubmissionEntity>()
                        .eq(SubmissionEntity::getAssignmentId, assignmentId)
                        .eq(SubmissionEntity::getStudentId, studentId)
        );
    }

    public List<SubmissionEntity> listByAssignmentId(Long assignmentId) {
        if (assignmentId == null) {
            return Collections.emptyList();
        }
        return submissionMapper.selectList(
                new LambdaQueryWrapper<SubmissionEntity>()
                        .eq(SubmissionEntity::getAssignmentId, assignmentId)
                        .orderByDesc(SubmissionEntity::getSubmitTime)
        );
    }

    public int insert(SubmissionEntity entity) {
        return submissionMapper.insert(entity);
    }

    public int updateById(SubmissionEntity entity) {
        return submissionMapper.updateById(entity);
    }

    public long countByStatus(String status) {
        if (status == null) {
            return 0L;
        }
        return submissionMapper.selectCount(
                new LambdaQueryWrapper<SubmissionEntity>().eq(SubmissionEntity::getStatus, status)
        );
    }
}
