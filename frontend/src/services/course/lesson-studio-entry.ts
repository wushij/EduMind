import type { Router } from 'vue-router';
import { ElMessage } from 'element-plus';
import { createChapterApi, getChapters } from '@/api/course/chapter';
import { updateLesson } from '@/api/course/lesson';
import { writeLessonPrepDiagnosis } from '@/utils/course/lesson-prep-diagnosis';
import { resolveNextLessonNo } from '@/utils/course/lesson-title-number';
import type { Chapter } from '@/types/course/chapter';

/** 教情报告中的薄弱考点（用于生成针对性备课课节的标题与导读） */
export interface LessonStudioWeakPoint {
  /** 考点 / 知识点名称，例如「等价无穷小代换」 */
  name: string;
  /** 错因类型中文名，例如「计算失误」 */
  errorTypeName?: string | null;
  /** 错因说明或改进建议，会作为备课导读与 AI 上下文的一部分 */
  errorReason?: string | null;
}

export interface LessonStudioEntryOptions {
  /** 薄弱考点：传入时走「新建草稿课节 → AI 备课」模式（A 方案） */
  weakPoints?: LessonStudioWeakPoint[];
  /** 直接进入指定课节，不做任何新建 */
  lessonId?: number;
  /** 新建课节后是否在工作台自动发起 AI 备课，默认 true */
  autoPrepare?: boolean;
}

/** 工作台识别「落地后自动发起 AI 备课」的 query 标记 */
export const LESSON_AI_PREP_QUERY = 'aiPrep';

/** 新建备课课节的默认课时（分钟） */
const DEFAULT_PREP_DURATION_MINUTES = 30;
/** 导读中最多列出的考点数 */
const MAX_FOCUS_POINTS = 2;
/**
 * 课节标题中考点主干的字数上限：超过后优先在连接词处收窄。
 *
 * <p>取值必须覆盖常见考点全名（如「洛必达法则求未定式极限」11 字）：旧值 10 会把这类
 * 没有连接词可依的考点名硬截成「洛必达法则求未定式极…」，标题里出现半截词。</p>
 */
const MAX_TITLE_FOCUS_CHARS = 16;
/** 标题总长上限（含 `1.2 ` 编号前缀与「专项突破」后缀）：超出时优先丢掉后缀，保住完整考点名 */
const MAX_PREP_TITLE_CHARS = 20;
/** 本入口生成的课节标题后缀（含历史「AI 备课专项」） */
const PREP_TITLE_SUFFIX = '专项突破';
const PREP_TITLE_SUFFIX_FALLBACK = 'AI 备课专项';
/** 复用判定用的考点指纹长度：标题压缩规则调整后仍须命中同一个备课课节 */
const TITLE_FINGERPRINT_CHARS = 6;
/** 考点名里的连接词：取标题主干时优先在此断开，避免切出「…应用条…」这种半截词 */
const FOCUS_SPLIT_MARKERS = ['及其', '以及', '与', '和', '及', '、', '，', ',', '（', '('];
/** 课节导读字符上限：工作台导读输入框限 300 字，这里留出安全余量 */
const MAX_LESSON_DESCRIPTION_CHARS = 280;
/** 单条错因写入 AI 学情诊断的长度上限（按句边界截断，超出才省略） */
const MAX_DIAGNOSIS_REASON_CHARS = 240;
/** 早期自动生成的导读特征：命中即视为需要按最新文案规则刷新 */
const LEGACY_PREP_DESCRIPTION_MARKERS = ['依据教情报告', '学情提示：', '概念理解错误'];

/** 课节的轻量视图，兼容 children 与历史 sections 两种承载方式 */
interface LessonRef {
  id: number;
  title?: string;
  description?: string;
  /** 后端排序值，用于续接新课节的 sortOrder */
  sortOrder?: number;
}

/** 后端 ChapterTreeVO 仅对课节节点 enrich 出 lessonMeta，可作为「该节点是课节」的判定依据 */
interface ChapterTreeNode extends Chapter {
  lessonMeta?: { lessonType?: string; hasContent?: boolean; durationMinutes?: number } | null;
}

/** 新建备课课节的产出：课节 ID、标题，以及是否为复用已有课节 */
interface PrepLessonResult {
  lessonId: number;
  title: string;
  reused: boolean;
}

/** 收集某一章下的课节：后端 ChapterTreeVO 用 children 承载，sections 仅作历史兼容 */
function collectLessons(chapter: Chapter): LessonRef[] {
  const children = chapter.children ?? [];
  if (children.length > 0) {
    return children.filter(child => child?.id).map(child => ({
      id: child.id,
      title: child.title,
      description: child.description,
      sortOrder: child.sort
    }));
  }
  return (chapter.sections ?? [])
    .filter(section => section?.id)
    .map(section => ({
      id: section.id,
      title: section.title,
      description: section.description
    }));
}

/**
 * 查找课程大纲中第一个课节 ID。
 * 后端 /courses/{courseId}/chapters 返回的是 ChapterTreeVO 树：章节为父节点，
 * 课节以 children 子节点承载，响应里并没有 sections 字段，
 * 因此必须优先读 children，其次再兼容 sections 与「章节自身即课节」的形态。
 */
function findFirstLessonId(chapters: Chapter[]): number | null {
  for (const chapter of chapters ?? []) {
    if (!chapter?.id) continue;

    const childNodes = chapter.children ?? [];
    // 1. 标准结构：课节挂在章节的 children 下，子节点 id 即 lessonId
    for (const child of childNodes) {
      if (child?.id) {
        return child.id;
      }
    }

    // 2. 章节自身就是课节（课程只建了课节、没有父章层）
    if ((chapter as ChapterTreeNode).lessonMeta) {
      return chapter.id;
    }

    // 3. 兼容把课节放在 sections 的历史结构
    for (const section of chapter.sections ?? []) {
      if (section?.id) {
        return section.id;
      }
    }

    // 4. 更深层级兜底
    const nested = findFirstLessonId(childNodes);
    if (nested) {
      return nested;
    }
  }
  return null;
}

/** 去重并清理考点名称，空名称一律丢弃 */
function normalizeWeakPoints(input?: LessonStudioWeakPoint[]): LessonStudioWeakPoint[] {
  if (!input?.length) return [];
  const seen = new Set<string>();
  const result: LessonStudioWeakPoint[] = [];
  for (const item of input) {
    const name = (item?.name || '').trim();
    if (!name || seen.has(name)) continue;
    seen.add(name);
    result.push({
      name,
      errorTypeName: item.errorTypeName ?? null,
      errorReason: item.errorReason ?? null
    });
  }
  return result;
}

/**
 * 选择备课课节的落点章节：优先命中考点相关的章（章标题或章下课节出现考点名），
 * 命中不到则退回第一章；课程还没有章时返回 null，由调用方补建。
 */
function pickTargetChapter(chapters: Chapter[], weakPoints: LessonStudioWeakPoint[]): Chapter | null {
  const validChapters = (chapters ?? []).filter(chapter => chapter?.id);
  if (validChapters.length === 0) return null;

  const names = weakPoints.map(point => point.name);
  if (names.length === 0) return validChapters[0];

  const hit = validChapters.find(chapter => {
    const haystack = [
      chapter.title,
      chapter.description,
      ...collectLessons(chapter).flatMap(lesson => [lesson.title, lesson.description])
    ]
      .filter(Boolean)
      .join(' ');
    return names.some(name => haystack.includes(name));
  });
  return hit ?? validChapters[0];
}

/**
 * 课节标题里的考点主干：优先在「及其 / 与 / 和」这类连接词处断开。
 *
 * <p>找不到断点时**保留完整考点名**——考点名是标题的语义主干，写死省略号会在标题里
 * 留下「…未定式极…」这种半截词；标题过长交给页面按宽度做视觉截断即可。</p>
 */
function toFocusHeadline(name: string): string {
  const trimmed = (name || '').trim();
  if (trimmed.length <= MAX_TITLE_FOCUS_CHARS) return trimmed;
  for (const marker of FOCUS_SPLIT_MARKERS) {
    const index = trimmed.indexOf(marker);
    if (index > 0 && index <= MAX_TITLE_FOCUS_CHARS) {
      return trimmed.slice(0, index);
    }
  }
  return trimmed;
}

/** 用给定序号前缀拼标题：只放主考点，其余考点留给导读，保证标题可完整显示 */
function buildLessonTitleWithPrefix(prefix: string, weakPoints: LessonStudioWeakPoint[]): string {
  const name = (weakPoints[0]?.name || '').trim();
  if (!name) return `${prefix} ${PREP_TITLE_SUFFIX_FALLBACK}`;
  const withSuffix = `${prefix} ${toFocusHeadline(name)}${PREP_TITLE_SUFFIX}`;
  if (withSuffix.length <= MAX_PREP_TITLE_CHARS) return withSuffix;
  // 超长时先丢后缀：宁可标题短一点，也不能把考点名截成半截词
  return `${prefix} ${name}`;
}

/** 考点指纹：取考点名前若干字，用于兼容标题压缩规则变化前后的历史标题 */
function focusFingerprint(name?: string): string {
  return (name || '').replace(/\s+/g, '').slice(0, TITLE_FINGERPRINT_CHARS);
}

/** 课节标题：`1.2 等价无穷小代换专项突破` */
function buildLessonTitle(chapterNo: number, lessonNo: number, weakPoints: LessonStudioWeakPoint[]): string {
  return buildLessonTitleWithPrefix(`${chapterNo}.${lessonNo}`, weakPoints);
}

/**
 * 课节导读：面向学生展示（大纲卡片 / 学习页），必须是自然的课程介绍——
 * 不写「依据教情报告创建」这类过程说明，也不搬运学情分析原文与错因标签。
 * 它同时会作为「课节导读」注入 AI 备课 prompt，因此保持简洁。
 */
function buildLessonDescription(weakPoints: LessonStudioWeakPoint[]): string {
  const focus = weakPoints
    .slice(0, MAX_FOCUS_POINTS)
    .map(point => (point.name || '').trim())
    .filter(Boolean);
  if (focus.length === 0) {
    return '本课节围绕本次教学重点展开，请结合教学需要补充课节导读。';
  }
  const description =
    `本课节聚焦${focus.join('与')}，梳理判定标准与常见误判，讲清适用条件与使用边界，` +
    '并结合典型例题与变式训练巩固应用，提升解题的准确性与严谨性。';
  // 导读输入框限 300 字，超限会直接红字报警，这里兜底截断
  return description.length > MAX_LESSON_DESCRIPTION_CHARS
    ? `${description.slice(0, MAX_LESSON_DESCRIPTION_CHARS)}…`
    : description;
}

/** 旧备课课节的导读是否需要按最新规则刷新：空、超字数上限、残留公式，或仍是早期模板文案 */
function shouldRefreshPrepDescription(description?: string): boolean {
  const text = (description || '').trim();
  if (!text) return true;
  if (text.length > MAX_LESSON_DESCRIPTION_CHARS) return true;
  if (/[\\$^_{}~]/.test(text)) return true;
  return LEGACY_PREP_DESCRIPTION_MARKERS.some(marker => text.includes(marker));
}

/** 公式定界符：$..$、$$..$$、\(..\)、\[..\]（后端错因原文默认用 \(..\)） */
const MATH_PATTERN = /\\\(([\s\S]*?)\\\)|\\\[([\s\S]*?)\\\]|\$\$([\s\S]*?)\$\$|\$([^$]*)\$/g;

/**
 * 清洗错因原文里的公式与换行，得到一句可读的诊断。
 * 诊断只喂给 AI（并展示在助教对话里），不进课节导读，因此不限制得过于死板。
 */
function sanitizeDiagnosisReason(reason?: string | null): string {
  if (!reason) return '';
  const cleaned = reason
    .replace(MATH_PATTERN, ' ')
    .replace(/\\[a-zA-Z]+/g, ' ')
    .replace(/[{}^_]/g, ' ')
    // 公式被剥离后留下的空括号，如「（如 ）」
    .replace(/[（(]\s*(?:如|例如|比如)?\s*[、,，；;]?\s*[)）]/g, '')
    .replace(/\s+/g, ' ')
    // 残留的中文空隙，如「当成与 等价」
    .replace(/[\u4e00-\u9fa5]\s+[\u4e00-\u9fa5]/g, match => match.replace(/\s+/g, ''))
    .replace(/\s+([，。；：、)）])/g, '$1')
    .replace(/[，,、；;：:\s]+$/g, '')
    .trim();
  // 只剩标点的错因没有可用信息，宁可不展示（避免出现「xxx：」这样的空条目）
  if (!/[\u4e00-\u9fa5a-zA-Z0-9]/.test(cleaned)) return '';
  if (cleaned.length <= MAX_DIAGNOSIS_REASON_CHARS) return cleaned;
  // 截断到最后一个完整句子，避免出现「半句 + 省略号」
  const cut = cleaned.slice(0, MAX_DIAGNOSIS_REASON_CHARS);
  const lastStop = Math.max(cut.lastIndexOf('。'), cut.lastIndexOf('；'), cut.lastIndexOf(';'));
  return lastStop >= MAX_DIAGNOSIS_REASON_CHARS / 2 ? cut.slice(0, lastStop + 1) : `${cut}…`;
}

/**
 * 组装给 AI 的学情诊断：保留错因分析（这是生成针对性教案的关键），
 * 但只走 AI 上下文通道，避免污染课节导读等对外字段。
 */
function buildPrepDiagnosis(weakPoints: LessonStudioWeakPoint[]): string {
  return weakPoints
    .map(point => {
      const name = (point.name || '').trim();
      const type = point.errorTypeName ? `【${point.errorTypeName}】` : '';
      const reason = sanitizeDiagnosisReason(point.errorReason);
      if (!name && !reason) return '';
      return reason ? `- ${name}${type}：${reason}` : `- ${name}${type}`;
    })
    .filter(Boolean)
    .join('\n');
}

/**
 * 旧自动标题是否需要修复：只有与「当前规则应产出的标题」不一致时才刷新。
 *
 * <p>旧实现按「以专项突破结尾且超过 18 字」判定，而规则产出的标题本身就可能超过 18 字，
 * 于是每次进入都会重新写库并弹一次「已按最新规则刷新」——既是无意义写库，也是重复提示。</p>
 */
function shouldRefreshPrepTitle(title: string | undefined, weakPoints: LessonStudioWeakPoint[]): boolean {
  const text = (title || '').trim();
  if (!text) return false;
  const prefix = text.match(/^\d+(?:\.\d+)?/)?.[0];
  if (!prefix) return false;
  // 只处理本入口生成的标题：教师手写的标题（无编号或自定义后缀）一律不动
  if (!text.endsWith(PREP_TITLE_SUFFIX) && !text.endsWith(PREP_TITLE_SUFFIX_FALLBACK)) return false;
  return buildLessonTitleWithPrefix(prefix, weakPoints) !== text;
}

/**
 * 复用旧备课课节时，按最新规则修复早期自动生成的标题（过长）与导读（超限 / 含公式 / 旧模板）。
 * 只修「明显由本入口生成」的内容，教师手写的标题与导读不会被覆盖。
 */
async function refreshPrepLessonIfLegacy(
  courseId: number,
  lesson: LessonRef,
  weakPoints: LessonStudioWeakPoint[]
): Promise<void> {
  const patch: { title?: string; description?: string } = {};

  if (shouldRefreshPrepTitle(lesson.title, weakPoints)) {
    const prefix = (lesson.title || '').match(/^\d+(?:\.\d+)?/)?.[0] ?? '';
    if (prefix) {
      patch.title = buildLessonTitleWithPrefix(prefix, weakPoints);
    }
  }
  if (shouldRefreshPrepDescription(lesson.description)) {
    patch.description = buildLessonDescription(weakPoints);
  }
  if (!patch.title && !patch.description) return;

  try {
    await updateLesson(courseId, lesson.id, patch);
    ElMessage.info('已按最新规则刷新该备课课节的标题与导读');
  } catch {
    // 刷新失败不阻塞进入备课
  }
}

/**
 * 新建（或复用）一个草稿态备课课节。
 * 后端 createChapter 对「parentId > 0」的节点自动置 contentStatus=DRAFT，
 * 因此挂到章下即可得到一份不影响已发布内容的空白教案容器。
 */
async function ensurePrepLesson(
  courseId: number,
  chapters: Chapter[],
  weakPoints: LessonStudioWeakPoint[]
): Promise<PrepLessonResult | null> {
  const target = pickTargetChapter(chapters, weakPoints);
  let parentId: number;
  let chapterNo: number;
  let existingLessons: LessonRef[];

  if (target) {
    parentId = target.id;
    chapterNo = chapters.findIndex(chapter => chapter.id === target.id) + 1;
    existingLessons = collectLessons(target);
  } else {
    // 课程还没有章层：必须先建章，否则后端 isLessonNode（parentId > 0）不会把新课节当课节
    const chapterRes = await createChapterApi(courseId, {
      title: weakPoints[0] ? `${weakPoints[0].name}专项突破` : 'AI 备课专项',
      sortOrder: chapters.length + 1,
      description: buildLessonDescription(weakPoints)
    });
    const newChapterId = Number(chapterRes?.data ?? 0);
    if (!newChapterId) {
      return null;
    }
    parentId = newChapterId;
    chapterNo = chapters.length + 1;
    existingLessons = [];
  }

  // 幂等保护：已存在聚焦同一考点的课节时直接复用，避免反复点击刷出重复课节。
  // 标题里的考点名是压缩过的，这里必须用同样的压缩形式匹配；
  // 被旧规则截断过的标题（带 `…`）再用考点指纹兜底，否则同一点击就会重复新建课节。
  const primaryHeadline = weakPoints[0] ? toFocusHeadline(weakPoints[0].name) : '';
  const primaryFingerprint = focusFingerprint(weakPoints[0]?.name);
  const reused = primaryHeadline
    ? existingLessons.find(lesson => {
        const title = lesson.title || '';
        if (title.includes(primaryHeadline)) return true;
        if (!primaryFingerprint || !/[…⋯]/.test(title)) return false;
        return title.replace(/\s+/g, '').includes(primaryFingerprint);
      })
    : undefined;
  if (reused) {
    await refreshPrepLessonIfLegacy(courseId, reused, weakPoints);
    writeLessonPrepDiagnosis(courseId, reused.id, buildPrepDiagnosis(weakPoints));
    return { lessonId: reused.id, title: reused.title || '', reused: true };
  }

  // 续编号：取已有标题里的最大节号（而非课节数量），删过课节、混入手工无编号课节时都不会撞号
  const lessonNo = resolveNextLessonNo(existingLessons.map(lesson => lesson.title));
  // 续接已有的最大 sortOrder（而不是简单用数量），避免与教师手工调过序的课节撞号
  const nextSortOrder =
    existingLessons.reduce((max, lesson) => Math.max(max, lesson.sortOrder ?? 0), 0) + 1;
  const title = buildLessonTitle(chapterNo, lessonNo, weakPoints);
  const lessonRes = await createChapterApi(courseId, {
    title,
    parentId,
    sortOrder: nextSortOrder,
    description: buildLessonDescription(weakPoints),
    durationMinutes: DEFAULT_PREP_DURATION_MINUTES,
    lessonType: 'LECTURE'
  });
  const newLessonId = Number(lessonRes?.data ?? 0);
  if (!newLessonId) {
    return null;
  }
  // 错因分析只走 AI 上下文通道，不写进上面的课节导读
  writeLessonPrepDiagnosis(courseId, newLessonId, buildPrepDiagnosis(weakPoints));
  return { lessonId: newLessonId, title, reused: false };
}

/**
 * 进入课程内的课节教案工作台（Lesson Studio），AI 备课能力在该处提供。
 *
 * - 传入 weakPoints（如教情报告入口）：新建一个聚焦薄弱考点的草稿课节再备课，
 *   不再复用已发布课节的正文，避免误改既有内容；
 * - 未传 weakPoints：沿用原行为，进入课程第一个已有课节。
 */
export async function openLessonStudio(
  router: Router,
  courseId: number,
  options: LessonStudioEntryOptions = {}
): Promise<void> {
  if (options.lessonId) {
    router.push(`/course/${courseId}/lessons/${options.lessonId}/edit`);
    return;
  }

  let chapters: Chapter[] = [];
  try {
    const res = await getChapters(courseId);
    chapters = res?.data ?? [];
  } catch {
    ElMessage.warning('教学大纲加载失败，请稍后重试或手动进入课节');
    router.push(`/course/${courseId}/chapters`);
    return;
  }

  const weakPoints = normalizeWeakPoints(options.weakPoints);
  if (weakPoints.length > 0) {
    try {
      const prepLesson = await ensurePrepLesson(courseId, chapters, weakPoints);
      if (prepLesson) {
        if (prepLesson.reused) {
          ElMessage.info(`已存在聚焦「${weakPoints[0].name}」的备课课节，直接进入`);
        } else {
          ElMessage.success(`已新建备课课节《${prepLesson.title}》，正在打开 AI 备课`);
        }
        const autoPrepareQuery = options.autoPrepare === false ? '' : `?${LESSON_AI_PREP_QUERY}=1`;
        router.push(`/course/${courseId}/lessons/${prepLesson.lessonId}/edit${autoPrepareQuery}`);
        return;
      }
      ElMessage.warning('备课课节创建失败，已改为进入已有课节');
    } catch (err) {
      ElMessage.error(err instanceof Error ? err.message : '备课课节创建失败');
    }
  }

  const lessonId = findFirstLessonId(chapters);
  if (lessonId) {
    router.push(`/course/${courseId}/lessons/${lessonId}/edit`);
    return;
  }
  ElMessage.info('当前课程还没有课节，请先在教学大纲中创建课节后再使用 AI 备课');
  router.push(`/course/${courseId}/chapters`);
}
