import { describe, it, expect } from 'vitest';
import { normalizeLessonListMarkdown, renderLessonMarkdown } from './lesson-markdown';

describe('lesson-markdown', () => {
  it('normalizes glued bullet lines into separate list items', () => {
    const glued =
      '- 能够准确说出 JDK - 能够辨析源码 - 能够描述 Java 程序从 .java 源码经编译生成 .class 字节码';
    const normalized = normalizeLessonListMarkdown(glued);
    expect(normalized.split('\n').filter((l) => l.trim().startsWith('- '))).toHaveLength(3);
  });

  it('normalizes glued ATX headings (same pipeline as global AI)', () => {
    const html = renderLessonMarkdown('##一、课节导入\n\n段落内容。');
    expect(html).toMatch(/<h2[^>]*>[\s\S]*一、课节导入/);
    expect(html).not.toContain('##一');
  });

  it('splits inline section markers like 链路##5.1', () => {
    const md = '五、完整示例：Hello.java的编译与运行链路##5.1编写源码\n\n正文';
    const html = renderLessonMarkdown(md);
    expect(html).not.toMatch(/链路##/);
    expect(html).toMatch(/<h2[^>]*>[\s\S]*5\.1/);
  });

  it('keeps multiline Java in one fence when closing unclosed block', () => {
    const md = `\`\`\`java
public class Hello {
public static void main(String[] args) {
System.out.println("Hello, Java!");
}
}
5.2 编译源码在命令行执行`;
    const html = renderLessonMarkdown(md);
    expect(html).toContain('Hello');
    expect(html).toContain('function_">main');
    expect(html).toContain('<pre');
    const preCount = (html.match(/<pre/g) || []).length;
    expect(preCount).toBe(1);
    expect(html).toContain('<p>5.2 编译源码在命令行执行</p>');
  });

  it('renders learning objectives as ul instead of literal dash paragraphs', () => {
    const body = `- 能够准确说出 JDK、JRE、JVM 的全称与核心职责。
- 能够辨析源码、字节码与机器码的区别。`;
    const html = renderLessonMarkdown(body);
    expect(html).toContain('<ul>');
    expect(html).toContain('<li>');
    expect(html).not.toMatch(/<p>- 能够/);
  });

  // 以下四例取自真实 AI 讲义（模型把标题与正文写成同一行，渲染时会整段变成粗体大标题）
  it('splits a glued heading from the sentence that follows it', () => {
    const md =
      '###一、未定式判型：一切计算的起点极限计算的第一步不是求导，而是**准确写出未定式类型**。常见的七种未定式如下。';
    const html = renderLessonMarkdown(md);
    expect(html).toMatch(/<h3[^>]*>[\s\S]*一、未定式判型[\s\S]*<\/h3>/);
    // 正文的首句绝不能被留在标题里
    expect(html).not.toMatch(/<h3[^>]*>[\s\S]*一切计算的起点[\s\S]*<\/h3>/);
    expect(html).toMatch(/<p>[\s\S]*一切计算的起点/);
    // 标题尾部不应残留冒号
    expect(html).not.toMatch(/<h3[^>]*>[\s\S]*判型：[\s\S]*<\/h3>/);
  });

  it('keeps a short appositive after the colon inside the heading', () => {
    const md = '###二、洛必达法则：条件重于计算**定理（$\\dfrac{0}{0}$型）**设 $f,g$ 可导。';
    const html = renderLessonMarkdown(md);
    expect(html).toMatch(/<h3[^>]*>[\s\S]*二、洛必达法则：条件重于计算[\s\S]*<\/h3>/);
    expect(html).toMatch(/<p>[\s\S]*设[\s\S]*可导/);
  });

  it('splits a glued heading before a list marker and demotes un-splittable长句 to body', () => {
    const list = renderLessonMarkdown('####使用时的三条纪律- **每次使用前重新判型。**求导一次后必须重新判型。');
    expect(list).toMatch(/<h4[^>]*>[\s\S]*使用时的三条纪律[\s\S]*<\/h4>/);
    expect(list).not.toMatch(/<h4[^>]*>[\s\S]*每次使用前重新判型[\s\S]*<\/h4>/);

    // 找不到任何分界信号的长句：整行按正文渲染，不猜边界切出半截标题
    const long = renderLessonMarkdown(
      '###四、与两个重要极限的衔接两个重要极限是等价无穷小与洛必达法则的共同源头，必须能相互印证：'
    );
    expect(long).not.toContain('<h3');
    expect(long).toMatch(/<p>[\s\S]*与两个重要极限的衔接/);
  });

  it('keeps complete short headings that carry a closing bracket', () => {
    const html = renderLessonMarkdown('####3.2常用等价无穷小（$x\\to0$）\n\n正文内容。');
    expect(html).toMatch(/<h4[^>]*>[\s\S]*3\.2常用等价无穷小[\s\S]*<\/h4>/);
  });

  it('strips the lesson title echoed at the beginning of the body', () => {
    const body =
      '##1.2洛必达法则求未定式极限专项突破本课节围绕两条主线展开：**适用条件与使用边界**，以及等价无穷小代换的判定标准。';
    const html = renderLessonMarkdown(body, '1.2 洛必达法则求未定式极限专项突破');
    // 页面标题已展示课节标题，正文里不得再出现一次
    expect(html).not.toContain('<h2');
    expect(html).not.toMatch(/专项突破[\s\S]*本课节围绕/);
    expect(html).toMatch(/<p>[\s\S]*本课节围绕两条主线展开/);
  });

  it('strips the echoed title even when the stored title was truncated with an ellipsis', () => {
    const body = '##1.2洛必达法则求未定式极限专项突破本课节围绕两条主线展开：适用条件与使用边界。';
    const html = renderLessonMarkdown(body, '1.2 洛必达法则求未定式极…专项突破');
    expect(html).not.toContain('<h2');
    expect(html).toMatch(/<p>[\s\S]*本课节围绕两条主线展开/);
  });

  it('preserves ASCII box diagrams in code fences without mangling into tables', () => {
    const md = `\`\`\`
JVM 运行时数据区域（Runtime Data Area）物理划分：
+-------------------------------------------------------------------------+
|                              JVM 进程内存空间                            |
+-------------------------------------------------------------------------+
| [线程共享区域]                                                           |
|  1. 方法区 (Method Area / Metaspace) : 存储类元数据、常量池、静态变量    |
|  2. 堆内存 (Heap Space)              : 存储全量 new 出来的对象实例与数组 |
|     - 新生代 (Eden, Survivor S0, S1)                                    |
|     - 老年代 (Tenured / Old Gen)                                        |
+-------------------------------------------------------------------------+
\`\`\``;
    const html = renderLessonMarkdown(md);
    expect(html).toContain('<pre');
    expect(html).toContain('JVM 进程内存空间');
    // Must NOT be parsed into a <table>
    expect(html).not.toContain('<table');
  });

  it('splits inline numbered items before formulas into clean lines', () => {
    const md =
      '定理（$\\frac{0}{0}$型）设 $f, g$ 在 $x_0$ 的去心邻域内可导，且1. $\\lim_{x\\to x_0} f(x) = 0, \\lim_{x\\to x_0} g(x) = 0$; 2. $g\'(x) \\neq 0$;';
    const html = renderLessonMarkdown(md);
    expect(html).not.toMatch(/且1\.\s*\$/);
  });

  it('correctly splits glued headings like ###1.数列极限设数列 and ###2.函数极限函数极限关注', () => {
    const raw = `##一、极限的思想与基本定义极限是微积分的基础语言。本课节是第一章函数与极限论中的1.1节。

###1.数列极限设数列 \\{a_n\\}，若存在常数 A。

###2.函数极限函数极限关注自变量趋近某一点或趋近无穷时，函数值的变化趋势。

###3.极限存在的常用判别除定义外，本课节常用：`;

    const html = renderLessonMarkdown(raw);
    expect(html).toMatch(/<h2[^>]*>[\s\S]*一、极限的思想与基本定义[\s\S]*<\/h2>/);
    expect(html).toMatch(/<h3[^>]*>[\s\S]*1\.数列极限[\s\S]*<\/h3>/);
    expect(html).toMatch(/<h3[^>]*>[\s\S]*2\.函数极限[\s\S]*<\/h3>/);
    expect(html).toMatch(/<h3[^>]*>[\s\S]*3\.极限存在的常用判别[\s\S]*<\/h3>/);
    expect(html).toMatch(/<p>[\s\S]*设数列/);
    expect(html).toMatch(/<p>[\s\S]*函数极限关注/);
  });

  it('correctly splits ##一、课节定位与核心问题本课节位于第一章', () => {
    const raw =
      '##一、课节定位与核心问题本课节位于第一章“函数与极限论”，承接“1.1数列与函数极限计算”，主题是洛必达法则求未定式极限。';
    const html = renderLessonMarkdown(raw);
    expect(html).toMatch(/<h2[^>]*>[\s\S]*一、课节定位与核心问题[\s\S]*<\/h2>/);
    expect(html).not.toMatch(/<h2[^>]*>[\s\S]*本课节位于[\s\S]*<\/h2>/);
    expect(html).toMatch(/<p>[\s\S]*本课节位于第一章/);
  });

  it('preserves list dash when glued heading is followed by bullet item', () => {
    const raw = `###字节码的三个关键属性- **平台无关**：指令语义由 JVM规范统一定义，不绑定任何物理 CPU。
- **面向栈**：绝大多数指令操作的是**操作数栈**，而非寄存器，简化了指令编码。
- **紧凑**：单条指令多为1字节操作码加若干操作数，.class文件体积小，利于网络传输。`;
    const html = renderLessonMarkdown(raw);
    expect(html).toMatch(/<h3[^>]*>[\s\S]*字节码的三个关键属性[\s\S]*<\/h3>/);
    // 列表项必须渲染为 <ul><li>，包含「平台无关」
    expect(html).toContain('<ul>');
    expect(html).toMatch(/<li>[\s\S]*平台无关[\s\S]*<\/li>/);
    expect(html).toMatch(/<li>[\s\S]*面向栈[\s\S]*<\/li>/);
    expect(html).toMatch(/<li>[\s\S]*紧凑[\s\S]*<\/li>/);
  });

  it('splits section heading glued with prose statement starting with acronym subject', () => {
    const raw = `##二、JVM指令与栈帧执行模型JVM是“字节码的 CPU”，它的运算模型是**基于栈**的。每调用一个方法，JVM就为该方法分配一个**栈帧（Frame）**，栈帧中包含：`;
    const html = renderLessonMarkdown(raw);
    expect(html).toMatch(/<h2[^>]*>[\s\S]*二、JVM指令与栈帧执行模型[\s\S]*<\/h2>/);
    expect(html).not.toMatch(/<h2[^>]*>[\s\S]*字节码的 CPU[\s\S]*<\/h2>/);
    expect(html).toMatch(/<p>[\s\S]*JVM是“字节码的 CPU”/);
  });
});
