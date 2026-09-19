package com.edumind;

import com.edumind.question.integration.export.PaperExportRequest;
import com.edumind.question.integration.export.PaperExportResult;
import com.edumind.question.integration.export.impl.HtmlPaperExportEngine;
import com.edumind.question.service.export.PaperExportHtmlRenderer;
import com.edumind.question.vo.question.QuestionVO;
import com.edumind.teaching.api.ExamQueryApi;
import com.edumind.teaching.vo.exam.ExamQuestionVO;
import com.edumind.teaching.vo.exam.ExamVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
public class HtmlPaperExportEngineIntegrationTest {

    @Autowired
    private PaperExportHtmlRenderer htmlRenderer;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testHtmlPaperExportEngineNoCourierError() {
        ExamQueryApi examQueryApi = Mockito.mock(ExamQueryApi.class);
        ExamVO exam = new ExamVO();
        exam.setId(501L);
        exam.setTitle("测试高等数学期中统一水平测试卷");
        exam.setTotalScore(100);
        exam.setDurationMinutes(90);

        QuestionVO q = new QuestionVO();
        q.setId(1001L);
        q.setType("SINGLE_CHOICE");
        q.setStem("当 $x \\to 0$ 时，下列无穷小量中与 $x$ 等价的无穷小量是（ ）。");
        q.setAnswer("B");
        q.setAnalysis("根据等价无穷小基本公式，当 $x \\to 0$ 时，\\ln(1+x) \\sim x；而 \\sin 2x \\sim 2x，$1-\\cos x \\sim \\frac{1}{2}x^2$。故正确答案为 B。");
        q.setOptions("[{\"key\":\"A\",\"content\":\"$\\sin 2x$\"},{\"key\":\"B\",\"content\":\"$\\ln(1 + x)$\"}]");

        ExamQuestionVO eq = new ExamQuestionVO();
        eq.setQuestionId(1001L);
        eq.setScore(5);
        eq.setSortOrder(1);
        eq.setQuestion(q);

        exam.setQuestions(List.of(eq));

        Mockito.when(examQueryApi.getExamById(501L)).thenReturn(exam);

        HtmlPaperExportEngine engine = new HtmlPaperExportEngine(examQueryApi, htmlRenderer, objectMapper);

        PaperExportRequest request = PaperExportRequest.builder()
                .taskId(12345L)
                .examId(501L)
                .exportParamsJson("{\"showAnalysis\":true,\"showAnswerSheet\":true}")
                .build();

        PaperExportResult result = engine.export(request);
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getFileBytes());
        Assertions.assertTrue(result.getFileBytes().length > 0);
        Assertions.assertEquals("application/pdf", result.getContentType());
        System.out.println(">>> PDF rendered successfully! Byte length: " + result.getFileBytes().length);
    }
}
