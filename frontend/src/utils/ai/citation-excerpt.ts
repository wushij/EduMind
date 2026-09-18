import { formatChunkListPreview } from '@/utils/format/chunk-preview';

export function citationExcerptSource(item: {
  snippet?: string;
  excerpt?: string;
  content?: string;
}): string {
  return (item.content || item.excerpt || item.snippet || '').trim();
}

/** 引用弹窗渲染前：修复切片里常见的围栏/标题粘连 */
export function prepareCitationMarkdown(raw: string): string {
  if (!raw) return '';
  let s = raw.replace(/\r\n/g, '\n');
  s = s.replace(/```javapublic/gi, '```java\npublic');
  s = s.replace(/```java(?=public\s)/gi, '```java\n');
  s = s.replace(/```text([A-Za-z\u4e00-\u9fa5])/gi, '```text\n$1');
  s = s.replace(/([^\n#])(#{2,6})/g, '$1\n\n$2');
  s = s.replace(/(^|\n)(#{1,6})([^\s#\n])/g, '$1$2 $3');
  return s.trim();
}

export function formatCitationCardPreview(text: string, maxChars = 168): string {
  if (!text) return '';
  return formatChunkListPreview(prepareCitationMarkdown(text), maxChars);
}
