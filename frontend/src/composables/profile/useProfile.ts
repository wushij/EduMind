import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/stores/auth/auth';
import { getProfile, updateProfile, bindEmail as bindEmailApi, uploadAvatar } from '@/api/system/user';
import { getUserPreferences, saveUserPreferences } from '@/api/profile/preferences';
import { getMyAiUsage } from '@/api/profile/ai-usage';
import { usePreferenceStore } from '@/stores/user/preference';
import { usePreferences } from '@/composables/profile/usePreferences';
import type { UserInfo } from '@/types/auth/auth';
import { USE_MOCK } from '@/config/mock';
import { resolveApiErrorMessage } from '@/core/http/api-error-message';
import { DEFAULT_AVATAR } from '@/constants/auth';
import type { UploadRequestOptions } from 'element-plus';
import { storeToRefs } from 'pinia';

export interface ProfileExtras {
  department?: string;
  bio?: string;
}

export function parseProfileExtras(preferencesJson?: string): ProfileExtras {
  if (!preferencesJson) {
    return {};
  }
  try {
    const parsed = JSON.parse(preferencesJson) as Record<string, unknown>;
    return {
      department: typeof parsed.department === 'string' ? parsed.department : '',
      bio: typeof parsed.bio === 'string' ? parsed.bio : ''
    };
  } catch {
    return {};
  }
}

export function maskEmail(email: string) {
  if (!email || !email.includes('@')) return email;
  const [name, domain] = email.split('@');
  if (name.length <= 2) return `${name}***@${domain}`;
  return `${name.slice(0, 2)}***${name.slice(-1)}@${domain}`;
}

export function getProfileRoleLabel(role: string | null | undefined) {
  switch (role) {
    case 'ADMIN':
      return '平台超级管理员';
    case 'TEACHER':
      return '任课主讲教师';
    case 'STUDENT':
      return '在校本科生';
    default:
      return '认证用户';
  }
}

export function useProfile() {
  const authStore = useAuthStore();
  const preferenceStore = usePreferenceStore();
  const { preferences, loading: prefLoading, saving: prefSaving } = storeToRefs(preferenceStore);
  const { availableModels, modelsLoading, fetchDynamicModels } = usePreferences();
  const isDev = import.meta.env.DEV;
  const avatarUploading = ref(false);
  const avatarLoadFailed = ref(false);
  const usageLoading = ref(false);
  const usageSnapshot = reactive({
    todayTokensUsed: 0,
    dailyTokenLimit: 100000,
    remainingPercent: 100,
    quotaStatus: '今日额度充足'
  });

  const currentUser = computed(() => authStore.currentUser);
  const currentRole = computed(() => authStore.currentRole || 'ADMIN');
  const displayAvatar = computed(() => {
    if (avatarLoadFailed.value) {
      return DEFAULT_AVATAR;
    }
    return currentUser.value?.avatar || DEFAULT_AVATAR;
  });

  const roleLabel = computed(() => getProfileRoleLabel(currentRole.value));

  const profileForm = reactive({
    realName: '',
    department: '',
    phone: '',
    bio: ''
  });

  const bindDialogVisible = ref(false);
  const bindingLoading = ref(false);
  const bindForm = reactive({
    email: '',
    code: ''
  });

  function handleAvatarError() {
    avatarLoadFailed.value = true;
  }

  function syncProfileForm(user: UserInfo, extras: ProfileExtras = {}) {
    profileForm.realName = user.realName || '';
    profileForm.phone = user.phone || '';
    profileForm.department = extras.department || user.department || '';
    profileForm.bio = extras.bio || '';
  }

  async function loadProfile() {
    try {
      const [profileRes, prefRes] = await Promise.all([
        getProfile(),
        getUserPreferences().catch(() => null)
      ]);
      if (profileRes?.data) {
        authStore.setUser(profileRes.data as UserInfo);
      }
      const user = authStore.currentUser;
      if (user) {
        syncProfileForm(user, parseProfileExtras(prefRes?.data?.preferencesJson));
      }
    } catch {
      if (authStore.currentUser) {
        syncProfileForm(authStore.currentUser);
      }
    }
  }

  async function handleSwitchRole(role: 'ADMIN' | 'TEACHER' | 'STUDENT') {
    try {
      await authStore.switchRole(role);
      ElMessage.success(`角色已一键切换至【${role === 'ADMIN' ? '超级管理员' : role === 'TEACHER' ? '任课教师' : '学生'}】视角`);
    } catch (err: any) {
      ElMessage.error(err?.message || '角色切换失败');
    }
  }

  function beforeAvatarUpload(file: File) {
    const isImage = file.type.startsWith('image/');
    const isLt5M = file.size / 1024 / 1024 < 5;
    if (!isImage) {
      ElMessage.warning('只能上传 JPG/PNG/GIF/WebP 图片');
      return false;
    }
    if (!isLt5M) {
      ElMessage.warning('头像大小不能超过 5MB');
      return false;
    }
    return true;
  }

  async function handleAvatarUpload(options: UploadRequestOptions) {
    const file = options.file as File;
    avatarUploading.value = true;
    try {
      const res = await uploadAvatar(file);
      if (res?.data) {
        avatarLoadFailed.value = false;
        authStore.setUser(res.data);
        ElMessage.success('头像已更新');
      }
    } catch (err: unknown) {
      ElMessage.error(resolveApiErrorMessage(err, '头像上传失败'));
    } finally {
      avatarUploading.value = false;
    }
  }

  async function handleSaveProfile() {
    try {
      const res = await updateProfile({
        realName: profileForm.realName.trim(),
        phone: profileForm.phone.trim()
      });

      let mergedPrefs: Record<string, unknown> = {};
      try {
        const prefRes = await getUserPreferences();
        if (prefRes?.data?.preferencesJson) {
          mergedPrefs = JSON.parse(prefRes.data.preferencesJson) as Record<string, unknown>;
        }
      } catch {
        // 偏好接口不可用时仍保存基础资料
      }

      mergedPrefs.department = profileForm.department.trim();
      mergedPrefs.bio = profileForm.bio.trim();
      await saveUserPreferences({
        preferencesJson: JSON.stringify(mergedPrefs)
      });

      if (res.data) {
        authStore.setUser(res.data as UserInfo);
        syncProfileForm(res.data as UserInfo, {
          department: profileForm.department,
          bio: profileForm.bio
        });
      }
      ElMessage.success('个人资料已成功更新并保存');
    } catch {
      if (USE_MOCK && authStore.currentUser) {
        authStore.currentUser.realName = profileForm.realName;
        authStore.currentUser.phone = profileForm.phone;
        authStore.currentUser.department = profileForm.department;
        ElMessage.success('个人资料已成功更新并保存');
      } else {
        ElMessage.error('保存失败，请重试');
      }
    }
  }

  async function loadUsageSnapshot() {
    usageLoading.value = true;
    try {
      const res = await getMyAiUsage({ logDays: 7, pageNum: 1, pageSize: 1 });
      if (res?.data) {
        usageSnapshot.todayTokensUsed = res.data.todayTokensUsed ?? 0;
        usageSnapshot.dailyTokenLimit = res.data.dailyTokenLimit ?? 100000;
        usageSnapshot.remainingPercent = res.data.remainingPercent ?? 100;
        usageSnapshot.quotaStatus = res.data.quotaStatus ?? '今日额度充足';
      }
    } catch {
      // 额度接口失败时保持默认 0，不展示假数据
    } finally {
      usageLoading.value = false;
    }
  }

  async function handleSaveAiPref() {
    if (availableModels.value.length === 0) {
      ElMessage.warning('暂无可用模型，无法保存偏好');
      return;
    }
    const modelExists = availableModels.value.some((m) => m.modelKey === preferences.value.defaultModel);
    if (!modelExists) {
      ElMessage.warning('请选择列表中的有效模型');
      return;
    }
    await preferenceStore.savePreferences();
    window.dispatchEvent(new CustomEvent('edumind:ai-usage-changed'));
  }

  function openBindDialog() {
    bindForm.email = '';
    bindForm.code = '';
    bindDialogVisible.value = true;
  }

  async function handleConfirmBind() {
    const email = bindForm.email.trim().toLowerCase();
    const code = bindForm.code.trim();

    if (!email) {
      ElMessage.warning('请输入新的电子邮箱地址');
      return;
    }
    if (!/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email)) {
      ElMessage.warning('请输入有效的电子邮箱格式');
      return;
    }
    if (!code) {
      ElMessage.warning('请输入 6 位邮箱专属验证码');
      return;
    }

    bindingLoading.value = true;
    try {
      const res = await bindEmailApi({ email, code });
      if (res?.data) {
        authStore.setUser(res.data as UserInfo);
      } else if (authStore.currentUser) {
        authStore.currentUser.email = email;
      }
      bindDialogVisible.value = false;
      ElMessage.success('安全邮箱绑定成功！已为您完成认证升级');
    } catch (err: any) {
      ElMessage.error(err?.response?.data?.message || err?.message || '邮箱绑定失败，请稍后重试');
    } finally {
      bindingLoading.value = false;
    }
  }

  onMounted(async () => {
    await Promise.all([
      loadProfile(),
      preferenceStore.loadPreferences(),
      fetchDynamicModels(),
      loadUsageSnapshot()
    ]);
    if (
      availableModels.value.length > 0
      && !availableModels.value.some((m) => m.modelKey === preferences.value.defaultModel)
    ) {
      const def = availableModels.value.find((m) => m.isDefault) || availableModels.value[0];
      if (def) {
        preferences.value.defaultModel = def.modelKey;
      }
    }
  });

  return {
    isDev,
    avatarUploading,
    avatarLoadFailed,
    currentUser,
    currentRole,
    displayAvatar,
    roleLabel,
    profileForm,
    availableModels,
    modelsLoading,
    prefLoading,
    prefSaving,
    preferences,
    usageSnapshot,
    usageLoading,
    fetchDynamicModels,
    loadUsageSnapshot,
    bindDialogVisible,
    bindingLoading,
    bindForm,
    maskEmail,
    handleAvatarError,
    handleSwitchRole,
    beforeAvatarUpload,
    handleAvatarUpload,
    handleSaveProfile,
    handleSaveAiPref,
    openBindDialog,
    handleConfirmBind,
    loadProfile
  };
}
