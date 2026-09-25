-- =============================================================================
-- V2.7.0  修正高等数学（课程 103）题库的知识点归属
-- -----------------------------------------------------------------------------
-- 现象：教师端「教学质量评估 → 本学期核心考点掌握度热力排行榜」出现
--       「标题与 AI 归因诊断正文自相矛盾」的条目。例如榜单第 2 行标题为
--       「复合函数链式求导法则」，AI 诊断正文却在讲零点定理的存在性与唯一性。
--
-- 根因：V2.6.0 补种真实考题时，edu_question.knowledge_point_id 的标注与题目
--       实际考查内容不符：
--         · 1103（零点定理选择题）/ 1110（方程根存在唯一性）/ 1111（单调性极值与最值）
--           标注为 19「复合函数链式求导法则」；
--         · 1105（切线方程）/ 1106（隐函数求导）/ 1109（参数方程求导）
--           标注为 18「洛必达法则求未定式极限」；
--         · 1107（第二个重要极限）标注为 17「等价无穷小代换及其应用条件」；
--         · 1101（可去间断点）标注为 17（应为连续性/间断点分类）。
--       榜单按错题记录的考点分组、以考点标题作为行标题、以该题自身的 AI 诊断
--       作为正文，因此「挂错考点」必然表现为「标题与正文两张皮」，且据此生成的
--       AI 教学建议会指向并不存在的薄弱环节。
--
-- 本脚本：
--   1) 补齐缺失的章节与考点（连续性与零点定理、重要极限、导数几何意义、导数应用、
--      隐函数与参数方程求导）；
--   2) 按题目实际考点重挂 edu_question.knowledge_point_id；
--   3) 把历史 wrong_question_record 的 knowledge_point_id 同步到题目当前考点，
--      使既有榜单条目立即修正（新产生的记录本就以题目考点落库）。
--
-- 口径说明：knowledge_mastery 是按 (学生, 考点) 聚合的历史测评结果，无法由题目
--       标签反推重算，本脚本不做改写；下一次测评由 upsertMastery 写回新考点。
--
-- 幂等性：全部使用显式主键 INSERT IGNORE / 带存在性判断的写入与 UPDATE，
--       可重复执行；知识点 ID 20~24、章节 ID 109~110 为课程 103 专用空闲 ID。
-- =============================================================================

-- 1. 补齐章节：第一章新增「1.3 函数的连续性与零点定理」，第二章新增「2.2 导数的几何意义与函数性质研究」
INSERT IGNORE INTO course_chapter (id, tenant_id, course_id, parent_id, title, sort_order) VALUES
(109, 1, 103, 11, '1.3 函数的连续性与零点定理',        3),
(110, 1, 103, 13, '2.2 导数的几何意义与函数性质研究',  2);

-- 2. 补齐考点（importance 保持默认档位 3，避免影响掌握度推算的微调系数）
INSERT IGNORE INTO course_knowledge_point (id, tenant_id, course_id, chapter_id, title, sort_order) VALUES
(20, 1, 103, 109, '函数的连续性与零点定理',          1),
(21, 1, 103, 12,  '两个重要极限及其应用',            3),
(22, 1, 103, 110, '导数的几何意义与切线方程',        1),
(23, 1, 103, 110, '导数应用：单调性、极值与最值',    2),
(24, 1, 103, 14,  '隐函数与参数方程求导',            4);

-- 3. 课节-知识点关联（无唯一键，用 NOT EXISTS 保证幂等）
INSERT INTO course_chapter_knowledge_point (tenant_id, course_id, chapter_id, knowledge_point_id, sort_order)
SELECT 1, 103, t.chapter_id, t.kp_id, t.sort_order FROM (
    SELECT 109 AS chapter_id, 20 AS kp_id, 1 AS sort_order UNION ALL
    SELECT 12,  21, 3 UNION ALL
    SELECT 110, 22, 1 UNION ALL
    SELECT 110, 23, 2 UNION ALL
    SELECT 14,  24, 4
) t
WHERE NOT EXISTS (
    SELECT 1 FROM course_chapter_knowledge_point c
    WHERE c.course_id = 103 AND c.chapter_id = t.chapter_id AND c.knowledge_point_id = t.kp_id
);

-- 4. 按题目实际考查内容重挂考点（关联考点所属课程，避免 ID 被其它课程占用时误挂）
UPDATE edu_question q JOIN course_knowledge_point kp ON kp.id = 20 AND kp.course_id = 103 SET q.knowledge_point_id = 20 WHERE q.id IN (1101, 1103, 1110);
UPDATE edu_question q JOIN course_knowledge_point kp ON kp.id = 21 AND kp.course_id = 103 SET q.knowledge_point_id = 21 WHERE q.id = 1107;
UPDATE edu_question q JOIN course_knowledge_point kp ON kp.id = 22 AND kp.course_id = 103 SET q.knowledge_point_id = 22 WHERE q.id = 1105;
UPDATE edu_question q JOIN course_knowledge_point kp ON kp.id = 23 AND kp.course_id = 103 SET q.knowledge_point_id = 23 WHERE q.id = 1111;
UPDATE edu_question q JOIN course_knowledge_point kp ON kp.id = 24 AND kp.course_id = 103 SET q.knowledge_point_id = 24 WHERE q.id IN (1106, 1109);

-- 5. 历史错题记录：考点跟随题目当前标注（课程以题目所属课程为准，与 V2.6.1 口径一致）
UPDATE wrong_question_record w
JOIN edu_question q ON q.id = w.question_id
JOIN course_knowledge_point kp ON kp.id = q.knowledge_point_id
SET w.knowledge_point_id = q.knowledge_point_id
WHERE q.course_id = 103
  AND w.course_id = 103
  AND (w.knowledge_point_id IS NULL OR w.knowledge_point_id <> q.knowledge_point_id);
