import type { AiUsageAnalyticsVO, LearningAnalyticsVO } from '@/types/analytics/learning';
import type {
  KnowledgeMasteryVO,
  TeachingAdviceVO,
  WrongQuestionAnalyticsVO
} from '@/types/analytics/mastery';

export const MOCK_LEARNING_ANALYTICS: LearningAnalyticsVO = {
  courseId: 1,
  studentCount: 86,
  completionRate: 78.5,
  avgScore: 82.4,
  avgStudyMinutes: 142,
  knowledgeMasteryAvg: 71.2,
  aiUsageCount: 1246,
  trends: {
    learning: [
      { date: '周一', activeUsers: 52 },
      { date: '周二', activeUsers: 68 },
      { date: '周三', activeUsers: 61 },
      { date: '周四', activeUsers: 74 },
      { date: '周五', activeUsers: 80 },
      { date: '周六', activeUsers: 45 },
      { date: '周日', activeUsers: 38 }
    ],
    score: [
      { date: '周一', avgScore: 76.2 },
      { date: '周二', avgScore: 78.5 },
      { date: '周三', avgScore: 79.1 },
      { date: '周四', avgScore: 81.3 },
      { date: '周五', avgScore: 82.4 },
      { date: '周六', avgScore: 80.8 },
      { date: '周日', avgScore: 79.6 }
    ]
  }
};

export const MOCK_KNOWLEDGE_MASTERY: KnowledgeMasteryVO = {
  dimensions: ['极限', '导数', '积分', '级数', '多元函数', '微分方程'],
  personal: [88, 76, 62, 54, 71, 48],
  classAvg: [82, 74, 68, 61, 69, 55],
  weakPoints: [
    {
      knowledgePointId: 101,
      title: '洛必达法则未定式前提',
      mastery: 42,
      suggestion: '建议安排 10 分钟典型例题辨析'
    },
    {
      knowledgePointId: 102,
      title: '泰勒公式高阶截断',
      mastery: 56,
      suggestion: '建议使用 AI 出题进行随堂微测验'
    }
  ]
};

export const MOCK_WRONG_QUESTIONS: WrongQuestionAnalyticsVO = {
  total: 3,
  list: [
    {
      questionId: 1001,
      wrongCount: 28,
      errorTypes: ['概念混淆', '计算失误'],
      diagnosis: '未验证 0/0 型未定式前提即套用洛必达法则',
      variantQuestionIds: [2001, 2002]
    },
    {
      questionId: 1002,
      wrongCount: 19,
      errorTypes: ['步骤不全'],
      diagnosis: '高阶展开项保留阶数不足',
      variantQuestionIds: [2003]
    },
    {
      questionId: 1003,
      wrongCount: 14,
      errorTypes: ['逻辑推演'],
      diagnosis: '未陈述函数连续可导前提',
      variantQuestionIds: []
    }
  ]
};

export const MOCK_AI_USAGE: AiUsageAnalyticsVO = {
  totalCalls: 3248,
  totalTokens: 1842000,
  daily: [
    { date: '周一', calls: 420, tokens: 238000 },
    { date: '周二', calls: 510, tokens: 291000 },
    { date: '周三', calls: 468, tokens: 265000 },
    { date: '周四', calls: 592, tokens: 338000 },
    { date: '周五', calls: 628, tokens: 356000 },
    { date: '周六', calls: 312, tokens: 178000 },
    { date: '周日', calls: 318, tokens: 176000 }
  ],
  byProvider: [
    { provider: 'DeepSeek', calls: 1820, tokens: 1024000 },
    { provider: 'OpenAI', calls: 980, tokens: 568000 },
    { provider: 'Qwen', calls: 448, tokens: 250000 }
  ]
};

export const MOCK_TEACHING_ADVICE: TeachingAdviceVO = {
  summary:
    '根据本周做题轨迹，学生在《未定式极限代换前提》与《泰勒公式高阶截断》存在理解偏差，建议下周安排针对性巩固。',
  actions: [
    '周二第 3 节课安排 10 分钟典型数形结合例题辨析',
    '使用 AI 出题生成 5 道变式练习推送至班级',
    '在课程 AI 助教中置顶相关知识点 FAQ'
  ]
};
