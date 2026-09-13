-- 消息通知：增加关联业务 ID，支持点击跳转
ALTER TABLE sys_notification
    ADD COLUMN ref_id BIGINT DEFAULT NULL COMMENT '关联业务ID' AFTER type;

ALTER TABLE sys_notification
    ADD KEY idx_user_read (user_id, is_read);
