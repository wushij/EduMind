package com.edumind.ai.service.question.impl;

import com.alibaba.fastjson2.JSON;
import com.edumind.ai.dto.QuestionGenerateDTO;
import com.edumind.question.vo.question.QuestionVO;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 教学领域智能试题生成兜底引擎
 * 当外部大模型离线、超时或处于纯单机开发演示环境时，
 * 依据课程学科、章节与知识点，动态生成高质量、具真实教学诊断价值的结构化试题。
 */
@Component
public class PedagogicalQuestionFallbackEngine {

    public List<QuestionVO> generateHighQualityQuestions(
            QuestionGenerateDTO dto,
            String courseName,
            List<String> chapterNames,
            List<String> knowledgePointNames
    ) {
        List<QuestionVO> result = new ArrayList<>();
        int count = dto.getCount() != null && dto.getCount() > 0 ? dto.getCount() : 5;
        int scoreEach = dto.getScorePerQuestion() != null && dto.getScorePerQuestion() > 0 ? dto.getScorePerQuestion() : 5;
        List<String> types = (dto.getQuestionTypes() != null && !dto.getQuestionTypes().isEmpty())
                ? dto.getQuestionTypes()
                : List.of("SINGLE_CHOICE");

        String cName = (courseName != null && !courseName.isBlank()) ? courseName : "专业课程";
        List<String> kps = (knowledgePointNames != null && !knowledgePointNames.isEmpty())
                ? knowledgePointNames
                : extractDefaultKpsForCourse(cName);

        for (int i = 0; i < count; i++) {
            String type = types.get(i % types.size());
            String kp = kps.get(i % kps.size());
            QuestionVO vo = buildSingleQuestion(dto.getCourseId(), cName, kp, type, dto.getDifficulty(), scoreEach, i + 1, dto.getPromptDirective());
            result.add(vo);
        }
        return result;
    }

    private QuestionVO buildSingleQuestion(
            Long courseId,
            String courseName,
            String kp,
            String type,
            String difficulty,
            int score,
            int index,
            String promptDirective
    ) {
        QuestionVO vo = new QuestionVO();
        vo.setCourseId(courseId);
        vo.setType(type);
        vo.setScore(score);
        vo.setKnowledgePointName(kp);
        vo.setDifficulty(mapDifficultyLevel(difficulty));

        if (isAiOrLlmCourse(courseName, kp)) {
            fillAiQuestion(vo, kp, type, promptDirective, index);
        } else if (isJavaCourse(courseName, kp)) {
            fillJavaQuestion(vo, kp, type, promptDirective, index);
        } else if (isOsOrSysCourse(courseName, kp)) {
            fillOsQuestion(vo, kp, type, promptDirective, index);
        } else if (isMathCourse(courseName, kp)) {
            fillMathQuestion(vo, kp, type, promptDirective, index);
        } else {
            fillAlgorithmQuestion(vo, kp, type, promptDirective, index);
        }
        return vo;
    }

    private void fillJavaQuestion(QuestionVO vo, String kp, String type, String directive, int idx) {
        vo.setCognitiveLevel("APPLY");
        String lowerKp = kp.toLowerCase();
        if ("MULTIPLE_CHOICE".equals(type)) {
            if (lowerKp.contains("类加载") || lowerKp.contains("双亲委派") || lowerKp.contains(".class") || lowerKp.contains("javac")) {
                vo.setStem("关于「" + kp + "」在 Java 虚拟机 (JVM) 中的运行规范与底层工作机制，下列说法正确的有哪些？");
                vo.setOptions(JSON.toJSONString(List.of(
                        Map.of("key", "A", "content", "类加载的「连接」阶段在 JVM 规范中细分为验证 (Verification)、准备 (Preparation) 与解析 (Resolution) 三个子阶段", "isCorrect", true),
                        Map.of("key", "B", "content", "在准备阶段，JVM 仅为类变量（static 变量）分配内存并设置初始零值，而非执行代码显式赋值", "isCorrect", true),
                        Map.of("key", "C", "content", "双亲委派模型通过委派父类加载器优先尝试加载，确保了 Java 核心基础类库（如 java.lang.Object）的唯一性与安全性", "isCorrect", true),
                        Map.of("key", "D", "content", "自定义类加载器只要继承 ClassLoader 并重写 loadClass() 方法，就绝不可能打破双亲委派模型", "isCorrect", false)
                )));
                vo.setAnswer("A,B,C");
                vo.setAnalysis("【考点点拨】本题考查 JVM 类加载生命周期与双亲委派模型。选项 D 错误：自定义类加载器若重写 loadClass() 即可改变委派逻辑（Tomcat 破坏双亲委派即是此机制）；若想保留双亲委派，应重写 findClass()。");
                vo.setDistractorAnalysis("混淆项 D 混淆了 loadClass()（负责委派逻辑编排）与 findClass()（负责具体字节码读取查找）的职责差异。");
            } else if (lowerKp.contains("jit") || lowerKp.contains("编译") || lowerKp.contains("jvm") || lowerKp.contains("内存")) {
                vo.setStem("关于「" + kp + "」在现代 JVM 执行引擎架构中的工程优化实践，下列说法正确的有哪些？");
                vo.setOptions(JSON.toJSONString(List.of(
                        Map.of("key", "A", "content", "JVM 采用解释执行与 JIT 即时编译的混合模式，热点代码会被编译为本地机器指令并缓存", "isCorrect", true),
                        Map.of("key", "B", "content", "逃逸分析（Escape Analysis）可辅助 JIT 完成标量替换、栈上分配与锁消除等重要优化", "isCorrect", true),
                        Map.of("key", "C", "content", "分层编译（Tiered Compilation）将执行分为解释执行、C1 客户端编译与 C2 服务端编译等不同层级", "isCorrect", true),
                        Map.of("key", "D", "content", "JIT 即时编译在程序启动阶段即编译全部方法，因此不需要任何热点探测计数器", "isCorrect", false)
                )));
                vo.setAnswer("A,B,C");
                vo.setAnalysis("【考点点拨】本题考查 JVM JIT 编译与执行优化。选项 D 错误：JIT 依赖方法调用计数器和回边计数器在运行时探测热点代码，并非全量预先编译（AOT 才是提前编译）。");
                vo.setDistractorAnalysis("混淆项 D 误将 AOT 静态提前编译与 JIT 动态即时编译的触发机制混淆。");
            } else {
                vo.setStem("关于「" + kp + "」在 Java 核心特性与企业级开发中的规范与原理，下列说法正确的有哪些？");
                vo.setOptions(JSON.toJSONString(List.of(
                        Map.of("key", "A", "content", "若重写 equals() 方法，必须同时重写 hashCode() 方法以维护哈希容器中的契约一致性", "isCorrect", true),
                        Map.of("key", "B", "content", "ConcurrentHashMap 在 JDK 1.8 中采用 Node 数组 + 链表/红黑树并通过 CAS 与 synchronized 保障并发安全", "isCorrect", true),
                        Map.of("key", "C", "content", "Java 泛型采用类型擦除（Type Erasure）机制，在编译后泛型信息会被擦除为原始类型或边界类型", "isCorrect", true),
                        Map.of("key", "D", "content", "Java 中的深拷贝与浅拷贝无本质区别，引用对象属性始终会自动递归复制", "isCorrect", false)
                )));
                vo.setAnswer("A,B,C");
                vo.setAnalysis("【考点点拨】本题考查 Java 核心基础规范。选项 D 错误：浅拷贝仅复制引用地址，修改引用的内部对象会互相影响，深拷贝才会递归复制独立对象。");
                vo.setDistractorAnalysis("混淆项 D 忽视了 Java 对象引用的内存模型特征。");
            }
        } else if ("TRUE_FALSE".equals(type)) {
            vo.setStem("在 Java 虚拟机中，两个类即便来自同一个 .class 字节码文件，如果加载它们的 ClassLoader 实例不同，JVM 也绝不会判定它们是同一个 Class。");
            vo.setOptions(JSON.toJSONString(List.of(
                    Map.of("key", "A", "content", "正确", "isCorrect", true),
                    Map.of("key", "B", "content", "错误", "isCorrect", false)
            )));
            vo.setAnswer("A");
            vo.setAnalysis("【考点点拨】正确。JVM 中确定一个类的唯一性是由「类加载器」和「全限定类名」共同决定的。");
            vo.setDistractorAnalysis("容易误认为类的全限定名相同且字节码一致就代表同一个 Class。");
        } else if ("FILL_BLANK".equals(type)) {
            vo.setStem("在 Java 平台体系中，javac 编译器将编写的 .java 源码编译成与平台无关的字节码（____ 文件），再由不同操作系统上的 Java 虚拟机（JVM）解释执行或 JIT 编译，从而实现了“一次编写，到处运行”（WORA）。");
            vo.setOptions("[]");
            vo.setAnswer(".class");
            vo.setAnalysis("【考点点拨】javac 编译输出为 .class 字节码文件，通过统一的二进制指令格式与虚拟机实现跨平台解耦。");
        } else {
            // SINGLE_CHOICE
            if (lowerKp.contains("类加载") || lowerKp.contains("双亲委派") || lowerKp.contains(".class")) {
                vo.setStem("关于「" + kp + "」，在标准 JVM 运行时规范与类加载机制中，下列判定正确的是：");
                vo.setOptions(JSON.toJSONString(List.of(
                        Map.of("key", "A", "content", "启动类加载器 (Bootstrap ClassLoader) 通常由 C++ 实现，负责加载 <JAVA_HOME>/lib 路径下的核心类库", "isCorrect", true),
                        Map.of("key", "B", "content", "双亲委派模型强制要求所有子类加载器在加载前必须优先由自身直接加载字节码", "isCorrect", false),
                        Map.of("key", "C", "content", "应用程序类加载器 (AppClassLoader) 负责加载扩展目录 <JAVA_HOME>/lib/ext 中的 jar 包", "isCorrect", false),
                        Map.of("key", "D", "content", "一个类在同一个 JVM 进程中，不论被何种类加载器加载，hashCode 永远绝对一致", "isCorrect", false)
                )));
                vo.setAnswer("A");
                vo.setAnalysis("【考点点拨】本题考查 JVM 类加载体系。启动类加载器负责加载核心类库，无法被 Java 程序直接引用，故 A 正确。B 项违背了双亲委派的自底向上委托原则；C 项扩展类库由 Extension ClassLoader（或 JDK 9+ PlatformClassLoader）加载。");
                vo.setDistractorAnalysis("混淆项 B 误将委托机制逆转；混淆项 C 混淆了 AppClassLoader 与 Extension ClassLoader 的职责边界。");
            } else if (lowerKp.contains("jit") || lowerKp.contains("编译") || lowerKp.contains("jvm") || lowerKp.contains("wora")) {
                vo.setStem("关于「" + kp + "」，在 JVM 跨平台运行机制与字节码执行过程中，下列判定正确的是：");
                vo.setOptions(JSON.toJSONString(List.of(
                        Map.of("key", "A", "content", "Java 实现 WORA（一次编写，到处运行）的核心依赖于统一的字节码规范以及各平台适配的虚拟机实现", "isCorrect", true),
                        Map.of("key", "B", "content", "JIT 即时编译器会直接绕过字节码将 .java 源代码编译为机器指令", "isCorrect", false),
                        Map.of("key", "C", "content", "JVM 规范要求所有的 Java 代码必须且只能通过纯解释执行模式运行", "isCorrect", false),
                        Map.of("key", "D", "content", "Java 字节码只能在安装了 JDK 的机器上运行，JRE 无法执行字节码", "isCorrect", false)
                )));
                vo.setAnswer("A");
                vo.setAnalysis("【考点点拨】Java“一次编写到处运行”的本质是通过统一的 .class 字节码规范解耦语言与底层硬件，各大操作系统拥有各自平台的 JVM 实现，故 A 正确。JIT 编译的是字节码而非源码，故 B 错误；现代 JVM 是混合执行模式，故 C 错误；JRE 完全足以运行字节码，故 D 错误。");
                vo.setDistractorAnalysis("混淆项 B 混淆了 javac 编译与 JIT 即时编译的输入对象。");
            } else {
                vo.setStem("关于「" + kp + "」在 Java 面向对象程序设计中的核心机制，下列说法正确的是：");
                vo.setOptions(JSON.toJSONString(List.of(
                        Map.of("key", "A", "content", "多态机制在运行期通过虚方法表（vtable）实现动态分派与方法绑定", "isCorrect", true),
                        Map.of("key", "B", "content", "Java 支持多继承，一个子类可使用 extends 关键字直接继承多个父类", "isCorrect", false),
                        Map.of("key", "C", "content", "final 修饰的类其成员方法仍可被子类随意重写", "isCorrect", false),
                        Map.of("key", "D", "content", "接口中声明的所有方法在 Java 中都必须强制包含方法体实现", "isCorrect", false)
                )));
                vo.setAnswer("A");
                vo.setAnalysis("【考点点拨】Java 多态的核心是运行期动态绑定，JVM 底层利用方法表实现快速寻址，故 A 正确。Java 类单继承（B 错误）；final 类不可被继承更不可重写（C 错误）；接口普通方法无方法体（D 错误）。");
                vo.setDistractorAnalysis("混淆项 B 混淆了类单继承与接口多实现。");
            }
        }
    }

    private void fillAiQuestion(QuestionVO vo, String kp, String type, String directive, int idx) {
        vo.setCognitiveLevel("APPLY");
        if ("MULTIPLE_CHOICE".equals(type)) {
            vo.setStem("关于「" + kp + "」在大语言模型预训练与微调中的工程实践，下列说法正确的有哪些？");
            vo.setOptions(JSON.toJSONString(List.of(
                    Map.of("key", "A", "content", "LoRA 微调通过低秩分解显著减少需更新的参数量，保持预训练权重冻结", "isCorrect", true),
                    Map.of("key", "B", "content", "RoPE 旋转位置编码通过将绝对位置信息注入键值向量内积，具备相对位置外推潜力", "isCorrect", true),
                    Map.of("key", "C", "content", "多头自注意力机制中各头计算完全串行以保证注意力权重因果性", "isCorrect", false),
                    Map.of("key", "D", "content", "FlashAttention 通过优化 GPU SRAM 与 HBM 间的数据读写调度，有效降低显存 IO 瓶颈", "isCorrect", true)
            )));
            vo.setAnswer("A,B,D");
            vo.setAnalysis("【考点点拨】本题考查大模型参数高效微调与自注意力硬件加速原理。选项 C 错误，各 Attention Head 是并行计算的。");
            vo.setDistractorAnalysis("混淆项 C 误将自注意力的因果掩码（Causal Mask）实现与硬件并行计算机制混淆。");
        } else if ("TRUE_FALSE".equals(type)) {
            vo.setStem("在基于 RAG（检索增强生成）的知识库问答系统中，向量检索的 Top-K 召回结果得分越高，一定意味着 LLM 生成的最终答案事实准确率越高。");
            vo.setOptions(JSON.toJSONString(List.of(
                    Map.of("key", "A", "content", "正确", "isCorrect", false),
                    Map.of("key", "B", "content", "错误", "isCorrect", true)
            )));
            vo.setAnswer("B");
            vo.setAnalysis("【考点点拨】错误。向量检索基于语义相似度，可能召回相关但不包含核心答案的切片（噪声上下文），高分切片若存在事实冲突或误导性表述，反而会诱发大模型幻觉，因此通常需结合 Rerank 与上下文去噪过滤。");
            vo.setDistractorAnalysis("容易误认为向量余弦相似度分数与下游事实正确性成绝对正比。");
        } else if ("FILL_BLANK".equals(type)) {
            vo.setStem("在 Transformer 架构中，自注意力机制（Self-Attention）计算注意力权重的核心缩放点积公式为：$\\mathrm{Attention}(Q, K, V) = \\mathrm{softmax}\\left(\\frac{QK^T}{\\sqrt{d_k}}\\right)V$，其中分母除以 $\\sqrt{d_k}$ 的主要目的是防止点积结果过大导致 softmax 进入梯度____区。");
            vo.setOptions("[]");
            vo.setAnswer("饱和 / 极小 / 消失");
            vo.setAnalysis("【考点点拨】当维度 $d_k$ 较大时，向量内积方差增大，softmax 输出会趋向于 one-hot 分布，梯度趋近于 0，因此除以 $\\sqrt{d_k}$ 进行尺度缩放以稳定训练。");
            vo.setDistractorAnalysis("易错答为「爆炸」，注意 softmax 在极端输入下梯度趋于 0 是饱和与梯度消失。");
        } else if ("SHORT_ANSWER".equals(type)) {
            vo.setStem("简述在「" + kp + "」场景下，如何通过设计系统提示词（System Prompt）和结构化约束来抑制大语言模型产生幻觉，并给出不少于三项具体工程落地措施。");
            vo.setOptions("[]");
            vo.setAnswer("【参考要点】1. 角色与边界设定：明确规定仅基于提供的上下文资料回答，未知内容明确回复不知道；2. 引用溯源约束：要求回复必须标注参考文档片段与 Citation 编号；3. 输出格式化（JSON Schema / Few-Shot）：利用严格结构化输出抑制自由散漫发散；4. 引入置信度自检机制与温度系数（Temperature）调优（推荐设为 0.1~0.3）。");
            vo.setAnalysis("【采分标准】每点阐述清晰得 2~3 分，满分 10 分。结合 RAG 检索链路落地策略者酌情加分。");
            vo.setDistractorAnalysis("常见不足是仅提出简单调整 prompt 文案，缺乏系统工程与格式约束闭环。");
        } else {
            vo.setStem("在基于大语言模型的「" + kp + "」系统中，当输入上下文长度超出注意力窗口时，下列哪种方法可以在不重新全量微调的前提下最有效地扩充长文本外推能力？");
            vo.setOptions(JSON.toJSONString(List.of(
                    Map.of("key", "A", "content", "直接增大训练时的 batch_size 参数", "isCorrect", false),
                    Map.of("key", "B", "content", "采用基于 RoPE 的线性插值（Linear Interpolation）或 NTK-aware 插值技术", "isCorrect", true),
                    Map.of("key", "C", "content", "将输入文本中的所有停用词与标点符号强制剔除", "isCorrect", false),
                    Map.of("key", "D", "content", "将隐藏层激活函数从 SwiGLU 切换为标准 ReLU", "isCorrect", false)
            )));
            vo.setAnswer("B");
            vo.setAnalysis("【考点点拨】基于位置编码的插值方法（如 RoPE 线性插值、动态 NTK 插值）能够平滑拉伸位置频率，是目前工业界扩充上下文窗口的标准非重训方案。");
            vo.setDistractorAnalysis("混淆项 A 混淆了批处理规模与序列长度外推；混淆项 D 更换激活函数无法解决长距离注意力衰减。");
        }
    }

    private void fillOsQuestion(QuestionVO vo, String kp, String type, String directive, int idx) {
        vo.setCognitiveLevel("ANALYZE");
        if ("MULTIPLE_CHOICE".equals(type)) {
            vo.setStem("在现代操作系统中，关于「" + kp + "」涉及的并发与互斥控制机制，下列说法正确的有：");
            vo.setOptions(JSON.toJSONString(List.of(
                    Map.of("key", "A", "content", "互斥锁（Mutex）与信号量（Semaphore）均可实现临界资源的互斥访问", "isCorrect", true),
                    Map.of("key", "B", "content", "死锁产生的四个必要条件包括互斥、占有且等待、不可抢占和循环等待", "isCorrect", true),
                    Map.of("key", "C", "content", "只要破坏循环等待条件，即可从根本上预防死锁发生", "isCorrect", true),
                    Map.of("key", "D", "content", "自旋锁（Spinlock）适合保护执行耗时极长的大型临界区", "isCorrect", false)
            )));
            vo.setAnswer("A,B,C");
            vo.setAnalysis("【考点点拨】自旋锁在获取不到锁时会持续占用 CPU 忙等（Busy Waiting），因此仅适合短临界区，长时间持有会导致 CPU 算力极大浪费，故 D 项错误。");
            vo.setDistractorAnalysis("选项 D 混淆了自旋锁与休眠等待互斥量的使用场景边界。");
        } else if ("TRUE_FALSE".equals(type)) {
            vo.setStem("在虚拟内存分页管理系统中，发生缺页中断（Page Fault）后，CPU 将直接从用户态切换至内核态执行中断服务程序。");
            vo.setOptions(JSON.toJSONString(List.of(
                    Map.of("key", "A", "content", "正确", "isCorrect", true),
                    Map.of("key", "B", "content", "错误", "isCorrect", false)
            )));
            vo.setAnswer("A");
            vo.setAnalysis("【考点点拨】正确。缺页异常属于硬件触发的故障（Fault），CPU 会自动保存当前上下文并陷中断切换至内核态，调用操作系统的缺页处理例程将页面调入内存。");
            vo.setDistractorAnalysis("易误认为缺页处理可在用户态由 runtime 自行处理。");
        } else {
            vo.setStem("关于「" + kp + "」中的页面置换算法，在请求调页系统发生缺页时，下列算法中可能出现 Belady 异常（分配物理页面数增加但缺页次数反而增加）的是：");
            vo.setOptions(JSON.toJSONString(List.of(
                    Map.of("key", "A", "content", "LRU（最近最少使用）算法", "isCorrect", false),
                    Map.of("key", "B", "content", "FIFO（先进先出）算法", "isCorrect", true),
                    Map.of("key", "C", "content", "OPT（最佳置换）算法", "isCorrect", false),
                    Map.of("key", "D", "content", "LFU（最不经常使用）算法", "isCorrect", false)
            )));
            vo.setAnswer("B");
            vo.setAnalysis("【考点点拨】FIFO 算法基于进入内存的先后顺序淘汰页面，不满足栈式置换算法性质，可能出现 Belady 异常。而 LRU 和 OPT 均属于严格栈式算法（Stack Algorithm），绝不会发生 Belady 现象。");
            vo.setDistractorAnalysis("混淆项 A 错误将工业界常用的 LRU 认作具有 Belady 异常，应牢记栈式算法不受该异常影响。");
        }
    }

    private void fillMathQuestion(QuestionVO vo, String kp, String type, String directive, int idx) {
        vo.setCognitiveLevel("APPLY");
        if ("MULTIPLE_CHOICE".equals(type)) {
            vo.setStem("设函数 $f(x)$ 在点 $x_0$ 处可导，关于「" + kp + "」的微分性质，下列命题正确的有：");
            vo.setOptions(JSON.toJSONString(List.of(
                    Map.of("key", "A", "content", "$f(x)$ 在 $x_0$ 处必连续", "isCorrect", true),
                    Map.of("key", "B", "content", "若 $x_0$ 为 $f(x)$ 的极值点，则必有 $f'(x_0) = 0$", "isCorrect", true),
                    Map.of("key", "C", "content", "若 $f'(x_0) = 0$，则 $x_0$ 必为 $f(x)$ 的极值点", "isCorrect", false),
                    Map.of("key", "D", "content", "$\\lim_{\\Delta x \\to 0} \\frac{f(x_0+\\Delta x) - f(x_0-\\Delta x)}{2\\Delta x} = f'(x_0)$", "isCorrect", true)
            )));
            vo.setAnswer("A,B,D");
            vo.setAnalysis("【考点点拨】费马引理指出可导极值点的导数为 0（必要非充分），如 $f(x)=x^3$ 在 $x=0$ 处导数为 0 但不是极值点，故 C 错误。");
            vo.setDistractorAnalysis("混淆项 C 混淆了可导极值点的充分条件与必要条件。");
        } else {
            vo.setStem("求极限：$\\lim_{x \\to 0} \\frac{\\ln(1 + 2x) - \\sin(2x)}{x^2}$ 的值为：");
            vo.setOptions(JSON.toJSONString(List.of(
                    Map.of("key", "A", "content", "$-2$", "isCorrect", true),
                    Map.of("key", "B", "content", "$0$", "isCorrect", false),
                    Map.of("key", "C", "content", "$2$", "isCorrect", false),
                    Map.of("key", "D", "content", "$1$", "isCorrect", false)
            )));
            vo.setAnswer("A");
            vo.setAnalysis("【考点点拨】利用泰勒展开式：$\\ln(1+2x) = 2x - \\frac{(2x)^2}{2} + o(x^2) = 2x - 2x^2 + o(x^2)$，$\\sin(2x) = 2x + o(x^2)$。分子相减得 $-2x^2 + o(x^2)$，除以 $x^2$ 取极限得 $-2$。");
            vo.setDistractorAnalysis("若仅用一阶等价无穷小替换会导致分子抵消为 0，误选 B。此题必须展开至 $x^2$ 阶。");
        }
    }

    private void fillAlgorithmQuestion(QuestionVO vo, String kp, String type, String directive, int idx) {
        vo.setCognitiveLevel("APPLY");
        if ("MULTIPLE_CHOICE".equals(type)) {
            vo.setStem("关于「" + kp + "」在数据结构与算法中的性能特征与应用，下列说法正确的有：");
            vo.setOptions(JSON.toJSONString(List.of(
                    Map.of("key", "A", "content", "平衡二叉搜索树（如 AVL 树）在最坏情况下的查找时间复杂度为 $O(\\log n)$", "isCorrect", true),
                    Map.of("key", "B", "content", "哈希表使用链地址法解决冲突时，最坏情况时间复杂度可能退化为 $O(n)$", "isCorrect", true),
                    Map.of("key", "C", "content", "快速排序平均时间复杂度为 $O(n \\log n)$，且是一种稳定的排序算法", "isCorrect", false),
                    Map.of("key", "D", "content", "红黑树相比 AVL 树，在插入和删除频繁的场景下能够减少平衡旋转调整次数", "isCorrect", true)
            )));
            vo.setAnswer("A,B,D");
            vo.setAnalysis("【考点点拨】快速排序在元素交换过程中会改变相同关键字的相对顺序，属于不稳定排序，故 C 选项错误。");
            vo.setDistractorAnalysis("混淆项 C 忽略了排序算法稳定性定义，将快速排序误记为稳定排序。");
        } else if ("TRUE_FALSE".equals(type)) {
            vo.setStem("在单源最短路径算法中，Dijkstra 算法在图包含负权边（Negative Weight Edge）时仍能保证计算出正确的最短路径。");
            vo.setOptions(JSON.toJSONString(List.of(
                    Map.of("key", "A", "content", "正确", "isCorrect", false),
                    Map.of("key", "B", "content", "错误", "isCorrect", true)
            )));
            vo.setAnswer("B");
            vo.setAnalysis("【考点点拨】错误。Dijkstra 算法基于贪心策略，要求所有边的权重非负。存在负权边时应采用 Bellman-Ford 算法或 SPFA 算法。");
            vo.setDistractorAnalysis("容易忽略贪心策略前提是权值非负单调递增。");
        } else {
            if (kp != null && (kp.contains("排序") || kp.contains("sort") || kp.contains("复杂度"))) {
                vo.setStem("已知一组关键码序列为 $\\{18, 73, 10, 5, 68, 99, 27\\}$，若采用基数排序（Radix Sort）按关键字升序排序，关于其时间复杂度与空间复杂度，下列判定正确的是：");
                vo.setOptions(JSON.toJSONString(List.of(
                        Map.of("key", "A", "content", "时间复杂度为 $O(d(n+r))$，空间复杂度为 $O(n+r)$", "isCorrect", true),
                        Map.of("key", "B", "content", "时间复杂度为 $O(n \\log n)$，空间复杂度为 $O(1)$", "isCorrect", false),
                        Map.of("key", "C", "content", "时间复杂度为 $O(n^2)$，空间复杂度为 $O(n)$", "isCorrect", false),
                        Map.of("key", "D", "content", "时间复杂度为 $O(n \\log r)$，空间复杂度为 $O(\\log n)$", "isCorrect", false)
                )));
                vo.setAnswer("A");
                vo.setAnalysis("【考点点拨】设待排序元素个数为 $n$，关键字位数为 $d$，基数为 $r$（十进制 $r=10$）。每趟分配需 $O(n)$，收集需 $O(r)$，共 $d$ 趟，总时间复杂度为 $O(d(n+r))$，空间复杂度为分配所需的队列空间 $O(n+r)$。");
                vo.setDistractorAnalysis("混淆项 B 错误将其当做基于比较的排序（如堆排序或快排），基数排序是非比较分配排序。");
            } else {
                vo.setStem("关于「" + kp + "」在算法设计与数据结构中的核心性质，下列判定正确的是：");
                vo.setOptions(JSON.toJSONString(List.of(
                        Map.of("key", "A", "content", "采用合适的数据结构（如平衡树或哈希表）能够在保证操作正确性的前提下显著降低渐近时间复杂度", "isCorrect", true),
                        Map.of("key", "B", "content", "任何递归算法都可以不借助辅助数据结构直接等价改写为非递归算法", "isCorrect", false),
                        Map.of("key", "C", "content", "贪心算法在所有具有重叠子问题特性的场景下都能求得全局最优解", "isCorrect", false),
                        Map.of("key", "D", "content", "动态规划算法必须要求子问题相互独立才能应用状态转移方程", "isCorrect", false)
                )));
                vo.setAnswer("A");
                vo.setAnalysis("【考点点拨】本题考查数据结构与算法设计核心思想。数据结构的核心价值在于优化查找、插入等操作的渐近时间复杂度，故 A 正确。递归消除通常需要显式借助栈结构（B 错误）；贪心要求最优子结构与贪心选择性质（C 错误）；动态规划针对重叠子问题而非独立子问题（D 错误）。");
                vo.setDistractorAnalysis("混淆项 C 混淆了贪心算法与动态规划的适用前提条件。");
            }
        }
    }

    private Integer mapDifficultyLevel(String difficulty) {
        if ("EASY".equalsIgnoreCase(difficulty)) return 2;
        if ("HARD".equalsIgnoreCase(difficulty)) return 4;
        return 3;
    }

    private boolean isJavaCourse(String courseName, String kp) {
        String s = (courseName + " " + kp).toLowerCase();
        return s.contains("java") || s.contains("jvm") || s.contains("面向对象") || s.contains("oop")
                || s.contains("spring") || s.contains("类加载") || s.contains("双亲委派") || s.contains("垃圾回收")
                || s.contains("jit") || s.contains("javac") || s.contains("wora") || s.contains("并发");
    }

    private boolean isAiOrLlmCourse(String courseName, String kp) {
        String s = (courseName + " " + kp).toLowerCase();
        return s.contains("ai") || s.contains("llm") || s.contains("大模型") || s.contains("人工智能")
                || s.contains("机器学习") || s.contains("深度学习") || s.contains("transformer");
    }

    private boolean isOsOrSysCourse(String courseName, String kp) {
        String s = (courseName + " " + kp).toLowerCase();
        return s.contains("操作系统") || s.contains("os") || s.contains("体系结构") || s.contains("计算机组成")
                || s.contains("进程") || s.contains("内存") || s.contains("并发");
    }

    private boolean isMathCourse(String courseName, String kp) {
        String s = (courseName + " " + kp).toLowerCase();
        return s.contains("数学") || s.contains("微积分") || s.contains("导数") || s.contains("极限")
                || s.contains("代数") || s.contains("概率");
    }

    private List<String> extractDefaultKpsForCourse(String courseName) {
        String s = courseName.toLowerCase();
        if (s.contains("java") || s.contains("面向对象") || s.contains("jvm")) {
            return List.of("Java类加载过程与双亲委派模型", "从.java到.class的javac编译流程", "WORA跨平台与JVM架构", "JIT即时编译与解释执行混合模式", "JVM运行时数据区与垃圾回收");
        }
        if (s.contains("ai") || s.contains("大模型") || s.contains("机器学习")) {
            return List.of("Transformer自注意力机制", "LoRA参数高效微调", "RAG检索增强生成", "RoPE位置编码外推", "RLHF对齐训练");
        }
        if (s.contains("操作系统") || s.contains("os")) {
            return List.of("进程互斥与信号量", "虚拟内存分页与缺页异常", "死锁预防与死锁避免", "CPU调度算法与周转时间", "Linux虚拟文件系统");
        }
        if (s.contains("数学") || s.contains("微积分")) {
            return List.of("泰勒公式与极限展开", "拉格朗日中值定理", "复合函数链式求导", "定积分几何应用", "多元偏导数极值");
        }
        return List.of("平衡二叉树平衡调整", "哈希冲突解决与装填因子", "快速排序分治思想", "Dijkstra最短路径算法", "动态规划最优子结构");
    }
}
