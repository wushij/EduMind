package com.edumind.course.vo.lesson;

import com.edumind.course.vo.knowledge.KnowledgePointVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonDetailVO implements Serializable {

    private Long id;
    private Long courseId;
    private Long parentChapterId;
    private String parentChapterTitle;
    private String title;
    private String description;
    private Integer durationMinutes;
    private String lessonType;
    private String contentStatus;
    private String contentJson;
    private LocalDateTime publishedAt;

    @Builder.Default
    private List<KnowledgePointVO> knowledgePoints = new ArrayList<>();

    @Builder.Default
    private List<LessonResourceSummaryVO> resources = new ArrayList<>();

    private LessonProgressVO progress;
}
