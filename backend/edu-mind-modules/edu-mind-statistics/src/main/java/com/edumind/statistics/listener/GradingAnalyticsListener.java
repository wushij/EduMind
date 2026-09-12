package com.edumind.statistics.listener;

import com.edumind.common.event.GradingCompletedEvent;
import com.edumind.statistics.dao.WrongQuestionRecordDao;
import com.edumind.statistics.entity.WrongQuestionRecordEntity;
import com.edumind.statistics.service.analytics.WrongQuestionDiagnosisService;
import com.edumind.statistics.service.analytics.impl.KnowledgeMasteryServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GradingAnalyticsListener {

    private final KnowledgeMasteryServiceImpl knowledgeMasteryService;
    private final WrongQuestionDiagnosisService wrongQuestionDiagnosisService;
    private final WrongQuestionRecordDao wrongQuestionRecordDao;

    @EventListener
    public void onGradingCompleted(GradingCompletedEvent event) {
        knowledgeMasteryService.upsertMastery(
                event.getStudentId(),
                event.getCourseId(),
                event.getKnowledgePointId(),
                event.isCorrect() ? event.getScoreRatio() : event.getScoreRatio() * 0.5
        );
        if (!event.isCorrect() && event.getQuestionId() != null) {
            handleWrongQuestion(event);
        }
    }

    private void handleWrongQuestion(GradingCompletedEvent event) {
        WrongQuestionRecordEntity existing = wrongQuestionRecordDao.findByStudentAndQuestion(
                event.getStudentId(), event.getQuestionId());
        if (existing != null) {
            existing.setWrongCount(existing.getWrongCount() + 1);
            wrongQuestionRecordDao.updateById(existing);
            return;
        }
        String diagnosis = wrongQuestionDiagnosisService.diagnose(
                event.getQuestionStem(),
                event.getStudentAnswer(),
                event.getCorrectAnswer()
        );
        wrongQuestionDiagnosisService.recordWrong(
                event.getStudentId(),
                event.getCourseId(),
                event.getQuestionId(),
                event.getKnowledgePointId(),
                diagnosis
        );
    }
}
