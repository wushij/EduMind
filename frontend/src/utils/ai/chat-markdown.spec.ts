import { describe, it, expect } from 'vitest';
import { renderChatMarkdown, normalizeChatMarkdown, repairChatBoldMarkers } from './chat-markdown';
import { renderMarkdownForChat } from '@/utils/markdown';

/** 复现截图场景：同一代码块内连续 3 个类，且代码里有中文注释 */
const MULTI_CLASS_SAMPLE = `核心机制是：**编译时看引用类型，运行时看实际对象类型**。

例如：

\`\`\`java
class Animal {
    void speak() {
        System.out.println("Animal");
    }
}

class Dog extends Animal {
    @Override void speak() {
        System.out.println("Dog");
    }
}

public class Test {
    public static void main(String[] args) {
        Animal a = new Dog();
        a.speak();//输出 Dog
    }
}
\`\`\`

执行过程可以理解为：父类引用指向子类对象。`;

/** 模型漏写闭合围栏、正文被吞进代码块的情况，仍需被拆出来 */
const UNCLOSED_SAMPLE = `\`\`\`java
class Animal {
    void speak() {
    }
}

## 执行过程说明

上述代码演示了多态的基本用法与动态绑定机制。`;

/** 去掉高亮 span 等标签，只看可见文本 */
const plainText = (html: string) => html.replace(/<[^>]*>/g, '');

/** 取第一个代码块内部的可见文本 */
const codeText = (html: string) => {
  const start = html.indexOf('<pre');
  const end = html.indexOf('</pre>');
  return start < 0 || end < 0 ? '' : plainText(html.slice(start, end));
};

describe('chat markdown 代码块边界', () => {
  it('同一代码块内的多个类应保持完整，不被当作正文甩出去', () => {
    const html = renderChatMarkdown(MULTI_CLASS_SAMPLE);

    expect(html.match(/<pre/g)?.length ?? 0).toBe(1);

    const code = codeText(html);
    expect(code).toContain('class Animal');
    expect(code).toContain('class Dog');
    expect(code).toContain('public class Test');

    // 正文必须留在代码块之外，且没有被丢弃
    expect(code).not.toContain('执行过程可以理解为');
    expect(plainText(html)).toContain('执行过程可以理解为');
  });

  it('漏写闭合围栏时，标题正文仍应被拆出代码块', () => {
    const html = renderChatMarkdown(UNCLOSED_SAMPLE);

    expect(codeText(html)).toContain('class Animal');
    expect(codeText(html)).not.toContain('执行过程说明');
    expect(plainText(html)).toContain('执行过程说明');
  });

  it('模型未加美元符的极限公式应正常渲染为 KaTeX 而不是红色报错', async () => {
    const input1 = `例如：

\\lim_{n\\to\\infty}\\left(\\frac{1}{\\sqrt{n^2+1}}+\\frac{1}{\\sqrt{n^2+2}}+\\cdots+\\frac{1}{\\sqrt{n^2+n}}\\right)`;
    const html1 = renderChatMarkdown(input1);
    expect(html1).toContain('katex');
    expect(html1).not.toContain('katex-error');

    const input2 = `第二个重要极限：

\\lim_{x\\to\\infty}\\left(1+\\frac1x\\right)^x=e`;
    const html2 = renderChatMarkdown(input2);
    expect(html2).toContain('katex');
    expect(html2).not.toContain('katex-error');

    const input6 = `所以原式变成：

$$
\\lim_{n\\to\\infty}
\\frac{3+\\frac{2}{n}+\\frac{1}{n^2}}
{2 - \\frac{5}{n^2}}
$$`;
    const html6 = renderChatMarkdown(input6);
    expect(html6).toContain('katex');
    expect(html6).not.toContain('katex-error');

    const input7 = `由于分母极限是 2，不是 0，所以可以直接使用极限的四则运算法则：

$$
\\lim_{n\\to\\infty}
\\frac{3+\\frac{2}{n}+\\frac{1}{n^2}}
{2 - \\frac{5}{n^2}} = \\frac{3}{2}
$$

所以：

$$\\boxed{
\\lim_{n\\to\\infty}\\frac{3n^2+2n+1}{2n^2-5}=\\frac{3}{2}
}$$`;
    const html7 = renderChatMarkdown(input7);
    expect(html7).toContain('katex');
    expect(html7).not.toContain('katex-error');

    // 修复大模型误输出的非法数字宏（如 \0 \cdot \infty、\1^\infty、\0^0）
    const input8 = `• **未定式**：进入变换层。七种基本型：
$$\\frac{0}{0}, \\frac{\\infty}{\\infty}, \\infty - \\infty, \\0 \\cdot \\infty, \\1^\\infty, \\0^0, \\infty^0.$$`;
    const html8 = renderChatMarkdown(input8);
    expect(html8).toContain('katex');
    expect(html8).not.toContain('katex-error');
    expect(html8).not.toContain('\\0');
    expect(html8).not.toContain('\\1');
  });

  it('副标题连字符与未闭合括号内部不应被拆行为列表项', () => {
    // 题目一/二（数列极限 - 四则运算与同除最高阶）必须保持单行，绝不能拆成 <li>
    const inputQuestion1 = `题目一（数列极限 - 四则运算与同除最高阶）

求极限：
$$\\lim_{n\\to\\infty} \\frac{5n^3 - 2n^2 + 1}{2n^3 + 7n - 4}$$

考查点：数列极限的四则运算、无穷小性质、分子分母同除最高阶。`;

    const html1 = renderChatMarkdown(inputQuestion1);
    expect(html1).not.toContain('<li>四则运算与同除最高阶');
    expect(html1).toContain('题目一（数列极限 - 四则运算与同除最高阶）');

    // Markdown 标题行同理
    const inputHeading = `### 题目二（等价无穷小 - 加减结构需谨慎）`;
    const htmlHeading = renderChatMarkdown(inputHeading);
    expect(htmlHeading).not.toContain('<li>加减结构需谨慎');
    expect(htmlHeading).toContain('等价无穷小 - 加减结构需谨慎');

    // 中文人名间隔号 · 不应被误拆
    const inputName = `计算机之父：约翰·冯·诺依曼与图灵`;
    const htmlName = renderChatMarkdown(inputName);
    expect(htmlName).not.toContain('<li>冯');

    // 真正同一行粘连的无序列表项应正常拆分
    const inputList = `考查重点如下：- 极限四则运算 - 无穷小性质比较`;
    const htmlList = renderChatMarkdown(inputList);
    expect(htmlList).toContain('<li>极限四则运算</li>');
    expect(htmlList).toContain('<li>无穷小性质比较</li>');

    const sampleNoBold = `易错提醒1. 数列极限同除最高阶时，要同时处理分子分母，不能只除一边。 2.等价无穷小替换在乘除结构中较安全，在加减结构中要谨慎，通常需要先变形或提取公因式。 3.洛必达法则使用前必须验证未定式类型，并且要满足可导、分母导数不为零等条件。 4.若求导后极限仍为未定式，可以继续洛必达，但要注意每一步都成立。`;
    const htmlNoBold = renderChatMarkdown(sampleNoBold);
    expect(htmlNoBold).toContain('<ol>');
    expect(htmlNoBold).toContain('<li>');
  });

  it('自动拆分标题末尾粘连的无序列表、多级标题及正文引导句', () => {
    // 1. 标题粘连无序列表第一项（用户实际场景）
    const rawGluedList = `##八、易错点提醒- **不是未定式不能用洛必达**；
- **每用一次洛必达，都要重新判断是否仍是未定式**；
- **洛必达后极限不存在，不能说明原极限不存在**；`;
    const htmlList = renderChatMarkdown(rawGluedList);
    expect(htmlList).toContain('<h2>八、易错点提醒</h2>');
    expect(htmlList).toContain('<li><strong>不是未定式不能用洛必达</strong>；</li>');
    expect(htmlList).toContain('<li><strong>每用一次洛必达，都要重新判断是否仍是未定式</strong>；</li>');

    // 2. 二级标题粘连三级标题与正文
    const rawGluedHeadings = `##三、洛必达法则###1.法则内容若极限存在
代入分析。`;
    const htmlHeadings = renderChatMarkdown(rawGluedHeadings);
    expect(htmlHeadings).toContain('<h2>三、洛必达法则</h2>');
    expect(htmlHeadings).toContain('<h3>1. 法则内容</h3>');
    expect(htmlHeadings).toMatch(/<p>若极限存在[\s\S]*代入分析。<\/p>/);

    // 3. 标题以经典收尾词（注意事项）结尾粘连正文
    const rawGluedProse = `##四、洛必达法则的注意事项洛必达法则很强，但不是万能的。`;
    const htmlProse = renderChatMarkdown(rawGluedProse);
    expect(htmlProse).toContain('<h2>四、洛必达法则的注意事项</h2>');
    expect(htmlProse).toContain('<p>洛必达法则很强，但不是万能的。</p>');

    // 4. 标题粘连假设正文引导词（如果不是）
    const rawGluedCondition = `###1.必须验证未定式类型如果不是 0/0 或无穷比无穷，不能随便使用。`;
    const htmlCondition = renderChatMarkdown(rawGluedCondition);
    expect(htmlCondition).toContain('<h3>1. 必须验证未定式类型</h3>');
    expect(htmlCondition).toContain('<p>如果不是 0/0 或无穷比无穷，不能随便使用。</p>');

    // 5. 普通副标题不被误拆（如「模块一 - 基础篇」）
    const rawNormalSub = `## 模块一 - 基础篇`;
    const htmlNormalSub = renderChatMarkdown(rawNormalSub);
    expect(htmlNormalSub).toContain('<h2>模块一 - 基础篇</h2>');
    expect(htmlNormalSub).not.toContain('<li>');
  });
});


