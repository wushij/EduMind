package com.edumind.teaching.service.submission.impl;

import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.UserContext;
import com.edumind.infrastructure.redis.DistributedLockService;
import com.edumind.infrastructure.redis.RedisKeyBuilder;
import com.edumind.teaching.dao.AssignmentDao;
import com.edumind.teaching.dao.SubmissionAnswerDao;
import com.edumind.teaching.dao.SubmissionDao;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final AssignmentDao assignmentDao;
    private final SubmissionDao submissionDao;
    private final SubmissionAnswerDao submissionAnswerDao;
    private final GradingService gradingService;
    private final DistributedLockService distributedLockService;
    private final TransactionTemplate transactionTemplate;

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

        SubmissionEntity submission = submissionDao.findByAssignmentAndStudent(assignmentId, studentId);
        if (submission == null) {
            submission = new SubmissionEntity();
            submission.setAssignmentId(assignmentId);
            submission.setStudentId(studentId);
            submission.setStatus("IN_PROGRESS");
            submissionDao.insert(submission);
        }

        submissionAnswerDao.deleteBySubmissionId(submission.getId());
        for (SubmissionAnswerItemDTO item : dto.getAnswers()) {
            SubmissionAnswerEntity answer = new SubmissionAnswerEntity();
            answer.setSubmissionId(submission.getId());
            answer.setQuestionId(item.getQuestionId());
            answer.setAnswer(item.getAnswer());
            submissionAnswerDao.insert(answer);
        }

        submission.setStatus("SUBMITTED");
        submission.setSubmitTime(LocalDateTime.now());
        submissionDao.updateById(submission);

        gradingService.gradeSubmission(submission.getId());
        return getById(submission.getId());
    }

    @Override
    public SubmissionVO getById(Long id) {
        SubmissionEntity entity = submissionDao.findById(id);
        if (entity == null) {
            throw new BusinessException("提交记录不存在");
        }
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
        return vo;
    }

    @Override
    public List<SubmissionVO> listByAssignmentId(Long assignmentId) {
        return submissionDao.listByAssignmentId(assignmentId).stream()
                .map(item -> getById(item.getId()))
                .collect(Collectors.toList());
    }

    private Long requireUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("未登录");
        }
        return userId;
    }
}
