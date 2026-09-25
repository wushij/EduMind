import { describe, it, expect } from 'vitest';
import { formatStructuredProposal } from './structured-text';

describe('formatStructuredProposal', () => {
  it('formats user diagnosis text containing LaTeX formulas and Chinese numbers', () => {
    const raw = '一、偏差本质标准解的关键是把混合增量恒等拆分为两项，并分别配凑成 \\(x=0\\)处的导数定义：\\[\\frac{f(2x)-f(x)}{x} =\\frac{f(2x)-f(0)}{x}-\\frac{f(-x)-f(0)}{x} =2\\cdot \\frac{f(2x)-f(0)}{2x}+\\frac{f(-x)-f(0)}{-x}\\] 因此极限应为 \\(2f\'(0)+f\'(0)=3f\'(0)=6\\)。学生选 B，说明其未能完整识别两个增量项都要归约到同一个导数定义。 二、教学诊断与补救根本原因是“增量与分母同构”的导数定义变形训练不足，学生未形成将 \\(f(kx)-f(0)\\)与 \\(kx\\)成对配凑的意识。主要失分诱因代码：[ ]';

    const result = formatStructuredProposal(raw);

    // 应该生成两个结构化要点卡片
    expect(result).toContain('structured-point-card');
    expect(result).toContain('point-num-pill');
    expect(result).toContain('偏差本质：');
    expect(result).toContain('教学诊断与补救：');
    // 公式应该被 KaTeX 解析渲染出 katex 类
    expect(result).toContain('katex');
    // 应该剔除末尾残留的模型标记
    expect(result).not.toContain('主要失分诱因代码');
  });

  it('formats contiguous 1. 2. 3. numbered notification text into structured cards', () => {
    const raw = '1.失分归因与认知障碍诊断：阶段测试断层显示2名预警生对极限、导数、微分停留机械记忆，未建“变化率—局部线性—可微性”语义网络，前概念负迁移致连续、可导、可微混淆，布鲁姆“理解—分析”断裂。 2.靶向微课点拨：考点精讲攻坚微课以反例、动态图和双编码对比表，拆解“极限存在≠连续”“连续≠可导”“可导必可微”，引导绘概念地图并口头解释。 3.梯度变式训练：3题按辨认—辨析—迁移递进，分别判断真伪、多表征互译、构造反例解变化率；错因归因、变式改编、即时反馈。预计两周辨析正确率与阶段成绩提升。';
    const result = formatStructuredProposal(raw);

    expect(result).toContain('structured-point-card');
    expect(result.match(/structured-point-card/g)?.length).toBe(3);
    expect(result).toContain('point-num-pill');
    expect(result).toContain('失分归因与认知障碍诊断');
    expect(result).toContain('重点考点点拨');
    expect(result).toContain('梯度变式训练');
  });
});
