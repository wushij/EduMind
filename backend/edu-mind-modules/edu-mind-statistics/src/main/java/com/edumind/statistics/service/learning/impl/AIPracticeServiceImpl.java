package com.edumind.statistics.service.learning.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.ai.api.AiChatApi;
import com.edumind.common.event.LearningActivityEvent;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.utils.IdUtil;
import com.edumind.course.api.CourseAccessApi;
import com.edumind.question.api.QuestionQueryApi;
import com.edumind.question.vo.question.QuestionVO;
import com.edumind.statistics.dao.WrongQuestionRecordDao;
import com.edumind.statistics.dto.learning.AiPracticeGradeDTO;
import com.edumind.statistics.dto.learning.AiPracticeStartDTO;
import com.edumind.statistics.dto.learning.AiPracticeSubmitDTO;
import com.edumind.statistics.entity.WrongQuestionRecordEntity;
import com.edumind.statistics.service.analytics.WrongQuestionDiagnosisService;
import com.edumind.statistics.service.learning.AIPracticeService;
import com.edumind.statistics.service.learning.support.AiPracticeAnswerGrader;
import com.edumind.statistics.service.learning.support.AiPracticeQuestionSelector;
import com.edumind.statistics.service.learning.support.AiPracticeSessionContext;
import com.edumind.statistics.service.learning.support.AiPracticeSessionStore;
import com.edumind.statistics.service.analytics.KnowledgeMasteryService;
import com.edumind.statistics.vo.learning.AiPracticeGradeVO;
import com.edumind.statistics.vo.learning.AiPracticeSessionVO;
import com.edumind.statistics.vo.learning.AiPracticeSubmitVO;
import com.edumind.statistics.vo.learning.StudentWrongQuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AIPracticeServiceImpl implements AIPracticeService {

    private final WrongQuestionRecordDao wrongQuestionRecordDao;
    private final QuestionQueryApi questionQueryApi;
    private final KnowledgeMasteryService knowledgeMasteryService;
    private final ApplicationEventPublisher eventPublisher;
    private final AiPracticeQuestionSelector questionSelector;
    private final AiPracticeSessionStore sessionStore;
    private final AiPracticeAnswerGrader answerGrader;
    private final WrongQuestionDiagnosisService wrongQuestionDiagnosisService;
    private final CourseAccessApi courseAccessApi;
    private final AiChatApi aiChatApi;

    @Override
    public StudentWrongQuestionVO listWrongQuestions(Long studentId, Long courseId, int page, int pageSize) {
        Page<WrongQuestionRecordEntity> result = wrongQuestionRecordDao.pageByStudent(
                new Page<>(page, pageSize), studentId, courseId);
        StudentWrongQuestionVO vo = new StudentWrongQuestionVO();
        vo.setTotal(result.getTotal());
        for (WrongQuestionRecordEntity entity : result.getRecords()) {
            StudentWrongQuestionVO.Item item = new StudentWrongQuestionVO.Item();
            item.setId(entity.getId());
            item.setQuestionId(entity.getQuestionId());
            item.setKnowledgePointId(entity.getKnowledgePointId());
            item.setWrongCount(entity.getWrongCount());
            item.setDiagnosis(entity.getDiagnosis());
            if (StringUtils.hasText(entity.getErrorTypes())) {
                item.setErrorTypes(Arrays.stream(entity.getErrorTypes().split(","))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toList()));
            }
            vo.getList().add(item);
        }
        return vo;
    }

    @Override
    public AiPracticeSessionVO startPractice(Long studentId, AiPracticeStartDTO dto) {
        if (dto.getCourseId() == null) {
            throw new BusinessException("课程ID不能为空");
        }
        courseAccessApi.assertCanView(dto.getCourseId());

        AiPracticeQuestionSelector.SelectionResult selection = questionSelector.select(studentId, dto);
        if (CollectionUtils.isEmpty(selection.getQuestions())) {
            throw new BusinessException("暂无可用练习题，请先完成课程作业或联系教师补充题库");
        }

        String sessionId = "practice-" + IdUtil.simpleUUID();
        AiPracticeSessionContext ctx = new AiPracticeSessionContext();
        ctx.setStudentId(studentId);
        ctx.setCourseId(dto.getCourseId());
        ctx.setMode(StringUtils.hasText(dto.getMode()) ? dto.getMode() : "WEAK_POINT");
        ctx.setInstantFeedback(dto.getInstantFeedback() == null || Boolean.TRUE.equals(dto.getInstantFeedback()));
        ctx.setQuestionIds(selection.getQuestions().stream().map(QuestionVO::getId).collect(Collectors.toList()));
        sessionStore.save(sessionId, ctx);

        AiPracticeSessionVO vo = new AiPracticeSessionVO();
        vo.setSessionId(sessionId);
        vo.setCourseId(dto.getCourseId());
        vo.setQuestionCount(selection.getQuestions().size());
        vo.setQuestions(selection.getQuestions());
        vo.setWeakPointHint(selection.getWeakPointHint());
        vo.setEstimatedMinutes(selection.getEstimatedMinutes());
        vo.setWeakKnowledgePointCount(selection.getWeakKnowledgePointCount());
        vo.setPendingWrongQuestionCount(selection.getPendingWrongQuestionCount());

        eventPublisher.publishEvent(new LearningActivityEvent(
                this, studentId, dto.getCourseId(), "AI_PRACTICE_START", 0, null));
        return vo;
    }

    @Override
    public AiPracticeGradeVO gradeAnswer(Long studentId, AiPracticeGradeDTO dto) {
        AiPracticeSessionContext ctx = sessionStore.require(dto.getSessionId(), studentId);
        if (!ctx.getQuestionIds().contains(dto.getQuestionId())) {
            throw new BusinessException("题目不属于当前练习会话");
        }
        QuestionVO question = questionQueryApi.getQuestionById(dto.getQuestionId());
        if (question == null) {
            throw new BusinessException("题目不存在");
        }
        return answerGrader.grade(question, dto.getStudentAnswer());
    }

    @Override
    public AiPracticeSubmitVO submitPractice(Long studentId, AiPracticeSubmitDTO dto) {
        AiPracticeSessionContext ctx = sessionStore.require(dto.getSessionId(), studentId);
        if (dto.getCourseId() != null && !dto.getCourseId().equals(ctx.getCourseId())) {
            throw new BusinessException("课程与练习会话不匹配");
        }
        Long courseId = ctx.getCourseId();

        Map<Long, String> answerMap = new LinkedHashMap<>();
        if (dto.getAnswers() != null) {
            for (AiPracticeSubmitDTO.AnswerItem item : dto.getAnswers()) {
                if (item.getQuestionId() != null) {
                    answerMap.put(item.getQuestionId(), item.getAnswer());
                }
            }
        }

        int correct = 0;
        List<AiPracticeSubmitVO.QuestionResultItem> results = new ArrayList<>();
        Set<Long> wrongIds = new LinkedHashSet<>();
        Set<Long> weakKpIds = new LinkedHashSet<>();

        for (Long questionId : ctx.getQuestionIds()) {
            QuestionVO question = questionQueryApi.getQuestionById(questionId);
            if (question == null) {
                continue;
            }
            String studentAnswer = answerMap.getOrDefault(questionId, "");
            AiPracticeGradeVO graded = answerGrader.grade(question, studentAnswer);

            AiPracticeSubmitVO.QuestionResultItem item = new AiPracticeSubmitVO.QuestionResultItem();
            item.setQuestionId(questionId);
            item.setStudentAnswer(studentAnswer);
            item.setCorrect(Boolean.TRUE.equals(graded.getCorrect()));
            item.setReferenceAnswer(graded.getReferenceAnswer());
            item.setAnalysis(graded.getAnalysis());
            item.setKnowledgePointName(graded.getKnowledgePointName());
            results.add(item);

            if (Boolean.TRUE.equals(graded.getCorrect())) {
                correct++;
                if (question.getKnowledgePointId() != null) {
                    knowledgeMasteryService.upsertMastery(studentId, courseId, question.getKnowledgePointId(), 1.0);
                }
            } else {
                wrongIds.add(questionId);
                if (question.getKnowledgePointId() != null) {
                    weakKpIds.add(question.getKnowledgePointId());
                    knowledgeMasteryService.upsertMastery(studentId, courseId, question.getKnowledgePointId(), 0.4);
                }
                wrongQuestionDiagnosisService.recordWrong(
                        studentId,
                        courseId,
                        questionId,
                        question.getKnowledgePointId(),
                        graded.getAnalysis(),
                        studentAnswer);
            }
        }

        int total = ctx.getQuestionIds().size();
        int duration = normalizeDuration(dto.getDurationSeconds(), total);

        AiPracticeSubmitVO vo = new AiPracticeSubmitVO();
        vo.setSessionId(dto.getSessionId());
        vo.setTotalCount(total);
        vo.setCorrectCount(correct);
        vo.setAccuracyRate(total == 0 ? 0.0 : correct * 100.0 / total);
        vo.setDurationSeconds(duration);
        vo.setWrongQuestionIds(new ArrayList<>(wrongIds));
        vo.setWeakKnowledgePointIds(new ArrayList<>(weakKpIds));
        vo.setQuestionResults(results);
        vo.setAiSummary(buildAiSummary(correct, total, weakKpIds.size()));

        sessionStore.remove(dto.getSessionId());

        eventPublisher.publishEvent(new LearningActivityEvent(
                this, studentId, courseId, "AI_PRACTICE_END", Math.max(1, total), null));
        return vo;
    }

    private int normalizeDuration(Integer clientSeconds, int questionCount) {
        int max = Math.max(questionCount * 600, 3600);
        if (clientSeconds == null || clientSeconds <= 0) {
            return Math.max(60, questionCount * 90);
        }
        return Math.min(clientSeconds, max);
    }

    private String buildAiSummary(int correct, int total, int weakKpCount) {
        if (total <= 0) {
            return "本次练习已结束，建议继续巩固薄弱考点。";
        }
        double rate = correct * 100.0 / total;
        String base = String.format(Locale.ROOT,
                "本次共完成 %d 题，正确 %d 题，正确率 %.0f%%。", total, correct, rate);
        try {
            String prompt = base + " 涉及薄弱考点约 " + weakKpCount
                    + " 个。请用 2 句话给出学习建议，语气专业简洁。";
            return aiChatApi.chat("LEARNING", "你是学习导师。", prompt);
        } catch (Exception ex) {
            if (rate >= 80) {
                return base + " 掌握情况良好，可适度挑战更高认知层级题目。";
            }
            return base + " 建议针对错题本中的考点进行变式复练，48 小时后再测一轮。";
        }
    }
}
