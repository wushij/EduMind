export const ASSIGNMENT_STATUS = {
  DRAFT: 'DRAFT',
  PUBLISHED: 'PUBLISHED',
  CLOSED: 'CLOSED'
} as const;

export type AssignmentStatus = (typeof ASSIGNMENT_STATUS)[keyof typeof ASSIGNMENT_STATUS];

export const ASSIGNMENT_STATUS_LABEL: Record<string, string> = {
  DRAFT: '草稿',
  PUBLISHED: '进行中',
  CLOSED: '已归档'
};

export const ASSIGNMENT_STATUS_TAG: Record<string, 'info' | 'warning' | 'success'> = {
  DRAFT: 'info',
  PUBLISHED: 'warning',
  CLOSED: 'success'
};

export const SUBMISSION_STATUS = {
  NOT_STARTED: 'NOT_STARTED',
  IN_PROGRESS: 'IN_PROGRESS',
  SUBMITTED: 'SUBMITTED',
  GRADED: 'GRADED',
  REVIEWED: 'REVIEWED'
} as const;

export const SUBMISSION_STATUS_LABEL: Record<string, string> = {
  NOT_STARTED: '未开始',
  IN_PROGRESS: '作答中',
  SUBMITTED: '待批改',
  GRADED: 'AI 已评 · 待确认',
  REVIEWED: '批改完成',
  PENDING: '待批改',
  AI_GRADED: 'AI 已评 · 待确认'
};

export const SUBMISSION_STATUS_TAG: Record<string, 'info' | 'warning' | 'primary' | 'success'> = {
  NOT_STARTED: 'info',
  IN_PROGRESS: 'info',
  SUBMITTED: 'warning',
  GRADED: 'success',
  REVIEWED: 'success',
  PENDING: 'warning',
  AI_GRADED: 'primary'
};

export function formatSubmissionRate(submitted: number, totalStudents: number): string {
  if (!totalStudents || totalStudents <= 0) {
    return '—';
  }
  return `${Math.round((submitted / totalStudents) * 1000) / 10}%`;
}
