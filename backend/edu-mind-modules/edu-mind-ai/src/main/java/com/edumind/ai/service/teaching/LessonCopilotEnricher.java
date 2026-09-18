package com.edumind.ai.service.teaching;

import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.api.LessonQueryApi;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.course.vo.lesson.LessonCopilotContextVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonCopilotEnricher {

    private final LessonQueryApi lessonQueryApi;
    private final CourseQueryApi courseQueryApi;

    public String buildEnrichmentBlock(Long courseId, Long lessonChapterId, boolean previewDraft, String userQuestion) {
        if (courseId == null || lessonChapterId == null) {
            return "";
        }
        try {
            LessonCopilotContextVO ctx = lessonQueryApi.getCopilotContext(
                    courseId, lessonChapterId, previewDraft, userQuestion);
            StringBuilder sb = new StringBuilder();
            sb.append("【当前课节学习上下文】\n");
            if (StringUtils.hasText(ctx.getTitle())) {
                sb.append("- 课节标题：").append(ctx.getTitle().trim()).append("\n");
            }
            if (StringUtils.hasText(ctx.getDescription())) {
                sb.append("- 课节导读：").append(ctx.getDescription().trim()).append("\n");
            }
            if (StringUtils.hasText(ctx.getObjectivesText())) {
                sb.append("\n【学习目标】\n").append(ctx.getObjectivesText().trim()).append("\n");
            }
            appendSiblingLessons(sb, courseId, lessonChapterId);
            sb.append("\n（回答须优先依据本课讲义检索资料与学习目标，勿编造课文中不存在的内容。）\n");
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    private void appendSiblingLessons(StringBuilder sb, Long courseId, Long lessonChapterId) {
        List<ChapterTreeVO> tree = courseQueryApi.listChaptersByCourseId(courseId);
        if (tree == null || tree.isEmpty()) {
            return;
        }
        List<String> lessonTitles = new ArrayList<>();
        collectLessonTitles(tree, lessonTitles);
        if (lessonTitles.size() <= 1) {
            return;
        }
        sb.append("- 本课程讲次脉络（节选）：");
        int limit = Math.min(lessonTitles.size(), 8);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                sb.append("；");
            }
            sb.append(lessonTitles.get(i));
        }
        sb.append("\n");
    }

    private void collectLessonTitles(List<ChapterTreeVO> nodes, List<String> out) {
        if (nodes == null) {
            return;
        }
        for (ChapterTreeVO node : nodes) {
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                collectLessonTitles(node.getChildren(), out);
            } else if (node.getLessonMeta() != null && StringUtils.hasText(node.getTitle())) {
                out.add(node.getTitle().trim());
            }
        }
    }
}
