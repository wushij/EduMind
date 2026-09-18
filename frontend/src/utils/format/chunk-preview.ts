/**
 * 知识库切片列表行预览：压平 Markdown 换行，避免单行 text-overflow 像在半句话上截断。
 */
export function formatChunkListPreview(content: string, maxChars = 220): string {
  if (!content) {
    return '';
  }
  let s = content
    .replace(/\r\n/g, '\n')
    .replace(/\n+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
  s = s.replace(/#{1,6}\s*/g, '');
  if (s.length <= maxChars) {
    return s;
  }
  return `${s.slice(0, maxChars).trimEnd()}…`;
}
