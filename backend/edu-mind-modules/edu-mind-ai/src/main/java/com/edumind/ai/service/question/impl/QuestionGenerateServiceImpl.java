package com.edumind.ai.service.question.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.edumind.ai.dto.QuestionGenerateDTO;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.prompt.question.QuestionPromptConstants;
import com.edumind.ai.service.audit.AiCallAuditContext;
import com.edumind.ai.service.prompt.PromptService;
import com.edumind.ai.service.question.QuestionGenerateService;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.UserContext;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.infrastructure.redis.cache.AiSessionCacheService;
import com.edumind.knowledge.api.KnowledgeQueryApi;
import com.edumind.question.vo.question.QuestionVO;
import com.edumind.question.util.QuestionStemValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionGenerateServiceImpl implements QuestionGenerateService {

    private static final long GENERATING_TTL_SECONDS = 120L;

    private final AiGatewayFacade aiGatewayFacade;
    private final PromptService promptService;
    private final AiSessionCacheService aiSessionCacheService;
    private final CourseQueryApi courseQueryApi;
    private final KnowledgeQueryApi knowledgeQueryApi;
    private final PedagogicalQuestionFallbackEngine fallbackEngine;

    @Override
    public List<QuestionVO> generate(QuestionGenerateDTO dto) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("未登录");
        }
        if (!aiSessionCacheService.tryStartGenerating("question", userId, GENERATING_TTL_SECONDS)) {
            throw new BusinessException("题目正在生成中，请稍候");
        }

        try {
            CourseContext context = loadCourseContext(dto);
            String systemPrompt = resolveSystemPrompt();
            String userPrompt = buildPedagogicalUserPrompt(dto, context);

            Map<String, Object> params = buildCallParams(dto);
            AiCallAuditContext auditContext = AiCallAuditContext.builder()
                    .userId(userId)
                    .tenantId(TenantContext.getTenantId())
                    .courseId(dto.getCourseId())
                    .build();

            try {
                String json = aiGatewayFacade.generateQuestions(
                        "question_generate",
                        null,
                        systemPrompt + "\n\n" + userPrompt,
                        params,
                        auditContext
                );
                List<QuestionVO> parsed = parseQuestions(json, dto);
                if (!parsed.isEmpty()) {
                    return parsed;
                }
            } catch (Exception ex) {
                log.warn("大模型生成题目调用或解析异常，启动高质量兜底引擎：{}", ex.getMessage());
            }

            // 兜底引擎保障返回高真实度教学题目
            return fallbackEngine.generateHighQualityQuestions(
                    dto,
                    context.courseName(),
                    context.chapterTitles(),
                    context.knowledgePointNames()
            );
        } finally {
            aiSessionCacheService.finishGenerating("question", userId);
        }
    }

    private CourseContext loadCourseContext(QuestionGenerateDTO dto) {
        String courseName = "课程 #" + dto.getCourseId();
        String courseCode = "";
        String courseDesc = "";
        try {
            CourseDetailVO detail = courseQueryApi.getCourseById(dto.getCourseId());
            if (detail != null) {
                if (detail.getName() != null) courseName = detail.getName();
                if (detail.getCode() != null) courseCode = detail.getCode();
                if (detail.getDescription() != null) courseDesc = detail.getDescription();
            }
        } catch (Exception e) {
            log.warn("查询课程详情异常 courseId={}: {}", dto.getCourseId(), e.getMessage());
        }

        List<String> chapterTitles = resolveChapterTitles(dto.getCourseId(), dto.getChapterIds());
        List<KnowledgePointVO> matchedKps = resolveKnowledgePoints(dto.getCourseId(), dto.getKnowledgePointIds());
        List<String> kpNames = new ArrayList<>(matchedKps.stream().map(KnowledgePointVO::getTitle).toList());
        if (dto.getKnowledgePointNames() != null) {
            for (String name : dto.getKnowledgePointNames()) {
                if (!kpNames.contains(name)) kpNames.add(name);
            }
        }

        return new CourseContext(courseName, courseCode, courseDesc, chapterTitles, kpNames, matchedKps);
    }

    private List<String> resolveChapterTitles(Long courseId, List<Long> selectedChapterIds) {
        try {
            List<ChapterTreeVO> trees = courseQueryApi.listChaptersByCourseId(courseId);
            if (trees == null || trees.isEmpty()) return List.of();
            Set<Long> filter = selectedChapterIds != null ? new HashSet<>(selectedChapterIds) : Set.of();
            List<String> titles = new ArrayList<>();
            for (ChapterTreeVO t : trees) {
                if (filter.isEmpty() || filter.contains(t.getId())) {
                    titles.add(t.getTitle());
                }
            }
            return titles;
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<KnowledgePointVO> resolveKnowledgePoints(Long courseId, List<Long> selectedKpIds) {
        try {
            List<KnowledgePointVO> list = courseQueryApi.listKnowledgePointsByCourseId(courseId);
            if (list == null || list.isEmpty()) return List.of();
            if (selectedKpIds == null || selectedKpIds.isEmpty()) return list;
            Set<Long> set = new HashSet<>(selectedKpIds);
            return list.stream().filter(k -> set.contains(k.getId())).collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    private String resolveSystemPrompt() {
        String sys = promptService.getSystemPrompt("question_generate");
        if (sys != null && sys.length() > 50) {
            return sys;
        }
        return QuestionPromptConstants.QUESTION_GENERATE_SYSTEM_PROMPT;
    }

    private String buildPedagogicalUserPrompt(QuestionGenerateDTO dto, CourseContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("【出题任务目标】\n");
        sb.append("- 课程名称：《").append(ctx.courseName()).append("》");
        if (!ctx.courseCode().isBlank()) sb.append("（").append(ctx.courseCode()).append("）");
        sb.append("\n");

        if (!ctx.chapterTitles().isEmpty()) {
            sb.append("- 考察章节范围：").append(String.join("、", ctx.chapterTitles())).append("\n");
        }
        if (!ctx.knowledgePointNames().isEmpty()) {
            sb.append("- 核心考点：").append(String.join("、", ctx.knowledgePointNames())).append("\n");
        }
        if (ctx.matchedKps() != null && !ctx.matchedKps().isEmpty()) {
            sb.append("- 知识点考查重点与认知维度：\n");
            for (KnowledgePointVO kp : ctx.matchedKps()) {
                sb.append("  * ").append(kp.getTitle());
                if (kp.getCognitiveDimension() != null) sb.append(" [").append(kp.getCognitiveDimension()).append("]");
                if (kp.getExamFocus() != null && !kp.getExamFocus().isBlank()) {
                    sb.append(" 易错/考查重点：").append(kp.getExamFocus());
                }
                sb.append("\n");
            }
        }

        sb.append("- 题型分布要求：").append(dto.getQuestionTypes() != null ? dto.getQuestionTypes() : "单选题").append("\n");
        sb.append("- 预期整体难度：").append(dto.getDifficulty() != null ? dto.getDifficulty() : "MEDIUM").append("\n");
        sb.append("- 生成题量：").append(dto.getCount() != null ? dto.getCount() : 5).append(" 题\n");
        sb.append("- 单题基准分值：").append(dto.getScorePerQuestion() != null ? dto.getScorePerQuestion() : 5).append(" 分\n");

        if (dto.getQuestionScene() != null && !dto.getQuestionScene().isBlank()) {
            sb.append("- 命题教学应用场景：").append(dto.getQuestionScene()).append("\n");
        }
        if (dto.getPromptDirective() != null && !dto.getPromptDirective().isBlank()) {
            sb.append("- 教师专属命题指令要求：").append(dto.getPromptDirective()).append("\n");
        }

        sb.append("\n请严格按照系统提示词的 JSON Schema 输出完整 questions 数组，包含题干、选项、答案、详尽解析与干扰项诊断。");
        return sb.toString();
    }

    private Map<String, Object> buildCallParams(QuestionGenerateDTO dto) {
        Map<String, Object> params = new HashMap<>();
        params.put("courseId", dto.getCourseId());
        params.put("count", dto.getCount() != null ? dto.getCount() : 5);
        params.put("scorePerQuestion", dto.getScorePerQuestion() != null ? dto.getScorePerQuestion() : 5);
        params.put("questionTypes", dto.getQuestionTypes());
        params.put("difficulty", dto.getDifficulty());
        return params;
    }

    private List<QuestionVO> parseQuestions(String raw, QuestionGenerateDTO dto) {
        List<QuestionVO> list = new ArrayList<>();
        if (raw == null || raw.isBlank()) return list;

        String jsonStr = extractPureJson(raw);
        JSONArray questions = null;
        try {
            Object parsed = JSON.parse(jsonStr);
            if (parsed instanceof JSONObject root) {
                questions = root.getJSONArray("questions");
                if (questions == null) {
                    questions = root.getJSONArray("list");
                }
                if (questions == null) {
                    questions = root.getJSONArray("data");
                }
            } else if (parsed instanceof JSONArray arr) {
                questions = arr;
            }
        } catch (Exception e) {
            log.warn("FastJSON 解析试题输出失败: {}, raw: {}", e.getMessage(), jsonStr);
            return list;
        }

        if (questions == null || questions.isEmpty()) return list;

        int scoreEach = dto.getScorePerQuestion() != null ? dto.getScorePerQuestion() : 5;
        for (int i = 0; i < questions.size(); i++) {
            JSONObject item = questions.getJSONObject(i);
            if (item == null) continue;
            QuestionVO vo = new QuestionVO();
            vo.setCourseId(dto.getCourseId());
            vo.setType(item.getString("type") != null ? item.getString("type") : "SINGLE_CHOICE");
            vo.setStem(item.getString("stem"));
            if (QuestionStemValidator.isGarbageStem(vo.getStem())) {
                continue;
            }

            Object optsObj = item.get("options");
            if (optsObj instanceof String s) {
                vo.setOptions(s);
            } else if (optsObj != null) {
                vo.setOptions(JSON.toJSONString(optsObj));
            } else {
                vo.setOptions("[]");
            }

            vo.setAnswer(item.getString("answer"));
            vo.setAnalysis(item.getString("analysis"));
            vo.setDistractorAnalysis(item.getString("distractorAnalysis"));
            vo.setKnowledgePointName(item.getString("knowledgePointName"));
            vo.setCognitiveLevel(item.getString("cognitiveLevel"));
            vo.setDifficulty(item.getInteger("difficulty") != null ? item.getInteger("difficulty") : 3);
            vo.setScore(item.getInteger("score") != null ? item.getInteger("score") : scoreEach);
            list.add(vo);
        }
        return list;
    }

    private String extractPureJson(String raw) {
        String s = raw.trim();
        // 1. 去除可能的思考链标签 <think>...</think>
        if (s.contains("</think>")) {
            s = s.substring(s.indexOf("</think>") + 8).trim();
        }
        // 2. 如果包含 ```json 代码块
        int jsonFenceStart = s.indexOf("```json");
        if (jsonFenceStart >= 0) {
            int fenceEnd = s.indexOf("```", jsonFenceStart + 7);
            if (fenceEnd > jsonFenceStart) {
                s = s.substring(jsonFenceStart + 7, fenceEnd).trim();
            }
        } else {
            int anyFenceStart = s.indexOf("```");
            if (anyFenceStart >= 0) {
                int fenceEnd = s.indexOf("```", anyFenceStart + 3);
                if (fenceEnd > anyFenceStart) {
                    s = s.substring(anyFenceStart + 3, fenceEnd).trim();
                }
            }
        }

        // 3. 智能截取最外层合法 JSON 边界（支持 { } 对象或 [ ] 数组）
        int firstBrace = s.indexOf('{');
        int firstBracket = s.indexOf('[');
        if (firstBrace >= 0 && (firstBracket < 0 || firstBrace < firstBracket)) {
            int lastBrace = s.lastIndexOf('}');
            if (lastBrace > firstBrace) {
                return s.substring(firstBrace, lastBrace + 1);
            }
        } else if (firstBracket >= 0) {
            int lastBracket = s.lastIndexOf(']');
            if (lastBracket > firstBracket) {
                return s.substring(firstBracket, lastBracket + 1);
            }
        }
        return s;
    }

    private record CourseContext(
            String courseName,
            String courseCode,
            String courseDesc,
            List<String> chapterTitles,
            List<String> knowledgePointNames,
            List<KnowledgePointVO> matchedKps
    ) {}
}
