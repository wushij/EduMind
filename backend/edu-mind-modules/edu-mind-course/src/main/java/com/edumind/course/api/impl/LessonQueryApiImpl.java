package com.edumind.course.api.impl;

import com.edumind.common.exception.BusinessException;
import com.edumind.course.api.LessonQueryApi;
import com.edumind.course.dao.ChapterDao;
import com.edumind.course.entity.ChapterEntity;
import com.edumind.course.service.access.CourseAccessService;
import com.edumind.course.service.lesson.LessonContentMarkdownSupport;
import com.edumind.course.service.lesson.LessonContentMarkdownSupport.ParsedLessonText;
import com.edumind.course.vo.lesson.LessonCopilotContextVO;
import com.edumind.course.vo.lesson.LessonIndexSourceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class LessonQueryApiImpl implements LessonQueryApi {

    private final ChapterDao chapterDao;
    private final CourseAccessService courseAccessService;

    @Override
    public LessonCopilotContextVO getCopilotContext(Long courseId, Long lessonChapterId, boolean previewDraft,
                                                    String retrievalQuery) {
        if (courseId == null || lessonChapterId == null) {
            throw new BusinessException("课程或课节参数不完整");
        }
        courseAccessService.assertCanViewByCourseId(courseId);
        ChapterEntity lesson = chapterDao.findById(lessonChapterId);
        if (lesson == null || !courseId.equals(lesson.getCourseId())) {
            throw new BusinessException("课节不存在");
        }
        if (!previewDraft && !"PUBLISHED".equalsIgnoreCase(lesson.getContentStatus())) {
            throw new BusinessException("课节尚未发布");
        }
        ParsedLessonText parsed = LessonContentMarkdownSupport.parseContentJson(lesson.getContentJson());
        String body = parsed.bodyMarkdown();
        String relevant = LessonContentMarkdownSupport.selectRelevantBody(
                body,
                retrievalQuery,
                LessonContentMarkdownSupport.DEFAULT_MAX_BODY_CHARS);
        boolean truncated = StringUtils.hasText(body) && relevant.length() < body.length();
        return LessonCopilotContextVO.builder()
                .courseId(courseId)
                .lessonChapterId(lessonChapterId)
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .objectivesText(parsed.objectivesText())
                .relevantBodyMarkdown(relevant)
                .totalBodyChars(body != null ? body.length() : 0)
                .bodyTruncated(truncated)
                .build();
    }

    @Override
    public LessonIndexSourceVO getLessonIndexSource(Long courseId, Long lessonChapterId) {
        if (courseId == null || lessonChapterId == null) {
            throw new BusinessException("课程或课节参数不完整");
        }
        ChapterEntity lesson = chapterDao.findById(lessonChapterId);
        if (lesson == null || !courseId.equals(lesson.getCourseId()) || !lesson.isLessonNode()) {
            throw new BusinessException("课节不存在");
        }
        boolean published = "PUBLISHED".equalsIgnoreCase(lesson.getContentStatus());
        ParsedLessonText parsed = LessonContentMarkdownSupport.parseContentJson(lesson.getContentJson());
        String markdown = LessonContentMarkdownSupport.buildIndexMarkdown(
                parsed.objectivesText(), parsed.bodyMarkdown());
        return LessonIndexSourceVO.builder()
                .courseId(courseId)
                .lessonChapterId(lessonChapterId)
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .markdown(markdown)
                .contentHash(LessonContentMarkdownSupport.computeContentHash(markdown))
                .published(published)
                .build();
    }
}
