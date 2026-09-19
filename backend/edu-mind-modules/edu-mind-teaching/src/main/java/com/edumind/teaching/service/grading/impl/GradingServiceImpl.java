package com.edumind.teaching.service.grading.impl;

import com.edumind.ai.dto.SubjectiveGradingDTO;
import com.edumind.ai.api.AiGradingApi;
import com.edumind.ai.vo.SubjectiveGradingVO;
import com.edumind.common.enums.QuestionType;
import com.edumind.common.event.GradingCompletedEvent;
import com.edumind.common.exception.BusinessException;
import com.edumind.infrastructure.redis.DistributedLockService;
import com.edumind.infrastructure.redis.RedisKeyBuilder;
import com.edumind.notification.api.NotificationWriteApi;
import com.edumind.question.api.QuestionQueryApi;
import com.edumind.question.vo.question.QuestionVO;
import com.edumind.teaching.converter.AssignmentConverter;
import com.edumind.teaching.dao.AssignmentDao;
import com.edumind.teaching.dao.ExamQuestionDao;
import com.edumind.teaching.dao.GradingResultDao;
import com.edumind.teaching.dao.SubmissionAnswerDao;
import com.edumind.teaching.dao.SubmissionDao;
import com.edumind.teaching.dto.submission.GradingReviewDTO;
import com.edumind.teaching.dto.submission.GradingReviewItemDTO;
import com.edumind.teaching.entity.AssignmentEntity;
import com.edumind.teaching.entity.ExamQuestionEntity;
import com.edumind.teaching.entity.GradingResultEntity;
import com.edumind.teaching.entity.SubmissionAnswerEntity;
import com.edumind.teaching.entity.SubmissionEntity;
import com.edumind.teaching.service.grading.GradingService;
import com.edumind.teaching.vo.assignment.AssignmentSettingsVO;
import com.edumind.teaching.vo.submission.GradingItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradingServiceImpl implements GradingService {

    private static final long DEFAULT_TENANT_ID = 1L;

    private final SubmissionDao submissionDao;
    private final SubmissionAnswerDao submissionAnswerDao;
    private final AssignmentDao assignmentDao;
    private final ExamQuestionDao examQuestionDao;
    private final GradingResultDao gradingResultDao;
    private final QuestionQueryApi questionQueryApi;
    private final AiGradingApi aiGradingApi;
    private final DistributedLockService distributedLockService;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final AssignmentConverter assignmentConverter;
    private final NotificationWriteApi notificationWriteApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void gradeSubmission(Long submissionId) {
        SubmissionEntity submission = submissionDao.findById(submissionId);
        if (submission == null) {
            throw new BusinessException("提交记录不存在");
        }
        AssignmentEntity assignment = assignmentDao.findById(submission.getAssignmentId());
        if (assignment == null || assignment.getExamId() == null) {
            throw new BusinessException("作业未关联试卷，无法批改");
        }

        AssignmentSettingsVO settings = assignmentConverter.parseSettings(assignment.getSettingsJson());
        boolean aiEnabled = settings == null || !Boolean.FALSE.equals(settings.getAiGradingEnabled());

        gradingResultDao.deleteBySubmissionId(submissionId);

        Map<Long, ExamQuestionEntity> examQuestionMap = examQuestionDao.listByExamId(assignment.getExamId()).stream()
                .collect(Collectors.toMap(ExamQuestionEntity::getQuestionId, q -> q, (a, b) -> a));
        List<SubmissionAnswerEntity> answers = submissionAnswerDao.listBySubmissionId(submissionId);

        int totalScore = 0;
        int maxScore = 0;
        boolean pendingManual = false;

        for (SubmissionAnswerEntity answer : answers) {
            QuestionVO question = questionQueryApi.getQuestionById(answer.getQuestionId());
            if (question == null) {
                continue;
            }
            ExamQuestionEntity examQuestion = examQuestionMap.get(answer.getQuestionId());
            int questionMaxScore = examQuestion != null ? examQuestion.getScore() : question.getScore();
            maxScore += questionMaxScore;

            GradingResultEntity result = new GradingResultEntity();
            result.setSubmissionId(submissionId);
            result.setQuestionId(answer.getQuestionId());
            result.setMaxScore(questionMaxScore);

            if (isObjectiveType(question.getType())) {
                boolean correct = StringUtils.hasText(answer.getAnswer())
                        && answer.getAnswer().trim().equalsIgnoreCase(question.getAnswer().trim());
                result.setIsCorrect(correct ? 1 : 0);
                result.setScore(correct ? questionMaxScore : 0);
                result.setAiComment(correct ? "回答正确" : "回答错误");
                result.setStatus("AUTO_GRADED");
            } else if (aiEnabled) {
                SubjectiveGradingDTO gradingDTO = new SubjectiveGradingDTO();
                gradingDTO.setQuestionId(question.getId());
                gradingDTO.setQuestionStem(question.getStem());
                gradingDTO.setReferenceAnswer(question.getAnswer());
                gradingDTO.setStudentAnswer(answer.getAnswer());
                gradingDTO.setMaxScore(questionMaxScore);
                SubjectiveGradingVO aiResult = aiGradingApi.gradeSubjective(gradingDTO);
                result.setScore(aiResult.getScore());
                result.setAiComment(aiResult.getAiComment());
                result.setStatus(aiResult.getStatus());
                result.setIsCorrect(null);
            } else {
                result.setScore(null);
                result.setAiComment("主观题待教师人工评阅");
                result.setStatus("PENDING_MANUAL");
                result.setIsCorrect(null);
                pendingManual = true;
            }
            totalScore += result.getScore() != null ? result.getScore() : 0;
            gradingResultDao.insert(result);
            publishGradingEvent(submission, assignment, question, answer, result);
        }

        submission.setTotalScore(totalScore);
        submission.setMaxScore(maxScore);
        submission.setStatus(pendingManual ? "SUBMITTED" : "GRADED");
        submissionDao.updateById(submission);
    }

    @Override
    public List<GradingItemVO> getGradingResults(Long submissionId) {
        List<GradingItemVO> items = new ArrayList<>();
        for (GradingResultEntity entity : gradingResultDao.listBySubmissionId(submissionId)) {
            GradingItemVO vo = new GradingItemVO();
            vo.setQuestionId(entity.getQuestionId());
            vo.setScore(entity.getScore());
            vo.setMaxScore(entity.getMaxScore());
            vo.setIsCorrect(entity.getIsCorrect() == null ? null : entity.getIsCorrect() == 1);
            vo.setAiComment(entity.getAiComment());
            vo.setTeacherComment(entity.getTeacherComment());
            vo.setStatus(entity.getStatus());
            QuestionVO question = questionQueryApi.getQuestionById(entity.getQuestionId());
            if (question != null) {
                vo.setType(question.getType());
                vo.setStem(question.getStem());
                vo.setStandardAnswer(question.getAnswer());
                vo.setAnalysis(question.getAnalysis());
            }
            items.add(vo);
        }
        return items;
    }

    @Override
    public void reviewGrading(Long submissionId, GradingReviewDTO dto) {
        String lockKey = RedisKeyBuilder.lock("grading:review", String.valueOf(submissionId));
        distributedLockService.executeWithLock(lockKey, 3, 30,
                () -> transactionTemplate.executeWithoutResult(status -> doReviewGrading(submissionId, dto)));
    }

    protected void doReviewGrading(Long submissionId, GradingReviewDTO dto) {
        SubmissionEntity submission = submissionDao.findById(submissionId);
        if (submission == null) {
            throw new BusinessException("提交记录不存在");
        }
        AssignmentEntity assignment = assignmentDao.findById(submission.getAssignmentId());
        int totalScore = 0;
        for (GradingReviewItemDTO item : dto.getItems()) {
            GradingResultEntity result = gradingResultDao.findBySubmissionIdAndQuestionId(submissionId, item.getQuestionId());
            if (result == null) {
                throw new BusinessException("题目批改记录不存在: " + item.getQuestionId());
            }
            result.setScore(item.getScore());
            result.setTeacherComment(item.getTeacherComment());
            result.setStatus("TEACHER_REVIEWED");
            gradingResultDao.updateById(result);
            totalScore += item.getScore() != null ? item.getScore() : 0;
        }
        submission.setTotalScore(totalScore);
        submission.setStatus("REVIEWED");
        submissionDao.updateById(submission);

        if (assignment != null && submission.getStudentId() != null) {
            String title = "作业成绩已发布：" + assignment.getTitle();
            String content = "您的作业已复核完成，总成绩 " + totalScore + " 分。"
                    + (submission.getMaxScore() != null ? "（满分 " + submission.getMaxScore() + " 分）" : "");
            notificationWriteApi.sendToUsers(
                    DEFAULT_TENANT_ID,
                    List.of(submission.getStudentId()),
                    title,
                    content,
                    "ASSIGNMENT",
                    assignment.getId());
        }
    }

    private void publishGradingEvent(SubmissionEntity submission, AssignmentEntity assignment,
                                     QuestionVO question, SubmissionAnswerEntity answer,
                                     GradingResultEntity result) {
        if (question.getKnowledgePointId() == null) {
            return;
        }
        int score = result.getScore() != null ? result.getScore() : 0;
        boolean correct = result.getIsCorrect() != null && result.getIsCorrect() == 1;
        if (result.getIsCorrect() == null && questionMaxScore(result) > 0) {
            correct = score >= questionMaxScore(result) * 0.6;
        }
        eventPublisher.publishEvent(new GradingCompletedEvent(
                this,
                submission.getStudentId(),
                assignment.getCourseId(),
                question.getId(),
                question.getKnowledgePointId(),
                question.getStem(),
                answer.getAnswer(),
                question.getAnswer(),
                questionMaxScore(result),
                score,
                correct
        ));
    }

    private int questionMaxScore(GradingResultEntity result) {
        return result.getMaxScore() != null ? result.getMaxScore() : 0;
    }

    private boolean isObjectiveType(String type) {
        return QuestionType.SINGLE_CHOICE.getCode().equals(type)
                || QuestionType.MULTIPLE_CHOICE.getCode().equals(type)
                || QuestionType.TRUE_FALSE.getCode().equals(type)
                || QuestionType.FILL_BLANK.getCode().equals(type);
    }
}
