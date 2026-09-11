export interface ChapterSection {
  id: number;
  title: string;
  duration?: string;
  knowledgePointCount?: number;
  completed?: boolean;
  type?: 'video' | 'doc' | 'quiz' | 'discussion';
}

export interface Chapter {
  id: number;
  title: string;
  orderNum: number;
  description?: string;
  sections: ChapterSection[];
}

export const MOCK_CHAPTERS: Record<number, Chapter[]> = {
  101: [
    {
      id: 1,
      title: '第一章 函数、极限与连续性',
      orderNum: 1,
      description: '掌握初等函数性质、数列极限与函数极限的定义、无穷小量比较与闭区间连续函数性质。',
      sections: [
        { id: 101, title: '1.1 映射与函数的核心概念', duration: '25分钟', knowledgePointCount: 4, completed: true, type: 'video' },
        { id: 102, title: '1.2 数列极限与 $\\epsilon-N$ 语言', duration: '40分钟', knowledgePointCount: 6, completed: true, type: 'video' },
        { id: 103, title: '1.3 两个重要极限及其在连续复利中的应用', duration: '35分钟', knowledgePointCount: 5, completed: true, type: 'video' },
        { id: 104, title: '第一章 单元知识诊断与微测试', duration: '20分钟', knowledgePointCount: 8, completed: false, type: 'quiz' }
      ]
    },
    {
      id: 2,
      title: '第二章 导数与微分及其几何意义',
      orderNum: 2,
      description: '导数的物理/几何定义、复合函数求导法则、高阶导数与微分形式不变性。',
      sections: [
        { id: 201, title: '2.1 导数的瞬时变化率本质', duration: '30分钟', knowledgePointCount: 5, completed: true, type: 'video' },
        { id: 202, title: '2.2 链式法则与参数方程求导', duration: '45分钟', knowledgePointCount: 7, completed: false, type: 'video' },
        { id: 203, title: '2.3 高阶导数与莱布尼茨公式', duration: '35分钟', knowledgePointCount: 4, completed: false, type: 'video' }
      ]
    },
    {
      id: 3,
      title: '第三章 微分中值定理与导数的应用',
      orderNum: 3,
      description: '罗尔定理、拉格朗日中值定理、柯西中值定理、泰勒公式与洛必达法则求未定式极限。',
      sections: [
        { id: 301, title: '3.1 罗尔中值定理与驻点判别', duration: '30分钟', knowledgePointCount: 4, completed: false, type: 'video' },
        { id: 302, title: '3.2 拉格朗日中值定理在不等式证明中的威力', duration: '40分钟', knowledgePointCount: 6, completed: false, type: 'video' },
        { id: 303, title: '3.3 泰勒中值定理与多项式近似', duration: '50分钟', knowledgePointCount: 8, completed: false, type: 'video' }
      ]
    },
    {
      id: 4,
      title: '第四章 不定积分与原函数求解技巧',
      orderNum: 4,
      description: '第一换元法、第二换元法、分部积分法与有理函数积分。',
      sections: [
        { id: 401, title: '4.1 原函数存在定理与基本积分表', duration: '30分钟', knowledgePointCount: 5, completed: false, type: 'video' },
        { id: 402, title: '4.2 换元积分法深度精讲', duration: '45分钟', knowledgePointCount: 6, completed: false, type: 'video' }
      ]
    }
  ],
  102: [
    {
      id: 11,
      title: '第1章 数据结构与算法复杂度概述',
      orderNum: 1,
      description: '数据结构四类逻辑关系、存储结构、渐进大 O 阶时间与空间复杂度度量。',
      sections: [
        { id: 1101, title: '1.1 数据结构核心四要素', duration: '20分钟', knowledgePointCount: 3, completed: true, type: 'video' },
        { id: 1102, title: '1.2 算法时间复杂度渐进分析', duration: '35分钟', knowledgePointCount: 5, completed: true, type: 'video' }
      ]
    },
    {
      id: 12,
      title: '第2章 线性表、栈与队列的实现',
      orderNum: 2,
      description: '顺序表、单链表/双向循环链表、后进先出栈的应用（表达式求值）与环形缓冲区队列。',
      sections: [
        { id: 1201, title: '2.1 顺序存储与链式存储的对比', duration: '30分钟', knowledgePointCount: 6, completed: true, type: 'video' },
        { id: 1202, title: '2.2 栈在函数调用与递归回溯中的实战', duration: '40分钟', knowledgePointCount: 7, completed: false, type: 'video' },
        { id: 1203, title: '2.3 环形循环队列的高并发无锁设计', duration: '35分钟', knowledgePointCount: 4, completed: false, type: 'video' }
      ]
    },
    {
      id: 13,
      title: '第3章 树结构、二叉树与哈夫曼树',
      orderNum: 3,
      description: '二叉树遍历递归与非递归实现、二叉搜索树、平衡二叉树（AVL）及哈夫曼压缩编码。',
      sections: [
        { id: 1301, title: '3.1 二叉树的三序遍历与层序遍历', duration: '40分钟', knowledgePointCount: 8, completed: false, type: 'video' },
        { id: 1302, title: '3.2 AVL 平衡树的单双旋转调整机制', duration: '45分钟', knowledgePointCount: 6, completed: false, type: 'video' }
      ]
    }
  ]
};
