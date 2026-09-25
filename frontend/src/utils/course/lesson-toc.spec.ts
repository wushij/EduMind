import { describe, expect, it } from 'vitest';
import { buildLessonToc, extractCleanTitleFromElement, applyLessonTocFromDom } from './lesson-toc';

describe('buildLessonToc', () => {
  it('parses headings after same normalize as render (glued ##)', () => {
    const items = buildLessonToc([
      {
        type: 'markdown',
        body: '##三、核心概念\n\n###3.1 JDK\n\n正文'
      }
    ]);
    expect(items.map(i => i.title)).toEqual(['三、核心概念', '3.1 JDK']);
  });

  it('collects objective, markdown headings in order', () => {
    const items = buildLessonToc([
      {
        type: 'callout',
        variant: 'objective',
        title: '学习目标',
        body: '- a'
      },
      {
        type: 'markdown',
        body: '## 一、JDK 体系\n\n### 1.1 组成\n\n正文'
      }
    ]);
    expect(items.map(i => i.title)).toEqual(['学习目标', '一、JDK 体系', '1.1 组成']);
    expect(items[1].level).toBe(2);
    expect(items[2].level).toBe(3);
  });

  it('parses user raw markdown correctly into 1.数列极限, 2.函数极限, 3.极限存在的常用判别', () => {
    const raw = `##一、极限的思想与基本定义极限是微积分的基础语言。本课节是第一章函数与极限论中的1.1节。

###1.数列极限设数列 \\{a_n\\}，若存在常数 A。

###2.函数极限函数极限关注自变量趋近某一点或趋近无穷时，函数值的变化趋势。

###3.极限存在的常用判别除定义外，本课节常用：`;

    const items = buildLessonToc([{ type: 'markdown', body: raw }]);
    expect(items.map(i => i.title)).toEqual([
      '一、极限的思想与基本定义',
      '1.数列极限',
      '2.函数极限',
      '3.极限存在的常用判别'
    ]);
  });

  it('parses second user lecture with 一、课节定位与核心问题 and 2.1, 2.2, 2.3', () => {
    const raw = `##一、课节定位与核心问题本课节位于第一章“函数与极限论”，承接“1.1数列与函数极限计算”，主题是洛必达法则。

##二、洛必达法则：条件、判定与书写规范###2.1基本形式设函数 $f(x)$、$g(x)$在点 $x_0$的去心邻域内可导。

###2.2三个使用前提第一，先判型。

###2.3书写流程建议固定为：

##三、等价无穷小代换：定义与边界###3.1严格定义若极限为1。`;

    const items = buildLessonToc([{ type: 'markdown', body: raw }]);
    expect(items.map(i => i.title)).toEqual([
      '一、课节定位与核心问题',
      '二、洛必达法则：条件、判定与书写规范',
      '2.1基本形式',
      '2.2三个使用前提',
      '2.3书写流程建议固定为',
      '三、等价无穷小代换：定义与边界',
      '3.1严格定义'
    ]);
  });
});

describe('extractCleanTitleFromElement & applyLessonTocFromDom', () => {
  it('correctly extracts clean LaTeX from rendered KaTeX DOM without text duplication', () => {
    const container = document.createElement('div');
    // 模拟 KaTeX 渲染生成的 DOM 结构（含 MathML 和 HTML 视图）
    container.innerHTML = `
      <h3>例2（<span class="katex"><span class="katex-mathml"><math><semantics><mrow><msup><mn>1</mn><mi mathvariant="normal">∞</mi></msup></mrow><annotation encoding="application/x-tex">1^{\\infty}</annotation></semantics></math></span><span class="katex-html"><span class="base">1<span class="msupsub">∞</span></span></span></span>型，先取对数）</h3>
    `;
    const heading = container.querySelector('h3') as HTMLElement;
    const cleanTitle = extractCleanTitleFromElement(heading);
    expect(cleanTitle).toBe('例2（$1^{\\infty}$型，先取对数）');
  });

  it('extracts strong heading from pseudo-heading paragraph without sucking full prose', () => {
    const container = document.createElement('div');
    container.innerHTML = `
      <p><strong>四、与两个重要极限的衔接</strong>两个重要极限是微积分的基础，特别是在处理未定式时...</p>
    `;
    const p = container.querySelector('p') as HTMLElement;
    const cleanTitle = extractCleanTitleFromElement(p);
    expect(cleanTitle).toBe('四、与两个重要极限的衔接');
  });

  it('applies toc from dom with proper levels for headings and examples', () => {
    const root = document.createElement('div');
    root.innerHTML = `
      <div class="lesson-block lesson-block--markdown">
        <div class="markdown-body">
          <h2>五、例题精讲</h2>
          <p><strong>例1（0/0型）</strong>这里是解题步骤说明...</p>
        </div>
      </div>
    `;
    const toc = applyLessonTocFromDom(root);
    expect(toc).toHaveLength(2);
    expect(toc[0].title).toBe('五、例题精讲');
    expect(toc[0].level).toBe(2);
    expect(toc[1].title).toBe('例1（0/0型）');
    expect(toc[1].level).toBe(3);
  });

  it('recognizes theorems with formulas as toc items and trims trailing body sentence', () => {
    const root = document.createElement('div');
    root.innerHTML = `
      <div class="lesson-block lesson-block--markdown">
        <div class="markdown-body">
          <h2>二、洛必达法则：条件先于计算</h2>
          <p><strong>定理（$0/0$型）</strong>设 $f, g$ 在 $x_0$ 的去心邻域内可导，且条件满足...</p>
        </div>
      </div>
    `;
    const toc = applyLessonTocFromDom(root);
    expect(toc).toHaveLength(2);
    expect(toc[0].title).toBe('二、洛必达法则：条件先于计算');
    expect(toc[1].title).toBe('定理（$0/0$型）');
    expect(toc[1].level).toBe(3);
  });

  it('recognizes numbered sections 1, 2, 3 together even if 1 and 2 lack bold tags', () => {
    const root = document.createElement('div');
    root.innerHTML = `
      <div class="lesson-block lesson-block--markdown">
        <div class="markdown-body">
          <h2>一、极限的思想与基本定义</h2>
          <p>1. 数列极限设数列 {an}，若存在常数 A...</p>
          <p>2. 函数极限函数极限关注自变量趋近某一点...</p>
          <p><strong>3. 极限存在的常用判别除定义外，本节常用</strong></p>
        </div>
      </div>
    `;
    const toc = applyLessonTocFromDom(root);
    expect(toc).toHaveLength(4);
    expect(toc[0].title).toBe('一、极限的思想与基本定义');
    expect(toc[1].title).toBe('1. 数列极限');
    expect(toc[2].title).toBe('2. 函数极限');
    expect(toc[3].title).toBe('3. 极限存在的常用判别');
  });
});
