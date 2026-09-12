<template>
  <div class="profile-page-container">
    <!-- 1. 顶部个人名片栏 (胶囊名片 + 演示快速切角色) -->
    <div class="profile-header-card">
      <div class="user-avatar-block">
        <img :src="currentUser?.avatar" alt="Avatar" class="avatar-img" />
      </div>

      <div class="user-main-info">
        <div class="name-title-row">
          <h1 class="user-name">{{ currentUser?.realName || '平台学者' }}</h1>
          <span class="username-tag">@{{ currentUser?.username }}</span>
          <span class="role-badge-pill">{{ roleLabel }}</span>
        </div>
        <p class="user-dept">{{ currentUser?.department || '教务处与信息化中心' }}</p>
      </div>

      <!-- 快捷角色一键切换演示器 (用于评审演示快速切视角) -->
      <div v-if="isDev" class="demo-role-switcher">
        <span class="switcher-hint">演示角色切换：</span>
        <div class="switcher-pills">
          <button
            type="button"
            class="role-switch-pill"
            :class="{ active: currentRole === 'ADMIN' }"
            @click="handleSwitchRole('ADMIN')"
          >
            <el-icon class="role-icon"><Management /></el-icon>
            <span>超级管理员</span>
          </button>
          <button
            type="button"
            class="role-switch-pill"
            :class="{ active: currentRole === 'TEACHER' }"
            @click="handleSwitchRole('TEACHER')"
          >
            <el-icon class="role-icon"><School /></el-icon>
            <span>任课教师</span>
          </button>
          <button
            type="button"
            class="role-switch-pill"
            :class="{ active: currentRole === 'STUDENT' }"
            @click="handleSwitchRole('STUDENT')"
          >
            <el-icon class="role-icon"><Reading /></el-icon>
            <span>在校学生</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 2. 主体分栏：左侧基本资料表单，右侧 AI 偏好与配额 -->
    <div class="profile-main-grid">
      <!-- 左列：基本信息修改 -->
      <div class="profile-panel-card">
        <div class="panel-header-line">
          <el-icon class="panel-icon"><User /></el-icon>
          <h3 class="panel-title">基本信息与个人资料</h3>
        </div>

        <el-form label-position="top" class="profile-capsule-form">
          <el-form-item label="登录账号 (唯一识别码)">
            <input
              :value="currentUser?.username"
              type="text"
              class="capsule-input"
              disabled
            />
          </el-form-item>

          <el-form-item label="真实姓名">
            <input
              v-model="profileForm.realName"
              type="text"
              class="capsule-input"
              placeholder="请输入您的真实姓名"
            />
          </el-form-item>

          <el-form-item label="所属院系 / 教学机构">
            <input
              v-model="profileForm.department"
              type="text"
              class="capsule-input"
              placeholder="例如：计算机科学与工程学院"
            />
          </el-form-item>

          <el-form-item label="安全联系邮箱">
            <div class="email-status-card">
              <div class="email-info-left">
                <span class="email-address">{{ currentUser?.email || profileForm.email || '未绑定电子邮箱' }}</span>
                <span v-if="currentUser?.email || profileForm.email" class="status-badge bound">
                  <svg viewBox="0 0 24 24" class="badge-icon" fill="none" stroke="currentColor" stroke-width="2.5">
                    <polyline points="20 6 9 17 4 12"></polyline>
                  </svg>
                  已验证绑定
                </span>
                <span v-else class="status-badge unbound">
                  未绑定
                </span>
              </div>
              <button
                type="button"
                class="email-action-btn"
                @click="openBindDialog"
              >
                {{ (currentUser?.email || profileForm.email) ? '更换绑定' : '立即绑定' }}
              </button>
            </div>
          </el-form-item>

          <el-form-item label="联系电话">
            <input
              v-model="profileForm.phone"
              type="tel"
              class="capsule-input"
              placeholder="请输入 11 位手机号码"
            />
          </el-form-item>

          <el-form-item label="教学与学术研究方向简介">
            <textarea
              v-model="profileForm.bio"
              rows="3"
              class="capsule-textarea"
              placeholder="简述您的授课领域或主要研究兴趣..."
            ></textarea>
          </el-form-item>

          <div class="form-submit-row">
            <button
              type="button"
              class="capsule-save-btn"
              @click="handleSaveProfile"
            >
              <span>保存个人资料修改</span>
            </button>
          </div>
        </el-form>
      </div>

      <!-- 右列：AI 模型偏好设置与 Token 额度 -->
      <div class="profile-side-column">
        <!-- A. AI 助教偏好配置 -->
        <div class="profile-panel-card">
          <div class="panel-header-line">
            <el-icon class="panel-icon"><Cpu /></el-icon>
            <h3 class="panel-title">AI 大模型推理与答疑偏好</h3>
          </div>

          <div class="preference-section">
            <div class="pref-item">
              <span class="pref-label">默认教学大模型引擎：</span>
              <div class="pill-radio-group">
                <span
                  v-for="model in modelOptions"
                  :key="model.value"
                  class="pill-radio-opt"
                  :class="{ active: aiPref.model === model.value }"
                  @click="aiPref.model = model.value"
                >
                  {{ model.label }}
                </span>
              </div>
            </div>

            <div class="pref-item">
              <span class="pref-label">生成推导创造性（Temperature 温度）：</span>
              <div class="pill-radio-group">
                <span
                  class="pill-radio-opt"
                  :class="{ active: aiPref.temp === 0.2 }"
                  @click="aiPref.temp = 0.2"
                >
                  严谨学术 (0.2)
                </span>
                <span
                  class="pill-radio-opt"
                  :class="{ active: aiPref.temp === 0.5 }"
                  @click="aiPref.temp = 0.5"
                >
                  平衡教学 (0.5)
                </span>
                <span
                  class="pill-radio-opt"
                  :class="{ active: aiPref.temp === 0.8 }"
                  @click="aiPref.temp = 0.8"
                >
                  启发发散 (0.8)
                </span>
              </div>
            </div>

            <div class="pref-item">
              <span class="pref-label">RAG 知识库召回切片数量 (TopK)：</span>
              <div class="pill-radio-group">
                <span
                  v-for="k in [3, 5, 8]"
                  :key="k"
                  class="pill-radio-opt"
                  :class="{ active: aiPref.topK === k }"
                  @click="aiPref.topK = k"
                >
                  Top {{ k }} 片段
                </span>
              </div>
            </div>

            <button
              type="button"
              class="capsule-ai-pref-btn"
              @click="handleSaveAiPref"
            >
              <span>保存 AI 推理偏好</span>
            </button>
          </div>
        </div>

        <!-- B. 个人 Token 额度消耗 -->
        <div class="profile-panel-card">
          <div class="panel-header-line">
            <el-icon class="panel-icon"><Lightning /></el-icon>
            <h3 class="panel-title">本学期个人 AI 算力额度</h3>
          </div>

          <div class="quota-meter-box">
            <div class="quota-top">
              <span class="quota-label">Token 消耗情况</span>
              <span class="quota-val"><strong>1,842,000</strong> / 5,000,000</span>
            </div>
            <div class="capsule-progress-track">
              <div class="capsule-progress-fill" style="width: 36.8%"></div>
            </div>
            <div class="quota-sub">
              <span>剩余 3,158,000 Tokens (已用 36.8%)</span>
              <span class="quota-status">高校校园网无限制支持</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 邮箱安全绑定 / 更换弹窗 (深度美化升级) -->
    <el-dialog
      v-model="bindDialogVisible"
      width="480px"
      destroy-on-close
      class="email-bind-dialog"
    >
      <template #header>
        <div class="bind-dialog-header">
          <div class="header-icon-badge">
            <el-icon><Message /></el-icon>
          </div>
          <div class="header-titles">
            <h3 class="title">{{ (currentUser?.email || profileForm.email) ? '更换安全绑定邮箱' : '绑定账户安全邮箱' }}</h3>
            <span class="subtitle">绑定后可用于接收安全验证码及敏感操作身份核验</span>
          </div>
        </div>
      </template>

      <div class="bind-dialog-body">
        <div class="dialog-notice-box">
          <el-icon class="notice-icon"><InfoFilled /></el-icon>
          <span>电子邮箱为账号首要密保渠道，绑定成功后可用于密码找回与极速安全登录。</span>
        </div>

        <el-form label-position="top" class="bind-dialog-form">
          <el-form-item label="电子邮箱地址">
            <el-input
              v-model="bindForm.email"
              type="email"
              size="large"
              placeholder="请输入有效的电子邮箱 (例如: name@univ.edu.cn)"
              :prefix-icon="Message"
              clearable
            />
          </el-form-item>

          <el-form-item label="邮箱专属验证码">
            <div class="dialog-code-row">
              <el-input
                v-model="bindForm.code"
                type="text"
                maxlength="6"
                size="large"
                placeholder="请输入 6 位验证码"
                :prefix-icon="Key"
                class="code-input"
              />
              <EmailCodeBtn
                :email="bindForm.email"
                scene="bind"
                size="large"
                class="dialog-code-btn"
              />
            </div>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div class="dialog-footer-actions">
          <el-button size="large" @click="bindDialogVisible = false">
            取消
          </el-button>
          <el-button
            type="primary"
            size="large"
            :loading="bindingLoading"
            @click="handleConfirmBind"
          >
            {{ bindingLoading ? '正在核验绑定...' : '确认绑定' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue';
import { ElMessage } from 'element-plus';
import {
  Management,
  School,
  Reading,
  User,
  Cpu,
  Lightning,
  Message,
  Key,
  InfoFilled
} from '@element-plus/icons-vue';
import { useAuthStore } from '@/stores/auth/auth';
import { updateProfile, bindEmail as bindEmailApi } from '@/api/system/user';
import EmailCodeBtn from '@/components/common/EmailCodeBtn.vue';
import { USE_MOCK } from '@/config/mock';

const authStore = useAuthStore();
const isDev = import.meta.env.DEV;

const currentUser = computed(() => authStore.currentUser);
const currentRole = computed(() => authStore.currentRole || 'ADMIN');

const roleLabel = computed(() => {
  switch (currentRole.value) {
    case 'ADMIN':
      return '平台超级管理员';
    case 'TEACHER':
      return '任课主讲教师';
    case 'STUDENT':
      return '在校本科生';
    default:
      return '认证用户';
  }
});

const profileForm = reactive({
  realName: currentUser.value?.realName || '',
  department: currentUser.value?.department || '信息化教学与网络中心',
  email: currentUser.value?.email || 'admin@edumind.edu.cn',
  phone: '13800138000',
  bio: '从事智能教育技术大模型算法研发与高校混合式教学改革试点。'
});

const modelOptions = [
  { label: 'DeepSeek-V3 (首推)', value: 'DeepSeek-V3' },
  { label: 'Qwen-2.5-72B', value: 'Qwen-2.5-72B' },
  { label: 'Claude-3.5-Sonnet', value: 'Claude-3.5-Sonnet' }
];

const aiPref = reactive({
  model: 'DeepSeek-V3',
  temp: 0.5,
  topK: 5
});

async function handleSwitchRole(role: 'ADMIN' | 'TEACHER' | 'STUDENT') {
  try {
    await authStore.switchRole(role);
    ElMessage.success(`角色已一键切换至【${role === 'ADMIN' ? '超级管理员' : role === 'TEACHER' ? '任课教师' : '学生'}】视角`);
  } catch (err: any) {
    ElMessage.error(err?.message || '角色切换失败');
  }
}

async function handleSaveProfile() {
  try {
    const res = await updateProfile({
      realName: profileForm.realName,
      department: profileForm.department,
      email: profileForm.email
    });
    if (res.data) authStore.setUser(res.data);
    ElMessage.success('个人资料已成功更新并保存');
  } catch {
    if (USE_MOCK && authStore.currentUser) {
      authStore.currentUser.realName = profileForm.realName;
      authStore.currentUser.department = profileForm.department;
      ElMessage.success('个人资料已成功更新并保存');
    } else {
      ElMessage.error('保存失败，请重试');
    }
  }
}

function handleSaveAiPref() {
  ElMessage.success(`AI 推理偏好已保存：默认使用 ${aiPref.model}，温度 ${aiPref.temp}`);
}

// 邮箱绑定弹窗状态与交互
const bindDialogVisible = ref(false);
const bindingLoading = ref(false);
const bindForm = reactive({
  email: '',
  code: ''
});

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
      authStore.setUser(res.data);
      profileForm.email = res.data.email || email;
    } else if (authStore.currentUser) {
      authStore.currentUser.email = email;
      profileForm.email = email;
    }
    bindDialogVisible.value = false;
    ElMessage.success('安全邮箱绑定成功！已为您完成认证升级');
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err?.message || '邮箱绑定失败，请稍后重试');
  } finally {
    bindingLoading.value = false;
  }
}
</script>

<style scoped lang="scss">
.profile-page-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;

  // 1. 顶部名片栏
  .profile-header-card {
    background: #FFFFFF;
    border-radius: 18px;
    padding: 24px 28px;
    border: 1px solid #E2E8F0;
    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
    display: flex;
    align-items: center;
    gap: 24px;
    flex-wrap: wrap;

    .user-avatar-block {
      flex-shrink: 0;

      .avatar-img {
        width: 72px;
        height: 72px;
        border-radius: 50%;
        border: 2px solid #1677FF;
        box-shadow: 0 4px 12px rgba(22, 119, 255, 0.2);
        object-fit: cover;
      }
    }

    .user-main-info {
      flex: 1;
      min-width: 200px;

      .name-title-row {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 10px;
        margin-bottom: 4px;

        .user-name {
          margin: 0;
          font-size: 20px;
          font-weight: 700;
          color: #0F172A;
        }

        .username-tag {
          font-size: 13px;
          color: #94A3B8;
        }

        .role-badge-pill {
          white-space: nowrap;
          padding: 2px 10px;
          border-radius: 9999px;
          background: #1677FF;
          color: #FFFFFF;
          font-size: 10.5px;
          font-weight: 600;
          line-height: 1.4;
          box-shadow: 0 2px 6px rgba(22, 119, 255, 0.3);
        }
      }

      .user-dept {
        margin: 0;
        font-size: 13px;
        color: #64748B;
      }
    }

    .demo-role-switcher {
      display: flex;
      flex-direction: column;
      gap: 6px;
      padding: 12px 18px;
      border-radius: 14px;
      background: #F8FAFC;
      border: 1px dashed #CBD5E1;

      .switcher-hint {
        font-size: 11px;
        color: #94A3B8;
        font-weight: 600;
      }

      .switcher-pills {
        display: flex;
        align-items: center;
        gap: 6px;

        .role-switch-pill {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          padding: 5px 14px;
          border-radius: 9999px; // 长圆按钮
          border: 1px solid #E2E8F0;
          background: #FFFFFF;
          color: #475569;
          font-size: 12px;
          font-weight: 500;
          cursor: pointer;
          transition: all 0.2s;

          .role-icon {
            font-size: 14px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
          }

          &:hover {
            border-color: #93C5FD;
            color: #1677FF;

            .role-icon {
              color: #1677FF;
            }
          }

          &.active {
            background: #1677FF;
            border-color: #1677FF;
            color: #FFFFFF;
            font-weight: 600;

            .role-icon {
              color: #FFFFFF;
            }
          }
        }
      }
    }
  }

  // 2. 主体分栏
  .profile-main-grid {
    display: grid;
    grid-template-columns: 55% 45%;
    gap: 20px;

    .profile-panel-card {
      background: #FFFFFF;
      border-radius: 18px;
      border: 1px solid #E2E8F0;
      padding: 24px 28px;
      box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
      margin-bottom: 20px;

      .panel-header-line {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 20px;
        padding-bottom: 14px;
        border-bottom: 1px solid #F1F5F9;

        .panel-icon {
          font-size: 18px;
          color: #1677FF;
          display: inline-flex;
          align-items: center;
        }

        .panel-title {
          margin: 0;
          font-size: 16px;
          font-weight: 700;
          color: #0F172A;
        }
      }

      // 表单输入
      .profile-capsule-form {
        :deep(.el-form-item) {
          margin-bottom: 16px;
        }

        .capsule-input {
          width: 100%;
          height: 42px;
          border-radius: 9999px; // 长圆输入框
          border: 1px solid #E2E8F0;
          background: #FFFFFF;
          padding: 0 18px;
          font-size: 13.5px;
          color: #1E293B;
          outline: none;
          box-sizing: border-box;
          transition: all 0.2s;

          &:focus {
            border-color: #1677FF;
            box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.16);
          }

          &:disabled {
            background: #F8FAFC;
            color: #94A3B8;
            cursor: not-allowed;
          }
        }

        .email-status-card {
          display: flex;
          align-items: center;
          justify-content: space-between;
          width: 100%;
          min-height: 44px;
          padding: 6px 14px;
          background: #F8FAFC;
          border: 1px solid #E2E8F0;
          border-radius: 9999px;
          box-sizing: border-box;

          .email-info-left {
            display: flex;
            align-items: center;
            gap: 10px;

            .email-address {
              font-size: 13.5px;
              color: #1E293B;
              font-weight: 500;
            }

            .status-badge {
              display: inline-flex;
              align-items: center;
              gap: 4px;
              padding: 2px 10px;
              border-radius: 9999px;
              font-size: 11px;
              font-weight: 600;

              .badge-icon {
                width: 12px;
                height: 12px;
              }

              &.bound {
                background: #ECFDF5;
                color: #059669;
                border: 1px solid #A7F3D0;
              }

              &.unbound {
                background: #FFFBEB;
                color: #D97706;
                border: 1px solid #FDE68A;
              }
            }
          }

          .email-action-btn {
            padding: 5px 14px;
            border-radius: 9999px;
            background: #FFFFFF;
            border: 1px solid #CBD5E1;
            color: #1677FF;
            font-size: 12px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s;

            &:hover {
              background: #1677FF;
              border-color: #1677FF;
              color: #FFFFFF;
              box-shadow: 0 2px 8px rgba(22, 119, 255, 0.2);
            }
          }
        }

        .capsule-textarea {
          width: 100%;
          border-radius: 14px;
          border: 1px solid #E2E8F0;
          background: #FFFFFF;
          padding: 12px 16px;
          font-size: 13px;
          color: #1E293B;
          outline: none;
          box-sizing: border-box;
          font-family: inherit;

          &:focus {
            border-color: #1677FF;
            box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.16);
          }
        }

        .form-submit-row {
          margin-top: 10px;

          .capsule-save-btn {
            height: 42px;
            padding: 0 26px;
            border-radius: 9999px; // 胶囊保存
            background: #1677FF;
            color: #FFFFFF;
            border: none;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
            box-shadow: 0 3px 12px rgba(22, 119, 255, 0.25);
            transition: all 0.2s;

            &:hover {
              background: #4096FF;
            }
          }
        }
      }

      // AI 偏好
      .preference-section {
        display: flex;
        flex-direction: column;
        gap: 18px;

        .pref-item {
          .pref-label {
            display: block;
            font-size: 13px;
            color: #64748B;
            font-weight: 500;
            margin-bottom: 8px;
          }

          .pill-radio-group {
            display: flex;
            align-items: center;
            gap: 8px;
            flex-wrap: wrap;

            .pill-radio-opt {
              padding: 6px 16px;
              border-radius: 9999px; // 纯正长圆单选
              background: #F8FAFC;
              border: 1px solid #E2E8F0;
              color: #475569;
              font-size: 12.5px;
              font-weight: 500;
              cursor: pointer;
              transition: all 0.2s;

              &:hover {
                color: #1677FF;
                border-color: #93C5FD;
              }

              &.active {
                background: #EFF6FF;
                border-color: #1677FF;
                color: #1677FF;
                font-weight: 600;
              }
            }
          }
        }

        .capsule-ai-pref-btn {
          margin-top: 6px;
          height: 38px;
          border-radius: 9999px;
          background: #722ED1;
          color: #FFFFFF;
          border: none;
          font-size: 13px;
          font-weight: 600;
          cursor: pointer;
          box-shadow: 0 2px 8px rgba(114, 46, 209, 0.25);
          transition: all 0.2s;

          &:hover {
            background: #531DAB;
          }
        }
      }

      // 配额条
      .quota-meter-box {
        .quota-top {
          display: flex;
          justify-content: space-between;
          font-size: 13px;
          margin-bottom: 8px;

          .quota-label {
            color: #64748B;
          }

          .quota-val {
            color: #1E293B;
          }
        }

        .capsule-progress-track {
          width: 100%;
          height: 8px;
          background: #E2E8F0;
          border-radius: 9999px;
          overflow: hidden;
          margin-bottom: 8px;

          .capsule-progress-fill {
            height: 100%;
            background: linear-gradient(90deg, #1677FF 0%, #38BDF8 100%);
            border-radius: 9999px;
          }
        }

        .quota-sub {
          display: flex;
          justify-content: space-between;
          font-size: 11.5px;
          color: #94A3B8;

          .quota-status {
            color: #059669;
            font-weight: 600;
          }
        }
      }
    }
  }
}

// 邮箱绑定弹窗深度美化
:deep(.email-bind-dialog),
.email-bind-dialog {
  border-radius: 20px !important;
  overflow: hidden;

  .el-dialog__header {
    margin-right: 0;
    padding: 20px 24px;
    border-bottom: 1px solid #F1F5F9;

    .bind-dialog-header {
      display: flex;
      align-items: center;
      gap: 12px;

      .header-icon-badge {
        width: 40px;
        height: 40px;
        border-radius: 12px;
        background: #EFF6FF;
        color: #1677FF;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
        border: 1px solid #BFDBFE;
      }

      .header-titles {
        display: flex;
        flex-direction: column;
        gap: 2px;

        .title {
          margin: 0;
          font-size: 16px;
          font-weight: 700;
          color: #0F172A;
        }

        .subtitle {
          font-size: 12px;
          color: #64748B;
        }
      }
    }
  }

  .el-dialog__body {
    padding: 20px 24px 8px;
  }

  .dialog-notice-box {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    padding: 12px 16px;
    background: #EFF6FF;
    border: 1px solid #BFDBFE;
    border-radius: 14px;
    margin-bottom: 20px;
    color: #1E40AF;
    font-size: 12.5px;
    line-height: 1.5;

    .notice-icon {
      font-size: 16px;
      color: #1677FF;
      margin-top: 2px;
      flex-shrink: 0;
    }
  }

  .bind-dialog-form {
    .el-form-item {
      margin-bottom: 18px;

      :deep(.el-form-item__label) {
        font-size: 13px;
        font-weight: 600;
        color: #1E293B;
        padding-bottom: 6px;
      }
    }

    .dialog-code-row {
      display: flex;
      align-items: center;
      gap: 12px;
      width: 100%;

      .code-input {
        flex: 1;

        :deep(.el-input__inner) {
          letter-spacing: 2px;
          font-family: monospace;
          font-weight: 600;
        }
      }

      .dialog-code-btn {
        flex-shrink: 0;
        white-space: nowrap;
      }
    }
  }

  .el-dialog__footer {
    padding: 14px 24px 18px;
    border-top: 1px solid #F1F5F9;
    background: #F8FAFC;

    .dialog-footer-actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
    }
  }
}

@media (max-width: 1024px) {
  .profile-main-grid {
    grid-template-columns: 1fr !important;
  }
}
</style>
