package com.edumind.teaching.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.teaching.entity.SubmissionEntity;
import com.edumind.teaching.mapper.SubmissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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

    public long countByAssignmentId(Long assignmentId) {
        if (assignmentId == null) {
            return 0L;
        }
        return submissionMapper.selectCount(
                new LambdaQueryWrapper<SubmissionEntity>()
                        .eq(SubmissionEntity::getAssignmentId, assignmentId)
                        .in(SubmissionEntity::getStatus, "SUBMITTED", "GRADED")
        );
    }

    public long countPendingGradingByAssignmentId(Long assignmentId) {
        if (assignmentId == null) {
            return 0L;
        }
        return submissionMapper.selectCount(
                new LambdaQueryWrapper<SubmissionEntity>()
                        .eq(SubmissionEntity::getAssignmentId, assignmentId)
                        .eq(SubmissionEntity::getStatus, "SUBMITTED")
        );
    }

    public long countGradedByAssignmentId(Long assignmentId) {
        if (assignmentId == null) {
            return 0L;
        }
        return submissionMapper.selectCount(
                new LambdaQueryWrapper<SubmissionEntity>()
                        .eq(SubmissionEntity::getAssignmentId, assignmentId)
                        .eq(SubmissionEntity::getStatus, "GRADED")
        );
    }

    public Page<SubmissionEntity> pageQuery(List<Long> assignmentIds, Long assignmentId, String status,
                                              List<Long> studentIds, List<Long> keywordAssignmentIds,
                                              List<Long> keywordStudentIds, long pageNum, long pageSize) {
        Page<SubmissionEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SubmissionEntity> wrapper = buildScopeWrapper(
                assignmentIds, assignmentId, status, studentIds, keywordAssignmentIds, keywordStudentIds);
        wrapper.orderByDesc(SubmissionEntity::getSubmitTime);
        return submissionMapper.selectPage(page, wrapper);
    }

    public long countByScope(List<Long> assignmentIds, Long assignmentId, String status, List<Long> studentIds,
                             List<Long> keywordAssignmentIds, List<Long> keywordStudentIds) {
        LambdaQueryWrapper<SubmissionEntity> wrapper = buildScopeWrapper(
                assignmentIds, assignmentId, status, studentIds, keywordAssignmentIds, keywordStudentIds);
        return submissionMapper.selectCount(wrapper);
    }

    public List<SubmissionEntity> listByScope(List<Long> assignmentIds, Long assignmentId, String status,
                                              List<Long> studentIds, List<Long> keywordAssignmentIds,
                                              List<Long> keywordStudentIds) {
        LambdaQueryWrapper<SubmissionEntity> wrapper = buildScopeWrapper(
                assignmentIds, assignmentId, status, studentIds, keywordAssignmentIds, keywordStudentIds);
        wrapper.orderByDesc(SubmissionEntity::getSubmitTime);
        return submissionMapper.selectList(wrapper);
    }

    private LambdaQueryWrapper<SubmissionEntity> buildScopeWrapper(List<Long> assignmentIds, Long assignmentId,
                                                                     String status, List<Long> studentIds,
                                                                     List<Long> keywordAssignmentIds,
                                                                     List<Long> keywordStudentIds) {
        LambdaQueryWrapper<SubmissionEntity> wrapper = new LambdaQueryWrapper<>();
        if (assignmentId != null) {
            wrapper.eq(SubmissionEntity::getAssignmentId, assignmentId);
        } else if (!CollectionUtils.isEmpty(assignmentIds)) {
            wrapper.in(SubmissionEntity::getAssignmentId, assignmentIds);
        } else if (assignmentIds != null) {
            wrapper.eq(SubmissionEntity::getAssignmentId, -1L);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SubmissionEntity::getStatus, status.trim());
        }
        if (!CollectionUtils.isEmpty(keywordAssignmentIds) || !CollectionUtils.isEmpty(keywordStudentIds)) {
            wrapper.and(q -> {
                if (!CollectionUtils.isEmpty(keywordAssignmentIds)) {
                    q.in(SubmissionEntity::getAssignmentId, keywordAssignmentIds);
                }
                if (!CollectionUtils.isEmpty(keywordStudentIds)) {
                    if (!CollectionUtils.isEmpty(keywordAssignmentIds)) {
                        q.or().in(SubmissionEntity::getStudentId, keywordStudentIds);
                    } else {
                        q.in(SubmissionEntity::getStudentId, keywordStudentIds);
                    }
                }
            });
        } else if (!CollectionUtils.isEmpty(studentIds)) {
            wrapper.in(SubmissionEntity::getStudentId, studentIds);
        }
        return wrapper;
    }
}
