-- 优化 LESSON_PREP_RAG_GENERAL：强化 sourceId 与时间分配规则（可重复执行）
USE edumind;

UPDATE prompt_template
SET system_prompt = REPLACE(
    system_prompt,
    '不得伪造不存在的 Source。',
    '不得伪造不存在的 Source。\n\nsourceIds 只能使用 {{retrieved_context}} 中实际出现的 source id。禁止引用未提供的编号。'
),
    update_time = NOW()
WHERE code = 'LESSON_PREP_RAG_GENERAL'
  AND system_prompt LIKE '%不得伪造不存在的 Source。%'
  AND system_prompt NOT LIKE '%sourceIds 只能使用 {{retrieved_context}}%';

UPDATE prompt_template_version ptv
INNER JOIN prompt_template pt ON pt.id = ptv.template_id
SET ptv.system_prompt = pt.system_prompt
WHERE pt.code = 'LESSON_PREP_RAG_GENERAL'
  AND ptv.version = pt.version;
