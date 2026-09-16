import { ref, reactive, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';
import {
  getBaseInfo,
  getConfigGroup,
  updateConfigGroup,
  getRoleOptions,
  getUserOptions,
  getStorageConfig,
  updateStorageConfig,
  testStorageConfig,
  testSms,
  getRecentSmsLogs,
  getSmsLogs,
  testPayment,
  testMailConfig,
  getRecentEmailLogs,
  getEmailLogs
} from '@/api/system/config';
import type {
  ConfigGroupCode,
  ConfigGroupMap,
  EmailLogRecord,
  RoleOption,
  SmsLogRecord,
  UserOption,
} from '@/types/system/config';
import type { StorageConfigDTO } from '@/types/system';

export const GROUP_CODES: readonly ConfigGroupCode[] = [
  'site',
  'session',
  'file',
  'rateLimit',
  'login',
  'register',
  'thirdParty',
  'payment',
  'sms',
  'email',
  'security',
  'ai',
] as const;

export const DEFAULTS: ConfigGroupMap = {
  site: {
    platformName: 'EduMind',
    platformSubtitle: '智教云 · EduMind',
    loginWelcome: '欢迎使用 EduMind AI 智能教学赋能平台',
    registerTitle: '欢迎注册 EduMind 账号',
    copyright: 'Copyright © 2026 EduMind. All rights reserved.',
    icpEnabled: true,
    icpNumber: '京ICP备20260001号-1',
    icpUrl: 'https://beian.miit.gov.cn',
  },
  session: {
    tokenExpireHours: 24,
    sessionSignExpireHours: 24,
  },
  file: {
    maxSizeMb: 50,
    allowedExtensions:
      'jpg,jpeg,png,gif,webp,bmp,svg,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,json,xml,zip,rar,mp4,mp3,wav,avi,mov',
  },
  rateLimit: {
    captchaPerIpMinute: 40,
    loginPerIpMinute: 30,
    registerPerIpMinute: 10,
    smsPerIpMinute: 5,
    smsSendIntervalSeconds: 60,
    smsPerPhoneDaily: 10,
    smsPerIpDaily: 30,
    aiChatPerUserMinute: 8,
  },
  login: {
    captchaEnabled: true,
    captchaType: 'image',
    smsLoginEnabled: false,
    smsLoginSliderCaptchaEnabled: false,
    emailLoginEnabled: true,
    emailLoginSliderCaptchaEnabled: false,
    rememberMe: true,
    maxRetryCount: 5,
    maxRetryCountIp: 20,
    lockTime: 10,
  },
  register: {
    enabled: true,
    captchaEnabled: true,
    captchaType: 'image',
    defaultRoleCode: 'STUDENT',
    needAudit: false,
    minPasswordLength: 6,
    auditorUserIds: [],
  },
  thirdParty: {
    wechat: { enabled: false, appId: '', appSecret: '' },
    alipay: { enabled: false, appId: '', privateKey: '', publicKey: '' },
    github: { enabled: false, clientId: '', clientSecret: '' },
    google: { enabled: false, clientId: '', clientSecret: '', redirectUri: '' },
  },
  payment: {
    wechatPay: {
      enabled: false,
      mchId: '',
      appId: '',
      apiV3Key: '',
      privateKey: '',
      certSerialNo: '',
      notifyUrl: '',
    },
    alipay: {
      enabled: false,
      appId: '',
      privateKey: '',
      publicKey: '',
      signType: 'RSA2',
      gatewayUrl: 'https://openapi.alipay.com/gateway.do',
      notifyUrl: '',
      returnUrl: '',
    },
  },
  sms: {
    enabled: false,
    provider: 'aliyunAuth',
    accessKeyId: '',
    accessKeySecret: '',
    signName: '智教云',
    tencentAppId: '',
    templateVerifyCode: '100001',
    templateModifyPhone: '100002',
    templateResetPassword: '100003',
    templateBindPhone: '100004',
    templateVerifyBindPhone: '100005',
    schemeName: '',
    codeExpireMinutes: 5,
  },
  email: {
    enabled: true,
    provider: 'qq',
    host: 'smtp.qq.com',
    port: 465,
    username: 'service@edumind.com',
    password: '',
    fromName: '智教云平台团队',
    authEnabled: true,
    securityType: 'SSL',
    connectionTimeoutMs: 5000,
    timeoutMs: 5000,
    writeTimeoutMs: 5000,
    encoding: 'UTF-8',
    debug: false,
    codeExpireMinutes: 5,
    codeLength: 6,
    dailyLimitPerEmail: 20,
    sendIntervalSeconds: 60,
  },
  security: {
    disableDevtool: false,
    isConcurrent: false,
    sm4EncryptEnabled: false,
    sm3SignEnabled: false,
    timestampEnabled: true,
    timestampWindowMs: 300000,
    nonceEnabled: true,
  },
  ai: {
    assistantEnabled: true,
    globalKnowledge: '',
    answerScope: 'focus',
    tokensPerUserDaily: 100000,
    roleTokenQuotas: [],
  },
};

function cloneConfig<T>(data: T): T {
  return JSON.parse(JSON.stringify(data)) as T;
}

function parseJson(str: string | undefined): any {
  try {
    return JSON.parse(str || '{}');
  } catch {
    return {};
  }
}

// 敏感密钥字段映射清单：[groupCode]: dotPath[]
export const SECRET_FIELDS: Record<string, string[]> = {
  email: ['password'],
  thirdParty: [
    'wechat.appSecret',
    'alipay.privateKey',
    'github.clientSecret',
    'google.clientSecret',
  ],
  payment: [
    'wechatPay.apiV3Key',
    'wechatPay.privateKey',
    'alipay.privateKey',
  ],
  sms: ['accessKeySecret'],
};

function getNestedValue(obj: any, path: string): any {
  if (!obj) return undefined;
  const parts = path.split('.');
  let curr = obj;
  for (const p of parts) {
    if (curr == null) return undefined;
    curr = curr[p];
  }
  return curr;
}

function setNestedValue(obj: any, path: string, val: any) {
  if (!obj) return;
  const parts = path.split('.');
  let curr = obj;
  for (let i = 0; i < parts.length - 1; i++) {
    if (curr[parts[i]] == null) {
      curr[parts[i]] = {};
    }
    curr = curr[parts[i]];
  }
  curr[parts[parts.length - 1]] = val;
}

export async function fetchBaseInfo() {
  return getBaseInfo();
}

export async function fetchStorageConfig() {
  return getStorageConfig();
}

export async function saveStorageConfig(data: StorageConfigDTO) {
  return updateStorageConfig(data);
}

export async function testStorageConnection(data: StorageConfigDTO) {
  return testStorageConfig(data);
}

export async function fetchFilePolicyConfig() {
  return getConfigGroup('file');
}

export async function saveFilePolicyConfig(configValue: string) {
  return updateConfigGroup('file', configValue);
}

export function useSystemConfig() {
  const canEdit = ref(true);
  const loading = ref(false);
  const saving = ref(false);
  const currentTab = ref<ConfigGroupCode>('site');
  const isDirty = ref(false);

  const roleOptions = ref<RoleOption[]>([]);
  const userOptions = ref<UserOption[]>([]);

  const savedSnapshot = reactive<ConfigGroupMap>(cloneConfig(DEFAULTS));
  const draft = reactive<ConfigGroupMap>(cloneConfig(DEFAULTS));

  // 记录哪些密钥已在数据库中保存配置过
  const hasSavedSecret = computed(() => {
    const res: Record<string, boolean> = {};
    for (const [code, paths] of Object.entries(SECRET_FIELDS)) {
      for (const path of paths) {
        const val = getNestedValue(savedSnapshot[code as ConfigGroupCode], path);
        res[`${code}.${path}`] = !!(val && typeof val === 'string' && val.trim().length > 0);
      }
    }
    return res;
  });

  const forbidConcurrentLogin = computed({
    get: () => !draft.security.isConcurrent,
    set: (value: boolean) => {
      draft.security.isConcurrent = !value;
    },
  });

  function checkDirty() {
    isDirty.value = GROUP_CODES.some((code) => {
      const secretPaths = SECRET_FIELDS[code];
      if (!secretPaths || secretPaths.length === 0) {
        return JSON.stringify(draft[code]) !== JSON.stringify(savedSnapshot[code]);
      }
      // 对比草稿与快照时，若草稿密钥为空，则代表保留原值，不计入脏数据
      const dCopy = cloneConfig(draft[code]);
      for (const path of secretPaths) {
        const dVal = getNestedValue(dCopy, path);
        if (!dVal || (typeof dVal === 'string' && dVal.trim() === '')) {
          const sVal = getNestedValue(savedSnapshot[code], path);
          setNestedValue(dCopy, path, sVal || '');
        }
      }
      return JSON.stringify(dCopy) !== JSON.stringify(savedSnapshot[code]);
    });
  }

  watch(draft, checkDirty, { deep: true });
  watch(savedSnapshot, checkDirty, { deep: true });

  async function loadGroup(code: ConfigGroupCode) {
    try {
      const res = await getConfigGroup(code);
      const serverJson = res?.data?.configValue ? parseJson(res.data.configValue) : {};
      const merged = { ...DEFAULTS[code], ...serverJson };

      // 拦截并清除浏览器可能将管理员账号误注入的污染数据
      if (code === 'thirdParty') {
        if (merged.wechat?.appId === 'admin') {
          merged.wechat.appId = '';
        }
      }
      if (code === 'payment') {
        if (merged.wechatPay?.appId === 'admin') {
          merged.wechatPay.appId = '';
        }
      }

      savedSnapshot[code] = cloneConfig(merged) as any;

      // 同步缓存 AI 每日配额，确保各页面无感即时感知
      if (code === 'ai' && merged.tokensPerUserDaily) {
        try {
          localStorage.setItem('edumind_sys_ai_tokens_per_user_daily', String(merged.tokensPerUserDaily));
        } catch {
          // ignore
        }
      }

      // 对齐 AI 模型的 API 密钥机制：已配置密钥默认不回显至前端输入框，避免明文泄露
      const draftVal = cloneConfig(merged);
      const secretPaths = SECRET_FIELDS[code];
      if (secretPaths) {
        for (const path of secretPaths) {
          setNestedValue(draftVal, path, '');
        }
      }
      draft[code] = draftVal as any;
    } catch {
      // 保持默认
    }
  }

  async function loadOptions() {
    try {
      const [roleRes, userRes] = await Promise.allSettled([
        getRoleOptions(),
        getUserOptions(),
      ]);
      if (roleRes.status === 'fulfilled' && roleRes.value?.data) {
        roleOptions.value = roleRes.value.data.map((r: any) => ({
          id: r.id,
          name: r.name || r.roleName,
          code: r.code || r.roleCode,
        }));
      }
      if (userRes.status === 'fulfilled' && userRes.value?.data) {
        userOptions.value = userRes.value.data.map((u: any) => ({
          id: u.id,
          label: u.label || `${u.realName || u.username} (${u.username})`,
        }));
      }
    } catch {
      // 忽略候选项加载异常
    }
  }

  async function loadAll() {
    loading.value = true;
    try {
      await Promise.allSettled([
        ...GROUP_CODES.map((code) => loadGroup(code)),
        loadOptions(),
      ]);
    } finally {
      loading.value = false;
      checkDirty();
    }
  }

  function handleReset() {
    for (const code of GROUP_CODES) {
      const draftVal = cloneConfig(savedSnapshot[code]);
      const secretPaths = SECRET_FIELDS[code];
      if (secretPaths) {
        for (const path of secretPaths) {
          setNestedValue(draftVal, path, '');
        }
      }
      draft[code] = draftVal as any;
    }
    checkDirty();
    ElMessage.info('已恢复为上次保存的配置');
  }

  function buildSavePayload(code: ConfigGroupCode) {
    const payload = cloneConfig(draft[code]);
    const secretPaths = SECRET_FIELDS[code];
    if (secretPaths) {
      for (const path of secretPaths) {
        const dVal = getNestedValue(payload, path);
        if (!dVal || (typeof dVal === 'string' && dVal.trim() === '')) {
          // 用户留空，保留原已保存密钥
          const sVal = getNestedValue(savedSnapshot[code], path);
          setNestedValue(payload, path, sVal || '');
        } else {
          // 用户输入了新值，更新快照
          setNestedValue(savedSnapshot[code], path, dVal.trim());
        }
        // 重置 draft 输入框为留空状态
        setNestedValue(draft[code], path, '');
      }
    }
    return payload;
  }

  async function handleSaveGroup(code: ConfigGroupCode) {
    saving.value = true;
    try {
      const payload = buildSavePayload(code);
      const json = JSON.stringify(payload);
      await updateConfigGroup(code, json);
      savedSnapshot[code] = cloneConfig(payload) as any;
      if (code === 'ai') {
        const aiCfg = payload as any;
        if (aiCfg?.tokensPerUserDaily) {
          try {
            localStorage.setItem('edumind_sys_ai_tokens_per_user_daily', String(aiCfg.tokensPerUserDaily));
            window.dispatchEvent(new CustomEvent('edumind:ai-config-changed', { detail: aiCfg }));
          } catch {
            // ignore
          }
        }
      }
      checkDirty();
      ElMessage.success('配置已保存并即时生效');
    } catch (err: any) {
      ElMessage.error(err?.response?.data?.message || err?.message || '保存失败');
    } finally {
      saving.value = false;
    }
  }

  async function handleSaveAll() {
    checkDirty();
    if (!isDirty.value) {
      ElMessage.info('暂无修改，无需保存');
      return;
    }
    saving.value = true;
    try {
      for (const code of GROUP_CODES) {
        const payload = buildSavePayload(code);
        const json = JSON.stringify(payload);
        await updateConfigGroup(code, json);
        savedSnapshot[code] = cloneConfig(payload) as any;
        if (code === 'ai') {
          const aiCfg = payload as any;
          if (aiCfg?.tokensPerUserDaily) {
            try {
              localStorage.setItem('edumind_sys_ai_tokens_per_user_daily', String(aiCfg.tokensPerUserDaily));
              window.dispatchEvent(new CustomEvent('edumind:ai-config-changed', { detail: aiCfg }));
            } catch {
              // ignore
            }
          }
        }
      }
      checkDirty();
      ElMessage.success('全部系统配置已成功保存并即时生效');
    } catch (err: any) {
      ElMessage.error(err?.response?.data?.message || err?.message || '保存失败');
    } finally {
      saving.value = false;
    }
  }

  const paymentTesting = ref(false);
  const smsTesting = ref(false);
  const emailTesting = ref(false);
  const recentSmsLogs = ref<SmsLogRecord[]>([]);
  const recentEmailLogs = ref<EmailLogRecord[]>([]);

  async function fetchRecentSmsLogs(limit = 5) {
    try {
      const res = await getRecentSmsLogs(limit);
      if (res?.data) recentSmsLogs.value = res.data;
    } catch {
      // ignore
    }
  }

  async function triggerSmsTest(phone: string, templateCode?: string) {
    smsTesting.value = true;
    try {
      await testSms({
        phone,
        templateCode: templateCode || draft.sms.templateVerifyCode
      });
      ElMessage.success(`测试短信已成功发送至 ${phone}`);
      await fetchRecentSmsLogs();
    } catch (err: any) {
      ElMessage.error(err?.response?.data?.message || err?.message || '短信发送失败');
      throw err;
    } finally {
      smsTesting.value = false;
    }
  }

  async function fetchSmsLogs(params: {
    page: number;
    size: number;
    phone?: string;
    status?: number;
  }) {
    const res = await getSmsLogs({
      page: params.page,
      size: params.size,
      phone: params.phone,
      status: params.status
    });
    return {
      list: res?.data?.list || [],
      total: res?.data?.total || 0
    };
  }

  async function fetchRecentEmailLogs(limit = 5) {
    try {
      const res = await getRecentEmailLogs(limit);
      if (res?.data) recentEmailLogs.value = res.data;
    } catch {
      // ignore
    }
  }

  async function sendTestEmail(toEmail: string) {
    emailTesting.value = true;
    try {
      await testMailConfig(toEmail);
      ElMessage.success(`测试邮件已成功投递至 ${toEmail}`);
      await fetchRecentEmailLogs();
    } catch (err: any) {
      ElMessage.error(err?.response?.data?.message || err?.message || '邮件发送失败');
      throw err;
    } finally {
      emailTesting.value = false;
    }
  }

  async function fetchEmailLogs(params: {
    page: number;
    size: number;
    email?: string;
    status?: number;
  }) {
    const res = await getEmailLogs({
      page: params.page,
      size: params.size,
      email: params.email,
      status: params.status
    });
    return {
      list: res?.data?.list || [],
      total: res?.data?.total || 0
    };
  }

  async function triggerPaymentTest(type: 'wechat' | 'alipay') {
    paymentTesting.value = true;
    try {
      const res = await testPayment({ type });
      if (res?.data) {
        ElMessage.success('测试支付订单已创建');
        return {
          type,
          orderNo: res.data.orderNo || `PAY_${Date.now()}`,
          qrcode: res.data.qrcode || '',
          amount: res.data.amount || '0.01',
          status: (res.data.status as 'PENDING' | 'PAID') || 'PAID'
        };
      }
      return null;
    } catch (err: any) {
      ElMessage.error(err?.response?.data?.message || err?.message || '生成测试支付订单失败');
      throw err;
    } finally {
      paymentTesting.value = false;
    }
  }

  return {
    canEdit,
    loading,
    saving,
    currentTab,
    isDirty,
    roleOptions,
    userOptions,
    draft,
    savedSnapshot,
    hasSavedSecret,
    forbidConcurrentLogin,
    loadAll,
    loadGroup,
    handleReset,
    handleSaveGroup,
    handleSaveAll,
    paymentTesting,
    smsTesting,
    emailTesting,
    recentSmsLogs,
    recentEmailLogs,
    fetchRecentSmsLogs,
    triggerSmsTest,
    fetchSmsLogs,
    fetchRecentEmailLogs,
    sendTestEmail,
    fetchEmailLogs,
    triggerPaymentTest
  };
}
