/** 全局统一的 AI 认知推演加载态文案（雷达 + 计时 + 流水线） */

export interface AiCognitiveThinkingPreset {
  title: string;
  pausedTitle: string;
  steps: string[];
}

export const AI_COGNITIVE_THINKING_PRESETS = {
  memoryExtract: {
    title: 'AI 认知引擎正在深度推演分析中...',
    pausedTitle: 'AI 认知引擎推演已暂停',
    steps: [
      '检索本空间师生问答交互与错题日志',
      '调用认知心理学模型推导多维学习风格与薄弱点',
      '执行语义去重对比与 PIPL 隐私合规安全评级'
    ]
  },
  courseObjectives: {
    title: 'AI 教学引擎正在推演课程教学目标...',
    pausedTitle: '教学目标推演已暂停',
    steps: [
      '汇总课程章节结构与已关联知识点',
      '通过 AI 网关调用大模型生成可评价目标',
      '解析并校验教学目标结构化结果'
    ]
  },
  courseDescription: {
    title: 'AI 文案引擎正在撰写课程简介...',
    pausedTitle: '简介撰写推演已暂停',
    steps: [
      '汇总章节大纲、学分学时与学科门类信息',
      '结合教学目标与修读要求生成结构化表述',
      '校验字数边界并输出可直接发布的简介草案'
    ]
  },
  knowledgePointSuggest: {
    title: 'AI 知识图谱引擎正在提炼考点体系...',
    pausedTitle: '考点提炼推演已暂停',
    steps: [
      '研读章节大纲与已有考点去重边界',
      '结合布鲁姆认知维度生成结构化考点',
      '标注考查重点与说明字段并输出候选清单'
    ]
  },
  chapterMicroLesson: {
    title: 'AI 教学大纲引擎正在规划微课节体系...',
    pausedTitle: '微课节规划推演已暂停',
    steps: [
      '研读本章教学目标与布鲁姆认知递进关系',
      '拆解讲授、演练与诊断自测等微课节组合',
      '映射考查点数量并生成结构化课时草案'
    ]
  },
  courseChapterOutline: {
    title: 'AI 课程引擎正在推导章节大纲...',
    pausedTitle: '章节大纲推演已暂停',
    steps: [
      '分析课程学科门类与学时学分约束',
      '匹配标准教学模板与知识递进路径',
      '生成可编辑的章节标题与概要结构'
    ]
  },
  questionGenerate: {
    title: 'AI 命题引擎正在深度推演分析中...',
    pausedTitle: 'AI 命题引擎推演已暂停',
    steps: [
      '解析知识点考纲与布鲁姆认知层级约束',
      '激活专属教研 Prompt 模板与试题场景化建模',
      '调用大模型推理生成题干、强诱惑干扰项与深度题解',
      '执行 LaTeX 科学公式排版校验与结构化题库规整'
    ]
  }
} as const satisfies Record<string, AiCognitiveThinkingPreset>;
