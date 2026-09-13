import type { PageResult } from '@/types/common/api';

export type BroadcastTargetType = 'all' | 'role';

export type BroadcastRoleCode = 'ADMIN' | 'TEACHER' | 'STUDENT';

export interface NotificationBroadcastVO {
  id: number;
  title: string;
  content: string;
  targetType: BroadcastTargetType;
  targetPayload?: BroadcastRoleCode | string;
  notifyType: string;
  priority: number;
  senderId: number;
  senderName: string;
  totalCount: number;
  readCount: number;
  createTime: string;
}

export interface BroadcastCreateRequest {
  title: string;
  content: string;
  targetType: BroadcastTargetType;
  targetPayload?: string;
  priority?: number;
}

export interface BroadcastEstimateVO {
  estimatedCount: number;
  targetType: string;
  formattedDesc: string;
}

export interface BroadcastStatsVO {
  totalBroadcasts: number;
  totalReach: number;
  totalRead: number;
  avgReadRate: number;
}

export type BroadcastListResult = PageResult<NotificationBroadcastVO>;
