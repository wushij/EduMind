-- 清理误入库的 AI 命题提示词 / Mock 占位题干（逻辑删除）
UPDATE edu_question
SET deleted = 1,
    update_time = NOW()
WHERE deleted = 0
  AND (
    stem LIKE 'Mock 题目%'
        OR stem LIKE '%你是一位专业的教学出题助手%'
        OR (stem LIKE '%JSON 格式%' AND stem LIKE '%questions%')
        OR (stem LIKE '%课程ID=%' AND stem LIKE '%知识点=%' AND stem LIKE '%题型=%')
  );
