export interface RoleQuotaConfig {
  id: number;
  role: 'ADMIN' | 'TEACHER' | 'STUDENT';
  roleName: string;
  dailyTokenLimit: number;
  monthlyTokenLimit: number;
  qpsLimit: number;
  usedTokensToday: number;
  usedTokensMonth: number;
  totalUsers: number;
  isUnlimited: boolean;
}

export interface UserQuotaOverride {
  userId: number;
  username: string;
  realName: string;
  role: string;
  customDailyLimit: number;
  usedTokensToday: number;
  status: 'NORMAL' | 'WARNED' | 'EXHAUSTED';
}
