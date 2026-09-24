/**
 * Prompt 提示词中心 ·「一键填入真实课程 RAG 示例」统一数据源。
 *
 * 历史问题：演示数据曾经以 `10001 / Java程序设计 / CH03 / KP003 / Q10001` 之类的
 * 假 ID 硬编码在 `usePromptEditor` 与 `usePromptList` 两处，随着 sql 种子数据演进
 * （课程已改为 101 数据结构与算法 / 102 Java面向对象程序设计 / 103 高等数学（上））全部失效，
 * 演示出来的「真实课程示例」既查不到课程，也无法命中题库与知识库。
 *
 * 现在统一收敛到本文件：
 * 1. `PROMPT_DEMO_SNAPSHOT` 与 sql/migration/R__seed_data.sql 的课程种子逐字段对齐（课程 102）；
 * 2. 运行时优先通过 `usePromptDemoData` 拉取真实课程 / 章节 / 知识点 / 知识库切片，
 *    仅在接口不可用（Mock 模式、无权限、后端未启动）时回落本快照；
 * 3. 场景类变量（角色、题型、教学条件等）集中在 `PROMPT_DEMO_SCENE_VARIABLES`，避免再次散落。
 */

/** 演示用课程快照：与当前种子数据一一对应，任何字段都必须是库里真实存在的 ID 或文本 */
export interface PromptDemoSnapshot {
  /** course.id */
  courseId: number;
  /** course.title */
  courseName: string;
  /** course.description */
  courseDescription: string;
  /** course_chapter.id */
  chapterId: number;
  /** course_chapter.title */
  chapterName: string;
  /** course_knowledge_point.id 列表（逗号分隔） */
  knowledgePointIds: string;
  /** course_knowledge_point.title 列表（逗号分隔，与 ID 顺序一致） */
  knowledgePointNames: string;
  /** knowledge_base.name */
  knowledgeBaseName: string;
  /** knowledge_document.file_name */
  documentName: string;
  /** knowledge_document_text.content（真实切片正文） */
  sourceText: string;
  /** edu_question.id */
  questionId: number;
  /** edu_question.type */
  questionType: string;
  /** edu_question.stem（含选项，供批改模板单插槽注入） */
  questionStem: string;
  /** edu_question.answer */
  questionAnswer: string;
  /** edu_question.analysis */
  questionAnalysis: string;
  /** 演示用分步采分点（严格对齐该题参考要点，分值合计等于 questionScore） */
  questionScoringPoints: string;
  /** edu_question.score */
  questionScore: number;
  /** 同课程已有题干（命题去重用） */
  existingQuestions: string;
}

/**
 * 课程演示快照：课程 102「Java面向对象程序设计」。
 * 选它的原因：课程简介、章节（2.1 封装、继承与多态机制）、知识点（15 / 16）、
 * 知识库（Java面向对象程序设计知识库）、文档（Java面向对象编程实战教程.docx）
 * 与题库（题 1007）在种子数据里全部齐备，四类模板（问答 / 命题 / 批改 / 备课）都能落到真实数据上。
 */
export const PROMPT_DEMO_SNAPSHOT: PromptDemoSnapshot = {
  courseId: 102,
  courseName: 'Java面向对象程序设计',
  courseDescription: '面向对象高级特性、集合框架、多线程并发与企业级工程架构实战。',
  chapterId: 10,
  chapterName: '2.1 封装、继承与多态机制',
  knowledgePointIds: '15,16',
  knowledgePointNames: '面向对象三大特征与多态运行时绑定,ArrayList 与 LinkedList 源码剖析',
  knowledgeBaseName: 'Java面向对象程序设计知识库',
  documentName: 'Java面向对象编程实战教程.docx',
  sourceText:
    '【面向对象三大特性剖析】封装隐藏了对象的内部细节，对外提供安全受控的公共访问入口；继承实现了代码复用与类型扩展；多态使得统一接口可以根据运行时的实际对象类型呈现不同的行为。多态三大必要条件：继承、方法重写、父类引用指向子类对象。',
  questionId: 1007,
  questionType: 'SINGLE_CHOICE',
  questionStem:
    '在 Java 集合框架中，关于 ArrayList 与 LinkedList 的特性描述，正确的是（　）。\nA. ArrayList 底层是动态数组，随机访问时间复杂度为 O(1)\nB. LinkedList 支持基于下标的 O(1) 常数时间随机访问\nC. ArrayList 插入元素永远不需要复制数组\nD. LinkedList 占用内存比 ArrayList 更少',
  questionAnswer: 'A',
  questionAnalysis:
    'ArrayList 底层是 Object[] 数组，支持下标随机访问（O(1)）；LinkedList 为双向链表，按下标查找需要从头遍历（O(n)），且每个节点额外持有前后指针，内存开销更大。故正确答案为 A。',
  questionScore: 5,
  questionScoringPoints: JSON.stringify([
    {
      id: 'SP1',
      description: '指出 ArrayList 底层为动态数组，随机访问时间复杂度 O(1)',
      score: 2
    },
    {
      id: 'SP2',
      description: '指出 LinkedList 按下标访问需要遍历，时间复杂度 O(n)',
      score: 2
    },
    {
      id: 'SP3',
      description: '指出 LinkedList 每个节点额外持有前后指针，内存开销更大',
      score: 1
    }
  ]),
  existingQuestions: '["在 Java 集合框架中，关于 ArrayList 与 LinkedList 的特性描述，正确的是（　）。"]'
};

/**
 * 与课程数据无关的演示场景变量：角色、回答深度、题型、教学条件等。
 * 键名必须与 prompt_template.variables 中声明的插槽名完全一致。
 *
 * 注意：这里的文案刻意保持「课程中立」，避免在其他部署环境解析到非 Java 课程时
 * 出现「课程是数据结构、教学要求却在讲多态」这类错配；
 * 真正与课程强相关的 `question` / `lesson_title` 会由 `usePromptDemoData`
 * 依据解析到的真实课程与知识点覆写。
 */
export const PROMPT_DEMO_SCENE_VARIABLES: Record<string, string> = {
  // —— 通用问答（RAG / chat_rag）——
  user_role: 'STUDENT',
  answer_depth: 'NORMAL',
  language: 'zh-CN',
  allow_general_knowledge: 'true',
  question: '多态的实现需要哪些必要条件？运行时又是如何确定实际调用的方法？',
  conversation_history:
    '[User]: 接口和抽象类有什么区别？\n[Assistant]: 接口描述行为契约，抽象类抽取共性实现，二者都通过多态对外提供统一调用入口。',
  // —— 智能命题 / 试题向导 ——
  question_types: 'SINGLE_CHOICE,MULTIPLE_CHOICE,SHORT_ANSWER',
  question_type: '单选题',
  difficulty: 'MEDIUM',
  question_count: '3',
  count: '3',
  task_purpose: 'HOMEWORK',
  student_level: '本科二年级',
  score_per_question: '5',
  generation_requirements:
    'knowledgePointId 必须使用系统知识点 ID，不得写中文名；chapterName 必须与输入完全一致；干扰项需体现真实易错点',
  // —— 智能批改 ——
  grading_mode: 'FULL_EXPLANATION',
  scoring_rubric: '按采分点逐项判定，语义等价即可得分；选项判定错误时对应采分点不得分。',
  student_answer: 'C（ArrayList 插入元素永远不需要复制数组）',
  teacher_requirements: '结合真实案例讲解，重点澄清易混淆概念',
  // —— 教案备课 ——
  lesson_title: '面向对象三大特征与多态运行时绑定',
  lesson_duration: '90分钟',
  lesson_count: '2',
  class_profile: '已完成前置章节学习，对核心概念的边界与适用条件理解不深',
  teaching_mode: 'BOPPPS',
  teaching_method: '问题驱动,案例教学,代码演示,任务驱动',
  teaching_objectives: '能说明本知识点的核心要点，并结合真实案例解释其推导或运行过程',
  previous_learning: '前置章节核心知识',
  next_learning: '后续章节核心知识',
  available_resources: '课程教材,课程 PPT,实验或 IDE 环境',
  assessment_requirement: '设计概念辨析题与应用分析题作为形成性评价',
  homework_requirement: '布置 2 道概念辨析题与 1 道应用分析题',
  output_depth: 'DETAILED',
  teaching_hours: '2',
  target_students: '计算机专业大二学生'
};

/** 由课程快照或实时课程数据拼装出的「课程域」演示变量 */
export interface PromptDemoCourseVariables {
  courseId: string;
  courseName: string;
  courseDescription: string;
  chapterId: string;
  chapterName: string;
  knowledgePointIds: string;
  knowledgePointNames: string;
}

/** 单条 RAG 检索来源，用于渲染 `<rag_context>` 演示上下文 */
export interface PromptDemoRagSource {
  documentName: string;
  chapterName?: string;
  section?: string;
  page?: number;
  content: string;
}

/** 将快照转换为课程域变量（实时链路失败时的兜底值） */
export function buildPromptDemoCourseVariables(
  snapshot: PromptDemoSnapshot = PROMPT_DEMO_SNAPSHOT
): PromptDemoCourseVariables {
  return {
    courseId: String(snapshot.courseId),
    courseName: snapshot.courseName,
    courseDescription: snapshot.courseDescription,
    chapterId: String(snapshot.chapterId),
    chapterName: snapshot.chapterName,
    knowledgePointIds: snapshot.knowledgePointIds,
    knowledgePointNames: snapshot.knowledgePointNames
  };
}

/**
 * 渲染 RAG 检索上下文演示块。
 * 格式与后端 rag/context 注入的 `<rag_context>` 保持一致，S1/S2 才能被模板的引用规则命中。
 */
export function buildPromptDemoRagContext(sources: PromptDemoRagSource[]): string {
  const valid = sources.filter((item) => item.documentName && item.content);
  if (!valid.length) return '';
  const blocks = valid.map((item, index) => {
    const attrs = [`document="${item.documentName}"`];
    if (item.chapterName) attrs.push(`chapter="${item.chapterName}"`);
    if (item.section) attrs.push(`section="${item.section}"`);
    if (item.page) attrs.push(`page="${item.page}"`);
    return `<source id="S${index + 1}" ${attrs.join(' ')}>\n${item.content}\n</source>`;
  });
  return `<rag_context>\n${blocks.join('\n')}\n</rag_context>`;
}

/** 快照自带的 RAG 上下文（真实知识库文档名 + 真实切片正文） */
export function buildPromptDemoSnapshotRagContext(
  snapshot: PromptDemoSnapshot = PROMPT_DEMO_SNAPSHOT
): string {
  return buildPromptDemoRagContext([
    {
      documentName: snapshot.documentName,
      chapterName: snapshot.chapterName,
      content: snapshot.sourceText
    }
  ]);
}
