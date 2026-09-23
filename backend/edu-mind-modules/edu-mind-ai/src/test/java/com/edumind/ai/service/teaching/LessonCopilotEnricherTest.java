package com.edumind.ai.service.teaching;

import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.api.LessonQueryApi;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.course.vo.lesson.LessonCopilotContextVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LessonCopilotEnricherTest {

    @Mock
    private LessonQueryApi lessonQueryApi;

    @Mock
    private CourseQueryApi courseQueryApi;

    private LessonCopilotEnricher enricher;

    @BeforeEach
    void setUp() {
        enricher = new LessonCopilotEnricher(lessonQueryApi, courseQueryApi);
    }

    @Test
    @DisplayName("应该能够生成包含课程全局大纲树以及具体课文Markdown正文的完整上下文")
    void shouldBuildFullEnrichmentBlockWithCurriculumAndArticleBody() {
        Long courseId = 1001L;
        Long lessonId = 97L;

        // 模拟课程章节大纲树
        ChapterTreeVO sec1 = ChapterTreeVO.builder()
                .id(97L)
                .title("Java技术体系全景：JDK、JRE、JVM与字节码关系辨析")
                .parentId(10L)
                .build();
        ChapterTreeVO sec2 = ChapterTreeVO.builder()
                .id(98L)
                .title("从.java到.class：javac编译机制")
                .parentId(10L)
                .build();
        ChapterTreeVO chap1 = ChapterTreeVO.builder()
                .id(10L)
                .title("第一章 java学习概述与核心理念")
                .children(List.of(sec1, sec2))
                .build();
        when(courseQueryApi.listChaptersByCourseId(courseId)).thenReturn(List.of(chap1));

        // 模拟课节具体讲义正文与学习目标
        LessonCopilotContextVO ctx = LessonCopilotContextVO.builder()
                .courseId(courseId)
                .lessonChapterId(lessonId)
                .title("Java技术体系全景：JDK、JRE、JVM与字节码关系辨析")
                .description("剖析Java平台基石技术规范与架构原理")
                .objectivesText("1. 深入理解JDK、JRE与JVM的边界划分；2. 理解Java跨平台原理")
                .relevantBodyMarkdown("## 1. 技术栈全景拆解\n\nJDK是面向开发者的完整工具包，包含JRE及javac调试工具。\n\n## 2. 核心架构关系\n\n```\nJDK > JRE > JVM\n```\n")
                .totalBodyChars(120)
                .bodyTruncated(false)
                .build();
        when(lessonQueryApi.getCopilotContext(anyLong(), anyLong(), anyBoolean(), anyString())).thenReturn(ctx);

        String result = enricher.buildEnrichmentBlock(courseId, lessonId, true, "请问JDK和JRE的区别是什么？");

        // 验证全局大纲
        assertThat(result).contains("【本课程全局章节目录体系】");
        assertThat(result).contains("第一章 java学习概述与核心理念");
        assertThat(result).contains("[课时 97] Java技术体系全景：JDK、JRE、JVM与字节码关系辨析");
        assertThat(result).contains("(用户当前正在研读该课节)");
        assertThat(result).contains("[课时 98] 从.java到.class：javac编译机制");

        // 验证课文详情与正文
        assertThat(result).contains("【当前选定课节详细信息】");
        assertThat(result).contains("【课节核心学习目标】");
        assertThat(result).contains("深入理解JDK、JRE与JVM的边界划分");
        assertThat(result).contains("【当前课节文章结构与讲义正文】");
        assertThat(result).contains("## 1. 技术栈全景拆解");
        assertThat(result).contains("JDK是面向开发者的完整工具包");
        assertThat(result).contains("【助教回答准则】");
    }

    @Test
    @DisplayName("当未指定具体课节时，依然能输出完整的课程全局章节大纲")
    void shouldBuildCurriculumTreeEvenWhenLessonIdIsNull() {
        Long courseId = 1001L;

        ChapterTreeVO chap1 = ChapterTreeVO.builder()
                .id(10L)
                .title("第一章 java学习概述与核心理念")
                .children(List.of())
                .build();
        when(courseQueryApi.listChaptersByCourseId(courseId)).thenReturn(List.of(chap1));

        String result = enricher.buildEnrichmentBlock(courseId, null, true, "这门课一共有哪些章节？");

        assertThat(result).contains("【本课程全局章节目录体系】");
        assertThat(result).contains("第一章 java学习概述与核心理念");
        assertThat(result).doesNotContain("【当前选定课节详细信息】");
    }

    @Test
    @DisplayName("当courseId为空时直接返回空字符串")
    void shouldReturnEmptyWhenCourseIdIsNull() {
        String result = enricher.buildEnrichmentBlock(null, 97L, true, "问答");
        assertThat(result).isEmpty();
    }
}
