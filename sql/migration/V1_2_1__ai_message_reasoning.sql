-- 消息表增加思考链字段（对齐 goblog reasoning_content）
ALTER TABLE ai_message
    ADD COLUMN reasoning_content TEXT COMMENT 'DeepSeek 思考链原文' AFTER content;
