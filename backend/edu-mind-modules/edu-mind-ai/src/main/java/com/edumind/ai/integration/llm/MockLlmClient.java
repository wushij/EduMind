package com.edumind.ai.integration.llm;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

@RequiredArgsConstructor
public class MockLlmClient implements LlmClient {

    private final LlmProperties llmProperties;
    private final String modelKey;

    public MockLlmClient(LlmProperties llmProperties) {
        this(llmProperties, "mock");
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        if (isQueryRewriteCall(systemPrompt)) {
            return mockQueryRewrite(userPrompt);
        }
        if (isReactAgentCall(systemPrompt)) {
            return buildReactAction(userPrompt);
        }
        if (systemPrompt != null && systemPrompt.contains("总结助手")) {
            return "基于工具观察结果，已生成教学推进建议。";
        }
        if (systemPrompt != null && (systemPrompt.contains("教学目标") || systemPrompt.contains("\"objectives\""))) {
            return mockCourseObjectivesJson(userPrompt);
        }
        if (systemPrompt != null && systemPrompt.contains("课程简介与修读要求")) {
            return mockCourseDescription(userPrompt);
        }
        if (systemPrompt != null && (systemPrompt.contains("学情分析专家") || systemPrompt.contains("长期记忆特征") || systemPrompt.contains("认知特征"))) {
            return mockMemoryExtractionJson(userPrompt);
        }
        if (systemPrompt != null && systemPrompt.contains("课节内容生成助手")) {
            return mockLessonContentBlocksJson(systemPrompt);
        }
        return "[" + modelKey + "] 这是 Mock LLM 的回复。您的问题是：" + userPrompt;
    }

    @Override
    public String chat(String systemPrompt, String userPrompt, LlmChatOptions options) {
        return chat(systemPrompt, userPrompt);
    }

    private boolean isQueryRewriteCall(String systemPrompt) {
        return systemPrompt != null
                && (systemPrompt.contains("检索查询") || systemPrompt.contains("检索 Query"));
    }

    private String mockQueryRewrite(String userPrompt) {
        String course = extractLineValue(userPrompt, "当前课程：");
        String question = extractLineValue(userPrompt, "用户当前问题：");
        if (!org.springframework.util.StringUtils.hasText(question)) {
            question = extractLineValue(userPrompt, "用户问题：");
        }
        if (!org.springframework.util.StringUtils.hasText(question)) {
            question = userPrompt != null ? userPrompt.trim() : "";
        }
        question = question.replaceAll("\\s+", " ").trim();
        if (org.springframework.util.StringUtils.hasText(course) && !question.contains(course)) {
            return course + " " + question;
        }
        return question;
    }

    private String mockCourseObjectivesJson(String userPrompt) {
        String courseName = extractLineValue(userPrompt, "课程名称：");
        if (!org.springframework.util.StringUtils.hasText(courseName)) {
            courseName = "本课程";
        }
        List<Map<String, Object>> objectives = new ArrayList<>();
        objectives.add(Map.of(
                "title", "理解" + courseName + "核心概念",
                "description", "能够准确表述课程关键知识点及其相互关系。"));
        objectives.add(Map.of(
                "title", "完成章节实践任务",
                "description", "能够依据教学大纲完成实验、作业与阶段性测验。"));
        objectives.add(Map.of(
                "title", "应用知识解决实际问题",
                "description", "能够将所学方法用于案例分析或小规模项目实践。"));
        objectives.add(Map.of(
                "title", "提升自主学习能力",
                "description", "能够利用课程资源与 AI 助教进行预习、复习与拓展。"));
        return JSON.toJSONString(Map.of("objectives", objectives));
    }

    private String mockLessonContentBlocksJson(String systemPrompt) {
        String lessonTitle = extractLineValue(systemPrompt, "课节：");
        if (!org.springframework.util.StringUtils.hasText(lessonTitle)) {
            lessonTitle = "本课节";
        }
        String courseName = extractLineValue(systemPrompt, "课程：");
        if (!org.springframework.util.StringUtils.hasText(courseName)) {
            courseName = "本课程";
        }
        List<Map<String, Object>> blocks = new ArrayList<>();
        blocks.add(Map.of(
                "type", "callout",
                "variant", "objective",
                "title", "学习目标",
                "body", "- 能够说明「" + lessonTitle + "」的核心概念与术语\n"
                        + "- 能够结合「" + courseName + "」大纲完成本课基础练习"));
        blocks.add(Map.of(
                "type", "markdown",
                "body", "## " + lessonTitle + "\n\n"
                        + "本节围绕「" + lessonTitle + "」展开，建议先阅读导读，再按要点完成练习。\n\n"
                        + "### 核心要点\n\n"
                        + "1. 概念界定与常见误区\n"
                        + "2. 典型示例与课堂讨论\n"
                        + "3. 小结与课后拓展"));
        return JSON.toJSONString(Map.of("version", 1, "blocks", blocks));
    }

    private String mockCourseDescription(String userPrompt) {
        String courseName = extractLineValue(userPrompt, "课程名称：");
        if (!org.springframework.util.StringUtils.hasText(courseName)) {
            courseName = "本课程";
        }
        return "《" + courseName + "》面向相关专业学习者，强调理论与实践结合。"
                + "课程将围绕教学大纲系统讲解核心知识，并通过实验与作业巩固理解。"
                + "建议具备相应学科基础，按周完成预习与复盘；可结合知识库与 AI 助教进行拓展学习。";
    }

    private String extractLineValue(String text, String label) {
        if (text == null || label == null) {
            return "";
        }
        int idx = text.indexOf(label);
        if (idx < 0) {
            return "";
        }
        int start = idx + label.length();
        int end = text.indexOf('\n', start);
        return (end >= 0 ? text.substring(start, end) : text.substring(start)).trim();
    }

    private boolean isReactAgentCall(String systemPrompt) {
        return systemPrompt != null && systemPrompt.contains("ReAct Agent");
    }

    private String buildReactAction(String userPrompt) {
        String goalSection = extractGoalSection(userPrompt);
        String observations = extractObservationHistory(userPrompt);
        boolean questionFlow = goalSection.contains("单选题")
                || goalSection.contains("多选题")
                || (goalSection.contains("生成") && goalSection.contains("题"));

        Map<String, Object> action = new HashMap<>();
        if (questionFlow) {
            if (!hasObservation(observations, "search_knowledge_point")) {
                action.put("thought", "检索相关知识点");
                action.put("action", "search_knowledge_point");
                action.put("actionInput", Map.of("courseId", 102));
            } else if (!hasObservation(observations, "generate_question")) {
                action.put("thought", "生成题目");
                action.put("action", "generate_question");
                action.put("actionInput", Map.of("courseId", 102, "count", 5));
            } else {
                action.put("thought", "题目已生成");
                action.put("action", "finish");
                action.put("finalAnswer", "已根据知识点生成练习题目。");
            }
            return JSON.toJSONString(action);
        }

        if (!hasObservation(observations, "get_student_profile")) {
            action.put("thought", "先获取学生学情画像");
            action.put("action", "get_student_profile");
            action.put("actionInput", Map.of("studentId", 1001, "courseId", 102));
        } else if (!hasObservation(observations, "recommend_resource")) {
            action.put("thought", "基于弱项推荐巩固资源");
            action.put("action", "recommend_resource");
            action.put("actionInput", Map.of("courseId", 102, "limit", 3));
        } else {
            action.put("thought", "信息已足够，生成最终回答");
            action.put("action", "finish");
            action.put("finalAnswer", "已综合分析学情画像并推荐匹配资源，建议按推荐顺序完成巩固练习。");
        }
        return JSON.toJSONString(action);
    }

    /** 仅解析 ReAct 模板中的「历史观察」段，避免工具清单里的 tool name 误判为已执行。 */
    private String extractObservationHistory(String userPrompt) {
        if (userPrompt == null) {
            return "";
        }
        int marker = userPrompt.indexOf("历史观察：");
        return marker >= 0 ? userPrompt.substring(marker) : "";
    }

    private String extractGoalSection(String userPrompt) {
        if (userPrompt == null) {
            return "";
        }
        int marker = userPrompt.indexOf("任务目标：");
        return marker >= 0 ? userPrompt.substring(marker) : userPrompt;
    }

    private boolean hasObservation(String observations, String toolName) {
        return observations.contains("Observation[" + toolName + "]");
    }

    @Override
    public String generateQuestions(String prompt, Map<String, Object> params) {
        int count = params.get("count") instanceof Number n ? n.intValue() : 3;
        int score = params.get("scorePerQuestion") instanceof Number s ? s.intValue() : 5;
        List<Map<String, Object>> questions = new ArrayList<>();
        boolean isAi = prompt != null && (prompt.contains("AI") || prompt.contains("大模型") || prompt.contains("机器学习"));
        boolean isOs = prompt != null && (prompt.contains("操作系统") || prompt.contains("Linux") || prompt.contains("进程"));
        boolean isMath = prompt != null && (prompt.contains("数学") || prompt.contains("微积分") || prompt.contains("极限"));

        for (int i = 1; i <= count; i++) {
            Map<String, Object> question = new HashMap<>();
            question.put("type", "SINGLE_CHOICE");
            question.put("difficulty", 3);
            question.put("score", score);
            question.put("cognitiveLevel", "APPLY");

            if (isAi) {
                question.put("knowledgePointName", "大模型注意力与微调机制");
                question.put("stem", "在大语言模型推理与长文本外推中，关于 RoPE（旋转位置编码）的数学性质，下列描述正确的是：");
                question.put("options", "[{\"key\":\"A\",\"content\":\"RoPE 通过绝对位置向量直接相加注入位置信息\",\"isCorrect\":false},{\"key\":\"B\",\"content\":\"RoPE 利用复数旋转性质将相对位置关系转化为内积中的角度旋转\",\"isCorrect\":true},{\"key\":\"C\",\"content\":\"RoPE 完全破坏了注意力矩阵的对称性与可并行性\",\"isCorrect\":false},{\"key\":\"D\",\"content\":\"RoPE 只能应用于固定 512 长度文本，无法进行线性插值扩展\",\"isCorrect\":false}]");
                question.put("answer", "B");
                question.put("analysis", "【权威解析】RoPE 巧妙利用旋转矩阵的正交性，通过对二维子空间施加旋转角变换，使两向量内积结果直接蕴含相对位置差值 $m-n$，兼具绝对位置编码的实现简易性与相对位置编码的泛化外推能力。");
                question.put("distractorAnalysis", "选项 A 为传统绝对位置编码做法（如原始 Transformer 的正弦余弦相加）；选项 D 忽视了 RoPE 的 NTK 插值与线性外推扩展能力。");
            } else if (isOs) {
                question.put("knowledgePointName", "进程同步与死锁预防");
                question.put("stem", "在多线程高并发系统中，某临界资源仅允许互斥访问，关于哲学家就餐问题中死锁现象的防范，下列方案不可行的是：");
                question.put("options", "[{\"key\":\"A\",\"content\":\"对所有筷子进行全局编号，要求哲学家只能先申请小编号筷子再申请大编号筷子\",\"isCorrect\":false},{\"key\":\"B\",\"content\":\"至多允许 4 位哲学家同时尝试拿起左手边的筷子\",\"isCorrect\":false},{\"key\":\"C\",\"content\":\"要求哲学家必须同时拿起左右两只筷子，否则一只都不拿\",\"isCorrect\":false},{\"key\":\"D\",\"content\":\"无限增大线程数量与每位哲学家就餐思考的休眠时间\",\"isCorrect\":true}]");
                question.put("answer", "D");
                question.put("analysis", "【权威解析】选项 A 破坏了死锁的循环等待条件；选项 B 破坏了占有且等待并防止全员死锁；选项 C 采用原子化资源申请破坏了保持且等待条件。选项 D 无法消除死锁四个必要条件，反而加剧资源竞争概率。");
                question.put("distractorAnalysis", "选项 D 属于典型的掩耳盗铃式参数调优，并发线程数增加反而更容易导致并发冲突。");
            } else if (isMath) {
                question.put("knowledgePointName", "泰勒展开与极限定理");
                question.put("stem", "计算极限：$\\lim_{x \\to 0} \\frac{\\tan x - \\sin x}{x^3}$ 的值为：");
                question.put("options", "[{\"key\":\"A\",\"content\":\"$1/2$\",\"isCorrect\":true},{\"key\":\"B\",\"content\":\"$1$\",\"isCorrect\":false},{\"key\":\"C\",\"content\":\"$0$\",\"isCorrect\":false},{\"key\":\"D\",\"content\":\"$2$\",\"isCorrect\":false}]");
                question.put("answer", "A");
                question.put("analysis", "【权威解析】$\\tan x - \\sin x = \\tan x (1 - \\cos x)$。当 $x \\to 0$ 时，$\\tan x \\sim x$，$1 - \\cos x \\sim \\frac{1}{2}x^2$，故分子等价于 $x \\cdot \\frac{1}{2}x^2 = \\frac{1}{2}x^3$。代入原式求得极限为 $1/2$。");
                question.put("distractorAnalysis", "若粗心忽略 $1-\\cos x$ 前面的系数 $\\frac{1}{2}$ 则极易误选 B。");
            } else {
                question.put("knowledgePointName", "散列表冲突与探测算法");
                question.put("stem", "已知散列表长度为 11，采用除留余数法散列函数 $H(key) = key \\bmod 11$，若用二次探测法（平方法）处理冲突，增量序列为 $1^2, -1^2, 2^2, -2^2, \\dots$，关于其特性描述正确的是：");
                question.put("options", "[{\"key\":\"A\",\"content\":\"二次探测法能够彻底避免二次聚集（Secondary Clustering）\",\"isCorrect\":false},{\"key\":\"B\",\"content\":\"二次探测法有效缓解了线性探测法容易出现的堆积（一次聚集）现象\",\"isCorrect\":true},{\"key\":\"C\",\"content\":\"二次探测法只要表中有空位就一定能保证探测到所有表元\",\"isCorrect\":false},{\"key\":\"D\",\"content\":\"二次探测法每次探测步长固定为常数 1\",\"isCorrect\":false}]");
                question.put("answer", "B");
                question.put("analysis", "【权威解析】线性探测法由于步长固定为 1，容易形成相邻块连片聚簇（一次聚集）。二次探测法通过非线性递增序列分散了冲突地址，有效解决了连续堆积问题。");
                question.put("distractorAnalysis", "选项 C 错误，二次探测法只能探测到表中的一部分位置，不能遍历全部表项；选项 A 错误，双重散列才能更好抑制二次聚集。");
            }
            questions.add(question);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("questions", questions);
        return JSON.toJSONString(result);
    }

    @Override
    public void streamChat(String systemPrompt, String userPrompt, StreamCallback callback) {
        streamChatWithHistory(systemPrompt, List.of(LlmChatMessage.user(userPrompt)), callback);
    }

    @Override
    public void streamChatWithHistory(String systemPrompt, List<LlmChatMessage> messages, StreamCallback callback) {
        streamChatWithHistory(systemPrompt, messages, null, callback);
    }

    @Override
    public void streamChatWithHistory(String systemPrompt, List<LlmChatMessage> messages,
                                      BooleanSupplier cancelled, StreamCallback callback) {
        if (!Boolean.TRUE.equals(llmProperties.getMockEnabled())) {
            callback.onError("Mock LLM 未启用");
            return;
        }
        try {
            String[] reasoningSteps = {
                    "对齐课程知识库与教学大纲检索结果…",
                    "\n规划：要点梳理 + 代码示例 + 学习建议"
            };
            for (String step : reasoningSteps) {
                if (cancelled != null && cancelled.getAsBoolean()) {
                    callback.onComplete();
                    return;
                }
                callback.onReasoning(step);
                Thread.sleep(50);
            }
            callback.onStatus("composing", "思考已完成，正在撰写回答正文…");

            String reply = chatWithHistory(systemPrompt, messages);
            int chunkSize = 3;
            for (int i = 0; i < reply.length(); i += chunkSize) {
                if (cancelled != null && cancelled.getAsBoolean()) {
                    callback.onComplete();
                    return;
                }
                int end = Math.min(i + chunkSize, reply.length());
                callback.onChunk(reply.substring(i, end));
                Thread.sleep(28);
            }
            callback.onComplete();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            callback.onError("Mock 流式中断");
        }
    }

    private String mockMemoryExtractionJson(String userPrompt) {
        try {
            Thread.sleep(800 + (long) (Math.random() * 500));
        } catch (InterruptedException ignored) {}

        String course = extractLineValue(userPrompt, "课程空间：");
        if (!org.springframework.util.StringUtils.hasText(course)) {
            course = "本课程专业领域";
        }

        List<Map<String, Object>> items = new ArrayList<>();
        long now = System.currentTimeMillis();
        int variant = (int) (now % 3);
        if (variant == 0) {
            items.add(Map.of(
                "memoryType", "PREFERENCE",
                "sensitivityLevel", "NORMAL",
                "summary", "【思维图谱偏好】在" + course + "核心概念剖析中偏好先建立具象可视化脑图，再深入底层因果逻辑。",
                "reasoning", "AI 分析近多轮交互：学生反复要求助教提供知识网络脑图与模块关系图，对碎片化知识点记忆表现出认知阻力。",
                "confidenceScore", 0.96
            ));
            items.add(Map.of(
                "memoryType", "PROFILE",
                "sensitivityLevel", "ACADEMIC",
                "summary", "【薄弱攻坚预警】对" + course + "核心边界条件与异常临界值推断存在定式思维，需强化反例辨析。",
                "reasoning", "AI 识别诊断：面对边界条件设陷题时连续 2 次直接跳步造成误判，已沉淀为专属考前易错警示点。",
                "confidenceScore", 0.93
            ));
        } else if (variant == 1) {
            items.add(Map.of(
                "memoryType", "FEEDBACK",
                "sensitivityLevel", "NORMAL",
                "summary", "【辅导节奏约定】要求助教在遇到复杂大题时采用苏格拉底递进式反问，优先提供方向线索而非直接给答案。",
                "reasoning", "学生在问答中主动强调：'不要直接把答案告诉我，请先提示第一步如何寻找突破口'，体现出强烈的自主探究导向。",
                "confidenceScore", 0.98
            ));
            items.add(Map.of(
                "memoryType", "PREFERENCE",
                "sensitivityLevel", "NORMAL",
                "summary", "【工程实战导向】倾向于结合企业级真实应用场景或高频经典真题来理解" + course + "关键原理。",
                "reasoning", "多次询问'该技术在生产环境中通常如何配置与调优'，表现出鲜明的工程落地与实战验证导向。",
                "confidenceScore", 0.95
            ));
        } else {
            items.add(Map.of(
                "memoryType", "PREFERENCE",
                "sensitivityLevel", "NORMAL",
                "summary", "【排版组织习惯】习惯在助教给出长篇解析前先浏览核心结论概要（Executive Summary），偏好总-分-总排版结构。",
                "reasoning", "学生提问时多次标注'先说核心结论和踩分点'，大局观统揽意图明确。",
                "confidenceScore", 0.97
            ));
            items.add(Map.of(
                "memoryType", "PROFILE",
                "sensitivityLevel", "ACADEMIC",
                "summary", "【严谨推演风格】在" + course + "多变量交叉综合题中展现出严谨的因果推演能力，步骤完整度极高。",
                "reasoning", "分析学生对话推演记录，其公式推导和逻辑因果链完整，具备良好的高阶分析素养。",
                "confidenceScore", 0.94
            ));
        }
        return JSON.toJSONString(items);
    }
}
