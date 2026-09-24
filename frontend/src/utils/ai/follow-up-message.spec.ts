import { ref } from 'vue';
import { describe, expect, it } from 'vitest';
import { pickTurnMessage } from './follow-up-message';

interface Message {
  id?: string | number;
  role: 'user' | 'assistant';
  followUpPrompts?: string[];
}

describe('pickTurnMessage', () => {
  it('按 id 命中当轮消息，并且回写能触发视图更新（回归：追问只有刷新才出现）', () => {
    const messages = ref<Message[]>([]);
    const raw: Message = { id: 'ai_1', role: 'assistant' };
    messages.value.push(raw);

    // 直接引用比较会失败：ref 数组读出来的是响应式代理，这正是当初守卫恒为 false 的根因
    expect(messages.value[0]).not.toBe(raw);

    const current = pickTurnMessage(messages.value, raw.id);
    expect(current).toBeDefined();

    current!.followUpPrompts = ['极限唯一是怎么推导出来的？'];
    expect(messages.value[0].followUpPrompts).toEqual(['极限唯一是怎么推导出来的？']);
  });

  it('当轮消息不再是最后一条时返回 undefined，避免迟到结果覆盖新会话', () => {
    const messages: Message[] = [
      { id: 'ai_1', role: 'assistant' },
      { id: 'user_2', role: 'user' }
    ];

    expect(pickTurnMessage(messages, 'ai_1')).toBeUndefined();
  });

  it('目标 id 缺失时不命中任何消息', () => {
    const messages: Message[] = [{ id: undefined, role: 'assistant' }];

    expect(pickTurnMessage(messages, undefined)).toBeUndefined();
    expect(pickTurnMessage([], 'ai_1')).toBeUndefined();
  });
});
