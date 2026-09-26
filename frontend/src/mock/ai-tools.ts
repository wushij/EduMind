import type { AITool } from '@/types/ai/tool';

export type { AITool };

/**
 * 开发兜底数据（仅 VITE_USE_MOCK=true 且 DEV 时启用）。
 *
 * 注意：这里刻意不写 `modelId` 与「调用次数」——
 * - 绑定模型由后端按「场景路由 → 平台默认模型」实时解析，前端 mock 无从得知，写了就是假的；
 * - 调用次数是真实累计值，mock 里填演示数字会与真实统计口径不一致。
 */
export const MOCK_AI_TOOLS: AITool[] = [
  {
    id: 'tool_question_gen',
    name: 'AI 智能出题',
    category: 'TEACHER',
    categoryLabel: '教师提效',
    description: '根据课程、章节和知识点智能生成高质量题目',
    detailedIntro: '支持按章节与知识点勾选范围，配置题型、难度与题量后批量生成结构化试题，并可一键入库。',
    iconBg: 'linear-gradient(135deg, #3B82F6 0%, #1D4ED8 100%)',
    iconName: 'EditPen',
    iconTheme: 'cyan',
    route: '/ai/question/generate',
    executionMode: 'ROUTE',
    tags: ['出题', '教师', '热门'],
    isRecommended: true,
    isHot: true,
    isFavorite: true,
    usageCount: 0
  },
  {
    id: 'tool_exam_gen',
    name: 'AI 智能组卷',
    category: 'TEACHER',
    categoryLabel: '教师提效',
    description: '按总分、题型比例与难度规则快速生成标准化试卷',
    detailedIntro: '内置总分校验与题型配比引擎，支持预览换题、调分并保存为可复用试卷。',
    iconBg: 'linear-gradient(135deg, #8B5CF6 0%, #6D28D9 100%)',
    iconName: 'Tickets',
    iconTheme: 'purple',
    route: '/ai/exam/generate',
    executionMode: 'ROUTE',
    tags: ['组卷', '教师'],
    isRecommended: true,
    isHot: true,
    isFavorite: true,
    usageCount: 0
  },
  {
    id: 'tool_grading',
    name: 'AI 智能批改',
    category: 'TEACHER',
    categoryLabel: '教师提效',
    description: '客观题秒级判分，主观题 AI 评分与评语生成',
    detailedIntro: '支持作业提交后自动批改与教师复核改分，减轻期末阅卷压力。',
    iconBg: 'linear-gradient(135deg, #10B981 0%, #059669 100%)',
    iconName: 'CircleCheck',
    iconTheme: 'emerald',
    route: '/ai/grading',
    executionMode: 'ROUTE',
    tags: ['批改', '教师'],
    isRecommended: true,
    isHot: false,
    isFavorite: false,
    usageCount: 0
  },
  {
    id: 'tool_summary',
    name: 'AI 课程总结',
    category: 'TEACHER',
    categoryLabel: '教师提效',
    description: '按章节或知识模块提炼核心要点与易错清单',
    detailedIntro: '支持长文档与课件要点结构化摘要，生成考前复习精要。',
    iconBg: 'linear-gradient(135deg, #14B8A6 0%, #0D9488 100%)',
    iconName: 'DocumentCopy',
    iconTheme: 'teal',
    route: '/ai/summary',
    executionMode: 'ROUTE',
    tags: ['总结', '知识提炼'],
    isRecommended: false,
    isHot: false,
    isFavorite: false,
    usageCount: 0
  },
  {
    id: 'tool_lesson_prep',
    name: 'AI 智能备课',
    category: 'TEACHER',
    categoryLabel: '教师提效',
    description: '结合课程大纲、课节目标与知识库资料，一键生成结构化课节教案',
    detailedIntro: '从创建课程空间开始备课：完成课程初始化与教学大纲后，进入课节教案工作台，依据教学设计目标与 RAG 检索资料生成教学目标、重难点、师生活动与板书建议，正文确认后即可插入课节，落库留存并可继续编辑。',
    iconBg: 'linear-gradient(135deg, #6366F1 0%, #4338CA 100%)',
    iconName: 'Notebook',
    iconTheme: 'indigo',
    route: '/course/create',
    executionMode: 'ROUTE',
    tags: ['备课', '教师'],
    isRecommended: true,
    isHot: false,
    isFavorite: false,
    usageCount: 0
  },
  {
    id: 'tool_chat',
    name: 'AI 课程问答',
    category: 'GENERAL',
    categoryLabel: '通用工具',
    description: '基于课程资料的上下文助教答疑（SSE 流式）',
    detailedIntro: '在课程空间内多轮对话，支持 Markdown、公式与代码高亮渲染。',
    iconBg: 'linear-gradient(135deg, #3B82F6 0%, #1D4ED8 100%)',
    iconName: 'Service',
    iconTheme: 'blue',
    route: '/course/101/ai',
    executionMode: 'ROUTE',
    tags: ['问答', '助教', '热门'],
    isRecommended: true,
    isHot: true,
    isFavorite: true,
    usageCount: 0
  },
  {
    id: 'tool_practice',
    name: 'AI 自适应刷题',
    category: 'STUDENT',
    categoryLabel: '学生助学',
    description: '根据薄弱知识点智能生成阶梯练习',
    detailedIntro: '分析近期学习数据，推送专项巩固题包与难度递进练习。',
    iconBg: 'linear-gradient(135deg, #F97316 0%, #C2410C 100%)',
    iconName: 'Reading',
    iconTheme: 'amber',
    route: '/learning/recommendations',
    executionMode: 'ROUTE',
    tags: ['练习', '学生', '推荐'],
    isRecommended: true,
    isHot: false,
    isFavorite: false,
    usageCount: 0
  }
];
