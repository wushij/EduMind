-- 修复种子账号密码哈希（旧哈希无法校验 admin123）
UPDATE sys_user
SET password = '$2b$10$DLVcEXn5RunOfqlY84u9S.nnXViwwLRheQgk0KIIpKr9y1im4mIOq'
WHERE username IN ('admin', 'teacher', 'student', 'student2');
