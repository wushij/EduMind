/**
 * 课节 AI 备课的「学情诊断」一次性上下文。
 *
 * 诊断内容（薄弱考点 + 错因分析）只用于喂给 AI 决定讲解重点，
 * 不能写进课节导读等对外展示字段，因此通过 sessionStorage
 * 在「备课入口页 → 课节工作台」之间传递，读取即销毁。
 */
const PREP_DIAGNOSIS_KEY_PREFIX = 'edumind:lesson-prep-diagnosis';

function buildKey(courseId: number, lessonId: number): string {
  return `${PREP_DIAGNOSIS_KEY_PREFIX}:${courseId}:${lessonId}`;
}

/** 写入备课诊断；内容为空时不做任何事 */
export function writeLessonPrepDiagnosis(courseId: number, lessonId: number, diagnosis: string): void {
  const text = (diagnosis || '').trim();
  if (!text) return;
  try {
    window.sessionStorage.setItem(buildKey(courseId, lessonId), text);
  } catch {
    // 隐私模式等写入失败场景：仅丢失 AI 诊断上下文，不影响备课流程
  }
}

/** 取出并清除备课诊断，避免刷新或二次进入时重复携带 */
export function takeLessonPrepDiagnosis(courseId: number, lessonId: number): string {
  const key = buildKey(courseId, lessonId);
  try {
    const text = window.sessionStorage.getItem(key) || '';
    if (text) {
      window.sessionStorage.removeItem(key);
    }
    return text;
  } catch {
    return '';
  }
}
