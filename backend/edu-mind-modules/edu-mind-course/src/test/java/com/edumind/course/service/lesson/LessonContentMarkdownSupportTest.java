package com.edumind.course.service.lesson;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LessonContentMarkdownSupportTest {

    @Test
    void buildIndexMarkdown_includesObjectives() {
        String json = """
                {"version":1,"blocks":[
                {"type":"callout","variant":"objective","title":"目标","body":"- 能够描述 JDK"},
                {"type":"markdown","body":"正文段落"}
                ]}
                """;
        var parsed = LessonContentMarkdownSupport.parseContentJson(json);
        String md = LessonContentMarkdownSupport.buildIndexMarkdown(parsed.objectivesText(), parsed.bodyMarkdown());
        assertTrue(md.contains("学习目标"));
        assertTrue(md.contains("JDK"));
        assertTrue(md.contains("正文段落"));
    }

    @Test
    void buildIndexMarkdown_doesNotDuplicateObjectivesHeading() {
        String json = """
                {"version":1,"blocks":[
                {"type":"callout","variant":"objective","title":"学习目标","body":"- 能够准确描述 JDK"},
                {"type":"markdown","body":"正文"}
                ]}
                """;
        var parsed = LessonContentMarkdownSupport.parseContentJson(json);
        String md = LessonContentMarkdownSupport.buildIndexMarkdown(parsed.objectivesText(), parsed.bodyMarkdown());
        assertTrue(md.contains("## 学习目标"));
        assertFalse(md.matches("(?s).*## 学习目标\\s*\\n+学习目标\\s*\\n+-.*"));
    }

    @Test
    void computeContentHash_stable() {
        String a = LessonContentMarkdownSupport.computeContentHash("hello");
        String b = LessonContentMarkdownSupport.computeContentHash("hello");
        assertFalse(a.isEmpty());
        assertTrue(a.equals(b));
    }
}
