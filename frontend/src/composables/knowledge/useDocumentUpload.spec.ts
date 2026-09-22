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

  it('handles backend VO with fileName only', () => {
    const doc: KBDocument = {
      id: 2,
      fileName: '经典算法图解.pdf',
      fileSize: 4 * 1024 * 1024,
      fileType: 'PDF',
      parseStatus: 'SUCCESS'
    };
    const info = buildDocumentInfo(doc);
    expect(info.fileName).toBe('经典算法图解.pdf');
    expect(info.fileSize).toBe('4.00 MB');
    expect(info.status).toBe('SUCCESS');
  });
});

describe('isPipelineStepDone', () => {
  it('marks step 1 done when document parsing completed or success', () => {
    expect(isPipelineStepDone(1, 'COMPLETED', 0, 0)).toBe(true);
    expect(isPipelineStepDone(1, 'SUCCESS', 0, 0)).toBe(true);
    expect(isPipelineStepDone(1, 'CHUNKED', 0, 0)).toBe(true);
    expect(isPipelineStepDone(1, 'PARSED', 0, 0)).toBe(true);
    expect(isPipelineStepDone(1, 'PENDING', 5, 0)).toBe(true);
    expect(isPipelineStepDone(1, 'PENDING', 0, 0)).toBe(false);
  });

  it('marks step 2 done when chunks exist or status is chunked', () => {
    expect(isPipelineStepDone(2, 'PENDING', 3, 0)).toBe(true);
    expect(isPipelineStepDone(2, 'CHUNKED', 0, 0)).toBe(true);
    expect(isPipelineStepDone(2, 'INDEXED', 0, 0)).toBe(true);
    expect(isPipelineStepDone(2, 'PENDING', 0, 0)).toBe(false);
  });

  it('marks step 3 done when indexed chunks exist or status is indexed', () => {
    expect(isPipelineStepDone(3, 'PENDING', 0, 2)).toBe(true);
    expect(isPipelineStepDone(3, 'INDEXED', 0, 0)).toBe(true);
    expect(isPipelineStepDone(3, 'PENDING', 0, 0)).toBe(false);
  });
});

