-- 回退 chat_rag 模板至原始简洁版本（移除误加的「3～5 条简要思考」结构）
-- 可重复执行

UPDATE prompt_template
SET content = '你是课程 AI 助教。请基于以下资料回答用户问题。若资料不足，请明确说明。

【参考资料】
{{context}}

【用户问题】
{{question}}',
    version = 1,
    update_time = NOW()
WHERE code = 'chat_rag' AND status = 'PUBLISHED';
