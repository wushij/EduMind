import type { AIToolExecutionMode } from '@/types/ai/tool';

export type AIToolCategory = 'TEACHER' | 'STUDENT' | 'GENERAL';

export interface AIToolAdminVO {
  id: string;
  name: string;
  description: string;
  detailedIntro: string;
  category: AIToolCategory | string;
  icon: string;
  modelId?: string;
  route: string;
  executionMode: AIToolExecutionMode;
  tags: string;
  isRecommended: boolean;
  isHot: boolean;
  useCount: number;
  sortOrder: number;
  status: number;
  createTime?: string;
  updateTime?: string;
}

export interface AIToolSaveRequest {
  id: string;
  name: string;
  description?: string;
  detailedIntro?: string;
  category: AIToolCategory | string;
  icon?: string;
  modelId?: string;
  route?: string;
  executionMode?: AIToolExecutionMode;
  tags?: string;
  isRecommended?: boolean;
  isHot?: boolean;
  sortOrder?: number;
  status?: number;
}

export interface AIToolUpdateRequest {
  name: string;
  description?: string;
  detailedIntro?: string;
  category: AIToolCategory | string;
  icon?: string;
  modelId?: string;
  route?: string;
  executionMode?: AIToolExecutionMode;
  tags?: string;
  isRecommended?: boolean;
  isHot?: boolean;
  sortOrder?: number;
  status?: number;
}

export interface AIToolStatsVO {
  total: number;
  online: number;
  offline: number;
  teacherCount: number;
  studentCount: number;
  generalCount: number;
  totalUseCount: number;
}

export interface AIToolQuery {
  category?: string;
  status?: number | '';
  keyword?: string;
  isRecommended?: boolean;
}

export interface AIToolFlagsRequest {
  isRecommended?: boolean;
  isHot?: boolean;
}

export const TOOL_CATEGORY_OPTIONS = [
  { value: 'TEACHER', label: '教师工具' },
  { value: 'STUDENT', label: '学生工具' },
  { value: 'GENERAL', label: '通用工具' }
] as const;

export const TOOL_EXECUTION_MODE_OPTIONS = [
  { value: 'ROUTE', label: '直接路由 (ROUTE)' },
  { value: 'V05_NOTICE', label: '预告页 (V05_NOTICE)' }
] as const;

export const TOOL_ROUTE_SUGGESTIONS = [
  '/ai/question/generate',
  '/ai/exam/generate',
  '/ai/grading',
  '/learning/recommendations',
  '/ai/marketplace'
] as const;

export const TOOL_ICON_OPTIONS = [
  'EditPen', 'Document', 'CircleCheck', 'Notebook', 'DataAnalysis',
  'ChatDotRound', 'Warning', 'Reading', 'Calendar', 'Monitor',
  'Connection', 'Promotion', 'MagicStick', 'Tickets', 'Cpu'
] as const;
