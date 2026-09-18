package com.edumind.course.api;

import com.edumind.course.vo.lesson.LessonCopilotContextVO;
import com.edumind.course.vo.lesson.LessonIndexSourceVO;

/**
 * 课节只读查询（供 AI 助教拉取讲义，非知识库 RAG）
 */
public interface LessonQueryApi {

    /**
     * @param previewDraft true=备课草稿；false=学习页仅已发布内容
     * @param retrievalQuery 学生/教师当前提问，用于在长讲义中选取相关段落
     */
    LessonCopilotContextVO getCopilotContext(Long courseId, Long lessonChapterId, boolean previewDraft,
                                             String retrievalQuery);

    /** 课节发布索引用：完整 Markdown + 内容哈希 */
    LessonIndexSourceVO getLessonIndexSource(Long courseId, Long lessonChapterId);
}
