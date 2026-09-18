package com.edumind.course.vo.lesson;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LessonCopilotContextVO {

    private Long lessonChapterId;
    private Long courseId;
    private String title;
    private String description;
    /** 学习目标 callout 全文 */
    private String objectivesText;
    /** 按提问选取后的课节正文（Markdown） */
    private String relevantBodyMarkdown;
    private int totalBodyChars;
    private boolean bodyTruncated;
}
