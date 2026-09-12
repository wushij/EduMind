package com.edumind.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 学习行为事件，供 statistics 模块写入 learning_record。
 */
@Getter
public class LearningActivityEvent extends ApplicationEvent {

    private final Long studentId;
    private final Long courseId;
    private final String actionType;
    private final int durationMinutes;
    private final Long resourceId;

    public LearningActivityEvent(Object source, Long studentId, Long courseId,
                                 String actionType, int durationMinutes, Long resourceId) {
        super(source);
        this.studentId = studentId;
        this.courseId = courseId;
        this.actionType = actionType;
        this.durationMinutes = durationMinutes;
        this.resourceId = resourceId;
    }
}
