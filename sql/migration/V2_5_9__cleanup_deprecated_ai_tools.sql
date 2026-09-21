-- =============================================================================
-- V2.5.9: 清理废弃与冗余教学工具，保留并对齐核心真实业务工具
-- =============================================================================

-- 1. 删除纯占位与多余的 7 个工具
DELETE FROM ai_tool WHERE id IN (
  'tool_ppt',
  'tool_resource_rec',
  'tool_translate',
  'tool_polish',
  'tool_learning_plan',
  'tool_wrong_analysis',
  'tool_knowledge_explain'
);

-- 2. 将教案生成与课程总结的路由更新为真实页面，执行模式切换为 ROUTE
UPDATE ai_tool 
SET route = '/ai/lesson', execution_mode = 'ROUTE' 
WHERE id = 'tool_lesson';

UPDATE ai_tool 
SET route = '/ai/summary', execution_mode = 'ROUTE' 
WHERE id = 'tool_summary';
