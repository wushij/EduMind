package com.edumind.ai.service.teaching;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LessonPrepOutputNormalizerTest {

    private final LessonPrepOutputNormalizer normalizer = new LessonPrepOutputNormalizer();

    private static final String RETRIEVED_CONTEXT = """
            <rag_context>
            <source id="S1" document="教材.pdf">多态定义</source>
            <source id="S2" document="讲义.pptx">虚方法表</source>
            </rag_context>
            """;

    @Test
    void normalize_shouldRemoveInvalidSourceIdsAndFlagReview() {
        String raw = """
                {
                  "status": "SUCCESS",
                  "groundingStatus": "FULL",
                  "requiresTeacherReview": false,
                  "reviewReason": null,
                  "lessonPlan": {
                    "basicInfo": {
                      "courseId": "10001",
                      "chapterName": "第三章面向对象程序设计",
                      "lessonCount": 2,
                      "totalDurationMinutes": 90
                    },
                    "stages": [
                      { "name": "情境导入", "durationMinutes": 8, "sourceIds": ["S2", "S3"] },
                      { "name": "多态机制探究", "durationMinutes": 22, "sourceIds": ["S2"] },
                      { "name": "课堂实践", "durationMinutes": 25, "sourceIds": ["S2", "S3"] }
                    ],
                    "sourceIds": ["S1", "S2", "S3"]
                  }
                }
                """;

        String normalized = normalizer.normalize(raw, Map.of(
                "course_id", "10001",
                "chapter_name", "第三章 面向对象程序设计",
                "lesson_count", "2",
                "lesson_duration", "90分钟",
                "retrieved_context", RETRIEVED_CONTEXT
        ));

        JSONObject root = JSON.parseObject(normalized);
        assertEquals("PARTIAL", root.getString("groundingStatus"));
        assertTrue(root.getBooleanValue("requiresTeacherReview"));
        assertTrue(root.getString("reviewReason").contains("sourceIds"));
        assertTrue(root.getString("reviewReason").contains("85%"));

        JSONObject firstStage = root.getJSONObject("lessonPlan").getJSONArray("stages").getJSONObject(0);
        assertEquals("[\"S2\"]", firstStage.getJSONArray("sourceIds").toJSONString());
        assertEquals("第三章 面向对象程序设计", root.getJSONObject("lessonPlan").getJSONObject("basicInfo").getString("chapterName"));
    }

    @Test
    void extractAllowedSourceIds_shouldParseRagContext() {
        Set<String> ids = normalizer.extractAllowedSourceIds(RETRIEVED_CONTEXT);
        assertEquals(Set.of("S1", "S2"), ids);
    }
}
