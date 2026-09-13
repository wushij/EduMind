package com.edumind.ai.service.graph.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.service.graph.GraphRelationSuggestService;
import com.edumind.ai.service.prompt.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphRelationSuggestServiceImpl implements GraphRelationSuggestService {

    private final AiGatewayFacade aiGatewayFacade;
    private final PromptService promptService;

    @Override
    public List<Map<String, Object>> suggestRelations(Long knowledgeBaseId,
                                                      Long sourceKnowledgePointId,
                                                      List<Map<String, Object>> candidatePoints,
                                                      Integer maxSuggestions) {
        if (candidatePoints == null || candidatePoints.isEmpty() || sourceKnowledgePointId == null) {
            return List.of();
        }
        int max = maxSuggestions != null && maxSuggestions > 0 ? maxSuggestions : 5;
        Map<String, Object> source = candidatePoints.stream()
                .filter(p -> sourceKnowledgePointId.equals(toLong(p.get("id"))))
                .findFirst()
                .orElse(candidatePoints.get(0));
        String sourceTitle = String.valueOf(source.getOrDefault("title", "知识点"));

        Map<String, String> vars = new HashMap<>();
        vars.put("maxSuggestions", String.valueOf(max));
        vars.put("sourceId", String.valueOf(sourceKnowledgePointId));
        vars.put("sourceTitle", sourceTitle);
        vars.put("candidates", JSON.toJSONString(candidatePoints));

        String userPrompt = promptService.renderTemplate("graph_relation_suggest", vars);
        try {
            String reply = aiGatewayFacade.chat("GRAPH_SUGGEST",
                    "你是课程知识图谱关系推断专家，仅输出 JSON 数组。", userPrompt);
            return parseSuggestions(reply, sourceKnowledgePointId, sourceTitle, candidatePoints, max);
        } catch (Exception ex) {
            log.warn("LLM graph relation suggest failed: {}", ex.getMessage());
            return ruleFallback(sourceKnowledgePointId, sourceTitle, candidatePoints, max);
        }
    }

    private List<Map<String, Object>> parseSuggestions(String reply,
                                                         Long sourceId,
                                                         String sourceTitle,
                                                         List<Map<String, Object>> candidates,
                                                         int max) {
        JSONArray array = extractJsonArray(reply);
        if (array == null || array.isEmpty()) {
            return ruleFallback(sourceId, sourceTitle, candidates, max);
        }
        Map<Long, String> titles = new HashMap<>();
        for (Map<String, Object> c : candidates) {
            titles.put(toLong(c.get("id")), String.valueOf(c.get("title")));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < array.size() && result.size() < max; i++) {
            JSONObject item = array.getJSONObject(i);
            if (item == null) {
                continue;
            }
            Long targetId = item.getLong("targetKnowledgePointId");
            if (targetId == null || targetId.equals(sourceId)) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("sourceKnowledgePointId", sourceId);
            row.put("targetKnowledgePointId", targetId);
            row.put("sourceTitle", sourceTitle);
            row.put("targetTitle", titles.getOrDefault(targetId, "知识点" + targetId));
            row.put("relationType", item.getOrDefault("relationType", "prerequisite"));
            row.put("confidence", item.getDoubleValue("confidence"));
            row.put("reason", item.getOrDefault("reason", "AI 推断关系"));
            result.add(row);
        }
        return result.isEmpty() ? ruleFallback(sourceId, sourceTitle, candidates, max) : result;
    }

    private JSONArray extractJsonArray(String reply) {
        if (!StringUtils.hasText(reply)) {
            return null;
        }
        try {
            int start = reply.indexOf('[');
            int end = reply.lastIndexOf(']');
            if (start >= 0 && end > start) {
                return JSON.parseArray(reply.substring(start, end + 1));
            }
            return JSON.parseArray(reply);
        } catch (Exception ex) {
            return null;
        }
    }

    private List<Map<String, Object>> ruleFallback(Long sourceId,
                                                   String sourceTitle,
                                                   List<Map<String, Object>> candidates,
                                                   int max) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> target : candidates) {
            Long targetId = toLong(target.get("id"));
            if (targetId == null || targetId.equals(sourceId)) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("sourceKnowledgePointId", sourceId);
            item.put("targetKnowledgePointId", targetId);
            item.put("sourceTitle", sourceTitle);
            item.put("targetTitle", target.get("title"));
            item.put("relationType", "prerequisite");
            item.put("confidence", 0.75);
            item.put("reason", "「" + sourceTitle + "」可能依赖「" + target.get("title") + "」的先修概念。");
            result.add(item);
            if (result.size() >= max) {
                break;
            }
        }
        return result;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
