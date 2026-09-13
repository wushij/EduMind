/** 平台基础信息 */
export interface SiteConfig {
  platformName: string;
  platformSubtitle: string;
  loginWelcome: string;
  registerTitle: string;
  copyright: string;
  icpEnabled: boolean;
  icpNumber: string;
  icpUrl: string;
}

/** 会话与令牌 */
export interface SessionConfig {
  tokenExpireHours: number;
  sessionSignExpireHours: number;
}

/** 文件存储上传限制与格式 */
export interface FileStoragePolicyConfig {
  maxSizeMb: number;
  allowedExtensions: string;
}

/** 接口限流与防刷矩阵 */
export interface RateLimitConfig {
  captchaPerIpMinute: number;
  loginPerIpMinute: number;
  registerPerIpMinute: number;
  smsPerIpMinute: number;
  smsSendIntervalSeconds: number;
  smsPerPhoneDaily: number;
  smsPerIpDaily: number;
  aiChatPerUserMinute: number;
}

/** 登录认证策略 */
export interface LoginAdminConfig {
  captchaEnabled: boolean;
  captchaType: 'image' | 'slider';
  smsLoginEnabled: boolean;
  smsLoginSliderCaptchaEnabled: boolean;
  emailLoginEnabled: boolean;
  emailLoginSliderCaptchaEnabled: boolean;
  rememberMe: boolean;
  maxRetryCount: number;
  maxRetryCountIp: number;
  lockTime: number;
}

/** 注册认证控制 */
export interface RegisterAdminConfig {
  enabled: boolean;
  captchaEnabled: boolean;
  captchaType: 'image' | 'slider';
  defaultRoleCode: string;
  needAudit: boolean;
  minPasswordLength: number;
  auditorUserIds: number[];
}

/** 第三方 OAuth 登录配置 */
export interface ThirdPartyOAuthConfig {
  enabled: boolean;
  appId?: string;
  appSecret?: string;
  privateKey?: string;
  publicKey?: string;
  clientId?: string;
  clientSecret?: string;
  redirectUri?: string;
}

export interface ThirdPartyConfig {
  wechat: Pick<ThirdPartyOAuthConfig, 'enabled' | 'appId' | 'appSecret'>;
  alipay: Pick<ThirdPartyOAuthConfig, 'enabled' | 'appId' | 'privateKey' | 'publicKey'>;
  github: Pick<ThirdPartyOAuthConfig, 'enabled' | 'clientId' | 'clientSecret'>;
  google: Pick<ThirdPartyOAuthConfig, 'enabled' | 'clientId' | 'clientSecret' | 'redirectUri'>;
}

/** 支付网关配置 */
export interface WechatPayConfig {
  enabled: boolean;
  mchId: string;
  appId: string;
  apiV3Key: string;
  privateKey: string;
  certSerialNo: string;
  notifyUrl: string;
}

export interface AlipayPayConfig {
  enabled: boolean;
  appId: string;
  privateKey: string;
  publicKey: string;
  signType: string;
  gatewayUrl: string;
  notifyUrl: string;
  returnUrl: string;
}

export interface PaymentConfig {
  wechatPay: WechatPayConfig;
  alipay: AlipayPayConfig;
}

/** 短信服务配置 */
export interface SmsConfig {
  enabled: boolean;
  provider: 'aliyunAuth' | 'tencent';
  accessKeyId: string;
  accessKeySecret: string;
  signName: string;
  tencentAppId: string;
  templateVerifyCode: string;
  templateModifyPhone: string;
  templateResetPassword: string;
  templateBindPhone: string;
  templateVerifyBindPhone: string;
  schemeName: string;
  codeExpireMinutes: number;
}

/** 邮件服务配置 */
export interface EmailConfig {
  enabled: boolean;
  provider: 'qq' | '163' | 'gmail' | 'custom';
  host: string;
  port: number;
  username: string;
  password?: string;
  fromName: string;
  authEnabled: boolean;
  securityType: 'SSL' | 'TLS' | 'STARTTLS' | 'NONE';
  connectionTimeoutMs: number;
  timeoutMs: number;
  writeTimeoutMs: number;
  encoding: string;
  debug: boolean;
  codeExpireMinutes: number;
  codeLength: number;
  dailyLimitPerEmail: number;
  sendIntervalSeconds: number;
}

/** 全链路安全防护 */
export interface SecurityPlatformConfig {
  disableDevtool: boolean;
  isConcurrent: boolean;
  sm4EncryptEnabled?: boolean;
  sm3SignEnabled?: boolean;
  timestampEnabled?: boolean;
  timestampWindowMs?: number;
  nonceEnabled?: boolean;
}

/** AI 角色差异化每日 Token 配额 */
export interface RoleTokenQuota {
  roleId?: number;
  roleCode?: string;
  tokensDaily?: number;
  maxTokensDaily?: number;
}

/** AI 助手全局配置 */
export interface AiConfig {
  assistantEnabled: boolean;
  globalKnowledge: string;
  answerScope: 'focus' | 'open';
  tokensPerUserDaily: number;
  roleTokenQuotas: RoleTokenQuota[];
}

/** 12大配置分组代码 */
export type ConfigGroupCode =
  | 'site'
  | 'session'
  | 'file'
  | 'rateLimit'
  | 'login'
  | 'register'
  | 'thirdParty'
  | 'payment'
  | 'sms'
  | 'email'
  | 'security'
  | 'ai';

export interface ConfigGroupMap {
  site: SiteConfig;
  session: SessionConfig;
  file: FileStoragePolicyConfig;
  rateLimit: RateLimitConfig;
  login: LoginAdminConfig;
  register: RegisterAdminConfig;
  thirdParty: ThirdPartyConfig;
  payment: PaymentConfig;
  sms: SmsConfig;
  email: EmailConfig;
  security: SecurityPlatformConfig;
  ai: AiConfig;
}

export interface SysConfigGroupVO {
  groupCode: string;
  groupName: string;
  configValue: string;
  remark?: string;
}

export interface SmsLogRecord {
  id: number;
  phone: string;
  content: string;
  smsType: string;
  templateId: string;
  provider: string;
  status: number;
  resultMsg: string;
  bizId: string;
  sendTime: string;
  createTime: string;
}

export interface EmailLogRecord {
  id?: number;
  email: string;
  subject?: string;
  content?: string;
  scene?: string;
  provider?: string;
  status: number;
  resultMsg?: string;
  ip?: string;
  createTime?: string;
}

export interface RoleOption {
  id: number;
  name: string;
  code: string;
}

export interface UserOption {
  id: number;
  label: string;
}
