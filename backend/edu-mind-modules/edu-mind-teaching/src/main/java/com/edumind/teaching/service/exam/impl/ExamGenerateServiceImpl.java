package com.edumind.teaching.service.exam.impl;

import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.UserContext;
import com.edumind.infrastructure.redis.cache.AiSessionCacheService;
import com.edumind.question.api.QuestionQueryApi;
import com.edumind.question.vo.question.QuestionVO;
import com.edumind.teaching.converter.ExamConverter;
import com.edumind.teaching.dto.exam.ExamGenerateDTO;
import com.edumind.teaching.dto.exam.ExamRuleDTO;
import com.edumind.teaching.entity.ExamQuestionEntity;
import com.edumind.teaching.service.exam.ExamGenerateService;
import com.edumind.teaching.vo.exam.ExamPreviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamGenerateServiceImpl implements ExamGenerateService {

    private static final long GENERATING_TTL_SECONDS = 120L;

    private final QuestionQueryApi questionQueryApi;
    private final ExamConverter examConverter;
    private final AiSessionCacheService aiSessionCacheService;

    @Override
    public ExamPreviewVO generate(ExamGenerateDTO dto) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("未登录");
        }
        if (!aiSessionCacheService.tryStartGenerating("exam", userId, GENERATING_TTL_SECONDS)) {
            throw new BusinessException("试卷正在生成中，请稍候");
        }
        try {
            List<ExamQuestionEntity> examQuestions = new ArrayList<>();
            int sortOrder = 1;
            int calculatedScore = 0;

            for (ExamRuleDTO rule : dto.getRules()) {
                List<?> rawQuestions = questionQueryApi.listQuestionsByCourseAndTypes(
                        dto.getCourseId(),
                        Collections.singletonList(rule.getType()),
                        rule.getCount() * 3
                );
                List<QuestionVO> candidates = rawQuestions.stream()
                        .filter(QuestionVO.class::isInstance)
                        .map(QuestionVO.class::cast)
                        .collect(Collectors.toList());
                if (candidates.size() < rule.getCount()) {
                    throw new BusinessException("题型 " + rule.getType() + " 可用题目不足，需要 "
                            + rule.getCount() + " 道，当前仅有 " + candidates.size() + " 道");
                }
                Collections.shuffle(candidates);
                for (int i = 0; i < rule.getCount(); i++) {
                    QuestionVO question = candidates.get(i);
                    ExamQuestionEntity item = new ExamQuestionEntity();
                    item.setQuestionId(question.getId());
                    item.setScore(rule.getScoreEach());
                    item.setSortOrder(sortOrder++);
                    examQuestions.add(item);
                    calculatedScore += rule.getScoreEach();
                }
            }

            ExamPreviewVO preview = new ExamPreviewVO();
            preview.setTitle(dto.getTitle());
            preview.setTotalScore(dto.getTotalScore());
            preview.setCalculatedScore(calculatedScore);
            preview.setScoreMatched(calculatedScore == dto.getTotalScore());
            preview.setDurationMinutes(dto.getDurationMinutes());
            preview.setQuestions(examConverter.toExamQuestionVOList(examQuestions));
            return preview;
        } finally {
            aiSessionCacheService.finishGenerating("exam", userId);
        }
    }
}
