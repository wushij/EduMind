import { describe, expect, it } from 'vitest';
import type { GlobalAssistantMessage } from '@/types/ai/assistant';
import {
  detectLessonMessageIntent,
  resolveLessonInsertIntent,
  extractKnowledgeSuggestions,
  matchKnowledgePointIds
} from '@/utils/ai/lesson-copilot-intent';

function assistant(content: string): GlobalAssistantMessage {
  return { role: 'assistant', content };
}

function user(content: string): GlobalAssistantMessage {
  return { role: 'user', content };
}

describe('detectLessonMessageIntent', () => {
  it('detects description from user instruction', () => {
    const messages = [user('请润色并生成课节导读，语气简洁'), assistant('导读：本课介绍…')];
    expect(detectLessonMessageIntent(messages[1], messages, 1)).toBe('description');
  });

  it('detects objective intent', () => {
    const messages = [user('请生成学习目标'), assistant('1. 理解概念')];
    expect(detectLessonMessageIntent(messages[1], messages, 1)).toBe('objective');
  });

  it('prefers objective when prompt includes 课节导读参考', () => {
    const prompt = `请根据课节标题《JDK》撰写学习目标（Markdown 列表）。
要求：动词可观测。
课节类型：讲义精讲。
课节导读参考：厘清 JVM 边界。`;
    const messages = [user(prompt), assistant('- **理解** JDK 与 JRE 的区别')];
    expect(detectLessonMessageIntent(messages[1], messages, 1)).toBe('objective');
  });

  it('prefers locked intent on user message over content heuristics', () => {
    const prompt = '请根据课节标题撰写学习目标（Markdown 列表）。';
    const answer = '- **理解** JDK 与 JRE 的区别';
    const messages = [
      user(prompt),
      assistant(answer)
    ];
    messages[0].lessonInsertIntent = 'editor';
    expect(resolveLessonInsertIntent(messages[1], messages, 1)).toBe('editor');
  });

  it('detects editor when prompt asks for lesson body', () => {
    const prompt = `请为课节《JDK》撰写完整教学正文（Markdown），可直接插入课节编辑器。`;
    const answer = '- **理解** JVM 结构\n- **掌握** 编译流程';
    const messages = [user(prompt), assistant(answer)];
    expect(detectLessonMessageIntent(messages[1], messages, 1)).toBe('editor');
  });

  it('keeps objective when model wrongly adds 推荐考点', () => {
    const prompt = `请根据课节标题撰写学习目标（Markdown 列表）。
课节导读参考：厘清 JVM。`;
    const answer = `本节匹配考点如下…\n【推荐考点】：JDK, JVM`;
    const messages = [user(prompt), assistant(answer)];
    expect(detectLessonMessageIntent(messages[1], messages, 1)).toBe('objective');
  });

  it('defaults to editor for long markdown body', () => {
    const messages = [
      user('续写正文\n\n# 第一章\n很长正文…'),
      assistant('## 下一节\n继续内容')
    ];
    expect(detectLessonMessageIntent(messages[1], messages, 1)).toBe('editor');
  });
});

describe('extractKnowledgeSuggestions', () => {
  it('parses labeled knowledge line', () => {
    const text = '【推荐考点】：导数定义, 极限运算';
    expect(extractKnowledgeSuggestions(text)).toEqual(['导数定义', '极限运算']);
  });
});

describe('matchKnowledgePointIds', () => {
  it('matches course knowledge points by title', () => {
    const ids = matchKnowledgePointIds(['导数定义'], [
      { id: 1, title: '其他' },
      { id: 2, title: '导数定义' }
    ]);
    expect(ids).toEqual([2]);
  });
});
