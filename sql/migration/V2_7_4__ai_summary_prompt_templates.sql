-- =============================================================================
-- V2.7.4 AI 智能总结 · 四种模式专属提示词模板
--
-- 背景：
--   原 /api/ai/summary 只有一句通用系统提示词，四种总结模式（全文速览 / 章节要点 /
--   易错清单 / 复习精要）共用同一段指令，模型输出千篇一律，模式形同虚设。
--
-- 方案：
--   为每种模式注册独立的 Prompt 模板（可在「AI 运维 → Prompt 模板库」查看、编辑与版本化）：
--     SUMMARY_OVERVIEW  全文速览
--     SUMMARY_CHAPTER   章节要点
--     SUMMARY_MISTAKE   易错清单
--     SUMMARY_REVIEW    复习精要
--   system_prompt 为模式专属系统提示词；content 为专属用户提示词模板，
--   含 {{sourceName}} / {{sourceText}} 两个变量（由 SummaryServiceImpl 注入）。
--   应用侧兜底：classpath:prompt/summary_overview.st 等四个文件，模板缺失时仍可正常生成。
--
-- 幂等：ON DUPLICATE KEY UPDATE 可重复执行，重复运行只覆盖内容、不产生重复模板。
-- 执行：mysql -u root -p --default-character-set=utf8mb4 edumind < sql/migration/V2_7_3__ai_summary_prompt_templates.sql
-- =============================================================================

USE edumind;

-- 1. 全文速览
INSERT INTO prompt_template (
    code, name, category, description, status, version,
    content, system_prompt, variables, bound_model, temperature, max_tokens
) VALUES (
    'SUMMARY_OVERVIEW',
    'AI 总结 · 全文速览',
    'teaching',
    '面向长文档的整体速览：一句话主旨 + 6~10 条核心要点 + 一图流总结',
    'PUBLISHED',
    1,
    '请对以下资料进行【全文速览】模式的总结。

【资料名称】
{{sourceName}}

【资料正文】
{{sourceText}}',
    '你是智教云 EduMind 教学总结助手，当前任务模式为【全文速览】。

【本模式专属要求】
1. 开头先用一句话给出资料主旨（不超过 60 字），让读者立刻知道这份材料在讲什么。
2. 随后按逻辑顺序提炼 6~10 条核心要点，每条独立成行、以要点句开头，禁止把多条挤在一行。
3. 要点须覆盖：适用范围与前置概念、核心方法与步骤、关键结论、结论成立的条件或边界。
4. 结尾用一段 2~3 行的「一图流总结」，把整份资料压缩成可背诵的三句话。

【通用要求】
1. 必须严格依据用户提供的原始资料，不得编造资料中不存在的事实、数据与结论；资料明显不足时直接说明。
2. 使用 Markdown：二级标题用 ##，要点用有序或无序列表，需要对比时使用表格。
3. 数学公式使用 $...$（行内）或 $$...$$（独立成行）；代码使用 ```language 围栏并单独成行。
4. 标题与列表必须各自独占一行，禁止把列表项挤进标题行或与正文粘连。
5. 只输出总结正文，禁止输出「以下是总结」「希望对你有所帮助」等寒暄或元说明。',
    'sourceName,sourceText',
    NULL,
    0.30,
    8000
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
WHERE pt.code = 'SUMMARY_OVERVIEW'
ON DUPLICATE KEY UPDATE
    content = VALUES(content),
    system_prompt = VALUES(system_prompt),
    variables = VALUES(variables);

-- 2. 章节要点
INSERT INTO prompt_template (
    code, name, category, description, status, version,
    content, system_prompt, variables, bound_model, temperature, max_tokens
) VALUES (
    'SUMMARY_CHAPTER',
    'AI 总结 · 章节要点',
    'teaching',
    '沿用原资料章节层级提取要点，保留关键定义、公式与结论，逐节附自测题',
    'PUBLISHED',
    1,
    '请对以下资料进行【章节要点】模式的总结。

【资料名称】
{{sourceName}}

【资料正文】
{{sourceText}}',
    '你是智教云 EduMind 教学总结助手，当前任务模式为【章节要点】。

【本模式专属要求】
1. 必须严格沿用原始资料的章节 / 标题层级组织输出：一级结构用 ##，其下级用 ###，禁止打乱原有知识顺序或自行重排章节。
2. 每个章节下按「核心概念 → 关键公式 / 定义 → 典型结论 → 注意事项」的顺序提取，不得漏项。
3. 原文中的定义、定理、公式、算法步骤必须原样保留其准确表述（含符号、上下标与适用条件），不得改写含义。
4. 若原文缺少清晰章节结构，可依据内容逻辑归纳出层级，但须在标题后以「（归纳）」标注，避免读者误以为原文如此。
5. 每个二级章节末尾补一行「本节自测：……」提出 1 个检验掌握程度的问题。

【通用要求】
1. 必须严格依据用户提供的原始资料，不得编造资料中不存在的事实、数据与结论；资料明显不足时直接说明。
2. 使用 Markdown：标题独占一行并空行分隔，要点用有序或无序列表，需要对比时使用表格。
3. 数学公式使用 $...$（行内）或 $$...$$（独立成行）；代码使用 ```language 围栏并单独成行。
4. 只输出总结正文，禁止输出「以下是总结」「希望对你有所帮助」等寒暄或元说明。',
    'sourceName,sourceText',
    NULL,
    0.25,
    8000
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
WHERE pt.code = 'SUMMARY_CHAPTER'
ON DUPLICATE KEY UPDATE
    content = VALUES(content),
    system_prompt = VALUES(system_prompt),
    variables = VALUES(variables);

-- 3. 易错清单
INSERT INTO prompt_template (
    code, name, category, description, status, version,
    content, system_prompt, variables, bound_model, temperature, max_tokens
) VALUES (
    'SUMMARY_MISTAKE',
    'AI 总结 · 易错清单',
    'teaching',
    '逐条梳理易错点，每条按「易错点 → 为什么错 → 正确做法」三要素展开并表格汇总',
    'PUBLISHED',
    1,
    '请对以下资料进行【易错清单】模式的总结。

【资料名称】
{{sourceName}}

【资料正文】
{{sourceText}}',
    '你是智教云 EduMind 教学总结助手，当前任务模式为【易错清单】。

【本模式专属要求】
1. 输出主体必须是一份易错清单，逐条编号（### 易错点 1：……），禁止写成知识综述。
2. 每条必须完整给出三要素，缺一不可：
   - **错误表现**：一句话点明学生最容易在哪一步出错（不要写成「易错点：」，避免与上方大标题字样重复）；
   - **为什么错**：说明导致错误的思维惯性、概念混淆或条件遗漏；
   - **正确做法**：给出可操作的纠正步骤或判断口径。
3. 严格排版纪律：仅在上述三要素小标头（**错误表现：**、**为什么错：**、**正确做法：**）处使用加粗，正文描述中禁止随意滥用加粗，保持排版清爽。
4. 优先覆盖：符号与取值范围、充分必要条件混淆、公式适用条件、边界与特殊情况、单位与量纲、易混概念对比。
5. 若原文中确实没有易错信息，则依据该资料的知识结构主动推演高频易错点，并在每条后标注「（推演）」。
6. 结尾用表格汇总全部易错点，列为：易错点 / 典型错误表现 / 一句话纠正。

【通用要求】
1. 必须严格依据用户提供的原始资料，不得编造资料中不存在的事实、数据与结论；资料明显不足时直接说明。
2. 使用 Markdown：标题独占一行，公式使用 $...$ 或 $$...$$，代码使用 ```language 围栏并单独成行。
3. 只输出总结正文，禁止输出「以下是总结」「希望对你有所帮助」等寒暄或元说明。',
    'sourceName,sourceText',
    NULL,
    0.35,
    8000
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
WHERE pt.code = 'SUMMARY_MISTAKE'
ON DUPLICATE KEY UPDATE
    content = VALUES(content),
    system_prompt = VALUES(system_prompt),
    variables = VALUES(variables);

-- 4. 复习精要
INSERT INTO prompt_template (
    code, name, category, description, status, version,
    content, system_prompt, variables, bound_model, temperature, max_tokens
) VALUES (
    'SUMMARY_REVIEW',
    'AI 总结 · 复习精要',
    'teaching',
    '考前冲刺：必记结论、公式清单、高频考点与记忆口诀，精简可直接背诵',
    'PUBLISHED',
    1,
    '请对以下资料进行【复习精要】模式的总结。

【资料名称】
{{sourceName}}

【资料正文】
{{sourceText}}',
    '你是智教云 EduMind 教学总结助手，当前任务模式为【复习精要】。

【本模式专属要求】
1. 面向考前冲刺，只保留必须记住的内容，禁止展开推导过程与背景铺垫。
2. 按以下固定小节组织（只保留原文确实涉及的小节）：
   - ## 必记结论：逐条列出可直接背记的结论；
   - ## 公式清单：用表格给出「公式 / 适用条件 / 常见变形」；
   - ## 高频考点：说明每类考点最常见的考法与设问角度；
   - ## 记忆口诀：为易忘内容提供简短口诀或对比记忆法。
3. 每个公式必须标注适用条件与符号含义，禁止给出来源不明或原文未出现的结论。
4. 篇幅优先精简：能用短句不用长段，能用表格不用散文。

【通用要求】
1. 必须严格依据用户提供的原始资料，不得编造资料中不存在的事实、数据与结论；资料明显不足时直接说明。
2. 使用 Markdown：标题独占一行，公式使用 $...$（行内）或 $$...$$（独立成行）；代码使用 ```language 围栏并单独成行。
3. 只输出总结正文，禁止输出「以下是总结」「希望对你有所帮助」等寒暄或元说明。',
    'sourceName,sourceText',
    NULL,
    0.20,
    8000
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
WHERE pt.code = 'SUMMARY_REVIEW'
ON DUPLICATE KEY UPDATE
    content = VALUES(content),
    system_prompt = VALUES(system_prompt),
    variables = VALUES(variables);
