-- V1.1.1 课程学情日聚合表 (PRD §34 / 报表高性能查询支撑)
CREATE TABLE IF NOT EXISTS course_statistics (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  course_id BIGINT NOT NULL COMMENT '课程ID',
  stat_date DATE NOT NULL COMMENT '统计日期 (YYYY-MM-DD)',
  student_count INT NOT NULL DEFAULT 0 COMMENT '当日活跃学生数',
  avg_score DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '班级均分',
  mastery_avg DECIMAL(5,4) NOT NULL DEFAULT 0.0000 COMMENT '班级知识点平均掌握度(0.0000~1.0000)',
  ai_call_count INT NOT NULL DEFAULT 0 COMMENT '当日AI助教调用总量',
  wrong_count INT NOT NULL DEFAULT 0 COMMENT '当日新增错题记录数',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_course_date (course_id, stat_date),
  INDEX idx_stat_date (stat_date),
  INDEX idx_course_stat (course_id, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程学情日聚合统计表';
