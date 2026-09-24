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
  });
});
