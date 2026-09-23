import { describe, expect, it } from 'vitest';
import { mergeServerWithLocalDrafts, generateSmartFollowUps, type ChatMessage } from './stream-service';

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

  it('falls back gracefully to intent-based prompts if answer has no headings', () => {
    const query = '请为我讲解这个算法的时间复杂度';
    const prompts = generateSmartFollowUps(query);
    expect(prompts).toHaveLength(3);
    expect(prompts[0]).toContain('时空复杂度');
  });
});
