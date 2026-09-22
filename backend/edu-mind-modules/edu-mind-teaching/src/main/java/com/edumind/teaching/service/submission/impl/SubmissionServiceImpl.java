package com.edumind.teaching.service.submission.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.edumind.common.event.LearningActivityEvent;
import com.edumind.common.api.ResultCode;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.UserContext;
import com.edumind.infrastructure.redis.DistributedLockService;
import com.edumind.infrastructure.redis.RedisKeyBuilder;
import com.edumind.system.api.OrganizationQueryApi;
import com.edumind.system.api.UserQueryApi;
import com.edumind.system.vo.tenant.MemberOrgBriefVO;
import com.edumind.system.vo.user.UserBriefVO;
import com.edumind.teaching.converter.AssignmentConverter;
import com.edumind.teaching.dao.AssignmentDao;
import com.edumind.teaching.dao.GradingResultDao;
import com.edumind.teaching.dao.SubmissionAnswerDao;
import com.edumind.teaching.dao.SubmissionDao;
import com.edumind.teaching.vo.assignment.AssignmentSettingsVO;
import com.edumind.teaching.dto.submission.SubmissionAnswerItemDTO;
import com.edumind.teaching.dto.submission.SubmissionCreateDTO;
import com.edumind.teaching.entity.AssignmentEntity;
import com.edumind.teaching.entity.SubmissionAnswerEntity;
import com.edumind.teaching.entity.SubmissionEntity;
import com.edumind.teaching.service.grading.GradingService;
import com.edumind.teaching.service.submission.SubmissionService;
import com.edumind.teaching.vo.submission.SubmissionAnswerVO;
import com.edumind.teaching.vo.submission.SubmissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final AssignmentDao assignmentDao;
    private final SubmissionDao submissionDao;
    private final SubmissionAnswerDao submissionAnswerDao;
    private final GradingResultDao gradingResultDao;
    private final GradingService gradingService;
    private final DistributedLockService distributedLockService;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final AssignmentConverter assignmentConverter;
    private final UserQueryApi userQueryApi;
    private final OrganizationQueryApi organizationQueryApi;

    private static final long DEFAULT_TENANT_ID = 1L;

    @Override
    public SubmissionVO submit(Long assignmentId, SubmissionCreateDTO dto) {
        Long studentId = requireUserId();
        String lockKey = RedisKeyBuilder.lock("assignment:submit", assignmentId + ":" + studentId);
        return distributedLockService.executeWithLock(lockKey, 3, 30,
                () -> transactionTemplate.execute(status -> doSubmit(assignmentId, dto, studentId)));
    }

    protected SubmissionVO doSubmit(Long assignmentId, SubmissionCreateDTO dto, Long studentId) {
        AssignmentEntity assignment = assignmentDao.findById(assignmentId);
        if (assignment == null) {
            throw new BusinessException("作业不存在");
        }
        if (!"PUBLISHED".equals(assignment.getStatus())) {
            throw new BusinessException("作业未发布，无法提交");
        }
        if (assignment.getDeadline() != null && LocalDateTime.now().isAfter(assignment.getDeadline())) {
            AssignmentSettingsVO settings = assignmentConverter.parseSettings(assignment.getSettingsJson());
            if (settings == null || !Boolean.TRUE.equals(settings.getAllowLate())) {
                throw new BusinessException("已超过截止时间，无法提交");
            }
        }

        SubmissionEntity submission = submissionDao.findByAssignmentAndStudent(assignmentId, studentId);
        if (submission == null) {
            submission = new SubmissionEntity();
            submission.setAssignmentId(assignmentId);
            submission.setStudentId(studentId);
            submission.setStatus("IN_PROGRESS");
            submissionDao.insert(submission);
        }

        submissionAnswerDao.deleteBySubmissionId(submission.getId());
        // 单条 SQL 批量写入作答明细（替代逐题 insert）
        List<SubmissionAnswerEntity> answerEntities = new ArrayList<>();
        for (SubmissionAnswerItemDTO item : dto.getAnswers()) {
            SubmissionAnswerEntity answer = new SubmissionAnswerEntity();
            answer.setSubmissionId(submission.getId());
            answer.setQuestionId(item.getQuestionId());
            answer.setAnswer(item.getAnswer());
            answerEntities.add(answer);
        }
        submissionAnswerDao.insertBatch(answerEntities);

        submission.setStatus("SUBMITTED");
        submission.setSubmitTime(LocalDateTime.now());
        submissionDao.updateById(submission);

        gradingService.gradeSubmission(submission.getId());
        eventPublisher.publishEvent(new LearningActivityEvent(
                this, studentId, assignment.getCourseId(), "STUDY", 30, null));
        return getById(submission.getId());
    }

    @Override
    public SubmissionVO getById(Long id) {
        SubmissionEntity entity = submissionDao.findById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "提交记录不存在");
        }
        assertCanViewSubmission(entity);
        SubmissionVO vo = new SubmissionVO();
        vo.setId(entity.getId());
        vo.setAssignmentId(entity.getAssignmentId());
        vo.setStudentId(entity.getStudentId());
        vo.setStatus(entity.getStatus());
        vo.setTotalScore(entity.getTotalScore());
        vo.setMaxScore(entity.getMaxScore());
        vo.setSubmitTime(entity.getSubmitTime());
        vo.setAnswers(submissionAnswerDao.listBySubmissionId(id).stream().map(answer -> {
            SubmissionAnswerVO item = new SubmissionAnswerVO();
            item.setQuestionId(answer.getQuestionId());
            item.setAnswer(answer.getAnswer());
            return item;
        }).collect(Collectors.toList()));
        vo.setGradingItems(gradingService.getGradingResults(id));
        enrichStudentInfo(vo);
        AssignmentEntity assignment = assignmentDao.findById(entity.getAssignmentId());
        if (assignment != null) {
            vo.setAssignmentTitle(assignment.getTitle());
        }
        return vo;
    }

    private void enrichStudentInfo(SubmissionVO vo) {
        if (vo.getStudentId() == null) {
            return;
        }
        UserBriefVO user = userQueryApi.getUserById(vo.getStudentId());
        if (user != null) {
            vo.setStudentName(user.getRealName() != null ? user.getRealName() : user.getUsername());
            vo.setStudentAvatar(user.getAvatar());
        }
        MemberOrgBriefVO org = organizationQueryApi.getPrimaryClassByUserId(DEFAULT_TENANT_ID, vo.getStudentId());
        if (org != null && org.getMemberNo() != null) {
            vo.setStudentNo(org.getMemberNo());
        } else if (user != null) {
            vo.setStudentNo(user.getUsername());
        }
    }

    @Override
    public List<SubmissionVO> listByAssignmentId(Long assignmentId) {
        return submissionDao.listByAssignmentId(assignmentId).stream()
                .map(item -> getById(item.getId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id == null) {
            return;
        }
        SubmissionEntity entity = submissionDao.findById(id);
        if (entity == null) {
            return;
        }
        gradingResultDao.deleteBySubmissionId(id);
        submissionAnswerDao.deleteBySubmissionId(id);
        submissionDao.deleteById(id);
    }

    /**
     * 答卷可见性校验（学生端必须能看自己的答卷，但不能看他人答卷）：
     * 1. 答卷归属学生本人 → 放行；
     * 2. 具备批改权限的教师/管理员（assignment:grade / ai:grading）→ 放行；
     * 3. 其余情况拒绝，避免通过遍历 id 越权读取他人作答与评语。
     */
    private void assertCanViewSubmission(SubmissionEntity entity) {
        if (entity == null) {
            return;
        }
        Long currentUserId = UserContext.getUserId();
        if (currentUserId != null && currentUserId.equals(entity.getStudentId())) {
            return;
        }
        boolean canGrade = StpUtil.isLogin()
                && (StpUtil.hasPermission("assignment:grade") || StpUtil.hasPermission("ai:grading"));
        if (canGrade) {
            return;
        }
        throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权查看他人答卷");
    }

    private Long requireUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("未登录");
        }
        return userId;
    }
}
