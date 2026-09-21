package com.edumind.ai.service.grading.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.edumind.ai.dto.SubjectiveGradingDTO;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.service.audit.AiCallAuditContext;
import com.edumind.ai.service.grading.AiGradingService;
import com.edumind.ai.service.prompt.PromptService;
import com.edumind.ai.vo.SubjectiveGradingVO;
import com.edumind.common.context.TenantContext;
import com.edumind.common.model.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AiGradingServiceImpl implements AiGradingService {

    /** 主观题及格比例：达到则直接采信 AI 评分，否则转人工复核 */
    private static final double PASS_RATIO = 0.6;

    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*}");
    private static final Pattern SCORE_PREFIX = Pattern.compile(
            "^\\s*(?:score|得分|分数)\\s*[:：]\\s*-?\\d+(?:\\s*/\\s*\\d+)?\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCORE_MARK = Pattern.compile(
            "(?:score|得分|分数)\\s*[:：]\\s*(-?\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMENT_MARK = Pattern.compile(
            "^\\s*(?:comment|评语|点评|批语)\\s*[:：]\\s*", Pattern.CASE_INSENSITIVE);

    private final AiGatewayFacade aiGatewayFacade;
    private final PromptService promptService;

    @Override
    public SubjectiveGradingVO gradeSubjective(SubjectiveGradingDTO dto) {
        int maxScore = dto.getMaxScore() != null ? dto.getMaxScore() : 10;
        String userPrompt = """
                题目：%s
                参考答案：%s
                学生答案：%s
                满分：%d

                请严格按如下 JSON 输出批改结果，除该 JSON 外不要输出任何文字，也不要使用代码块围栏：
                {"score": 0 到 %d 之间的整数, "comment": "面向学生的评语，可含 Markdown 与 $...$ 公式"}
                """.formatted(
                dto.getQuestionStem(),
                dto.getReferenceAnswer(),
                dto.getStudentAnswer(),
                maxScore,
                maxScore);

        AiCallAuditContext auditContext = AiCallAuditContext.builder()
                .userId(UserContext.getUserId())
                .tenantId(TenantContext.getTenantId())
                .courseId(dto.getCourseId())
                .build();

        String reply = aiGatewayFacade.chat("subjective_grading", null,
                promptService.getSystemPrompt("subjective_grading"), userPrompt, auditContext);

        ParsedGrading parsed = parseGradingReply(reply, maxScore);
        int score = parsed.score() != null
                ? parsed.score()
                : fallbackScore(maxScore, dto.getStudentAnswer(), dto.getReferenceAnswer());
        String status = score >= maxScore * PASS_RATIO ? "AUTO_GRADED" : "PENDING_REVIEW";

        return SubjectiveGradingVO.builder()
                .score(score)
                .maxScore(maxScore)
                .aiComment(parsed.comment())
                .status(status)
                .build();
    }

    /**
     * 解析大模型批改回复。
     * <p>
     * 兼容三类输出：提示词约定的 JSON、{@code score:..comment:..} 文本格式、纯评语文本。
     * 解析结果会剥离 "score:0" / "comment:" 之类的协议前缀，避免脏数据直接落库后展示成
     * "score:0comment:同学你好…"（历史缺陷）。
     */
    static ParsedGrading parseGradingReply(String reply, int maxScore) {
        if (!StringUtils.hasText(reply)) {
            return new ParsedGrading(null, "AI 未返回批改内容，请教师复核。");
        }
        String text = reply.trim();

        // 1) JSON（提示词约定格式，兼容代码块围栏与前后多余文字）
        JSONObject json = tryParseJson(text);
        if (json != null) {
            Integer score = toInt(json.get("score"));
            String comment = normalizeComment(json.getString("comment"));
            if (score != null || comment != null) {
                String finalComment = comment != null ? comment : stripProtocolPrefix(text);
                return new ParsedGrading(clampScore(score, maxScore), finalComment);
            }
        }

        // 2) score:..comment:.. 文本格式：分数取全文首个标记，评语取剥离前缀后的正文
        Integer score = null;
        Matcher scoreMark = SCORE_MARK.matcher(text);
        if (scoreMark.find()) {
            score = toInt(scoreMark.group(1));
        }
        return new ParsedGrading(clampScore(score, maxScore), stripProtocolPrefix(text));
    }

    private static JSONObject tryParseJson(String text) {
        Matcher matcher = JSON_BLOCK.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        try {
            return JSON.parseObject(matcher.group());
        } catch (Exception ignored) {
            return null;
        }
    }

    /** 去掉开头的 score:0 / comment: 等协议前缀，仅保留真正的评语正文 */
    private static String stripProtocolPrefix(String text) {
        String result = text;
        Matcher scorePrefix = SCORE_PREFIX.matcher(result);
        if (scorePrefix.find()) {
            result = result.substring(scorePrefix.end());
        }
        Matcher commentMark = COMMENT_MARK.matcher(result);
        if (commentMark.find()) {
            result = result.substring(commentMark.end());
        }
        result = result.trim();
        return result.isEmpty() ? "AI 未返回评语，请教师复核。" : result;
    }

    private static String normalizeComment(String comment) {
        if (!StringUtils.hasText(comment)) {
            return null;
        }
        return stripProtocolPrefix(comment.trim());
    }

    private static Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String raw = String.valueOf(value).trim();
        Matcher matcher = Pattern.compile("-?\\d+").matcher(raw);
        return matcher.find() ? Integer.parseInt(matcher.group()) : null;
    }

    private static Integer clampScore(Integer score, int maxScore) {
        if (score == null) {
            return null;
        }
        return Math.max(0, Math.min(maxScore, score));
    }

    /** 模型输出无法解析时的兜底评分策略（保持历史行为，不做静默改动） */
    private int fallbackScore(int maxScore, String studentAnswer, String referenceAnswer) {
        if (!StringUtils.hasText(studentAnswer)) {
            return 0;
        }
        if (studentAnswer.trim().equalsIgnoreCase(referenceAnswer != null ? referenceAnswer.trim() : "")) {
            return maxScore;
        }
        return Math.max(1, (int) (maxScore * 0.7));
    }

    /** 解析结果：score 可能为空（解析失败），comment 永不为空 */
    record ParsedGrading(Integer score, String comment) {
    }
}
