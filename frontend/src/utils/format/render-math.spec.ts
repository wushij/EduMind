import { describe, expect, it } from 'vitest';
import { normalizeMathTextNewlines, renderMathText } from './render-math';

describe('renderMathText', () => {
  it('turns paragraph breaks into br, single \\n into spaces', () => {
    const html = renderMathText('第一段\\n\\n第二段 $x^2$');
    expect(html).toContain('<br /><br />');
    expect(html).not.toContain('\\n');
    expect(html).toContain('katex');
    const oneLine = renderMathText('行A\\n行B');
    expect(oneLine).toContain('行A 行B');
    expect(oneLine).not.toContain('<br />');
  });

  it('repairs form-feed corrupted \\frac from bad JSON escaping', () => {
    const corrupted = '当 $x \\to 0$ 时，$1-\\cos x \\sim \u000Crac{1}{2}x^2$。';
    const html = renderMathText(corrupted);
    expect(html).toContain('katex');
    expect(html).toContain('\\frac{1}{2}');
  });

  it('renders block math delimited by \\[ \\]', () => {
    const html = renderMathText('故\\[x^{2}\\sin x\\sim x^{3}\\]原式');
    expect(html).toContain('math-block');
    expect(html).toContain('katex');
  });

  it('keeps gaokao-style analysis on one flowing line without spurious breaks', () => {
    const analysis =
      '根据等价无穷小基本公式，当 $x \\to 0$ 时，$\\ln(1+x) \\sim x$；而 $\\sin 2x \\sim 2x$，$1-\\cos x \\sim \\frac{1}{2}x^2$。故正确答案为 B。';
    const html = renderMathText(analysis);
    expect(html).not.toContain('<br />');
    expect(html).toContain('katex');
    expect(html).toContain('故正确答案为 B');
  });

  it('repairs double-escaped LaTeX commands and prevents false linebreaks', () => {
    const doubleEscaped =
      '根据等价无穷小基本公式，当 $x \\to 0$ 时，\\\\ln(1+x) \\sim x；而 \\\\sin 2x \\sim 2x，$1-\\\\cos x \\sim \\\\frac{1}{2}x^2$。故正确答案为 B。';
    const html = renderMathText(doubleEscaped);
    expect(html).not.toContain('<br />');
    expect(html).not.toContain('newline');
    expect(html).toContain('mfrac');
    expect(html).toContain('故正确答案为 B');
  });

  it('renders bare LaTeX commands without dollar signs into katex html', () => {
    const rawAnalysis =
      '根据等价无穷小基本公式，当 $x \\to 0$ 时，\\ln(1+x) \\sim x；而 \\sin 2x \\sim 2x，$1-\\cos x \\sim \\frac{1}{2}x^2$。故正确答案为 B。';
    const html = renderMathText(rawAnalysis);
    expect(html).toContain('katex');
    expect(html).toContain('故正确答案为 B');
    // 验证 \ln(1+x) 和 \sin 2x 也被渲染成了 katex
    expect(html.match(/class="katex"/g)?.length).toBeGreaterThanOrEqual(4);
    });

    it('renders bare plain-text formulas without dollar signs', () => {
    expect(renderMathText('e^6')).toContain('katex');
    expect(renderMathText('2^h - 1')).toContain('katex');
    const parametric = renderMathText('dy/dx = 3(t^2+1)/(2t), d^2y/dx^2 = 3(t^2-1)/(4t^3)');
    expect(parametric).toContain('katex');
    expect(parametric).not.toContain('katex-error');
    // 整段被公式化渲染，不应再以纯文本形式裸出
    expect(parametric.startsWith('<span class="katex">')).toBe(true);
    });

    it('renders bare math expressions embedded in AI Chinese grading comments', () => {
      const comment =
        '遗漏采分项：-分母处理：x^2(e^x-1)中 e^x-1 ~ x，故分母等价于 x^3；-分子处理：x - sin x需用泰勒展开，sin x = x - x^3/6 + o(x^3)，所以 x - sin x = x^3/6 + o(x^3)；-约分求极限：原式 = (x^3/6 + o(x^3))/x^3 ->1/6；尤其是 sin x、e^x、ln(1+x)等。';
      const html = renderMathText(comment);
      expect(html).toContain('katex');
      // 验证至少识别出多个公式
      const matches = html.match(/class="katex"/g);
      expect(matches).not.toBeNull();
      expect(matches!.length).toBeGreaterThanOrEqual(6);
    });

    it('renders markdown bold markers *** and ** into strong tags without raw asterisks', () => {
      const comment =
        '**采分点对照：** - **求导并分析单调性：**未命中。应求出 $f\'(x) = 6x^2 - 18x + 12$。- **极值：**未命中。';
      const html = renderMathText(comment);
      expect(html).not.toContain('**');
      expect(html).toContain('<strong>采分点对照：</strong>');
      expect(html).toContain('<strong>求导并分析单调性：</strong>');
      expect(html).toContain('<strong>极值：</strong>');
      expect(html).toContain('katex');
    });

    it('does not treat Chinese prose, English sentences or code as bare formulas', () => {
      expect(renderMathText('时间复杂度最坏为O(n^2)')).not.toContain('katex');
      expect(renderMathText('The value of x^2 is 4')).not.toContain('katex');
      expect(renderMathText('\\d{3}-\\d{4}')).not.toContain('katex');
    });

    it('renders plain-text math inside AI teaching advice into katex html', () => {
      // 复现 AI 教学策略建议里的真实句子：模型未用 $ 包裹公式时也必须排版出来
      expect(renderMathText('重点识别 lim(1+a/x)^(bx)=e^(ab)。')).toContain('katex');
      expect(renderMathText('让学生比较 sin2x、1-cosx、e^x-1-x 与 x 的比值极限')).toContain('katex');
    });
  });

describe('normalizeMathTextNewlines', () => {
  it('converts escaped newlines when not a LaTeX command', () => {
    expect(normalizeMathTextNewlines('第一段\\n第二段')).toBe('第一段\n第二段');
  });

  it('does not break LaTeX commands starting with \\n', () => {
    expect(normalizeMathTextNewlines('$x \\neq y$')).toBe('$x \\neq y$');
    expect(normalizeMathTextNewlines('\\not')).toBe('\\not');
  });

  it('repairs double backslashes before LaTeX command words', () => {
    expect(normalizeMathTextNewlines('\\\\frac{1}{2}')).toBe('\\frac{1}{2}');
    expect(normalizeMathTextNewlines('\\\\cos x')).toBe('\\cos x');
    expect(normalizeMathTextNewlines('\\\\sin 2x')).toBe('\\sin 2x');
  });
});
