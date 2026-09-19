/** 客观题答案规范化（前端预检；最终以服务端为准） */
export function normalizeObjectiveAnswer(raw?: string): string {
  if (!raw) return '';
  const trimmed = raw.trim().toUpperCase();
  if (trimmed.includes(',')) {
    return trimmed
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean)
      .sort()
      .join(',');
  }
  return trimmed;
}

export function isObjectiveQuestionType(type?: string): boolean {
  if (!type) return true;
  const t = type.toUpperCase();
  return (
    t === 'SINGLE_CHOICE' ||
    t === 'MULTIPLE_CHOICE' ||
    t === 'TRUE_FALSE' ||
    t === 'JUDGE' ||
    t === 'FILL_BLANK' ||
    t === 'SINGLE'
  );
}
