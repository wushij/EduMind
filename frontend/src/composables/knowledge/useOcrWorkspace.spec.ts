import { describe, it, expect } from 'vitest';
import { DEFAULT_OCR_PAGES, resolveOcrTaskStatusLabel } from './useOcrWorkspace';
import type { OcrTaskVO } from '@/types/knowledge/ocr';

describe('resolveOcrTaskStatusLabel', () => {
  it('returns ready label when task is null', () => {
    expect(resolveOcrTaskStatusLabel(null)).toBe('就绪');
  });

  it('maps known statuses to Chinese labels', () => {
    expect(resolveOcrTaskStatusLabel({ status: 'PROOFREADING' } as OcrTaskVO)).toBe('待人工校对');
    expect(resolveOcrTaskStatusLabel({ status: 'FAILED' } as OcrTaskVO)).toBe('识别失败');
  });

  it('returns raw status for unknown values', () => {
    expect(resolveOcrTaskStatusLabel({ status: 'CUSTOM' } as unknown as OcrTaskVO)).toBe('CUSTOM');
  });
});

describe('DEFAULT_OCR_PAGES', () => {
  it('provides at least two default workspace pages', () => {
    expect(DEFAULT_OCR_PAGES.length).toBeGreaterThanOrEqual(2);
  });

  it('includes raw text for each default page', () => {
    DEFAULT_OCR_PAGES.forEach((page) => {
      expect(page.rawText.length).toBeGreaterThan(0);
    });
  });
});
