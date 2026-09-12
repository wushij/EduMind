package com.edumind.ai.service.tool.impl;

import com.edumind.ai.dto.tool.LessonPlanDTO;
import com.edumind.ai.integration.llm.LlmClient;
import com.edumind.ai.service.tool.LessonPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class LessonPlanServiceImpl implements LessonPlanService {

    private final LlmClient llmClient;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public String generate(LessonPlanDTO dto) {
        return llmClient.chat(systemPrompt(), buildUserPrompt(dto));
    }

    @Override
    public SseEmitter streamGenerate(LessonPlanDTO dto) {
        SseEmitter emitter = new SseEmitter(120_000L);
        executor.execute(() -> {
            try {
                llmClient.streamChat(systemPrompt(), buildUserPrompt(dto), new LlmClient.StreamCallback() {
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
            }
        });
        return emitter;
    }

    private String systemPrompt() {
        return "你是资深高校教学设计师，请输出结构清晰、可直接用于课堂的 Markdown 教案。";
    }

    private String buildUserPrompt(LessonPlanDTO dto) {
        return "课程ID：" + dto.getCourseId() + "\n"
                + "授课主题：" + dto.getTopic() + "\n"
                + "学时：" + (dto.getHours() != null ? dto.getHours() : 2) + "\n"
                + "教学目标：" + (dto.getObjectives() != null ? dto.getObjectives() : "掌握核心概念") + "\n"
                + "请生成：教学大纲、课堂流程、互动提问、作业建议。";
    }

    private String escape(String token) {
        return token.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
