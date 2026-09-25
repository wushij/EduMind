package com.edumind.ai.service.teaching.impl;

import com.edumind.ai.dto.teaching.LessonContentGenerateDTO;
import com.edumind.ai.integration.llm.LlmClient;
import com.edumind.ai.service.prompt.PromptService;
import com.edumind.ai.service.teaching.LessonContentBlockNormalizer;
import com.edumind.ai.service.teaching.LessonContentGenerateService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.edumind.common.exception.BusinessException;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.api.LessonContentCommandApi;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.course.vo.course.CourseDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LessonContentGenerateServiceImpl implements LessonContentGenerateService {

    private final LlmClient llmClient;
    private final PromptService promptService;
    private final CourseQueryApi courseQueryApi;
    private final LessonContentCommandApi lessonContentCommandApi;
    private final LessonContentBlockNormalizer lessonContentBlockNormalizer;

    @Override
    public String generateAndSaveDraft(LessonContentGenerateDTO dto) {
        CourseDetailVO course = courseQueryApi.getCourseById(dto.getCourseId());
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        ChapterTreeVO lesson = findChapter(courseQueryApi.listChaptersByCourseId(dto.getCourseId()), dto.getLessonChapterId());
        if (lesson == null) {
            throw new BusinessException("课节不存在");
        }
        Map<String, String> vars = new HashMap<>();
        vars.put("course_name", course.getName() != null ? course.getName() : "");
        vars.put("lesson_title", lesson.getTitle() != null ? lesson.getTitle() : "");
        vars.put("lesson_description", lesson.getDescription() != null ? lesson.getDescription() : "");
        vars.put("generation_requirements", StringUtils.hasText(dto.getDepth()) ? dto.getDepth() : "生成完整课节块式正文");
        String systemPrompt = promptService.renderTemplate("lesson_content_blocks", vars);
        String raw = llmClient.chat(systemPrompt, "请输出课节 JSON 内容。");
        String contentJson = sanitizeJson(raw);
        // 落库前剥离正文里重复的课节标题：页面标题已展示课节名，正文再写一遍会显示成两个「1.2 xxx」
        String normalized = lessonContentBlockNormalizer.normalize(contentJson, lesson.getTitle());
        lessonContentCommandApi.saveLessonContentDraft(dto.getCourseId(), dto.getLessonChapterId(), normalized);
        return normalized;
    }

    private ChapterTreeVO findChapter(List<ChapterTreeVO> roots, Long id) {
        if (roots == null || id == null) {
            return null;
        }
        for (ChapterTreeVO root : roots) {
            if (id.equals(root.getId())) {
                return root;
            }
            ChapterTreeVO child = findChapter(root.getChildren(), id);
            if (child != null) {
                return child;
            }
        }
        return null;
    }

    private String sanitizeJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new BusinessException("AI 未返回有效内容");
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```\\s*$", "").trim();
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BusinessException("AI 输出格式不符合课节 JSON 规范");
        }
        String json = trimmed.substring(start, end + 1);
        try {
            JSON.parseObject(json);
        } catch (JSONException ex) {
            throw new BusinessException("AI 输出格式不符合课节 JSON 规范");
        }
        return json;
    }
}
