import { getCourseList } from '@/api/course/course';
import { getChapters } from '@/api/course/chapter';
import { getCourseKnowledgePoints } from '@/api/course/knowledge-point';
import { getKnowledgeBases } from '@/api/knowledge/knowledge-base';
import { getDocuments } from '@/api/knowledge/document';
import { getChunks } from '@/api/knowledge/chunk';
import { getQuestions } from '@/api/question/question';
import { normalizeQuestionList } from '@/utils/question/normalize-question';
import type { HttpRequestConfig } from '@/core/http/types';
import type { Course } from '@/types/course/course';
import type { Chapter } from '@/types/course/chapter';
import type { KnowledgePoint } from '@/types/course/knowledge-point';
import type { QuestionItem } from '@/types/question/question';
import {
  PROMPT_DEMO_SCENE_VARIABLES,
  PROMPT_DEMO_SNAPSHOT,
  buildPromptDemoCourseVariables,
  buildPromptDemoRagContext,
  buildPromptDemoSnapshotRagContext,
  type PromptDemoRagSource
} from '@/constants/system/prompt-demo';

/**
 * Prompt 提示词中心 ·「一键填入真实课程 RAG 示例」解析器。
 *
 * 设计目标：演示数据不再写死课程 ID，而是实时读取当前部署里真实存在的
 * 课程 → 章节 → 知识点 → 知识库切片 → 题库题目，避免种子数据演进后演示数据再次失效。
 *
 * 降级策略：任一环节失败（Mock 模式 / 无权限 / 后端未启动）都静默回落到
 * `PROMPT_DEMO_SNAPSHOT` 快照，保证按钮永远可点、且不会弹出误导性的报错。
 */

/** 演示数据解析来源：remote = 真实接口数据，snapshot = 仓库种子快照 */
export type PromptDemoSource = 'remote' | 'snapshot';

export interface PromptDemoFillResult {
  /** 已按插槽名归一的演示变量，仅包含本次命中的键 */
  variables: Record<string, string>;
  source: PromptDemoSource;
  /** 供 UI 展示的数据来源摘要 */
  label: string;
}

/** 演示数据取自各类只读接口，失败必须静默降级，不能打扰正在编辑提示词的老师 */
const SILENT_REQUEST: HttpRequestConfig = { silent: true };

function unwrapList<T>(payload: T[] | undefined | null): T[] {
  return Array.isArray(payload) ? payload : [];
}

async function safeCall<T>(task: () => Promise<T>, fallback: T): Promise<T> {
  try {
    return await task();
  } catch {
    return fallback;
  }
}

/** 章节存在多级嵌套，演示只需要能拿到 id/title 的扁平列表 */
function flattenChapters(list: Chapter[]): Chapter[] {
  const result: Chapter[] = [];
  const walk = (items: Chapter[]) => {
    items.forEach((item) => {
      result.push(item);
      if (item.children?.length) walk(item.children);
    });
  };
  walk(list);
  return result;
}

function knowledgePointTitle(kp: KnowledgePoint): string {
  return kp.name || kp.title || `知识点 ${kp.id}`;
}

/** 快照里的去重题干是 JSON 字符串，必须解析后再使用（字符串直接展开会退化成单字符数组） */
function parseExistingQuestions(raw: string): string[] {
  try {
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.map((item) => String(item)) : [];
  } catch {
    return [];
  }
}

/** 选取演示章节：优先包含知识点最多的一章（并列取 id 更小者），无知识点时退化为首章 */
function pickDemoChapter(chapters: Chapter[], knowledgePoints: KnowledgePoint[]): Chapter | null {
  if (!chapters.length) return null;
  const countOf = (chapterId: number) =>
    knowledgePoints.filter((kp) => kp.chapterId === chapterId).length;
  const withKnowledge = chapters.filter((item) => countOf(item.id) > 0);
  if (!withKnowledge.length) return chapters[0];
  return withKnowledge.reduce((best, current) => {
    const bestCount = countOf(best.id);
    const currentCount = countOf(current.id);
    if (currentCount > bestCount) return current;
    if (currentCount === bestCount && current.id < best.id) return current;
    return best;
  });
}

/** 题目选项拼回题干，让「单插槽」的批改 / 命题模板也能看到完整题目 */
function composeQuestionStem(question: QuestionItem): string {
  const stem = (question.stem || '').trim();
  const options = unwrapList(question.options);
  if (!options.length) return stem;
  const optionLines = options
    .filter((option) => option?.content)
    .map((option) => `${option.key}. ${option.content}`);
  return optionLines.length ? `${stem}\n${optionLines.join('\n')}` : stem;
}

function buildQuestionScoringPoints(question: QuestionItem, kpIds: number[]): string {
  const score = Number(question.score) || 5;
  const reference = (question.correctAnswer || question.analysis || '').trim();
  const summary = reference.length > 60 ? `${reference.slice(0, 60)}…` : reference;
  const useSnapshotRubric =
    String(question.id) === String(PROMPT_DEMO_SNAPSHOT.questionId) &&
    kpIds.includes(Number(question.knowledgePointId));
  if (useSnapshotRubric) return PROMPT_DEMO_SNAPSHOT.questionScoringPoints;
  return JSON.stringify([
    { id: 'SP1', description: summary || '按参考答案要点采信', score }
  ]);
}

async function resolveDemoCourse(): Promise<Course | null> {
  const response = await safeCall(
    () => getCourseList({ page: 1, pageSize: 50 }, SILENT_REQUEST),
    null
  );
  const list = unwrapList(response?.data?.list);
  if (!list.length) return null;
  // 优先命中种子快照课程，保证本地演示文案（多态 / 集合框架）与其知识库、题库内容一致；
  // 其他部署环境则直接使用真实课程列表首项。
  return list.find((item) => Number(item.id) === PROMPT_DEMO_SNAPSHOT.courseId) || list[0];
}

async function resolveRagContext(
  courseId: number
): Promise<{ context: string; knowledgeBaseName: string } | null> {
  const kbResponse = await safeCall(
    () => getKnowledgeBases({ courseId }, SILENT_REQUEST),
    null
  );
  const knowledgeBase = unwrapList(kbResponse?.data)[0];
  if (!knowledgeBase) return null;

  const docResponse = await safeCall(
    () => getDocuments(knowledgeBase.id, SILENT_REQUEST),
    null
  );
  const documents = unwrapList(docResponse?.data)
    .filter((doc) => doc?.id && (!doc.parseStatus || doc.parseStatus === 'SUCCESS'))
    .slice(0, 3);

  const sources: PromptDemoRagSource[] = [];
  for (const doc of documents) {
    const chunks = await safeCall(
      () => getChunks(doc.id, { pageSize: 3 }, SILENT_REQUEST),
      []
    );
    const chunk = chunks.find((item) => (item.content || '').trim());
    if (!chunk) continue;
    sources.push({
      documentName: doc.fileName || doc.name || '课程资料',
      chapterName: chunk.heading,
      page: chunk.pageNo,
      content: chunk.content.trim()
    });
    if (sources.length >= 2) break;
  }

  if (!sources.length) return null;
  return { context: buildPromptDemoRagContext(sources), knowledgeBaseName: knowledgeBase.name };
}

async function resolveDemoQuestion(
  courseId: number,
  knowledgePointIds: number[]
): Promise<QuestionItem | null> {
  const response = await safeCall(
    () => getQuestions({ courseId, page: 1, pageSize: 50 }, SILENT_REQUEST),
    null
  );
  // 后端 QuestionVO 的 options 是 JSON 字符串、正确答案字段叫 answer，
  // 必须复用统一的 normalizer（否则选项会变成字符数组、参考答案会丢失）
  const list = normalizeQuestionList(unwrapList(response?.data?.list));
  if (!list.length) return null;
  return (
    list.find(
      (item) => Number(item.knowledgePointId) && knowledgePointIds.includes(Number(item.knowledgePointId))
    ) ||
    list.find((item) => item.type === 'SHORT_ANSWER') ||
    list[0]
  );
}

/** 完全使用仓库种子快照（接口不可用时） */
function buildSnapshotFill(reason: string): PromptDemoFillResult {
  const snapshot = PROMPT_DEMO_SNAPSHOT;
  const courseVariables = buildPromptDemoCourseVariables(snapshot);
  const ragContext = buildPromptDemoSnapshotRagContext(snapshot);
  const referenceAnswer = `${snapshot.questionAnswer}（${snapshot.questionAnalysis}）`;
  return {
    source: 'snapshot',
    label: `${snapshot.courseName} (ID: ${snapshot.courseId}) · ${snapshot.chapterName} · 种子快照（${reason}）`,
    variables: {
      ...PROMPT_DEMO_SCENE_VARIABLES,
      course_id: courseVariables.courseId,
      course_name: courseVariables.courseName,
      course_description: courseVariables.courseDescription,
      chapter_id: courseVariables.chapterId,
      chapter_name: courseVariables.chapterName,
      knowledge_point_ids: courseVariables.knowledgePointIds,
      knowledge_point_names: courseVariables.knowledgePointNames,
      knowledge_point: snapshot.knowledgePointNames.split(',')[0],
      knowledge_base_name: snapshot.knowledgeBaseName,
      retrieved_context: ragContext,
      context: ragContext,
      question_id: String(snapshot.questionId),
      question_type: snapshot.questionType,
      question_stem: snapshot.questionStem,
      reference_answer: referenceAnswer,
      standard_answer: referenceAnswer,
      max_score: String(snapshot.questionScore),
      scoring_points: snapshot.questionScoringPoints,
      existing_questions: snapshot.existingQuestions
    }
  };
}

/**
 * 解析「一键填入」所需的全部演示变量。
 * 任何一步失败都不会抛出异常，最差情况返回种子快照。
 */
export async function resolvePromptDemoFill(): Promise<PromptDemoFillResult> {
  const course = await safeCall(() => resolveDemoCourse(), null);
  if (!course) return buildSnapshotFill('课程接口不可用');

  const courseId = Number(course.id);
  const courseName = course.title || course.name || `课程 ${courseId}`;
  const courseDescription = course.description || PROMPT_DEMO_SNAPSHOT.courseDescription;
  /** 命中种子课程时沿用快照里已对齐真实题库的示例问题，其它课程则依据真实知识点改写 */
  const isSnapshotCourse = courseId === PROMPT_DEMO_SNAPSHOT.courseId;

  const [chapterResponse, knowledgePointResponse] = await Promise.all([
    safeCall(() => getChapters(courseId, SILENT_REQUEST), null),
    safeCall(() => getCourseKnowledgePoints(courseId, undefined, SILENT_REQUEST), null)
  ]);

  const chapters = flattenChapters(unwrapList(chapterResponse?.data));
  const knowledgePointList = unwrapList(knowledgePointResponse?.data);
  const chapter = pickDemoChapter(chapters, knowledgePointList);
  const chapterKnowledgePoints = chapter
    ? knowledgePointList
        .filter((kp) => kp.chapterId === chapter.id)
        .sort((a, b) => a.id - b.id)
        .slice(0, 3)
    : [];
  const knowledgePointIds = chapterKnowledgePoints.map((kp) => kp.id);
  const knowledgePointNames = chapterKnowledgePoints.map(knowledgePointTitle);

  const rag = await safeCall(() => resolveRagContext(courseId), null);
  const question = await safeCall(() => resolveDemoQuestion(courseId, knowledgePointIds), null);

  const ragContext = rag?.context || buildPromptDemoSnapshotRagContext();
  const variables: Record<string, string> = {
    ...PROMPT_DEMO_SCENE_VARIABLES,
    course_id: String(courseId),
    course_name: courseName,
    course_description: courseDescription,
    retrieved_context: ragContext,
    context: ragContext
  };

  if (chapter) {
    variables.chapter_id = String(chapter.id);
    variables.chapter_name = chapter.title;
  } else {
    variables.chapter_id = String(PROMPT_DEMO_SNAPSHOT.chapterId);
    variables.chapter_name = PROMPT_DEMO_SNAPSHOT.chapterName;
  }

  if (knowledgePointNames.length) {
    variables.knowledge_point_ids = knowledgePointIds.join(',');
    variables.knowledge_point_names = knowledgePointNames.join(',');
    variables.knowledge_point = knowledgePointNames[0];
  } else {
    const snapshotVariables = buildPromptDemoCourseVariables();
    variables.knowledge_point_ids = snapshotVariables.knowledgePointIds;
    variables.knowledge_point_names = snapshotVariables.knowledgePointNames;
    variables.knowledge_point = snapshotVariables.knowledgePointNames.split(',')[0];
  }

  if (question) {
    const referenceAnswer = (
      question.correctAnswer || question.analysis || ''
    ).trim();
    variables.question_id = String(question.id);
    variables.question_type = question.type;
    variables.question_stem = composeQuestionStem(question);
    variables.reference_answer = referenceAnswer;
    variables.standard_answer = referenceAnswer;
    variables.max_score = String(Number(question.score) || PROMPT_DEMO_SNAPSHOT.questionScore);
    variables.scoring_points = buildQuestionScoringPoints(question, knowledgePointIds);
    // 去重清单里的题干必须全部来自真实题库，不能把快照的固定文案串到别的课程上
    const existingStems = isSnapshotCourse
      ? [question.stem, ...parseExistingQuestions(PROMPT_DEMO_SNAPSHOT.existingQuestions)]
      : [question.stem];
    variables.existing_questions = JSON.stringify(
      Array.from(new Set(existingStems.filter((stem) => Boolean(stem)))).slice(0, 3)
    );
    if (!isSnapshotCourse) {
      // 非种子课程时，学生作答示例必须跟着真实题目走，否则会出现「题目是 A、作答在讲 B」
      const brief = referenceAnswer.length > 40 ? `${referenceAnswer.slice(0, 40)}…` : referenceAnswer;
      variables.student_answer = `${brief}（仅复述结论，未展开推导过程）`;
    }
  } else {
    variables.question_id = String(PROMPT_DEMO_SNAPSHOT.questionId);
    variables.question_type = PROMPT_DEMO_SNAPSHOT.questionType;
    variables.question_stem = PROMPT_DEMO_SNAPSHOT.questionStem;
    variables.reference_answer = `${PROMPT_DEMO_SNAPSHOT.questionAnswer}（${PROMPT_DEMO_SNAPSHOT.questionAnalysis}）`;
    variables.standard_answer = variables.reference_answer;
    variables.max_score = String(PROMPT_DEMO_SNAPSHOT.questionScore);
    variables.scoring_points = PROMPT_DEMO_SNAPSHOT.questionScoringPoints;
    variables.existing_questions = PROMPT_DEMO_SNAPSHOT.existingQuestions;
  }

  if (rag?.knowledgeBaseName) {
    variables.knowledge_base_name = rag.knowledgeBaseName;
  }

  if (!isSnapshotCourse) {
    // 课程域与场景文案解耦：换课程后示例提问与课题必须改写为真实知识点，避免「课程是 A、示例讲 B」
    const primaryKnowledgePoint = knowledgePointNames[0] || courseName;
    variables.question = `请结合「${courseName}」的课程资料，说明知识点「${primaryKnowledgePoint}」的核心要点与常见误区。`;
    variables.lesson_title = primaryKnowledgePoint;
  }

  const labelParts = [
    `${courseName} (ID: ${courseId})`,
    variables.chapter_name,
    knowledgePointNames.length ? knowledgePointNames.join(' / ') : ''
  ].filter(Boolean);

  return {
    source: 'remote',
    label: labelParts.join(' · '),
    variables
  };
}

/**
 * 把解析结果按当前模板已声明的插槽写入表单模型，
 * 只覆盖模板真正用到的变量，避免污染其它模板的测试入参。
 */
export function applyPromptDemoVariables(
  target: Record<string, string>,
  variables: Record<string, string>,
  slotNames: string[]
): number {
  let filled = 0;
  slotNames.forEach((name) => {
    const value = variables[name];
    if (typeof value === 'string' && value !== '') {
      target[name] = value;
      filled += 1;
    }
  });
  return filled;
}
