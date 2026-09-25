package com.edumind.ai.service.tool.impl;

import com.edumind.ai.api.AiChatApi;
import com.edumind.ai.dto.tool.CourseDescriptionSuggestDTO;
import com.edumind.ai.dto.tool.CourseDescriptionSuggestResultVO;
import com.edumind.ai.service.tool.CourseDescriptionSuggestService;
import com.edumind.ai.support.CourseAiContextBuilder;
import com.edumind.common.exception.BusinessException;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseDescriptionSuggestServiceImpl implements CourseDescriptionSuggestService {

    private static final String SCENE = "COURSE_DESCRIPTION";

    private final AiChatApi aiChatApi;
    private final CourseQueryApi courseQueryApi;

    @Override
    public CourseDescriptionSuggestResultVO suggest(CourseDescriptionSuggestDTO dto) {
        CourseDetailVO course = courseQueryApi.getCourseById(dto.getCourseId());
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        List<ChapterTreeVO> chapters = courseQueryApi.listChaptersByCourseId(dto.getCourseId());
        List<KnowledgePointVO> knowledgePoints = courseQueryApi.listKnowledgePointsByCourseId(dto.getCourseId());

        String systemPrompt = "你是高校课程建设专家，擅长撰写课程简介与修读要求。"
                + "只输出正文纯文本，不要 Markdown，不要 JSON，不超过 480 字。";
        String userPrompt = CourseAiContextBuilder.buildContext(course, chapters, knowledgePoints)
                + "\n请撰写「课程简介与修读要求」：包含培养目标、前置要求、学习建议，语气专业。";

        try {
            log.info("[AI Course Description] 调用网关生成课程简介 courseId={}", dto.getCourseId());
            String raw = aiChatApi.chat(SCENE, dto.getCourseId(), systemPrompt, userPrompt);
            String text = normalizeText(raw);
            if (StringUtils.hasText(text)) {
                return CourseDescriptionSuggestResultVO.builder()
                        .text(text)
                        .aiGenerated(true)
                        .sourceLabel("模型推演已生成")
                        .build();
            }
        } catch (Exception ex) {
            log.warn("[AI Course Description] 模型调用失败，启用兜底: {}", ex.getMessage());
        }

        return CourseDescriptionSuggestResultVO.builder()
                .text(buildFallbackDescription(course, chapters))
                .aiGenerated(false)
                .sourceLabel("上下文智能兜底（模型暂不可用）")
                .build();
    }

    private String normalizeText(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String text = raw.trim();
        if (text.startsWith("```")) {
            text = text.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```\\s*$", "").trim();
        }
        if (text.length() > 500) {
            text = text.substring(0, 500);
        }
        return text;
    }

    private String buildFallbackDescription(CourseDetailVO course, List<ChapterTreeVO> chapters) {
        String name = course.getName() != null ? course.getName() : "本课程";
        List<String> chapterTitles = CourseAiContextBuilder.flattenChapterTitles(chapters);
        String outline = chapterTitles.isEmpty()
                ? "核心章节与综合实践"
                : String.join("、", chapterTitles.subList(0, Math.min(3, chapterTitles.size())));
        return "《" + name + "》面向相关专业学生，系统讲解" + outline
                + "等内容，强调理论与实践结合。建议具备相应学科基础，按周完成预习、实验与作业；"
                + "学习过程中可结合课程 AI 助教与知识库资料进行巩固与拓展。";
    }
}
