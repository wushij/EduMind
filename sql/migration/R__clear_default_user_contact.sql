-- =============================================================================
-- 清除默认演示账号的种子邮箱/手机号（幂等，可重复执行）
-- 适用：admin / teacher / student / student2 仍带有 init 旧种子联系方式
-- 执行：mysql -u root -p edumind < sql/migration/R__clear_default_user_contact.sql
-- =============================================================================

USE edumind;

UPDATE sys_user
SET email = NULL,
    phone = NULL
WHERE username IN ('admin', 'teacher', 'student', 'student2');

SELECT username, email, phone
FROM sys_user
WHERE username IN ('admin', 'teacher', 'student', 'student2')
ORDER BY id;
