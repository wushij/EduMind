-- =============================================================================
-- V2.7.6 AI 智能总结 · AI 自动命名（标题）提示词模板
--
-- 背景：
--   总结标题原先由「来源名·模式」规则拼接（如「第3章-查找.pptx·章节要点」），
--   文件名往往无意义，标题无法体现资料主题。现改为生成完成后由大模型命名。
--
-- 方案：
--   注册 SUMMARY_TITLE 模板（可在「AI 运维 → Prompt 模板库」编辑与版本化）：
--     system_prompt 约束输出格式（只出标题、无引号书名号、8~20 字、无冗余词）；
--     content 为专属用户提示词模板，含 {{sourceName}} / {{modeLabel}} / {{excerpt}} 三个变量。
--   应用侧兜底：classpath:prompt/summary_title.st；模板缺失时仍可按规则命名，不影响主流程。
--   标题长度在服务端硬截断为 30 字（sanitizeAiTitle），手填标题上限为 60 字。
--
-- 幂等：ON DUPLICATE KEY UPDATE 可重复执行。
-- 执行：mysql -u root -p --default-character-set=utf8mb4 edumind < sql/migration/V2_7_4__ai_summary_ai_title_prompt.sql
-- =============================================================================

USE edumind;

INSERT INTO prompt_template (
    code, name, category, description, status, version,
    content, system_prompt, variables, bound_model, temperature, max_tokens
) VALUES (
    'SUMMARY_TITLE',
    'AI 总结 · 自动命名',
    'teaching',
    '为教学总结拟定简短标题：只输出标题本身，8~20 字，体现资料主题与总结模式',
    'PUBLISHED',
    1,
    '【资料名称】
{{sourceName}}

【总结模式】
{{modeLabel}}

【总结正文节选】
{{excerpt}}',
    '你是智教云 EduMind 教学资料的标题拟定助手。请为下面这份教学总结起一个标题。

【要求】
1. 只输出标题本身：不要引号、书名号、Markdown 标记、序号与结尾标点，不要任何解释、前缀或后缀。
2. 标题需同时体现「资料主题」与「本次总结模式」，长度 8~20 个汉字，最长不超过 30 字。
3. 不要出现「总结」「AI」「资料」等冗余词，直接给信息点。
4. 示例：查找算法核心要点·易错清单；线性代数矩阵运算·复习精要；第三次课件·章节要点。',
    'sourceName,modeLabel,excerpt',
    NULL,
    0.20,
    64
) ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    category = VALUES(category),
    description = VALUES(description),
    status = 'PUBLISHED',
    content = VALUES(content),
    system_prompt = VALUES(system_prompt),
    variables = VALUES(variables),
    temperature = VALUES(temperature),
    max_tokens = VALUES(max_tokens);

INSERT INTO prompt_template_version (template_id, version, content, system_prompt, variables, published_by, create_time)
SELECT pt.id, 1, pt.content, pt.system_prompt, pt.variables, 1, NOW()
FROM prompt_template pt
WHERE pt.code = 'SUMMARY_TITLE'
ON DUPLICATE KEY UPDATE
    content = VALUES(content),
    system_prompt = VALUES(system_prompt),
    variables = VALUES(variables);
