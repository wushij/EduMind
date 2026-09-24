package com.edumind.teaching.service.submission.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.common.api.PageResult;
import com.edumind.common.api.ResultCode;
import com.edumind.common.exception.BusinessException;
import com.edumind.course.api.CourseAccessApi;
import com.edumind.course.api.CourseDataScope;
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
import java.util.Set;
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
    private final CourseAccessApi courseAccessApi;
    private final GradingService gradingService;

    @Override
    public PageResult<SubmissionListItemVO> pageQuery(Long courseId, Long assignmentId, String status, String keyword,
                                                      Long page, Long pageSize) {
        long pageNum = page != null && page > 0 ? page : 1L;
        long size = pageSize != null && pageSize > 0 ? pageSize : 10L;

        // 数据范围收敛：答卷列表必须限定在「当前用户可见课程」的作业范围内，
        // 否则不传 courseId 时会返回全库学生答卷（含成绩），属严重越权读取。
        CourseDataScope dataScope = courseAccessApi.resolveCurrentDataScope();
        if (dataScope.isEmpty()) {
            return PageResult.empty(pageNum, size);
        }
        assertCourseAccessible(dataScope, courseId);
        assertAssignmentAccessible(dataScope, assignmentId);

        QueryScope scope = resolveScope(visibleCourseIdsOrNull(dataScope), courseId, assignmentId, keyword);

        Page<SubmissionEntity> result = submissionDao.pageQuery(
                scope.assignmentIds(), assignmentId, status, null,
                scope.keywordAssignmentIds(), scope.keywordStudentIds(), pageNum, size);

        Map<Long, AssignmentEntity> assignmentMap = loadAssignmentMap(result.getRecords());
        Map<Long, String> courseNameMap = new HashMap<>();
        // 预批量拉取本页涉及的学生用户与班级信息（固定次数批量查询），替代渲染时逐条跨模块查询
        Map<Long, UserBriefVO> userMap = loadUserMap(result.getRecords());
        Map<Long, MemberOrgBriefVO> orgMap = loadOrgMap(result.getRecords());

        List<SubmissionListItemVO> list = result.getRecords().stream()
                .map(entity -> toListItem(entity, assignmentMap.get(entity.getAssignmentId()), courseNameMap, userMap, orgMap))
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
        SubmissionOverviewStatsVO stats = new SubmissionOverviewStatsVO();
        // 统计口径必须与列表一致，否则会出现"卡片统计全库、列表只剩自己的"矛盾数据
        CourseDataScope dataScope = courseAccessApi.resolveCurrentDataScope();
        if (dataScope.isEmpty()) {
            return stats;
        }
        assertCourseAccessible(dataScope, courseId);
        assertAssignmentAccessible(dataScope, assignmentId);
        QueryScope scope = resolveScope(visibleCourseIdsOrNull(dataScope), courseId, assignmentId, null);
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
        boolean force = Boolean.TRUE.equals(dto.getForceRegrade());

        CourseDataScope dataScope = courseAccessApi.resolveCurrentDataScope();
        if (dataScope.isEmpty()) {
            return 0;
        }
        assertCourseAccessible(dataScope, dto.getCourseId());
        assertAssignmentAccessible(dataScope, dto.getAssignmentId());

        List<SubmissionEntity> targets;
        if (!CollectionUtils.isEmpty(dto.getSubmissionIds())) {
            // 按显式提交 ID 批量批改：逐条剔除不在可见范围内的答卷，
            // 防止通过遍历 ID 越权调用 AI 批改并覆盖他人课程的成绩
            Set<Long> allowedAssignmentIds = visibleAssignmentIds(dataScope);
            targets = dto.getSubmissionIds().stream()
                    .map(submissionDao::findById)
                    .filter(e -> e != null && (force || "SUBMITTED".equals(e.getStatus())))
                    .filter(e -> allowedAssignmentIds == null || allowedAssignmentIds.contains(e.getAssignmentId()))
                    .toList();
        } else {
            QueryScope scope = resolveScope(visibleCourseIdsOrNull(dataScope), dto.getCourseId(),
                    dto.getAssignmentId(), null);
            String targetStatus = force ? null : "SUBMITTED";
            targets = submissionDao.listByScope(scope.assignmentIds(), dto.getAssignmentId(), targetStatus, null,
                    scope.keywordAssignmentIds(), scope.keywordStudentIds());
            if (force) {
                // 不重新覆盖已经最终审阅完成（REVIEWED）的答卷，保护教师终审成绩
                targets = targets.stream()
                        .filter(e -> !"REVIEWED".equals(e.getStatus()))
                        .toList();
            }
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

    /**
     * 解析答卷查询范围。
     *
     * @param visibleCourseIds 当前用户可见课程集合；null 表示不限（平台/租户管理员）。
     *                         未指定具体作业时用它收敛到「可见课程下的作业」，避免全库答卷泄露
     */
    private QueryScope resolveScope(List<Long> visibleCourseIds, Long courseId, Long assignmentId, String keyword) {
        List<Long> assignmentIds = null;
        List<Long> keywordAssignmentIds = null;
        List<Long> keywordStudentIds = null;

        if (assignmentId == null) {
            if (courseId != null) {
                assignmentIds = assignmentDao.listByCourseId(courseId).stream()
                        .map(AssignmentEntity::getId)
                        .collect(Collectors.toList());
            } else if (visibleCourseIds != null) {
                assignmentIds = assignmentDao.listByCourseIds(visibleCourseIds).stream()
                        .map(AssignmentEntity::getId)
                        .collect(Collectors.toList());
            }
        }

        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            // 关键字检索只按可见课程/课程收敛，不限作业状态（status 传 null），取前 500 条命中作业即可
            keywordAssignmentIds = assignmentDao.pageQuery(courseId, visibleCourseIds, null, kw, 1, 500)
                    .getRecords().stream()
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

    /** 批量预取本页学生用户信息，避免列表渲染时逐条查询用户（原有 computeIfAbsent 缓存仅能去重，仍为多次查询） */
    private Map<Long, UserBriefVO> loadUserMap(List<SubmissionEntity> records) {
        List<Long> studentIds = records.stream()
                .map(SubmissionEntity::getStudentId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (studentIds.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return new HashMap<>(userQueryApi.mapUserBriefsByIds(studentIds));
        } catch (Exception ignored) {
            return new HashMap<>();
        }
    }

    /** 批量预取本页学生班级/学号信息，避免列表渲染时逐条解析班级造成 N+1 */
    private Map<Long, MemberOrgBriefVO> loadOrgMap(List<SubmissionEntity> records) {
        List<Long> studentIds = records.stream()
                .map(SubmissionEntity::getStudentId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (studentIds.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return new HashMap<>(organizationQueryApi.mapPrimaryClassesByUserIds(DEFAULT_TENANT_ID, studentIds));
        } catch (Exception ignored) {
            return new HashMap<>();
        }
    }

    private SubmissionListItemVO toListItem(SubmissionEntity entity, AssignmentEntity assignment,
                                            Map<Long, String> courseNameMap,
                                            Map<Long, UserBriefVO> userMap,
                                            Map<Long, MemberOrgBriefVO> orgMap) {
        SubmissionListItemVO vo = new SubmissionListItemVO();
        vo.setId(entity.getId());
        vo.setAssignmentId(entity.getAssignmentId());
        vo.setStudentId(entity.getStudentId());
        vo.setStatus(entity.getStatus());
        vo.setTotalScore(entity.getTotalScore());
        vo.setMaxScore(entity.getMaxScore());
        vo.setSubmitTime(entity.getSubmitTime());
        if (assignment != null && assignment.getCourseId() != null) {
            vo.setAssignmentTitle(assignment.getTitle());
            vo.setCourseId(assignment.getCourseId());
            String courseName = courseNameMap.computeIfAbsent(assignment.getCourseId(), cid -> {
                try {
                    CourseDetailVO course = courseQueryApi.getCourseById(cid);
                    return course != null ? course.getName() : null;
                } catch (Exception ignored) {
                    return null;
                }
            });
            if (courseName != null) {
                vo.setCourseName(courseName);
            }
        }
        enrichStudent(vo, userMap, orgMap);
        return vo;
    }

    private void enrichStudent(SubmissionListItemVO vo,
                               Map<Long, UserBriefVO> userMap,
                               Map<Long, MemberOrgBriefVO> orgMap) {
        if (vo.getStudentId() == null) {
            return;
        }
        // 用户与班级信息已由 pageQuery 批量预取，这里只做内存映射，不再逐条跨模块查询
        UserBriefVO user = userMap.get(vo.getStudentId());
        if (user != null) {
            vo.setStudentName(user.getRealName() != null ? user.getRealName() : user.getUsername());
            // 头像同样来自已批量预取的用户信息，无需额外查询
            vo.setStudentAvatar(user.getAvatar());
        }
        MemberOrgBriefVO org = orgMap.get(vo.getStudentId());
        if (org != null && org.getMemberNo() != null) {
            vo.setStudentNo(org.getMemberNo());
        } else if (user != null) {
            vo.setStudentNo(user.getUsername());
        }
    }

    /** 数据范围折算成 DAO 过滤参数：null 表示不限（平台/租户管理员） */
    private List<Long> visibleCourseIdsOrNull(CourseDataScope dataScope) {
        return dataScope.isAll() ? null : new ArrayList<>(dataScope.getCourseIds());
    }

    /** 当前可见课程下的作业 ID 集合；null 表示不限（平台/租户管理员） */
    private Set<Long> visibleAssignmentIds(CourseDataScope dataScope) {
        if (dataScope.isAll()) {
            return null;
        }
        return assignmentDao.listByCourseIds(dataScope.getCourseIds()).stream()
                .map(AssignmentEntity::getId)
                .collect(Collectors.toSet());
    }

    /** 显式指定的 courseId 必须落在可见范围内，否则视为越权读取 */
    private void assertCourseAccessible(CourseDataScope dataScope, Long courseId) {
        if (courseId != null && !dataScope.contains(courseId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权访问该课程的答卷数据");
        }
    }

    /** 显式指定的作业必须落在可见范围内，否则视为越权读取 */
    private void assertAssignmentAccessible(CourseDataScope dataScope, Long assignmentId) {
        if (assignmentId == null) {
            return;
        }
        AssignmentEntity entity = assignmentDao.findById(assignmentId);
        if (entity == null) {
            throw new BusinessException("作业不存在");
        }
        if (!dataScope.contains(entity.getCourseId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权访问该作业的答卷数据");
        }
    }

    private record QueryScope(List<Long> assignmentIds, List<Long> keywordAssignmentIds, List<Long> keywordStudentIds) {
    }
}
