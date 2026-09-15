import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { PromptTemplate } from '@/types/system/prompt';
import type { AIModelConfigItem } from '@/types/system/model';
import {
  getCategoryLabel,
  getCategoryCount,
  filterPromptList,
  isBoundModelUnset,
  getDisplayModelName,
  getDisplayModelTooltip,
  computePublishedCount,
  computeRagCount,
  computeTotalVariablesCount,
  buildFallbackTestOutput,
  categoryOptions
} from './usePromptList';

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() })
}));

vi.mock('@/api/system/model', () => ({
  fetchModels: vi.fn()
}));

vi.mock('@/api/system/prompt', () => ({
  getPromptTemplates: vi.fn(),
  getPromptById: vi.fn(),
  getPromptVersions: vi.fn(),
  savePromptTemplate: vi.fn(),
  publishPromptTemplate: vi.fn(),
  rollbackPromptTemplate: vi.fn(),
  testPromptTemplate: vi.fn()
}));

const samplePrompts: PromptTemplate[] = [
  {
    id: 1,
    code: 'COURSE_RAG_GENERAL',
    name: '课程 RAG 问答',
    category: 'rag',
    description: 'RAG 模板',
    systemPrompt: 'sys',
    userPromptTemplate: 'user {{question}}',
    variables: [{ name: 'question', label: '问题' }, { name: 'context', label: '上下文' }],
    version: 'v1.0.0',
    status: 'PUBLISHED',
    boundModel: '',
    temperature: 0.3,
    maxTokens: 8000,
    callCount: 0,
    createdAt: '2026-01-01',
    updatedAt: '2026-01-01'
  },
  {
    id: 2,
    code: 'EXAM_RAG_GENERAL',
    name: '智能命题',
    category: 'question',
    description: '命题模板',
    systemPrompt: 'sys',
    userPromptTemplate: 'user',
    variables: [{ name: 'course_name', label: '课程' }],
    version: 'v1.0.0',
    status: 'DRAFT',
    boundModel: 'deepseek-chat',
    temperature: 0.5,
    maxTokens: 4000,
    callCount: 0,
    createdAt: '2026-01-02',
    updatedAt: '2026-01-02'
  },
  {
    id: 3,
    code: 'GRADING_RAG_GENERAL',
    name: '智能批改',
    category: 'grading',
    description: '批改模板',
    systemPrompt: 'sys',
    userPromptTemplate: 'user',
    variables: [],
    version: 'v1.0.0',
    status: 'PUBLISHED',
    boundModel: 'gpt-4o',
    temperature: 0.2,
    maxTokens: 2000,
    callCount: 0,
    createdAt: '2026-01-03',
    updatedAt: '2026-01-03'
  }
];

const sampleModels: AIModelConfigItem[] = [
  {
    id: 1,
    name: 'DeepSeek Flash',
    modelName: 'deepseek-chat',
    provider: 'deepseek',
    configType: 'chat',
    temperature: 0.3,
    status: 'enabled',
    isDefault: true
  },
  {
    id: 2,
    name: 'GPT-4o',
    modelName: 'gpt-4o',
    provider: 'openai',
    configType: 'chat',
    temperature: 0.5,
    status: 'enabled',
    isDefault: false
  }
];

describe('usePromptList helpers', () => {
  it('exposes five category options including ALL', () => {
    expect(categoryOptions).toHaveLength(5);
    expect(categoryOptions[0].key).toBe('ALL');
  });

  it('returns category label by code', () => {
    expect(getCategoryLabel('rag')).toBe('课程问答 RAG');
    expect(getCategoryLabel('question')).toBe('智能命题');
    expect(getCategoryLabel('agent')).toBe('Agent 编排');
    expect(getCategoryLabel('unknown')).toBe('通用');
  });

  it('counts prompts per category', () => {
    expect(getCategoryCount(samplePrompts, 'ALL')).toBe(3);
    expect(getCategoryCount(samplePrompts, 'rag')).toBe(1);
    expect(getCategoryCount(samplePrompts, 'teaching')).toBe(0);
  });

  it('computes aggregate stats', () => {
    expect(computePublishedCount(samplePrompts)).toBe(2);
    expect(computeRagCount(samplePrompts)).toBe(1);
    expect(computeTotalVariablesCount(samplePrompts)).toBe(3);
  });

  it('filters by category, status and keyword', () => {
    const byCategory = filterPromptList(samplePrompts, 'rag', '', '');
    expect(byCategory).toHaveLength(1);
    expect(byCategory[0].code).toBe('COURSE_RAG_GENERAL');

    const byStatus = filterPromptList(samplePrompts, 'ALL', 'PUBLISHED', '');
    expect(byStatus).toHaveLength(2);

    const byKeyword = filterPromptList(samplePrompts, 'ALL', '', '命题');
    expect(byKeyword).toHaveLength(1);
    expect(byKeyword[0].category).toBe('question');

    const byVariable = filterPromptList(samplePrompts, 'ALL', '', 'context');
    expect(byVariable).toHaveLength(1);
  });

  it('detects unset bound model', () => {
    expect(isBoundModelUnset('')).toBe(true);
    expect(isBoundModelUnset('deepseek-chat')).toBe(true);
    expect(isBoundModelUnset('gpt-4o')).toBe(false);
  });

  it('resolves display model name and tooltip', () => {
    expect(getDisplayModelName('', sampleModels, 'DeepSeek Flash')).toBe('默认: DeepSeek Flash');
    expect(getDisplayModelName('gpt-4o', sampleModels, 'DeepSeek Flash')).toBe('GPT-4o');
    expect(getDisplayModelName('unknown-model', sampleModels, '')).toBe('unknown-model');

    expect(getDisplayModelTooltip('gpt-4o', sampleModels, '')).toContain('GPT-4o');
    expect(getDisplayModelTooltip('', sampleModels, 'DeepSeek Flash')).toContain('DeepSeek Flash');
  });

  it('builds category-specific fallback test output', () => {
    const gradingOutput = buildFallbackTestOutput(samplePrompts[2], { question_type: 'SHORT_ANSWER' });
    expect(gradingOutput).toContain('"status": "SUCCESS"');
    expect(gradingOutput).toContain('gradingPoints');

    const questionOutput = buildFallbackTestOutput(samplePrompts[1], { course_name: 'Java程序设计' });
    expect(questionOutput).toContain('"questions"');

    const ragOutput = buildFallbackTestOutput(samplePrompts[0], {});
    expect(ragOutput).toContain('多态的核心原理');
  });
});

describe('usePromptList composable', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('reactively filters prompt list when category changes', async () => {
    const { usePromptList } = await import('./usePromptList');
    const {
      promptList,
      filteredPrompts,
      publishedCount,
      selectedCategory,
      changeCategory,
      resetFilters,
      searchKeyword,
      selectedStatus
    } = usePromptList();

    promptList.value = samplePrompts;

    expect(publishedCount.value).toBe(2);
    expect(filteredPrompts.value).toHaveLength(3);

    changeCategory('rag');
    expect(selectedCategory.value).toBe('rag');
    expect(filteredPrompts.value).toHaveLength(1);

    resetFilters();
    expect(selectedCategory.value).toBe('ALL');
    expect(selectedStatus.value).toBe('');
    expect(searchKeyword.value).toBe('');
  });
});
