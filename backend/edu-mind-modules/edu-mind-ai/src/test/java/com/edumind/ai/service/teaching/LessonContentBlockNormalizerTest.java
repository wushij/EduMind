package com.edumind.ai.service.teaching;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 课节正文规范化测试：正文里重复出现的课节标题必须被剥离。
 */
class LessonContentBlockNormalizerTest {

    private final LessonContentBlockNormalizer normalizer = new LessonContentBlockNormalizer();

    private static final String LESSON_TITLE = "1.2 洛必达法则求未定式极限专项突破";

    private static String bodyOf(String contentJson) {
        JSONArray blocks = JSON.parseObject(contentJson).getJSONArray("blocks");
        return blocks.getJSONObject(1).getString("body");
    }

    @Test
    @DisplayName("剥离正文开头与课节标题同行的标题回声，保留同行后续正文")
    void stripsEchoGluedWithFollowingParagraph() {
        String body = "##1.2洛必达法则求未定式极限专项突破本课节围绕两条主线展开：适用条件与使用边界。";
        String result = normalizer.stripTitleEcho(body, LESSON_TITLE);

        assertEquals("本课节围绕两条主线展开：适用条件与使用边界。", result);
    }

    @Test
    @DisplayName("标题独占一行时整行删除，并保留后续内容")
    void stripsEchoHeadingLine() {
        String body = "## 1.2 洛必达法则求未定式极限专项突破\n\n本课节围绕两条主线展开。";
        String result = normalizer.stripTitleEcho(body, LESSON_TITLE);

        assertEquals("本课节围绕两条主线展开。", result);
    }

    @Test
    @DisplayName("正文只剩标题回声时返回空正文，而不是把回声留在块里")
    void returnsEmptyBodyWhenOnlyEcho() {
        String result = normalizer.stripTitleEcho("## 1.2洛必达法则求未定式极限专项突破", LESSON_TITLE);

        assertEquals("", result);
    }

    @Test
    @DisplayName("正文本来就没有回声时不做任何改动")
    void keepsBodyWithoutEcho() {
        assertNull(normalizer.stripTitleEcho("## 一、未定式判型\n\n极限计算的第一步是判型。", LESSON_TITLE));
        assertNull(normalizer.stripTitleEcho("本课节围绕两条主线展开：适用条件与使用边界。", LESSON_TITLE));
        // 标题相似但并非同一标题：不得误剥离
        assertNull(normalizer.stripTitleEcho("## 二、洛必达法则：条件重于计算\n\n正文。", LESSON_TITLE));
    }

    @Test
    @DisplayName("规范化整个块 JSON 时只改动含回声的块")
    void normalizesWholeBlocksJson() {
        String contentJson = "{\"version\":1,\"blocks\":["
                + "{\"type\":\"callout\",\"variant\":\"objective\",\"title\":\"学习目标\",\"body\":\"- 能判定七种未定式。\"},"
                + "{\"type\":\"markdown\",\"body\":\"##1.2洛必达法则求未定式极限专项突破本课节围绕两条主线展开。\\n\\n###一、未定式判型\\n\\n正文。\"}"
                + "]}";

        String normalized = normalizer.normalize(contentJson, LESSON_TITLE);

        assertEquals("本课节围绕两条主线展开。\n\n###一、未定式判型\n\n正文。", bodyOf(normalized));
        assertTrue(normalized.contains("能判定七种未定式"));
        assertFalse(normalized.contains("专项突破本课节围绕"));
    }

    @Test
    @DisplayName("无法解析的 JSON 原样返回，不影响备课落库")
    void keepsRawWhenJsonInvalid() {
        String raw = "not-a-json";

        assertEquals(raw, normalizer.normalize(raw, LESSON_TITLE));
        assertEquals("", normalizer.normalize("", LESSON_TITLE));
    }
}
