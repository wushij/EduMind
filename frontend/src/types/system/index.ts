export interface SystemUser {
  id: number;
  username: string;
  realName: string;
  email: string;
  phone?: string;
  avatar?: string;
  role: 'ADMIN' | 'TEACHER' | 'STUDENT';
  status: 'ACTIVE' | 'DISABLED';
  createdAt: string;
  lastLoginAt?: string;
}

export interface UserQuery {
  keyword?: string;
  role?: string;
  status?: string;
  page?: number;
  pageSize?: number;
}

export interface UserForm {
  username: string;
  realName: string;
  password?: string;
  email: string;
  phone?: string;
  role: 'ADMIN' | 'TEACHER' | 'STUDENT';
  status: 'ACTIVE' | 'DISABLED';
}

export interface Role {
  id: number;
  code: string;
  name: string;
  description: string;
  permissions: string[];
  createdAt: string;
}

export interface AIModelConfig {
  id: number;
  modelKey: string;
  name: string;
  provider: 'DeepSeek' | 'Qwen' | 'OpenAI' | 'Zhipu';
  contextLength: number;
  maxOutputTokens: number;
  temperature: number;
  enabled: boolean;
  isDefault: boolean;
}

export interface SystemPromptTemplate {
  id: number;
  code: string;
  title: string;
  category: 'teaching' | 'question' | 'grading' | 'agent' | 'rag';
  content: string;
  variables: string[];
  version: string;
  status: 'PUBLISHED' | 'DRAFT';
  updatedAt: string;
}

export interface AuditLog {
  id: number;
  userId: number;
  username: string;
  action: string;
  endpoint: string;
  method: string;
  ip: string;
  status: number;
  durationMs: number;
  tokenCost?: number;
  createdAt: string;
}

export interface MailConfigVO {
  enabled: boolean;
  host: string;
  port: number;
  username: string;
  password?: string;
  hasPassword?: boolean;
  fromName: string;
  useSsl: boolean;
  codeExpireMinutes: number;
  codeIntervalSeconds: number;
  dailyLimitPerEmail: number;
}

export interface MailConfigDTO {
  enabled: boolean;
  host: string;
  port: number;
  username: string;
  password?: string;
  fromName: string;
  useSsl: boolean;
  codeExpireMinutes?: number;
  codeIntervalSeconds?: number;
  dailyLimitPerEmail?: number;
}
