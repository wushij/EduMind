package com.edumind.ai.service.question;

import com.edumind.ai.dto.QuestionGenerateDTO;
import com.edumind.ai.service.question.impl.PedagogicalQuestionFallbackEngine;
import com.edumind.question.vo.question.QuestionVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestionGenerateServiceTest {

    private final PedagogicalQuestionFallbackEngine engine = new PedagogicalQuestionFallbackEngine();

    @Test
    @DisplayName("验证 AI 大模型学科出题兜底：生成包含选项、答案、解析与干扰项诊断的真实试题")
    void testAiCourseQuestionGeneration() {
        QuestionGenerateDTO dto = new QuestionGenerateDTO();
        dto.setCourseId(2026L);
        dto.setCount(3);
        dto.setScorePerQuestion(5);
        dto.setDifficulty("MEDIUM");
        dto.setQuestionTypes(List.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE"));
        dto.setPromptDirective("重点考查大模型注意力机制与微调显存开销");

        List<QuestionVO> questions = engine.generateHighQualityQuestions(
                dto,
                "人工智能与大语言模型核心架构",
                List.of("Transformer 与自注意力架构", "高效微调 PEFT"),
                List.of("Transformer自注意力机制", "LoRA参数高效微调", "RoPE旋转位置编码")
        );

        assertNotNull(questions);
        assertEquals(3, questions.size());

        for (QuestionVO q : questions) {
            assertEquals(2026L, q.getCourseId());
            assertNotNull(q.getStem(), "题干不应为空");
            assertNotNull(q.getAnswer(), "正确答案不应为空");
            assertNotNull(q.getAnalysis(), "题目解析不应为空");
            assertNotNull(q.getOptions(), "选项不应为空");
            assertNotNull(q.getKnowledgePointName(), "考点名称不应为空");
            assertNotNull(q.getCognitiveLevel(), "认知层级不应为空");
            assertFalse(q.getStem().contains("Mock 题目"), "不应包含粗糙的 Mock 题目字样");
        }
    }

    @Test
    @DisplayName("验证操作系统与高并发学科出题兜底：涵盖死锁、信号量与虚拟内存")
    void testOsCourseQuestionGeneration() {
        QuestionGenerateDTO dto = new QuestionGenerateDTO();
        dto.setCourseId(101L);
        dto.setCount(2);
        dto.setScorePerQuestion(10);
        dto.setDifficulty("HARD");
        dto.setQuestionTypes(List.of("SINGLE_CHOICE", "MULTIPLE_CHOICE"));

        List<QuestionVO> questions = engine.generateHighQualityQuestions(
                dto,
                "操作系统与Linux内核架构",
                List.of("并发控制与死锁", "虚拟内存管理"),
                List.of("进程互斥与信号量PV操作", "死锁产生条件与死锁避免")
        );

        assertEquals(2, questions.size());
        assertEquals("进程互斥与信号量PV操作", questions.get(0).getKnowledgePointName());
        assertEquals(10, questions.get(0).getScore());
        assertTrue(questions.get(0).getStem().contains("操作系统") || questions.get(0).getStem().contains("进程"), "题干需包含真实操作系统专业语境");
    }
}
