-- =============================================================================
-- V2.6.5  补回高等数学（课程 103）丢失的知识点元数据
-- -----------------------------------------------------------------------------
-- 现象：教师端「学员画像 → 自适应推荐学习与提分周计划」中，第 1 周与第 2 周的
--       任务文案完全相同（均为「学习课程章节内容」占位文案），且推的是同一道练习题。
--
-- 根因：edu_question / wrong_question_record / knowledge_mastery 中大量数据仍引用
--       knowledge_point_id = 17 / 18 / 19，但 course_knowledge_point 中这三行在部分
--       环境（较早期 init.sql 建库）缺失，导致：
--         1) KnowledgeMasteryService.getMastery 因课程无知识点直接返回空 VO；
--         2) AdaptivePathOrchestrator 无法按薄弱考点排期，只能退化为章节维度计划，
--            且题库标签里的知识点 ID 无法反查标题 → 文案退化为通用占位文案；
--         3) 同一份计划内多周取题命中同一条缓存分支 → 每周推同一道题。
--
-- 说明：纯数据补种，幂等可重复执行；不改结构，无需 rollback 脚本。
--       如目标库 id=17/18/19 已被其它课程占用，INSERT IGNORE 会跳过对应行，
--       此时应改为按 title 人工核对后补种，避免覆盖既有数据。
-- =============================================================================

INSERT IGNORE INTO course_knowledge_point (id, tenant_id, course_id, chapter_id, title, sort_order) VALUES
(17, 1, 103, 12, '等价无穷小代换及其应用条件', 1),
(18, 1, 103, 12, '洛必达法则求未定式极限',     2),
(19, 1, 103, 14, '复合函数链式求导法则',       3);

-- 课节-知识点关联：保证章节详情页与课节学习页也能看到上述考点
INSERT IGNORE INTO course_chapter_knowledge_point (tenant_id, course_id, chapter_id, knowledge_point_id, sort_order) VALUES
(1, 103, 12, 17, 1),
(1, 103, 12, 18, 2),
(1, 103, 14, 19, 3);
