# 智教云 · EduMind｜AI 智能教学赋能平台 - 全栈开发与架构规范

你现在正在开发项目：**智教云 · EduMind｜AI 智能教学赋能平台**。

本项目为标准前后端分离的现代化全栈架构，涵盖**后端模块化服务**与 **Vue3 现代化前端工程**：
- **后端技术栈与架构**：Spring Boot 3.x + JDK 17 + Maven 多模块 + MyBatis-Plus + Redis + Sa-Token，整体架构定位为 **模块化单体 Modular Monolith + 领域模块化 + 分层架构**。
- **前端技术栈与架构**：Vue 3 + TypeScript + Vite + Pinia + Vue Router + Element Plus + Axios + SCSS + ECharts，严格遵循**领域分层与无重复职责目录规范**。

后续所有代码生成、修改、重构、新功能开发，必须严格遵守以下两大部分规范。

---

# 第一部分：后端架构与编码规范

## 1. 总体分层规则

标准调用链必须遵循：

```text
Controller
→ DTO
→ Service
→ Domain / DAO
→ Mapper
→ Entity
→ Database

Database
→ Entity
→ DAO
→ Service
→ Converter
→ VO
→ Controller
```

禁止跳层调用。

禁止出现：

```text
Controller → Mapper
Controller → Entity
Controller → DAO
Service → Controller
Mapper → Service
```

Controller 只负责：

- 接收请求
- 参数校验
- 调用 Service
- 返回统一响应

Controller 中禁止编写复杂业务逻辑。

---

## 2. DTO、VO、Entity、Domain 职责必须严格区分

### DTO

DTO 只用于：

- 接收前端请求参数
- 接口参数传输
- 参数校验

禁止把数据库 Entity 直接作为 Controller 入参。

---

### VO

VO 只用于：

- 返回给前端的数据
- 页面展示模型
- API 响应模型

禁止直接向前端返回 Entity。

---

### Entity

Entity 只负责：

- 数据库表映射
- MyBatis-Plus 持久化

Entity 不允许承担 Controller 请求对象或前端返回对象职责。

---

### Domain

Domain 用于：

- 核心业务模型
- 领域规则
- 复杂业务状态
- 业务行为封装

不要为了“DDD”而机械创建 Domain。

简单 CRUD 业务允许：

```text
DTO → Service → DAO → Mapper → Entity
```

复杂业务再引入 Domain。

---

## 3. Converter 规则

对象转换统一通过 Converter 完成。

例如：

```text
UserCreateDTO → UserEntity
UserEntity → UserVO
CourseDTO → Course
Course → CourseEntity
```

禁止在 Controller 中大量：

```java
vo.setXxx(entity.getXxx());
```

禁止在 Service 中散落大量重复 Bean 属性转换代码。

Converter 按业务对象划分：

```text
UserConverter
RoleConverter
CourseConverter
ChapterConverter
KnowledgeConverter
```

---

## 4. DAO 和 Mapper 必须明确区分

### Mapper

Mapper 是底层数据库访问层。

职责：

- MyBatis / MyBatis-Plus SQL 操作
- BaseMapper
- 自定义 SQL
- XML Mapper

例如：

```text
UserMapper
CourseMapper
KnowledgeMapper
```

---

### DAO

DAO 是业务层访问数据的统一入口。

调用关系：

```text
Service
↓
DAO
↓
Mapper
↓
Database
```

Service 不允许直接调用 Mapper。

正确：

```java
private final UserDao userDao;
```

禁止：

```java
private final UserMapper userMapper;
```

DAO 可以：

- 封装多个 Mapper 查询
- 封装复杂查询条件
- 隔离 MyBatis-Plus
- 对上层隐藏数据库实现细节

---

## 5. 模块之间禁止直接访问内部实现

EduMind 当前主要业务模块：

```text
edu-mind-system
edu-mind-course
edu-mind-teaching
edu-mind-resource
edu-mind-knowledge
edu-mind-ai
edu-mind-notification
edu-mind-statistics
```

每个模块必须保持边界清晰。

禁止：

```text
AI → KnowledgeMapper
AI → KnowledgeEntity

Teaching → CourseMapper
Teaching → CourseEntity

Statistics → TeachingMapper
```

跨模块调用统一使用公开 API / Facade。

例如：

```text
AI
↓
KnowledgeQueryApi
↓
Knowledge Module
```

```text
Teaching
↓
CourseQueryApi
↓
Course Module
```

建议每个需要对外提供能力的模块增加：

```text
api/
```

例如：

```text
edu-mind-knowledge/
└── api/
    ├── KnowledgeQueryApi.java
    └── KnowledgePointQueryApi.java
```

---

## 6. 严禁循环依赖

禁止出现：

```text
edu-mind-ai
→ edu-mind-knowledge
→ edu-mind-ai
```

也禁止：

```text
course
→ teaching
→ course
```

如果两个模块需要互相获取数据：

优先设计：

- API
- Facade
- Domain Event
- MQ
- 公共只读模型

禁止直接互相注入 Service。

---

## 7. Knowledge 与 AI 的边界

这是 EduMind 必须严格遵守的规则。

### edu-mind-knowledge

负责“知识资产”。

包括：

```text
知识库
知识点
知识体系
知识图谱
知识关系
知识结构
知识元数据
```

---

### edu-mind-ai

负责“AI 如何使用知识”。

包括：

```text
AI 对话
AI 助手
RAG
Embedding
Vector Retrieval
Rerank
Prompt
Agent
LLM
```

因此：

禁止在：

```text
edu-mind-ai/knowledge/
```

重复建设知识库、知识点、知识图谱 CRUD。

AI 只能通过 Knowledge 模块提供的 API 获取知识数据。

---

## 8. RAG 职责必须集中

禁止同时在多个模块重复实现 RAG 检索流程。

RAG 核心编排归：

```text
edu-mind-ai
```

建议目录：

```text
rag/
├── retrieval/
├── rerank/
├── context/
├── pipeline/
└── query/
```

流程：

```text
用户问题
→ Query Rewrite
→ Embedding
→ Vector Retrieval
→ Metadata Filter
→ Rerank
→ Context Builder
→ Prompt Builder
→ LLM
→ Answer
```

Knowledge 模块只负责提供知识数据，不负责完整 AI RAG 流程。

---

## 9. Vector Search 不允许重复实现

`edu-mind-infrastructure` 和 `edu-mind-ai` 职责必须分开。

Infrastructure：

```text
vector/
```

只负责底层向量数据库访问和技术适配。

例如：

```text
MilvusVectorStore
PgVectorStore
ElasticVectorStore
```

AI 模块：

```text
rag/retrieval/
```

负责：

- 检索策略
- topK
- filter
- score threshold
- retrieval pipeline

禁止两个模块都创建职责完全相同的：

```text
VectorSearchService
```

---

## 10. OSS / 文件存储规则

文件存储统一抽象。

推荐：

```text
FileStorageService
```

作为统一接口。

具体实现：

```text
MinioFileStorageService
LocalFileStorageService
S3FileStorageService
```

禁止同时存在职责完全相同的：

```text
OssService
MinioService
FileStorageService
```

如果存在，则必须明确：

```text
FileStorageService = 抽象接口

MinioFileStorageService = MinIO 实现
```

---

## 11. Security 模块规则

Security 只负责：

```text
认证
授权
Token
权限校验
验证码
签名
防重放
密码安全
加解密
```

推荐：

```text
security/
├── config/
├── filter/
├── handler/
├── permission/
├── captcha/
└── crypto/
```

`UserContext` 负责保存当前登录用户上下文。

`PermissionChecker` 负责权限判断。

两者职责不能混合。

---

## 12. AI 模块内部规范

AI 模块必须尽量避免“大 Service”。

禁止：

```text
AiService.java
```

一个文件承担：

```text
对话
RAG
Prompt
Embedding
Agent
知识检索
模型调用
```

应该拆分：

```text
ChatService
RagService
AgentService
EmbeddingService
RerankService
PromptService
```

外部模型调用统一放：

```text
integration/
├── llm/
├── embedding/
├── vector/
├── rerank/
└── ocr/
```

禁止把 DeepSeek、OpenAI 等 SDK 调用直接写进 Controller。

---

## 13. Prompt 不允许散落

AI Prompt 必须统一管理。

禁止在 Service 中大量硬编码：

```java
String prompt = "你是一个...";
```

统一放：

```text
prompt/
├── system/
├── teaching/
├── rag/
├── grading/
├── question/
└── agent/
```

---

## 14. Common 模块禁止变成垃圾桶

`edu-mind-common` 只允许放真正全局共享内容：

```text
统一响应
异常
基础枚举
基础常量
通用工具
通用校验
基础上下文模型
```

禁止把具体业务代码放到 Common。

例如以下内容禁止放 Common：

```text
CourseUtil
KnowledgeService
AiPromptUtil
UserBusinessHelper
ExamHelper
```

---

## 15. Infrastructure 只负责技术实现

Infrastructure 负责：

```text
MyBatis
Redis
MinIO
MQ
搜索引擎
向量数据库
WebSocket
第三方 SDK
```

禁止在 Infrastructure 中写核心业务逻辑。

判断标准：

如果一段代码在以后从 MySQL 换 PostgreSQL、MinIO 换 S3、Milvus 换 pgvector 后仍然属于业务规则，那么它不应该放 Infrastructure。

---

## 16. 文件和类规模限制

默认遵守：

```text
单 Java 文件尽量 ≤ 600 行
单方法尽量 ≤ 50 行
超过 80 行的方法必须考虑拆分
```

禁止创建：

```text
CommonUtils.java
BusinessUtils.java
AIUtils.java
SystemHelper.java
```

这种超级工具类。

一个类只承担一个主要职责。

---

## 17. Service 规范

复杂业务 Service 使用：

```text
接口 + 实现
```

例如：

```text
UserService
UserServiceImpl
```

简单且没有替换需求的内部服务允许直接使用具体类，不为了形式机械创建接口。

Service 中：

- 可以编排业务
- 可以调用 DAO
- 可以调用其他模块 API
- 可以发布领域事件
- 可以调用基础设施抽象

禁止 Service 直接操作 HTTP Request/Response。

---

## 18. 命名规范

必须做到见名知意。

推荐：

```text
UserCreateDTO
UserUpdateDTO
UserQueryDTO

UserDetailVO
UserListVO

UserEntity

UserConverter

UserDao

UserMapper

UserService
UserServiceImpl
```

禁止大量：

```text
UserReq
UserResp
UserInfo
UserData
UserObj
UserBean
```

这种语义模糊命名。

---

## 19. 开发前必须检查重复职责

每次新增：

- Service
- DAO
- Util
- Manager
- Component
- API
- Integration

之前，必须先检查项目中是否已经有相同或相近职责。

如果已有类似实现：

优先：

```text
复用
扩展
抽象
重构
```

不要直接创建第二套。

特别检查：

```text
Knowledge vs AI Knowledge

RAG vs Retrieval

VectorSearchService vs Vector Integration

OssService vs FileStorageService

PermissionService vs PermissionChecker

UserContext vs LoginUser
```

---

## 20. 代码生成与修改时的强制自检清单 (Checklist)

以后生成或修改代码前，先分析：

```text
1. 代码属于哪个业务模块？
2. 应该放在哪一层？
3. 当前项目是否已有相同职责？
4. 是否会形成跨模块直接依赖？
5. 是否会形成循环依赖？
6. 是否错误使用 Entity 作为 DTO / VO？
7. 是否绕过 DAO 直接访问 Mapper？
```

如果发现当前需求会破坏以上架构：

不要直接按照错误方式实现。

应优先按照本规则调整为正确架构，再完成需求。

任何情况下：

**不要为了快速实现功能而破坏模块边界和分层规范。**

---

# 第二部分：前端项目目录架构强约束规范

你现在正在开发 **EduMind｜AI 智能教学赋能平台** 的 Vue3 前端项目。

本项目采用：

- Vue 3
- TypeScript
- Vite
- Pinia
- Vue Router
- Element Plus
- Axios
- SCSS
- ECharts

后续所有代码生成、修改、重构、新功能开发，都必须严格遵守以下前端目录架构规范。

---

# 一、最高优先级规则

以下规则属于项目强制架构规范，优先级高于普通代码生成习惯。

## 1. 禁止平铺目录

禁止将大量不同业务文件直接堆放在同一个目录中。

错误：

```text
views/
├── UserList.vue
├── RoleList.vue
├── CourseList.vue
├── QuestionList.vue
├── KnowledgeList.vue
├── ModelList.vue
└── PromptList.vue
```

正确：

```text
views/
├── course/
├── question/
├── knowledge/
├── system/
└── ai/
```

当某个目录文件逐渐增多，必须继续按照：

```text
业务领域
    ↓
业务模块
    ↓
具体页面 / 文件
```

进行拆分。

---

# 二、目录职责必须严格区分

项目目录的核心分层如下：

```text
src/
├── app/
├── assets/
├── layouts/
├── router/
├── views/
├── components/
├── features/
├── services/
├── api/
├── stores/
├── types/
├── composables/
├── core/
├── directives/
├── constants/
├── utils/
├── styles/
├── config/
└── main.ts
```

禁止混淆这些目录的职责。

---

# 三、app 目录

```text
app/
├── App.vue
├── bootstrap.ts
└── providers.ts
```

职责：

- 应用启动
- 全局插件初始化
- 全局 Provider 注册
- 应用级初始化逻辑

禁止：

- 放业务页面
- 放 HTTP API
- 放普通工具函数
- 放业务常量
- 放业务组件

特别注意：

禁止重新创建：

```text
app/constants.ts
```

全局常量统一放：

```text
constants/
```

---

# 四、views 页面层

`views/` 只负责：

> 路由级页面组件。

例如：

```text
views/
├── dashboard/
├── ai/
├── course/
├── knowledge/
├── question/
├── learning/
├── analytics/
├── system/
├── auth/
└── profile/
```

例如：

```text
views/course/
├── CourseList.vue
├── CourseCreate.vue
├── CourseDetail.vue
└── detail/
    ├── Overview.vue
    ├── Chapters.vue
    ├── KnowledgePoints.vue
    ├── Resources.vue
    ├── CourseAI.vue
    └── Members.vue
```

规则：

如果组件是一个完整路由页面，放 `views`。

如果只是页面内部复用 UI，不允许放 `views`，必须放 `components`。

---

# 五、components 组件层

`components/` 只负责：

> 可复用 UI 组件。

按照业务领域组织：

```text
components/
├── ai/
├── course/
├── knowledge/
├── question/
├── learning/
├── analytics/
├── system/
└── common/
```

例如：

```text
components/knowledge/
├── KnowledgeBaseCard.vue
├── DocumentTable.vue
├── DocumentUploader.vue
├── ChunkViewer.vue
├── RetrievalResult.vue
├── CitationList.vue
└── RAGDebugPanel.vue
```

规则：

业务组件：

```text
components/course/
components/knowledge/
components/ai/
```

真正跨业务公共组件：

```text
components/common/
```

禁止把明显属于某一个业务模块的组件全部丢进：

```text
components/common/
```

---

# 六、views 与 components 不属于重复

例如：

```text
views/learning/LearningPath.vue
```

和：

```text
components/learning/LearningPath.vue
```

如果前者是完整页面，后者是页面内部业务组件，则允许同时存在。

但是命名最好进一步区分，例如：

```text
views/learning/LearningPath.vue

components/learning/
└── LearningPathPanel.vue
```

避免产生语义混乱。

同理：

```text
views/knowledge/detail/RAGDebug.vue
```

与：

```text
components/knowledge/RAGDebugPanel.vue
```

属于正常的页面 + UI 组件关系。

---

# 七、features 业务能力层

`features/` 用于：

> 跨多个页面、包含完整业务逻辑的独立业务能力。

例如：

```text
features/
├── rag/
│   ├── retrieval/
│   ├── citation/
│   └── rerank/
│
├── agent/
│   ├── planning/
│   ├── tool-calling/
│   └── execution/
│
└── recommendation/
    ├── learning/
    └── resource/
```

必须明确：

```text
views       = 页面
components  = UI
features    = 业务能力
```

禁止在 `features` 中重新实现已经存在于 `components` 中的 UI。

例如：

```text
components/ai/Agent/AgentPlan.vue
```

负责 Agent Plan 的 UI 展示。

而：

```text
features/agent/planning/
```

负责 Agent Planning 的业务逻辑。

二者不能重复实现同一个职责。

---

# 八、api 接口层

`api/` 只负责：

> 与后端 HTTP API 通信。

必须按照后端业务领域拆分：

```text
api/
├── auth/
├── dashboard/
├── ai/
├── course/
├── knowledge/
├── question/
├── learning/
├── analytics/
└── system/
```

例如：

```text
api/course/
├── course.ts
├── chapter.ts
├── knowledge-point.ts
└── resource.ts
```

API 文件只能负责：

- 请求地址
- 请求方法
- 请求参数
- 返回值类型

禁止：

- 写复杂页面逻辑
- 写大量业务编排
- 操作 DOM
- 管理页面状态

---

# 九、services 业务服务层

复杂业务统一允许增加：

```text
services/
```

例如：

```text
services/
├── ai/
├── rag/
├── course/
├── learning/
└── analytics/
```

职责：

> 对多个 API 进行业务编排。

例如：

AI 对话可能涉及：

```text
创建会话
→ 发送消息
→ SSE
→ 引用解析
→ 会话状态更新
```

这种复杂逻辑应该：

```text
views
↓
composables
↓
services
↓
api
```

简单 CRUD 不需要强制经过 services：

```text
views
↓
api
```

即可。

禁止为了“形式完整”而创建没有实际业务逻辑的空 Service。

---

# 十、composables

`composables/` 负责：

> Vue Composition API 可复用逻辑。

例如：

```text
composables/
├── common/
├── ai/
├── course/
├── knowledge/
└── learning/
```

例如：

```text
usePagination.ts
useTable.ts
useDialog.ts

useAIChat.ts
useSSE.ts
useAIStream.ts

useDocumentUpload.ts
useRAG.ts
```

禁止：

- 把纯工具函数放 composables
- 把 HTTP API 定义放 composables
- 把页面组件放 composables

如果完全不依赖 Vue 响应式能力，应放：

```text
utils/
```

而不是 composables。

---

# 十一、stores

Pinia 只保存：

> 真正需要跨页面共享或者长期保存的全局状态。

例如：

```text
stores/
├── app/
├── auth/
├── user/
├── ai/
├── course/
├── knowledge/
└── learning/
```

适合进入 Store：

- 当前用户
- Token
- 权限
- Layout 状态
- Tabs 状态
- 当前 AI 会话
- 全局课程上下文

不适合进入 Store：

- 普通列表查询结果
- 单个 Dialog 状态
- 某页面 Loading
- 一次性表单状态
- 局部分页

禁止把 Pinia 当作所有页面状态的存储容器。

---

# 十二、types 类型层

所有 TypeScript 数据结构必须统一进入：

```text
types/
```

按领域拆分：

```text
types/
├── common/
├── auth/
├── ai/
├── course/
├── knowledge/
├── question/
├── learning/
└── analytics/
```

前后端接口返回对象尽量与 Java DTO / VO 保持语义对应。

例如：

后端：

```text
CourseCreateDTO
CourseVO
```

前端：

```text
CourseCreateRequest
CourseVO
```

禁止：

在每个 Vue 页面里重复声明大型 Interface。

---

# 十三、core 基础设施层

```text
core/
├── http/
├── auth/
├── websocket/
├── sse/
└── storage/
```

负责与业务无关的底层能力。

例如：

```text
core/http/
├── axios.ts
├── request.ts
├── interceptors.ts
└── error-handler.ts
```

禁止在：

```text
views
components
api
```

重复创建 Axios 实例。

项目必须保持唯一统一 HTTP 基础设施入口。

---

# 十四、utils 工具层

`utils/` 只允许存放：

> 无业务状态、无 Vue 状态、可独立调用的纯工具函数。

例如：

```text
utils/
├── format/
├── validation/
├── crypto/
├── download/
└── object/
```

禁止：

```text
utils/user.ts
utils/course.ts
utils/knowledge.ts
```

里面塞大量业务逻辑。

业务逻辑应该进入：

```text
services
features
composables
```

---

# 十五、constants

所有项目常量统一：

```text
constants/
├── app.ts
├── auth.ts
├── ai.ts
├── course.ts
├── question.ts
├── learning.ts
└── index.ts
```

禁止在以下目录重复维护相同常量：

```text
app/
utils/
views/
components/
```

禁止魔法字符串大量散落。

---

# 十六、目录新增规则

当开发新功能时，必须先判断它属于哪个业务领域。

例如新增：

“知识库文档向量化状态页面”

首先判断：

```text
领域：knowledge
模块：document
能力：embedding
```

然后分别进入适当目录：

```text
views/knowledge/
components/knowledge/
api/knowledge/
types/knowledge/
composables/knowledge/
```

不要为了一个功能随意创建新的顶级目录。

---

# 十七、禁止重复目录

在创建任何新文件之前，必须先检查是否已经存在：

```text
相同业务
相同组件
相同 API
相同 Type
相同 Composable
相同工具函数
相同常量
```

如果已经存在，应优先：

```text
复用
扩展
重构
```

而不是重新创建一套。

禁止出现：

```text
utils/request.ts
core/http/request.ts
api/request.ts
```

三套 HTTP 请求工具。

禁止出现：

```text
components/common/CourseCard.vue
components/course/CourseCard.vue
```

两个职责相同的组件。

禁止出现：

```text
types/course.ts
types/course/course.ts
```

两套课程类型。

---

# 十八、禁止随意增加顶级目录

未经明确必要性判断，不允许在 `src/` 下新增：

```text
helpers/
hooks/
libs/
models/
modules/
common/
shared/
services2/
business/
```

因为现有目录已经有对应职责：

```text
helpers       → utils
hooks         → composables
models        → types
shared UI     → components/common
business      → features/services
```

如果确实必须增加新的顶级目录，先说明：

1. 为什么现有目录无法承载
2. 新目录具体职责
3. 是否会与现有目录重复

否则不得创建。

---

# 十九、文件命名规则

Vue 组件：

```text
PascalCase.vue
```

例如：

```text
CourseCard.vue
KnowledgeBaseCard.vue
AIMessage.vue
```

Composable：

```text
useXxx.ts
```

例如：

```text
useAIChat.ts
useCourse.ts
```

普通 TS 文件：

```text
kebab-case.ts
```

例如：

```text
knowledge-base.ts
knowledge-point.ts
wrong-question.ts
```

Store：

按照现有模块规则统一命名，不随意变化。

---

# 二十、修改项目代码前必须执行的检查

每次准备实现功能前，必须先进行以下判断：

```text
1. 当前功能属于哪个业务领域？
2. 是否已有对应 view？
3. 是否已有对应 component？
4. 是否已有对应 API？
5. 是否已有对应 type？
6. 是否已有对应 composable？
7. 是否已有对应 service / feature？
8. 是否存在可复用实现？
9. 是否会产生重复目录或重复文件？
10. 是否违反目录职责？
```

确认之后再开始编码。

---

# 二十一、重构要求

如果发现当前代码已经违反目录规范：

不要为了完成当前任务继续扩大错误结构。

应该优先：

```text
识别问题
→ 调整目录
→ 修改 import
→ 保证功能正常
→ 再继续开发
```

但是不要无意义地进行大规模重构。

只重构当前功能涉及到的明显架构问题。

---

# 二十二、最终代码组织原则

整个 EduMind 前端必须长期保持：

```text
页面
 ↓
views
 ↓
components
 ↓
composables / features
 ↓
services
 ↓
api
 ↓
core/http
 ↓
Java Backend
```

其中：

```text
views       = 页面
components  = UI
composables = Vue 可复用逻辑
features    = 完整业务能力
services    = 业务编排
api         = 后端接口定义
stores      = 全局状态
types       = 数据类型
core        = 基础设施
utils       = 纯工具
constants   = 常量
```

任何代码必须首先找到自己所属的职责层，禁止随意放置。

---

# 二十三、UI 图标与 Emoji 规范

EduMind **产品界面**与前后端**用户可见文案**中，**禁止使用 Unicode Emoji 字符**作为按钮、菜单、状态、空状态、标签、提示或装饰图形。

## 必须使用的替代方式

- **前端 UI**：使用 **Element Plus Icons**（`<el-icon>` + `@element-plus/icons-vue`），或项目内既有的 **SVG 图标**（如 `btn-icon-svg`、组件内 inline SVG、`DocumentFileIcon` 等领域图标）。
- **文件类型 / 业务状态**：使用专用图标组件或统一视觉（渐变底 + 矢量图标），**不得**用 Emoji，**不得**用纯文字徽标冒充品牌图标（例如用「WORD」「PDF」大字块代替图标）。
- **菜单与权限**：与 `IconPicker`、侧边栏菜单一致，使用已注册的 icon 组件名，不得写死 Emoji。

## 禁止场景

```text
按钮、标题、Tab、面包屑前缀中的 Emoji
空状态用 Emoji 代替插画或图标组件
表格 / 卡片状态用 Emoji 表示成功、失败、警告
用户可见的 placeholder、toast、弹窗正文、表单 label 中堆砌 Emoji
在 CSS content 中用 Emoji 充当图标
```

## 代码与 AI 产出

生成或修改 **Vue 模板**、**组件**、**会展示在系统内的文案** 时，必须遵守本规则。架构说明文档若描述 UI 实现，应推荐 icon 方案而非 Emoji。

## 自检

```text
□ 新增 / 修改的模板与组件中无裸 Emoji 字符
□ 状态与操作可用 el-icon 或项目 SVG 表达
□ 与课程中心、操作日志等模块的胶囊按钮 + 图标风格一致
```

---

# 二十四、分层迁移铁律（纯迁移，禁止改 UI / 功能）

> **优先级：高于一切「顺手优化」**。将 `views` 拆分到 `components` / `composables` 时，默认任务是**搬迁**，不是**重做**。

## 1. 唯一目标

分层迁移只允许改变**代码所在目录与 import 路径**，不允许改变用户可见结果：

- DOM 结构（标签层级、class 名、`data-*`、aria）
- 样式（scoped SCSS 规则、变量、间距、颜色、响应式断点）
- 交互（按钮、表单、弹窗、路由跳转、校验提示、loading / 空状态）
- 业务行为（API 入参、调用时机、成功 / 失败处理、权限判断）

**迁移前后页面必须像素级、行为级一致。**

## 2. 强制搬迁方式（按顺序执行）

```text
1. 从原 views 文件复制 template → 目标 components（先整段复制，再按模块切分）
2. 从原 views 文件复制 <style scoped> → 对应 components（禁止重写 class 名）
3. 从原 views 文件复制 script 业务逻辑 → composables（只改 props / emit / 依赖注入方式）
4. views 保留为薄路由壳（布局编排 + 子组件引用，通常 < 150 行）
5. 对比迁移前后：template 行 diff、style 规则数、事件绑定数量必须一致或仅因拆分而等价
```

## 3. 绝对禁止（迁移期间）

- 禁止把自定义 UI 换成 Element Plus 默认样式（例如 `el-tag` 替代原有 `code-badge` / `category-pill`）
- 禁止删除未使用的函数 / 按钮 / 注释块（先保留，确认 template 无引用后再删）
- 禁止重命名 CSS class（除非原 class 随组件拆分且**子组件内保持同名**）
- 禁止「顺便」调整布局比例、配色、文案、图标
- 禁止在迁移同时做性能优化、接口改造、交互改版
- 禁止 `views` 互相 import（复用 UI 必须进 `components/`）

## 4. 迁移完成自检（必须全部通过）

```text
□ 原页面每个 @click / @submit / v-model 在新结构中仍有等价绑定
□ 原页面每个 class 名在拆分后仍能命中样式（注意 scoped 与 :deep）
□ 原页面 API 调用次数、参数、触发时机未变
□ 浏览器并排对比截图无肉眼可见差异（或 diff 工具 0 布局偏移）
□ npm run build 通过；相关 composable 单测通过
□ rg "from '@/views/" frontend/src 结果为 0
```

## 5. 允许的唯一例外

- 修复原页面已存在的 bug（须在 PR / 提交说明中**单独注明**，不得混入迁移提交）
- 提取 composable 时将纯函数导出以便单测（不改变运行时行为）

---

# 二十五、执行与遵守要求

以后当我要求你：

- 新增功能
- 新增页面
- 修改功能
- 重构页面
- 对接 API
- 新增组件
- 修复 Bug

你必须自动遵守本规则。

特别是：

**不要因为实现方便而创建重复文件。**

**不要因为目录不存在就随意新建顶级目录。**

**不要把所有代码写进一个 Vue 页面。**

**不要把不同领域代码平铺。**

**不要重复创建已有 API、Type、Composable、Store、组件和工具类。**

**不要用 Emoji 充当 UI 图标；必须使用 Element Plus Icons 或项目内 SVG / 图标组件。**

在新增文件前，优先查看现有项目结构并选择正确目录。

如果现有代码和本规范发生冲突：

> 新增代码优先遵守本规范，并在必要范围内逐步整理旧代码。

除非我明确要求，否则不要修改本目录架构规范。

