<template>
  <div class="security-page-container">
    <!-- 1. 顶部 Hero 横幅 -->
    <div class="security-hero-dock">
      <div class="hero-left">
        <div class="title-with-badge">
          <div class="hero-icon-box">
            <el-icon class="hero-icon"><Lock /></el-icon>
          </div>
          <div class="hero-text">
            <div class="title-row">
              <h1 class="main-title">账号安全与密码中心</h1>
              <span class="capsule-tag">全方位防御 · 双模重置</span>
            </div>
            <p class="sub-desc">
              守护您的教学资产与个人数据安全。支持原密码快速更新与密保邮箱免密验证重置，时刻护航您的数字化教学。
            </p>
          </div>
        </div>
      </div>

      <div class="hero-right">
        <button type="button" class="hero-aux-btn" @click="handleGoToForgotPassword">
          <el-icon><Promotion /></el-icon>
          <span>外部找回密码通道</span>
        </button>
      </div>
    </div>

    <!-- 2. 安全健康度三维指标看板 -->
    <div class="security-metrics-grid">
      <!-- 指标 1：安全健康评分 -->
      <div class="metric-card score-card">
        <div class="metric-header">
          <span class="metric-title">账号综合安全指数</span>
          <span class="score-badge" :class="securityLevel.class">{{ securityLevel.label }}</span>
        </div>
        <div class="metric-body">
          <div class="score-number-row">
            <span class="score-val">{{ securityScore }}</span>
            <span class="score-total">/ 100 分</span>
          </div>
          <div class="capsule-progress-track">
            <div
              class="capsule-progress-fill"
              :style="{ width: `${securityScore}%`, background: securityLevel.color }"
            ></div>
          </div>
          <p class="metric-hint">{{ securityLevel.tip }}</p>
        </div>
      </div>

      <!-- 指标 2：安全密保邮箱 -->
      <div class="metric-card email-card">
        <div class="metric-header">
          <span class="metric-title">安全密保邮箱</span>
          <span v-if="userEmail" class="status-pill bound">
            <el-icon><CircleCheckFilled /></el-icon>
            <span>已绑定</span>
          </span>
          <span v-else class="status-pill unbound">
            <el-icon><WarningFilled /></el-icon>
            <span>未绑定</span>
          </span>
        </div>
        <div class="metric-body">
          <div class="email-display-row">
            <span class="email-addr">{{ userEmail ? maskEmail(userEmail) : '未绑定安全邮箱' }}</span>
          </div>
          <p class="metric-hint">用于免密重置密码、异地登录警报及敏感教学操作身份核验</p>
          <div class="card-action-row">
            <button type="button" class="card-text-btn" @click="openBindDialog">
              {{ userEmail ? '更换安全邮箱' : '立即绑定密保邮箱' }} →
            </button>
          </div>
        </div>
      </div>

      <!-- 指标 3：会话与访问防护 -->
      <div class="metric-card session-card">
        <div class="metric-header">
          <span class="metric-title">会话防护与防重放机制</span>
          <span class="status-pill active">
            <span class="pulse-dot"></span>
            <span>保护中</span>
          </span>
        </div>
        <div class="metric-body">
          <div class="session-info-row">
            <span class="session-label">鉴权引擎：</span>
            <span class="session-val">Sa-Token v1.38 (分布式会话)</span>
          </div>
          <div class="session-info-row">
            <span class="session-label">安全签名：</span>
            <span class="session-val">国密 SM3-HMAC / 13位时间戳防重放</span>
          </div>
          <div class="card-action-row">
            <button type="button" class="card-text-btn danger" @click="handleLogoutConfirm">
              安全退出登录
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 3. 主体工作区分栏：左侧密码管理，右侧安全准则与环境 -->
    <div class="security-main-grid">
      <!-- 左列：密码修改与重置中心 (双模切换) -->
      <div class="security-panel-card left-panel">
        <div class="panel-header-line">
          <div class="title-with-icon">
            <el-icon class="panel-icon"><Lock /></el-icon>
            <h3 class="panel-title">密码管理与身份安全重置</h3>
          </div>

          <!-- 双模分段胶囊切换器 (Pill Segmented Switcher) -->
          <div class="mode-pill-switcher">
            <button
              type="button"
              class="mode-switch-btn"
              :class="{ active: activeMode === 'password' }"
              @click="activeMode = 'password'"
            >
              <el-icon><Key /></el-icon>
              <span>原密码验证修改</span>
            </button>
            <button
              type="button"
              class="mode-switch-btn"
              :class="{ active: activeMode === 'email' }"
              @click="activeMode = 'email'"
            >
              <el-icon><Message /></el-icon>
              <span>安全邮箱验证重置</span>
              <span v-if="userEmail" class="mini-badge">推荐</span>
            </button>
          </div>
        </div>

        <!-- 模式 1：通过原密码验证修改 -->
        <div v-if="activeMode === 'password'" class="mode-content password-mode">
          <div class="mode-desc-alert">
            <el-icon class="alert-icon"><InfoFilled /></el-icon>
            <span>如果您记得当前登录密码，请输入原密码验证身份并设置新密码。</span>
          </div>

          <el-form label-position="top" class="security-capsule-form" @submit.prevent="handleChangePassword">
            <el-form-item label="当前登录密码">
              <div class="capsule-input-box">
                <el-icon class="input-icon"><Lock /></el-icon>
                <input
                  v-model="pwdForm.oldPassword"
                  :type="showOldPwd ? 'text' : 'password'"
                  class="capsule-input"
                  placeholder="请输入您当前使用的登录密码"
                />
                <button type="button" class="pwd-eye-btn" @click="showOldPwd = !showOldPwd">
                  <el-icon><View v-if="showOldPwd" /><Hide v-else /></el-icon>
                </button>
              </div>
            </el-form-item>

            <el-form-item label="设定新密码">
              <div class="capsule-input-box">
                <el-icon class="input-icon"><Key /></el-icon>
                <input
                  v-model="pwdForm.newPassword"
                  :type="showNewPwd ? 'text' : 'password'"
                  class="capsule-input"
                  placeholder="请输入 8~20 位新密码 (包含字母与数字)"
                />
                <button type="button" class="pwd-eye-btn" @click="showNewPwd = !showNewPwd">
                  <el-icon><View v-if="showNewPwd" /><Hide v-else /></el-icon>
                </button>
              </div>

              <!-- 实时密码强度计 -->
              <div v-if="pwdForm.newPassword" class="strength-meter-box">
                <div class="meter-top">
                  <span class="meter-label">密码强度：</span>
                  <span class="meter-text" :class="pwdStrength.class">{{ pwdStrength.label }}</span>
                </div>
                <div class="meter-bars">
                  <div class="bar" :class="{ active: pwdStrength.score >= 1, weak: pwdStrength.score === 1, medium: pwdStrength.score === 2, strong: pwdStrength.score === 3 }"></div>
                  <div class="bar" :class="{ active: pwdStrength.score >= 2, medium: pwdStrength.score === 2, strong: pwdStrength.score === 3 }"></div>
                  <div class="bar" :class="{ active: pwdStrength.score >= 3, strong: pwdStrength.score === 3 }"></div>
                </div>
              </div>
            </el-form-item>

            <el-form-item label="确认新密码">
              <div class="capsule-input-box">
                <el-icon class="input-icon"><Key /></el-icon>
                <input
                  v-model="pwdForm.confirmPassword"
                  :type="showConfirmPwd ? 'text' : 'password'"
                  class="capsule-input"
                  placeholder="请再次输入新密码以确认"
                />
                <button type="button" class="pwd-eye-btn" @click="showConfirmPwd = !showConfirmPwd">
                  <el-icon><View v-if="showConfirmPwd" /><Hide v-else /></el-icon>
                </button>
              </div>
            </el-form-item>

            <div class="submit-action-row">
              <button
                type="button"
                class="capsule-action-btn primary"
                :disabled="submittingPwd"
                @click="handleChangePassword"
              >
                <span v-if="submittingPwd">正在安全提交...</span>
                <span v-else>确认修改登录密码</span>
              </button>
            </div>

            <!-- 忘记密码辅助引导 -->
            <div class="forgot-hint-row">
              <span>不记得当前原密码？</span>
              <button type="button" class="link-btn" @click="activeMode = 'email'">
                切换至安全邮箱验证码免密重置
              </button>
              <span class="divider">或</span>
              <button type="button" class="link-btn" @click="handleGoToForgotPassword">
                系统外部找回通道
              </button>
            </div>
          </el-form>
        </div>

        <!-- 模式 2：通过安全邮箱验证重置 (免原密码) -->
        <div v-else class="mode-content email-mode">
          <!-- 情况 A：已绑定邮箱 -->
          <div v-if="userEmail">
            <div class="mode-desc-alert email-alert">
              <el-icon class="alert-icon"><InfoFilled /></el-icon>
              <span>无需原密码！系统将向您绑定的安全邮箱发送 6 位临时验证码，核验通过即可直接重设密码。</span>
            </div>

            <el-form label-position="top" class="security-capsule-form" @submit.prevent="handleEmailResetPassword">
              <el-form-item label="接收验证码的安全邮箱">
                <div class="email-preset-display">
                  <div class="email-info">
                    <el-icon class="email-icon"><Message /></el-icon>
                    <span class="email-text">{{ userEmail }}</span>
                    <span class="verify-badge">已认证</span>
                  </div>
                  <button type="button" class="change-email-btn" @click="openBindDialog">
                    更换邮箱
                  </button>
                </div>
              </el-form-item>

              <el-form-item label="邮箱验证码">
                <div class="capsule-code-row">
                  <div class="capsule-input-box code-input">
                    <input
                      v-model="emailResetForm.code"
                      type="text"
                      maxlength="6"
                      class="capsule-input"
                      placeholder="请输入 6 位数字验证码"
                    />
                  </div>
                  <button
                    type="button"
                    class="capsule-code-btn"
                    :disabled="sendingCode || countdown > 0"
                    @click="handleSendEmailResetCode"
                  >
                    <span v-if="sendingCode">发送中...</span>
                    <span v-else-if="countdown > 0">{{ countdown }}s 后重新获取</span>
                    <span v-else>获取验证码</span>
                  </button>
                </div>
              </el-form-item>

              <el-form-item label="设置新密码">
                <div class="capsule-input-box">
                  <el-icon class="input-icon"><Key /></el-icon>
                  <input
                    v-model="emailResetForm.newPassword"
                    :type="showEmailNewPwd ? 'text' : 'password'"
                    class="capsule-input"
                    placeholder="请输入 8~20 位新密码 (包含字母与数字)"
                  />
                  <button type="button" class="pwd-eye-btn" @click="showEmailNewPwd = !showEmailNewPwd">
                    <el-icon><View v-if="showEmailNewPwd" /><Hide v-else /></el-icon>
                  </button>
                </div>

                <!-- 实时密码强度计 -->
                <div v-if="emailResetForm.newPassword" class="strength-meter-box">
                  <div class="meter-top">
                    <span class="meter-label">密码强度：</span>
                    <span class="meter-text" :class="pwdStrength.class">{{ pwdStrength.label }}</span>
                  </div>
                  <div class="meter-bars">
                    <div class="bar" :class="{ active: pwdStrength.score >= 1, weak: pwdStrength.score === 1, medium: pwdStrength.score === 2, strong: pwdStrength.score === 3 }"></div>
                    <div class="bar" :class="{ active: pwdStrength.score >= 2, medium: pwdStrength.score === 2, strong: pwdStrength.score === 3 }"></div>
                    <div class="bar" :class="{ active: pwdStrength.score >= 3, strong: pwdStrength.score === 3 }"></div>
                  </div>
                </div>
              </el-form-item>

              <el-form-item label="确认新密码">
                <div class="capsule-input-box">
                  <el-icon class="input-icon"><Key /></el-icon>
                  <input
                    v-model="emailResetForm.confirmPassword"
                    :type="showEmailConfirmPwd ? 'text' : 'password'"
                    class="capsule-input"
                    placeholder="请再次输入新密码以确认"
                  />
                  <button type="button" class="pwd-eye-btn" @click="showEmailConfirmPwd = !showEmailConfirmPwd">
                    <el-icon><View v-if="showEmailConfirmPwd" /><Hide v-else /></el-icon>
                  </button>
                </div>
              </el-form-item>

              <div class="submit-action-row">
                <button
                  type="button"
                  class="capsule-action-btn primary"
                  :disabled="submittingEmailReset"
                  @click="handleEmailResetPassword"
                >
                  <span v-if="submittingEmailReset">正在核验并重置...</span>
                  <span v-else>安全核验并重置密码</span>
                </button>
              </div>
            </el-form>
          </div>

          <!-- 情况 B：未绑定邮箱提示与引导 -->
          <div v-else class="unbound-email-empty">
            <div class="empty-icon-box">
              <el-icon><Message /></el-icon>
            </div>
            <h4 class="empty-title">当前账号尚未绑定安全密保邮箱</h4>
            <p class="empty-desc">
              绑定邮箱后，您不仅可以在遗忘密码时随时秒级免密重置，还能接收敏感操作预警与教学通知推送。
            </p>
            <div class="empty-actions">
              <button type="button" class="capsule-action-btn primary" @click="openBindDialog">
                <el-icon><Plus /></el-icon>
                <span>立即绑定安全邮箱</span>
              </button>
              <button type="button" class="capsule-action-btn secondary" @click="activeMode = 'password'">
                <span>使用原密码修改</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 右列：安全准则与当前环境 -->
      <div class="security-side-column">
        <!-- 卡片 1：密码复杂度合规建议 (动态高亮) -->
        <div class="security-panel-card side-card">
          <div class="side-card-header">
            <el-icon class="side-icon"><Check /></el-icon>
            <h4 class="side-title">安全密码合规准则</h4>
          </div>
          <div class="rules-checklist">
            <div class="rule-item" :class="{ passed: currentCheckingPwd.length >= 8 }">
              <el-icon class="check-dot">
                <CircleCheckFilled v-if="currentCheckingPwd.length >= 8" />
                <CircleCloseFilled v-else />
              </el-icon>
              <span>长度至少达到 8 个字符（建议 8~20 位）</span>
            </div>
            <div class="rule-item" :class="{ passed: /[A-Za-z]/.test(currentCheckingPwd) }">
              <el-icon class="check-dot">
                <CircleCheckFilled v-if="/[A-Za-z]/.test(currentCheckingPwd)" />
                <CircleCloseFilled v-else />
              </el-icon>
              <span>包含英文字母（大小写混合更佳）</span>
            </div>
            <div class="rule-item" :class="{ passed: /[0-9]/.test(currentCheckingPwd) }">
              <el-icon class="check-dot">
                <CircleCheckFilled v-if="/[0-9]/.test(currentCheckingPwd)" />
                <CircleCloseFilled v-else />
              </el-icon>
              <span>包含阿拉伯数字 (0-9)</span>
            </div>
            <div class="rule-item" :class="{ passed: /[^A-Za-z0-9]/.test(currentCheckingPwd) }">
              <el-icon class="check-dot">
                <CircleCheckFilled v-if="/[^A-Za-z0-9]/.test(currentCheckingPwd)" />
                <CircleCloseFilled v-else />
              </el-icon>
              <span>建议包含特殊符号（如 !@#$%^&* 等）</span>
            </div>
          </div>
          <div class="security-tip-quote">
            <p>
              <el-icon class="tip-quote-icon"><InfoFilled /></el-icon>
              <span>定期（每 90 天）更换密码有助于杜绝撞库风险，平台全面采用 BCrypt 强盐哈希密文存储。</span>
            </p>
          </div>
        </div>

        <!-- 卡片 2：当前登录环境与凭证 -->
        <div class="security-panel-card side-card">
          <div class="side-card-header">
            <el-icon class="side-icon"><Monitor /></el-icon>
            <h4 class="side-title">当前会话与身份状态</h4>
          </div>
          <div class="env-info-list">
            <div class="env-row">
              <span class="label">登录用户名</span>
              <span class="val">@{{ currentUser?.username || 'admin' }}</span>
            </div>
            <div class="env-row">
              <span class="label">所属角色身份</span>
              <span class="val role-tag">{{ roleLabel }}</span>
            </div>
            <div class="env-row">
              <span class="label">会话有效性</span>
              <span class="val status-green">
                <span class="dot"></span>活跃通行中
              </span>
            </div>
            <div class="env-row">
              <span class="label">多端单点登录</span>
              <span class="val">已启用并发控制</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 4. 密保邮箱绑定 / 更换弹窗 -->
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
            <h3 class="title">{{ userEmail ? '更换安全绑定邮箱' : '绑定账户安全邮箱' }}</h3>
            <span class="subtitle">绑定后可用于接收安全验证码及敏感操作身份核验</span>
          </div>
        </div>
      </template>

      <div class="dialog-body-box">
        <div class="dialog-input-group">
          <label class="group-label">电子邮箱地址</label>
          <div class="capsule-input-box">
            <el-icon class="input-icon"><Message /></el-icon>
            <input
              v-model="bindForm.email"
              type="email"
              placeholder="请输入新的安全电子邮箱"
              class="capsule-input"
            />
          </div>
        </div>

        <div class="dialog-input-group">
          <label class="group-label">邮箱验证码</label>
          <div class="capsule-code-row">
            <div class="capsule-input-box code-input">
              <input
                v-model="bindForm.code"
                type="text"
                maxlength="6"
                placeholder="6 位数字验证码"
                class="capsule-input"
              />
            </div>
            <button
              type="button"
              class="capsule-code-btn"
              :disabled="sendingBindCode || bindCountdown > 0"
              @click="handleSendBindCode"
            >
              <span v-if="sendingBindCode">发送中...</span>
              <span v-else-if="bindCountdown > 0">{{ bindCountdown }}s</span>
              <span v-else>获取验证码</span>
            </button>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer-row">
          <button type="button" class="dialog-btn cancel" @click="bindDialogVisible = false">
            取消
          </button>
          <button
            type="button"
            class="dialog-btn confirm"
            :disabled="bindingLoading"
            @click="handleConfirmBind"
          >
            <span v-if="bindingLoading">正在绑定...</span>
            <span v-else>确认保存绑定</span>
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { ElMessage } from 'element-plus';
import {
  Lock,
  Key,
  Message,
  Check,
  CircleCheckFilled,
  CircleCloseFilled,
  WarningFilled,
  Promotion,
  Monitor,
  View,
  Hide,
  Plus,
  InfoFilled
} from '@element-plus/icons-vue';
import { useAuthStore } from '@/stores/auth/auth';
import { changePassword, sendEmailCode, verifyResetCode, resetPassword } from '@/api/auth/auth';
import { sendBindEmailCode, bindEmail as bindEmailApi } from '@/api/system/user';

const router = useRouter();
const authStore = useAuthStore();
const { currentUser } = storeToRefs(authStore);

// 当前工作模式：'password' (原密码修改) 或 'email' (邮箱免密重置)
const activeMode = ref<'password' | 'email'>('password');

// 密码眼睛显隐切换
const showOldPwd = ref(false);
const showNewPwd = ref(false);
const showConfirmPwd = ref(false);
const showEmailNewPwd = ref(false);
const showEmailConfirmPwd = ref(false);

// 状态加载
const submittingPwd = ref(false);
const submittingEmailReset = ref(false);
const sendingCode = ref(false);
const countdown = ref(0);
let timer: ReturnType<typeof setInterval> | null = null;

// 表单数据：原密码修改
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

// 表单数据：邮箱免密重置
const emailResetForm = reactive({
  code: '',
  newPassword: '',
  confirmPassword: ''
});

// 绑定邮箱弹窗
const bindDialogVisible = ref(false);
const bindingLoading = ref(false);
const sendingBindCode = ref(false);
const bindCountdown = ref(0);
let bindTimer: ReturnType<typeof setInterval> | null = null;
const bindForm = reactive({
  email: '',
  code: ''
});

// 邮箱正则
const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

// 计算属性：用户当前绑定邮箱
const userEmail = computed(() => currentUser.value?.email || '');

// 计算属性：角色中文名
const roleLabel = computed(() => {
  const role = authStore.currentRole;
  if (role === 'ADMIN') return '超级管理员';
  if (role === 'TEACHER') return '任课教师';
  if (role === 'STUDENT') return '在校学生';
  return '平台学者';
});

// 当前正在输入的待检测新密码
const currentCheckingPwd = computed(() => {
  return activeMode.value === 'password' ? pwdForm.newPassword : emailResetForm.newPassword;
});

// 密码强度评定
const pwdStrength = computed(() => {
  const pwd = currentCheckingPwd.value;
  if (!pwd) return { score: 0, label: '', class: '', color: '#e2e8f0' };
  let score = 0;
  if (pwd.length >= 8) score++;
  if (/[A-Z]/.test(pwd) && /[a-z]/.test(pwd)) score++;
  else if (/[A-Za-z]/.test(pwd) && /[0-9]/.test(pwd)) score++;
  if (/[^A-Za-z0-9]/.test(pwd) || (/[0-9]/.test(pwd) && /[A-Z]/.test(pwd) && /[a-z]/.test(pwd))) score++;

  if (score <= 1) return { score: 1, label: '弱 (建议加强)', class: 'weak', color: '#ef4444' };
  if (score === 2) return { score: 2, label: '中等 (符合要求)', class: 'medium', color: '#f59e0b' };
  return { score: 3, label: '极强 (安全稳固)', class: 'strong', color: '#10b981' };
});

// 综合安全评分
const securityScore = computed(() => {
  let score = 50;
  if (userEmail.value) score += 35;
  if (currentUser.value?.roles?.length) score += 15;
  return Math.min(score, 100);
});

// 安全等级
const securityLevel = computed(() => {
  const score = securityScore.value;
  if (score >= 90) {
    return { label: '安全极佳', class: 'excellent', color: '#10b981', tip: '已开启密保邮箱与分布式安全会话保护，防御系数极高' };
  }
  if (score >= 70) {
    return { label: '防护良好', class: 'good', color: '#3b82f6', tip: '密保邮箱已生效，建议每 90 天定期更新一次登录密码' };
  }
  return { label: '等级偏低', class: 'warn', color: '#f59e0b', tip: '尚未绑定安全邮箱，建议立即绑定以防密码遗忘丢失' };
});

// 邮箱脱敏展示
function maskEmail(email: string) {
  if (!email || !email.includes('@')) return email;
  const [name, domain] = email.split('@');
  if (name.length <= 2) return `${name}***@${domain}`;
  return `${name.slice(0, 2)}***${name.slice(-1)}@${domain}`;
}

// 1. 原密码修改逻辑
async function handleChangePassword() {
  const oldPwd = pwdForm.oldPassword.trim();
  const newPwd = pwdForm.newPassword.trim();
  const confirmPwd = pwdForm.confirmPassword.trim();

  if (!oldPwd) {
    ElMessage.warning('请输入当前使用的登录密码');
    return;
  }
  if (!newPwd) {
    ElMessage.warning('请输入新密码');
    return;
  }
  if (newPwd.length < 8) {
    ElMessage.warning('新密码长度不能少于 8 个字符');
    return;
  }
  if (newPwd === oldPwd) {
    ElMessage.warning('新密码不能与原密码相同');
    return;
  }
  if (newPwd !== confirmPwd) {
    ElMessage.warning('两次输入的新密码不一致，请重新核对');
    return;
  }

  submittingPwd.value = true;
  try {
    await changePassword(oldPwd, newPwd);
    ElMessage.success('密码修改成功，请使用新密码重新登录');
    authStore.logout();
    router.push('/auth/login');
  } catch (_err: any) {
    // 全局拦截器已自动提示错误信息
  } finally {
    submittingPwd.value = false;
  }
}

// 2. 邮箱免密重置逻辑：发送验证码
async function handleSendEmailResetCode() {
  if (sendingCode.value || countdown.value > 0) return;
  if (!userEmail.value) {
    ElMessage.warning('当前账号尚未绑定密保邮箱，请先绑定邮箱');
    return;
  }

  sendingCode.value = true;
  try {
    await sendEmailCode({
      email: userEmail.value,
      scene: 'resetpwd'
    });
    ElMessage.success(`验证码已成功发送至 ${maskEmail(userEmail.value)}，请前往查收`);
    countdown.value = 60;
    if (timer) clearInterval(timer);
    timer = setInterval(() => {
      countdown.value--;
      if (countdown.value <= 0) {
        clearInterval(timer!);
        timer = null;
      }
    }, 1000);
  } catch (_err: any) {
    // 全局拦截器统一提示
  } finally {
    sendingCode.value = false;
  }
}

// 2. 邮箱免密重置逻辑：提交重置
async function handleEmailResetPassword() {
  const code = emailResetForm.code.trim();
  const newPwd = emailResetForm.newPassword.trim();
  const confirmPwd = emailResetForm.confirmPassword.trim();

  if (!userEmail.value) {
    ElMessage.warning('当前账号尚未绑定安全邮箱，请先完成绑定');
    return;
  }
  if (!code) {
    ElMessage.warning('请输入 6 位邮箱验证码');
    return;
  }
  if (code.length !== 6) {
    ElMessage.warning('验证码格式不正确，应为 6 位数字');
    return;
  }
  if (!newPwd) {
    ElMessage.warning('请输入新密码');
    return;
  }
  if (newPwd.length < 8) {
    ElMessage.warning('新密码长度不能少于 8 个字符');
    return;
  }
  if (newPwd !== confirmPwd) {
    ElMessage.warning('两次输入的新密码不一致，请重新核对');
    return;
  }

  submittingEmailReset.value = true;
  try {
    // A. 校验验证码获取重置 Token
    const res = await verifyResetCode({
      email: userEmail.value,
      code
    });
    const resetToken = res.data?.resetToken || '';

    // B. 执行重置新密码
    await resetPassword({
      resetToken,
      newPassword: newPwd
    });

    ElMessage.success('密码重置成功，请使用新密码重新登录');
    authStore.logout();
    router.push('/auth/login');
  } catch (_err: any) {
    // 拦截器已提示
  } finally {
    submittingEmailReset.value = false;
  }
}

// 3. 邮箱绑定弹窗相关
function openBindDialog() {
  bindForm.email = '';
  bindForm.code = '';
  bindDialogVisible.value = true;
}

async function handleSendBindCode() {
  const targetEmail = bindForm.email.trim().toLowerCase();
  if (!targetEmail) {
    ElMessage.warning('请输入电子邮箱地址');
    return;
  }
  if (!emailRegex.test(targetEmail)) {
    ElMessage.warning('请输入合法的电子邮箱格式');
    return;
  }

  sendingBindCode.value = true;
  try {
    await sendBindEmailCode(targetEmail);
    ElMessage.success('绑定验证码已发送至该邮箱，请注意查收');
    bindCountdown.value = 60;
    if (bindTimer) clearInterval(bindTimer);
    bindTimer = setInterval(() => {
      bindCountdown.value--;
      if (bindCountdown.value <= 0) {
        clearInterval(bindTimer!);
        bindTimer = null;
      }
    }, 1000);
  } catch (_err: any) {
    // 全局拦截器已提示
  } finally {
    sendingBindCode.value = false;
  }
}

async function handleConfirmBind() {
  const targetEmail = bindForm.email.trim().toLowerCase();
  const code = bindForm.code.trim();

  if (!targetEmail) {
    ElMessage.warning('请输入电子邮箱');
    return;
  }
  if (!emailRegex.test(targetEmail)) {
    ElMessage.warning('请输入合法的电子邮箱格式');
    return;
  }
  if (!code) {
    ElMessage.warning('请输入 6 位验证码');
    return;
  }

  bindingLoading.value = true;
  try {
    const res = await bindEmailApi({ email: targetEmail, code });
    if (res?.data) {
      authStore.setUser(res.data);
    } else {
      await authStore.fetchUserInfo();
    }
    ElMessage.success('安全密保邮箱绑定成功！');
    bindDialogVisible.value = false;
  } catch (_err: any) {
    // 错误处理
  } finally {
    bindingLoading.value = false;
  }
}

// 4. 辅助跳转与注销
function handleGoToForgotPassword() {
  router.push('/auth/forgot-password');
}

async function handleLogoutConfirm() {
  try {
    await authStore.logout();
    ElMessage.success('已安全退出当前会话');
  } catch {
    // 降级
  } finally {
    router.push('/auth/login');
  }
}

onMounted(() => {
  if (!currentUser.value?.email) {
    authStore.fetchUserInfo();
  }
});

onBeforeUnmount(() => {
  if (timer) clearInterval(timer);
  if (bindTimer) clearInterval(bindTimer);
});
</script>

<style scoped lang="scss">
.security-page-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px 48px;
  color: #0f172a;
}

/* 1. 顶部 Hero 横幅 */
.security-hero-dock {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  padding: 24px 28px;
  margin-bottom: 24px;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.03);

  .hero-left {
    .title-with-badge {
      display: flex;
      align-items: center;
      gap: 16px;

      .hero-icon-box {
        width: 48px;
        height: 48px;
        border-radius: 14px;
        background: linear-gradient(135deg, #2563eb, #3b82f6);
        color: #ffffff;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 24px;
        box-shadow: 0 6px 16px rgba(37, 99, 235, 0.25);
      }

      .hero-text {
        .title-row {
          display: flex;
          align-items: center;
          gap: 12px;
          margin-bottom: 4px;

          .main-title {
            font-size: 22px;
            font-weight: 700;
            color: #0f172a;
            letter-spacing: -0.02em;
            margin: 0;
          }

          .capsule-tag {
            padding: 3px 12px;
            border-radius: 9999px;
            font-size: 12px;
            font-weight: 600;
            background: #eff6ff;
            color: #2563eb;
            border: 1px solid #dbeafe;
          }
        }

        .sub-desc {
          margin: 0;
          font-size: 13px;
          color: #64748b;
          line-height: 1.5;
        }
      }
    }
  }

  .hero-right {
    .hero-aux-btn {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 9px 18px;
      border-radius: 9999px;
      border: 1px solid #cbd5e1;
      background: #ffffff;
      color: #334155;
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        border-color: #2563eb;
        color: #2563eb;
        background: #f8fafc;
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(37, 99, 235, 0.1);
      }
    }
  }
}

/* 2. 三维指标看板 */
.security-metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 24px;

  .metric-card {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 14px;
    padding: 20px;
    box-shadow: 0 2px 10px rgba(15, 23, 42, 0.02);
    display: flex;
    flex-direction: column;
    justify-content: space-between;

    .metric-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      .metric-title {
        font-size: 14px;
        font-weight: 600;
        color: #475569;
      }

      .score-badge {
        padding: 3px 10px;
        border-radius: 9999px;
        font-size: 12px;
        font-weight: 600;

        &.excellent {
          background: #ecfdf5;
          color: #059669;
        }
        &.good {
          background: #eff6ff;
          color: #2563eb;
        }
        &.warn {
          background: #fffbeb;
          color: #d97706;
        }
      }

      .status-pill {
        display: inline-flex;
        align-items: center;
        gap: 5px;
        padding: 3px 10px;
        border-radius: 9999px;
        font-size: 12px;
        font-weight: 600;

        &.bound {
          background: #ecfdf5;
          color: #059669;
        }
        &.unbound {
          background: #fff1f2;
          color: #e11d48;
        }
        &.active {
          background: #eff6ff;
          color: #2563eb;

          .pulse-dot {
            width: 7px;
            height: 7px;
            border-radius: 50%;
            background: #2563eb;
            box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.2);
            animation: pulse 1.8s infinite;
          }
        }
      }
    }

    .metric-body {
      .score-number-row {
        display: flex;
        align-items: baseline;
        gap: 6px;
        margin-bottom: 8px;

        .score-val {
          font-size: 32px;
          font-weight: 800;
          color: #0f172a;
          line-height: 1;
        }

        .score-total {
          font-size: 13px;
          color: #94a3b8;
          font-weight: 500;
        }
      }

      .capsule-progress-track {
        height: 6px;
        background: #f1f5f9;
        border-radius: 9999px;
        overflow: hidden;
        margin-bottom: 10px;

        .capsule-progress-fill {
          height: 100%;
          border-radius: 9999px;
          transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
        }
      }

      .email-display-row {
        margin-bottom: 8px;

        .email-addr {
          font-size: 16px;
          font-weight: 700;
          color: #0f172a;
          font-family: 'SF Mono', Monaco, Inconsolata, monospace;
        }
      }

      .session-info-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 12px;
        margin-bottom: 6px;

        .session-label {
          color: #64748b;
        }

        .session-val {
          color: #1e293b;
          font-weight: 500;
        }
      }

      .metric-hint {
        margin: 0;
        font-size: 12px;
        color: #64748b;
        line-height: 1.4;
      }

      .card-action-row {
        margin-top: 12px;
        padding-top: 10px;
        border-top: 1px dashed #f1f5f9;
        display: flex;
        justify-content: flex-end;

        .card-text-btn {
          background: none;
          border: none;
          padding: 0;
          font-size: 12px;
          font-weight: 600;
          color: #2563eb;
          cursor: pointer;
          transition: color 0.15s;

          &:hover {
            color: #1d4ed8;
            text-decoration: underline;
          }

          &.danger {
            color: #ef4444;

            &:hover {
              color: #dc2626;
            }
          }
        }
      }
    }
  }
}

/* 3. 主体分栏 */
.security-main-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 24px;
  align-items: start;

  .security-panel-card {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 16px;
    padding: 26px;
    box-shadow: 0 4px 20px rgba(15, 23, 42, 0.03);

    .panel-header-line {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 22px;
      padding-bottom: 16px;
      border-bottom: 1px solid #f1f5f9;

      .title-with-icon {
        display: flex;
        align-items: center;
        gap: 10px;

        .panel-icon {
          font-size: 20px;
          color: #2563eb;
        }

        .panel-title {
          font-size: 17px;
          font-weight: 700;
          color: #0f172a;
          margin: 0;
        }
      }

      /* 分段胶囊按钮 */
      .mode-pill-switcher {
        display: inline-flex;
        background: #f1f5f9;
        padding: 4px;
        border-radius: 9999px;
        gap: 4px;

        .mode-switch-btn {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          padding: 6px 14px;
          border-radius: 9999px;
          border: none;
          background: transparent;
          color: #64748b;
          font-size: 13px;
          font-weight: 600;
          cursor: pointer;
          transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

          &:hover {
            color: #1e293b;
          }

          &.active {
            background: #ffffff;
            color: #2563eb;
            box-shadow: 0 2px 8px rgba(37, 99, 235, 0.12);
          }

          .mini-badge {
            background: #dbeafe;
            color: #1d4ed8;
            font-size: 10px;
            padding: 1px 6px;
            border-radius: 9999px;
            font-weight: 700;
          }
        }
      }
    }
  }
}

/* 模式说明条 */
.mode-desc-alert {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px 16px;
  margin-bottom: 22px;
  font-size: 13px;
  color: #475569;

  .alert-icon {
    font-size: 18px;
    color: #3b82f6;
    flex-shrink: 0;
  }

  &.email-alert {
    background: #eff6ff;
    border-color: #dbeafe;
    color: #1e40af;

    .alert-icon {
      color: #2563eb;
    }
  }
}

/* 表单胶囊样式 */
.security-capsule-form {
  :deep(.el-form-item) {
    margin-bottom: 20px;

    .el-form-item__label {
      font-size: 13px;
      font-weight: 600;
      color: #334155;
      padding-bottom: 6px;
    }
  }

  .capsule-input-box {
    position: relative;
    display: flex;
    align-items: center;
    width: 100%;
    height: 46px;
    background: #f8fafc;
    border: 1px solid #cbd5e1;
    border-radius: 9999px;
    padding: 0 16px;
    transition: all 0.2s ease;

    &:focus-within {
      background: #ffffff;
      border-color: #2563eb;
      box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
    }

    .input-icon {
      font-size: 16px;
      color: #94a3b8;
      margin-right: 10px;
      flex-shrink: 0;
    }

    .capsule-input {
      flex: 1;
      border: none;
      background: transparent;
      outline: none;
      font-size: 14px;
      color: #0f172a;
      height: 100%;
      width: 100%;

      &::placeholder {
        color: #94a3b8;
      }
    }

    .pwd-eye-btn {
      border: none;
      background: transparent;
      color: #94a3b8;
      font-size: 16px;
      cursor: pointer;
      display: flex;
      align-items: center;
      padding: 4px;
      transition: color 0.15s;

      &:hover {
        color: #475569;
      }
    }
  }

  /* 验证码复合行 */
  .capsule-code-row {
    display: flex;
    gap: 12px;
    width: 100%;

    .code-input {
      flex: 1;
    }

    .capsule-code-btn {
      height: 46px;
      padding: 0 20px;
      border-radius: 9999px;
      border: none;
      background: linear-gradient(135deg, #2563eb, #1d4ed8);
      color: #ffffff !important;
      font-size: 13px;
      font-weight: 600;
      white-space: nowrap;
      cursor: pointer;
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
      transition: all 0.2s;

      &:hover:not(:disabled) {
        transform: translateY(-1px);
        box-shadow: 0 6px 16px rgba(37, 99, 235, 0.3);
      }

      &:disabled {
        background: #94a3b8 !important;
        color: #f1f5f9 !important;
        cursor: not-allowed;
        box-shadow: none;
      }
    }
  }

  /* 预设邮箱展示条 */
  .email-preset-display {
    display: flex;
    justify-content: space-between;
    align-items: center;
    height: 46px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 9999px;
    padding: 0 16px;

    .email-info {
      display: flex;
      align-items: center;
      gap: 10px;

      .email-icon {
        font-size: 16px;
        color: #2563eb;
      }

      .email-text {
        font-size: 14px;
        font-weight: 600;
        color: #0f172a;
        font-family: 'SF Mono', Monaco, monospace;
      }

      .verify-badge {
        font-size: 11px;
        background: #dcfce7;
        color: #15803d;
        padding: 1px 8px;
        border-radius: 9999px;
        font-weight: 600;
      }
    }

    .change-email-btn {
      background: none;
      border: none;
      color: #2563eb;
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;

      &:hover {
        text-decoration: underline;
      }
    }
  }

  /* 密码强度条 */
  .strength-meter-box {
    margin-top: 8px;
    padding: 0 4px;

    .meter-top {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      margin-bottom: 4px;

      .meter-label {
        color: #64748b;
      }

      .meter-text {
        font-weight: 600;

        &.weak { color: #ef4444; }
        &.medium { color: #f59e0b; }
        &.strong { color: #10b981; }
      }
    }

    .meter-bars {
      display: flex;
      gap: 4px;
      height: 4px;

      .bar {
        flex: 1;
        background: #e2e8f0;
        border-radius: 9999px;
        transition: background 0.3s;

        &.active.weak { background: #ef4444; }
        &.active.medium { background: #f59e0b; }
        &.active.strong { background: #10b981; }
      }
    }
  }

  /* 提交按钮行 */
  .submit-action-row {
    margin-top: 28px;

    .capsule-action-btn {
      width: 100%;
      height: 46px;
      border-radius: 9999px;
      border: none;
      font-size: 15px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 8px;

      &.primary {
        background: linear-gradient(135deg, #2563eb, #1d4ed8);
        color: #ffffff;
        box-shadow: 0 4px 16px rgba(37, 99, 235, 0.25);

        &:hover:not(:disabled) {
          transform: translateY(-1px);
          box-shadow: 0 6px 20px rgba(37, 99, 235, 0.35);
        }

        &:disabled {
          background: #94a3b8;
          cursor: not-allowed;
          box-shadow: none;
        }
      }
    }
  }

  /* 底部提示 */
  .forgot-hint-row {
    margin-top: 18px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    font-size: 13px;
    color: #64748b;

    .divider {
      color: #cbd5e1;
    }

    .link-btn {
      background: none;
      border: none;
      color: #2563eb;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      padding: 0;

      &:hover {
        color: #1d4ed8;
        text-decoration: underline;
      }
    }
  }
}

/* 未绑定邮箱空状态 */
.unbound-email-empty {
  text-align: center;
  padding: 36px 20px;

  .empty-icon-box {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    background: #eff6ff;
    color: #2563eb;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 28px;
    margin-bottom: 16px;
  }

  .empty-title {
    font-size: 17px;
    font-weight: 700;
    color: #0f172a;
    margin: 0 0 8px;
  }

  .empty-desc {
    font-size: 13px;
    color: #64748b;
    max-width: 440px;
    margin: 0 auto 24px;
    line-height: 1.6;
  }

  .empty-actions {
    display: flex;
    justify-content: center;
    gap: 12px;

    .capsule-action-btn {
      height: 44px;
      padding: 0 24px;
      border-radius: 9999px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      border: none;
      transition: all 0.2s;

      &.primary {
        background: linear-gradient(135deg, #2563eb, #1d4ed8);
        color: #ffffff;
        box-shadow: 0 4px 14px rgba(37, 99, 235, 0.2);

        &:hover {
          transform: translateY(-1px);
          box-shadow: 0 6px 18px rgba(37, 99, 235, 0.3);
        }
      }

      &.secondary {
        background: #f1f5f9;
        color: #334155;

        &:hover {
          background: #e2e8f0;
        }
      }
    }
  }
}

/* 右侧边栏 */
.security-side-column {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .side-card {
    .side-card-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 16px;

      .side-icon {
        font-size: 18px;
        color: #2563eb;
      }

      .side-title {
        font-size: 15px;
        font-weight: 700;
        color: #0f172a;
        margin: 0;
      }
    }

    .rules-checklist {
      display: flex;
      flex-direction: column;
      gap: 10px;

      .rule-item {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 13px;
        color: #64748b;
        transition: color 0.2s;

        .check-dot {
          font-size: 16px;
          color: #cbd5e1;
          transition: color 0.2s;
        }

        &.passed {
          color: #0f172a;
          font-weight: 500;

          .check-dot {
            color: #10b981;
          }
        }
      }
    }

    .security-tip-quote {
      margin-top: 16px;
      padding: 10px 12px;
      background: #f8fafc;
      border-radius: 10px;
      border-left: 3px solid #3b82f6;

      p {
        margin: 0;
        font-size: 12px;
        color: #64748b;
        line-height: 1.5;
        display: flex;
        align-items: flex-start;
        gap: 6px;

        .tip-quote-icon {
          font-size: 14px;
          color: #3b82f6;
          margin-top: 2px;
          flex-shrink: 0;
        }
      }
    }

    .env-info-list {
      display: flex;
      flex-direction: column;
      gap: 12px;

      .env-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 13px;

        .label {
          color: #64748b;
        }

        .val {
          color: #0f172a;
          font-weight: 600;

          &.role-tag {
            background: #eff6ff;
            color: #2563eb;
            padding: 2px 8px;
            border-radius: 9999px;
            font-size: 11px;
          }

          &.status-green {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            color: #059669;

            .dot {
              width: 6px;
              height: 6px;
              border-radius: 50%;
              background: #10b981;
            }
          }
        }
      }
    }
  }
}

/* 邮箱绑定弹窗 */
:deep(.email-bind-dialog) {
  border-radius: 20px;
  overflow: hidden;

  .el-dialog__header {
    margin: 0;
    padding: 20px 24px;
    border-bottom: 1px solid #f1f5f9;
  }

  .el-dialog__body {
    padding: 24px;
  }

  .el-dialog__footer {
    padding: 16px 24px 24px;
    border-top: 1px solid #f1f5f9;
  }

  .bind-dialog-header {
    display: flex;
    align-items: center;
    gap: 14px;

    .header-icon-badge {
      width: 42px;
      height: 42px;
      border-radius: 12px;
      background: #eff6ff;
      color: #2563eb;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
    }

    .header-titles {
      .title {
        font-size: 17px;
        font-weight: 700;
        color: #0f172a;
        margin: 0 0 2px;
      }
      .subtitle {
        font-size: 12px;
        color: #64748b;
      }
    }
  }

  .dialog-body-box {
    display: flex;
    flex-direction: column;
    gap: 18px;

    .dialog-input-group {
      .group-label {
        display: block;
        font-size: 13px;
        font-weight: 600;
        color: #334155;
        margin-bottom: 6px;
      }

      .capsule-input-box {
        position: relative;
        display: flex;
        align-items: center;
        width: 100%;
        height: 46px;
        background: #f8fafc;
        border: 1px solid #cbd5e1;
        border-radius: 9999px;
        padding: 0 16px;

        &:focus-within {
          border-color: #2563eb;
          background: #ffffff;
          box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
        }

        .input-icon {
          font-size: 16px;
          color: #94a3b8;
          margin-right: 10px;
        }

        .capsule-input {
          flex: 1;
          border: none;
          background: transparent;
          outline: none;
          font-size: 14px;
          color: #0f172a;
          height: 100%;
        }
      }

      .capsule-code-row {
        display: flex;
        gap: 10px;

        .code-input {
          flex: 1;
        }

        .capsule-code-btn {
          height: 46px;
          padding: 0 18px;
          border-radius: 9999px;
          border: none;
          background: linear-gradient(135deg, #2563eb, #1d4ed8);
          color: #ffffff;
          font-size: 13px;
          font-weight: 600;
          cursor: pointer;
          white-space: nowrap;

          &:disabled {
            background: #94a3b8;
            cursor: not-allowed;
          }
        }
      }
    }
  }

  .dialog-footer-row {
    display: flex;
    justify-content: flex-end;
    gap: 12px;

    .dialog-btn {
      height: 42px;
      padding: 0 22px;
      border-radius: 9999px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      border: none;
      transition: all 0.2s;

      &.cancel {
        background: #f1f5f9;
        color: #475569;

        &:hover {
          background: #e2e8f0;
        }
      }

      &.confirm {
        background: linear-gradient(135deg, #2563eb, #1d4ed8);
        color: #ffffff;
        box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);

        &:hover:not(:disabled) {
          box-shadow: 0 6px 16px rgba(37, 99, 235, 0.3);
        }

        &:disabled {
          background: #94a3b8;
          cursor: not-allowed;
        }
      }
    }
  }
}

@keyframes pulse {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(37, 99, 235, 0.7); }
  70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(37, 99, 235, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(37, 99, 235, 0); }
}

/* 移动端/窄屏响应式适配 */
@media (max-width: 992px) {
  .security-metrics-grid {
    grid-template-columns: 1fr;
  }

  .security-main-grid {
    grid-template-columns: 1fr;
  }

  .security-hero-dock {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
}
</style>
