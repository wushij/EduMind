package com.edumind.statistics.listener;

import com.edumind.common.event.LearningActivityEvent;
import com.edumind.statistics.dao.LearningRecordDao;
import com.edumind.statistics.entity.LearningRecordEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LearningActivityListener {

    private final LearningRecordDao learningRecordDao;

    @EventListener
    public void onLearningActivity(LearningActivityEvent event) {
        if (event.getStudentId() == null || event.getCourseId() == null) {
            return;
        }
        LearningRecordEntity entity = new LearningRecordEntity();
        entity.setStudentId(event.getStudentId());
        entity.setCourseId(event.getCourseId());
        entity.setActionType(event.getActionType());
        entity.setDurationMinutes(event.getDurationMinutes());
        entity.setResourceId(event.getResourceId());
        entity.setChapterId(event.getChapterId());
        entity.setKnowledgePointId(event.getKnowledgePointId());
        learningRecordDao.insert(entity);
    }
}
