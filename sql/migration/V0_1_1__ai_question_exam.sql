-- V0.1 AI、题目、试卷、会话
CREATE TABLE IF NOT EXISTS ai_tool (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    category VARCHAR(32),
    icon VARCHAR(64),
    route VARCHAR(256),
    tags VARCHAR(256),
    is_recommended TINYINT DEFAULT 0,
    use_count INT DEFAULT 0,
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS edu_question (
    id BIGINT PRIMARY KEY,
    bank_id BIGINT,
    course_id BIGINT,
    knowledge_point_id BIGINT,
    stem TEXT NOT NULL,
    type VARCHAR(32) NOT NULL,
    options TEXT,
    answer TEXT,
    analysis TEXT,
    difficulty INT DEFAULT 3,
    score INT DEFAULT 5,
    status INT DEFAULT 1,
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS question_option (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    option_key VARCHAR(8),
    content TEXT,
    is_correct TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS teaching_exam (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    total_score INT DEFAULT 100,
    pass_score INT DEFAULT 60,
    duration_minutes INT DEFAULT 90,
    start_time DATETIME,
    end_time DATETIME,
    status INT DEFAULT 1,
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS exam_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    score INT DEFAULT 5,
    sort_order INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ai_conversation (
    id VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    course_id BIGINT,
    title VARCHAR(128),
    message_count INT DEFAULT 0,
    total_tokens INT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_message (
    id VARCHAR(64) PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT,
    token_count INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    course_id BIGINT,
    description TEXT,
    document_count INT DEFAULT 0,
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS teaching_resource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT,
    chapter_id BIGINT,
    title VARCHAR(128) NOT NULL,
    resource_type VARCHAR(32),
    file_url VARCHAR(512),
    description TEXT,
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
