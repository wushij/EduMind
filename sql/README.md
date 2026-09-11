# 智教云 · EduMind 数据库脚本中心

本项目所有数据库脚本统一维护在根目录 `sql/` 下。**后端启动不会自动改库**，需由你在 MySQL 中手动执行。

---

## 目录结构

```text
sql/
├── init.sql                                    # 全量单文件初始化（建库 + 30 张表 + 种子数据）
├── migration/                                  # 增量版本脚本（按版本号顺序手动执行）
│   ├── V0_1_0__init_core.sql
│   ├── V0_1_1__ai_question_exam.sql
│   ├── V0_2_0__question_bank.sql
│   ├── V0_2_1__assignment_grading.sql
│   ├── V0_2_2__knowledge_document.sql
│   ├── V0_2_3__rbac_system.sql
│   ├── V0_2_4__notification_statistics.sql
│   └── R__seed_data.sql                        # 种子数据（可重复执行，注意幂等）
└── README.md
```

---

## 使用方式

### 方式一：一键全量初始化（新建库推荐）

适用于本地/测试库从零搭建：

```bash
mysql -u root -p < sql/init.sql
```

或在 Navicat / DataGrip 中打开并执行 `sql/init.sql`。

### 方式二：按版本增量迁移（已有空库或需分步升级）

在已创建 `edumind` 数据库的前提下，**严格按文件名顺序**依次执行 `sql/migration/` 下脚本：

```text
1. V0_1_0__init_core.sql
2. V0_1_1__ai_question_exam.sql
3. V0_2_0__question_bank.sql
4. V0_2_1__assignment_grading.sql
5. V0_2_2__knowledge_document.sql
6. V0_2_3__rbac_system.sql
7. V0_2_4__notification_statistics.sql
8. R__seed_data.sql
```

示例（MySQL CLI，需先 `USE edumind;` 或脚本内含建库语句）：

```bash
mysql -u root -p edumind < sql/migration/V0_1_0__init_core.sql
mysql -u root -p edumind < sql/migration/V0_1_1__ai_question_exam.sql
# ... 依此类推
```

> **注意：** 若已执行过 `init.sql`，通常无需再跑 migration 脚本，避免重复建表。两种方式二选一即可。

---

## 与后端的关系

- 后端**不包含**任何数据库迁移框架或自动建表逻辑，启动时只连接已有库。
- 表结构变更只改 `sql/` 下脚本，由你手动执行后再启动后端。
- 脚本 `V0_x_x__` 前缀仅用于版本对照，与运行时无关。

## 本地依赖服务

后端 **prod 环境强制依赖 Redis**（Sa-Token 会话、验证码、防重放、限流、RBAC 缓存、AI 会话态）。

本地需自行启动：

1. **MySQL 8**：`localhost:3306`，库名 `edumind`，账号 `root` / `root`
2. **Redis**：`localhost:6379`，无密码
3. **MinIO**（可选，文件上传）：`localhost:9000`

初始化数据库：

```bash
mysql -u root -proot < sql/init.sql
```

---

## 预置系统默认账号

| 用户名 | 登录密码 | 角色 | 姓名 | 说明 |
|:---|:---|:---|:---|:---|
| **admin** | `admin123` | 系统管理员 | 系统管理员 | 具备系统设置、用户与角色管理等全量权限 |
| **teacher** | `admin123` | 教师 | 张老师 | 负责备课授课、出题组卷、发布作业与批改 |
| **student** | `admin123` | 学生 | 李同学 | 选课学习、在线作业提交、查看 AI 批改诊断 |
| **student2** | `admin123` | 学生 | 王同学 | 选课学生，具备选课与练习权限 |

---

## 30 张核心业务表全景清单

| 业务领域 | 数据表名 | Java Entity 实体映射 | 职责说明 |
|:---|:---|:---|:---|
| **系统管理** | `sys_user` | `UserEntity` | 用户基础信息（密码采用 BCrypt 加密） |
| | `sys_role` | `RoleEntity` | 角色表（ADMIN, TEACHER, STUDENT） |
| | `sys_user_role` | `UserRoleEntity` | 用户与角色多对多映射 |
| | `sys_permission` | `PermissionEntity` | 细粒度操作权限编码 |
| | `sys_role_permission`| `RolePermissionEntity` | 角色与权限映射 |
| | `sys_notification` | `NotificationEntity` | 站内消息与作业通知 |
| **课程教学** | `course` | `CourseEntity` | 课程主信息 |
| | `course_chapter` | `ChapterEntity` | 课程大纲层级章节树 |
| | `course_knowledge_point` | `KnowledgePointEntity` | 知识图谱核心知识点 |
| | `course_member` | `CourseMemberEntity` | 选课成员与教师绑定 |
| **题库试卷** | `question_bank` | `QuestionBankEntity` | 课程题库管理 |
| | `edu_question` | `QuestionEntity` | 试题（支持单选/多选/判断/填空/简答） |
| | `question_option` | - | 试题选项细项 |
| | `question_bank_item` | `QuestionBankItemEntity` | 题库与题目关联 |
| | `teaching_exam` | `ExamEntity` | 试卷测验信息 |
| | `exam_question` | `ExamQuestionEntity` | 试卷题目与分值排序 |
| **作业批改** | `assignment` | `AssignmentEntity` | 教学作业发布与截止控制 |
| | `assignment_submission` | `SubmissionEntity` | 学生作业提交记录 |
| | `submission_answer` | `SubmissionAnswerEntity` | 学生题目具体作答 |
| | `grading_result` | `GradingResultEntity` | AI 智能打分、评语与教师最终确认 |
| **知识管理** | `knowledge_base` | `KnowledgeBaseEntity` | 课程关联知识库 |
| | `knowledge_document` | `KnowledgeDocumentEntity` | 上传文档（PDF/Word/Markdown） |
| | `knowledge_document_text` | `KnowledgeDocumentTextEntity` | 向量化切片正文 |
| **教学资源** | `teaching_resource` | `ResourceEntity` | 课件、教案与媒体资源 |
| | `course_resource` | `CourseResourceEntity` | 课程与资源/文档关联 |
| **AI 智能赋能**| `ai_tool` | `AiToolEntity` | AI 工具广场元数据 |
| | `ai_conversation` | `ConversationEntity` | AI 多轮会话记录 |
| | `ai_message` | `MessageEntity` | 对话消息历史与 Token 统计 |
| | `ai_call_log` | `AiCallLogEntity` | 大模型真实调用耗时与用量审计 |
| **统计分析** | `statistics_daily_snapshot` | `StatisticsEntity` | 每日学情与 AI 消耗快照（供 ECharts 大屏） |
