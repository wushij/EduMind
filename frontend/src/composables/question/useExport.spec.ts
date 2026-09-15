import { describe, it, expect } from 'vitest';
import {
  PRESET_TEMPLATES,
  createDefaultConfigForm,
  parseExportTaskTitle,
  mapExportTaskToHistoryItem,
  applyPresetConfig,
  DEFAULT_EXPORT_HISTORY
} from './useExport';
import type { ExportTaskVO } from '@/types/question/export';

describe('createDefaultConfigForm', () => {
  it('returns form with expected default exam preset values', () => {
    const form = createDefaultConfigForm();
    expect(form.examId).toBe(101);
    expect(form.paperSize).toBe('A4');
    expect(form.showSealingLine).toBe(true);
    expect(form.showAnswerSheet).toBe(true);
    expect(form.fontFamily).toBe('SimSun');
  });

  it('returns a new object each call', () => {
    const first = createDefaultConfigForm();
    first.paperTitle = 'changed';
    expect(createDefaultConfigForm().paperTitle).not.toBe('changed');
  });
});

describe('PRESET_TEMPLATES', () => {
  it('contains four preset schemes', () => {
    expect(PRESET_TEMPLATES).toHaveLength(4);
    expect(PRESET_TEMPLATES[0].id).toBe('preset-1');
    expect(PRESET_TEMPLATES[1].name).toContain('精练卷');
  });
});

describe('applyPresetConfig', () => {
  it('merges preset config into form', () => {
    const form = createDefaultConfigForm();
    applyPresetConfig(form, PRESET_TEMPLATES[1]);
    expect(form.paperTitle).toContain('函数与导数');
    expect(form.showSealingLine).toBe(false);
    expect(form.lineSpacing).toBe('compact');
  });
});

describe('parseExportTaskTitle', () => {
  it('uses paperTitle when present', () => {
    const item: ExportTaskVO = {
      taskId: 'T1',
      bizType: 'PAPER',
      status: 'SUCCESS',
      progress: 100,
      paperTitle: '直接标题'
    };
    expect(parseExportTaskTitle(item)).toBe('直接标题');
  });

  it('parses title from exportParams JSON', () => {
    const item: ExportTaskVO = {
      taskId: 'T2',
      bizType: 'PAPER',
      status: 'SUCCESS',
      progress: 100,
      exportParams: JSON.stringify({ paperTitle: 'JSON 标题' })
    };
    expect(parseExportTaskTitle(item)).toBe('JSON 标题');
  });

  it('falls back to bizId label', () => {
    const item: ExportTaskVO = {
      taskId: 'T3',
      bizType: 'PAPER',
      bizId: 42,
      status: 'PENDING',
      progress: 0
    };
    expect(parseExportTaskTitle(item)).toBe('试卷 #42 考务排版');
  });
});

describe('mapExportTaskToHistoryItem', () => {
  it('maps API task to history row', () => {
    const item: ExportTaskVO = {
      taskId: 'EXP-001',
      bizType: 'PAPER',
      status: 'PROCESSING',
      progress: 60,
      paperTitle: '期中试卷',
      createTime: '2026-09-15 12:00:00'
    };
    const row = mapExportTaskToHistoryItem(item, 'B4');
    expect(row).toEqual({
      taskId: 'EXP-001',
      title: '期中试卷',
      size: 'B4',
      type: 'PDF',
      status: 'PROCESSING',
      progress: 60,
      errorMsg: undefined,
      createTime: '2026-09-15 12:00:00',
      downloadUrl: undefined
    });
  });
});

describe('DEFAULT_EXPORT_HISTORY', () => {
  it('includes sample successful export tasks', () => {
    expect(DEFAULT_EXPORT_HISTORY.length).toBeGreaterThanOrEqual(3);
    expect(DEFAULT_EXPORT_HISTORY[0].status).toBe('SUCCESS');
  });
});
