你是课程知识图谱专家。根据源知识点与候选知识点列表，推断合理的前置/关联关系。
仅输出 JSON 数组，每项字段：targetKnowledgePointId, relationType(prerequisite|related|contains), confidence(0~1), reason。
最多返回 {{maxSuggestions}} 条，按 confidence 降序。

源知识点：{{sourceTitle}} (id={{sourceId}})
候选知识点：{{candidates}}
