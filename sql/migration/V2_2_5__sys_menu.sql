-- ==========================================================
-- V2.2.5: 系统菜单表（动态路由 / 菜单管理 CRUD）
-- ==========================================================

CREATE TABLE IF NOT EXISTS sys_menu (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    parent_id    BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单ID，0 为根',
    name         VARCHAR(100) NOT NULL COMMENT '菜单名称',
    type         TINYINT      NOT NULL COMMENT '类型：1目录 2菜单 3按钮',
    path         VARCHAR(255) DEFAULT NULL COMMENT '路由路径',
    component    VARCHAR(255) DEFAULT NULL COMMENT '前端组件路径',
    icon         VARCHAR(64)  DEFAULT NULL COMMENT '图标',
    permission   VARCHAR(128) DEFAULT NULL COMMENT '权限标识',
    sort         INT          NOT NULL DEFAULT 0 COMMENT '排序',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    visible      TINYINT      NOT NULL DEFAULT 1 COMMENT '是否在侧栏显示：0否 1是',
    keep_alive   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否缓存页面：0否 1是',
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_sys_menu_parent (parent_id),
    KEY idx_sys_menu_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统菜单表';

-- 作业删除权限（与前端 DEFAULT_SYSTEM_PERMISSIONS id=141 对齐）
INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, parent_id) VALUES
(141, 'assignment:delete', '作业删除', 0);

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE permission_code = 'assignment:delete';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE permission_code = 'assignment:delete';
