package com.edumind.statistics.service.learning.support;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiPracticeSessionContext {
    private Long studentId;
    private Long courseId;
    private String mode;
    private boolean instantFeedback;
    private List<Long> questionIds = new ArrayList<>();
    private long createdAtMs = System.currentTimeMillis();
}
