<template>
  <div class="security-side-column">
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

    <div class="security-panel-card side-card">
      <div class="side-card-header">
        <el-icon class="side-icon"><Monitor /></el-icon>
        <h4 class="side-title">当前会话与身份状态</h4>
      </div>
      <div class="env-info-list">
        <div class="env-row">
          <span class="label">登录用户名</span>
          <span class="val">@{{ currentUser?.username || '—' }}</span>
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
</template>

<script setup lang="ts">
import {
  Check,
  CircleCheckFilled,
  CircleCloseFilled,
  Monitor,
  InfoFilled
} from '@element-plus/icons-vue';
import type { UserInfo } from '@/types/auth/auth';

defineProps<{
  currentCheckingPwd: string;
  currentUser: UserInfo | null;
  roleLabel: string;
}>();
</script>

<style scoped lang="scss">
.security-side-column {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .security-panel-card {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 16px;
    padding: 26px;
    box-shadow: 0 4px 20px rgba(15, 23, 42, 0.03);
  }

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
</style>
