export interface DashboardKpiItem {
  id: string;
  label: string;
  value: string;
  trend: string;
  trendUp: boolean;
  icon: string;
  gradient: string;
  sublabel: string;
}

export interface DashboardShortcut {
  id: string;
  title: string;
  icon: string;
  path: string;
  badge?: string;
  gradient?: string;
}

export interface RecentActivityItem {
  id: string;
  title: string;
  time: string;
  tag: string;
  tagType: 'primary' | 'success' | 'warning' | 'info';
  user: string;
  avatar: string;
}

export interface DashboardData {
  kpiStats: Record<'ADMIN' | 'TEACHER' | 'STUDENT', DashboardKpiItem[]>;
  shortcuts: DashboardShortcut[];
  recentActivities: RecentActivityItem[];
}
