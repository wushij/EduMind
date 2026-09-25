/**
 * 微课节标题序号工具。
 *
 * 同一章节的课节编号由两个入口共同写入，规则必须完全一致：
 * - AI 备课（`services/course/lesson-studio-entry.ts`）：`1.2 等价无穷小代换专项突破`
 * - AI 大纲拆解（`components/course/ChapterAiGenerateModal.vue`）：接着已有课节续编为 `1.3 xxx`
 *
 * 判定「已带编号」时只认「数字.数字」形态，避免把「3 个重要极限与等价无穷小替换」这类
 * 以裸数字开头的正常标题误判成编号。
 */

/** 已带编号前缀：`1.2 `、`1.2、`、`1.2xxx`、`1. ` —— 必须带小数点，裸数字不算编号 */
const LESSON_NO_PREFIX_RE = /^\d+\.(?:\d+)?\s*/;

/** 模型偶尔输出「课时1:」「微课 2、」这类序号标签，入库前必须剥掉 */
const LESSON_LABEL_PREFIX_RE = /^(?:微课|课时|课节)\s*\d+\s*[:：、.,]?\s*/;

/** 从已有标题里提取「章.节」中的节号 */
const LESSON_NO_EXTRACT_RE = /^\d+\.(\d+)/;

const FALLBACK_LESSON_TITLE = '未命名微课节';

/** 标题是否已带 `1.2 ` 形态的编号前缀 */
export function hasLessonNoPrefix(title?: string): boolean {
  return LESSON_NO_PREFIX_RE.test((title || '').trim());
}

/** 剥离「课时1:」「微课 2、」这类序号标签，只保留真实标题主干 */
export function stripLessonLabelPrefix(title?: string): string {
  return (title || '').trim().replace(LESSON_LABEL_PREFIX_RE, '').trim();
}

/**
 * 推导下一个可用课节序号。
 *
 * 取「已有编号中的最大节号」与「已有课节数量」的较大值 + 1：
 * 这样即使中途手工录入过无编号课节，续编号也不会与已占用编号撞号。
 */
export function resolveNextLessonNo(titles: Array<string | undefined>): number {
  const list = (titles || []).map(title => (title || '').trim()).filter(Boolean);
  let maxNo = 0;
  for (const title of list) {
    const matched = title.match(LESSON_NO_EXTRACT_RE);
    if (matched) {
      maxNo = Math.max(maxNo, Number(matched[1]));
    }
  }
  return Math.max(maxNo, list.length) + 1;
}

/** 拼装带编号的课节标题：`1.3 函数定义域与反函数结构解析` */
export function buildLessonTitle(title: string, chapterNo: number, lessonNo: number): string {
  const body = stripLessonLabelPrefix(title) || FALLBACK_LESSON_TITLE;
  return `${chapterNo}.${lessonNo} ${body}`;
}

/**
 * AI 生成结果归一化：模型自带的编号一律重写为期望编号，
 * 保证弹窗预览的序号与最终入库、列表展示完全一致。
 */
export function normalizeLessonTitle(title: string, chapterNo: number, lessonNo: number): string {
  const text = stripLessonLabelPrefix(title);
  if (!text) return buildLessonTitle('', chapterNo, lessonNo);
  if (LESSON_NO_PREFIX_RE.test(text)) {
    return buildLessonTitle(text.replace(LESSON_NO_PREFIX_RE, ''), chapterNo, lessonNo);
  }
  return buildLessonTitle(text, chapterNo, lessonNo);
}

/**
 * 入库兜底：教师手工编辑过的标题若已带编号则原样保留（尊重人工意图），只补空缺编号。
 */
export function ensureLessonTitle(title: string, chapterNo: number, lessonNo: number): string {
  const text = (title || '').trim();
  if (!text) return buildLessonTitle('', chapterNo, lessonNo);
  return hasLessonNoPrefix(text) ? text : buildLessonTitle(text, chapterNo, lessonNo);
}
