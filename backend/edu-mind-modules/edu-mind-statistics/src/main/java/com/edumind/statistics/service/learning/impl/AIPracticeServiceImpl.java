package com.edumind.statistics.service.learning.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.statistics.api.KnowledgeMasteryQueryApi;
import com.edumind.common.event.LearningActivityEvent;
import com.edumind.common.utils.IdUtil;
import com.edumind.question.api.QuestionQueryApi;
import com.edumind.question.vo.question.QuestionVO;
import com.edumind.statistics.dao.WrongQuestionRecordDao;
import com.edumind.statistics.dto.learning.AiPracticeStartDTO;
import com.edumind.statistics.dto.learning.AiPracticeSubmitDTO;
import com.edumind.statistics.entity.WrongQuestionRecordEntity;
import com.edumind.statistics.service.analytics.KnowledgeMasteryService;
import com.edumind.statistics.service.learning.AIPracticeService;
import com.edumind.statistics.vo.learning.AiPracticeSessionVO;
import com.edumind.statistics.vo.learning.AiPracticeSubmitVO;
import com.edumind.statistics.vo.learning.StudentWrongQuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AIPracticeServiceImpl implements AIPracticeService {

    private final WrongQuestionRecordDao wrongQuestionRecordDao;
    private final QuestionQueryApi questionQueryApi;
    private final KnowledgeMasteryQueryApi knowledgeMasteryQueryApi;
    private final KnowledgeMasteryService knowledgeMasteryService;
    private final ApplicationEventPublisher eventPublisher;

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
        int count = dto.getCount() != null && dto.getCount() > 0 ? dto.getCount() : 5;
        Long courseId = dto.getCourseId();

        Set<Long> weakKpIds = knowledgeMasteryQueryApi.getMasteryByStudentAndCourse(studentId, courseId).entrySet()
                .stream()
                .filter(e -> e.getValue() < 0.7)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        List<QuestionVO> pool = questionQueryApi.listQuestionsByCourseId(courseId);
        if (pool == null) {
            pool = List.of();
        }

        List<QuestionVO> candidates = pool.stream()
                .filter(q -> weakKpIds.isEmpty() || (q.getKnowledgePointId() != null && weakKpIds.contains(q.getKnowledgePointId())))
                .collect(Collectors.toCollection(ArrayList::new));
        if (candidates.isEmpty()) {
            candidates = new ArrayList<>(pool);
        }
        Collections.shuffle(candidates);

        List<QuestionVO> selected = candidates.stream().limit(count).collect(Collectors.toList());
        AiPracticeSessionVO vo = new AiPracticeSessionVO();
        vo.setSessionId("practice-" + IdUtil.simpleUUID());
        vo.setCourseId(courseId);
        vo.setQuestionCount(selected.size());
        vo.setQuestions(selected);

        eventPublisher.publishEvent(new LearningActivityEvent(
                this, studentId, courseId, "AI_PRACTICE_START", 0, null));
        return vo;
    }

    @Override
    public AiPracticeSubmitVO submitPractice(Long studentId, AiPracticeSubmitDTO dto) {
        int total = dto.getAnswers() != null ? dto.getAnswers().size() : 0;
        int correct = 0;
        if (dto.getAnswers() != null) {
            for (AiPracticeSubmitDTO.AnswerItem item : dto.getAnswers()) {
                boolean isCorrect = Boolean.TRUE.equals(item.getCorrect());
                if (isCorrect) {
                    correct++;
                }
                if (item.getKnowledgePointId() != null) {
                    knowledgeMasteryService.upsertMastery(
                            studentId,
                            dto.getCourseId(),
                            item.getKnowledgePointId(),
                            isCorrect ? 1.0 : 0.4
                    );
                }
            }
        }

        eventPublisher.publishEvent(new LearningActivityEvent(
                this, studentId, dto.getCourseId(), "AI_PRACTICE_END", Math.max(1, total), null));

        AiPracticeSubmitVO vo = new AiPracticeSubmitVO();
        vo.setTotalCount(total);
        vo.setCorrectCount(correct);
        vo.setAccuracyRate(total == 0 ? 0.0 : correct * 100.0 / total);
        vo.setDurationSeconds(total * 60);
        return vo;
    }
}
