import type { KBDocument } from '@/types/knowledge/document';

export type DocumentFileKind = 'pdf' | 'word' | 'ppt' | 'md' | 'default';

export function resolveDocumentFileKind(fileName?: string, fileType?: string): DocumentFileKind {
  const name = (fileName || '').toLowerCase();
  const mime = (fileType || '').toLowerCase();

  if (name.endsWith('.pdf') || mime.includes('pdf')) return 'pdf';
  if (/\.(doc|docx)$/.test(name) || mime.includes('word') || mime.includes('msword')) return 'word';
  if (/\.(ppt|pptx)$/.test(name) || mime.includes('presentation') || mime.includes('powerpoint')) return 'ppt';
  if (/\.(md|markdown)$/.test(name) || mime.includes('markdown')) return 'md';
  return 'default';
}

export function normalizeKBDocument(raw: Record<string, unknown>): KBDocument {
  const fileSizeRaw = raw.fileSize ?? raw.size;
  const fileName = String(raw.fileName ?? raw.name ?? '未命名文档');
  return {
    id: Number(raw.id),
    knowledgeBaseId: Number(raw.knowledgeBaseId ?? raw.baseId ?? 0) || undefined,
    name: fileName,
    fileName,
    fileType: String(raw.fileType ?? raw.type ?? ''),
    fileSize: fileSizeRaw != null ? Number(fileSizeRaw) : undefined,
    chunkCount: Number(raw.chunkCount ?? 0),
    sourceType: raw.sourceType ? String(raw.sourceType) : undefined,
    parseStatus: String(raw.parseStatus ?? raw.status ?? 'PENDING'),
    chunkStatus: String(raw.chunkStatus ?? ''),
    errorMessage: raw.errorMessage ? String(raw.errorMessage) : undefined,
    createTime: String(raw.createTime ?? raw.uploadTime ?? raw.createdAt ?? ''),
    createdAt: String(raw.createTime ?? raw.uploadTime ?? raw.createdAt ?? '')
  };
}

export function formatParseStatusLabel(
  status?: string,
  doc?: Pick<KBDocument, 'sourceType' | 'chunkCount'>
): string {
  if (doc?.sourceType === 'LESSON') {
    if ((doc.chunkCount ?? 0) > 0) {
      return '讲义已入库';
    }
    return '待发布课节';
  }
  switch (status) {
    case 'SUCCESS':
    case 'PARSED':
    case 'CHUNKED':
      return '已完成解析';
    case 'PARSING':
    case 'CHUNKING':
      return '正在解析';
    case 'FAILED':
      return '解析失败';
    case 'CHUNK_FAILED':
      return '切片失败';
    case 'PENDING':
      return '待解析';
    default:
      return status || '待解析';
  }
}

export function getParseStatusTagType(
  status?: string,
  doc?: Pick<KBDocument, 'sourceType' | 'chunkCount'>
): '' | 'success' | 'warning' | 'info' | 'danger' {
  if (doc?.sourceType === 'LESSON' && (doc.chunkCount ?? 0) > 0) {
    return 'success';
  }
  switch (status) {
    case 'SUCCESS':
    case 'PARSED':
    case 'CHUNKED':
      return 'success';
    case 'PARSING':
    case 'CHUNKING':
      return 'warning';
    case 'FAILED':
    case 'CHUNK_FAILED':
      return 'danger';
    default:
      return 'info';
  }
}

export function formatChunkStatusLabel(chunkStatus?: string, chunkCount = 0): string {
  switch (chunkStatus) {
    case 'INDEXED':
    case 'CHUNKED':
      return chunkCount > 0 ? '已入库' : '已切片';
    case 'CHUNKING':
      return '切片中';
    case 'CHUNK_FAILED':
      return '切片失败';
    case 'PARSED':
      return '待切片';
    default:
      return chunkCount > 0 ? '已就绪' : '待切片';
  }
}

export function getChunkStatusTagType(chunkStatus?: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  switch (chunkStatus) {
    case 'INDEXED':
    case 'CHUNKED':
      return 'success';
    case 'CHUNKING':
      return 'warning';
    case 'CHUNK_FAILED':
      return 'danger';
    default:
      return 'info';
  }
}

export function canTriggerParse(
  parseStatus?: string,
  sourceType?: string
): boolean {
  if (sourceType === 'LESSON') {
    return false;
  }
  return parseStatus !== 'SUCCESS' && parseStatus !== 'PARSED' && parseStatus !== 'PARSING';
}
