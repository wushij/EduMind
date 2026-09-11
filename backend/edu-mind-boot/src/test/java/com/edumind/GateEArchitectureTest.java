package com.edumind;

import com.edumind.course.controller.course.CourseController;
import com.edumind.question.controller.bank.QuestionBankController;
import com.edumind.question.controller.question.QuestionController;
import com.edumind.statistics.controller.dashboard.DashboardController;
import com.edumind.system.controller.auth.AuthController;
import com.edumind.system.controller.system.UserManageController;
import com.edumind.teaching.controller.assignment.AssignmentController;
import com.edumind.teaching.controller.exam.ExamController;
import com.edumind.teaching.controller.submission.SubmissionController;
import cn.hutool.crypto.digest.BCrypt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Gate E 架构验收：核心 Controller 已注册且具备所需 API 路由注解（不依赖外部 MySQL / Redis 运行时）。
 */
class GateEArchitectureTest {

    @Test
    void coreControllersShouldBePresent() {
        Assertions.assertNotNull(AuthController.class);
        Assertions.assertNotNull(CourseController.class);
        Assertions.assertNotNull(ExamController.class);
        Assertions.assertNotNull(DashboardController.class);
        Assertions.assertNotNull(QuestionBankController.class);
        Assertions.assertNotNull(QuestionController.class);
        Assertions.assertNotNull(AssignmentController.class);
        Assertions.assertNotNull(SubmissionController.class);
        Assertions.assertNotNull(UserManageController.class);
    }

    @Test
    void courseControllerShouldHaveKnowledgePointsMapping() {
        boolean hasKpPost = Arrays.stream(CourseController.class.getDeclaredMethods())
                .anyMatch(m -> m.isAnnotationPresent(PostMapping.class) &&
                        Arrays.stream(m.getAnnotation(PostMapping.class).value()).anyMatch(v -> v.contains("knowledge-points")));
        Assertions.assertTrue(hasKpPost, "CourseController 必须具备 POST /{id}/knowledge-points 创建知识点接口");
    }

    @Test
    void questionBankControllerShouldHaveQuestionManagementMappings() {
        boolean hasAddQuestions = Arrays.stream(QuestionBankController.class.getDeclaredMethods())
                .anyMatch(m -> m.isAnnotationPresent(PostMapping.class) &&
                        Arrays.stream(m.getAnnotation(PostMapping.class).value()).anyMatch(v -> v.contains("questions")));
        boolean hasRemoveQuestion = Arrays.stream(QuestionBankController.class.getDeclaredMethods())
                .anyMatch(m -> m.isAnnotationPresent(DeleteMapping.class) &&
                        Arrays.stream(m.getAnnotation(DeleteMapping.class).value()).anyMatch(v -> v.contains("questions")));

        Assertions.assertTrue(hasAddQuestions, "QuestionBankController 必须具备 POST /{id}/questions 批量入库接口");
        Assertions.assertTrue(hasRemoveQuestion, "QuestionBankController 必须具备 DELETE /{id}/questions/{questionId} 移出接口");
    }

    @Test
    void submissionControllerShouldHaveGradingMappings() {
        boolean hasGradePost = Arrays.stream(SubmissionController.class.getDeclaredMethods())
                .anyMatch(m -> m.isAnnotationPresent(PostMapping.class) &&
                        Arrays.stream(m.getAnnotation(PostMapping.class).value()).anyMatch(v -> v.contains("grade")));
        boolean hasReviewPut = Arrays.stream(SubmissionController.class.getDeclaredMethods())
                .anyMatch(m -> m.isAnnotationPresent(PutMapping.class) &&
                        Arrays.stream(m.getAnnotation(PutMapping.class).value()).anyMatch(v -> v.contains("review")));

        Assertions.assertTrue(hasGradePost, "SubmissionController 必须具备 POST /{id}/grade 触发 AI 预批改接口");
        Assertions.assertTrue(hasReviewPut, "SubmissionController 必须具备 PUT /{id}/grading/review 保存复核接口");
    }

    @Test
    void examControllerShouldHaveExportMapping() {
        boolean hasExportGet = Arrays.stream(ExamController.class.getDeclaredMethods())
                .anyMatch(m -> m.isAnnotationPresent(GetMapping.class) &&
                        Arrays.stream(m.getAnnotation(GetMapping.class).value()).anyMatch(v -> v.contains("export")));
        Assertions.assertTrue(hasExportGet, "ExamController 必须具备 GET /{id}/export 试卷导出接口");
    }

    @Test
    void seedPasswordHashShouldMatchAdmin123() {
        String hash = "$2b$10$DLVcEXn5RunOfqlY84u9S.nnXViwwLRheQgk0KIIpKr9y1im4mIOq";
        Assertions.assertTrue(BCrypt.checkpw("admin123", hash));
    }
}

