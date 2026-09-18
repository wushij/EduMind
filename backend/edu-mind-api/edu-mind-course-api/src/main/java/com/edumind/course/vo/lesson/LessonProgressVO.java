package com.edumind.course.vo.lesson;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonProgressVO implements Serializable {

    private String status;
    private Integer progressPercent;
    private String lastBlockId;
    private LocalDateTime lastStudyAt;
    private LocalDateTime completedAt;
}
