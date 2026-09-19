/** 卷面展示时去掉题干自带的题号前缀，避免与卷面编号重复 */
export function stripQuestionStemNumber(stem: string): string {
  const trimmed = String(stem || '').trim();
  const withoutPrefix = trimmed.replace(/^\d+[.、．)\]】]\s*/, '');
  return withoutPrefix || trimmed;
}
