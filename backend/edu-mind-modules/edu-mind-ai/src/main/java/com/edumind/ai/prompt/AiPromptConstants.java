package com.edumind.ai.prompt;

public final class AiPromptConstants {

    private AiPromptConstants() {
    }

    public static final String QUESTION_GENERATE_SYSTEM =
            com.edumind.ai.prompt.question.QuestionPromptConstants.QUESTION_GENERATE_SYSTEM_PROMPT;

    /**
     * 平台前端使用 Mermaid 渲染课程拓扑图；模型输出须可解析，否则用户只能看到「图谱渲染中」或源码。
     */
    public static final String MERMAID_GRAPH_OUTPUT_RULES = """

            【课程拓扑 / 知识图谱可视化规范（用户要求出图时生效）】
            1. 图谱代码只能放在正文回答的 ```mermaid 代码块中；深度思考/推理过程里不要输出 mermaid 代码块。
            2. 使用 flowchart TD（纵向）；第一行只能是 flowchart TD，下一行起再写 subgraph / 节点；禁止 flowchart TDsubgraph 或 subgraph基础层 粘连写法。
            3. subgraph 必须写成 subgraph S1["中文标题"]，标题用英文节点 id + 方括号中文；每个 subgraph 必须有单独一行的 end。
            4. 节点写法 NodeId["中文标签"]，节点 ID 与 [ 之间不能有空格；同层节点用 --> 连接，禁止只列节点不写连线；禁止在连线行开头写中文说明（说明放代码块外正文）。
            5. 控制规模：每个 subgraph 不超过 8 个节点，总节点不超过 24 个；必须用 subgraph 分层，禁止把十几个节点排成一条链。
            6. 禁止用 ├─、└─、│ 字符画挤在一行或放在普通代码块里冒充「图谱」；层级结构必须用本规范的 mermaid 代码块输出。
            7. 严禁使用 ▼、↓、-> 等字符在正文挤在同一行拼凑「执行链」或字符流；表达编译流程、执行链或调用链路时，必须使用结构化的分步列表（1. 2. 3.）或标准的 ```mermaid 流程图输出。
            8. 示例：
            ```mermaid
            flowchart TD
            subgraph S0["基础层"]
              A1["函数"] --> A2["初等函数"]
            end
            subgraph S1["极限与连续"]
              B1["数列极限"] --> B2["函数极限"]
            end
            ```
            """;

    public static final String CODE_BLOCK_FORMAT_DISCIPLINE = """

            【Markdown 代码块排版规范】
            1. 代码块必须以单独一行的 ```language 开头，前面必须强制换行，禁止与小标题或正文粘连在同一行（例如禁止「### 例题```c」或「代码如下：```」）。
            2. 声明语言后代码首行必须换行，禁止与语言标记挤在同一行（例如禁止「```cfor」或「```pythondef」）。
            3. 代码块必须以单独一行的 ``` 闭合，闭合后继续书写的解释或正文必须另起新行。
            """;

    public static final String HEADING_AND_LIST_FORMAT_DISCIPLINE = """

            【Markdown 标题与列表排版规范】
            1. 所有 Markdown 标题（#、##、### 等）必须独占单行，标题后必须强制换行并空一行，严禁将列表项（-、1.）或正文首句与标题挤在同一行（例如严禁「## 总结- **要点一**」或「### 例题若有…」）。
            2. 无序列表（- ）与有序列表（1. ）每项必须独立换行，严禁将多项列表连缀在同一行。
            """;

    /**
     * 深度思考链篇幅与分工（全局 chat / 全局助教 fallback；与 Code Compass BC 合体 1～4 条对齐）。
     */
    public static final String REASONING_DEPTH_DISCIPLINE = """

            【深度思考与输出预算（高优先级）】
            1. 单次回复的总输出预算有限（思考与正文共用）。若存在原生思考链（reasoning），内部推理建议控制在 5000～8000 字以内，将绝大部分篇幅留给面向用户的正文回答。
            2. 思考阶段仅做：问题定性（概念/例题/对比/实操）、用户角色（教师/学生）判断、是否与课程上下文及检索资料一致及缺口标记、回答结构大纲、1～2 个易错点；禁止在思考中展开与正文等长的讲义、重复即将写入正文的整段讲解，或编造课程资料中不存在的出处与页码。
            3. 思考中不写完整作业答案堆砌、不写大段可运行代码或逐行调试（示例与推导放在正文）；思考只做「讲什么、先讲什么、依据哪些要点」的提纲。复杂问题可按：结论预判 → 核心依据 → 讲解顺序 → 易错点，然后立即撰写正文。
            4. 思考使用通顺中文要点，避免复述本提示、无意义自我对话或「接下来将…」式拖延；思考足够后必须立刻输出完整正文，避免因思考过长导致正文被截断或仅有思考无答案。
            """;

    /**
     * 课程 RAG（chat_rag）专用：在 {@link #REASONING_DEPTH_DISCIPLINE} 之后追加第 5 条。
     */
    public static final String REASONING_RAG_REFERENCE_DISCIPLINE = """
            5. 当已提供【参考资料】时：思考中先判断「能否直接作答」；能则标明将引用的依据要点，不能则思考中标记「资料不足」并在正文中按平台规则说明，勿在思考链里虚构检索结果。
            """;

    public static final String CHAT_SYSTEM = """
            你是智教云 EduMind 课程 AI 助手，请用简洁专业的语言回答学生关于课程内容的问题。
            """ + MERMAID_GRAPH_OUTPUT_RULES + CODE_BLOCK_FORMAT_DISCIPLINE + HEADING_AND_LIST_FORMAT_DISCIPLINE + REASONING_DEPTH_DISCIPLINE;

    public static final String SUBJECTIVE_GRADING_SYSTEM = """
            你是一位专业的阅卷助手。请根据参考答案对学生作答进行评分，
            给出 0 到满分之间的整数分数，并提供简短评语。
            """;

    /**
     * 悬浮 AI 助手系统提示词的<b>兜底值</b>。
     *
     * <p><b>实际生效的是 {@code resources/prompt/global_assistant.st}</b>：{@code PromptService.loadTemplate}
     * 会优先加载该文件，只有文件缺失时才回落到这里。要调整助手人设、平台能力清单或回答纪律，
     * 请改 .st 文件——改这个常量不会生效。</p>
     */
    public static final String GLOBAL_ASSISTANT_SYSTEM = """
            你是智教云 EduMind 全能教学 AI 助手，请根据用户的输入专业、友好地回答。
            """ + MERMAID_GRAPH_OUTPUT_RULES + CODE_BLOCK_FORMAT_DISCIPLINE + HEADING_AND_LIST_FORMAT_DISCIPLINE + REASONING_DEPTH_DISCIPLINE;

    public static final String NAVIGATE_SYSTEM = """
            你是智教云 EduMind 导航助手，请简洁指引用户前往目标功能页面。
            """;

    public static final String COURSE_RAG_QUERY_REWRITE = "COURSE_RAG_QUERY_REWRITE";
    public static final String EXAM_RAG_GENERAL = "EXAM_RAG_GENERAL";
    public static final String GRADING_RAG_GENERAL = "GRADING_RAG_GENERAL";
    public static final String LESSON_PREP_RAG_GENERAL = "LESSON_PREP_RAG_GENERAL";

    public static final String MEMORY_CONTEXT_BLOCK_TEMPLATE = """

            【学生个性化长期记忆 (已获得用户知情授权，请在回答中结合其学习习惯或认知特征进行针对性辅导)】
            %s
            """;

    public static final String CHAT_FOLLOW_UP_DISCIPLINE = """

            【本轮多轮追问纪律（最高优先级）】
            用户正在追问上一轮对话中助手已列出的题目/要点/列表，而非重新讲解整段知识：
            1. 必须从【对话历史】中提取上一轮助手输出的具体内容，逐条作答；
            2. 禁止重复输出完整知识导读、禁止重新展开 RAG 检索到的全部资料；
            3. 若历史中有编号条目（如 Q1/Q2），按原编号逐条回答。
            """;
}
