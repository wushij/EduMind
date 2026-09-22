package com.edumind.ai.service.tool.impl;

import com.edumind.ai.dto.tool.LessonPlanDTO;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.integration.llm.LlmChatMessage;
import com.edumind.ai.integration.llm.LlmClient;
import com.edumind.ai.service.audit.AiCallAuditContext;
import com.edumind.ai.service.tool.LessonPlanService;
import com.edumind.common.context.TenantContext;
import com.edumind.common.model.UserContext;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.course.vo.course.CourseDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class LessonPlanServiceImpl implements LessonPlanService {

    /** 场景键：走网关以便统一参与场景路由、熔断降级与调用审计（此前直连 LlmClient 会绕过这些能力） */
    private static final String SCENE = "lesson_plan";

    /**
     * SSE 生成专用有界线程池。
     * 原实现使用无界 newCachedThreadPool，存在内存与线程耗尽风险；改为有界队列 + 命名线程，便于排障。
     */
    private static final int SSE_CORE_POOL_SIZE = 4;
    private static final int SSE_MAX_POOL_SIZE = 16;
    private static final int SSE_QUEUE_CAPACITY = 64;
    private static final AtomicInteger SSE_THREAD_SEQ = new AtomicInteger();

    private final AiGatewayFacade aiGatewayFacade;
    private final CourseQueryApi courseQueryApi;

    private final ExecutorService executor = new ThreadPoolExecutor(
            SSE_CORE_POOL_SIZE,
            SSE_MAX_POOL_SIZE,
            60L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(SSE_QUEUE_CAPACITY),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "lesson-plan-sse-" + SSE_THREAD_SEQ.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
            },
            new ThreadPoolExecutor.AbortPolicy());

    @Override
    public String generate(LessonPlanDTO dto) {
        return aiGatewayFacade.chat(SCENE, null, systemPrompt(), buildUserPrompt(dto), auditContext(dto));
    }

    @Override
    public SseEmitter streamGenerate(LessonPlanDTO dto) {
        SseEmitter emitter = new SseEmitter(120_000L);
        // SSE 在新线程中执行，必须显式捕获并传递租户与用户上下文，
        // 否则课程查询与调用审计会丢失租户归属（甚至串到上一个任务残留的租户）
        final Long capturedTenantId = TenantContext.getTenantId();
        final com.edumind.common.model.LoginUser capturedUser = UserContext.get();
        executor.execute(() -> {
            TenantContext.setTenantId(capturedTenantId);
            if (capturedUser != null) {
                UserContext.set(capturedUser);
            }
            try {
                aiGatewayFacade.streamChat(SCENE, null, systemPrompt(),
                        List.of(LlmChatMessage.user(buildUserPrompt(dto))), auditContext(dto),
                        () -> false,
                        new LlmClient.StreamCallback() {
                    @Override
                    public void onChunk(String content) {
                        try {
                            emitter.send(SseEmitter.event().name("delta").data("{\"content\":\"" + escape(content) + "\"}"));
                        } catch (IOException ex) {
                            emitter.completeWithError(ex);
                        }
                    }

                    @Override
                    public void onComplete() {
                        try {
                            emitter.send(SseEmitter.event().name("done").data("{}"));
                            emitter.complete();
                        } catch (IOException ex) {
                            emitter.completeWithError(ex);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        emitter.completeWithError(new IllegalStateException(message));
                    }
                });
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            } finally {
                // 无条件清理，避免线程池复用导致上下文残留串租户
                UserContext.clear();
                TenantContext.clear();
            }
        });
        return emitter;
    }

    private String systemPrompt() {
        return "你是资深高校教学设计师，请输出结构清晰、可直接用于课堂的 Markdown 教案。";
    }

    private AiCallAuditContext auditContext(LessonPlanDTO dto) {
        return AiCallAuditContext.builder()
                .userId(UserContext.getUserId())
                .tenantId(TenantContext.getTenantId())
                .courseId(dto != null ? dto.getCourseId() : null)
                .build();
    }

    private String buildUserPrompt(LessonPlanDTO dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("课程ID：").append(dto.getCourseId()).append('\n');
        CourseDetailVO course = dto.getCourseId() != null ? courseQueryApi.getCourseById(dto.getCourseId()) : null;
        if (course != null) {
            sb.append("课程名称：").append(course.getName()).append('\n');
            if (StringUtils.hasText(course.getDescription())) {
                sb.append("课程简介：").append(course.getDescription()).append('\n');
            }
            if (course.getPlannedHours() != null) {
                sb.append("计划学时：").append(course.getPlannedHours()).append('\n');
            }
            List<String> chapterTitles = flattenChapterTitles(
                    courseQueryApi.listChaptersByCourseId(dto.getCourseId()));
            if (!chapterTitles.isEmpty()) {
                sb.append("章节大纲：").append(String.join("；", chapterTitles)).append('\n');
            }
        }
        sb.append("授课主题：").append(dto.getTopic()).append('\n');
        sb.append("本节学时：").append(dto.getHours() != null ? dto.getHours() : 2).append('\n');
        sb.append("教学目标：")
                .append(dto.getObjectives() != null ? dto.getObjectives() : "掌握核心概念")
                .append('\n');
        sb.append("请结合上述真实课程信息生成：教学大纲、课堂流程、互动提问、作业建议。");
        return sb.toString();
    }

    private List<String> flattenChapterTitles(List<ChapterTreeVO> nodes) {
        List<String> titles = new ArrayList<>();
        if (nodes == null) {
            return titles;
        }
        for (ChapterTreeVO node : nodes) {
            if (node == null) {
                continue;
            }
            if (StringUtils.hasText(node.getTitle())) {
                titles.add(node.getTitle());
            }
            titles.addAll(flattenChapterTitles(node.getChildren()));
        }
        return titles;
    }

    private String escape(String token) {
        return token.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
