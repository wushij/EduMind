-- =============================================================================
-- V2.6.7: 下线独立「AI 教案生成」页，AI 备课统一收敛到课程内课节教案工作台
-- =============================================================================
-- 背景：独立页面 /ai/lesson 的产出为纯 Markdown 草案，既不落库也不绑定课节，
--       能力已被课程内课节教案工作台（Lesson Studio + 课程 AI 助教）全面覆盖。
--       前端已删除该路由（/ai/lesson、/ai/lesson-plan 统一重定向到 /course/ai），
--       后端已删除 LessonPlanController / LessonPlanService / LessonPlanDTO。
-- 影响：清理 AI 工具广场入口、ai:lesson:generate 权限点及其角色授权、残留菜单。
-- 幂等：全部为 DELETE，可重复执行。
-- =============================================================================

-- 1. 移除 AI 工具广场「AI 教案生成」工具卡片
DELETE FROM ai_tool WHERE id = 'tool_lesson';

-- 2. 移除 ai:lesson:generate 权限点的角色授权与权限定义
DELETE rp FROM sys_role_permission rp
JOIN sys_permission p ON p.id = rp.permission_id
WHERE p.permission_code = 'ai:lesson:generate';

DELETE FROM sys_permission WHERE permission_code = 'ai:lesson:generate';

-- 3. 清理残留菜单项（路由已下线，避免侧栏点击 404）
DELETE FROM sys_menu WHERE path IN ('/ai/lesson', '/ai/lesson-plan');
