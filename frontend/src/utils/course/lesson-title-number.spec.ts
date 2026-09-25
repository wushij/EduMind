import { describe, expect, it } from 'vitest';
import {
  buildLessonTitle,
  ensureLessonTitle,
  hasLessonNoPrefix,
  normalizeLessonTitle,
  resolveNextLessonNo,
  stripLessonLabelPrefix
} from './lesson-title-number';

describe('hasLessonNoPrefix', () => {
  it('识别 1.2 / 3 形态的编号前缀', () => {
    expect(hasLessonNoPrefix('1.2 等价无穷小代换专项突破')).toBe(true);
    expect(hasLessonNoPrefix('1.2等价无穷小代换')).toBe(true);
    expect(hasLessonNoPrefix('2. 极限存在准则')).toBe(true);
  });

  it('不把裸数字开头的正常标题当成编号', () => {
    expect(hasLessonNoPrefix('3 个重要极限与等价无穷小替换')).toBe(false);
    expect(hasLessonNoPrefix('函数定义域、复合与反函数的结构解析')).toBe(false);
    expect(hasLessonNoPrefix('')).toBe(false);
  });
});

describe('stripLessonLabelPrefix', () => {
  it('剥离「课时1:」「微课 2、」这类标签', () => {
    expect(stripLessonLabelPrefix('课时1: JVM运行原理与跨平台机制')).toBe('JVM运行原理与跨平台机制');
    expect(stripLessonLabelPrefix('微课 2、平衡二叉搜索树旋转')).toBe('平衡二叉搜索树旋转');
  });
});

describe('resolveNextLessonNo', () => {
  it('已有 1.1 / 1.2 时续编为 1.3', () => {
    expect(resolveNextLessonNo(['1.1 数列与函数极限计算', '1.2 洛必达法则求未定式极限专项突破'])).toBe(3);
  });

  it('夹杂无编号课节时按数量兜底，不撞号', () => {
    expect(resolveNextLessonNo(['1.1 数列与函数极限计算', '函数定义域解析'])).toBe(3);
  });

  it('历史编号出现跳号时取最大值 +1', () => {
    expect(resolveNextLessonNo(['1.1 a', '1.4 b'])).toBe(5);
  });

  it('空章节从 1 开始', () => {
    expect(resolveNextLessonNo([])).toBe(1);
    expect(resolveNextLessonNo([undefined, ''])).toBe(1);
  });
});

describe('normalizeLessonTitle', () => {
  it('模型输出「课时1:」标签时改写成期望编号', () => {
    expect(normalizeLessonTitle('课时1: JVM运行原理与跨平台(WORA)核心机制', 1, 3)).toBe(
      '1.3 JVM运行原理与跨平台(WORA)核心机制'
    );
  });

  it('模型自带编号与期望不一致时以期望编号为准', () => {
    expect(normalizeLessonTitle('1.9 两个重要极限实战', 1, 3)).toBe('1.3 两个重要极限实战');
  });

  it('无编号标题直接补齐', () => {
    expect(normalizeLessonTitle('函数定义域与反函数结构解析', 2, 4)).toBe('2.4 函数定义域与反函数结构解析');
  });
});

describe('ensureLessonTitle', () => {
  it('尊重教师手工填写的编号', () => {
    expect(ensureLessonTitle('1.2 手写编号课节', 1, 3)).toBe('1.2 手写编号课节');
  });

  it('标题空缺时补上编号', () => {
    expect(ensureLessonTitle('洛必达法则求未定式极限', 2, 4)).toBe('2.4 洛必达法则求未定式极限');
    expect(ensureLessonTitle('', 1, 1)).toBe('1.1 未命名微课节');
  });
});

describe('buildLessonTitle', () => {
  it('拼装 章节号.序号 + 标题', () => {
    expect(buildLessonTitle('极限存在准则与函数连续性', 1, 4)).toBe('1.4 极限存在准则与函数连续性');
  });
});
