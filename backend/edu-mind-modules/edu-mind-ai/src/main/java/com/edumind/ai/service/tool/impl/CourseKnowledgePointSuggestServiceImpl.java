package com.edumind.ai.service.tool.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.edumind.ai.api.AiChatApi;
import com.edumind.ai.dto.tool.CourseKnowledgePointSuggestDTO;
import com.edumind.ai.dto.tool.CourseKnowledgePointSuggestItemVO;
import com.edumind.ai.dto.tool.CourseKnowledgePointSuggestResultVO;
import com.edumind.ai.service.tool.CourseKnowledgePointSuggestService;
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
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseKnowledgePointSuggestServiceImpl implements CourseKnowledgePointSuggestService {

    private static final String SCENE = "COURSE_KNOWLEDGE_POINT";
    private static final int MAX_COUNT = 6;

    private final AiChatApi aiChatApi;
    private final CourseQueryApi courseQueryApi;

    @Override
    public CourseKnowledgePointSuggestResultVO suggest(CourseKnowledgePointSuggestDTO dto) {
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
        String contextBlock = CourseAiContextBuilder.buildChapterSuggestContext(
                course, chapters, knowledgePoints, dto.getChapterId());
        String chapterTitle = extractLine(contextBlock, "目标章节：");

        String systemPrompt = buildSystemPrompt();
        String userPrompt = contextBlock
                + "\n请针对目标章节提炼 " + count + " 个具体、可考核的课程知识点/考点。"
                + "禁止空泛套话（如「状态迁移模型与拓扑演进」等与章节无关的抽象名词）。"
                + "认知维度仅可为 REMEMBER、UNDERSTAND、APPLY、ANALYZE。";

        try {
            log.info("[AI KP Suggest] courseId={}, chapterId={}, count={}", dto.getCourseId(), dto.getChapterId(), count);
            String aiReply = aiChatApi.chat(SCENE, systemPrompt, userPrompt);
            List<CourseKnowledgePointSuggestItemVO> parsed = parsePoints(aiReply, count);
            if (!parsed.isEmpty()) {
                return CourseKnowledgePointSuggestResultVO.builder()
                        .points(parsed)
                        .aiGenerated(true)
                        .sourceLabel("模型推演已生成")
                        .build();
            }
            String repair = userPrompt + "\n\n上次输出无法解析，请严格只输出合法 JSON 数组，字段与示例一致。";
            String retryReply = aiChatApi.chat(SCENE, systemPrompt, repair);
            parsed = parsePoints(retryReply, count);
            if (!parsed.isEmpty()) {
                return CourseKnowledgePointSuggestResultVO.builder()
                        .points(parsed)
                        .aiGenerated(true)
                        .sourceLabel("模型推演已生成")
                        .build();
            }
        } catch (Exception ex) {
            log.warn("[AI KP Suggest] failed: {}", ex.getMessage());
        }

        return CourseKnowledgePointSuggestResultVO.builder()
                .points(buildFallback(course, chapterTitle, count))
                .aiGenerated(false)
                .sourceLabel("章节上下文兜底")
                .build();
    }

    private String extractLine(String context, String prefix) {
        for (String line : context.split("\n")) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return "核心章节";
    }

    private String buildSystemPrompt() {
        return "你是高校课程教研室主任与命题专家。\n"
                + "根据课程真实章节与已有考点，提炼可教学、可测评的知识点。\n"
                + "【硬性格式】必须输出合法纯 JSON 数组，禁止 Markdown 代码块，直接以 [ 开始：\n"
                + "[{\"title\":\"具体考点名称\",\"cognitiveDimension\":\"APPLY\",\"importance\":4,"
                + "\"description\":\"40～120字掌握要求\",\"examFocus\":\"易错点或考查形式\","
                + "\"prerequisiteTitles\":[\"同课程已有考点标题\"]}]";
    }

    private List<CourseKnowledgePointSuggestItemVO> parsePoints(String raw, int maxCount) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        String json = extractJsonPayload(raw);
        Object parsed = JSON.parse(json);
        JSONArray arr = null;
        if (parsed instanceof JSONArray jsonArray) {
            arr = jsonArray;
        } else if (parsed instanceof JSONObject root) {
            arr = root.getJSONArray("points");
            if (arr == null) {
                arr = root.getJSONArray("knowledgePoints");
            }
            if (arr == null) {
                arr = root.getJSONArray("items");
            }
        }
        if (arr == null || arr.isEmpty()) {
            return List.of();
        }
        List<CourseKnowledgePointSuggestItemVO> list = new ArrayList<>();
        for (int i = 0; i < arr.size() && list.size() < maxCount; i++) {
            JSONObject item = arr.getJSONObject(i);
            if (item == null) {
                continue;
            }
            String title = firstNonBlank(item.getString("title"), item.getString("name"));
            if (!StringUtils.hasText(title)) {
                continue;
            }
            String dimension = normalizeDimension(item.getString("cognitiveDimension"));
            Integer importance = item.getInteger("importance");
            if (importance == null || importance < 1 || importance > 5) {
                importance = 4;
            }
            List<String> prereq = new ArrayList<>();
            JSONArray preArr = item.getJSONArray("prerequisiteTitles");
            if (preArr != null) {
                for (int j = 0; j < preArr.size(); j++) {
                    String t = preArr.getString(j);
                    if (StringUtils.hasText(t)) {
                        prereq.add(t.trim());
                    }
                }
            }
            list.add(CourseKnowledgePointSuggestItemVO.builder()
                    .title(title.trim())
                    .cognitiveDimension(dimension)
                    .importance(importance)
                    .description(trimOrNull(item.getString("description")))
                    .examFocus(trimOrNull(item.getString("examFocus")))
                    .prerequisiteTitles(prereq.isEmpty() ? List.of() : prereq)
                    .build());
        }
        return list;
    }

    private String normalizeDimension(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "APPLY";
        }
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        if (upper.equals("REMEMBER") || upper.equals("UNDERSTAND") || upper.equals("APPLY") || upper.equals("ANALYZE")) {
            return upper;
        }
        return "APPLY";
    }

    private String trimOrNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    private String extractJsonPayload(String raw) {
        String s = raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```\\s*$", "").trim();
        }
        int arrStart = s.indexOf('[');
        int arrEnd = s.lastIndexOf(']');
        if (arrStart >= 0 && arrEnd > arrStart) {
            return s.substring(arrStart, arrEnd + 1);
        }
        int objStart = s.indexOf('{');
        int objEnd = s.lastIndexOf('}');
        if (objStart >= 0 && objEnd > objStart) {
            return s.substring(objStart, objEnd + 1);
        }
        return s;
    }

    private String firstNonBlank(String a, String b) {
        if (StringUtils.hasText(a)) {
            return a;
        }
        return StringUtils.hasText(b) ? b : null;
    }

    private List<CourseKnowledgePointSuggestItemVO> buildFallback(CourseDetailVO course, String chapterTitle, int count) {
        String courseName = course.getName() != null ? course.getName() : "本课程";
        List<CourseKnowledgePointSuggestItemVO> list = new ArrayList<>();
        list.add(CourseKnowledgePointSuggestItemVO.builder()
                .title(chapterTitle + "核心概念界定")
                .cognitiveDimension("UNDERSTAND")
                .importance(4)
                .description("能够准确说明「" + chapterTitle + "」中的关键术语与基本结论，并举例说明其在" + courseName + "中的位置。")
                .examFocus("概念混淆与定义不完整")
                .prerequisiteTitles(List.of())
                .build());
        if (count > 1) {
            list.add(CourseKnowledgePointSuggestItemVO.builder()
                    .title(chapterTitle + "典型应用与解题思路")
                    .cognitiveDimension("APPLY")
                    .importance(4)
                    .description("能够依据本章方法完成例题推演，说明步骤依据与常见边界条件。")
                    .examFocus("跳步推导与边界遗漏")
                    .prerequisiteTitles(List.of())
                    .build());
        }
        return list.subList(0, Math.min(count, list.size()));
    }
}
