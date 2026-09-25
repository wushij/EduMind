<div align="center">

# 🎓 智教云 · EduMind

### AI 智能教学赋能平台 · 全栈工程

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.3-6DB33F?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![JDK](https://img.shields.io/badge/JDK-17-ED8B00?style=flat-square&logo=openjdk)](https://openjdk.org)
[![Vue 3](https://img.shields.io/badge/Vue-3.4-4FC08D?style=flat-square&logo=vuedotjs)](https://vuejs.org)
[![Vite](https://img.shields.io/badge/Vite-5.4-646CFF?style=flat-square&logo=vite)](https://vitejs.dev)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql)](https://www.mysql.com)
[![Redis](https://img.shields.io/badge/Redis-7.x-DC382D?style=flat-square&logo=redis)](https://redis.io)
[![Milvus](https://img.shields.io/badge/Milvus-2.4-00A1EA?style=flat-square)](https://milvus.io)

<p align="center">
  <a href="#-快速开始"><b>🚀 快速开始</b></a> &nbsp;•&nbsp;
  <a href="#️-系统架构"><b>🏗️ 系统架构</b></a> &nbsp;•&nbsp;
  <a href="#-目录结构"><b>📁 目录结构</b></a> &nbsp;•&nbsp;
  <a href="#-开发指南"><b>💡 开发指南</b></a> &nbsp;•&nbsp;
  <a href="#-常见问题"><b>❓ 常见问题</b></a>
</p>

</div>

---

**智教云 · EduMind** 是一套面向高校教学场景的 **AI 智能教学赋能平台**：后端 **Spring Boot 3.3 + JDK 17 + Maven 多模块 + MyBatis-Plus + Sa-Token + Redis**，前端 **Vue 3 + TypeScript + Vite + Pinia + Element Plus + ECharts**。整体架构定位为 **模块化单体（Modular Monolith）+ 领域模块化 + 分层架构**，通过 `*-api` 接口子模块严格隔离跨模块调用。

平台覆盖从 **课程建设 → 知识库治理 → RAG 检索增强 → AI 备课/出题/组卷/批改/伴学 → 作业考试 → 学情分析与教学干预** 的完整教学闭环；内置 **多租户与院系组织体系、RBAC 权限、国密 SM2/SM3/SM4 加解密与 KMS 密钥版本、请求签名防重放、算力配额**，并以 **ArchUnit 架构守卫 + 分层审计脚本** 长期锁死工程规范。

统一响应信封 `ApiResult { code, message, data, timestamp }`，成功码为 `code === 200`。

---

## 目录

- [🌟 功能特性](#-功能特性)
  - [⚡ 架构亮点](#-架构亮点)
  - [🤖 AI 智能引擎与 RAG 知识库](#-ai-智能引擎与-rag-知识库)
  - [🕸️ 智能 Agent 与工具广场](#️-智能-agent-与工具广场)
  - [📚 课程建设与教学运营](#-课程建设与教学运营)
  - [🎯 题库、作业与考试](#-题库作业与考试)
  - [📊 学情分析与教学干预](#-学情分析与教学干预)
  - [🏢 多租户、组织与配额](#-多租户组织与配额)
  - [🛡️ 安全体系](#️-安全体系)
  - [⚙️ 系统治理与可观测性](#️-系统治理与可观测性)
- [🛠️ 技术栈](#️-技术栈)
- [🏗️ 系统架构](#️-系统架构)
- [🧩 工程与模块清单](#-工程与模块清单)
- [🚀 快速开始](#-快速开始)
- [📁 目录结构](#-目录结构)
- [⚙️ 配置说明](#️-配置说明)
- [💡 开发指南](#-开发指南)
- [📦 构建与部署](#-构建与部署)
- [📜 许可证](#-许可证)

---

## 🌟 功能特性

### ⚡ 架构亮点

- **🧩 模块化单体 + 领域模块化**：`edu-mind-modules` 下 9 大业务模块（system / course / question / teaching / resource / knowledge / ai / notification / statistics），单进程部署，模块内部高内聚、模块之间低耦合
- **🔌 API 子模块强制隔离**：跨模块调用只允许走 `edu-mind-api/*-api` 中声明的 40+ 个 `*Api` 接口（如 `KnowledgeQueryApi`、`CourseQueryApi`、`AiChatApi`），实现类收拢在各模块 `api/impl/`，**禁止注入他人 Service / Mapper / Entity**
- **📂 七层职责分明**：`Controller → DTO → Service → Domain/DAO → Mapper → Entity → DB`，回程 `Entity → DAO → Converter → VO → Controller`；**Controller 禁止碰 Mapper，Service 禁止直接用 Mapper**
- **🧱 架构守卫常态化**：`ModuleBoundaryArchTest`、`GateEArchitectureTest` 等 ArchUnit 用例 + 前端 `layering-audit.mjs` 双向锁死分层红线
- **🎯 职责唯一**：RAG 编排只在 `edu-mind-ai/rag`、向量库适配只在 `edu-mind-infrastructure/vector`、知识资产业务只在 `edu-mind-knowledge`，全项目杜绝"第二套同名实现"

### 🤖 AI 智能引擎与 RAG 知识库

- **📚 知识库治理**：知识库 / 文档 / 切片（Chunk）三级资产模型，支持结构感知切片、向量持久化、文档解析流水线异步编排
- **🔀 混合检索（Hybrid Search）**：向量语义检索 + 词法匹配双通路召回，经 **RRF（Reciprocal Rank Fusion）** 融合重排（`edumind.rag.hybrid-enabled` / `min-rrf-score`），支持元数据过滤、TopK 与相似度阈值
- **🧭 RAG 全链路可观测**：`/api/ai/rag/debug` 可视化调试检索链路（召回明细、得分、上下文拼装、Prompt 预览），`/api/admin/knowledge/rag-dashboard` 汇总知识库健康度
- **💬 SSE 流式对话**：`POST /api/ai/chat/stream` 支持流式多轮对话（可上传附件、可中断流）、会话持久化与引用溯源；全局助手 `GlobalAssistant` 悬浮于任意页面提供伴学/伴教能力
- **🧠 Agent 与记忆体系**：Planner / ReAct Executor / ToolRegistry / Memory 四件套，`/api/ai/agents` 与 `/api/ai/agent/runs` 管理智能体定义与运行轨迹，`/api/ai/memories` 沉淀 Agent 长期记忆
- **✍️ AI 教学创作**：课程简介、教学目标、知识点、教案（Lesson Plan）、讲义内容、内容摘要、智能出题、智能组卷（`/api/ai/**` 全家桶）
- **📝 自动批改**：主观题 AI 批改 + 评分结果回流，与作业/考试模块闭环
- **🛰️ 模型网关与审计**：`AiGatewayFacade` 统一出入口，模型路由、失败降级、Token 计量（`/api/system/ai-audit`）与租户/用户算力配额（`/api/system/ai-quota`）
- **🗂️ Prompt 集中治理**：所有 Prompt 归类在 `ai/prompt/{system,teaching,rag,grading,question,agent,exam}` + `PromptRegistry`，支持后台在线编辑与版本管理（`/api/system/prompts`）
- **🖼️ 文档 OCR**：图片/扫描件 OCR 解析进知识库（`/api/knowledge/ocr-tasks`），dev 默认 mock 引擎、prod 可切 PaddleOCR

### 🕸️ 智能 Agent 与工具广场

- **🧰 AI 工具广场**：`/api/ai/tools` 面向师生开放统一工具入口，`/api/system/ai-tools` 支持管理端上下架、排序与权限配置
- **🧩 工具调用协议**：`agent/tool` 注册中心统一描述工具入参出参，Agent 按意图自动编排多工具链路
- **🔗 知识图谱**：`/api/**/graph` 提供知识点关系与图谱查询，前端以 **AntV G6** 渲染关系网络，并支持图谱关系智能推荐
- **🎯 意图路由**：`router/IntentRouterImpl` 按用户问题语义分发到问答 / 检索 / 工具 / 教学等不同处理链路

### 📚 课程建设与教学运营

- **🏫 课程全生命周期**：课程创建、章节 / 微课节编排、教学大纲、课程成员与角色、课程概览门户（教学目标、公告、教学团队）
- **📖 微课节与讲义**：课节正文编辑、学习进度、课节-知识点关联，课节讲义可一键建成"虚拟文档"进入知识库供 AI 伴学检索（`lesson_chapter_id` 溯源）
- **📂 课件资源中心**：课件上传 / 分类 / 媒体素材管理，支持与知识库文档双向关联，实现"资源即知识"
- **🎓 学习中心**：学习首页、学习路径、任务清单、推荐资源、在线练习、错题本、学习报告
- **🔔 通知中心**：站内消息、公告播报（普通 / 强弹窗 / 跑马灯三级优先级）与 WebSocket 实时推送

### 🎯 题库、作业与考试

- **🗃️ 题库体系**：题库 / 题目 / 题型（单选、多选、判断、填空、简答等）与知识点标签，支持 LaTeX 公式与富文本题干
- **🧪 智能组卷**：按知识点、难度、题型分布自动组卷，AI 生成题目 + 人工审核双通道，试卷预览与在线作答
- **📝 作业管理**：作业创建、分值/及格线/高级设置、学生提交、教师批改、评分与评语回流
- **📤 异步导出**：试卷 / 成绩导出任务化（`/api/question/exports`），带进度跟踪与 HTML/PDF 渲染引擎（PDFBox + openhtmltopdf + JLaTeXMath）
- **📕 个人错题本**：错题自动归集，记录最近作答与攻克状态，支持跨课程纠偏与专项巩固

### 📊 学情分析与教学干预

- **📈 多维学情大盘**：学习分析、课程统计、知识点掌握度、AI 使用分析、错题分析、教学报告
- **🚨 教学干预中心**：基于学情指标识别风险学生，生成干预建议与跟踪闭环
- **🗺️ 个性化推荐**：学习路径推荐、资源推荐、AI 练习推荐，`statistics-api` 对外暴露 `RecommendationQueryApi`
- **⏱️ 日聚合任务**：`CourseStatisticsJob` 每日 02:00 自动汇总前一日课程学习数据

### 🏢 多租户、组织与配额

- **🏛️ 租户隔离**：全链路 `tenant_id` 贯穿（Wave1/Wave2 已覆盖 35 张业务表），`TenantContext` + `TenantContextFilter` 自动注入数据域
- **🌳 组织院系树**：多层级组织（学校 / 院系 / 年级 / 班级）与成员管理，`TenantDataScope` 定义数据可见范围
- **🔑 租户内 RBAC**：`sys_user_role` 带 `tenant_id`，切换租户权限真实变化；支持预算 / 名额（SEATS）等多维配额（`/api/system/tenant-quotas`）
- **🧾 操作日志多租户加固**：操作日志带租户与差异快照（`OperationLogDiffUtils`），审计闭环可追溯

### 🛡️ 安全体系

- **🔐 Sa-Token 鉴权**：Token 名 `satoken`，默认 86400s 过期，Redis 集中会话；`UserContext` 保存当前登录上下文，`PermissionChecker` 独立负责权限判定
- **👥 RBAC 权限**：用户 / 角色 / 菜单 / 权限码四位一体，后端权限拦截 + 前端 `v-permission` / `v-role` 指令双向校验
- **✉️ 登录与验证码**：图形验证码 + **滑块验证码行为轨迹校验**（`/api/auth/captcha`、`/api/auth/captcha/slider/challenge`、`/slider/verify`）+ 邮箱 / 短信验证码（阿里云号码认证、腾讯云短信）
- **🔏 国密算法**：BouncyCastle 驱动 **SM2**（非对称）、**SM3**（摘要 / HMAC 验签）、**SM4-GCM**（对称加密）
- **🧿 KMS 密钥版本**：租户级密钥版本管理（`/api/system/security/keys`），支持密钥轮换与明文迁移（`CipherKdfMigrationRunner`）
- **🚧 传输安全**：`SignatureFilter` 校验 `X-Timestamp` / `X-Nonce` / `X-Signature`，`ReplayAttackFilter` 拦截重放，时间戳偏移窗口可配（`timestamp-skew-ms`）
- **🚦 限流与防护**：Redis 维度限流（`edumind.rate-limit.enabled`）、敏感路径清单（`sensitive-paths`）、全局异常兜底 `GlobalExceptionHandler`
- **🧬 链路追踪**：`TraceIdFilter` 为每个请求生成 trace id，贯穿日志与响应

### ⚙️ 系统治理与可观测性

- **🗂️ 系统配置中心**：字典 / 参数配置在线管理（`/api/system/configs`），多租户可覆盖
- **🧭 菜单驱动导航**：`sys_menu` 与前端路由 / 侧边栏 / 面包屑 / 多页签联动
- **🔧 模型与网关运维**：AI 模型配置（`/api/system/ai-models`）、网关路由看板（`/api/system/gateway`）、Token 审计（`/api/system/ai-audit`）
- **📜 操作日志**：模块、耗时、路径、入参出参与前后差异全量留痕，支持列表检索与详情下钻
- **🏁 启动自检**：`GaProductionStartupValidator` 在 GA 环境启动阶段校验关键配置，防止误配置上线

---

## 🛠️ 技术栈

- **后端框架**：Spring Boot **3.3.3** · JDK **17** · Maven 多模块（7 个顶级模块）· 模块化单体
- **持久层**：MyBatis-Plus **3.5.7**（`BaseMapper` + XML Mapper，`classpath*:/mapper/**/*.xml`）· MySQL **8.0** · HikariCP
- **缓存与锁**：Redis（Lettuce 连接池）+ Sa-Token Redis 会话 + **Redisson 3.34.1** 分布式锁
- **认证授权**：**Sa-Token 1.38.0**（`sa-token-spring-boot3-starter` + `sa-token-redis-jackson`）· RBAC · 图形/邮箱/短信验证码
- **安全与密码学**：**BouncyCastle 1.78.1**（SM2 / SM3 / SM4-GCM）· 请求签名与防重放 · 租户级 KMS 密钥版本
- **AI 与 RAG**：OpenAI 兼容协议（默认 `deepseek-chat`）· `text-embedding-3-small`（**1536 维**）· **Milvus SDK 2.4.4**（默认关闭时回落内存向量库）· 混合检索 + RRF 融合重排
- **对象存储**：**MinIO 8.5.10**（本地 / MinIO / 云 OSS 统一 `FileStorageService` 抽象）
- **文档处理**：**Apache PDFBox 2.0.31** · **openhtmltopdf 1.0.10** · **JLaTeXMath 1.0.7** · **Apache POI 5.2.5**（Word/Excel 导入导出）
- **工具库**：Hutool **5.8.28** · Fastjson2 **2.0.51** · Lombok
- **架构守卫**：**ArchUnit 1.3.0**（`archunit-junit5`）模块边界与分层断言
- **前端框架**：Vue **3.4.38** · TypeScript **5.5** · Vite **5.4** · Pinia **2.2** · Vue Router **4.4** · `vue-tsc` 类型门禁
- **前端 UI 与可视化**：Element Plus **2.8** · ECharts **5.5** · **AntV G6 5.1**（知识图谱）· SCSS
- **前端文档渲染**：markdown-it **14**（+ multimd-table）· KaTeX **0.16** · Mermaid **12** · highlight.js **11**
- **前端工程化**：Axios **1.7** · @vueuse/core **10.11** · Vitest **2.1**（happy-dom）· ESLint · 自研分层审计脚本
- **运行时要求**：JDK **17+** · Maven **3.9+** · Node.js **20+** · MySQL **8.0+** · Redis **7.x**

---

## 🏗️ 系统架构

```text
┌──────────────────────────────────────────────┐
│           edu-mind-frontend  :3000           │
│   Vue 3 + TS + Vite + Pinia + Element Plus   │
│   AI 教学 / 知识库 / 题库 / 学情 / 多租户后台  │
│   views → components → composables → services │
│                 → api → core/http             │
└───────────────────────┬──────────────────────┘
                        │  Vite /api 代理 (ws 支持)
                        ▼
┌──────────────────────────────────────────────────────────────┐
│                   edu-mind-boot :8080                        │
│  /api/auth · /api/courses · /api/questions · /api/ai/**       │
│  /api/system/** · /api/notifications · /api/analytics/**      │
│                                                              │
│  Sa-Token 鉴权 · TenantContext 多租户 · 签名防重放 · 限流     │
│                                                              │
│  Controller → DTO → Service → Domain/DAO → Mapper → Entity   │
│             ← Converter ← VO ←                               │
│          跨模块只走 edu-mind-api/*-api                        │
└───┬───────────────┬────────────────┬───────────────┬─────────┘
    │               │                │               │
    ▼               ▼                ▼               ▼
┌─────────┐   ┌──────────┐   ┌────────────┐   ┌──────────────┐
│ MySQL 8 │   │ Redis 7  │   │ MinIO 9000 │   │ Milvus 19530 │
│ edumind │   │ 会话/缓存 │   │ 课件/OCR   │   │ 向量检索      │
│ 3306    │   │ 限流/锁   │   │ bucket:    │   │ collection:  │
│         │   │ 6379     │   │ edumind    │   │ edumind_chunks│
└─────────┘   └──────────┘   └────────────┘   │ (1536 维)    │
                                              └──────────────┘
        ▲                                       (enabled=false
        │                                        → 内存向量库)
        │ OpenAI 兼容协议
        └──────────────► DeepSeek / OpenAI · Embedding API · PaddleOCR
```

| 维度 | 说明 |
|------|------|
| **🧩 架构形态** | 模块化单体（Modular Monolith）：单进程、单端口，模块边界靠 Maven 依赖 + `*-api` 接口 + ArchUnit 守卫 |
| **🔀 调用约束** | 禁止 `Controller → Mapper`、`Controller → Entity`、`Service → Mapper`、跨模块直接注入 Service；跨模块统一走 `KnowledgeQueryApi` / `CourseQueryApi` 等 41 个公开 API |
| **🔑 鉴权链** | 登录 `/api/auth` → Sa-Token 签发 `satoken` → Redis 会话 → `UserContext` 注入 → `PermissionChecker` 校验权限码 |
| **🏢 多租户链** | 请求进入 → `TenantContextFilter` 解析租户 → `TenantContext` 线程上下文 → MyBatis-Plus 租户条件 → DAO 自动收敛数据域 |
| **📡 接口信封** | `ApiResult { code, message, data, timestamp }`，成功 `code = 200`；分页统一 `PageResult` |
| **🤖 AI 链路** | 用户提问 → 意图路由 → Query 改写 → Embedding → 向量/词法双召回 → RRF 融合 → Context 构建 → Prompt 组装 → LLM → SSE 流式输出 → Token 审计 |
| **⏱️ 定时任务** | `@EnableScheduling` 已开启；`CourseStatisticsJob` 每日 02:00 聚合课程统计；`edu-mind-job` 承载文档解析与 OCR 调度 handler |

---

## 🧩 工程与模块清单

### 🖥️ 可运行应用（2 个）

| 模块 | 类型 | 端口 | 核心职责 |
|------|------|------|----------|
| `edu-mind-boot` | 🚪 后端 API | **8080** | Spring Boot 启动入口，聚合 9 大业务模块，统一提供 REST API、鉴权、多租户、AI 网关与定时任务 |
| `frontend` | 💻 前端应用 | **3000** | Vue 3 单页应用：教学运营、知识库治理、AI 课堂、题库作业、学情分析、多租户后台 |

### 📦 后端模块分层

| 模块 | 职责 | 包结构要点 |
|------|------|-----------|
| `edu-mind-common` | 全局共享内核（不含业务） | `api`（ApiResult / PageResult / ResultCode）· `exception` + `GlobalExceptionHandler` · `context`（UserContext / TenantContext）· `event` · `enums` · `markdown`（LaTeX 与索引清洗）· `utils` · `web`（TraceIdFilter） |
| `edu-mind-api` | 跨模块公开接口聚合（**纯接口，无实现**） | `edu-mind-{system,course,question,teaching,resource,knowledge,ai,notification,statistics}-api` 9 个子模块，共 41 个 `*Api` |
| `edu-mind-security` | 安全能力 | `crypto`（SM2 / SM3 / SM4-GCM）· `filter`（TenantContext / Signature / ReplayAttack）· `permission`（PermissionChecker）· `captcha` · `ratelimit` · `context` · `handler` |
| `edu-mind-infrastructure` | 技术设施适配（不写业务） | `oss`（统一 `FileStorageService`：Local / MinIO / 云 OSS 路由）· `vector`（Milvus / InMemory）· `redis`（cache / gateway）· `messaging` · `mail` · `sms` · `search` · `config` |
| `edu-mind-modules` | 9 大业务领域模块聚合 | 每模块统一 `controller / service / dao / mapper / entity / dto / vo / converter / domain / api(+impl)` |
| `edu-mind-job` | 定时与离线任务调度 | `handler/{ai,course,knowledge,notification,resource,statistics,system,teaching}` + `schedule`；已实现文档解析流水线与 OCR 调度 |
| `edu-mind-boot` | 启动与配置 | `EduMindApplication`（`@SpringBootApplication @EnableScheduling @MapperScan("com.edumind.**.mapper")`）+ `boot/ga/GaProductionStartupValidator` + 5 套 profile 配置 |

### 🧠 业务模块明细（`edu-mind-modules`）

| 模块 | 领域职责 | 特色子包 |
|------|----------|----------|
| `edu-mind-ai` | AI 对话 / RAG / Agent / 模型网关 / Prompt / 出题组卷 / 批改 / 配额审计 | `rag/{config,context,model,pipeline,query,rerank,retrieval}` · `prompt/{system,teaching,rag,grading,question,agent,exam}` · `agent/{planner,react,executor,tool,memory}` · `integration/{llm,embedding,vector,rerank,ocr,crypto}` · `gateway` · `router` |
| `edu-mind-knowledge` | 知识资产：知识库 / 文档 / 切片 / 图谱 / 知识点 / OCR | `service/{knowledge,chunk,index,graph,point,query,rag,lesson,ocr}` · `integration/ocr` · `domain/{graph,knowledge}` |
| `edu-mind-course` | 课程 / 章节 / 微课节 / 成员 / 大纲 / 概览门户 | `service/{course,chapter,lesson,knowledge,member,outline,overview,access,query}` |
| `edu-mind-question` | 题库 / 题目 / 错题 / 异步导出 | `service/{question,bank,wrong,export,query}` · `integration/export` |
| `edu-mind-teaching` | 作业 / 考试 / 班级 / 提交批改 | `service/{assignment,exam,classroom,submission,grade,grading,homework,teaching,query}` |
| `edu-mind-resource` | 课件 / 素材 / 媒体 / 文档资源 | `service/{resource,material,media,document,course}` |
| `edu-mind-system` | 用户 / 角色 / 权限 / 菜单 / 租户 / 组织 / 配额 / 配置 / 密钥 | `service/{user,role,permission,menu,tenant,department,quota,config,security,email,sms,log,rbac,query}` · `controller/tenant` · `controller/security` |
| `edu-mind-notification` | 站内通知 / 公告 / 广播 / WebSocket 推送 | `service/{notification,announcement,broadcast,message,push}` · `websocket` |
| `edu-mind-statistics` | 学情 / 课程统计 / 掌握度 / 干预 / 个性化推荐 | `service/{analytics,dashboard,learning,teaching,ai,intervention,query}` · `job` |

### 🎨 前端领域分层（`frontend/src`）

| 目录 | 职责 | 领域子目录 |
|------|------|-----------|
| `views/` | 路由级页面 | ai · analytics · auth · course · dashboard · knowledge · learning · notice · profile · question · system（107 个页面） |
| `components/` | 可复用 UI 组件 | ai · analytics · auth · common · course · dashboard · knowledge · learning · notification · profile · question · system（357 个组件） |
| `composables/` | Vue 可复用逻辑 | 同一批领域 + `layout`，共 13 个领域（170 个 `useXxx.ts`，含单测） |
| `api/` | 后端 HTTP 接口定义 | 11 个领域共 70 个文件，按后端资源一一对应 |
| `services/` | 跨 API 的业务编排 | `ai`（SSE 流式服务）、`rag`、`course`、`analytics`、`system` |
| `features/` | 完整业务能力 | `agent/{planning,tool-calling,execution}` · `rag/{retrieval,rerank,citation,pipeline}` · `recommendation/{learning,resource}` |
| `stores/` | Pinia 全局状态 | app · auth · user · ai · course · knowledge · learning · notification · system |
| `types/` | TS 类型定义 | 12 个领域，与后端 DTO / VO 语义对齐 |
| `core/` | 基础设施（唯一 HTTP 出口） | http（axios / 拦截器 / 请求签名）· auth · sse · websocket · storage |
| `utils/` | 纯工具函数 | 17 个子目录（format / markdown / ai / crypto / question …） |
| `constants/` | 全局常量 | ai · app · auth · course · knowledge · learning · question · tenant · permission |
| `router/` · `layouts/` · `directives/` · `config/` · `mock/` · `styles/` | 路由、布局、指令、环境配置、Mock、样式 | 路由按领域拆 `routes/*.ts`，布局含侧边栏 / 头部 / 多页签 |

> **分层铁律**：`views`（页面）→ `components`（UI）→ `composables / features`（逻辑）→ `services`（编排）→ `api`（接口定义）→ `core/http`（基础设施）。`views` 之间禁止互相 import。

---

## 🚀 快速开始

### ⚙️ 环境要求

| 软件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 后端编译与运行 |
| Maven | 3.9+ | 多模块构建 |
| Node.js | 20+ | 前端构建 |
| MySQL | 8.0+ | 库名 **`edumind`**，默认 `root` / `root` |
| Redis | 7.x | `127.0.0.1:6379`，默认无密码 |
| MinIO | 可选 | `localhost:9000`，`minioadmin` / `minioadmin`，bucket `edumind` |
| Milvus | 可选 | 默认 `enabled: false`，自动回落内存向量库，开箱即用 |

### 1. 🗄️ 初始化数据库

**全新空库（仅当 `edumind` 库内 0 张表）**：

```powershell
mysql -u root -p < sql/init.sql
```

`init.sql` 已包含 V0.1 ~ V2.5.8 全部迁移最终状态（75+ 张表 + 权限目录 + 演示种子），**新库跑完 init 无需再执行任何 `V*.sql`**。

**已有库升级**：按 `sql/README.md` 中的顺序执行 `sql/migration/V*.sql`（执行前务必备份，`init.sql` 会拒绝在非空库执行）。

### 2. 🔴 启动 Redis（与可选中间件）

```powershell
redis-server            # 监听 6379
# 可选：课件对象存储
minio.exe server C:\minio-data --console-address ":9001"
```

### 3. ☕ 启动后端

工作目录必须是 `backend`（聚合 POM 无 main class，禁止在根 POM 上跑 `spring-boot:run`）：

```powershell
cd backend
mvn -pl edu-mind-boot -am spring-boot:run
```

或先构建再运行：

```powershell
cd backend
mvn clean package -DskipTests
java -jar edu-mind-boot/target/edu-mind-boot-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

IDE（IDEA / VS Code）：运行 `com.edumind.EduMindApplication`，Active profiles 填 `dev`。

启动成功会打印 EduMind 彩色横幅，输出本地入口、局域网入口与当前激活环境；健康检查 `http://localhost:8080/api/auth/captcha`（图形验证码接口可验证服务已就绪）。

### 4. 💻 启动前端

```powershell
cd frontend
npm install
npm run dev
```

浏览器打开 **http://localhost:3000**（Vite 已把 `/api` 代理到 `http://localhost:8080`，WebSocket 亦转发）。

### 🔐 默认演示账号

| 账号 | 密码 | 角色 | 说明 |
|------|------|------|------|
| `admin` | `admin123` | ADMIN | 平台系统管理员（`tenant_id = 0`，全租户生效） |
| `teacher` | `admin123` | TEACHER | 教师：课程、题库、作业、批改、AI 备课 |
| `student` | `admin123` | STUDENT | 学生：学习、练习、作业、错题本 |
| `student2` | `admin123` | STUDENT | 第二个学生账号，便于对比学情与多租户验证 |

> 种子仅用于演示；生产环境请立即修改密码并轮换密钥。

---

## 📁 目录结构

```text
EduMind/
├── README.md
├── AGENTS.md / .cursorrules                # 全栈架构与编码规范（AI 强制遵循）
├── pom.xml                                 # 工作区聚合 POM（仅用于 IDE 识别 backend）
│
├── docs/                                   # PRD、原型图、开发计划、宣传图
│   ├── EduMind_AI智能教学赋能平台_PRD_V1.2.md
│   ├── 原型图.png / logo.png / ...
│   └── 开发计划/
│
├── sql/                                    # 数据库脚本中心（详见 sql/README.md）
│   ├── init.sql                            # 全新空库唯一基线（建表 + 种子，不 DROP）
│   ├── migration/                          # 增量版本脚本 V0.1 → V2.6.4
│   └── rollback/                           # 回滚脚本
│
├── scripts/
│   └── regression/                         # 回归测试脚本
│
├── backend/                                # ☕ Spring Boot 多模块后端
│   ├── pom.xml                             # edu-mind-parent（Spring Boot 3.3.3 / JDK 17）
│   ├── docs/                               # 架构、领域边界、安全设计文档
│   ├── scripts/                            # API 子模块脚手架与校验脚本（PowerShell）
│   │
│   ├── edu-mind-common/                    # 全局内核：ApiResult / 异常 / 上下文 / 事件 / 工具
│   │   └── src/main/java/com/edumind/common/{api,annotation,constant,context,enums,event,
│   │                                            exception,markdown,model,utils,validator,web}
│   │
│   ├── edu-mind-api/                       # 跨模块公开接口（纯接口，无实现）
│   │   ├── edu-mind-ai-api/                #   AiChatApi · AiQueryApi · RagRuntimeQueryApi …
│   │   ├── edu-mind-knowledge-api/         #   KnowledgeQueryApi · ChunkRetrievalApi …
│   │   ├── edu-mind-course-api/            #   CourseQueryApi · LessonQueryApi …
│   │   ├── edu-mind-system-api/            #   UserQueryApi · TenantQueryApi · TenantQuotaApi …
│   │   ├── edu-mind-teaching-api/          #   ExamQueryApi · SubmissionQueryApi …
│   │   ├── edu-mind-question-api/          #   QuestionQueryApi · QuestionCommandApi
│   │   ├── edu-mind-resource-api/          #   ResourceQueryApi
│   │   ├── edu-mind-statistics-api/        #   KnowledgeMasteryQueryApi · RecommendationQueryApi
│   │   └── edu-mind-notification-api/      #   NotificationWriteApi
│   │
│   ├── edu-mind-security/                  # 认证授权 / 国密 / 签名 / 防重放 / 限流
│   │   └── com/edumind/security/{captcha,config,context,crypto,exception,
│   │                             filter,handler,permission,ratelimit}
│   │
│   ├── edu-mind-infrastructure/            # 技术设施适配（不写业务规则）
│   │   └── com/edumind/infrastructure/{config,mail,messaging,oss,redis,search,sms,vector}
│   │
│   ├── edu-mind-modules/                   # 9 大业务领域模块
│   │   ├── edu-mind-system/                # 用户 / 角色 / 权限 / 菜单 / 租户 / 组织 / 配置 / 密钥
│   │   ├── edu-mind-course/                # 课程 / 章节 / 微课节 / 成员 / 概览
│   │   ├── edu-mind-question/              # 题库 / 题目 / 错题 / 导出
│   │   ├── edu-mind-teaching/              # 作业 / 考试 / 班级 / 提交批改
│   │   ├── edu-mind-resource/              # 课件 / 素材 / 媒体
│   │   ├── edu-mind-knowledge/             # 知识库 / 文档 / 切片 / 图谱 / OCR
│   │   ├── edu-mind-ai/                    # AI 对话 / RAG / Agent / Prompt / 出题组卷 / 批改
│   │   ├── edu-mind-notification/          # 通知 / 公告 / 广播 / WebSocket
│   │   └── edu-mind-statistics/            # 学情 / 统计 / 干预 / 推荐
│   │       └── src/main/java/com/edumind/<domain>/
│   │           ├── api/ + api/impl/        #   对外 API 实现
│   │           ├── controller/<子域>/       #   仅绑参 + 调 Service + 返回 ApiResult
│   │           ├── service/<子域>/ + impl/  #   业务编排、事务、事件发布
│   │           ├── dao/                    #   业务层唯一数据入口（封装 Mapper）
│   │           ├── mapper/                 #   MyBatis-Plus BaseMapper / 自定义 SQL
│   │           ├── entity/                 #   一表一实体
│   │           ├── dto/ · vo/              #   入参 / 出参，禁止互串
│   │           ├── converter/              #   DTO / Entity / VO / Domain 转换
│   │           └── domain/                 #   领域模型（复杂业务才引入）
│   │
│   ├── edu-mind-job/                       # 定时与离线任务（文档解析 / OCR 调度 handler）
│   │
│   └── edu-mind-boot/                      # 🚪 启动模块
│       └── src/main/
│           ├── java/com/edumind/EduMindApplication.java
│           ├── java/com/edumind/boot/ga/GaProductionStartupValidator.java
│           └── resources/
│               ├── application.yml            # 基座：端口 8080 / MySQL / Redis / Sa-Token
│               ├── application-dev.yml        # 本地：MinIO / LLM / Embedding / RAG 参数
│               ├── application-prod.yml       # 生产：全部走环境变量
│               ├── application-sm.yml         # 国密 profile：SM 开关 / 签名 / 敏感路径
│               ├── application-test.yml       # 测试：全 Mock
│               ├── logback-spring.xml
│               ├── mapper/                    # 全局 XML Mapper
│               └── captcha-bg/                # 验证码背景图
│
└── frontend/                               # 💻 Vue 3 前端工程
    ├── package.json / vite.config.ts / tsconfig.json
    ├── docs/frontend-layering-audit.md     # 前端分层审计规范
    ├── scripts/layering-audit.mjs          # 分层违规扫描（CI 可用）
    ├── .env / .env.development / .env.production
    └── src/
        ├── main.ts                          # → app/bootstrap.ts → app/providers.ts
        ├── app/                             # App.vue / bootstrap.ts / providers.ts
        ├── api/(11 个领域)                   # 后端接口定义
        ├── views/(11 个领域)                 # 107 个路由级页面
        ├── components/(12 个领域)            # 357 个可复用组件
        ├── composables/(13 个领域)           # 170 个 Vue 逻辑单元（含单测）
        ├── features/                        # agent / rag / recommendation 业务能力
        ├── services/                        # ai / rag / course / analytics / system 编排
        ├── stores/                          # app · auth · user · ai · course · knowledge …
        ├── core/{http,auth,sse,websocket,storage}
        ├── router/{index.ts,guards.ts,routes/*.ts}
        ├── layouts/ · directives/ · config/ · constants/ · types/ · utils/
        ├── mock/                            # 18 个 Mock 数据集（dev 默认关闭）
        └── styles/                          # 主题变量 / mixins / Element Plus 覆盖 / 打印样式
```

---

## ⚙️ 配置说明

### 🌍 环境 profile

| 环境 | 配置文件 | 如何启用 | 关键差异 |
|------|----------|----------|----------|
| **开发（默认）** | `application-dev.yml` | `spring.profiles.active: dev`（基座已默认） | 本地 MySQL / Redis；MinIO `localhost:9000`；LLM `deepseek-chat`；Embedding / OCR `mock-enabled: true`；Milvus `enabled: false`；打印 SQL |
| **测试** | `application-test.yml` | `--spring.profiles.active=test` | 本地库；LLM / Embedding / OCR 全 Mock；限流关闭 |
| **国密** | `application-sm.yml` | 与业务 profile 叠加 | `sm-enabled: true`、HMAC 密钥、时间戳偏移 300s、敏感路径清单 |
| **生产** | `application-prod.yml` | `--spring.profiles.active=prod` | 数据源 / Redis 全走环境变量；OCR 切换 PaddleOCR；Mock 全部关闭 |

### 🔑 关键环境变量

| 变量 | 说明 |
|------|------|
| `MYSQL_HOST` / `MYSQL_USER` / `MYSQL_PASSWORD` | 生产数据源（prod profile 必填） |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 连接（基座即支持） |
| `AI_API_KEY` / `DEEPSEEK_API_KEY` | LLM 调用密钥（`ai.llm.api-key`） |
| `EMBEDDING_API_KEY` | 向量化模型密钥（`edumind.embedding.api-key`） |
| `EDUMIND_SECURITY_MASTER_SECRET` | 安全主密钥（`edumind.security.master-secret`） |
| `EDUMIND_AI_MODEL_KEY_SECRET` | 模型 API Key 落库加密密钥（`edumind.ai.model-key-secret`） |
| `PADDLE_OCR_BASE_URL` | PaddleOCR 服务地址（prod OCR 引擎） |

### 📋 后端主要配置项

| 配置项 | 开发默认 | 说明 |
|--------|----------|------|
| `server.port` | `8080` | 后端端口 |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/edumind` | 库名 `edumind`，`root` / `root` |
| `spring.servlet.multipart.max-file-size` | `50MB` | 单文件上限（请求体 55MB） |
| `sa-token.token-name` / `timeout` | `satoken` / `86400` | Token 名与过期秒数 |
| `edumind.tenant.demo-auto-bind-enabled` | `true` | 演示租户自动绑定 |
| `edumind.rate-limit.enabled` | `true` | 限流总开关 |
| `edumind.ai.daily-quota` | `500` | 用户每日 AI 调用配额 |
| `edumind.rag.hybrid-enabled` / `min-rrf-score` | `true` / `0.03` | 混合检索开关与 RRF 分数下限 |
| `edumind.chunk.size` / `overlap` | `800` / `128` | 切片长度与重叠 |
| `edumind.embedding.dimensions` | `1536` | 向量维度（须与 Milvus collection 一致） |
| `milvus.enabled` / `port` / `collection` | `false` / `19530` / `edumind_chunks` | 关闭时使用内存向量库 |
| `edumind.security.sm-enabled` | `false` | 国密总开关（`application-sm.yml` 置 `true`） |
| `knowledge.ocr.engine` | `mock`（prod 为 `paddle`） | OCR 引擎 |
| `mybatis-plus.configuration.log-impl` | 开发打印 SQL | 生产关闭 |

### 💻 前端环境变量

| 变量 | 值 | 说明 |
|------|-----|------|
| `VITE_APP_TITLE` | `智教云 · EduMind｜AI 智能教学赋能平台` | 浏览器标题 |
| `VITE_API_BASE_URL` | `/api` | 开发走 Vite 代理，生产走 Nginx 同域反代 |
| `VITE_API_TIMEOUT` | `30000` | 常规请求超时（AI 请求单独 180s，见 `config/api.ts`） |
| `VITE_USE_MOCK` | `false` | dev 默认关闭 Mock，直接连真实后端 |
| `VITE_SM_HMAC_SECRET` | 演示密钥 | 国密请求签名（`core/http/request-signature.ts`） |

---

## 💡 开发指南

### 📐 后端分层约定（必守）

```text
Controller → DTO → Service → Domain / DAO → Mapper → Entity → Database
Database → Entity → DAO → Service → Converter → VO → Controller

禁止：Controller → Mapper / Entity / DAO
禁止：Service  → Mapper
禁止：跨模块注入他人 Service / Mapper / Entity  →  必须走 *-api
```

- `Controller` 只做：接请求 · 校验参数 · 调 Service · 返回 `ApiResult`，**禁止业务逻辑**
- `DAO` 是业务层唯一数据入口，负责封装 Mapper 与查询条件，**对上隐藏持久化细节**
- `Converter` 统一承载 DTO / Entity / VO / Domain 互转，**禁止散落 `vo.setXxx(entity.getXxx())`**
- `edu-mind-common` 只放全局共享物，**禁止塞业务代码**；`edu-mind-infrastructure` 只做技术适配，**禁止写业务规则**
- 单文件 ≤ 600 行、单方法 ≤ 50 行、超 80 行必须拆分；**禁止 `CommonUtils` / `BusinessUtils` 之类超级工具类**

### 🎨 前端目录约定（必守）

```text
views/{领域}/{功能}          →  页面（薄壳，禁止互相 import）
components/{领域}/XxxPanel.vue →  可复用 UI
composables/{领域}/useXxx.ts   →  响应式逻辑
features/{能力}/              →  跨页面完整业务能力
services/{领域}/              →  多 API 编排
api/{领域}/{资源}.ts          →  仅声明请求地址 / 方法 / 参数 / 类型
core/http/                    →  全局唯一 Axios 实例
```

- 页面**禁止直接 import Axios**，必须经 `api` / `services`
- 新增功能前先判断领域，再分别落到 `views / components / api / types / composables`
- **禁止平铺目录**、**禁止重复文件**、**禁止随意新增顶级目录**；UI 图标一律使用 Element Plus Icons 或项目内 SVG，**禁止 Emoji 充当图标**
- 分层迁移必须"纯搬迁"：DOM 结构、样式、交互、接口调用一律不变

### 📝 统一响应与错误码

| 字段 | 说明 |
|------|------|
| `code` | `200` 成功 · `400` 参数校验失败 · `401` 未登录或 Token 过期 · `403` 无权限 · `404` 资源不存在 · `405` 方法不支持 · `429` 请求过于频繁 · `500` 系统异常 |
| `message` | 提示文案 |
| `data` | 业务数据（分页使用 `PageResult`） |
| `timestamp` | 服务端时间戳 |

### 🔗 接口约定（摘录）

| 用途 | 方法 | 路径 |
|------|------|------|
| 登录 / 登出 / 当前用户 | POST/GET | `/api/auth/**` |
| 用户偏好设置 | GET/PUT | `/api/users/me/preferences` |
| 个人 AI 用量 | GET | `/api/users/me/ai-usage` |
| 课程 / 章节 / 课节 | GET/POST/PUT | `/api/courses/**` |
| 课程成员 / 概览门户 | GET/POST | `/api/courses/{courseId}/members` · `/api/courses/{courseId}` |
| 课件资源 | GET/POST | `/api/courses`（resource 模块，`/api/courses/{id}/resources`） |
| 题库 / 题目 / 错题 | GET/POST | `/api/question-banks` · `/api/questions` |
| 试卷导出 | POST/GET | `/api/question/exports` |
| 作业 / 考试 / 提交 | GET/POST | `/api/assignments` · `/api/exams` · `/api/submissions` |
| AI 流式对话 | POST | `/api/ai/chat/stream`（SSE）· 附件 `/attachment` · 中断 `DELETE /stream/{streamId}` |
| 对话可用模型 | GET | `/api/ai/chat/models` |
| AI 会话管理 | GET/POST/DELETE | `/api/ai/conversations` |
| 全局助手 | POST | `/api/ai/assistant` |
| RAG 调试 | GET/POST | `/api/ai/rag/models` · `/api/ai/rag/debug` |
| 知识库检索 | POST | `/api/knowledge-bases/{id}/retrieve` |
| 知识库 / 文档 / 切片 | GET/POST | `/api/knowledge-bases/**` · `/api/documents/**` |
| 知识图谱 | GET/POST/DELETE | `/api/knowledge-bases/{id}/graph` · `/graph/gaps` · `/graph/suggest-relations` · `/api/knowledge-points/{id}/relations` |
| 文档 OCR | GET/POST | `/api/knowledge/ocr-tasks` |
| RAG 大盘 | GET | `/api/admin/knowledge/rag-dashboard` |
| Agent 定义与运行 | GET/POST | `/api/ai/agents` · `/api/ai/agent/runs` |
| Agent 记忆 | GET/POST/DELETE | `/api/ai/memories` |
| AI 工具广场 | GET/POST | `/api/ai/tools`（师生）· `/api/system/ai-tools`（管理端） |
| AI 出题 / 组卷 | POST | `/api/ai/questions` · `/api/ai/paper` · `/api/ai/exams` |
| AI 教学创作 | POST | `/api/ai/lesson-content` · `/api/ai/summary` · `/api/ai/course-objectives` · `/api/ai/course-knowledge-points` · `/api/ai/course-profile` |
| 模型与网关 | GET/POST | `/api/system/ai-models` · `/api/system/gateway` |
| Prompt 管理 | GET/POST | `/api/system/prompts` |
| Token 审计 | GET | `/api/system/ai-audit` |
| AI 配额 | GET/PUT | `/api/system/ai-quota` |
| 用户 / 角色 / 权限 / 菜单 | GET/POST | `/api/system/users` · `/roles` · `/permissions` · `/menus` |
| 租户 / 组织 / 配额 | GET/POST | `/api/system/tenants` · `/organizations` · `/tenant-quotas` |
| 系统配置 | GET/POST | `/api/system/configs` |
| 密钥版本管理 | GET/POST | `/api/system/security/keys` |
| 操作日志 | GET | `/api/system/oper-log` |
| 文件存储 | GET/POST | `/api/storage/files/**` |
| 通知 / 公告广播 | GET/POST | `/api/notifications` · `/api/notifications/broadcast` |
| 学情分析 | GET | `/api/analytics/**` |
| 学习中心 / 推荐 / 错题本 | GET/POST | `/api/learning/**` |

完整路由见各模块 `controller/` 包。

### 🖥️ 本地调试入口

| 端 | 地址 | 说明 |
|----|------|------|
| 后端 API | http://localhost:8080 | Spring Boot 主服务 |
| 前端应用 | http://localhost:3000 | Vite Dev Server（`/api` 代理到 8080） |
| MinIO 控制台 | http://localhost:9001 | 可选，对象存储管理 |

### ❓ 常见问题

| 现象 | 排查 |
|------|------|
| 启动报数据库连接失败 | MySQL 未启动、库 `edumind` 未创建、或账号密码与 `application.yml` 不一致 |
| 启动报 Redis 连接失败 | Redis 未监听 6379，或 `REDIS_PASSWORD` 与实例配置不一致 |
| 表不存在 / 字段缺失 | 全新库请执行 `sql/init.sql`；已有库按 `sql/README.md` 顺序补 `V*.sql` |
| `init.sql` 执行报错终止 | 库中已有表（脚本设计为只允许空库执行），请改用 `sql/migration/` |
| 接口返回 401 | Token 缺失或过期；检查请求头是否携带 `satoken` |
| 接口返回 403 | 当前角色缺少权限码；检查 `sys_role_permission` 与租户内角色绑定 |
| 多租户看不到数据 / 串数据 | 检查 `TenantContext` 是否注入成功、业务表是否含 `tenant_id`、`TenantDataScope` 配置 |
| 签名校验失败（国密） | 检查 `X-Timestamp` / `X-Nonce` / `X-Signature`、HMAC 密钥与服务器时间偏差（`timestamp-skew-ms`） |
| AI 对话无响应 | 检查 `AI_API_KEY` / `DEEPSEEK_API_KEY`、模型是否启用、网关路由与用户配额是否耗尽 |
| 向量检索无结果 | `edumind.embedding.mock-enabled`、向量维度是否 1536、Milvus collection 是否与配置一致 |
| OCR 任务不执行 | dev 默认 `mock` 引擎；prod 需配置 `PADDLE_OCR_BASE_URL` 并切换 `engine: paddle` |
| 上传失败 | MinIO 是否启动、bucket `edumind` 是否存在、单文件是否超过 50MB |
| 前端 401 循环跳登录 | 检查 `satoken` 是否被清理、`router/guards.ts` 鉴权分支与后端返回码是否对齐 |
| 前端分层审计报错 | 执行 `npm run audit:layering` 查看违规文件，按 `frontend/docs/frontend-layering-audit.md` 修正 |

### ✅ 提交前自检

```text
□ mvn clean package -DskipTests 通过（含 ArchUnit 架构守卫）
□ npm run build（vue-tsc 类型检查 + 构建）通过
□ npm run lint / npm run audit:layering 无违规
□ 新增 Service / DAO / API / 组件前已确认无重复职责实现
□ 未破坏模块边界、未绕过 DAO、未用 Entity 充当前端入参或返回体
```

---

## 📦 构建与部署

### ☕ 后端

```powershell
cd backend
mvn clean package -DskipTests
# 产物：backend/edu-mind-boot/target/edu-mind-boot-1.0.0-SNAPSHOT.jar

$env:SPRING_PROFILES_ACTIVE = "prod"
java -jar edu-mind-boot/target/edu-mind-boot-1.0.0-SNAPSHOT.jar
```

生产上线前务必：

1. 通过环境变量注入数据库 / Redis 密码与 `EDUMIND_SECURITY_MASTER_SECRET`、`EDUMIND_AI_MODEL_KEY_SECRET`
2. 按需启用 `sm` profile（`SPRING_PROFILES_ACTIVE=prod,sm`）并配置国密 HMAC 密钥
3. 关闭 `mybatis-plus` SQL 打印，关闭所有 `mock-enabled`
4. 配置 Milvus（`milvus.enabled: true`）与 PaddleOCR 服务地址

### 💻 前端

```powershell
cd frontend
npm run build      # 产物 dist/
npm run preview    # 本地预览
```

Nginx 示例：静态托管 `frontend/dist`，把 `/api` 反代到 `127.0.0.1:8080`（需保留 WebSocket 升级头），并为 AI 流式接口关闭响应缓冲（`proxy_buffering off`）。

建议启动顺序：**MySQL → Redis →（MinIO / Milvus 可选）→ 后端 → 前端**。

---

## 📜 许可证

本项目仅供学习与内部教学使用。
