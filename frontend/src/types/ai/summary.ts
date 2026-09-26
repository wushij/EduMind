/** AI 智能总结相关类型定义 */

/** 总结模式 */
export type SummaryMode = 'OVERVIEW' | 'CHAPTER' | 'MISTAKE' | 'REVIEW';

/** 资料来源类型：DOCUMENT=知识库文档，TEXT=自由文本 */
export type SummarySourceType = 'DOCUMENT' | 'TEXT';

/** 生成总结请求体 */
export interface SummaryGenerateRequest {
  courseId?: number;
  documentId?: number;
  content?: string;
  mode?: SummaryMode;
  title?: string;
}

/** 总结记录（列表视图，不含正文） */
export interface SummaryRecord {
  id: number;
  courseId?: number | null;
  courseName?: string | null;
  sourceType: SummarySourceType;
  documentId?: number | null;
  documentName?: string | null;
  mode: SummaryMode;
  modeLabel: string;
  title: string;
  sourceExcerpt?: string | null;
  sourceLength?: number | null;
  wordCount?: number | null;
  createTime?: string | null;
  updateTime?: string | null;
}

/** 总结记录详情（含 Markdown 正文） */
export interface SummaryRecordDetail extends SummaryRecord {
  content: string;
}

/** 流式生成完成（done 事件）返回的回执 */
export interface SummaryStreamDonePayload {
  recordId: number;
  title: string;
  wordCount: number;
  content: string;
  courseName?: string | null;
}
