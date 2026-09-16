package com.edumind.ai.service.tool.impl;

import com.edumind.ai.dto.tool.LessonPlanDTO;
import com.edumind.ai.integration.llm.LlmClient;
import com.edumind.ai.service.tool.LessonPlanService;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.course.vo.course.CourseDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class LessonPlanServiceImpl implements LessonPlanService {

    private final LlmClient llmClient;
    private final CourseQueryApi courseQueryApi;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public String generate(LessonPlanDTO dto) {
        return llmClient.chat(systemPrompt(), buildUserPrompt(dto));
    }

    @Override
    public SseEmitter streamGenerate(LessonPlanDTO dto) {
        SseEmitter emitter = new SseEmitter(120_000L);
        executor.execute(() -> {
            try {
                llmClient.streamChat(systemPrompt(), buildUserPrompt(dto), new LlmClient.StreamCallback() {
                    @Override
                    public void onChunk(String content) {
                        try {
                            emitter.send(SseEmitter.event().name("delta").data("{\"content\":\"" + escape(content) + "\"}"));
                        } catch (IOException ex) {
                            emitter.completeWithError(ex);
                        }
                    }

                    @Override
                    public void onComplete() {
                        try {
                            emitter.send(SseEmitter.event().name("done").data("{}"));
                            emitter.complete();
                        } catch (IOException ex) {
                            emitter.completeWithError(ex);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        emitter.completeWithError(new IllegalStateException(message));
                    }
                });
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }

    private String systemPrompt() {
        return "你是资深高校教学设计师，请输出结构清晰、可直接用于课堂的 Markdown 教案。";
    }

    private String buildUserPrompt(LessonPlanDTO dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("课程ID：").append(dto.getCourseId()).append('\n');
        CourseDetailVO course = dto.getCourseId() != null ? courseQueryApi.getCourseById(dto.getCourseId()) : null;
        if (course != null) {
            sb.append("课程名称：").append(course.getName()).append('\n');
            if (StringUtils.hasText(course.getDescription())) {
                sb.append("课程简介：").append(course.getDescription()).append('\n');
            }
            if (course.getPlannedHours() != null) {
                sb.append("计划学时：").append(course.getPlannedHours()).append('\n');
            }
            List<String> chapterTitles = flattenChapterTitles(
                    courseQueryApi.listChaptersByCourseId(dto.getCourseId()));
            if (!chapterTitles.isEmpty()) {
                sb.append("章节大纲：").append(String.join("；", chapterTitles)).append('\n');
            }
        }
        sb.append("授课主题：").append(dto.getTopic()).append('\n');
        sb.append("本节学时：").append(dto.getHours() != null ? dto.getHours() : 2).append('\n');
        sb.append("教学目标：")
                .append(dto.getObjectives() != null ? dto.getObjectives() : "掌握核心概念")
                .append('\n');
        sb.append("请结合上述真实课程信息生成：教学大纲、课堂流程、互动提问、作业建议。");
        return sb.toString();
    }

    private List<String> flattenChapterTitles(List<ChapterTreeVO> nodes) {
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

    private String escape(String token) {
        return token.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
