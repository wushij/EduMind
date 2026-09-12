-- Gate G E2E 种子（幂等）
USE edumind;

-- 课程 102 Java：掌握度样本（student id=3）
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

-- 图谱关系：多态(15) prerequisite 继承相关 — 用 14 作为前置（Java基础）
INSERT INTO knowledge_point_relation (source_knowledge_point_id, target_knowledge_point_id, relation_type)
SELECT 15, 14, 'prerequisite'
WHERE NOT EXISTS (
    SELECT 1 FROM knowledge_point_relation
    WHERE source_knowledge_point_id = 15 AND target_knowledge_point_id = 14 AND relation_type = 'prerequisite'
);

-- 错题记录
INSERT INTO wrong_question_record (student_id, course_id, question_id, knowledge_point_id, error_types, diagnosis, wrong_count)
SELECT 3, 102, 1007, 16, 'CONCEPT,LOGIC', '混淆编译期与运行期绑定', 3
WHERE NOT EXISTS (SELECT 1 FROM wrong_question_record WHERE student_id = 3 AND question_id = 1007);

-- 学习记录
INSERT INTO learning_record (student_id, course_id, action_type, duration_minutes)
SELECT 3, 102, 'STUDY', 45
WHERE NOT EXISTS (SELECT 1 FROM learning_record WHERE student_id = 3 AND course_id = 102 AND action_type = 'STUDY' LIMIT 1);

INSERT INTO learning_record (student_id, course_id, action_type, duration_minutes)
SELECT 4, 102, 'STUDY', 60
WHERE NOT EXISTS (SELECT 1 FROM learning_record WHERE student_id = 4 AND course_id = 102 AND action_type = 'STUDY' LIMIT 1);
