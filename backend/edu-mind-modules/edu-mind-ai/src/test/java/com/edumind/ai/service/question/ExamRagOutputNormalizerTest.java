package com.edumind.ai.service.question;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExamRagOutputNormalizerTest {

    private final ExamRagOutputNormalizer normalizer = new ExamRagOutputNormalizer();

    @Test
    void normalize_shouldFixKnowledgePointIdsAndChapterName() {
        String raw = """
                {
                  "status": "SUCCESS",
                  "courseId": "10001",
                  "courseName": "Java程序设计",
                  "chapterName": "第三章面向对象程序设计",
                  "questions": [
                    {
                      "type": "SINGLE_CHOICE",
                      "knowledgePoints": [
                        {
                          "knowledgePointId": "多态与动态分派",
                          "knowledgePointName": "多态与动态分派"
                        }
                      ]
                    },
                    {
                      "type": "MULTIPLE_CHOICE",
                      "knowledgePoints": [
                        {
                          "knowledgePointId": "继承与重写",
                          "knowledgePointName": "继承与重写"
                        }
                      ]
                    }
                  ]
                }
                """;

        Map<String, String> vars = Map.of(
                "course_id", "10001",
                "course_name", "Java程序设计",
                "chapter_id", "CH03",
                "chapter_name", "第三章 面向对象程序设计",
                "knowledge_point_ids", "KP003,KP004",
                "knowledge_point_names", "继承与重写,多态与动态分派"
        );

        String normalized = normalizer.normalize(raw, vars);
        JSONObject root = JSON.parseObject(normalized);

        assertEquals("第三章 面向对象程序设计", root.getString("chapterName"));
        assertEquals("CH03", root.getString("chapterId"));

        JSONArray questions = root.getJSONArray("questions");
        assertEquals("KP004", questions.getJSONObject(0).getJSONArray("knowledgePoints")
                .getJSONObject(0).getString("knowledgePointId"));
        assertEquals("KP003", questions.getJSONObject(1).getJSONArray("knowledgePoints")
                .getJSONObject(0).getString("knowledgePointId"));
    }

    @Test
    void buildKnowledgePointMapping_shouldPairIdsAndNames() {
        Map<String, String> mapping = normalizer.buildKnowledgePointMapping(Map.of(
                "knowledge_point_ids", "KP003,KP004",
                "knowledge_point_names", "继承与重写,多态与动态分派"
        ));

        assertEquals("KP003", mapping.get("继承与重写"));
        assertEquals("KP004", mapping.get("多态与动态分派"));
    }
}
