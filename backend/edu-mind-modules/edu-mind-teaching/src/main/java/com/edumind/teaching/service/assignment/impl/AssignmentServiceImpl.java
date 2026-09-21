package com.edumind.teaching.service.assignment.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.common.api.PageResult;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.UserContext;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.notification.api.NotificationWriteApi;
import com.edumind.question.api.QuestionQueryApi;
import com.edumind.question.vo.question.QuestionVO;
import com.edumind.teaching.converter.AssignmentConverter;
import com.edumind.teaching.dao.AssignmentDao;
import com.edumind.teaching.dao.ExamQuestionDao;
import com.edumind.teaching.dao.GradingResultDao;
import com.edumind.teaching.dao.SubmissionAnswerDao;
import com.edumind.teaching.dao.SubmissionDao;
import com.edumind.teaching.dto.assignment.AssignmentCreateDTO;
import com.edumind.teaching.dto.exam.ExamCreateDTO;
import com.edumind.teaching.dto.exam.ExamQuestionItemDTO;
import com.edumind.teaching.entity.AssignmentEntity;
import com.edumind.teaching.entity.SubmissionEntity;
import com.edumind.teaching.service.assignment.AssignmentService;
import com.edumind.teaching.service.exam.ExamService;
import com.edumind.teaching.vo.assignment.AssignmentPaperVO;
import com.edumind.teaching.vo.assignment.AssignmentStatsVO;
import com.edumind.teaching.vo.assignment.AssignmentVO;
import com.edumind.teaching.vo.assignment.StudentAssignmentVO;
import com.edumind.teaching.vo.exam.ExamQuestionVO;
import com.edumind.teaching.vo.exam.ExamVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private static final long DEFAULT_TENANT_ID = 1L;

    private final AssignmentDao assignmentDao;
    private final SubmissionDao submissionDao;
    private final SubmissionAnswerDao submissionAnswerDao;
    private final GradingResultDao gradingResultDao;
    private final ExamQuestionDao examQuestionDao;
    private final AssignmentConverter assignmentConverter;
    private final ExamService examService;
    private final QuestionQueryApi questionQueryApi;
    private final CourseQueryApi courseQueryApi;
    private final NotificationWriteApi notificationWriteApi;

    @Override
    public PageResult<AssignmentVO> pageQuery(Long courseId, String status, String keyword, Long page, Long pageSize) {
        long pageNum = page != null && page > 0 ? page : 1L;
        long size = pageSize != null && pageSize > 0 ? pageSize : 10L;
        Page<AssignmentEntity> result = assignmentDao.pageQuery(courseId, status, keyword, pageNum, size);
        List<AssignmentVO> list = result.getRecords().stream()
                .map(this::enrich)
                .collect(Collectors.toList());
        return PageResult.<AssignmentVO>builder()
                .total(result.getTotal())
                .pageNum(result.getCurrent())
                .pageSize(result.getSize())
                .list(list)
                .build();
    }

    @Override
    public AssignmentStatsVO getStats(Long courseId) {
        AssignmentStatsVO stats = new AssignmentStatsVO();
        List<AssignmentEntity> assignments = courseId != null
                ? assignmentDao.listByCourseId(courseId)
                : assignmentDao.pageQuery(null, null, null, 1, 1000).getRecords();

        long active = assignments.stream().filter(a -> "PUBLISHED".equals(a.getStatus())).count();
        stats.setActiveAssignmentCount(active);

        long pendingGrading = 0;
        long aiGraded = 0;
        int submittedSlots = 0;
        int expectedSlots = 0;

        for (AssignmentEntity assignment : assignments) {
            if (!"PUBLISHED".equals(assignment.getStatus()) && !"CLOSED".equals(assignment.getStatus())) {
                continue;
            }
            pendingGrading += submissionDao.countPendingGradingByAssignmentId(assignment.getId());
            aiGraded += submissionDao.countGradedByAssignmentId(assignment.getId());
            int submitted = (int) submissionDao.countByAssignmentId(assignment.getId());
            submittedSlots += submitted;
            AssignmentVO enriched = enrich(assignment);
            int studentCount = enriched.getStudentCount() != null ? enriched.getStudentCount() : 0;
            expectedSlots += Math.max(studentCount, 1);
        }

        stats.setPendingGradingCount(pendingGrading);
        stats.setAiGradedCount(aiGraded);
        stats.setAvgSubmissionRate(expectedSlots > 0 ? (submittedSlots * 100.0 / expectedSlots) : 0.0);
        return stats;
    }

    @Override
    public AssignmentVO getById(Long id) {
        AssignmentEntity entity = assignmentDao.findById(id);
        if (entity == null) {
            throw new BusinessException("作业不存在");
        }
        return enrich(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AssignmentCreateDTO dto) {
        Long examId = resolveExamId(dto);

        AssignmentEntity entity = new AssignmentEntity();
        entity.setCourseId(dto.getCourseId());
        entity.setExamId(examId);
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setDeadline(dto.getDeadline());
        entity.setTotalScore(dto.getTotalScore());
        entity.setPassScore(dto.getPassScore());
        entity.setSettingsJson(assignmentConverter.toSettingsJson(dto.getSettings()));
        entity.setStatus("DRAFT");
        assignmentDao.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        AssignmentEntity entity = requireAssignment(id);
        if (!"DRAFT".equals(entity.getStatus())) {
            throw new BusinessException("仅草稿状态的作业可发布");
        }
        validateExamLinked(entity);
        entity.setStatus("PUBLISHED");
        assignmentDao.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void close(Long id) {
        AssignmentEntity entity = requireAssignment(id);
        if (!"PUBLISHED".equals(entity.getStatus())) {
            throw new BusinessException("仅进行中的作业可归档关闭");
        }
        entity.setStatus("CLOSED");
        assignmentDao.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id == null) {
            return;
        }
        AssignmentEntity entity = requireAssignment(id);
        // 级联清理作业名下所有答卷、答案与评分记录
        List<SubmissionEntity> submissions = submissionDao.listByAssignmentId(id);
        if (submissions != null && !submissions.isEmpty()) {
            for (SubmissionEntity sub : submissions) {
                gradingResultDao.deleteBySubmissionId(sub.getId());
                submissionAnswerDao.deleteBySubmissionId(sub.getId());
                submissionDao.deleteById(sub.getId());
            }
        }
        assignmentDao.deleteById(id);
    }

    @Override
    public List<StudentAssignmentVO> listMine(Long courseId) {
        Long userId = requireUserId();
        List<Long> courseIds = courseQueryApi.listCourseIdsByUserId(userId);
        if (courseIds.isEmpty()) {
            return List.of();
        }
        if (courseId != null) {
            if (!courseIds.contains(courseId)) {
                return List.of();
            }
            courseIds = List.of(courseId);
        }
        List<AssignmentEntity> assignments = assignmentDao.listPublishedByCourseIds(courseIds);
        List<StudentAssignmentVO> result = new ArrayList<>();
        for (AssignmentEntity entity : assignments) {
            StudentAssignmentVO vo = new StudentAssignmentVO();
            AssignmentVO base = enrich(entity);
            copyAssignmentFields(base, vo);
            SubmissionEntity submission = submissionDao.findByAssignmentAndStudent(entity.getId(), userId);
            if (submission == null) {
                vo.setMySubmissionStatus("NOT_STARTED");
            } else {
                vo.setMySubmissionId(submission.getId());
                vo.setMySubmissionStatus(mapSubmissionStatus(submission.getStatus()));
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public AssignmentPaperVO getPaper(Long id) {
        Long userId = requireUserId();
        AssignmentEntity entity = requireAssignment(id);
        if (!"PUBLISHED".equals(entity.getStatus()) && !"CLOSED".equals(entity.getStatus())) {
            throw new BusinessException("作业未发布或不可作答");
        }
        if (!courseQueryApi.isCourseMember(entity.getCourseId(), userId)) {
            throw new BusinessException("您未加入该课程，无法作答");
        }
        if (entity.getExamId() == null) {
            throw new BusinessException("作业未关联试卷，无法作答");
        }
        ExamVO exam = examService.getById(entity.getExamId());
        AssignmentPaperVO paper = new AssignmentPaperVO();
        paper.setAssignmentId(entity.getId());
        paper.setTitle(entity.getTitle());
        paper.setCourseId(entity.getCourseId());
        paper.setDeadline(entity.getDeadline());
        paper.setTotalScore(entity.getTotalScore() != null ? entity.getTotalScore() : exam.getTotalScore());
        paper.setPassScore(entity.getPassScore() != null ? entity.getPassScore() : exam.getPassScore());
        paper.setSettings(assignmentConverter.parseSettings(entity.getSettingsJson()));
        paper.setQuestions(stripAnswers(exam.getQuestions()));
        SubmissionEntity submission = submissionDao.findByAssignmentAndStudent(id, userId);
        if (submission == null) {
            paper.setMySubmissionStatus("NOT_STARTED");
        } else {
            paper.setMySubmissionId(submission.getId());
            paper.setMySubmissionStatus(mapSubmissionStatus(submission.getStatus()));
        }
        return paper;
    }

    @Override
    public int remindUnsubmitted(Long id) {
        AssignmentEntity entity = requireAssignment(id);
        List<Long> studentIds = courseQueryApi.listStudentUserIdsByCourseId(entity.getCourseId());
        if (studentIds.isEmpty()) {
            return 0;
        }
        Set<Long> submitted = submissionDao.listByAssignmentId(id).stream()
                .filter(s -> "SUBMITTED".equals(s.getStatus()) || "GRADED".equals(s.getStatus())
                        || "REVIEWED".equals(s.getStatus()))
                .map(SubmissionEntity::getStudentId)
                .collect(Collectors.toSet());
        List<Long> targets = studentIds.stream().filter(sid -> !submitted.contains(sid)).toList();
        if (targets.isEmpty()) {
            return 0;
        }
        String title = "作业催交提醒：" + entity.getTitle();
        String content = "您有一份课程作业尚未提交，请在截止时间前完成提交。"
                + (entity.getDeadline() != null ? " 截止时间：" + entity.getDeadline() : "");
        notificationWriteApi.sendToUsers(DEFAULT_TENANT_ID, targets, title, content, "ASSIGNMENT", entity.getId());
        return targets.size();
    }

    private Long resolveExamId(AssignmentCreateDTO dto) {
        if (dto.getExamId() != null) {
            ExamVO exam = examService.getById(dto.getExamId());
            if (!dto.getCourseId().equals(exam.getCourseId())) {
                throw new BusinessException("所选试卷不属于当前课程");
            }
            if (dto.getTotalScore() == null) {
                dto.setTotalScore(exam.getTotalScore());
            }
            if (dto.getPassScore() == null) {
                dto.setPassScore(exam.getPassScore());
            }
            return dto.getExamId();
        }
        if (!CollectionUtils.isEmpty(dto.getQuestionIds())) {
            ExamCreateDTO examDto = new ExamCreateDTO();
            examDto.setCourseId(dto.getCourseId());
            examDto.setTitle(dto.getTitle() + "·作业卷");
            examDto.setTotalScore(dto.getTotalScore() != null ? dto.getTotalScore() : 100);
            examDto.setPassScore(dto.getPassScore() != null ? dto.getPassScore() : 60);
            List<ExamQuestionItemDTO> items = new ArrayList<>();
            int sort = 1;
            for (Long questionId : dto.getQuestionIds()) {
                QuestionVO question = questionQueryApi.getQuestionById(questionId);
                ExamQuestionItemDTO item = new ExamQuestionItemDTO();
                item.setQuestionId(questionId);
                item.setScore(question != null && question.getScore() != null ? question.getScore() : 5);
                item.setSortOrder(sort++);
                items.add(item);
            }
            examDto.setQuestions(items);
            return examService.create(examDto);
        }
        if (dto.getExamId() == null && CollectionUtils.isEmpty(dto.getQuestionIds())) {
            throw new BusinessException("请关联试卷或选择至少一道题目");
        }
        return null;
    }

    private void validateExamLinked(AssignmentEntity entity) {
        if (entity.getExamId() == null) {
            throw new BusinessException("作业未关联试卷，无法发布");
        }
        if (examQuestionDao.listByExamId(entity.getExamId()).isEmpty()) {
            throw new BusinessException("关联试卷无试题，无法发布");
        }
    }

    private AssignmentVO enrich(AssignmentEntity entity) {
        AssignmentVO vo = assignmentConverter.toVO(entity);
        try {
            CourseDetailVO course = courseQueryApi.getCourseById(entity.getCourseId());
            if (course != null) {
                vo.setCourseName(course.getName());
                vo.setStudentCount(course.getStudentCount() != null ? course.getStudentCount().intValue() : 0);
            }
        } catch (Exception ignored) {
            vo.setStudentCount(0);
        }
        int submitted = (int) submissionDao.countByAssignmentId(entity.getId());
        vo.setSubmittedCount(submitted);
        vo.setSubmissionCount(submitted);
        vo.setPendingGradingCount((int) submissionDao.countPendingGradingByAssignmentId(entity.getId()));
        if (vo.getStudentCount() == null || vo.getStudentCount() == 0) {
            vo.setStudentCount(Math.max(submitted, 0));
        }
        return vo;
    }

    private List<ExamQuestionVO> stripAnswers(List<ExamQuestionVO> questions) {
        if (questions == null) {
            return List.of();
        }
        for (ExamQuestionVO item : questions) {
            if (item.getQuestion() != null) {
                item.getQuestion().setAnswer(null);
                item.getQuestion().setAnalysis(null);
            }
        }
        return questions;
    }

    private AssignmentEntity requireAssignment(Long id) {
        AssignmentEntity entity = assignmentDao.findById(id);
        if (entity == null) {
            throw new BusinessException("作业不存在");
        }
        return entity;
    }

    private Long requireUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("未登录");
        }
        return userId;
    }

    private String mapSubmissionStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "NOT_STARTED";
        }
        return switch (status) {
            case "IN_PROGRESS" -> "IN_PROGRESS";
            case "SUBMITTED" -> "SUBMITTED";
            case "GRADED", "REVIEWED" -> "GRADED";
            default -> status;
        };
    }

    private void copyAssignmentFields(AssignmentVO source, StudentAssignmentVO target) {
        target.setId(source.getId());
        target.setCourseId(source.getCourseId());
        target.setExamId(source.getExamId());
        target.setTitle(source.getTitle());
        target.setDescription(source.getDescription());
        target.setDeadline(source.getDeadline());
        target.setStatus(source.getStatus());
        target.setCreateTime(source.getCreateTime());
        target.setTotalScore(source.getTotalScore());
        target.setPassScore(source.getPassScore());
        target.setSettings(source.getSettings());
        target.setCourseName(source.getCourseName());
        target.setSubmissionCount(source.getSubmissionCount());
        target.setSubmittedCount(source.getSubmittedCount());
        target.setPendingGradingCount(source.getPendingGradingCount());
        target.setStudentCount(source.getStudentCount());
    }
}
