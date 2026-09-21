package com.edumind.statistics.service.analytics;

import com.edumind.ai.api.QuestionGenerateApi;
import com.edumind.ai.dto.QuestionGenerateDTO;
import com.edumind.ai.api.AiChatApi;
import com.edumind.common.exception.BusinessException;
import com.edumind.question.api.QuestionCommandApi;
import com.edumind.question.api.QuestionQueryApi;
import com.edumind.question.dto.question.QuestionBatchCreateDTO;
import com.edumind.question.dto.question.QuestionCreateDTO;
import com.edumind.question.vo.question.QuestionBatchSaveVO;
import com.edumind.question.vo.question.QuestionVO;
import com.edumind.statistics.dao.WrongQuestionRecordDao;
import com.edumind.statistics.entity.WrongQuestionRecordEntity;
import com.edumind.statistics.enums.WrongErrorType;
import com.edumind.statistics.support.AiCancelRegistry;
import com.edumind.common.markdown.LatexTextNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WrongQuestionDiagnosisService {

    /** 学生未作答时的固定结论：不消耗模型额度，也不产生误导性的失分类型标签 */
    public static final String UNANSWERED_DIAGNOSIS =
            "本次作答为空白（未提交任何答案），无法定位概念理解或计算环节的具体偏差，系统不作认知归因。建议先完成作答，再查看归因分析。";

    private static final String DIAGNOSIS_SYSTEM_PROMPT = "你是错题认知归因助手。";
    private static final int MAX_STEM_IN_PROMPT = 600;
    private static final int MAX_ANSWER_IN_PROMPT = 300;

    /**
     * 正在生成变式题的错题记录 ID。
     * 前端「中止」只取消了自己的等待，服务端这次模型调用仍会跑完；
     * 若用户立刻再点生成，两次并发模型调用会互相干扰并失败（表现为 500）。
     * 这里做单机在途互斥，把并发失败变成明确提示，同时避免重复消耗模型额度。
     */
    private final java.util.Set<Long> variantsInFlight = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** 中止标记命中后的统一提示：模型调用无法挽回，但结果必须丢弃 */
    private static final String ABORTED_MESSAGE = "已中止本次 AI 生成，服务端已丢弃本次结果";

    private final AiChatApi aiChatApi;
    private final AiCancelRegistry cancelRegistry;
    private final WrongQuestionRecordDao wrongQuestionRecordDao;
    private final QuestionQueryApi questionQueryApi;
    private final QuestionGenerateApi questionGenerateApi;
    private final QuestionCommandApi questionCommandApi;

    public String diagnose(String questionStem, String studentAnswer, String correctAnswer) {
        if (!StringUtils.hasText(studentAnswer)) {
            return null;
        }
        String prompt = "题目：" + questionStem + "\n学生答案：" + studentAnswer + "\n正确答案：" + correctAnswer
                + "\n请用一句话诊断错因，并标注类型 CONCEPT/LOGIC/CALC/READING 之一。";
        return callDiagnosisModel(prompt);
    }

    /**
     * 携带题型、难度、标准答案与解析的完整上下文诊断（诊断结论质量取决于上下文完整度）。
     * 返回 null 表示本次无法给出可信结论（未作答 / 模型异常），由调用方决定降级策略。
     */
    public String diagnose(QuestionVO question, String studentAnswer) {
        if (question == null || !StringUtils.hasText(studentAnswer)) {
            return null;
        }
        String prompt = "题干：" + truncateForPrompt(question.getStem(), MAX_STEM_IN_PROMPT)
                + "\n题型：" + defaultValue(question.getType(), "未知")
                + "\n难度：" + difficultyText(question.getDifficulty())
                + "\n考点：" + defaultValue(question.getKnowledgePointName(), "未标注")
                + "\n学生作答：" + truncateForPrompt(studentAnswer, MAX_ANSWER_IN_PROMPT)
                + "\n标准答案：" + truncateForPrompt(question.getAnswer(), MAX_ANSWER_IN_PROMPT)
                + "\n参考答案解析：" + defaultValue(truncateForPrompt(question.getAnalysis(), MAX_STEM_IN_PROMPT), "（无）")
                + "\n请先指明学生失分的根本原因（区分概念理解、逻辑推理、计算失误、审题偏差），"
                + "并在句末标注类型 CONCEPT/LOGIC/CALC/READING 之一；若学生实际未作答，请只说明「作答缺失」，不要臆测错因。";
        return callDiagnosisModel(prompt);
    }

    /**
     * 调用大模型获取归因结论。
     * 模型异常或返回空时返回 null，并保留日志——
     * 不再回退成 “CONCEPT: 概念理解不完整” 这类看起来正常、实则会误导学生的假结论。
     */
    private String callDiagnosisModel(String prompt) {
        try {
            String reply = aiChatApi.chat("GRADING", DIAGNOSIS_SYSTEM_PROMPT, prompt);
            if (!StringUtils.hasText(reply)) {
                log.warn("[错题归因] 大模型返回空结论，本次不更新归因结果");
                return null;
            }
            return reply.trim();
        } catch (Exception ex) {
            log.warn("[错题归因] 大模型调用失败，本次不更新归因结果：{}", ex.getMessage());
            return null;
        }
    }

    private static String truncateForPrompt(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) + "…" : trimmed;
    }

    private static String defaultValue(String text, String fallback) {
        return StringUtils.hasText(text) ? text : fallback;
    }

    private static String difficultyText(Integer difficulty) {
        if (difficulty == null) {
            return "未标注";
        }
        return switch (difficulty) {
            case 1, 2 -> "简单";
            case 3 -> "中等";
            case 4, 5 -> "困难";
            default -> "未标注";
        };
    }

    public void recordWrong(Long studentId, Long courseId, Long questionId, Long knowledgePointId,
                            String diagnosis) {
        recordWrong(studentId, courseId, questionId, knowledgePointId, diagnosis, null);
    }

    public void recordWrong(Long studentId, Long courseId, Long questionId, Long knowledgePointId,
                            String diagnosis, String lastStudentAnswer) {
        // 入库前剥离 “类型：CONCEPT” 这类供解析使用的机器标记，正文只保留面向学生的中文结论
        String cleanDiagnosis = WrongErrorType.stripTypeMarker(diagnosis);
        WrongQuestionRecordEntity existing = wrongQuestionRecordDao.findByStudentAndQuestion(studentId, questionId);
        if (existing != null) {
            existing.setWrongCount(existing.getWrongCount() != null ? existing.getWrongCount() + 1 : 2);
            if (StringUtils.hasText(diagnosis)) {
                existing.setDiagnosis(cleanDiagnosis);
                existing.setErrorTypes(extractTypes(diagnosis));
            }
            if (StringUtils.hasText(lastStudentAnswer)) {
                existing.setLastStudentAnswer(trimAnswer(lastStudentAnswer));
            }
            if (knowledgePointId != null) {
                existing.setKnowledgePointId(knowledgePointId);
            }
            existing.setStatus(0);
            existing.setMasteredTime(null);
            wrongQuestionRecordDao.updateById(existing);
            // 归因结论无对应失分类型时（例如模型判定为作答缺失），updateById 会忽略 null，
            // 需再显式清空一次，避免该题上一次的错误标签继续展示。
            if (StringUtils.hasText(diagnosis) && !StringUtils.hasText(existing.getErrorTypes())) {
                wrongQuestionRecordDao.updateDiagnosisResult(existing.getId(), cleanDiagnosis, null);
            }
            return;
        }
        WrongQuestionRecordEntity entity = new WrongQuestionRecordEntity();
        entity.setStudentId(studentId);
        entity.setCourseId(courseId);
        entity.setQuestionId(questionId);
        entity.setKnowledgePointId(knowledgePointId);
        entity.setDiagnosis(cleanDiagnosis);
        entity.setErrorTypes(extractTypes(diagnosis));
        entity.setLastStudentAnswer(trimAnswer(lastStudentAnswer));
        entity.setWrongCount(1);
        entity.setStatus(0);
        wrongQuestionRecordDao.insert(entity);
    }

    private static String trimAnswer(String answer) {
        if (answer == null) {
            return null;
        }
        return answer.length() > 1024 ? answer.substring(0, 1024) : answer;
    }

    /** 变式题生成的中止标记键（按错题记录维度） */
    public static String variantsCancelKey(Long recordId) {
        return "wrong-book:variants:" + recordId;
    }

    /** 归因诊断的中止标记键（按错题记录维度） */
    public static String diagnoseCancelKey(Long recordId) {
        return "wrong-book:diagnose:" + recordId;
    }

    /** 前端中止归因诊断：本次模型返回后丢弃结果、不再落库 */
    public void cancelDiagnosis(Long recordId) {
        cancelRegistry.markCancelled(diagnoseCancelKey(recordId));
    }

    /** 前端中止变式题生成：本次模型返回后丢弃结果，且不写入题库 */
    public void cancelVariants(Long recordId) {
        cancelRegistry.markCancelled(variantsCancelKey(recordId));
    }

    /** 仅执行 AI 认知归因诊断（单次大模型调用），与变式题生成解耦，避免一次请求串行两次模型调用 */
    public WrongQuestionRecordEntity diagnoseRecord(Long recordId) {
        WrongQuestionRecordEntity entity = requireRecord(recordId);
        String cancelKey = diagnoseCancelKey(recordId);
        // 每次新诊断开始先清掉历史中止标记，避免过期标记误伤本次生成
        cancelRegistry.clear(cancelKey);
        QuestionVO question = requireQuestion(entity);
        String studentAnswer = StringUtils.hasText(entity.getLastStudentAnswer())
                ? entity.getLastStudentAnswer() : "";

        // 未作答：作答缺失本身不构成可归因的失分模式，直接给出说明，
        // 既不调用大模型（省一次额度），也不写入 CONCEPT/READING 这类误导性标签。
        if (!StringUtils.hasText(studentAnswer)) {
            entity.setDiagnosis(UNANSWERED_DIAGNOSIS);
            entity.setErrorTypes(null);
            wrongQuestionRecordDao.updateDiagnosisResult(entity.getId(), UNANSWERED_DIAGNOSIS, null);
            return entity;
        }

        String diagnosis = diagnose(question, studentAnswer);
        // 用户在模型返回前中止：这次调用费用已无法挽回，但结果必须丢弃，不覆盖原有结论
        if (cancelRegistry.isCancelled(cancelKey)) {
            cancelRegistry.clear(cancelKey);
            log.info("[错题归因] 记录 {} 已被用户中止，丢弃本次模型结论", recordId);
            throw new BusinessException(ABORTED_MESSAGE);
        }
        if (!StringUtils.hasText(diagnosis)) {
            // 模型不可用：保留已有结论并向上抛出明确失败，避免把故障伪装成归因结论
            throw new BusinessException("AI 认知归因服务暂不可用，请稍后重试");
        }
        String errorTypes = extractTypes(diagnosis);
        // 类型 code 从原始回复中提取，但入库正文必须剥离英文标记，否则会向学生暴露 “类型：CONCEPT” 这类内部枚举
        String cleanDiagnosis = WrongErrorType.stripTypeMarker(diagnosis);
        entity.setDiagnosis(cleanDiagnosis);
        entity.setErrorTypes(errorTypes);
        // 结论可能无对应失分类型（如模型判定作答缺失），必须显式写库才能清掉旧标签
        wrongQuestionRecordDao.updateDiagnosisResult(entity.getId(), cleanDiagnosis, errorTypes);
        return entity;
    }

    /**
     * 按需生成同构变式题：调用大模型生成 2 道同考点变式题并落库，返回题目 ID 列表。
     *
     * @param regenerate false=已生成过直接复用，避免重复消耗模型额度；true=重新生成并替换上一批
     */
    public List<Long> generateVariants(Long recordId, boolean regenerate) {
        WrongQuestionRecordEntity entity = requireRecord(recordId);
        List<Long> existing = parseVariantIds(entity.getVariantQuestionIds());
        if (!regenerate && !CollectionUtils.isEmpty(existing)) {
            return existing;
        }
        if (!variantsInFlight.add(recordId)) {
            throw new BusinessException("该错题的变式题正在生成中，请稍候片刻再试");
        }
        try {
            return doGenerateVariants(entity, existing);
        } finally {
            variantsInFlight.remove(recordId);
        }
    }

    private List<Long> doGenerateVariants(WrongQuestionRecordEntity entity, List<Long> existing) {
        String cancelKey = variantsCancelKey(entity.getId());
        // 每次新生成开始先清掉历史中止标记，避免过期标记误伤本次生成
        cancelRegistry.clear(cancelKey);
        QuestionVO question = requireQuestion(entity);

        QuestionGenerateDTO generateDto = new QuestionGenerateDTO();
        generateDto.setCourseId(entity.getCourseId());
        if (entity.getKnowledgePointId() != null) {
            generateDto.setKnowledgePointIds(List.of(entity.getKnowledgePointId()));
        }
        generateDto.setCount(2);
        generateDto.setDifficulty("MEDIUM");
        generateDto.setQuestionTypes(List.of(
                question.getType() != null ? question.getType() : "SINGLE_CHOICE"));
        generateDto.setQuestionScene("错题变式");
        // 模型默认时常输出纯文本/Unicode 数学（如 lim_{x→0}），必须在源头要求 LaTeX 定界符，
        // 否则前端 KaTeX 无法渲染（读取时虽有兜底归一化，但源头规范更可靠）
        generateDto.setPromptDirective(
                "题干与选项中的所有数学表达式必须使用 LaTeX 并用 $ 包裹（行内公式如 $\\lim_{x \\to 0}\\frac{\\sin x}{x}$）；"
                        + "禁止使用纯文本或 Unicode 数学符号（如 →、×、∞），请改用 \\to、\\times、\\infty。");

        List<QuestionVO> generated;
        try {
            generated = questionGenerateApi.generate(generateDto);
        } catch (Exception ex) {
            log.warn("[错题变式题] 大模型生成失败：{}", ex.getMessage());
            throw new BusinessException("AI 变式题生成失败，请稍后重试");
        }
        if (CollectionUtils.isEmpty(generated)) {
            // 重新生成时不能静默丢掉旧题，否则界面会变成"暂无变式题"
            if (!CollectionUtils.isEmpty(existing)) {
                throw new BusinessException("本次未能生成新的变式题，请稍后重试");
            }
            return List.of();
        }
        // 用户在模型返回前中止：本次费用已无法挽回，但题目不能再写入题库污染数据
        if (cancelRegistry.isCancelled(cancelKey)) {
            cancelRegistry.clear(cancelKey);
            log.info("[错题变式题] 记录 {} 已被用户中止，丢弃本次生成结果且不入库", entity.getId());
            throw new BusinessException(ABORTED_MESSAGE);
        }

        QuestionBatchCreateDTO batchDto = new QuestionBatchCreateDTO();
        batchDto.setCourseId(entity.getCourseId());
        batchDto.setQuestions(generated.stream()
                .map(q -> toCreateDto(q, entity.getCourseId(), entity.getKnowledgePointId()))
                .collect(Collectors.toList()));
        QuestionBatchSaveVO saved;
        try {
            saved = questionCommandApi.batchSave(batchDto);
        } catch (Exception ex) {
            log.warn("[错题变式题] 变式题落库失败：{}", ex.getMessage());
            throw new BusinessException("变式题入库失败，请稍后重试");
        }
        if (saved == null || CollectionUtils.isEmpty(saved.getQuestionIds())) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>(saved.getQuestionIds());
        // 重新生成时直接替换旧的一批，避免列表随点击次数越滚越长
        entity.setVariantQuestionIds(ids.stream().map(String::valueOf).collect(Collectors.joining(",")));
        wrongQuestionRecordDao.updateById(entity);
        return ids;
    }

    private WrongQuestionRecordEntity requireRecord(Long recordId) {
        WrongQuestionRecordEntity entity = wrongQuestionRecordDao.findById(recordId);
        if (entity == null) {
            throw new BusinessException("错题记录不存在");
        }
        return entity;
    }

    private QuestionVO requireQuestion(WrongQuestionRecordEntity entity) {
        QuestionVO question = questionQueryApi.getQuestionById(entity.getQuestionId());
        if (question == null) {
            throw new BusinessException("题目不存在");
        }
        return question;
    }

    private static List<Long> parseVariantIds(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    private QuestionCreateDTO toCreateDto(QuestionVO q, Long courseId, Long knowledgePointId) {
        QuestionCreateDTO dto = new QuestionCreateDTO();
        dto.setCourseId(courseId);
        dto.setKnowledgePointId(knowledgePointId);
        // 大模型常输出裸 LaTeX（无反斜杠定界符），落库前补全 $ 定界符，保证题库/试卷/练习各处正常渲染
        dto.setStem(LatexTextNormalizer.wrapBareMath(q.getStem()));
        dto.setType(q.getType() != null ? q.getType() : "SINGLE_CHOICE");
        dto.setOptions(q.getOptions());
        dto.setAnswer(q.getAnswer());
        dto.setAnalysis(q.getAnalysis());
        dto.setDifficulty(q.getDifficulty() != null ? q.getDifficulty() : 3);
        dto.setScore(q.getScore() != null ? q.getScore() : 5);
        return dto;
    }

    private String extractTypes(String diagnosis) {
        return WrongErrorType.extractCodeFromDiagnosis(diagnosis);
    }
}
