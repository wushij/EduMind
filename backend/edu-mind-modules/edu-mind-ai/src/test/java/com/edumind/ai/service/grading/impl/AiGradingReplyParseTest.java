package com.edumind.ai.service.grading.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 主观题批改结果解析测试：覆盖提示词约定的 JSON 格式、历史 score:/comment: 文本格式与脏前缀剥离。
 */
class AiGradingReplyParseTest {

    @Test
    void parseJsonReply_shouldExtractScoreAndComment() {
        String reply = "```json\n{\"score\": 8, \"comment\": \"采分点命中 3/4，泰勒展开阶数略低。\"}\n```";

        AiGradingServiceImpl.ParsedGrading parsed = AiGradingServiceImpl.parseGradingReply(reply, 15);

        Assertions.assertEquals(8, parsed.score());
        Assertions.assertEquals("采分点命中 3/4，泰勒展开阶数略低。", parsed.comment());
    }

    @Test
    void parseJsonReply_shouldClampScoreIntoRange() {
        Assertions.assertEquals(15,
                AiGradingServiceImpl.parseGradingReply("{\"score\": 99, \"comment\": \"满分\"}", 15).score());
        Assertions.assertEquals(0,
                AiGradingServiceImpl.parseGradingReply("{\"score\": -3, \"comment\": \"无效\"}", 15).score());
    }

    @Test
    void parseLegacyTextReply_shouldStripProtocolPrefix() {
        String reply = "score:0comment:同学你好，目前提交的答案区域没有看到任何公式、步骤或结论。";

        AiGradingServiceImpl.ParsedGrading parsed = AiGradingServiceImpl.parseGradingReply(reply, 15);

        Assertions.assertEquals(0, parsed.score());
        Assertions.assertEquals("同学你好，目前提交的答案区域没有看到任何公式、步骤或结论。", parsed.comment());
        Assertions.assertFalse(parsed.comment().contains("score:"));
        Assertions.assertFalse(parsed.comment().contains("comment:"));
    }

    @Test
    void parseLegacyTextReply_shouldSupportFullWidthColonAndScoreRatio() {
        AiGradingServiceImpl.ParsedGrading fullWidth =
                AiGradingServiceImpl.parseGradingReply("score:0comment：等价无穷小使用有误。", 15);
        Assertions.assertEquals(0, fullWidth.score());
        Assertions.assertEquals("等价无穷小使用有误。", fullWidth.comment());

        AiGradingServiceImpl.ParsedGrading ratio =
                AiGradingServiceImpl.parseGradingReply("score:0/15comment:缺少极限存在性说明。", 15);
        Assertions.assertEquals(0, ratio.score());
        Assertions.assertEquals("缺少极限存在性说明。", ratio.comment());
    }

    @Test
    void parseChineseProtocolReply_shouldExtractScoreAndComment() {
        AiGradingServiceImpl.ParsedGrading parsed =
                AiGradingServiceImpl.parseGradingReply("得分：7\n评语：思路正确，结论需要补充。", 15);

        Assertions.assertEquals(7, parsed.score());
        Assertions.assertEquals("思路正确，结论需要补充。", parsed.comment());
    }

    @Test
    void parsePlainCommentReply_shouldKeepCommentAndLeaveScoreNull() {
        String reply = "本题证明过程完整，但未说明引理来源。";

        AiGradingServiceImpl.ParsedGrading parsed = AiGradingServiceImpl.parseGradingReply(reply, 15);

        Assertions.assertNull(parsed.score());
        Assertions.assertEquals(reply, parsed.comment());
    }

    @Test
    void parseBlankReply_shouldReturnPlaceholderComment() {
        AiGradingServiceImpl.ParsedGrading parsed = AiGradingServiceImpl.parseGradingReply("   ", 15);

        Assertions.assertNull(parsed.score());
        Assertions.assertTrue(parsed.comment().contains("请教师复核"));
    }
}
