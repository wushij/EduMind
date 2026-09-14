-- -----------------------------------------------------------------------------
-- V2.0.13: 课程 AI-RAG 轻量版提示词规范与 Token 上限提升
-- 1. 补齐 chat_rag 模板独立的 System Prompt 角色人设约束与纯净 User 插槽
-- 2. 将 chat_rag 的 max_tokens 上限提升至 8000（与大模型长文输出对齐）
-- 3. 同步校准 prompt_template_version 历史版本快照，清理重复版本冗余
-- -----------------------------------------------------------------------------

-- 1. 更新 chat_rag 模板定义
UPDATE prompt_template SET
    name = '课程 AI-RAG 对话 (轻量版)',
    description = '课程知识库 RAG 对话轻量级模板，快速注入资料上下文并返回解析',
    system_prompt = '你是 EduMind｜AI智能教学赋能平台中的“课程 AI 助教”（轻量级问答引擎）。\n你的核心职责是：围绕课程资料检索到的上下文参考内容，为师生提供专业、清晰、准确的课程知识问答服务。\n请遵守以下原则：\n1. 优先依据提供的【参考资料】进行解答，确保结论严谨可溯源；\n2. 回答应条理清晰，重点突出；\n3. 若【参考资料】不足以支撑回答，应明确说明并提示联系任课教师或查阅课程教材。',
    content = '请结合以下课程参考资料，回答用户的学习问题。\n\n【参考资料】\n{{context}}\n\n【用户问题】\n{{question}}',
    variables = 'context,question',
    bound_model = NULL,
    temperature = 0.30,
    max_tokens = 8000,
    version = 1,
    status = 'PUBLISHED',
    update_time = NOW()
WHERE code = 'chat_rag';

-- 2. 清理历史测试产生的重复版本快照，仅保留基线版本
DELETE FROM prompt_template_version
WHERE template_id = (SELECT id FROM prompt_template WHERE code = 'chat_rag')
  AND version > 1;

-- 3. 同步更新/初始化 chat_rag 的 v1.0 版本快照
INSERT INTO prompt_template_version (template_id, version, content, system_prompt, variables, published_by, create_time)
SELECT
    pt.id,
    1,
    pt.content,
    pt.system_prompt,
    pt.variables,
    1,
    NOW()
FROM prompt_template pt
WHERE pt.code = 'chat_rag'
ON DUPLICATE KEY UPDATE
    content = VALUES(content),
    system_prompt = VALUES(system_prompt),
    variables = VALUES(variables);
