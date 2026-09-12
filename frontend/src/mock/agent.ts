import type { AgentRunVO, AgentVO } from '@/types/ai/agent';

export const MOCK_AGENTS: AgentVO[] = [
  {
    code: 'course-tutor',
    name: '课程智能助教 Agent',
    status: 'ACTIVE',
    modelKey: 'deepseek-chat',
    toolCount: 4,
    totalRuns: 1280,
    successRate: 98.4
  },
  {
    code: 'exam-builder',
    name: '多维组卷评估 Agent',
    status: 'ACTIVE',
    modelKey: 'deepseek-chat',
    toolCount: 4,
    totalRuns: 456,
    successRate: 97.2
  },
  {
    code: 'grading-assistant',
    name: '主观题智能精判 Agent',
    status: 'ACTIVE',
    modelKey: 'gpt-4o-mini',
    toolCount: 4,
    totalRuns: 892,
    successRate: 96.8
  },
  {
    code: 'lesson-planner',
    name: '教学大纲与教案生成 Agent',
    status: 'ACTIVE',
    modelKey: 'qwen-plus',
    toolCount: 4,
    totalRuns: 312,
    successRate: 99.1
  }
];

export const MOCK_AGENT_RUN: AgentRunVO = {
  runId: 'mock-run-001',
  status: 'COMPLETED',
  steps: [
    {
      index: 1,
      type: 'PLAN',
      title: '解析教学目标',
      tool: '',
      status: 'DONE',
      outputPreview: '识别课程章节与薄弱知识点'
    },
    {
      index: 2,
      type: 'TOOL',
      title: '检索知识库',
      tool: 'knowledge_search',
      status: 'DONE',
      outputPreview: '命中 6 条相关文档切片'
    },
    {
      index: 3,
      type: 'TOOL',
      title: '生成练习题',
      tool: 'question_generate',
      status: 'DONE',
      outputPreview: '生成 5 道变式练习题'
    },
    {
      index: 4,
      type: 'SUMMARY',
      title: '输出教学建议',
      tool: '',
      status: 'DONE',
      outputPreview: '已生成针对性巩固方案'
    }
  ],
  result: {
    summary: '已根据班级薄弱点生成巩固练习与教学建议',
    questionCount: 5
  }
};
