-- 补全菜单/侧栏引用但尚未入库的权限码，与前端 DEFAULT_SYSTEM_PERMISSIONS 对齐

INSERT IGNORE INTO sys_permission (id, permission_code, permission_name, parent_id) VALUES
(71, 'course:delete',               '课程删除', 0),
(72, 'course:ai:use',               '课程 AI 使用', 0),
(73, 'ai:lesson:generate',          'AI 教案生成', 0),
(74, 'ai:summary:view',             'AI 课堂总结查看', 0),
(75, 'ai:recommendation:view',      'AI 推荐查看', 0),
(76, 'knowledge:create',            '知识库创建', 0),
(77, 'knowledge:delete',            '知识库删除', 0),
(78, 'knowledge:chunk:view',        '切片管理查看', 0),
(79, 'knowledge:vector:view',       '向量状态查看', 0),
(80, 'knowledge:graph:view',        '知识图谱查看', 0),
(81, 'question:create',             '题目创建', 0),
(82, 'question:delete',             '题目删除', 0),
(83, 'question:bank:view',          '题库查看', 0),
(84, 'learning:view',               '学习总览查看', 0),
(85, 'learning:task:view',          '学习任务查看', 0),
(86, 'learning:practice',           'AI 练习使用', 0),
(87, 'learning:wrong:view',         '错题本查看', 0),
(88, 'learning:report:view',        '学习报告查看', 0),
(89, 'learning:path:view',          '学习路径查看', 0),
(90, 'analytics:learning',          '学情分析查看', 0),
(91, 'analytics:mastery',           '知识点掌握分析', 0),
(92, 'analytics:wrong',             '错题分析查看', 0),
(93, 'analytics:ai-usage',          'AI 使用分析查看', 0),
(94, 'analytics:report',            '教学报告查看', 0),
(95, 'system:user:add',             '用户档案新增', 0),
(96, 'system:user:delete',          '用户档案删除', 0),
(97, 'system:menu:add',             '菜单管理新增', 0),
(98, 'system:menu:delete',          '菜单管理删除', 0),
(99, 'profile:view',                '个人资料查看', 0),
(100, 'profile:security',            '账号安全设置', 0),
(101, 'profile:preferences',         '偏好设置管理', 0);

-- 管理员默认拥有新增权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE id BETWEEN 71 AND 101;

-- 教师角色：教学域新增权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission
WHERE permission_code IN (
    'course:delete', 'course:ai:use',
    'ai:lesson:generate', 'ai:summary:view', 'ai:recommendation:view',
    'knowledge:create', 'knowledge:delete', 'knowledge:chunk:view', 'knowledge:vector:view', 'knowledge:graph:view',
    'question:create', 'question:delete', 'question:bank:view',
    'analytics:learning', 'analytics:mastery', 'analytics:wrong', 'analytics:ai-usage', 'analytics:report',
    'profile:view', 'profile:security', 'profile:preferences'
);

-- 学生角色：学习中心 + 个人中心
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission
WHERE permission_code IN (
    'learning:view', 'learning:task:view', 'learning:practice', 'learning:wrong:view',
    'learning:report:view', 'learning:path:view',
    'profile:view', 'profile:security', 'profile:preferences'
);
