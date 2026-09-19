-- 个人错题本：最近一次作答、攻克状态

ALTER TABLE wrong_question_record
    ADD COLUMN last_student_answer VARCHAR(1024) NULL COMMENT '最近一次错误作答' AFTER wrong_count,
    ADD COLUMN status TINYINT NOT NULL DEFAULT 0 COMMENT '0=待攻坚 1=已攻克' AFTER last_student_answer,
    ADD COLUMN mastered_time DATETIME NULL COMMENT '标记已攻克时间' AFTER status;

ALTER TABLE wrong_question_record
    ADD KEY idx_student_course_status (student_id, course_id, status);
