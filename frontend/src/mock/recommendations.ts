import type { RecommendationItem } from '@/types/learning/recommendation';

export type { RecommendationItem };

export const MOCK_RECOMMENDATIONS: RecommendationItem[] = [
  {
    id: 'rec_1',
    title: '洛必达法则 “0/0” 与 “∞/∞” 未定式专项训练 (3题)',
    type: 'exercise',
    typeLabel: '专项自适应练习',
    category: '薄弱巩固',
    matchScore: 98,
    courseName: '高等数学（上）',
    knowledgePoint: '洛必达法则与未定式极限',
    difficulty: 'MEDIUM',
    difficultyLabel: '中等难度',
    estimatedMinutes: 12,
    description: 'AI 诊断检测到你在第 3 次阶段作业中该知识点正答率仅为 40%，推荐优先完成 3 道阶梯进阶题。',
    tags: ['期末高频', '错题雷区', '智能推导支持'],
    exerciseMeta: {
      questionCount: 3,
      averageAccuracy: '62%'
    }
  },
  {
    id: 'rec_2',
    title: '《微分中值定理的几何本质与辅助函数构造》名师精讲微课',
    type: 'resource',
    typeLabel: '重点解析微课',
    category: '精选课件',
    matchScore: 95,
    courseName: '高等数学（上）',
    knowledgePoint: '拉格朗日与罗尔中值定理',
    difficulty: 'HARD',
    difficultyLabel: '较难拓展',
    estimatedMinutes: 15,
    description: '通过动态切线动画直观还原罗尔定理与柯西定理的几何投影，帮助建立数形结合的思维范式。',
    tags: ['名师动画', '高赞微课', '1080P'],
    resourceMeta: {
      format: 'MP4 视频',
      fileSize: '42.8 MB'
    }
  },
  {
    id: 'rec_3',
    title: '二叉树非递归遍历（基于栈的显式模拟）经典题解',
    type: 'exercise',
    typeLabel: '算法编程实战',
    category: '核心必刷',
    matchScore: 92,
    courseName: '数据结构与算法',
    knowledgePoint: '二叉树的前序/中序遍历算法',
    difficulty: 'MEDIUM',
    difficultyLabel: '中等难度',
    estimatedMinutes: 20,
    description: '掌握用显式调用栈代替系统函数调用栈的关键循环不变量设计，考研与大厂面试高频必考。',
    tags: ['LeetCode 经典', '考研 408', '代码评测'],
    exerciseMeta: {
      questionCount: 2,
      averageAccuracy: '74%'
    }
  },
  {
    id: 'rec_4',
    title: '定积分的几何应用：旋转体体积与微元切片法解题模板 (PDF)',
    type: 'resource',
    typeLabel: '核心知识手册',
    category: '精选课件',
    matchScore: 89,
    courseName: '高等数学（上）',
    knowledgePoint: '定积分的几何与物理应用',
    difficulty: 'EASY',
    difficultyLabel: '基础巩固',
    estimatedMinutes: 10,
    description: '系统梳理绕 x 轴、y 轴旋转体体积的圆盘法与圆柱壳法（外壳法）双公式对比图谱。',
    tags: ['高清课件', '考前速记', '包含公式表'],
    resourceMeta: {
      format: 'PDF 讲义',
      fileSize: '8.4 MB'
    }
  },
  {
    id: 'rec_5',
    title: '泰勒公式的高阶展开与极限快速代换技巧演练 (5题)',
    type: 'exercise',
    typeLabel: '考点专项攻坚',
    category: '拓展进阶',
    matchScore: 86,
    courseName: '高等数学（上）',
    knowledgePoint: '泰勒中值定理与麦克劳林展开',
    difficulty: 'HARD',
    difficultyLabel: '压轴挑战',
    estimatedMinutes: 25,
    description: '突破展开阶数不足或冗余的痛点，精选包含 $\\cos(\\sin x)$ 等复合函数极限化简经典题。',
    tags: ['竞赛真题', '压轴大题', '图谱拓扑依赖'],
    exerciseMeta: {
      questionCount: 5,
      averageAccuracy: '48%'
    }
  },
  {
    id: 'rec_6',
    title: '哈希冲突解决法：开放寻址与链地址法空间局部性对比解析',
    type: 'resource',
    typeLabel: '学术深度解析',
    category: '拓展进阶',
    matchScore: 84,
    courseName: '数据结构与算法',
    knowledgePoint: '散列表查找与装载因子',
    difficulty: 'MEDIUM',
    difficultyLabel: '中等难度',
    estimatedMinutes: 15,
    description: '结合 CPU Cache 命中率分析为什么工业级实现倾向于使用开放寻址探查。',
    tags: ['底层原理', '系统设计', '拓展讲义'],
    resourceMeta: {
      format: 'Markdown/PDF',
      fileSize: '3.6 MB'
    }
  }
];
