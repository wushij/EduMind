-- V0.5.1 用户偏好设置
CREATE TABLE IF NOT EXISTS sys_user_preference (
    user_id            BIGINT       NOT NULL COMMENT '用户ID',
    theme              VARCHAR(16)  DEFAULT 'LIGHT' COMMENT '主题',
    language           VARCHAR(16)  DEFAULT 'zh-CN' COMMENT '语言',
    default_model      VARCHAR(64)  DEFAULT NULL COMMENT '默认模型',
    enable_rag         TINYINT(1)   DEFAULT 1 COMMENT '默认启用 RAG',
    enable_notification TINYINT(1)  DEFAULT 1 COMMENT '启用通知',
    preferences_json   JSON         DEFAULT NULL COMMENT '扩展偏好 JSON',
    updated_at         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好设置';
