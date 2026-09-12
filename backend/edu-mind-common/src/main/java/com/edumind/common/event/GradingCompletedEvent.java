package com.edumind.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 批改完成事件，供 statistics 模块更新掌握度与错题记录。
 */
@Getter
public class GradingCompletedEvent extends ApplicationEvent {

    private final Long studentId;
    private final Long courseId;
    private final Long questionId;
    private final Long knowledgePointId;
    private final String questionStem;
    private final String studentAnswer;
    private final String correctAnswer;
    private final int maxScore;
    private final int score;
    private final boolean correct;
    private final double scoreRatio;

    public GradingCompletedEvent(Object source, Long studentId, Long courseId, Long questionId,
                                 Long knowledgePointId, String questionStem, String studentAnswer,
                                 String correctAnswer, int maxScore, int score, boolean correct) {
        super(source);
        this.studentId = studentId;
        this.courseId = courseId;
        this.questionId = questionId;
        this.knowledgePointId = knowledgePointId;
        this.questionStem = questionStem;
        this.studentAnswer = studentAnswer;
        this.correctAnswer = correctAnswer;
        this.maxScore = maxScore;
        this.score = score;
        this.correct = correct;
        this.scoreRatio = maxScore > 0 ? score * 1.0 / maxScore : 0.0;
    }
}
