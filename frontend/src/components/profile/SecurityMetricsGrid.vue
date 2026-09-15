<template>
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
        <button type="button" class="hero-aux-btn" @click="emit('forgot-password')">
          <el-icon><Promotion /></el-icon>
          <span>外部找回密码通道</span>
        </button>
      </div>
    </div>

    <div class="security-metrics-grid">
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
          <button type="button" class="card-text-btn" @click="emit('open-bind')">
            {{ userEmail ? '更换安全邮箱' : '立即绑定密保邮箱' }} →
          </button>
        </div>
      </div>
    </div>

    <div class="metric-card session-card">
      <div class="metric-header">
        <span class="metric-title">当前会话与身份状态</span>
        <span class="status-pill" :class="sessionActive ? 'active' : 'unbound'">
          <span v-if="sessionActive" class="pulse-dot"></span>
          <span>{{ sessionActive ? '会话活跃' : '未登录' }}</span>
        </span>
      </div>
      <div class="metric-body">
        <div class="session-info-row">
          <span class="session-label">登录账号：</span>
          <span class="session-val">@{{ currentUser?.username || '—' }}</span>
        </div>
        <div class="session-info-row">
          <span class="session-label">显示名称：</span>
          <span class="session-val">{{ displayName }}</span>
        </div>
        <div class="session-info-row">
          <span class="session-label">绑定手机：</span>
          <span class="session-val">{{ userPhone ? maskPhone(userPhone) : '未绑定' }}</span>
        </div>
        <div class="card-action-row">
          <button type="button" class="card-text-btn danger" @click="emit('logout')">
            安全退出登录
          </button>
        </div>
      </div>
    </div>
    </div>
</template>

<script setup lang="ts">
import { Lock, Promotion, CircleCheckFilled, WarningFilled } from '@element-plus/icons-vue';
import type { SecurityLevel } from '@/composables/profile/useSecurity';
import type { UserInfo } from '@/types/auth/auth';

defineProps<{
  securityScore: number;
  securityLevel: SecurityLevel;
  userEmail: string;
  userPhone: string;
  displayName: string;
  sessionActive: boolean;
  currentUser: UserInfo | null;
  maskEmail: (email: string) => string;
  maskPhone: (phone: string) => string;
}>();

const emit = defineEmits<{
  'open-bind': [];
  logout: [];
  'forgot-password': [];
}>();
</script>

<style scoped lang="scss">
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

@keyframes pulse {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(37, 99, 235, 0.7); }
  70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(37, 99, 235, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(37, 99, 235, 0); }
}

@media (max-width: 992px) {
  .security-metrics-grid {
    grid-template-columns: 1fr;
  }

  .security-hero-dock {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
}
</style>
