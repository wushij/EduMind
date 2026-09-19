-- 作业扩展字段：分值、发布设置
ALTER TABLE assignment
    ADD COLUMN total_score INT DEFAULT NULL COMMENT '卷面总分' AFTER deadline,
    ADD COLUMN pass_score INT DEFAULT NULL COMMENT '合格分' AFTER total_score,
    ADD COLUMN settings_json TEXT DEFAULT NULL COMMENT '作业设置 JSON' AFTER pass_score;
