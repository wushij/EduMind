/** 识别误入库的 AI 提示词 / Mock 占位题干 */
export function isGarbageQuestionStem(stem?: string | null): boolean {
  if (!stem || !stem.trim()) return true;
  const text = stem.trim();
  if (text.startsWith('Mock 题目') || text.includes('Mock 题目')) return true;
  if (text.includes('你是一位专业的教学出题助手') && text.includes('JSON 格式')) return true;
  if (text.includes('请严格按 JSON 格式输出') && text.includes('questions')) return true;
  if (text.length > 500 && text.includes('课程ID=') && text.includes('知识点=') && text.includes('题型=')) {
    return true;
  }
  return false;
}
