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

    /** 提交流程状态：SUBMITTED=待批改 / GRADED=AI 已评待确认 / REVIEWED=教师已终审 */
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_GRADED = "GRADED";
    private static final String STATUS_REVIEWED = "REVIEWED";

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

    /**
     * 已提交答卷数（累计口径）。
     *
     * <p>REVIEWED（教师已终审）同样属于"已提交"——与
     * {@code AssignmentServiceImpl#remindUnsubmitted} 的已提交定义保持一致。
     * 早期只统计 SUBMITTED/GRADED，导致答卷被教师终审后，
     * 作业列表卡片的提交进度会回落为 0（详情页仍显示正确的已提交人数），两处数据自相矛盾。</p>
     */
    public long countByAssignmentId(Long assignmentId) {
        if (assignmentId == null) {
            return 0L;
        }
        return submissionMapper.selectCount(
                new LambdaQueryWrapper<SubmissionEntity>()
                        .eq(SubmissionEntity::getAssignmentId, assignmentId)
                        .in(SubmissionEntity::getStatus, STATUS_SUBMITTED, STATUS_GRADED, STATUS_REVIEWED)
        );
    }

    /** 待批改答卷数：仅 SUBMITTED 需要 AI 初次批改 */
    public long countPendingGradingByAssignmentId(Long assignmentId) {
        if (assignmentId == null) {
            return 0L;
        }
        return submissionMapper.selectCount(
                new LambdaQueryWrapper<SubmissionEntity>()
                        .eq(SubmissionEntity::getAssignmentId, assignmentId)
                        .eq(SubmissionEntity::getStatus, STATUS_SUBMITTED)
        );
    }

    /** 已批改答卷数（含 AI 已评待确认与教师已终审），供"AI 已批改"统计使用 */
    public long countGradedByAssignmentId(Long assignmentId) {
        if (assignmentId == null) {
            return 0L;
        }
        return submissionMapper.selectCount(
                new LambdaQueryWrapper<SubmissionEntity>()
                        .eq(SubmissionEntity::getAssignmentId, assignmentId)
                        .in(SubmissionEntity::getStatus, STATUS_GRADED, STATUS_REVIEWED)
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
        }
        return wrapper;
    }

    public int deleteById(Long id) {
        if (id == null) {
            return 0;
        }
        return submissionMapper.deleteById(id);
    }
}
