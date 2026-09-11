-- V0.2 补充：系统消息通知与每日学情统计快照
CREATE TABLE IF NOT EXISTS sys_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    content TEXT,
    type VARCHAR(32) DEFAULT 'SYSTEM',
    is_read TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS statistics_daily_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stat_date DATE NOT NULL,
    course_id BIGINT DEFAULT NULL,
    active_student_count INT DEFAULT 0,
    total_ai_conversations INT DEFAULT 0,
    total_tokens_consumed BIGINT DEFAULT 0,
    avg_score DOUBLE DEFAULT NULL,
    KEY idx_stat_date (stat_date),
    KEY idx_course_id (course_id)
);
