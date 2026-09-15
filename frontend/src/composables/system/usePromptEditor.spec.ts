import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { PromptVersionItem } from '@/api/system/prompt';
import {
  getCategoryLabel,
  getVariablesList,
  isCurrentVersion,
  getVersionDiffInfo,
  formatVersionDate,
  extractVariableNamesFromPrompts,
  createDefaultForm,
  createEmptyForm
} from './usePromptEditor';

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

const sampleVersions: PromptVersionItem[] = [
  {
    id: 1,
    templateId: 10,
    version: 1,
    content: 'Hello {{name}}',
    systemPrompt: 'You are helpful',
    variables: 'name,age',
    createTime: '2026-01-01T10:00:00'
  },
  {
    id: 2,
    templateId: 10,
    version: 2,
    content: 'Hello {{name}} updated',
    systemPrompt: 'You are helpful v2',
    variables: 'name,age,city',
    createTime: '2026-01-02T10:00:00'
  }
];

describe('usePromptEditor helpers', () => {
  it('returns category label by code', () => {
    expect(getCategoryLabel('rag')).toBe('课程问答 RAG');
    expect(getCategoryLabel('question')).toBe('智能命题');
    expect(getCategoryLabel('unknown')).toBe('通用');
  });

  it('parses variables from JSON array or comma-separated string', () => {
    expect(getVariablesList('a,b,c')).toEqual(['a', 'b', 'c']);
    expect(getVariablesList('[{"name":"course_name"},{"name":"question"}]')).toEqual([
      'course_name',
      'question'
    ]);
    expect(getVariablesList(undefined)).toEqual([]);
  });

  it('detects current version from string or number', () => {
    expect(isCurrentVersion('v2.0.0', 2)).toBe(true);
    expect(isCurrentVersion('v1.0.0', 2)).toBe(false);
    expect(isCurrentVersion(2, 2)).toBe(true);
  });

  it('formats version date', () => {
    expect(formatVersionDate('2026-01-01T10:30:00.123')).toBe('2026-01-01 10:30:00');
    expect(formatVersionDate()).toBe('最近发布');
  });

  it('extracts variable names from prompts', () => {
    const names = extractVariableNamesFromPrompts(
      'System {{role}}',
      'User {{course_name}} and {{question}}'
    );
    expect(names).toEqual(['role', 'course_name', 'question']);
  });

  it('builds version diff info for base and updated versions', () => {
    const baseInfo = getVersionDiffInfo(sampleVersions[0], sampleVersions);
    expect(baseInfo.tagType).toBe('base');
    expect(baseInfo.isBase).toBe(true);

    const updatedInfo = getVersionDiffInfo(sampleVersions[1], sampleVersions);
    expect(updatedInfo.tagType).toBe('updated');
    expect(updatedInfo.summary).toContain('v1.0');
  });

  it('detects identical consecutive versions', () => {
    const identicalVersions: PromptVersionItem[] = [
      { id: 1, templateId: 10, version: 1, content: 'same', systemPrompt: 'sys' },
      { id: 2, templateId: 10, version: 2, content: 'same', systemPrompt: 'sys' }
    ];
    const info = getVersionDiffInfo(identicalVersions[1], identicalVersions);
    expect(info.tagType).toBe('identical');
    expect(info.isIdentical).toBe(true);
  });

  it('creates default and empty form templates', () => {
    const defaultForm = createDefaultForm();
    expect(defaultForm.code).toBe('COURSE_RAG_GENERAL');
    expect(defaultForm.variables.length).toBeGreaterThan(0);

    const emptyForm = createEmptyForm();
    expect(emptyForm.code).toBe('');
    expect(emptyForm.variables).toEqual([]);
  });
});

