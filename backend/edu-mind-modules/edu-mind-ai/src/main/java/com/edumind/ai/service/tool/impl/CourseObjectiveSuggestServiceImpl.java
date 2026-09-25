package com.edumind.ai.service.tool.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.edumind.ai.api.AiChatApi;
import com.edumind.ai.dto.tool.CourseObjectiveSuggestDTO;
import com.edumind.ai.dto.tool.CourseObjectiveSuggestItemVO;
import com.edumind.ai.dto.tool.CourseObjectiveSuggestResultVO;
import com.edumind.ai.service.tool.CourseObjectiveSuggestService;
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

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseObjectiveSuggestServiceImpl implements CourseObjectiveSuggestService {

    private static final String SCENE = "COURSE_OBJECTIVE";
    private static final int MAX_COUNT = 6;

    private final AiChatApi aiChatApi;
    private final CourseQueryApi courseQueryApi;

    @Override
    public CourseObjectiveSuggestResultVO suggest(CourseObjectiveSuggestDTO dto) {
        int count = dto.getCount() != null ? dto.getCount() : 4;
        if (count < 1 || count > MAX_COUNT) {
            throw new BusinessException("生成条数需在 1～6 之间");
        }
        CourseDetailVO course = courseQueryApi.getCourseById(dto.getCourseId());
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        List<ChapterTreeVO> chapters = courseQueryApi.listChaptersByCourseId(dto.getCourseId());
        List<KnowledgePointVO> knowledgePoints = courseQueryApi.listKnowledgePointsByCourseId(dto.getCourseId());
        List<String> chapterTitles = CourseAiContextBuilder.flattenChapterTitles(chapters);

        String systemPrompt = buildSystemPrompt();
        String userPrompt = CourseAiContextBuilder.buildContext(course, chapters, knowledgePoints)
                + "\n请生成 " + count + " 条教学目标，面向学生能力达成，标题简洁，描述可评价。";

        try {
            log.info("[AI Course Objective] 调用网关推演教学目标 courseId={}, count={}", dto.getCourseId(), count);
            String aiReply = aiChatApi.chat(SCENE, dto.getCourseId(), systemPrompt, userPrompt);
            log.info("[AI Course Objective] 模型返回 length={}", aiReply != null ? aiReply.length() : 0);

            List<CourseObjectiveSuggestItemVO> parsed = parseObjectives(aiReply, count);
            if (!parsed.isEmpty()) {
                return CourseObjectiveSuggestResultVO.builder()
                        .objectives(parsed)
                        .aiGenerated(true)
                        .sourceLabel("模型推演已生成")
                        .build();
            }

            String repairPrompt = userPrompt + "\n\n上次输出无法解析，请严格只输出合法 JSON，字段 objectives 为数组。";
            String retryReply = aiChatApi.chat(SCENE, dto.getCourseId(), systemPrompt, repairPrompt);
            parsed = parseObjectives(retryReply, count);
            if (!parsed.isEmpty()) {
                return CourseObjectiveSuggestResultVO.builder()
                        .objectives(parsed)
                        .aiGenerated(true)
                        .sourceLabel("模型推演已生成")
                        .build();
            }
        } catch (Exception ex) {
            log.warn("[AI Course Objective] 模型调用或解析失败，启用上下文兜底: {}", ex.getMessage());
        }

        List<CourseObjectiveSuggestItemVO> fallback = buildFallbackObjectives(course, chapterTitles, count);
        return CourseObjectiveSuggestResultVO.builder()
                .objectives(fallback)
                .aiGenerated(false)
                .sourceLabel("上下文智能兜底（模型暂不可用）")
                .build();
    }

    private String buildSystemPrompt() {
        return "你是一位拥有丰富课程建设经验的教学设计专家。\n"
                + "根据课程真实信息推演可衡量的教学目标。\n"
                + "【硬性格式约束】必须输出合法纯 JSON，禁止 Markdown 代码块，直接以 { 开始：\n"
                + "{\"objectives\":[{\"title\":\"短标题\",\"description\":\"说明\"}]}";
    }

    private List<CourseObjectiveSuggestItemVO> parseObjectives(String raw, int maxCount) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        String json = extractJsonPayload(raw);
        Object parsed = JSON.parse(json);
        JSONArray arr = null;
        if (parsed instanceof JSONArray jsonArray) {
            arr = jsonArray;
        } else if (parsed instanceof JSONObject root) {
            arr = root.getJSONArray("objectives");
            if (arr == null) {
                arr = root.getJSONArray("data");
            }
            if (arr == null) {
                arr = root.getJSONArray("items");
            }
        }
        if (arr == null || arr.isEmpty()) {
            return List.of();
        }
        List<CourseObjectiveSuggestItemVO> list = new ArrayList<>();
        for (int i = 0; i < arr.size() && list.size() < maxCount; i++) {
            JSONObject item = arr.getJSONObject(i);
            if (item == null) {
                continue;
            }
            String title = firstNonBlank(item.getString("title"), item.getString("name"));
            if (!StringUtils.hasText(title)) {
                continue;
            }
            String description = firstNonBlank(item.getString("description"), item.getString("desc"));
            list.add(CourseObjectiveSuggestItemVO.builder()
                    .title(title.trim())
                    .description(description != null ? description.trim() : null)
                    .build());
        }
        return list;
    }

    private String extractJsonPayload(String raw) {
        String s = raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```\\s*$", "").trim();
        }
        int objStart = s.indexOf('{');
        int objEnd = s.lastIndexOf('}');
        if (objStart >= 0 && objEnd > objStart) {
            return s.substring(objStart, objEnd + 1);
        }
        int arrStart = s.indexOf('[');
        int arrEnd = s.lastIndexOf(']');
        if (arrStart >= 0 && arrEnd > arrStart) {
            return s.substring(arrStart, arrEnd + 1);
        }
        return s;
    }

    private String firstNonBlank(String a, String b) {
        if (StringUtils.hasText(a)) {
            return a;
        }
        return StringUtils.hasText(b) ? b : null;
    }

    private List<CourseObjectiveSuggestItemVO> buildFallbackObjectives(
            CourseDetailVO course,
            List<String> chapterTitles,
            int count) {
        String courseName = course.getName() != null ? course.getName() : "本课程";
        List<CourseObjectiveSuggestItemVO> list = new ArrayList<>();
        if (!chapterTitles.isEmpty()) {
            for (String chapter : chapterTitles) {
                if (list.size() >= count) {
                    break;
                }
                list.add(CourseObjectiveSuggestItemVO.builder()
                        .title("掌握「" + chapter + "」核心内容")
                        .description("能够说明" + chapter + "的关键概念，并完成相关练习与实验。")
                        .build());
            }
        }
        if (list.size() < count) {
            list.add(CourseObjectiveSuggestItemVO.builder()
                    .title("理解" + courseName + "知识体系")
                    .description("能够梳理课程知识结构，建立与前后续课程的联系。")
                    .build());
        }
        if (list.size() < count) {
            list.add(CourseObjectiveSuggestItemVO.builder()
                    .title("应用所学解决实际问题")
                    .description("能够运用课程方法完成案例分析、实验或项目任务，并规范表达结果。")
                    .build());
        }
        return list.subList(0, Math.min(count, list.size()));
    }
}
