import { describe, expect, it } from 'vitest';
import {
  mergeServerWithLocalDrafts,
  generateSmartFollowUps,
  extractKeyTopicsFromContent,
  normalizeFollowUpPrompts,
  requestFollowUps,
  buildSessionTitleFromPrompt,
  type ChatMessage
} from './stream-service';

describe('mergeServerWithLocalDrafts', () => {
  it('does not duplicate user prompts when local has persona/chapter prefixes and server does not', () => {
    const server: ChatMessage[] = [
      {
        id: '1',
        role: 'user',
        content: '这个章节的内容是学什么的',
        createdAt: '14:59'
      },
      {
        id: '2',
        role: 'assistant',
        content: '本章主要讲解 JVM 体系。',
        createdAt: '15:00'
      }
    ];
    const local: ChatMessage[] = [
      {
        id: 'user_local',
        role: 'user',
        content:
          '[助教风格: 苏格拉底启发型，善用反问引导、步步启发自主思考] [针对课时: Java技术体系与JDK/JRE/JVM职责边界辨析] 这个章节的内容是学什么的',
        createdAt: '19:31'
      },
      {
        id: 'ai_local',
        role: 'assistant',
        content: '本章主要讲解 JVM 体系。',
        createdAt: '19:32'
      }
    ];

    const merged = mergeServerWithLocalDrafts(server, local);
    const userMsgs = merged.filter((m) => m.role === 'user');
    expect(userMsgs).toHaveLength(1);
    expect(userMsgs[0].id).toBe('1');
  });

  it('extracts key topics from markdown headings and generates specific follow-ups', () => {
    const query = '请结合大纲为我梳理【Java学习】的核心知识图谱架构';
    const answer = `
## 1. JDK、JRE与JVM的包含关系
JDK包含开发工具与JRE，JRE包含JVM与核心类库。

## 2. 字节码跨平台机制与WORA
Java源码通过 javac 编译成 .class 字节码。
`;
    const prompts = generateSmartFollowUps(query, answer);
    expect(prompts).toHaveLength(3);
    expect(prompts[0]).toContain('JDK、JRE与JVM的包含关系');
    expect(prompts[1]).toContain('字节码跨平台机制与WORA');
  });

  it('falls back to subject-appropriate prompts when no anchor can be extracted', () => {
    const prompts = generateSmartFollowUps('请为我讲解这个算法的时间复杂度');

    expect(prompts).toHaveLength(3);
    expect(prompts.join('')).not.toMatch(/企业级|工业级/);
  });

  it('never anchors follow-ups on narrative sentences (regression: 「它有没有一个稳定目标？」)', () => {
    const answer = [
      '## 一、底层原理：极限是“误差可控的稳定趋势”',
      '**它有没有一个稳定目标？**',
      '**极限唯一**：趋势只能稳定到一个目标。'
    ].join('\n');

    const topics = extractKeyTopicsFromContent('请结合 1.1 节讲透极限', answer);
    const prompts = generateSmartFollowUps('请结合 1.1 节讲透极限', answer);

    expect(topics).not.toContain('它有没有一个稳定目标');
    expect(prompts).toHaveLength(3);
    expect(prompts.join('')).not.toMatch(/它有没有一个稳定目标/);
  });

  it('keeps math follow-ups free of engineering / code wording', () => {
    const prompts = generateSmartFollowUps(
      '极限到底怎么理解',
      '极限、收敛、导数与数列的性质讲解。'.repeat(4)
    );

    expect(prompts).toHaveLength(3);
    expect(prompts.join('')).not.toMatch(/企业级|工业级|代码|落地|避坑/);
  });
});

describe('buildSessionTitleFromPrompt', () => {
  it('strips template brackets and keeps the title short', () => {
    expect(
      buildSessionTitleFromPrompt('请结合「1.1 数列与函数极限计算」，用通俗易懂的逻辑讲透')
    ).toBe('请结合1.1 数列与函数极限');
  });

  it('falls back to the default title for blank input', () => {
    expect(buildSessionTitleFromPrompt('   ')).toBe('新问答会话');
  });
});

describe('normalizeFollowUpPrompts', () => {
  it('strips numbering and markdown, drops generic and duplicated prompts', () => {
    const prompts = normalizeFollowUpPrompts([
      '1. worktree 和主分支共享对象库吗？',
      'worktree 和主分支共享对象库吗',
      '- 还有什么想了解的？',
      '**worktree 与 clone 在磁盘占用上有差别吗**',
      '这条追问会因为超过四十个字而被丢弃因为它实在是太长了根本不像学生会问出来的问题（真的）'
    ]);

    expect(prompts).toEqual([
      'worktree 和主分支共享对象库吗？',
      'worktree 与 clone 在磁盘占用上有差别吗'
    ]);
  });

  it('splits prompts that the model merged into one single line', () => {
    const prompts = normalizeFollowUpPrompts([
      '1. 极限唯一是怎么推导出来的？ 2. 收敛必有界有反例吗？ 3. 保号性做题第一步判断什么？'
    ]);

    expect(prompts).toEqual([
      '极限唯一是怎么推导出来的？',
      '收敛必有界有反例吗？',
      '保号性做题第一步判断什么？'
    ]);
  });

  it('strips section numbers such as 1.1 and list numbering from follow up prompts', () => {
    const prompts = normalizeFollowUpPrompts([
      '1.1 算法复杂度与渐近表示法为什么是衡量尺度？',
      '1.1求函数极限时为什么要先分左右极限？',
      '2. 渐进表示法里最坏情况怎么算？',
      '什么是 1.1 算法复杂度与渐近表示法的衡量口径？'
    ], 4);

    expect(prompts).toEqual([
      '算法复杂度与渐近表示法为什么是衡量尺度？',
      '求函数极限时为什么要先分左右极限？',
      '渐进表示法里最坏情况怎么算？',
      '什么是算法复杂度与渐近表示法的衡量口径？'
    ]);
  });

  it('caps prompts at the configured max count', () => {
    const prompts = normalizeFollowUpPrompts(
      ['A 方案在哪里会失败？', 'B 方案的边界在哪？', 'C 方案和 D 有什么差别？', 'E 方案怎么自测？'],
      3
    );

    expect(prompts).toHaveLength(3);
  });
});

describe('requestFollowUps', () => {
  it('returns nothing for degenerate answers that cannot anchor follow-ups', () => {
    expect(requestFollowUps('讲讲 JVM', '好的')).toEqual([]);
    expect(requestFollowUps('', 'JVM 由类加载器、运行时数据区与执行引擎组成。'.repeat(4))).toEqual([]);
  });

  it('returns rule-based prompts synchronously when no model upgrade is wired', () => {
    const answer = `## 1. JDK、JRE与JVM的包含关系\n${'JDK 包含开发工具与 JRE，JRE 包含 JVM 与核心类库。'.repeat(4)}`;

    const prompts = requestFollowUps('请梳理 Java 体系结构', answer);

    expect(prompts.length).toBeGreaterThan(0);
  });
});
