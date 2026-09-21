-- ============================================================
-- 修复跨课程误归类的历史数据
--
-- 背景：练习会话的课程来自前端课程选择器（默认第一个课程），
--      当学生从错题本「立即自测」进入、而选择器课程与题目实际课程不一致时，
--      提交练习会把错题记录与掌握度写到会话课程下，
--      表现为「高等数学的题出现在 java 课程的错题本 / 学情里」。
--
-- 现已改为以【题目自身所属课程】落库（AIPracticeServiceImpl），
-- 本脚本负责把已经错位的数据纠回。
-- ============================================================

-- 1) 错题记录：课程以题目实际所属课程为准
UPDATE wrong_question_record w
JOIN edu_question q ON q.id = w.question_id
SET w.course_id = q.course_id
WHERE q.course_id IS NOT NULL
  AND w.course_id <> q.course_id;

-- 2) 知识点掌握度：课程以知识点实际所属课程为准
UPDATE knowledge_mastery m
JOIN course_knowledge_point kp ON kp.id = m.knowledge_point_id
SET m.course_id = kp.course_id
WHERE kp.course_id IS NOT NULL
  AND m.course_id <> kp.course_id;
