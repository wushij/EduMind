-- Gate H E2E 种子（幂等）— V1.1 智能中枢深化版验收
-- 请在目标库执行：mysql -u root -p <database> < sql/migration/R__gate_h_e2e_seed.sql

-- ai_call_log 课程维度样本（course 102）
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

-- 课程 102 日聚合样本（近 7 天）
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

-- 继承 Gate G 学情样本（若不存在则插入）
INSERT INTO knowledge_mastery (student_id, course_id, knowledge_point_id, mastery_score, sample_count, last_assessed_at)
SELECT 3, 102, 14, 0.8500, 5, NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_mastery WHERE student_id = 3 AND knowledge_point_id = 14);

INSERT INTO knowledge_mastery (student_id, course_id, knowledge_point_id, mastery_score, sample_count, last_assessed_at)
SELECT 4, 102, 15, 0.7000, 3, NOW()
WHERE NOT EXISTS (SELECT 1 FROM knowledge_mastery WHERE student_id = 4 AND knowledge_point_id = 15);

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
