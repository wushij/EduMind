-- =============================================================================
-- V2.6.8 加宽错题 AI 归因结论字段（VARCHAR(512) → TEXT）
--
-- 背景（真实故障）：
--   wrong_question_record.diagnosis 原为 VARCHAR(512)，而 AI 归因诊断正文
--   （偏差本质剖析 + 教学补救对策 + 含 LaTeX 公式的分点推导）普遍超过 512 字符。
--   MySQL 严格模式（STRICT_TRANS_TABLES，5.7+/8.0 默认开启）下不会静默截断，
--   而是直接抛 "Data too long for column 'diagnosis' at row 1"；
--   Spring 将其翻译为 DataIntegrityViolationException，被 GlobalExceptionHandler
--   映射为 HTTP 400「数据操作冲突，请检查后重试」。
--   现象：点击「AI 诊断」→ 前端弹「数据操作冲突 + Request failed with status code 400」，
--         大模型其实调用成功（额度已消耗），但结论一律无法落库，表格内容不更新。
--
-- 方案：
--   1) 列宽提升为 TEXT（最长 65535 字节，utf8mb4 下可容纳约 1.6 万汉字），
--      足以承载带公式的详实归因结论；
--   2) 应用侧同时增加入库长度上限与「按句边界安全截断」
--      （见 WrongQuestionDiagnosisService#fitDiagnosis），避免任何列宽问题再次变成 400。
--
-- 幂等：MODIFY COLUMN 可重复执行，重复运行不报错、不丢数据。
-- 执行：mysql -u root -p --default-character-set=utf8mb4 edumind < sql/migration/V2_6_8__widen_wrong_question_diagnosis.sql
-- =============================================================================

USE edumind;

ALTER TABLE wrong_question_record
    MODIFY COLUMN diagnosis TEXT NULL COMMENT '错因诊断（AI 归因结论，长文本）';
