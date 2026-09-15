package com.edumind;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

/**
 * Gate GA-1 · 台账 100% 迁移验证：TENANT_TABLES 与 schema tenant_id 列对齐
 */
@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
public class TenantLegacyMigrationIntegrationTest {

    private static final Set<String> TENANT_TABLES = Set.of(
            "sys_campus", "sys_organization", "sys_term", "sys_tenant_member", "sys_member_org",
            "sys_tenant_quota", "sys_org_quota", "ai_memory_namespace", "knowledge_ocr_task",
            "export_task", "teaching_intervention", "course", "knowledge_base", "ai_conversation",
            "ai_call_log", "edu_question", "sys_notification", "sys_notification_broadcast",
            "security_key_version", "sys_oper_log",
            "course_chapter", "course_member", "course_resource", "course_knowledge_point",
            "knowledge_document", "knowledge_document_text", "knowledge_document_chunk",
            "knowledge_index_task", "knowledge_chunk_index", "knowledge_ocr_page",
            "teaching_exam", "exam_question", "assignment", "assignment_submission", "submission_answer",
            "ai_message", "ai_memory_item", "ai_memory_feedback", "agent_run",
            "question_bank", "question_option", "question_bank_item", "grading_result", "teaching_resource",
            "agent_step", "agent_tool_call", "prompt_template", "prompt_template_version",
            "sys_user_preference", "statistics_daily_snapshot", "learning_record", "knowledge_mastery",
            "wrong_question_record", "course_statistics", "knowledge_point_relation"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Gate GA-1: TENANT_TABLES 共 55 项且均含 tenant_id 列")
    void tenantTablesHaveTenantIdColumn() {
        Assertions.assertEquals(55, TENANT_TABLES.size(), "TENANT_TABLES 清单应与 MybatisPlusConfig 一致");

        for (String table : TENANT_TABLES) {
            List<String> columns = jdbcTemplate.queryForList(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = 'tenant_id'",
                    String.class,
                    table
            );
            Assertions.assertFalse(columns.isEmpty(),
                    "表 " + table + " 缺少 tenant_id 列，请执行 V2_2_0/V2_2_1 迁移或重建 init.sql");
        }
    }
}
