package com.edumind.teaching.service.submission.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.common.api.PageResult;
import com.edumind.common.exception.BusinessException;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.system.api.OrganizationQueryApi;
import com.edumind.system.api.UserQueryApi;
import com.edumind.system.vo.tenant.MemberOrgBriefVO;
import com.edumind.system.vo.user.UserBriefVO;
import com.edumind.teaching.dao.AssignmentDao;
import com.edumind.teaching.dao.SubmissionDao;
import com.edumind.teaching.dto.submission.SubmissionBatchGradeDTO;
import com.edumind.teaching.entity.AssignmentEntity;
import com.edumind.teaching.entity.SubmissionEntity;
import com.edumind.teaching.service.grading.GradingService;
import com.edumind.teaching.service.submission.SubmissionOverviewService;
import com.edumind.teaching.vo.submission.SubmissionListItemVO;
import com.edumind.teaching.vo.submission.SubmissionOverviewStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionOverviewServiceImpl implements SubmissionOverviewService {

    private static final long DEFAULT_TENANT_ID = 1L;

    private final SubmissionDao submissionDao;
    private final AssignmentDao assignmentDao;
    private final UserQueryApi userQueryApi;
    private final OrganizationQueryApi organizationQueryApi;
    private final CourseQueryApi courseQueryApi;
    private final GradingService gradingService;

    @Override
    public PageResult<SubmissionListItemVO> pageQuery(Long courseId, Long assignmentId, String status, String keyword,
                                                      Long page, Long pageSize) {
        long pageNum = page != null && page > 0 ? page : 1L;
        long size = pageSize != null && pageSize > 0 ? pageSize : 10L;
        QueryScope scope = resolveScope(courseId, assignmentId, keyword);

        Page<SubmissionEntity> result = submissionDao.pageQuery(
                scope.assignmentIds(), assignmentId, status, null,
                scope.keywordAssignmentIds(), scope.keywordStudentIds(), pageNum, size);

        Map<Long, AssignmentEntity> assignmentMap = loadAssignmentMap(result.getRecords());
        List<SubmissionListItemVO> list = result.getRecords().stream()
                .map(entity -> toListItem(entity, assignmentMap.get(entity.getAssignmentId())))
                .collect(Collectors.toList());

        return PageResult.<SubmissionListItemVO>builder()
                .total(result.getTotal())
                .pageNum(result.getCurrent())
                .pageSize(result.getSize())
                .list(list)
                .build();
    }

    @Override
    public SubmissionOverviewStatsVO getStats(Long courseId, Long assignmentId) {
        QueryScope scope = resolveScope(courseId, assignmentId, null);
        SubmissionOverviewStatsVO stats = new SubmissionOverviewStatsVO();
        stats.setTotal(submissionDao.countByScope(scope.assignmentIds(), assignmentId, null, null,
                scope.keywordAssignmentIds(), scope.keywordStudentIds()));
        stats.setSubmittedCount(submissionDao.countByScope(scope.assignmentIds(), assignmentId, "SUBMITTED", null,
                scope.keywordAssignmentIds(), scope.keywordStudentIds()));
        stats.setGradedCount(submissionDao.countByScope(scope.assignmentIds(), assignmentId, "GRADED", null,
                scope.keywordAssignmentIds(), scope.keywordStudentIds()));
        stats.setReviewedCount(submissionDao.countByScope(scope.assignmentIds(), assignmentId, "REVIEWED", null,
                scope.keywordAssignmentIds(), scope.keywordStudentIds()));
        return stats;
    }

    @Override
    public int batchGrade(SubmissionBatchGradeDTO dto) {
        if (dto == null) {
            throw new BusinessException("请求参数不能为空");
        }
        List<SubmissionEntity> targets;
        if (!CollectionUtils.isEmpty(dto.getSubmissionIds())) {
            targets = dto.getSubmissionIds().stream()
                    .map(submissionDao::findById)
                    .filter(e -> e != null && "SUBMITTED".equals(e.getStatus()))
                    .toList();
        } else {
            QueryScope scope = resolveScope(dto.getCourseId(), dto.getAssignmentId(), null);
            targets = submissionDao.listByScope(scope.assignmentIds(), dto.getAssignmentId(), "SUBMITTED", null,
                    scope.keywordAssignmentIds(), scope.keywordStudentIds());
        }
        int success = 0;
        for (SubmissionEntity entity : targets) {
            try {
                gradingService.gradeSubmission(entity.getId());
                success++;
            } catch (Exception ignored) {
                // continue batch
            }
        }
        return success;
    }

    private QueryScope resolveScope(Long courseId, Long assignmentId, String keyword) {
        List<Long> assignmentIds = null;
        List<Long> keywordAssignmentIds = null;
        List<Long> keywordStudentIds = null;

        if (assignmentId == null && courseId != null) {
            assignmentIds = assignmentDao.listByCourseId(courseId).stream()
                    .map(AssignmentEntity::getId)
                    .collect(Collectors.toList());
        }

        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            keywordAssignmentIds = assignmentDao.pageQuery(courseId, null, kw, 1, 500).getRecords().stream()
                    .map(AssignmentEntity::getId)
                    .toList();
            keywordStudentIds = userQueryApi.findUserIdsByKeyword(kw);
            if (CollectionUtils.isEmpty(keywordAssignmentIds) && CollectionUtils.isEmpty(keywordStudentIds)) {
                assignmentIds = List.of();
            }
        }

        return new QueryScope(assignmentIds, keywordAssignmentIds, keywordStudentIds);
    }

    private Map<Long, AssignmentEntity> loadAssignmentMap(List<SubmissionEntity> records) {
        Map<Long, AssignmentEntity> map = new HashMap<>();
        for (SubmissionEntity record : records) {
            if (record.getAssignmentId() == null || map.containsKey(record.getAssignmentId())) {
                continue;
            }
            AssignmentEntity assignment = assignmentDao.findById(record.getAssignmentId());
            if (assignment != null) {
                map.put(assignment.getId(), assignment);
            }
        }
        return map;
    }

    private SubmissionListItemVO toListItem(SubmissionEntity entity, AssignmentEntity assignment) {
        SubmissionListItemVO vo = new SubmissionListItemVO();
        vo.setId(entity.getId());
        vo.setAssignmentId(entity.getAssignmentId());
        vo.setStudentId(entity.getStudentId());
        vo.setStatus(entity.getStatus());
        vo.setTotalScore(entity.getTotalScore());
        vo.setMaxScore(entity.getMaxScore());
        vo.setSubmitTime(entity.getSubmitTime());
        if (assignment != null) {
            vo.setAssignmentTitle(assignment.getTitle());
            vo.setCourseId(assignment.getCourseId());
            try {
                CourseDetailVO course = courseQueryApi.getCourseById(assignment.getCourseId());
                if (course != null) {
                    vo.setCourseName(course.getName());
                }
            } catch (Exception ignored) {
                // optional enrich
            }
        }
        enrichStudent(vo);
        return vo;
    }

    private void enrichStudent(SubmissionListItemVO vo) {
        if (vo.getStudentId() == null) {
            return;
        }
        UserBriefVO user = userQueryApi.getUserById(vo.getStudentId());
        if (user != null) {
            vo.setStudentName(user.getRealName() != null ? user.getRealName() : user.getUsername());
        }
        MemberOrgBriefVO org = organizationQueryApi.getPrimaryClassByUserId(DEFAULT_TENANT_ID, vo.getStudentId());
        if (org != null && org.getMemberNo() != null) {
            vo.setStudentNo(org.getMemberNo());
        } else if (user != null) {
            vo.setStudentNo(user.getUsername());
        }
    }

    private record QueryScope(List<Long> assignmentIds, List<Long> keywordAssignmentIds, List<Long> keywordStudentIds) {
    }
}
