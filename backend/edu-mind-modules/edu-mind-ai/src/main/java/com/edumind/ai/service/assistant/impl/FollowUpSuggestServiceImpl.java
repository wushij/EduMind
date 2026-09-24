package com.edumind.ai.service.assistant.impl;

import com.edumind.ai.dto.assistant.FollowUpSuggestDTO;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.service.assistant.FollowUpSuggestService;
import com.edumind.ai.service.prompt.PromptService;
import com.edumind.ai.vo.assistant.FollowUpSuggestVO;
import com.edumind.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 追问建议生成：让模型读完整轮问答后现生成「用户下一步会问什么」。
 *
 * <p>为什么不能继续用规则模板：规则只能把回答里的小标题套进固定句式
 * （「深入剖析「X」的底层原理」），句式与本轮内容无关，用户看到的就是一批同质化
 * 的写死提问。这里改为把真实问答全文交给模型，并在提示词中强制「必须锚定回答里出现过的
 * 具体术语/结论/题号」，同时用 {@link #parsePrompts} 做后置净化：剔除套话、
 * 编号、Markdown 残留与重复项。</p>
 *
 * <p>模型不可用时不抛异常，返回 {@code aiGenerated=false} 的空列表，由前端规则兜底，
 * 保证「追问胶囊」不会因为 AI 网关抖动而消失。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FollowUpSuggestServiceImpl implements FollowUpSuggestService {

    /** 独立场景键：网关路由页可单独为追问调度便宜的小模型 */
    private static final String SCENE = "chat_follow_up";
    private static final String TEMPLATE = "chat_follow_up";
    private static final String SYSTEM_PROMPT = "你是智教云追问设计器，只依据本轮真实问答内容生成用户接下来会问的具体问题，"
            + "只输出纯文本行，不输出编号、Markdown 与任何解释。";

    private static final int DEFAULT_COUNT = 3;
    private static final int MAX_COUNT = 5;
    private static final int MIN_PROMPT_CHARS = 4;
    private static final int MAX_PROMPT_CHARS = 40;
    private static final int MAX_QUESTION_CHARS = 1000;
    private static final int MAX_ANSWER_CHARS = 6000;
    private static final int ANSWER_TAIL_CHARS = 2000;

    /** 行首编号 / 项目符号，例如「1. 」「2）」「- 」「* 」「• 」 */
    private static final Pattern LEADING_MARKER =
            Pattern.compile("^(?:[-*•·]+|\\d{1,2}\\s*[.、)）:：]|[（(]\\d{1,2}[)）])\\s*");
    /**
     * 模型把多条追问挤进同一行时的切分点：
     * 「1. 甲？ 2. 乙？ 3. 丙？」这类输出必须先拆开，否则整行超长会被当成无效内容丢弃。
     */
    private static final Pattern MERGED_MARKER =
            Pattern.compile("(?:(?<=\\s)|(?<=[？?。！!]))(?=\\d{1,2}\\s*[.、)）:：])");
    private static final Pattern MARKDOWN_NOISE = Pattern.compile("[*`_#>]");
    private static final Pattern WRAP_QUOTES =
            Pattern.compile("^[\"'“”‘’「」《》【】]+|[\"'“”‘’「」《》【】]+$");
    /** 模型偷懒时的通用套话、客套开场与书面转述词，一律丢弃，避免又变成「写死的追问」 */
    private static final Pattern GENERIC_PROMPT = Pattern.compile(
            "还有什么(想|需要)|需要我(进一步|继续|再|展开)|希望(这些|以上|对你有帮助)|如需(更多|进一步)"
                    + "|欢迎(继续|随时)|还有(什么)?(疑问|问题吗)|你想(了解|深入|先看)哪|想(先|深入)?了解哪"
                    + "|上述(回答|内容|讲解)|上文|本文|以上(内容|回答)"
                    + "|^好的[，,。]|^当然|以下(是|为)|如下[：:]|建议(的)?(追问|问题)|希望对你");
    private static final Pattern PUNCTUATION = Pattern.compile("[\\s\\p{Punct}，。！？、；：（）【】「」《》“”‘’]+");

    private final AiGatewayFacade aiGatewayFacade;
    private final PromptService promptService;

    @Override
    public FollowUpSuggestVO suggest(FollowUpSuggestDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getQuestion()) || !StringUtils.hasText(dto.getAnswer())) {
            throw new BusinessException("缺少本轮问答内容，无法生成追问");
        }
        int count = normalizeCount(dto.getCount());

        Map<String, String> vars = new HashMap<>();
        vars.put("count", String.valueOf(count));
        vars.put("question", clip(dto.getQuestion(), MAX_QUESTION_CHARS));
        vars.put("answer", clipAnswer(dto.getAnswer()));
        vars.put("context", buildContextBlock(dto.getContext()));

        String userPrompt = promptService.renderTemplate(TEMPLATE, vars);
        if (!StringUtils.hasText(userPrompt)) {
            log.warn("[AI FollowUp] 追问提示词模板缺失，跳过模型调用 template={}", TEMPLATE);
            return empty("提示词模板缺失");
        }

        try {
            String reply = aiGatewayFacade.chat(SCENE, dto.getModelKey(), SYSTEM_PROMPT, userPrompt);
            List<String> prompts = parsePrompts(reply, count, dto.getQuestion());
            if (prompts.size() < count) {
                // 首次输出条数不足（模型常把三条挤在同一行）：用一次修复请求让它按行重出
                log.warn("[AI FollowUp] 首次输出可用条数 {}/{}，原始输出片段：{}",
                        prompts.size(), count, abbreviate(reply));
                List<String> repaired = repairPrompts(userPrompt, count, dto, prompts.size());
                if (repaired.size() > prompts.size()) {
                    prompts = repaired;
                }
            }
            if (prompts.isEmpty()) {
                log.warn("[AI FollowUp] 模型未返回可用追问 replyLength={}",
                        reply != null ? reply.length() : 0);
                return empty("模型未返回可用追问");
            }
            log.info("[AI FollowUp] 生成追问 {} 条（依据本轮问答长度 question={}, answer={}）",
                    prompts.size(), dto.getQuestion().length(), dto.getAnswer().length());
            return FollowUpSuggestVO.builder()
                    .prompts(prompts)
                    .aiGenerated(true)
                    .sourceLabel("模型依据本轮内容实时生成")
                    .build();
        } catch (Exception ex) {
            // 追问只是锦上添花，失败必须静默降级，不能把异常抛给聊天主流程
            log.warn("[AI FollowUp] 模型调用失败，交由前端规则兜底: {}", ex.getMessage());
            return empty("模型暂不可用");
        }
    }

    /**
     * 修复请求：首次输出条数不足时，明确告诉模型「上一次不合格」并要求严格按行重出。
     *
     * <p>实测模型会把三条追问写在同一行（一次调用只产出约 60 字符），
     * 这种输出即使能拆开也容易丢内容，因此宁可让它重出一个规范的版本。</p>
     */
    private List<String> repairPrompts(String userPrompt, int count, FollowUpSuggestDTO dto, int gotCount) {
        String repairPrompt = userPrompt
                + "\n\n【上一次输出不合格："
                + (gotCount == 0 ? "没有任何一条能被直接使用" : "只识别出 " + gotCount + " 条")
                + "】请重新输出：" + count + " 行，每行一条完整追问；"
                + "禁止把多条追问写在同一行，禁止编号、括号、引号与任何解释文字。";
        try {
            String retryReply = aiGatewayFacade.chat(SCENE, dto.getModelKey(), SYSTEM_PROMPT, repairPrompt);
            return parsePrompts(retryReply, count, dto.getQuestion());
        } catch (Exception ex) {
            log.warn("[AI FollowUp] 修复请求失败，沿用首次可用结果: {}", ex.getMessage());
            return List.of();
        }
    }

    /** 日志用片段：压成单行并截断，便于在控制台直接看出模型到底回了什么 */
    static String abbreviate(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String oneLine = raw.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= 300 ? oneLine : oneLine.substring(0, 300) + "…";
    }

    private FollowUpSuggestVO empty(String sourceLabel) {
        return FollowUpSuggestVO.builder()
                .prompts(List.of())
                .aiGenerated(false)
                .sourceLabel(sourceLabel)
                .build();
    }

    private int normalizeCount(Integer count) {
        if (count == null || count < 1) {
            return DEFAULT_COUNT;
        }
        return Math.min(count, MAX_COUNT);
    }

    /**
     * 净化模型输出：剥离编号与 Markdown 残留、剔除套话与超长/重复条目。
     *
     * @param raw      模型原始输出
     * @param count    期望条数
     * @param question 本轮用户提问，用于丢弃「把用户问题原样抄回来」的伪追问
     */
    static List<String> parsePrompts(String raw, int count, String question) {
        if (!StringUtils.hasText(raw) || count <= 0) {
            return List.of();
        }
        String questionKey = normalizeForCompare(question);
        Map<String, Boolean> seen = new LinkedHashMap<>();
        List<String> prompts = new ArrayList<>();

        for (String line : splitSegments(raw)) {
            String text = sanitizeLine(line);
            if (text.length() < MIN_PROMPT_CHARS || text.length() > MAX_PROMPT_CHARS) {
                continue;
            }
            if (GENERIC_PROMPT.matcher(text).find()) {
                continue;
            }
            String key = normalizeForCompare(text);
            if (key.isEmpty() || key.equals(questionKey) || seen.containsKey(key)) {
                continue;
            }
            seen.put(key, Boolean.TRUE);
            prompts.add(text);
            if (prompts.size() >= count) {
                break;
            }
        }
        return prompts;
    }

    /** 先把换行切开，再把「1. 甲？ 2. 乙？ 3. 丙？」这类被挤在同一行的多条追问拆开 */
    private static List<String> splitSegments(String raw) {
        List<String> segments = new ArrayList<>();
        for (String line : raw.split("\\r?\\n")) {
            if (!StringUtils.hasText(line)) {
                continue;
            }
            for (String part : MERGED_MARKER.split(line.trim())) {
                if (StringUtils.hasText(part)) {
                    segments.addAll(splitByQuestionMarks(part.trim()));
                }
            }
        }
        return segments;
    }

    /**
     * 连编号都没有、直接把多条追问连成一长串时的兜底：按问号切分（问号留在前一条末尾）。
     * 只有在整行已经超过长度上限、且至少出现两个问号时才动手，避免误伤正常长句。
     */
    private static List<String> splitByQuestionMarks(String text) {
        if (text.length() <= MAX_PROMPT_CHARS || countOf(text, '？') + countOf(text, '?') < 2) {
            return List.of(text);
        }
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            current.append(ch);
            if (ch == '？' || ch == '?') {
                result.add(current.toString().trim());
                current.setLength(0);
            }
        }
        if (StringUtils.hasText(current.toString())) {
            result.add(current.toString().trim());
        }
        return result;
    }

    private static int countOf(String text, char target) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == target) {
                count++;
            }
        }
        return count;
    }

    private static String sanitizeLine(String line) {
        String text = line == null ? "" : line.trim();
        if (text.isEmpty()) {
            return "";
        }
        text = LEADING_MARKER.matcher(text).replaceFirst("");
        text = MARKDOWN_NOISE.matcher(text).replaceAll("");
        text = WRAP_QUOTES.matcher(text).replaceAll("");
        return text.trim();
    }

    /** 比较用归一化：去掉空白与标点，避免「同义改写但实际重复」的追问占位 */
    private static String normalizeForCompare(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return PUNCTUATION.matcher(text.trim()).replaceAll("").toLowerCase();
    }

    private static String clip(String text, int maxChars) {
        String trimmed = text == null ? "" : text.trim();
        return trimmed.length() <= maxChars ? trimmed : trimmed.substring(0, maxChars);
    }

    /**
     * 回答截断策略：保留开头（结论与主线）与结尾（易错点、总结），中间省略。
     * 追问需要的是「回答讲了什么、最后给了什么建议」，掐掉结尾会明显降低追问质量。
     */
    static String clipAnswer(String answer) {
        String trimmed = answer == null ? "" : answer.trim();
        if (trimmed.length() <= MAX_ANSWER_CHARS) {
            return trimmed;
        }
        int head = MAX_ANSWER_CHARS - ANSWER_TAIL_CHARS;
        return trimmed.substring(0, head)
                + "\n…（中间内容已省略）…\n"
                + trimmed.substring(trimmed.length() - ANSWER_TAIL_CHARS);
    }

    private static String buildContextBlock(String context) {
        return StringUtils.hasText(context) ? "【当前场景】" + context.trim() + "\n" : "";
    }
}
