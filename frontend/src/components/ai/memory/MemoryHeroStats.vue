<template>
  <div class="memory-hero-stats">
    <div class="hero-top-badge">
      <div class="status-pulse-dot" :class="{ active: consentGranted }"></div>
      <span class="badge-text">
        {{ consentGranted ? 'Agent 跨会话认知演化已就绪' : '记忆沉淀保护模式 · 已暂停写入' }}
      </span>
      <span class="badge-divider">|</span>
      <span class="badge-sub">零知识端到端加密 · 遵循个人信息保护法 (PIPL) 与可解释 AI 标准</span>
    </div>

    <div class="stats-capsules-grid">
      <div class="stat-capsule-card primary">
        <div class="capsule-icon-box">
          <el-icon><Cpu /></el-icon>
        </div>
        <div class="capsule-info">
          <div class="num-row">
            <span class="stat-num">{{ totalCount }}</span>
            <span class="stat-unit">条</span>
          </div>
          <span class="stat-label">已沉淀长效记忆</span>
        </div>
        <div class="capsule-glow-bg"></div>
      </div>

      <div class="stat-capsule-card success">
        <div class="capsule-icon-box">
          <el-icon><Compass /></el-icon>
        </div>
        <div class="capsule-info">
          <div class="num-row">
            <span class="stat-num">{{ preferenceCount }}</span>
            <span class="stat-unit">项</span>
          </div>
          <span class="stat-label">学习风格与偏好</span>
        </div>
        <div class="capsule-glow-bg"></div>
      </div>

      <div class="stat-capsule-card violet">
        <div class="capsule-icon-box">
          <el-icon><DataAnalysis /></el-icon>
        </div>
        <div class="capsule-info">
          <div class="num-row">
            <span class="stat-num">{{ profileCount }}</span>
            <span class="stat-unit">个</span>
          </div>
          <span class="stat-label">学术能力与盲区</span>
        </div>
        <div class="capsule-glow-bg"></div>
      </div>

      <div class="stat-capsule-card warning">
        <div class="capsule-icon-box">
          <el-icon><Opportunity /></el-icon>
        </div>
        <div class="capsule-info">
          <div class="num-row">
            <span class="stat-num">{{ episodicCount }}</span>
            <span class="stat-unit">段</span>
          </div>
          <span class="stat-label">历史攻坚情境</span>
        </div>
        <div class="capsule-glow-bg"></div>
      </div>

      <div class="stat-capsule-card cyan">
        <div class="capsule-icon-box">
          <el-icon><Lock /></el-icon>
        </div>
        <div class="capsule-info">
          <div class="num-row">
            <span class="stat-num">{{ encryptedCount }}</span>
            <span class="stat-unit">密文</span>
          </div>
          <span class="stat-label">国密 SM4 硬件加密</span>
        </div>
        <div class="capsule-glow-bg"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Cpu, Compass, DataAnalysis, Opportunity, Lock } from '@element-plus/icons-vue';

defineProps<{
  totalCount: number;
  preferenceCount: number;
  profileCount: number;
  episodicCount: number;
  encryptedCount: number;
  consentGranted: boolean;
}>();
</script>

<style scoped lang="scss">
.memory-hero-stats {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;

  .hero-top-badge {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    background: rgba(255, 255, 255, 0.75);
    backdrop-filter: blur(12px);
    border: 1px solid rgba(226, 232, 240, 0.8);
    border-radius: 9999px;
    padding: 6px 16px;
    align-self: flex-start;
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);

    .status-pulse-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #94A3B8;
      transition: all 0.3s ease;

      &.active {
        background: #10B981;
        box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.25);
        animation: pulseDot 2s infinite ease-in-out;
      }
    }

    .badge-text {
      font-size: 13px;
      font-weight: 600;
      color: #1E293B;
    }

    .badge-divider {
      color: #CBD5E1;
      font-size: 12px;
    }

    .badge-sub {
      font-size: 12px;
      color: #64748B;
    }
  }

  .stats-capsules-grid {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: 14px;

    @media (max-width: 1200px) {
      grid-template-columns: repeat(3, 1fr);
    }
    @media (max-width: 768px) {
      grid-template-columns: 1fr;
    }

    .stat-capsule-card {
      position: relative;
      background: #FFFFFF;
      border-radius: 20px;
      border: 1px solid #E2E8F0;
      padding: 14px 18px;
      display: flex;
      align-items: center;
      gap: 14px;
      overflow: hidden;
      box-shadow: 0 4px 14px rgba(15, 23, 42, 0.04);
      transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
      }

      .capsule-icon-box {
        width: 44px;
        height: 44px;
        border-radius: 14px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
        flex-shrink: 0;
        transition: transform 0.25s ease;
      }

      &:hover .capsule-icon-box {
        transform: scale(1.08);
      }

      .capsule-info {
        display: flex;
        flex-direction: column;
        z-index: 1;

        .num-row {
          display: flex;
          align-items: baseline;
          gap: 3px;

          .stat-num {
            font-size: 22px;
            font-weight: 700;
            line-height: 1.1;
            font-family: var(--font-mono, ui-monospace, monospace);
          }

          .stat-unit {
            font-size: 11px;
            color: #94A3B8;
            font-weight: 500;
          }
        }

        .stat-label {
          font-size: 12px;
          color: #64748B;
          margin-top: 3px;
          white-space: nowrap;
        }
      }

      &.primary {
        border-color: rgba(37, 99, 235, 0.2);
        .capsule-icon-box {
          background: rgba(37, 99, 235, 0.1);
          color: #2563EB;
        }
        .stat-num { color: #2563EB; }
      }

      &.success {
        border-color: rgba(16, 185, 129, 0.2);
        .capsule-icon-box {
          background: rgba(16, 185, 129, 0.1);
          color: #10B981;
        }
        .stat-num { color: #10B981; }
      }

      &.violet {
        border-color: rgba(124, 58, 237, 0.2);
        .capsule-icon-box {
          background: rgba(124, 58, 237, 0.1);
          color: #7C3AED;
        }
        .stat-num { color: #7C3AED; }
      }

      &.warning {
        border-color: rgba(245, 158, 11, 0.2);
        .capsule-icon-box {
          background: rgba(245, 158, 11, 0.1);
          color: #F59E0B;
        }
        .stat-num { color: #F59E0B; }
      }

      &.cyan {
        border-color: rgba(6, 182, 212, 0.2);
        .capsule-icon-box {
          background: rgba(6, 182, 212, 0.1);
          color: #06B6D4;
        }
        .stat-num { color: #06B6D4; }
      }
    }
  }
}

@keyframes pulseDot {
  0% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.3); opacity: 0.6; }
  100% { transform: scale(1); opacity: 1; }
}
</style>
