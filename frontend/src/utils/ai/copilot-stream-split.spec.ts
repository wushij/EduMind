import { describe, expect, it } from 'vitest';
import { splitCopilotStream } from './copilot-stream-split';

describe('splitCopilotStream', () => {
  it('does not treat first code fence in thinking as start of answer', () => {
    const raw = `## 简要思考
先理清包含关系，再加上执行链路：
\`\`\`
.java --javac编译--> .class --JVM--> 结果
\`\`\`

### 1. JDK是什么？
**JDK：** 开发工具包。`;
    const split = splitCopilotStream(raw);
    expect(split.answer).toContain('### 1. JDK');
    expect(split.answer).not.toMatch(/^```/);
    expect(split.thinking).toContain('执行链路');
  });
});
