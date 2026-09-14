export interface OperLogVO {
  id: number;
  tenantId?: number;
  title: string;
  businessType: number;
  businessTypeText?: string;
  method: string;
  requestMethod: string;
  operUserId?: number;
  operName: string;
  operUrl: string;
  operIp: string;
  operParam?: string;
  jsonResult?: string;
  status: number; // 0: 正常, 1: 异常
  errorMsg?: string;
  costTime: number;
  operTime: string;
}

export interface OperLogPageQuery {
  pageNo?: number;
  pageSize?: number;
  title?: string;
  operName?: string;
  businessType?: number | null;
  status?: number | null;
  startTime?: string;
  endTime?: string;
  operUserId?: number;
}

export interface OperLogStatsVO {
  totalCount: number;
  todayCount: number;
  successRate: number;
  errorCount: number;
  avgCostTime: number;
}
