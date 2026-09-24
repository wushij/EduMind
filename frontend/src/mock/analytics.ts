import type {
  AiUsageAnalyticsVO,
  LearningAnalyticsVO,
  StudentPortraitVO
} from '@/types/analytics/learning';
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
      { date: '周一', avgScore: 76.2, schoolAvgScore: 73.5 },
      { date: '周二', avgScore: 78.5, schoolAvgScore: 74.2 },
      { date: '周三', avgScore: 79.1, schoolAvgScore: 74.8 },
      { date: '周四', avgScore: 81.3, schoolAvgScore: 75.1 },
      { date: '周五', avgScore: 82.4, schoolAvgScore: 76.0 },
      { date: '周六', avgScore: 80.8, schoolAvgScore: 75.6 },
      { date: '周日', avgScore: 79.6, schoolAvgScore: 75.2 }
    ]
  },
  chapterProgressList: [
    { chapterId: 101, chapterTitle: '第 1 章：Java 基础语法与面向对象初探', sort: 1, completionRate: 96.5, avgScore: 89.2, studentCount: 86, avgStudyMinutes: 120 },
    { chapterId: 102, chapterTitle: '第 2 章：类与对象、封装继承多态实战', sort: 2, completionRate: 91.0, avgScore: 86.4, studentCount: 84, avgStudyMinutes: 145 },
    { chapterId: 103, chapterTitle: '第 3 章：集合框架 ArrayList 与 LinkedList 源码剖析', sort: 3, completionRate: 84.5, avgScore: 78.2, studentCount: 79, avgStudyMinutes: 160 },
    { chapterId: 104, chapterTitle: '第 4 章：泛型、异常处理与反射机制', sort: 4, completionRate: 77.0, avgScore: 74.5, studentCount: 72, avgStudyMinutes: 180 },
    { chapterId: 105, chapterTitle: '第 5 章：并发编程与 JVM 内存模型基础', sort: 5, completionRate: 68.5, avgScore: 71.0, studentCount: 65, avgStudyMinutes: 210 }
  ],
  courseHealth: {
    overallScore: 88.5,
    syllabusCoverage: 92,
    assignmentCompletion: 85,
    studentInteraction: 78,
    passRate: 92,
    aiAssistanceRate: 82,
    healthLevel: 'EXCELLENT'
  },
  courseWeakPoints: [
    { knowledgePointId: 16, title: 'ArrayList 与 LinkedList 源码剖析', mastery: 67.5, wrongCount: 14, affectedStudents: 28, urgency: 'HIGH' },
    { knowledgePointId: 10, title: '面向对象三大特征与多态运行时绑定', mastery: 72.0, wrongCount: 9, affectedStudents: 18, urgency: 'MEDIUM' },
    { knowledgePointId: 12, title: '基本数据类型与包装类自动拆装箱', mastery: 78.5, wrongCount: 5, affectedStudents: 11, urgency: 'MEDIUM' }
  ],
  students: [
    {
      studentId: 3,
      username: 'student',
      realName: '李同学',
      studentNo: 'STU-0003',
      avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
      studyMinutes: 154,
      avgScore: 88.5,
      submissionRate: 100,
      masteryScore: 82.0,
      aiUsageCount: 26,
      wrongCount: 2,
      status: 'EXCELLENT'
    },
    {
      studentId: 4,
      username: 'student2',
      realName: '王同学',
      studentNo: 'STU-0004',
      avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
      studyMinutes: 110,
      avgScore: 78.0,
      submissionRate: 85,
      masteryScore: 74.0,
      aiUsageCount: 18,
      wrongCount: 4,
      status: 'GOOD'
    },
    {
      studentId: 6,
      username: 'chen_student',
      realName: '陈晨',
      studentNo: 'STU-0006',
      studyMinutes: 65,
      avgScore: 62.5,
      submissionRate: 60,
      masteryScore: 58.0,
      aiUsageCount: 8,
      wrongCount: 7,
      status: 'WARNING'
    }
  ]
};

export const MOCK_STUDENT_PORTRAIT: StudentPortraitVO = {
  studentInfo: {
    studentId: 3,
    username: 'student',
    realName: '李同学',
    avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
    studentNo: 'STU-0003',
    className: '高三(1)班 [理科实验班]',
    role: '在册学员',
    lastActiveTime: '2026-09-16 10:45'
  },
  summary: {
    totalStudyMinutes: 154,
    classAvgStudyMinutes: 142,
    avgScore: 88.5,
    classAvgScore: 82.4,
    submissionRate: 100,
    overallMastery: 82.0,
    aiUsageCount: 26,
    wrongQuestionCount: 2,
    learningPace: 'FAST'
  },
  radar: {
    dimensions: ['注意力机制', 'Transformer架构', '预训练与微调', '模型对齐RLHF', '长文本推理', '量化与部署'],
    personalScores: [92, 88, 75, 62, 85, 90],
    classAvgScores: [80, 78, 72, 68, 76, 75]
  },
  knowledgePoints: [
    {
      knowledgePointId: 101,
      title: 'Self-Attention 矩阵运算与缩放点积',
      masteryScore: 92,
      sampleCount: 6,
      status: 'MASTERED',
      lastAssessedAt: '2026-09-15',
      suggestion: '掌握牢固，建议继续挑战高阶变式'
    },
    {
      knowledgePointId: 102,
      title: '多头注意力机制维度投影',
      masteryScore: 88,
      sampleCount: 5,
      status: 'MASTERED',
      lastAssessedAt: '2026-09-15',
      suggestion: '核心考点理解充分'
    },
    {
      knowledgePointId: 103,
      title: 'LoRA 低秩自适应参数更新机理',
      masteryScore: 75,
      sampleCount: 4,
      status: 'LEARNING',
      lastAssessedAt: '2026-09-14',
      suggestion: '梯度截断细节需进一步巩固'
    },
    {
      knowledgePointId: 104,
      title: 'PPO 与 DPO 强化学习偏好对齐损失函数',
      masteryScore: 62,
      sampleCount: 4,
      status: 'WEAK',
      lastAssessedAt: '2026-09-14',
      suggestion: 'KL 散度约束项理解存在偏差，建议复习相关推导'
    }
  ],
  weakPoints: [
    {
      knowledgePointId: 104,
      title: 'PPO 与 DPO 强化学习偏好对齐损失函数',
      mastery: 62,
      suggestion: '建议针对对齐损失推导进行 10 分钟强化测验并结合 AI 答疑'
    }
  ],
  masteredPoints: [
    {
      knowledgePointId: 101,
      title: 'Self-Attention 矩阵运算与缩放点积',
      mastery: 92
    },
    {
      knowledgePointId: 102,
      title: '多头注意力机制维度投影',
      mastery: 88
    }
  ],
  wrongQuestions: [
    {
      recordId: 1,
      questionId: 1007,
      questionStem: '在 DPO 算法优化目标中，隐式奖励模型与标准 PPO 策略梯度的主要区别体现在哪个方面？',
      knowledgePointId: 104,
      knowledgePointTitle: 'PPO 与 DPO 强化学习偏好对齐损失函数',
      errorTypes: 'CONCEPT,LOGIC',
      diagnosis: '混淆了直接策略优化与显式 Critic 估值网络的更新时机',
      wrongCount: 2,
      createTime: '2026-09-15'
    }
  ],
  adaptiveWeeks: [
    {
      weekNo: 1,
      theme: '强化：PPO 与 DPO 强化学习偏好对齐',
      tasks: [
        { title: '复习知识点与公式推导精讲', type: 'READ', status: 'COMPLETED' },
        { title: '完成 3 道偏好对齐损失变式题', type: 'PRACTICE', status: 'PENDING' },
        { title: 'AI 助教答疑辨析 KL 散度约束', type: 'AI_CHAT', status: 'PENDING' }
      ]
    }
  ],
  aiDiagnosis:
    '【AI 导师学情综合评价】李同学在《深度学习与大语言模型系统工程》中的学习表现极其突出，平均分达 88.5 分（高于班级均值 6.1 分），知识图谱达成度为 82.0%。在注意力机制、模型量化部署等工程实践考点展现出强大的理解力；仅在偏好对齐损失函数推导上存在偶发概念混淆。已为您生成自适应巩固周计划，建议通过 AI 专属助教进行 1 对 1 定向突破。'
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
  ],
  byScene: [
    { scene: '智能答疑解惑', calls: 1820, ratio: 56.0, sourceScenes: ['CHAT', 'CHAT_RAG', 'GLOBAL_ASSISTANT'] },
    { scene: '试题精准批阅', calls: 620, ratio: 19.1, sourceScenes: ['GRADING', 'SUBJECTIVE_GRADING'] },
    { scene: '靶向变式推演', calls: 496, ratio: 15.3, sourceScenes: ['QUESTION_GENERATE'] },
    { scene: '学情诊断评估', calls: 312, ratio: 9.6, sourceScenes: ['TEACHING_ADVICE', 'LEARNING'] }
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
