package com.edumind.ai.service.teaching;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规范化 LESSON_PREP_RAG_GENERAL 模型输出：修正元数据、过滤无效 sourceId、校验课时结构。
 */
@Component
public class LessonPrepOutputNormalizer {

    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern SOURCE_ID = Pattern.compile("(?i)<source\\s+id=[\"']([^\"']+)[\"']");
    private static final Pattern DURATION_MINUTES = Pattern.compile("(\\d+)");
    private static final Pattern LESSON_SESSION = Pattern.compile("第\\s*[1-9一二三四五六七八九十]+\\s*课时");
    private static final double MIN_DURATION_COVERAGE = 0.85D;

    public String normalize(String rawOutput, Map<String, String> variables) {
        if (!StringUtils.hasText(rawOutput)) {
            return rawOutput;
        }
        String jsonText = extractJson(rawOutput.trim());
        if (!StringUtils.hasText(jsonText)) {
            return rawOutput;
        }
        try {
            JSONObject root = JSON.parseObject(jsonText);
            if (root == null || root.isEmpty()) {
                return rawOutput;
            }
            JSONObject lessonPlan = root.getJSONObject("lessonPlan");
            if (lessonPlan == null) {
                return rawOutput;
            }

            Set<String> allowedSourceIds = extractAllowedSourceIds(variables != null ? variables.get("retrieved_context") : null);
            boolean removedInvalidSources = sanitizeLessonPlanSources(lessonPlan, allowedSourceIds);
            applyBasicInfo(lessonPlan.getJSONObject("basicInfo"), variables);
            applyReviewFlags(root, lessonPlan, variables, removedInvalidSources, allowedSourceIds);
            return JSON.toJSONString(root, com.alibaba.fastjson2.JSONWriter.Feature.PrettyFormat);
        } catch (Exception ex) {
            return rawOutput;
        }
    }

    private void applyBasicInfo(JSONObject basicInfo, Map<String, String> variables) {
        if (basicInfo == null || variables == null || variables.isEmpty()) {
            return;
        }
        putIfPresent(basicInfo, "courseId", variables.get("course_id"));
        putIfPresent(basicInfo, "courseName", variables.get("course_name"));
        putIfPresent(basicInfo, "chapterId", variables.get("chapter_id"));
        putIfPresent(basicInfo, "chapterName", variables.get("chapter_name"));
        putIfPresent(basicInfo, "lessonTitle", variables.get("lesson_title"));
        if (StringUtils.hasText(variables.get("student_level"))) {
            basicInfo.put("studentLevel", variables.get("student_level").trim());
        }
        Integer lessonCount = parsePositiveInt(variables.get("lesson_count"));
        if (lessonCount != null) {
            basicInfo.put("lessonCount", lessonCount);
        }
        Integer totalDuration = parseDurationMinutes(variables.get("lesson_duration"));
        if (totalDuration != null) {
            basicInfo.put("totalDurationMinutes", totalDuration);
        }
    }

    private void applyReviewFlags(JSONObject root, JSONObject lessonPlan, Map<String, String> variables,
                                  boolean removedInvalidSources, Set<String> allowedSourceIds) {
        List<String> reasons = new ArrayList<>();

        JSONObject basicInfo = lessonPlan.getJSONObject("basicInfo");
        int totalDuration = basicInfo != null && basicInfo.getIntValue("totalDurationMinutes") > 0
                ? basicInfo.getIntValue("totalDurationMinutes")
                : parseDurationMinutes(variables != null ? variables.get("lesson_duration") : null);
        int stageDuration = sumStageDurationMinutes(lessonPlan.getJSONArray("stages"));
        if (totalDuration > 0 && stageDuration < Math.round(totalDuration * MIN_DURATION_COVERAGE)) {
            reasons.add(String.format(
                    "教学环节时间合计 %d 分钟，未达到总课时 %d 分钟的 85%%，请补全课堂小结、反馈或练习环节。",
                    stageDuration, totalDuration));
        }

        int lessonCount = basicInfo != null && basicInfo.getIntValue("lessonCount") > 0
                ? basicInfo.getIntValue("lessonCount")
                : parsePositiveInt(variables != null ? variables.get("lesson_count") : null);
        if (lessonCount > 1 && !hasMultiLessonStructure(lessonPlan)) {
            reasons.add(String.format("lessonCount=%d，但教案未划分第1/第2课时，请拆分课时或调整 lessonCount。", lessonCount));
        }

        if (removedInvalidSources) {
            reasons.add("部分 sourceIds 不在当前 RAG 检索资料中，已自动移除无效引用，请教师确认。");
            root.put("groundingStatus", "PARTIAL");
        } else if (!allowedSourceIds.isEmpty()) {
            root.put("groundingStatus", "FULL");
        }

        if (!reasons.isEmpty()) {
            root.put("requiresTeacherReview", true);
            root.put("reviewReason", String.join(" ", reasons));
        }
    }

    private boolean sanitizeLessonPlanSources(JSONObject lessonPlan, Set<String> allowedSourceIds) {
        if (allowedSourceIds.isEmpty()) {
            return false;
        }
        boolean removed = false;
        removed |= sanitizeSourceIdsField(lessonPlan, allowedSourceIds);
        removed |= sanitizeArraySources(lessonPlan.getJSONArray("objectives"), allowedSourceIds);
        removed |= sanitizeArraySources(lessonPlan.getJSONArray("keyPoints"), allowedSourceIds);
        removed |= sanitizeArraySources(lessonPlan.getJSONArray("stages"), allowedSourceIds);
        return removed;
    }

    private boolean sanitizeArraySources(JSONArray array, Set<String> allowedSourceIds) {
        if (array == null || array.isEmpty()) {
            return false;
        }
        boolean removed = false;
        for (int i = 0; i < array.size(); i++) {
            JSONObject item = array.getJSONObject(i);
            if (item != null) {
                removed |= sanitizeSourceIdsField(item, allowedSourceIds);
            }
        }
        return removed;
    }

    private boolean sanitizeSourceIdsField(JSONObject object, Set<String> allowedSourceIds) {
        JSONArray sourceIds = object.getJSONArray("sourceIds");
        if (sourceIds == null || sourceIds.isEmpty()) {
            return false;
        }
        JSONArray sanitized = new JSONArray();
        boolean removed = false;
        for (int i = 0; i < sourceIds.size(); i++) {
            String sourceId = trim(sourceIds.getString(i));
            if (!StringUtils.hasText(sourceId)) {
                continue;
            }
            if (allowedSourceIds.contains(sourceId)) {
                sanitized.add(sourceId);
            } else {
                removed = true;
            }
        }
        object.put("sourceIds", sanitized);
        return removed;
    }

    private boolean hasMultiLessonStructure(JSONObject lessonPlan) {
        JSONArray sessions = lessonPlan.getJSONArray("sessions");
        if (sessions != null && sessions.size() >= 2) {
            return true;
        }
        JSONArray stages = lessonPlan.getJSONArray("stages");
        if (stages == null) {
            return false;
        }
        Set<Integer> lessonIndexes = new LinkedHashSet<>();
        for (int i = 0; i < stages.size(); i++) {
            JSONObject stage = stages.getJSONObject(i);
            if (stage == null) {
                continue;
            }
            if (stage.containsKey("lessonIndex") && stage.getIntValue("lessonIndex") > 0) {
                lessonIndexes.add(stage.getIntValue("lessonIndex"));
            }
            String name = stage.getString("name");
            if (StringUtils.hasText(name) && LESSON_SESSION.matcher(name).find()) {
                return true;
            }
        }
        return lessonIndexes.size() >= 2;
    }

    private int sumStageDurationMinutes(JSONArray stages) {
        if (stages == null || stages.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < stages.size(); i++) {
            JSONObject stage = stages.getJSONObject(i);
            if (stage != null) {
                total += Math.max(stage.getIntValue("durationMinutes"), 0);
            }
        }
        return total;
    }

    Set<String> extractAllowedSourceIds(String retrievedContext) {
        Set<String> ids = new LinkedHashSet<>();
        if (!StringUtils.hasText(retrievedContext)) {
            return ids;
        }
        Matcher matcher = SOURCE_ID.matcher(retrievedContext);
        while (matcher.find()) {
            ids.add(matcher.group(1).trim());
        }
        return ids;
    }

    private Integer parseDurationMinutes(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        Matcher matcher = DURATION_MINUTES.matcher(raw);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private Integer parsePositiveInt(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void putIfPresent(JSONObject object, String key, String value) {
        if (StringUtils.hasText(value)) {
            object.put(key, value.trim());
        }
    }

    private String trim(String value) {
        return value != null ? value.trim() : "";
    }

    String extractJson(String raw) {
        Matcher matcher = JSON_BLOCK.matcher(raw);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1).trim();
        }
        return raw;
    }
}
