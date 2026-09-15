import { describe, it, expect } from 'vitest';
import type { KBDocument } from '@/types/knowledge/document';
import { buildDocumentInfo, isPipelineStepDone } from './useDocumentUpload';

describe('buildDocumentInfo', () => {
  it('returns placeholder info when document is missing', () => {
    expect(buildDocumentInfo(undefined)).toEqual({
      fileName: '',
      fileSize: '-',
      fileType: '-',
      status: 'PENDING'
    });
  });

  it('formats document metadata', () => {
    const doc: KBDocument = {
      id: 1,
      fileName: '讲义.pdf',
      name: '讲义.pdf',
      fileSize: 2 * 1024 * 1024,
      fileType: 'PDF',
      parseStatus: 'COMPLETED'
    };
    const info = buildDocumentInfo(doc);
    expect(info.fileName).toBe('讲义.pdf');
    expect(info.fileSize).toBe('2.00 MB');
    expect(info.status).toBe('COMPLETED');
  });
});

describe('isPipelineStepDone', () => {
  it('marks step 1 done when document parsing completed', () => {
    expect(isPipelineStepDone(1, 'COMPLETED', 0, 0)).toBe(true);
  });

  it('marks step 2 done when chunks exist', () => {
    expect(isPipelineStepDone(2, 'PENDING', 3, 0)).toBe(true);
  });

  it('marks step 3 done when indexed chunks exist', () => {
    expect(isPipelineStepDone(3, 'PENDING', 0, 2)).toBe(true);
  });
});
