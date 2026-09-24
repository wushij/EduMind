import type { PromptTemplate } from '@/types/system/prompt';
import {
  PROMPT_DEMO_SCENE_VARIABLES,
  PROMPT_DEMO_SNAPSHOT,
  buildPromptDemoSnapshotRagContext
} from '@/constants/system/prompt-demo';

/**
 * Mock 模板默认参数统一取自 constants/system/prompt-demo，
 * 与「一键填入真实课程示例」共用同一份真实课程数据，避免再次出现
 * 10001 / CH03 / KP003 / Q10001 这类已失效的假 ID。
 */
const DEMO = PROMPT_DEMO_SNAPSHOT;
const DEMO_SCENE = PROMPT_DEMO_SCENE_VARIABLES;
const DEMO_RAG_CONTEXT = buildPromptDemoSnapshotRagContext();
const DEMO_REFERENCE_ANSWER = `${DEMO.questionAnswer}（${DEMO.questionAnalysis}）`;

export const mockPromptTemplates: PromptTemplate[] = [
  {
    id: 1,
    code: 'COURSE_RAG_GENERAL',
    name: '通用课程问答（RAG）',
    category: 'rag',
    description: '围绕当前课程、章节、知识点和课程知识库，为教师和学生提供具备资料溯源能力的专业智能问答。',
    systemPrompt: `你是 EduMind｜AI智能教学赋能平台中的“课程 AI 助手”。\n围绕当前课程 {{course_name}} 与检索上下文 {{retrieved_context}} 为用户提供精准、可溯源的解答。[S1][S2]`,
    userPromptTemplate: '请基于当前课程知识库回答下面的问题。\n\n当前课程：{{course_name}}\n当前章节：{{chapter_name}}\n当前知识点：{{knowledge_point_name}}\n\n用户问题：\n{{question}}',
    variables: [
      { name: 'course_name', label: '课程名称', defaultValue: DEMO.courseName },
      { name: 'chapter_name', label: '当前章节', defaultValue: DEMO.chapterName },
      { name: 'knowledge_point_name', label: '知识点', defaultValue: DEMO.knowledgePointNames.split(',')[0] },
      { name: 'question', label: '用户问题', defaultValue: DEMO_SCENE.question },
      { name: 'retrieved_context', label: 'RAG检索资料', defaultValue: DEMO_RAG_CONTEXT }
    ],
    version: 'v1.0',
    status: 'PUBLISHED',
    boundModel: 'deepseek-chat',
    temperature: 0.3,
    maxTokens: 8000,
    callCount: 128,
    createdAt: '',
    updatedAt: ''
  },
  {
    id: 2,
    code: 'chat_rag',
    name: '课程 AI-RAG 对话 (轻量版)',
    category: 'rag',
    description: '课程知识库 RAG 对话轻量级模板，快速注入资料上下文并返回解析。',
    systemPrompt: '你是 EduMind｜AI智能教学赋能平台中的“课程 AI 助教”（轻量级问答引擎）。\n你的核心职责是：围绕课程资料检索到的上下文参考内容，为师生提供专业、清晰、准确的课程知识问答服务。\n请遵守以下原则：\n1. 优先依据提供的【参考资料】进行解答，确保结论严谨可溯源；\n2. 回答应条理清晰，重点突出；\n3. 若【参考资料】不足以支撑回答，应明确说明并提示联系任课教师或查阅课程教材。',
    userPromptTemplate: '请结合以下课程参考资料，回答用户的学习问题。\n\n【参考资料】\n{{context}}\n\n【用户问题】\n{{question}}',
    variables: [
      { name: 'context', label: '检索上下文', defaultValue: '' },
      { name: 'question', label: '用户问题', defaultValue: '' }
    ],
    version: 'v1.0',
    status: 'PUBLISHED',
    boundModel: 'deepseek-chat',
    temperature: 0.3,
    maxTokens: 8000,
    callCount: 42,
    createdAt: '',
    updatedAt: ''
  },
  {
    id: 3,
    code: 'QUESTION_GENERATE',
    name: '智能试题向导生成',
    category: 'question',
    description: '依据课程大纲、指定知识点与布鲁姆认知层级，全自动构建客观单选、多选与主观简答试题。',
    systemPrompt: '你是一位专业的教学出题助手。请根据课程知识点生成结构化试题，输出 JSON 格式。',
    userPromptTemplate: '请为课程【{{course_name}}】的知识点【{{knowledge_point}}】生成 {{count}} 道难度为【{{difficulty}}】的【{{question_type}}】试题。',
    variables: [
      { name: 'course_name', label: '课程名称', defaultValue: '数据结构与算法' },
      { name: 'knowledge_point', label: '知识点', defaultValue: '单链表就地逆置算法' },
      { name: 'question_type', label: '题型', defaultValue: DEMO_SCENE.question_type },
      { name: 'difficulty', label: '难度', defaultValue: DEMO_SCENE.difficulty },
      { name: 'count', label: '题目数量', defaultValue: DEMO_SCENE.count }
    ],
    version: 'v1.0',
    status: 'PUBLISHED',
    boundModel: 'deepseek-chat',
    temperature: 0.7,
    maxTokens: 2500,
    callCount: 86,
    createdAt: '',
    updatedAt: ''
  },
  {
    id: 6,
    code: 'EXAM_RAG_GENERAL',
    name: '智能命题（RAG）',
    category: 'question',
    description: '基于指定课程、章节、知识点与 RAG 课程知识库，高质量生成具备资料溯源、答案唯一可信、解析完备的单选/多选/判断/简答等教学试题。',
    systemPrompt: `你是 EduMind｜AI智能教学赋能平台中的“智能命题专家”。\n你的核心职责是：基于当前课程、章节、知识点以及系统提供的 RAG 课程资料，按照教师给定的题型、数量、难度和教学目标，生成准确、规范、可作答、可评分、可追溯的高质量教学题目。\n严格遵守 RAG Grounding 原则，保证题目、答案、解析均有课程资料依据，杜绝幻觉与答案泄露，并以标准 JSON Schema 格式输出。`,
    userPromptTemplate: '请根据当前课程知识库完成本次智能命题任务。\n\n课程：\n{{course_name}} (ID: {{course_id}})\n\n章节：\n{{chapter_name}}\n\n目标知识点：\n{{knowledge_point_names}}\n\n题型：\n{{question_types}}\n\n题目数量：\n{{question_count}}\n\n难度：\n{{difficulty}}\n\n使用场景：\n{{task_purpose}}\n\n学生水平：\n{{student_level}}\n\n每题建议分值：\n{{score_per_question}}\n\n教师额外要求：\n{{generation_requirements}}\n\n请严格依据已经提供的课程 RAG Context 命题，并按照系统规定的 JSON Schema 返回结果。',
    variables: [
      { name: 'course_id', label: '课程ID', defaultValue: String(DEMO.courseId) },
      { name: 'course_name', label: '课程名称', defaultValue: DEMO.courseName },
      { name: 'course_description', label: '课程简介', defaultValue: DEMO.courseDescription },
      { name: 'chapter_id', label: '章节ID', defaultValue: String(DEMO.chapterId) },
      { name: 'chapter_name', label: '章节名称', defaultValue: DEMO.chapterName },
      { name: 'knowledge_point_ids', label: '目标知识点ID', defaultValue: DEMO.knowledgePointIds },
      { name: 'knowledge_point_names', label: '目标知识点', defaultValue: DEMO.knowledgePointNames },
      { name: 'question_types', label: '题型要求', defaultValue: DEMO_SCENE.question_types },
      { name: 'difficulty', label: '难度', defaultValue: DEMO_SCENE.difficulty },
      { name: 'question_count', label: '题目数量', defaultValue: DEMO_SCENE.question_count },
      { name: 'task_purpose', label: '使用场景', defaultValue: DEMO_SCENE.task_purpose },
      { name: 'student_level', label: '学生水平', defaultValue: DEMO_SCENE.student_level },
      { name: 'score_per_question', label: '每题建议分值', defaultValue: DEMO_SCENE.score_per_question },
      { name: 'generation_requirements', label: '教师额外要求', defaultValue: DEMO_SCENE.generation_requirements },
      { name: 'retrieved_context', label: 'RAG检索资料', defaultValue: DEMO_RAG_CONTEXT },
      { name: 'existing_questions', label: '已有题目去重', defaultValue: DEMO.existingQuestions },
      { name: 'language', label: '输出语言', defaultValue: DEMO_SCENE.language }
    ],
    version: 'v1.0',
    status: 'PUBLISHED',
    boundModel: 'deepseek-chat',
    temperature: 0.35,
    maxTokens: 8000,
    callCount: 112,
    createdAt: '',
    updatedAt: ''
  },
  {
    id: 4,
    code: 'SUBJECTIVE_GRADING',
    name: '主观题多维智能批阅',
    category: 'grading',
    description: '基于标准采分点、关键词覆盖与逻辑连贯性，对学生主观题作答实现分步赋分与改进评语生成。',
    systemPrompt: '你是一位高校资深阅卷教师。请结合采分要点对学生作答进行客观公正批改，输出得分与诊断改进建议。',
    userPromptTemplate: '【题干】：{{question_stem}}\n【参考答案与采分要点】：{{standard_answer}}\n【满分分值】：{{max_score}}\n【学生实际作答】：{{student_answer}}\n\n请按采分点核算得分并生成针对性诊断建议。',
    variables: [
      { name: 'question_stem', label: '题干', defaultValue: DEMO.questionStem },
      { name: 'standard_answer', label: '标准答案', defaultValue: DEMO_REFERENCE_ANSWER },
      { name: 'max_score', label: '满分', defaultValue: String(DEMO.questionScore) },
      { name: 'student_answer', label: '学生作答', defaultValue: DEMO_SCENE.student_answer }
    ],
    version: 'v1.0',
    status: 'PUBLISHED',
    boundModel: 'deepseek-chat',
    temperature: 0.2,
    maxTokens: 1500,
    callCount: 65,
    createdAt: '',
    updatedAt: ''
  },
  {
    id: 7,
    code: 'GRADING_RAG_GENERAL',
    name: '智能批改（RAG）',
    category: 'grading',
    description: '基于课程知识库、标准答案与分步评分点（Scoring Points），对主观题进行语义等价识别、部分得分计算、失分诊断与证据溯源。',
    systemPrompt: `你是 EduMind｜AI智能教学赋能平台中的“智能批改专家”。\n你的核心职责是：根据当前课程、题目要求、标准答案、评分标准、评分点、目标知识点、课程 RAG 资料和学生实际作答，对学生答案进行客观、稳定、可解释、可追溯的教学评价。\n严格遵守语义等价评分原则，支持细粒度评分点（Scoring Points）部分得分，提供失分诊断与证据溯源（Source IDs），并以标准 JSON Schema 格式输出。`,
    userPromptTemplate: '请根据当前题目、评分标准、课程知识库资料，对学生本次作答进行智能批改。\n\n课程：\n{{course_name}} (ID: {{course_id}})\n\n章节：\n{{chapter_name}}\n\n知识点：\n{{knowledge_point_names}}\n\n题目：\n{{question_stem}} (ID: {{question_id}})\n\n题型：\n{{question_type}}\n\n满分：\n{{max_score}}\n\n参考答案：\n{{reference_answer}}\n\n评分标准：\n{{scoring_rubric}}\n\n评分点：\n{{scoring_points}}\n\n学生答案：\n{{student_answer}}\n\n批改模式：\n{{grading_mode}}\n\n教师额外要求：\n{{teacher_requirements}}\n\n请严格按照 System Prompt 中的评分规则进行逐评分点评价，并按照指定 JSON Schema 返回。',
    variables: [
      { name: 'course_id', label: '课程ID', defaultValue: String(DEMO.courseId) },
      { name: 'course_name', label: '课程名称', defaultValue: DEMO.courseName },
      { name: 'course_description', label: '课程简介', defaultValue: DEMO.courseDescription },
      { name: 'chapter_id', label: '章节ID', defaultValue: String(DEMO.chapterId) },
      { name: 'chapter_name', label: '章节名称', defaultValue: DEMO.chapterName },
      { name: 'knowledge_point_ids', label: '目标知识点ID', defaultValue: DEMO.knowledgePointIds },
      { name: 'knowledge_point_names', label: '目标知识点', defaultValue: DEMO.knowledgePointNames },
      { name: 'question_id', label: '题目ID', defaultValue: String(DEMO.questionId) },
      { name: 'question_type', label: '题型', defaultValue: DEMO.questionType },
      { name: 'question_stem', label: '题目内容', defaultValue: DEMO.questionStem },
      { name: 'max_score', label: '满分', defaultValue: String(DEMO.questionScore) },
      { name: 'reference_answer', label: '参考答案', defaultValue: DEMO_REFERENCE_ANSWER },
      { name: 'scoring_rubric', label: '评分标准', defaultValue: DEMO_SCENE.scoring_rubric },
      { name: 'scoring_points', label: '评分点列表', defaultValue: DEMO.questionScoringPoints },
      { name: 'student_answer', label: '学生作答', defaultValue: DEMO_SCENE.student_answer },
      { name: 'retrieved_context', label: 'RAG检索资料', defaultValue: DEMO_RAG_CONTEXT },
      { name: 'grading_mode', label: '批改模式', defaultValue: DEMO_SCENE.grading_mode },
      { name: 'student_level', label: '学生水平', defaultValue: DEMO_SCENE.student_level },
      { name: 'teacher_requirements', label: '教师额外要求', defaultValue: DEMO_SCENE.teacher_requirements },
      { name: 'language', label: '输出语言', defaultValue: DEMO_SCENE.language }
    ],
    version: 'v1.0',
    status: 'PUBLISHED',
    boundModel: 'deepseek-chat',
    temperature: 0.2,
    maxTokens: 8000,
    callCount: 98,
    createdAt: '',
    updatedAt: ''
  },
  {
    id: 5,
    code: 'TEACHING_PLAN_GEN',
    name: '高校教案备课向导设计',
    category: 'teaching',
    description: '结合课程标准与学时安排，全要素生成包含教学目标、重难点、教学环节与板书设计的规范教案。',
    systemPrompt: '你是一位高校教学名师兼教案设计专家。请结合所提供章节与学时要求，输出规范完整的标准化教案。',
    userPromptTemplate: '请为课程【{{course_name}}】的【{{chapter_name}}】章设计一份授课时长为【{{teaching_hours}}】学时的规范教案，授课对象为【{{target_students}}】。',
    variables: [
      { name: 'course_name', label: '课程名称', defaultValue: DEMO.courseName },
      { name: 'chapter_name', label: '章节', defaultValue: DEMO.chapterName },
      { name: 'teaching_hours', label: '学时', defaultValue: DEMO_SCENE.teaching_hours },
      { name: 'target_students', label: '教学对象', defaultValue: DEMO_SCENE.target_students }
    ],
    version: 'v1.0',
    status: 'PUBLISHED',
    boundModel: 'deepseek-chat',
    temperature: 0.5,
    maxTokens: 3000,
    callCount: 39,
    createdAt: '',
    updatedAt: ''
  },
  {
    id: 8,
    code: 'LESSON_PREP_RAG_GENERAL',
    name: '教案备课（RAG）',
    category: 'teaching',
    description: '基于当前课程知识库、课程标准与多源教材课件，融合课时与学情，结构化生成教学目标、重难点、师生活动、形成性评价与课后任务。',
    systemPrompt: `你是 EduMind｜AI智能教学赋能平台中的“AI教案备课专家”。\n你的核心职责是：基于当前课程、章节、知识点、课程标准、教材、教师课件、教学资源以及系统提供的 RAG 检索资料，结合课时长度、学生基础、前置知识、教学目标和教学模式，生成一份准确、完整、结构清晰、时间合理、具有教学可执行性的课程教案与教学设计。\n严格践行“目标—活动—评价一致性检查”，时间总和不超过课时时长，区分课程客观事实（保留Source IDs）与AI教学设计建议，并以标准 JSON Schema 格式输出。`,
    userPromptTemplate: '请基于当前课程知识库和本次教学条件，完成一份可实际用于课堂的教学设计。\n\n课程：\n{{course_name}} (ID: {{course_id}})\n\n章节：\n{{chapter_name}}\n\n课题：\n{{lesson_title}}\n\n知识点：\n{{knowledge_point_names}}\n\n课时：\n{{lesson_count}}\n\n总时长：\n{{lesson_duration}}\n\n学生层次：\n{{student_level}}\n\n班级学情：\n{{class_profile}}\n\n前置知识：\n{{previous_learning}}\n\n指定教学模式：\n{{teaching_mode}}\n\n教师教学目标：\n{{teaching_objectives}}\n\n教师额外要求：\n{{teacher_requirements}}\n\n可用教学资源：\n{{available_resources}}\n\n评价要求：\n{{assessment_requirement}}\n\n课后任务要求：\n{{homework_requirement}}\n\n请严格依据系统提供的课程 RAG Context，完成教学目标、重点难点、教学流程、教师活动、学生活动、时间分配、课堂评价、课后任务和教学反思建议。\n\n请严格按照规定的 JSON Schema 返回结果。',
    variables: [
      { name: 'course_id', label: '课程ID', defaultValue: String(DEMO.courseId) },
      { name: 'course_name', label: '课程名称', defaultValue: DEMO.courseName },
      { name: 'course_description', label: '课程简介', defaultValue: DEMO.courseDescription },
      { name: 'chapter_id', label: '章节ID', defaultValue: String(DEMO.chapterId) },
      { name: 'chapter_name', label: '章节名称', defaultValue: DEMO.chapterName },
      { name: 'knowledge_point_ids', label: '目标知识点ID', defaultValue: DEMO.knowledgePointIds },
      { name: 'knowledge_point_names', label: '目标知识点', defaultValue: DEMO.knowledgePointNames },
      { name: 'lesson_title', label: '本次课题', defaultValue: DEMO_SCENE.lesson_title },
      { name: 'lesson_duration', label: '课时总长度', defaultValue: DEMO_SCENE.lesson_duration },
      { name: 'lesson_count', label: '课时数量', defaultValue: DEMO_SCENE.lesson_count },
      { name: 'student_level', label: '学生层次', defaultValue: DEMO_SCENE.student_level },
      { name: 'class_profile', label: '班级整体学情', defaultValue: DEMO_SCENE.class_profile },
      { name: 'teaching_mode', label: '教学模式', defaultValue: DEMO_SCENE.teaching_mode },
      { name: 'teaching_method', label: '教学方法', defaultValue: DEMO_SCENE.teaching_method },
      { name: 'teaching_objectives', label: '教师教学目标', defaultValue: DEMO_SCENE.teaching_objectives },
      { name: 'teacher_requirements', label: '教师额外要求', defaultValue: DEMO_SCENE.teacher_requirements },
      { name: 'previous_learning', label: '前置知识', defaultValue: DEMO_SCENE.previous_learning },
      { name: 'next_learning', label: '后续知识', defaultValue: DEMO_SCENE.next_learning },
      { name: 'available_resources', label: '可用教学资源', defaultValue: DEMO_SCENE.available_resources },
      { name: 'retrieved_context', label: 'RAG检索资料', defaultValue: DEMO_RAG_CONTEXT },
      { name: 'assessment_requirement', label: '教学评价要求', defaultValue: DEMO_SCENE.assessment_requirement },
      { name: 'homework_requirement', label: '课后作业要求', defaultValue: DEMO_SCENE.homework_requirement },
      { name: 'output_depth', label: '输出深度', defaultValue: DEMO_SCENE.output_depth },
      { name: 'language', label: '输出语言', defaultValue: DEMO_SCENE.language }
    ],
    version: 'v1.0',
    status: 'PUBLISHED',
    boundModel: 'deepseek-chat',
    temperature: 0.4,
    maxTokens: 8000,
    callCount: 86,
    createdAt: '',
    updatedAt: ''
  }
];
