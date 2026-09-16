package com.edumind.ai.support;

import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class CourseAiContextBuilder {

    private CourseAiContextBuilder() {
    }

    public static String buildContext(
            CourseDetailVO course,
            List<ChapterTreeVO> chapters,
            List<KnowledgePointVO> knowledgePoints) {
        StringBuilder sb = new StringBuilder();
        sb.append("课程名称：").append(course.getName() != null ? course.getName() : "").append('\n');
        if (StringUtils.hasText(course.getCategory())) {
            sb.append("课程分类：").append(course.getCategory()).append('\n');
        }
        if (course.getCredits() != null) {
            sb.append("学分：").append(course.getCredits()).append('\n');
        }
        if (course.getPlannedHours() != null) {
            sb.append("计划学时：").append(course.getPlannedHours()).append('\n');
        }
        if (StringUtils.hasText(course.getDescription())) {
            sb.append("已有简介：").append(course.getDescription()).append('\n');
        }
        List<String> chapterTitles = flattenChapterTitles(chapters);
        if (!chapterTitles.isEmpty()) {
            sb.append("章节大纲：").append(String.join("；", chapterTitles)).append('\n');
        }
        if (!CollectionUtils.isEmpty(knowledgePoints)) {
            String kp = knowledgePoints.stream()
                    .limit(20)
                    .map(KnowledgePointVO::getTitle)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining("；"));
            if (StringUtils.hasText(kp)) {
                sb.append("知识点：").append(kp).append('\n');
            }
        }
        return sb.toString();
    }

    public static List<String> flattenChapterTitles(List<ChapterTreeVO> nodes) {
        List<String> titles = new ArrayList<>();
        if (nodes == null) {
            return titles;
        }
        for (ChapterTreeVO node : nodes) {
            if (node == null) {
                continue;
            }
            if (StringUtils.hasText(node.getTitle())) {
                titles.add(node.getTitle());
            }
            titles.addAll(flattenChapterTitles(node.getChildren()));
        }
        return titles;
    }
}
