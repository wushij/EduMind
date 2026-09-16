import { describe, expect, it } from 'vitest';
import { mergeServerWithLocalDrafts, type ChatMessage } from './stream-service';

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
});
