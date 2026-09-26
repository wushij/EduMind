export interface TeachingReportWeeklyActivity {
  date: string;
  count: number;
}

export interface TeachingReportErrorCategory {
  type: string;
  name: string;
  percent: number;
}

/**
 * 考点掌握度状态。
 * unknown 表示该考点尚无实测掌握度记录，前端需展示「暂无测评数据」而非 0%。
 */
export type TeachingReportMasteryStatus = 'good' | 'normal' | 'warning' | 'danger' | 'unknown';

export interface TeachingReportWeakPoint {
  questionId?: number | string;
  questionStem?: string;
  /** 原题题型；选择题才需要渲染选项区 */
  questionType?: string | null;
  /** 原题选项 JSON 字符串，统一用 parseQuestionOptions 解析 */
  questionOptions?: string | null;
  /** 原题参考答案，用于在选项列表上标注正确项 */
  questionAnswer?: string | null;
  knowledgePointId?: number | string;
  knowledgePointName?: string;
  chapterName?: string;
  title: string;
  /** 该考点累计答错人次（同一考点的多道错题已合并累加） */
  wrongCount: number;
  /** 班级掌握度 (0~100)；null 表示无实测掌握度记录 */
  masteryRate?: number | null;
  /** 该考点掌握度的实测覆盖人数（有实测记录的学生数）；样本极小时 0% 不代表全班都不掌握 */
  masterySampleCount?: number | null;
  /** 该考点累计测评次数（knowledge_mastery.sample_count 求和），与覆盖人数成对展示 */
  masteryAssessmentCount?: number | null;
  /**
   * 该考点聚合的**不同题目数**（按 question_id 去重）；>1 表示多道错题已合并为一行。
   * 与 wrongCount（累计答错人次）口径不同：同一道题被多人答错时题目数是 1、人次可能是 N。
   */
  wrongQuestionCount?: number | null;
  /** 该考点下出现过错误的去重学生数 */
  wrongStudentCount?: number | null;
  /**
   * 该考点的错题是否全部来自空白作答（没有可归因的作答痕迹）。
   * 为 true 时列表会把它排在可归因薄弱点之后，前端需标注「N 人未作答」。
   */
  unansweredOnly?: boolean;
  /** 合并的错题明细，供「查看原题」抽屉逐题切换 */
  mergedQuestions?: TeachingReportMergedQuestion[] | null;
  errorType?: string | null;
  errorTypeName?: string | null;
  /** 错因类型是否为关键词推断（非库中显式标注），前端需标注「推断」 */
  errorTypeInferred?: boolean;
  errorReason?: string | null;
  suggestion?: string | null;
  status?: TeachingReportMasteryStatus;
  statusLabel?: string;
}

/** 同一考点下合并的单个错题明细（每个条目自带题干与自身的错因诊断，切换时不串题） */
export interface TeachingReportMergedQuestion {
  questionId?: number | string;
  questionStem?: string;
  questionType?: string | null;
  questionOptions?: string | null;
  questionAnswer?: string | null;
  /** 该题累计答错人次 */
  wrongCount: number;
  /** 该题答错学生数 */
  wrongStudentCount?: number | null;
  errorType?: string | null;
  errorTypeName?: string | null;
  /** 该题的 AI 诊断正文；空白作答或尚未归因时为 null */
  errorReason?: string | null;
}

export interface TeachingReportVO {
  courseId: number;
  courseName?: string;
  courseCode?: string;
  teacherName?: string;
  studentCount?: number;
  /** 教学大纲推进度 (0~100)：周期内有学习行为的章节数 / 总章节数 */
  syllabusProgress?: number;
  range: string;
  totalChapters: number;
  aiCallCount: number;
  /** 作业提交率 (0~100)：已提交答卷数 / 应提交答卷槽位数 */
  avgSubmissionRate: number;
  submittedCount?: number;
  expectedSubmissionCount?: number;
  gradedCount?: number;
  /** 班级测验及格率 (0~100)：已批改学生中均分 ≥ 60 的占比；0 表示暂无样本 */
  passRate?: number;
  /** 班级归一平均分 (0~100) */
  avgScore?: number;
  /** 知识点全班平均掌握度 (0~100)；null 表示无法计算 */
  knowledgeMasteryAvg?: number | null;
  /** 掌握度是否含推算成分（无实测掌握度记录时为 true） */
  masteryEstimated?: boolean;
  masteryStudentCount?: number;
  /** AI 辅助批改估算节约工时（小时） */
  savedHours?: number;
  /** savedHours 是否为估算值 */
  savedHoursEstimated?: boolean;
  /** 数据更新时间 yyyy-MM-dd HH:mm；null 表示周期内无学习行为 */
  dataUpdatedAt?: string | null;
  /** 是否存在可用于分析的真实数据 */
  hasRealData?: boolean;
  weeklyActivity?: TeachingReportWeeklyActivity[];
  errorCategories?: TeachingReportErrorCategory[];
  weakPoints: TeachingReportWeakPoint[];
}
