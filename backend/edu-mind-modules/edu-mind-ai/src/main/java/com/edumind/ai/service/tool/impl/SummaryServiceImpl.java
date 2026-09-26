package com.edumind.ai.service.tool.impl;

import com.alibaba.fastjson2.JSON;
import com.edumind.ai.converter.SummaryRecordConverter;
import com.edumind.ai.dao.SummaryRecordDao;
import com.edumind.ai.dto.tool.SummaryGenerateDTO;
import com.edumind.ai.dto.tool.SummaryRecordRenameDTO;
import com.edumind.ai.entity.SummaryRecordEntity;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.integration.llm.LlmChatMessage;
import com.edumind.ai.integration.llm.LlmChatOptions;
import com.edumind.ai.integration.llm.LlmClient;
import com.edumind.ai.integration.llm.LlmProperties;
import com.edumind.ai.integration.llm.LlmStreamRelay;
import com.edumind.ai.prompt.AiPromptConstants;
import com.edumind.ai.service.audit.AiCallAuditContext;
import com.edumind.ai.service.prompt.PromptService;
import com.edumind.ai.service.tool.SummaryMode;
import com.edumind.ai.service.tool.SummaryService;
import com.edumind.ai.service.tool.SummaryStreamRegistry;
import com.edumind.ai.vo.tool.SummaryRecordDetailVO;
import com.edumind.ai.vo.tool.SummaryRecordVO;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.course.CourseVO;
import com.edumind.knowledge.api.KnowledgeQueryApi;
import com.edumind.knowledge.vo.knowledge.KnowledgeDocumentVO;
import com.edumind.security.context.LoginUserResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * AI 智能总结服务实现。
 *
 * <p>核心职责：把「取源 → 取专属提示词 → 走 AI 网关调模型 → 落库 → 组装 VO」收敛到一处，
 * 同步与流式两条链路共享同一套取源/提示词/落库逻辑，避免两处实现漂移。</p>
 *
 * <p><b>必须经 {@link AiGatewayFacade} 调用模型</b>：网关负责按「场景路由 → 平台默认模型 → 可用模型」
 * 解析出真实可用的模型配置、失败重试与 fallback，并写入 ai_call_log 审计。
 * 直接注入 {@code LlmClient} 会绕过模型路由，落到全局 {@code ai.llm} 配置（无 API Key 时直接抛异常），
 * 表现为「其他 AI 功能正常、唯独这里报生成失败」。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryServiceImpl implements SummaryService {

    /** AI 网关业务场景：可在「网关路由与模型调度规则」中为该场景单独指定首选模型 */
    private static final String SCENE = "SUMMARY";
    /** 单次送入模型的原始资料上限（字符）：超长文档只取前部，避免 Prompt 溢出与费用失控 */
    private static final int MAX_SOURCE_CHARS = 12000;
    /** 来源摘要片段长度 */
    private static final int SOURCE_EXCERPT_CHARS = 500;
    /** 手填标题最长字符数（与 DTO 校验口径一致） */
    private static final int TITLE_MAX_CHARS = 60;
    /** AI 自动命名的标题最长字符数：标题必须短，超长直接截断 */
    private static final int TITLE_AI_MAX_CHARS = 30;
    /** 送给 AI 起标题的正文节选长度：标题只需看开头即可判断主题，没必要把全文再送一遍 */
    private static final int TITLE_AI_EXCERPT_CHARS = 1500;
    /** AI 命名专用提示词模板编码 */
    private static final String TITLE_TEMPLATE_CODE = "SUMMARY_TITLE";
    /** AI 命名提示词模板缺失时的兜底 */
    private static final String FALLBACK_TITLE_PROMPT =
            "你是教学资料的标题拟定助手。只输出标题本身，不要引号、书名号、Markdown 标记与结尾标点，"
                    + "长度控制在 8~20 个汉字，同时体现资料主题与总结模式。";
    /** 专属提示词模板缺失时的兜底系统提示 */
    private static final String FALLBACK_SYSTEM_PROMPT =
            "你是智教云 EduMind 教学总结助手，请依据原始资料输出结构化、可复习的总结，使用 Markdown。";

    /**
     * 总结场景专属的<b>输出预算纪律</b>（比 {@link AiPromptConstants#REASONING_DEPTH_DISCIPLINE} 更严格）。
     *
     * <p>本功能一次要产出整篇长总结，而推理模型的思考链与正文<b>共用同一个 max_tokens</b>。
     * 实测资料稍长时模型会把预算全烧在思考里，流结束时 {@code finish_reason=length}、
     * 只回推理不回正文，用户端表现为「思考了一大段然后报错、结果区空白」。
     * 因此这里把思考长度硬压到 800 字以内并强制先出正文。</p>
     */
    private static final String SUMMARY_REASONING_BUDGET_DISCIPLINE = """

            【输出预算纪律（本功能最高优先级）】
            1. 本任务一次要产出整篇总结，思考链与正文共用同一输出预算：思考请控制在 1200 字以内，
               并务必为正文留足篇幅——宁可压缩思考，也不要压缩正文。
            2. 思考中只做四件事：资料主题定位、章节结构盘点、输出结构大纲、考点与主次取舍；
               禁止复述原文、逐段翻译或自我辩论。
            3. 思考完成后必须立即开始输出 Markdown 正文，严禁出现「只有思考、没有正文」的情况。
            4. 正文务必一次性输出完整，不要在结尾追加「如需继续请告诉我」之类的截断式表达。
            """;

    /** 模型只回推理、未产出正文时的提示：用户需要可执行的处置动作，而不是一句「请稍后重试」 */
    private static final String EMPTY_CONTENT_MESSAGE =
            "模型本轮只输出了思考过程、没有产出正文（输出预算被推理占用）。"
                    + "请缩短资料、改用更轻的总结模式，或前往「AI 运维 → AI 模型接入」调大该模型的最大输出 Token"
                    + "（或降低思考强度）后重试。";

    private final AiGatewayFacade aiGatewayFacade;
    private final LlmProperties llmProperties;
    private final KnowledgeQueryApi knowledgeQueryApi;
    private final CourseQueryApi courseQueryApi;
    private final PromptService promptService;
    private final SummaryRecordDao summaryRecordDao;
    private final SummaryRecordConverter summaryRecordConverter;
    private final SummaryStreamRegistry summaryStreamRegistry;

    @Override
    public SummaryRecordVO generate(SummaryGenerateDTO dto) {
        Long userId = LoginUserResolver.requireUserId();
        ResolvedSource source = resolveSource(dto);
        SummaryMode mode = SummaryMode.from(dto.getMode());

        // 网关内部已完成「场景路由 → 平台默认模型」解析、失败重试与 fallback；
        // 模型不可用时抛出带真实原因的 BusinessException，由全局异常处理回给前端
        String content = aiGatewayFacade.chat(SCENE, null, buildSystemPrompt(mode),
                buildUserPrompt(mode, source), auditContext(userId, dto));
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(EMPTY_CONTENT_MESSAGE);
        }
        SummaryRecordEntity saved = persist(userId, dto, source, mode, content.trim());
        return toVO(saved);
    }

    @Override
    public SseEmitter stream(SummaryGenerateDTO dto) {
        Long userId = LoginUserResolver.requireUserId();
        Long tenantId = TenantContext.requireTenantId();
        // 取源与提示词组装在请求线程内完成：失败可立即返回 400，而不是先建立 SSE 再报错
        ResolvedSource source = resolveSource(dto);
        SummaryMode mode = SummaryMode.from(dto.getMode());
        String systemPrompt = buildSystemPrompt(mode);
        String userPrompt = buildUserPrompt(mode, source);

        String streamId = summaryStreamRegistry.register();
        SseEmitter emitter = new SseEmitter(llmProperties.getTimeoutMs().longValue());
        sendEvent(emitter, "stream", Map.of("streamId", streamId));

        final LoginUser currentUser = UserContext.get();
        CompletableFuture.runAsync(() -> {
            // SSE 异步线程丢失了主线程的租户/登录上下文，必须显式回填，
            // 否则网关审计与落库都会因取不到 tenantId / userId 而失败
            TenantContext.setTenantId(tenantId);
            UserContext.set(currentUser != null ? currentUser : LoginUser.builder().id(userId).build());
            try {
                doStream(emitter, streamId, userId, dto, source, mode, systemPrompt, userPrompt);
            } finally {
                UserContext.clear();
                TenantContext.clear();
            }
        });
        return emitter;
    }

    @Override
    public void cancelStream(String streamId) {
        summaryStreamRegistry.cancel(streamId);
    }

    @Override
    public List<SummaryRecordVO> listRecords(Long courseId, String keyword) {
        Long userId = LoginUserResolver.requireUserId();
        List<SummaryRecordEntity> entities = summaryRecordDao.listByUser(userId, courseId, keyword);
        Map<Long, String> courseNames = resolveCourseNames(entities);
        List<SummaryRecordVO> result = new ArrayList<>(entities.size());
        for (SummaryRecordEntity entity : entities) {
            SummaryRecordVO vo = summaryRecordConverter.toVO(entity);
            vo.setCourseName(courseNameOf(entity.getCourseId(), courseNames));
            result.add(vo);
        }
        return result;
    }

    @Override
    public SummaryRecordDetailVO getRecord(Long id) {
        Long userId = LoginUserResolver.requireUserId();
        SummaryRecordEntity entity = requireOwnedRecord(id, userId);
        SummaryRecordDetailVO vo = summaryRecordConverter.toDetailVO(entity);
        vo.setCourseName(resolveCourseName(entity.getCourseId()));
        return vo;
    }

    @Override
    public void renameRecord(Long id, SummaryRecordRenameDTO dto) {
        Long userId = LoginUserResolver.requireUserId();
        SummaryRecordEntity entity = requireOwnedRecord(id, userId);
        entity.setTitle(truncate(dto.getTitle().trim(), TITLE_MAX_CHARS));
        entity.setUpdateTime(LocalDateTime.now());
        summaryRecordDao.updateById(entity);
    }

    @Override
    public void deleteRecord(Long id) {
        Long userId = LoginUserResolver.requireUserId();
        requireOwnedRecord(id, userId);
        summaryRecordDao.deleteById(id);
    }

    // ==================== 流式生成 ====================

    private void doStream(SseEmitter emitter, String streamId, Long userId, SummaryGenerateDTO dto,
                          ResolvedSource source, SummaryMode mode, String systemPrompt, String userPrompt) {
        StringBuilder contentBuffer = new StringBuilder();
        StringBuilder reasoningBuffer = new StringBuilder();
        // 网关失败时会先通过 relay 抛出 error 事件，这里记录一下，
        // 避免流结束后再补一条「模型未返回有效内容」把真实故障原因顶掉
        AtomicBoolean streamErrored = new AtomicBoolean(false);
        try {
            LlmClient.StreamCallback relay = LlmStreamRelay.create(
                    (eventName, payload) -> {
                        if ("error".equals(eventName)) {
                            streamErrored.set(true);
                        }
                        if (summaryStreamRegistry.isCancelled(streamId)) {
                            return;
                        }
                        sendEvent(emitter, eventName, payload);
                    },
                    contentBuffer,
                    reasoningBuffer);

            // 网关负责模型解析、重试、fallback 与审计；cancelled 会传导到上游读取循环，
            // 用户点「中止」后立即断开连接、停止计费
            aiGatewayFacade.streamChat(SCENE, null, systemPrompt, List.of(LlmChatMessage.user(userPrompt)),
                    auditContext(userId, dto),
                    () -> summaryStreamRegistry.isCancelled(streamId),
                    relay);

            if (summaryStreamRegistry.isCancelled(streamId)) {
                emitter.complete();
                return;
            }
            if (streamErrored.get()) {
                emitter.complete();
                return;
            }

            String content = contentBuffer.toString().trim();
            if (!StringUtils.hasText(content)) {
                // 有推理无正文 = 输出预算被思考链吃光，必须给出可执行的处置建议；
                // 完全没有推理才回落到通用文案
                String message = reasoningBuffer.length() > 0
                        ? EMPTY_CONTENT_MESSAGE
                        : "模型未返回有效总结内容，请稍后重试";
                sendEvent(emitter, "error", Map.of("message", message));
                emitter.complete();
                return;
            }

            SummaryRecordEntity saved = persist(userId, dto, source, mode, content);
            Map<String, Object> done = new HashMap<>();
            done.put("recordId", saved.getId());
            done.put("title", saved.getTitle());
            done.put("wordCount", saved.getWordCount());
            done.put("content", saved.getContent());
            done.put("courseName", resolveCourseName(saved.getCourseId()));
            sendEvent(emitter, "done", done);
            emitter.complete();
        } catch (Exception ex) {
            log.warn("[AI Summary] 流式生成失败 userId={} documentId={}: {}",
                    userId, source.documentId(), ex.getMessage());
            if (!streamErrored.get()) {
                sendEvent(emitter, "error", Map.of("message", "总结生成失败，请稍后重试"));
            }
            emitter.complete();
        } finally {
            summaryStreamRegistry.remove(streamId);
        }
    }

    // ==================== 取源 / 提示词 / 落库 ====================

    private ResolvedSource resolveSource(SummaryGenerateDTO dto) {
        if (dto == null) {
            throw new BusinessException("请求参数不能为空");
        }
        if (StringUtils.hasText(dto.getContent())) {
            String text = truncate(dto.getContent().trim(), MAX_SOURCE_CHARS);
            return new ResolvedSource("TEXT", null, null, text);
        }
        if (dto.getDocumentId() != null) {
            String text = knowledgeQueryApi.getDocumentText(dto.getDocumentId());
            if (!StringUtils.hasText(text)) {
                throw new BusinessException("该文档尚未完成解析或正文为空，无法生成总结");
            }
            KnowledgeDocumentVO doc = knowledgeQueryApi.getDocumentById(dto.getDocumentId());
            String name = doc != null && StringUtils.hasText(doc.getFileName())
                    ? doc.getFileName() : "知识库文档";
            return new ResolvedSource("DOCUMENT", dto.getDocumentId(), name,
                    truncate(text, MAX_SOURCE_CHARS));
        }
        throw new BusinessException("请选择知识库文档或粘贴需要总结的文本");
    }

    /**
     * 取该模式的<b>专属系统提示词</b>：优先后台「Prompt 模板库」中已发布的模板，
     * 其次 classpath:prompt/{code}.st，最后才是兜底文案。
     *
     * <p>末尾统一追加平台的 Markdown 排版纪律与总结专属输出预算纪律：
     * 前者保证标题 / 列表 / 公式可被前端正确渲染，后者防止推理链吃光输出预算导致「只有思考没有正文」。
     * 追加在代码里而不是模板里，模板在「Prompt 模板库」被编辑后也不会丢掉这两条约束。</p>
     */
    private String buildSystemPrompt(SummaryMode mode) {
        String prompt = promptService.getSystemPrompt(mode.getTemplateCode());
        String base = StringUtils.hasText(prompt)
                ? prompt
                : FALLBACK_SYSTEM_PROMPT + "\n当前模式：" + mode.getLabel();
        return base + AiPromptConstants.HEADING_AND_LIST_FORMAT_DISCIPLINE + SUMMARY_REASONING_BUDGET_DISCIPLINE;
    }

    /** 取该模式的专属用户提示词模板（{{sourceName}} / {{sourceText}}）；模板缺失时用内置拼接兜底。 */
    private String buildUserPrompt(SummaryMode mode, ResolvedSource source) {
        Map<String, String> variables = new HashMap<>();
        variables.put("sourceName", StringUtils.hasText(source.documentName())
                ? source.documentName() : "自由文本");
        variables.put("sourceText", source.text());
        String rendered = promptService.renderUserContent(mode.getTemplateCode(), variables);
        return StringUtils.hasText(rendered) ? rendered : "请总结以下资料：\n\n" + source.text();
    }

    private AiCallAuditContext auditContext(Long userId, SummaryGenerateDTO dto) {
        return AiCallAuditContext.builder()
                .userId(userId)
                .tenantId(TenantContext.getTenantId())
                .courseId(dto.getCourseId())
                .build();
    }

    private SummaryRecordEntity persist(Long userId, SummaryGenerateDTO dto, ResolvedSource source,
                                        SummaryMode mode, String content) {
        SummaryRecordEntity entity = new SummaryRecordEntity();
        entity.setUserId(userId);
        entity.setCourseId(dto.getCourseId());
        entity.setSourceType(source.sourceType());
        entity.setDocumentId(source.documentId());
        entity.setDocumentName(source.documentName());
        entity.setSummaryMode(mode.name());
        entity.setTitle(resolveTitle(dto.getTitle(), source, mode, content, auditContext(userId, dto)));
        entity.setSourceExcerpt(buildExcerpt(source.text()));
        entity.setContent(content);
        entity.setSourceLength(source.text().length());
        entity.setWordCount(countNonWhitespace(content));
        LocalDateTime now = LocalDateTime.now();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        summaryRecordDao.insert(entity);
        return entity;
    }

    /**
     * 解析总结标题：手填优先 → AI 命名 → 「来源名·模式」规则兜底。
     *
     * <p>AI 命名在正文生成完成后、落库之前进行（标题要随 done 事件一起返回，
     * 否则用户会先看到规则名、刷新后才变），失败时静默回落到规则命名，不影响主流程。</p>
     */
    private String resolveTitle(String customTitle, ResolvedSource source, SummaryMode mode,
                                String content, AiCallAuditContext audit) {
        if (StringUtils.hasText(customTitle)) {
            return truncate(customTitle.trim(), TITLE_MAX_CHARS);
        }
        String aiTitle = generateAiTitle(source, mode, content, audit);
        if (StringUtils.hasText(aiTitle)) {
            return aiTitle;
        }
        String base = StringUtils.hasText(source.documentName()) ? source.documentName() : "自由文本";
        return truncate(base + "·" + mode.getLabel(), TITLE_MAX_CHARS);
    }

    /** AI 命名：失败或结果为空时返回 null，由调用方回落到规则命名 */
    private String generateAiTitle(ResolvedSource source, SummaryMode mode, String content,
                                   AiCallAuditContext audit) {
        try {
            String systemPrompt = promptService.getSystemPrompt(TITLE_TEMPLATE_CODE);
            if (!StringUtils.hasText(systemPrompt)) {
                systemPrompt = FALLBACK_TITLE_PROMPT;
            }
            String sourceName = StringUtils.hasText(source.documentName()) ? source.documentName() : "自由文本";
            String excerpt = truncate(content, TITLE_AI_EXCERPT_CHARS);
            Map<String, String> variables = new HashMap<>();
            variables.put("sourceName", sourceName);
            variables.put("modeLabel", mode.getLabel());
            variables.put("excerpt", excerpt);
            String userPrompt = promptService.renderUserContent(TITLE_TEMPLATE_CODE, variables);
            if (!StringUtils.hasText(userPrompt)) {
                userPrompt = "【资料名称】\n" + sourceName
                        + "\n\n【总结模式】\n" + mode.getLabel()
                        + "\n\n【总结正文节选】\n" + excerpt;
            }
            // 起标题是轻量任务：关掉深度思考（disableThinking）并限制输出长度，
            // 既省一次推理预算，也避免用户为标题多等好几秒
            String raw = aiGatewayFacade.chatWithMeta(SCENE, null, systemPrompt, userPrompt,
                    LlmChatOptions.forQueryRewrite(0.20, 64), audit).content();
            return sanitizeAiTitle(raw);
        } catch (Exception ex) {
            log.warn("[AI Summary] AI 命名失败，回落规则命名: {}", ex.getMessage());
            return null;
        }
    }

    /** 清洗模型返回的标题：只取首行，剥掉 Markdown 标记 / 引号 / 书名号 / 结尾标点，并硬截断 */
    private String sanitizeAiTitle(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String title = raw.trim();
        int lineBreak = title.indexOf('\n');
        if (lineBreak > 0) {
            title = title.substring(0, lineBreak);
        }
        title = title.replaceAll("^[#>*\\-\\s]+", "")
                .replaceAll("^[\"'「『《【]+", "")
                .replaceAll("[\"'」』》】]+$", "")
                .replaceAll("[。．.、,，：:；;！!？?]+$", "")
                .trim();
        title = truncate(title, TITLE_AI_MAX_CHARS);
        return StringUtils.hasText(title) ? title : null;
    }

    // ==================== 归属校验 / 课程名解析 ====================

    private SummaryRecordEntity requireOwnedRecord(Long id, Long userId) {
        SummaryRecordEntity entity = summaryRecordDao.findById(id);
        if (entity == null || !Objects.equals(userId, entity.getUserId())) {
            throw new BusinessException("总结记录不存在或无权访问");
        }
        return entity;
    }

    private SummaryRecordVO toVO(SummaryRecordEntity entity) {
        SummaryRecordVO vo = summaryRecordConverter.toVO(entity);
        vo.setCourseName(resolveCourseName(entity.getCourseId()));
        return vo;
    }

    private Map<Long, String> resolveCourseNames(List<SummaryRecordEntity> entities) {
        List<Long> courseIds = entities.stream()
                .map(SummaryRecordEntity::getCourseId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        try {
            return courseQueryApi.listCoursesByIds(courseIds).stream()
                    .filter(c -> c.getId() != null)
                    .collect(Collectors.toMap(CourseVO::getId,
                            c -> c.getName() != null ? c.getName() : "", (a, b) -> a));
        } catch (Exception ex) {
            log.warn("[AI Summary] 批量解析课程名失败: {}", ex.getMessage());
            return Map.of();
        }
    }

    private String courseNameOf(Long courseId, Map<Long, String> courseNames) {
        return courseId == null ? null : courseNames.get(courseId);
    }

    private String resolveCourseName(Long courseId) {
        if (courseId == null) {
            return null;
        }
        try {
            var course = courseQueryApi.getCourseById(courseId);
            return course != null ? course.getName() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    // ==================== 通用工具 ====================

    private void sendEvent(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(JSON.toJSONString(data)));
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        }
    }

    private String buildExcerpt(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return truncate(normalized, SOURCE_EXCERPT_CHARS);
    }

    /** 统计去空白后的有效字符数（与前端展示的「字数」口径保持一致） */
    private int countNonWhitespace(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }

    /** 已解析完成的总结资料来源 */
    private record ResolvedSource(String sourceType, Long documentId, String documentName, String text) {
    }
}
