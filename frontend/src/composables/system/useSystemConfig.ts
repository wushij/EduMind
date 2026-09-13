import { ref, reactive, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';
import {
  getConfigGroup,
  updateConfigGroup,
  getRoleOptions,
  getUserOptions,
} from '@/api/system/config';
import type {
  ConfigGroupCode,
  ConfigGroupMap,
  RoleOption,
  UserOption,
} from '@/types/system/config';

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
    platformName: '智教云 · EduMind',
    platformSubtitle: 'AI 智能教学赋能平台',
    loginWelcome: '欢迎登录智教云平台',
    registerTitle: '开启智教未来之旅',
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
      }
      checkDirty();
      ElMessage.success('全部系统配置已成功保存并即时生效');
    } catch (err: any) {
      ElMessage.error(err?.response?.data?.message || err?.message || '保存失败');
    } finally {
      saving.value = false;
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
  };
}
