package com.edumind.common.markdown;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownIndexNormalizerTest {

    @Test
    void splitsHeadingGluedToBody_forSection21() {
        String raw = "### 2.1从源码到运行的总链路一段 Java程序从编写到运行，可以概括为以下链路：\n\n正文";
        String out = MarkdownIndexNormalizer.normalize(raw);
        assertTrue(out.contains("### 2.1 从源码到运行的总链路"));
        assertTrue(out.contains("\n\n一段 Java程序"));
    }

    @Test
    void mergesImportHeading_withFollowingParagraphStarter() {
        String raw = "## 一、课节导入：从\n\n一段 Java程序说起在 Java学习中，初学者";
        String out = MarkdownIndexNormalizer.normalize(raw);
        assertTrue(out.contains("## 一、课节导入：从一段 Java程序说起"), () -> out);
        assertFalse(out.contains("说起在"), () -> out);
        assertTrue(out.contains("在 Java学习中"), () -> out);
    }

    @Test
    void splitsMarkdownTableFromHeading_forSection42() {
        String raw = "### 4.2 职责边界对比|组件 |层级 |";
        String out = MarkdownIndexNormalizer.normalize(raw);
        assertTrue(out.contains("### 4.2 职责边界对比"));
        assertTrue(out.contains("\n\n|组件"));
    }

    @Test
    void splitsHeadingGluedToDefinitionList_forSection22() {
        String raw = "### 2.2四个关键词的一句话定义- JDK\n：Java Development Kit";
        String out = MarkdownIndexNormalizer.normalize(raw);
        assertTrue(out.contains("### 2.2 四个关键词的一句话定义"));
        assertTrue(out.contains("\n\n- JDK") || out.contains("\n\n-  JDK"));
    }
}
