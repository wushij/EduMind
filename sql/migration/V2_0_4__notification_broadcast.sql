-- 消息通知：优先级 + 管理员广播任务表
ALTER TABLE sys_notification
    ADD COLUMN priority TINYINT NOT NULL DEFAULT 0 COMMENT '0普通 1强弹窗 2跑马灯' AFTER ref_id;

CREATE TABLE IF NOT EXISTS sys_notification_broadcast (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(128) NOT NULL,
    content         TEXT         NOT NULL,
    target_type     VARCHAR(16)  NOT NULL DEFAULT 'all' COMMENT 'all/role',
    target_payload  VARCHAR(128) DEFAULT NULL COMMENT '角色编码 ADMIN/TEACHER/STUDENT',
    notify_type     VARCHAR(32)  NOT NULL DEFAULT 'SYSTEM',
    priority        TINYINT      NOT NULL DEFAULT 0 COMMENT '0普通 1强弹窗 2跑马灯',
    sender_id       BIGINT       NOT NULL,
    sender_name     VARCHAR(64)  DEFAULT '',
    total_count     INT          NOT NULL DEFAULT 0,
    read_count      INT          NOT NULL DEFAULT 0,
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统消息广播任务表';
