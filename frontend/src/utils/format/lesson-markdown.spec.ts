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
});
