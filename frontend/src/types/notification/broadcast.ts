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
  senderAvatar?: string;
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

export interface BroadcastRecipientVO {
  id: number;
  userId: number;
  username: string;
  realName?: string;
  avatar?: string;
  roleCode?: string;
  roleName?: string;
  isRead: number;
  createTime?: string;
}

export interface BroadcastRecipientSummaryVO {
  broadcastId: number;
  broadcastTitle: string;
  totalCount: number;
  readCount: number;
  unreadCount: number;
  readRate: number;
  recipients: PageResult<BroadcastRecipientVO>;
}
