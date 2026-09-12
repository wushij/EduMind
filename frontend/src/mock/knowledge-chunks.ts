import { DocumentChunk, ChunkStatsVO } from '@/types/knowledge/chunk';

export const mockChunks: DocumentChunk[] = [
  {
    id: 101,
    documentId: 1,
    documentName: '第3章_Java多态性与接口深度解析.pdf',
    chunkIndex: 1,
    heading: '3.1 多态的概念与底层方法表机制',
    pageNo: 12,
    tokenCount: 286,
    charCount: 450,
    status: 'INDEXED',
    content: '在面向对象编程中，多态（Polymorphism）指同一操作作用于不同的对象可以有不同的解释并产生不同的执行结果。在Java中，多态主要通过方法重写（Override）和接口实现达成。JVM在执行动态绑定时，通过对象的虚方法表（vtable）查找实际调用的方法入口，向上转型（Upcasting）后，编译期类型为父类，运行期类型为实际子类。',
    createdAt: '2026-09-10 10:20:15',
    updatedAt: '2026-09-10 10:22:30'
  },
  {
    id: 102,
    documentId: 1,
    documentName: '第3章_Java多态性与接口深度解析.pdf',
    chunkIndex: 2,
    heading: '3.2 向上转型与方法隐藏避坑指南',
    pageNo: 14,
    tokenCount: 310,
    charCount: 512,
    status: 'INDEXED',
    content: '需要特别注意的是：Java中的静态方法（static method）与成员变量（field）不支持动态绑定！当子类声明了与父类同名的静态方法或成员变量时，发生的是“隐藏”（Hiding）而非“重写”。通过父类引用调用静态方法时，执行的始终是父类定义的方法，这一点是高校期末考试与技术面试中的常见高频陷阱。',
    createdAt: '2026-09-10 10:20:16',
    updatedAt: '2026-09-10 10:22:32'
  },
  {
    id: 103,
    documentId: 1,
    documentName: '第3章_Java多态性与接口深度解析.pdf',
    chunkIndex: 3,
    heading: '3.3 抽象类与接口的设计权衡 (ISP原则)',
    pageNo: 18,
    tokenCount: 345,
    charCount: 560,
    status: 'INDEXED',
    content: '从架构设计视角来看，抽象类表达的是 "is-a" 的强血缘关系，适合代码复用与模板方法模式；而接口表达的是 "like-a" 或具备某种能力规范，契合接口隔离原则（Interface Segregation Principle）。Java 8 引入 default 方法后，接口获得了向后兼容演进的能力，但在多重继承冲突时仍须遵循类优先原则与显式重写规则。',
    createdAt: '2026-09-10 10:20:18',
    updatedAt: '2026-09-10 10:22:35'
  },
  {
    id: 104,
    documentId: 1,
    documentName: '第3章_Java多态性与接口深度解析.pdf',
    chunkIndex: 4,
    heading: '3.4 深入JVM invoke指令集辨析',
    pageNo: 22,
    tokenCount: 420,
    charCount: 680,
    status: 'INDEXED',
    content: 'JVM规范中与方法调用相关的5条字节码指令：invokevirtual（调用对象的虚方法）、invokeinterface（调用接口方法，需itable检索）、invokespecial（调用私有实例方法、构造器及父类super方法）、invokestatic（调用静态方法）、invokedynamic（动态类型绑定与Lambda表达式实现）。其中前两条指令是运行时多态分派的核心。',
    createdAt: '2026-09-10 10:20:20',
    updatedAt: '2026-09-10 10:22:38'
  },
  {
    id: 105,
    documentId: 2,
    documentName: '第4章_Java异常体系与最佳实践.docx',
    chunkIndex: 1,
    heading: '4.1 Throwable体系结构与受检异常分层',
    pageNo: 5,
    tokenCount: 260,
    charCount: 390,
    status: 'INDEXED',
    content: 'Java异常根类为Throwable，衍生出Error与Exception两大分支。Error表示严重的系统级故障（如OutOfMemoryError、StackOverflowError），应用程序通常不应尝试捕获。Exception分为RuntimeException（未受检异常）与受检异常（Checked Exception），受检异常强制编译器检查，要求开发者必须通过try-catch捕获或throws声明。',
    createdAt: '2026-09-10 11:15:00',
    updatedAt: '2026-09-10 11:18:10'
  },
  {
    id: 106,
    documentId: 2,
    documentName: '第4章_Java异常体系与最佳实践.docx',
    chunkIndex: 2,
    heading: '4.2 Try-with-resources与资源自动关闭机制',
    pageNo: 8,
    tokenCount: 295,
    charCount: 470,
    status: 'INDEXED',
    content: 'Java 7引入的try-with-resources语句要求所管理的资源对象必须实现AutoCloseable接口。无论代码正常结束还是抛出异常，所有声明的资源都会以创建时相反的顺序自动调用close()关闭。如果在关闭过程中抛出异常，将作为被抑制的异常（Suppressed Exception）附着在主异常后，彻底杜绝了传统finally中close容易吞掉主异常的问题。',
    createdAt: '2026-09-10 11:15:02',
    updatedAt: '2026-09-10 11:18:12'
  },
  {
    id: 107,
    documentId: 3,
    documentName: '高数期末总复习_核心定理集锦.pdf',
    chunkIndex: 1,
    heading: '第2章 罗尔定理与拉格朗日中值定理',
    pageNo: 3,
    tokenCount: 320,
    charCount: 510,
    status: 'INDEXED',
    content: '拉格朗日中值定理：若函数 f(x) 在闭区间 [a, b] 上连续，在开区间 (a, b) 内可导，则至少存在一点 ξ ∈ (a, b)，使得 f(b) - f(a) = f\'(ξ)(b - a)。几何意义为：连续光滑曲线弧上至少有一点的切线平行于连接两端点的割线。该定理是利用导数研究函数单调性、凹凸性及极值的核心理论工具。',
    createdAt: '2026-09-10 14:02:10',
    updatedAt: '2026-09-10 14:05:00'
  },
  {
    id: 108,
    documentId: 3,
    documentName: '高数期末总复习_核心定理集锦.pdf',
    chunkIndex: 2,
    heading: '第3章 柯西中值定理与泰勒展开公式',
    pageNo: 7,
    tokenCount: 380,
    charCount: 590,
    status: 'PENDING',
    content: '泰勒公式带有佩亚诺余项的形式是展开多项式近似的核心。当 x → x0 时，f(x) = f(x0) + f\'(x0)(x - x0) + ... + (f^(n)(x0)/n!)(x - x0)^n + o((x - x0)^n)。在处理0/0或∞/∞等复杂未定式极限时，结合常见函数的麦克劳林展开式往往比连续使用洛必达法则更加迅速且不容易出错。',
    createdAt: '2026-09-11 09:30:00',
    updatedAt: '2026-09-11 09:30:00'
  },
  {
    id: 109,
    documentId: 3,
    documentName: '高数期末总复习_核心定理集锦.pdf',
    chunkIndex: 3,
    heading: '第4章 不定积分计算换元与分部积分法',
    pageNo: 15,
    tokenCount: 270,
    charCount: 430,
    status: 'INDEX_FAILED',
    content: '分部积分法公式：∫u dv = uv - ∫v du。选取 u 的一般经验原则遵循“反三角、对数、幂函数、三角函数、指数函数”优先级（简称“反对幂三指”），排在前面的函数优先选为 u，排在后面的函数与 dx 结合组成 dv。',
    createdAt: '2026-09-11 09:30:05',
    updatedAt: '2026-09-11 09:31:20'
  }
];

export const mockChunkStats: ChunkStatsVO = {
  totalChunks: 128,
  indexedChunks: 124,
  pendingChunks: 3,
  failedChunks: 1,
  avgTokens: 318,
  totalTokens: 40704
};
