/**
 * 知识库与文档状态常量定义
 */
export const DOCUMENT_STATUS = {
  UPLOADING: 'UPLOADING',
  UPLOADED: 'UPLOADED',
  PARSING: 'PARSING',
  PARSED: 'PARSED',
  FAILED: 'FAILED'
} as const;

export const DOCUMENT_PARSE_STATUS = {
  PENDING: 'PENDING',
  PARSING: 'PARSING',
  PARSED: 'PARSED',
  FAILED: 'FAILED'
} as const;

export const KNOWLEDGE_BASE_TYPE = {
  COURSE: 'COURSE',
  GENERAL: 'GENERAL',
  DEPARTMENT: 'DEPARTMENT'
} as const;

export const DOCUMENT_TYPE_LABELS: Record<string, string> = {
  pdf: 'PDF 电子文献',
  docx: 'Word 文档',
  md: 'Markdown 课件',
  txt: '纯文本'
};
