-- =============================================================================
-- V2.7.2: 上线「AI 智能备课」工具入口，点击进入「创建新课程空间」页
-- =============================================================================
-- 背景：V2.6.7 下线了独立「AI 教案生成」页，AI 备课统一收敛到课程内的
--       课节教案工作台（Lesson Studio）。但 AI 工具广场缺少面向教师的显式
--       备课入口；若直接跳已有课节工作台，会落到课程的任意首个课节，体验突兀。
-- 方案：新增工具卡片「AI 智能备课」，点击后跳转「创建课程」页（/course/create）：
--       教师先在该页完成课程空间初始化（大纲 / 知识库 / AI 助教），再进入备课。
-- 幂等：INSERT ... ON DUPLICATE KEY UPDATE，可重复执行；不重置 use_count。
-- =============================================================================

INSERT INTO ai_tool
  (id, name, description, detailed_intro, category, icon, model_id, route, execution_mode, tags, is_recommended, is_hot, use_count, status)
VALUES
  ('tool_lesson_prep', 'AI 智能备课',
   '结合课程大纲、课节目标与知识库资料，一键生成结构化课节教案',
   '从创建课程空间开始备课：完成课程初始化与教学大纲后，进入课节教案工作台，依据教学设计目标与 RAG 检索资料生成教学目标、重难点、师生活动与板书建议，正文确认后即可插入课节，落库留存并可继续编辑。',
   'TEACHER', 'Notebook', '', '/course/create', 'ROUTE', '备课,教师', 1, 0, 0, 1)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  description = VALUES(description),
  detailed_intro = VALUES(detailed_intro),
  category = VALUES(category),
  icon = VALUES(icon),
  model_id = VALUES(model_id),
  route = VALUES(route),
  execution_mode = VALUES(execution_mode),
  tags = VALUES(tags),
  is_recommended = VALUES(is_recommended),
  is_hot = VALUES(is_hot),
  status = VALUES(status);
