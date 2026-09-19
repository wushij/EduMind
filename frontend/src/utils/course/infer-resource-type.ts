export function inferResourceTypeFromFileName(fileName: string): string {
  const lower = (fileName || '').toLowerCase();
  if (lower.endsWith('.pdf')) return 'PDF';
  if (lower.endsWith('.ppt') || lower.endsWith('.pptx')) return 'PPT';
  if (lower.endsWith('.doc') || lower.endsWith('.docx')) return 'WORD';
  if (lower.endsWith('.mp4') || lower.endsWith('.mov')) return 'VIDEO';
  if (lower.endsWith('.md') || lower.endsWith('.markdown')) return 'MD';
  if (lower.endsWith('.txt')) return 'TXT';
  return 'DOCUMENT';
}

export function isTextPreviewableFileName(fileName: string): boolean {
  const lower = (fileName || '').toLowerCase();
  return lower.endsWith('.md') || lower.endsWith('.markdown') || lower.endsWith('.txt');
}

/** 列表/预览展示用：纠正历史误标为 DOCUMENT 的类型 */
export function resolveResourceType(resourceType?: string, title?: string): string {
  const normalized = (resourceType || '').toUpperCase();
  if (normalized && normalized !== 'DOCUMENT') {
    return normalized;
  }
  const fromTitle = inferResourceTypeFromFileName(title || '');
  return fromTitle === 'DOCUMENT' && normalized ? normalized : fromTitle;
}

export function formatUploadFileSize(bytes: number): string {
  if (!bytes || bytes <= 0) return '0 KB';
  if (bytes >= 1024 * 1024) {
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }
  return `${Math.round(bytes / 1024)} KB`;
}

export function defaultResourceTitleFromFileName(fileName: string): string {
  const base = fileName.trim();
  if (!base) return '';
  const dot = base.lastIndexOf('.');
  if (dot > 0) {
    return base.slice(0, dot);
  }
  return base;
}

export function resourceTypeShortLabel(resourceType?: string, title?: string): string {
  const type = resolveResourceType(resourceType, title).toUpperCase();
  switch (type) {
    case 'PDF':
      return 'PDF';
    case 'PPT':
      return 'PPT';
    case 'WORD':
      return 'WORD';
    case 'VIDEO':
      return '视频';
    case 'MD':
    case 'MARKDOWN':
      return 'MD';
    case 'TXT':
      return 'TXT';
    case 'DOCUMENT':
      return 'DOC';
    default:
      return type.length > 4 ? type.slice(0, 4) : type;
  }
}

export function isMarkdownLikeResourceType(resourceType?: string, title?: string): boolean {
  const type = resolveResourceType(resourceType, title).toUpperCase();
  return type === 'MD' || type === 'MARKDOWN' || type === 'TXT';
}
