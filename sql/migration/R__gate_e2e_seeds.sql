-- =============================================================================
-- Gate F / G / H 集成测试种子（幂等，可重复执行）
-- 用途：E2E 与集成测试专用数据，init.sql 未包含 Gate F 隔离场景
-- 执行：mysql -u root -p edumind < sql/migration/R__gate_e2e_seeds.sql
-- =============================================================================

USE edumind;

-- -----------------------------------------------------------------------------
-- Gate F：teacher2 独占课程 104 + 知识库 3（数据隔离测试）
-- -----------------------------------------------------------------------------
INSERT INTO sys_user (id, username, password, real_name, email, phone, avatar, status)
SELECT 5, 'teacher2', '$2b$10$DLVcEXn5RunOfqlY84u9S.nnXViwwLRheQgk0KIIpKr9y1im4mIOq',
       '李老师', 'teacher2@edumind.edu', '13800000005',
       'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 'ENABLE'
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 5 OR username = 'teacher2');

-- V2.6.4 起按租户维度授权：teacher2 为演示租户(1)内的教师
INSERT INTO sys_user_role (user_id, role_id, tenant_id)
SELECT 5, 2, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 5 AND role_id = 2 AND tenant_id = 1);

INSERT INTO course (id, title, code, teacher_id, semester, description, cover_image, status)
SELECT 104, 'Gate F 隔离测试课程', 'GATE104', 5, '2025秋',
       '仅 teacher2 可访问，用于 Gate F 数据隔离 E2E', NULL, 1
WHERE NOT EXISTS (SELECT 1 FROM course WHERE id = 104);

INSERT INTO course_member (course_id, user_id, member_role)
SELECT 104, 5, 'TEACHER'
WHERE NOT EXISTS (SELECT 1 FROM course_member WHERE course_id = 104 AND user_id = 5);

INSERT INTO knowledge_base (id, name, course_id, description, document_count, chunk_count, index_status, status)
SELECT 3, 'Gate F 隔离测试知识库', 104, 'teacher2 专属，teacher 不可访问', 0, 0, 'PENDING', 1
WHERE NOT EXISTS (SELECT 1 FROM knowledge_base WHERE id = 3);

-- -----------------------------------------------------------------------------
-- Gate G：掌握度 / 图谱 / 错题 / 学习记录（课程 102）
-- -----------------------------------------------------------------------------
INSERT INTO knowledge_mastery (student_id, course_id, knowledge_point_id, mastery_score, sample_count, last_assessed_at)
SELECT 3, 102, 14, 0.8500, 5, NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_mastery WHERE student_id = 3 AND knowledge_point_id = 14);

INSERT INTO knowledge_mastery (student_id, course_id, knowledge_point_id, mastery_score, sample_count, last_assessed_at)
SELECT 3, 102, 15, 0.6400, 4, NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_mastery WHERE student_id = 3 AND knowledge_point_id = 15);

INSERT INTO knowledge_mastery (student_id, course_id, knowledge_point_id, mastery_score, sample_count, last_assessed_at)
SELECT 3, 102, 16, 0.7800, 3, NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_mastery WHERE student_id = 3 AND knowledge_point_id = 16);

INSERT INTO knowledge_mastery (student_id, course_id, knowledge_point_id, mastery_score, sample_count, last_assessed_at)
SELECT 4, 102, 14, 0.8200, 4, NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_mastery WHERE student_id = 4 AND knowledge_point_id = 14);

INSERT INTO knowledge_mastery (student_id, course_id, knowledge_point_id, mastery_score, sample_count, last_assessed_at)
SELECT 4, 102, 15, 0.7000, 3, NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_mastery WHERE student_id = 4 AND knowledge_point_id = 15);

INSERT INTO knowledge_mastery (student_id, course_id, knowledge_point_id, mastery_score, sample_count, last_assessed_at)
SELECT 4, 102, 16, 0.7500, 3, NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_mastery WHERE student_id = 4 AND knowledge_point_id = 16);

INSERT INTO knowledge_point_relation (source_knowledge_point_id, target_knowledge_point_id, relation_type)
SELECT 15, 14, 'prerequisite'
WHERE NOT EXISTS (
    SELECT 1 FROM knowledge_point_relation
    WHERE source_knowledge_point_id = 15 AND target_knowledge_point_id = 14 AND relation_type = 'prerequisite'
);

INSERT INTO wrong_question_record (student_id, course_id, question_id, knowledge_point_id, error_types, diagnosis, wrong_count)
SELECT 3, 102, 1007, 16, 'CONCEPT,LOGIC', '混淆编译期与运行期绑定', 3
WHERE NOT EXISTS (SELECT 1 FROM wrong_question_record WHERE student_id = 3 AND question_id = 1007);

INSERT INTO learning_record (student_id, course_id, action_type, duration_minutes)
SELECT 3, 102, 'STUDY', 45
WHERE NOT EXISTS (SELECT 1 FROM learning_record WHERE student_id = 3 AND course_id = 102 AND action_type = 'STUDY' LIMIT 1);

INSERT INTO learning_record (student_id, course_id, action_type, duration_minutes)
SELECT 4, 102, 'STUDY', 60
WHERE NOT EXISTS (SELECT 1 FROM learning_record WHERE student_id = 4 AND course_id = 102 AND action_type = 'STUDY' LIMIT 1);

-- -----------------------------------------------------------------------------
-- Gate H：ai_call_log / course_statistics 深化样本（课程 102）
-- -----------------------------------------------------------------------------
INSERT INTO ai_call_log (user_id, course_id, model, scene, prompt_tokens, completion_tokens, latency_ms, create_time)
SELECT 2, 102, 'mock', 'GLOBAL_ASSISTANT', 120, 80, 450, DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE NOT EXISTS (
    SELECT 1 FROM ai_call_log WHERE user_id = 2 AND course_id = 102 AND scene = 'GLOBAL_ASSISTANT' LIMIT 1
);

INSERT INTO ai_call_log (user_id, course_id, model, scene, prompt_tokens, completion_tokens, latency_ms, create_time)
SELECT 2, 102, 'mock', 'CHAT_RAG', 200, 150, 680, DATE_SUB(NOW(), INTERVAL 2 DAY)
WHERE NOT EXISTS (
    SELECT 1 FROM ai_call_log WHERE user_id = 2 AND course_id = 102 AND scene = 'CHAT_RAG' LIMIT 1
);

INSERT INTO course_statistics (course_id, stat_date, student_count, avg_score, mastery_avg, ai_call_count, wrong_count)
SELECT 102, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 4, 82.50, 0.7600, 3, 2
WHERE NOT EXISTS (
    SELECT 1 FROM course_statistics WHERE course_id = 102 AND stat_date = DATE_SUB(CURDATE(), INTERVAL 1 DAY)
);

INSERT INTO course_statistics (course_id, stat_date, student_count, avg_score, mastery_avg, ai_call_count, wrong_count)
SELECT 102, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 3, 80.00, 0.7400, 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM course_statistics WHERE course_id = 102 AND stat_date = DATE_SUB(CURDATE(), INTERVAL 2 DAY)
);

INSERT INTO knowledge_mastery (student_id, course_id, knowledge_point_id, mastery_score, sample_count, last_assessed_at)
SELECT 5, 102, 16, 0.6200, 2, NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_mastery WHERE student_id = 5 AND knowledge_point_id = 16);

INSERT INTO wrong_question_record (student_id, course_id, question_id, knowledge_point_id, error_types, diagnosis, wrong_count, create_time)
SELECT 3, 102, 1007, 16, 'CONCEPT', 'Gate H 错题样本', 2, DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE NOT EXISTS (SELECT 1 FROM wrong_question_record WHERE student_id = 3 AND question_id = 1007);

INSERT INTO learning_record (student_id, course_id, action_type, duration_minutes, create_time)
SELECT 3, 102, 'AI_CHAT', 15, DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE NOT EXISTS (
    SELECT 1 FROM learning_record WHERE student_id = 3 AND course_id = 102 AND action_type = 'AI_CHAT' LIMIT 1
);
