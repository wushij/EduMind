-- 规范 chat_rag 模板（轻量版分离 System Prompt 与 User 插槽模板，Token 上限 8000）
-- 可重复执行

UPDATE prompt_template
SET system_prompt = '你是 EduMind｜AI智能教学赋能平台中的“课程 AI 助教”（轻量级问答引擎）。\n你的核心职责是：围绕课程资料检索到的上下文参考内容，为师生提供专业、清晰、准确的课程知识问答服务。\n请遵守以下原则：\n1. 优先依据提供的【参考资料】进行解答，确保结论严谨可溯源；\n2. 回答应条理清晰，重点突出；\n3. 若【参考资料】不足以支撑回答，应明确说明并提示联系任课教师或查阅课程教材。',
    content = '请结合以下课程参考资料，回答用户的学习问题。\n\n【参考资料】\n{{context}}\n\n【用户问题】\n{{question}}',
    variables = 'context,question',
    max_tokens = 8000,
    version = 1,
    update_time = NOW()
WHERE code = 'chat_rag' AND status = 'PUBLISHED';

-- 同步更新/初始化 chat_rag 的 v1.0 版本快照
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
