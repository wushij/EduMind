-- 优化 EXAM_RAG_GENERAL：强化知识点 ID 映射与 JSON 输出规范（可重复执行）
USE edumind;

UPDATE prompt_template
SET system_prompt = REPLACE(
    system_prompt,
    '==================================================\n六、知识点约束\n==================================================\n\n每一道题必须绑定至少一个：\n\nknowledgePointId\nknowledgePointName',
    '==================================================\n六、知识点约束与 ID 映射\n==================================================\n\n系统已提供知识点 ID 与名称对照（按顺序一一对应）：\n\n知识点 ID 列表：\n{{knowledge_point_ids}}\n\n知识点名称列表：\n{{knowledge_point_names}}\n\n每一道题的 knowledgePoints 中：\nknowledgePointId 只能使用 ID 列表中的值（如 KP003），禁止写中文名称；\nknowledgePointName 填写对应中文名称。\n\n错误：\"knowledgePointId\": \"多态与动态分派\"\n正确：\"knowledgePointId\": \"KP004\", \"knowledgePointName\": \"多态与动态分派\"\n\n每一道题必须绑定至少一个 knowledgePointId + knowledgePointName'
),
    update_time = NOW()
WHERE code = 'EXAM_RAG_GENERAL'
  AND system_prompt LIKE '%六、知识点约束%'
  AND system_prompt NOT LIKE '%知识点 ID 列表：%';

UPDATE prompt_template
SET system_prompt = REPLACE(
    system_prompt,
    '  \"chapterName\": \"{{chapter_name}}\",\n  \"questions\": []',
    '  \"chapterId\": \"{{chapter_id}}\",\n  \"chapterName\": \"{{chapter_name}}\",\n  \"questions\": []'
),
    update_time = NOW()
WHERE code = 'EXAM_RAG_GENERAL'
  AND system_prompt LIKE '%\"chapterName\": \"{{chapter_name}}\"%'
  AND system_prompt NOT LIKE '%\"chapterId\": \"{{chapter_id}}\"%';

UPDATE prompt_template_version ptv
INNER JOIN prompt_template pt ON pt.id = ptv.template_id
SET ptv.system_prompt = pt.system_prompt
WHERE pt.code = 'EXAM_RAG_GENERAL'
  AND ptv.version = pt.version;
