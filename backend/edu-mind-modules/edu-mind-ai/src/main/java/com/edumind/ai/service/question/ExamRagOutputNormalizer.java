package com.edumind.ai.service.question;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规范化 EXAM_RAG_GENERAL 模型输出的 JSON，修正知识点 ID、课程元数据等入库字段。
 */
@Component
public class ExamRagOutputNormalizer {

    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

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
            applyMetadata(root, variables);
            normalizeQuestionKnowledgePoints(root, variables);
            return JSON.toJSONString(root, com.alibaba.fastjson2.JSONWriter.Feature.PrettyFormat);
        } catch (Exception ex) {
            return rawOutput;
        }
    }

    private void applyMetadata(JSONObject root, Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) {
            return;
        }
        putIfPresent(root, "courseId", variables.get("course_id"));
        putIfPresent(root, "courseName", variables.get("course_name"));
        putIfPresent(root, "chapterName", variables.get("chapter_name"));
        putIfPresent(root, "chapterId", variables.get("chapter_id"));
    }

    private void normalizeQuestionKnowledgePoints(JSONObject root, Map<String, String> variables) {
        JSONArray questions = root.getJSONArray("questions");
        if (questions == null || questions.isEmpty()) {
            return;
        }
        Map<String, String> nameToId = buildKnowledgePointMapping(variables);
        if (nameToId.isEmpty()) {
            return;
        }
        for (int i = 0; i < questions.size(); i++) {
            JSONObject question = questions.getJSONObject(i);
            if (question == null) {
                continue;
            }
            JSONArray knowledgePoints = question.getJSONArray("knowledgePoints");
            if (knowledgePoints == null || knowledgePoints.isEmpty()) {
                continue;
            }
            for (int j = 0; j < knowledgePoints.size(); j++) {
                JSONObject kp = knowledgePoints.getJSONObject(j);
                if (kp == null) {
                    continue;
                }
                normalizeKnowledgePointEntry(kp, nameToId);
            }
        }
    }

    private void normalizeKnowledgePointEntry(JSONObject kp, Map<String, String> nameToId) {
        String id = trim(kp.getString("knowledgePointId"));
        String name = trim(kp.getString("knowledgePointName"));

        if (StringUtils.hasText(name) && nameToId.containsKey(name)) {
            kp.put("knowledgePointId", nameToId.get(name));
            kp.put("knowledgePointName", name);
            return;
        }

        if (StringUtils.hasText(id) && nameToId.containsKey(id)) {
            kp.put("knowledgePointId", nameToId.get(id));
            if (!StringUtils.hasText(name)) {
                kp.put("knowledgePointName", id);
            }
            return;
        }

        if (StringUtils.hasText(id) && looksLikeSystemKnowledgePointId(id)) {
            return;
        }

        if (StringUtils.hasText(name) && nameToId.containsKey(name)) {
            kp.put("knowledgePointId", nameToId.get(name));
        }
    }

    private boolean looksLikeSystemKnowledgePointId(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return value.matches("(?i)^KP\\d+$") || value.matches("^\\d+$");
    }

    Map<String, String> buildKnowledgePointMapping(Map<String, String> variables) {
        Map<String, String> nameToId = new LinkedHashMap<>();
        if (variables == null) {
            return nameToId;
        }
        String idsRaw = variables.get("knowledge_point_ids");
        String namesRaw = variables.get("knowledge_point_names");
        if (!StringUtils.hasText(idsRaw) || !StringUtils.hasText(namesRaw)) {
            return nameToId;
        }
        String[] ids = idsRaw.split("[,，]");
        String[] names = namesRaw.split("[,，]");
        int size = Math.min(ids.length, names.length);
        for (int i = 0; i < size; i++) {
            String id = trim(ids[i]);
            String name = trim(names[i]);
            if (StringUtils.hasText(id) && StringUtils.hasText(name)) {
                nameToId.put(name, id);
            }
        }
        return nameToId;
    }

    private void putIfPresent(JSONObject root, String key, String value) {
        if (StringUtils.hasText(value)) {
            root.put(key, value.trim());
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
