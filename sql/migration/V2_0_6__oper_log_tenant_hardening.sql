-- =============================================================================
-- V2.0.6 系统业务操作日志多租户加固与审计闭环 (Gate I11)
-- 智教云 · EduMind
-- =============================================================================

-- 1. 创建或校验操作日志表 sys_oper_log
CREATE TABLE IF NOT EXISTS sys_oper_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id      BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    title          VARCHAR(64)  DEFAULT NULL COMMENT '模块标题',
    business_type  INT          NOT NULL DEFAULT 0 COMMENT '业务类型(0其它 1新增 2修改 3删除 4查询 5导出 6导入 7授权/变更 8清空)',
    method         VARCHAR(255) DEFAULT NULL COMMENT 'Java方法名称',
    request_method VARCHAR(16)  DEFAULT NULL COMMENT '请求方式(GET/POST/PUT/DELETE)',
    oper_user_id   BIGINT       DEFAULT NULL COMMENT '操作人用户ID',
    oper_name      VARCHAR(64)  DEFAULT NULL COMMENT '操作人员账号/姓名',
    oper_url       VARCHAR(255) DEFAULT NULL COMMENT '请求URL',
    oper_ip        VARCHAR(128) DEFAULT NULL COMMENT '客户端主机IP地址',
    oper_param     TEXT         DEFAULT NULL COMMENT '请求参数(JSON，含action/diffItems/params，已脱敏)',
    json_result    TEXT         DEFAULT NULL COMMENT '返回参数(JSON，已截断)',
    status         INT          NOT NULL DEFAULT 0 COMMENT '操作状态(0正常 1异常)',
    error_msg      TEXT         DEFAULT NULL COMMENT '错误消息/异常摘要',
    cost_time      BIGINT       NOT NULL DEFAULT 0 COMMENT '消耗时间(ms)',
    oper_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_oper_tenant_time (tenant_id, oper_time),
    KEY idx_oper_name (oper_name),
    KEY idx_oper_title (title),
    KEY idx_oper_user_id (oper_user_id),
    KEY idx_oper_time_status (oper_time, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统业务操作日志表';

-- 2. 补全操作日志权限点
INSERT IGNORE INTO sys_permission (permission_code, permission_name, parent_id)
VALUES 
    ('system:operlog:query', '操作日志查询', 0),
    ('system:operlog:delete', '操作日志删除', 0),
    ('system:operlog:clear', '操作日志清空', 0),
    ('system:operlog:export', '操作日志导出', 0);

-- 3. 关联至管理员角色
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code IN ('ADMIN', 'TENANT_ADMIN')
  AND p.permission_code IN ('system:operlog:query', 'system:operlog:delete', 'system:operlog:clear', 'system:operlog:export');
