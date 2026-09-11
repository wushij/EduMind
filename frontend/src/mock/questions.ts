import type {
  QuestionOption,
  QuestionType,
  Difficulty,
  Question,
  QuestionItem
} from '@/types/question/question';

export type { QuestionOption, QuestionType, Difficulty, Question, QuestionItem };

export const MOCK_QUESTIONS: Question[] = [
  {
    id: 1001,
    courseId: 101,
    courseName: '大学数学：高等数学（上）',
    chapterId: 1,
    chapterName: '第一章 函数、极限与连续性',
    type: 'SINGLE_CHOICE',
    typeLabel: '单选题',
    difficulty: 'MEDIUM',
    difficultyLabel: '中等',
    score: 5,
    stem: '当 $x \\to 0$ 时，下列无穷小量中与 $x$ 等价的无穷小量是（ ）。',
    options: [
      { key: 'A', content: '$\\sin 2x$', isCorrect: false },
      { key: 'B', content: '$\\ln(1 + x)$', isCorrect: true },
      { key: 'C', content: '$1 - \\cos x$', isCorrect: false },
      { key: 'D', content: '$e^x - 1 - x$', isCorrect: false }
    ],
    correctAnswer: 'B',
    analysis: '根据等价无穷小基本公式，当 $x \\to 0$ 时，$\\ln(1+x) \\sim x$；而 $\\sin 2x \\sim 2x$，$1-\\cos x \\sim \\frac{1}{2}x^2$。故正确答案为 B。',
    knowledgePointNames: ['等价无穷小代换', '极限性质'],
    createdAt: '2026-09-08'
  },
  {
    id: 1002,
    courseId: 101,
    courseName: '大学数学：高等数学（上）',
    chapterId: 2,
    chapterName: '第二章 导数与微分及其几何意义',
    type: 'SINGLE_CHOICE',
    typeLabel: '单选题',
    difficulty: 'EASY',
    difficultyLabel: '简单',
    score: 5,
    stem: '设函数 $f(x) = \\ln(1 + x^2)$，则导数 $f\'(1)$ 的值为（ ）。',
    options: [
      { key: 'A', content: '$\\frac{1}{2}$', isCorrect: false },
      { key: 'B', content: '$1$', isCorrect: true },
      { key: 'C', content: '$2$', isCorrect: false },
      { key: 'D', content: '$\\ln 2$', isCorrect: false }
    ],
    correctAnswer: 'B',
    analysis: '求复合函数导数：$f\'(x) = \\frac{1}{1 + x^2} \\cdot 2x = \\frac{2x}{1 + x^2}$，代入 $x = 1$ 得 $f\'(1) = \\frac{2}{2} = 1$。',
    knowledgePointNames: ['复合函数求导', '导数计算'],
    createdAt: '2026-09-08'
  },
  {
    id: 1003,
    courseId: 102,
    courseName: '计算机核心：数据结构与算法',
    chapterId: 12,
    chapterName: '第2章 线性表、栈与队列的实现',
    type: 'SINGLE_CHOICE',
    typeLabel: '单选题',
    difficulty: 'MEDIUM',
    difficultyLabel: '中等',
    score: 5,
    stem: '已知一个栈的入栈序列为 1, 2, 3, 4, 5，则不可能得到的出栈序列是（ ）。',
    options: [
      { key: 'A', content: '5, 4, 3, 2, 1', isCorrect: false },
      { key: 'B', content: '4, 5, 3, 2, 1', isCorrect: false },
      { key: 'C', content: '4, 3, 5, 1, 2', isCorrect: true },
      { key: 'D', content: '1, 2, 3, 4, 5', isCorrect: false }
    ],
    correctAnswer: 'C',
    analysis: '选项 C 中，4、3、5 出栈后，栈内剩余元素为 2、1（2 在栈顶，1 在栈底），接下来的出栈顺序必须是 2 先出栈，不可能 1 先于 2 出栈。',
    knowledgePointNames: ['栈的后进先出特性', '出栈序列合法性判断'],
    createdAt: '2026-09-09'
  },
  {
    id: 1004,
    courseId: 102,
    courseName: '计算机核心：数据结构与算法',
    chapterId: 13,
    chapterName: '第3章 树结构、二叉树与哈夫曼树',
    type: 'MULTIPLE_CHOICE',
    typeLabel: '多选题',
    difficulty: 'HARD',
    difficultyLabel: '困难',
    score: 6,
    stem: '关于二叉平衡树（AVL 树）和二叉搜索树（BST），下列说法正确的有（ ）。',
    options: [
      { key: 'A', content: '二叉搜索树在最坏情况下的查找时间复杂度为 $O(n)$', isCorrect: true },
      { key: 'B', content: 'AVL 树的任何结点的左右子树高度差绝对值不超过 1', isCorrect: true },
      { key: 'C', content: '插入新结点引起失衡时，最多需要进行两次单旋转或一次双旋转调整', isCorrect: true },
      { key: 'D', content: '二叉搜索树的中序遍历序列必然是单调递增序列', isCorrect: true }
    ],
    correctAnswer: 'A, B, C, D',
    analysis: '四项表述全部正确。BST退化为单支树时查找为O(n)；AVL树通过四种旋转（LL/RR/LR/RL）保证平衡因子在[-1, 1]内。',
    knowledgePointNames: ['AVL平衡树', '二叉搜索树中序遍历', '旋转调整'],
    createdAt: '2026-09-09'
  },
  {
    id: 1005,
    courseId: 102,
    courseName: '计算机核心：数据结构与算法',
    chapterId: 12,
    chapterName: '第2章 线性表、栈与队列的实现',
    type: 'SHORT_ANSWER',
    typeLabel: '简答题',
    difficulty: 'HARD',
    difficultyLabel: '困难',
    score: 15,
    stem: '请简述循环队列解决假溢出问题的基本原理，并给出采用“少用一个存储单元”法判断队空与队满的条件表达式。',
    correctAnswer: '队空条件：front == rear；队满条件：(rear + 1) % MAXSIZE == front',
    analysis: '原理：利用模运算（%）将一维数组在逻辑上首尾相连形成环形。条件：设 front 为头指针，rear 为尾指针。队空时 front == rear；队满时 (rear + 1) % MAXSIZE == front。',
    knowledgePointNames: ['循环队列', '取模运算', '队满队空条件判断'],
    createdAt: '2026-09-10'
  }
];
