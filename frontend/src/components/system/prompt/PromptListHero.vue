<template>
  <div class="prompt-hero-card">
    <div class="hero-bg-glow hero-bg-glow--blue" />
    <div class="hero-bg-glow hero-bg-glow--purple" />

    <div class="hero-header-row">
      <div class="hero-title-area">
        <div class="hero-eyebrow">
          <span class="eyebrow-chip">EduMind AI Engineering</span>
          <span class="eyebrow-divider">/</span>
          <span class="eyebrow-sub">提示词工程资产中枢</span>
        </div>
        <h1 class="hero-title">Prompt 提示词模板中心</h1>
        <p class="hero-desc">
          统一纳管课程 RAG 问答、试题生成向导、主客观智能批阅及教学备课的高质量提示词资产，构建具备上下文注入、多轮会话、引用溯源与防注入屏障的生产级教学中枢。
        </p>
      </div>
      <div class="hero-action-area">
        <el-button
          type="primary"
          class="create-btn"
          :icon="Plus"
          @click="$emit('create')"
        >
          新建 Prompt 模板
        </el-button>
        <el-button
          round
          class="btn-refresh"
          :icon="Refresh"
          :loading="loading"
          @click="$emit('reload')"
        >
          刷新资产
        </el-button>
      </div>
    </div>

    <!-- 4 大核心指标数据面板 -->
    <div class="metrics-grid">
      <div class="metric-card">
        <div class="metric-icon-box blue">
          <el-icon><CollectionTag /></el-icon>
        </div>
        <div class="metric-content">
          <div class="metric-label">纳管工程模板</div>
          <div class="metric-value">{{ promptListLength }} <span class="unit">个</span></div>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon-box green">
          <span class="online-pulse-dot"></span>
          <el-icon><Cpu /></el-icon>
        </div>
        <div class="metric-content">
          <div class="metric-label">生产环境运行中</div>
          <div class="metric-value text-green">{{ publishedCount }} <span class="unit">项</span></div>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon-box cyan">
          <el-icon><ChatDotRound /></el-icon>
        </div>
        <div class="metric-content">
          <div class="metric-label">课程 RAG 赋能引擎</div>
          <div class="metric-value text-cyan">{{ ragCount }} <span class="unit">个</span></div>
        </div>
      </div>

      <div class="metric-card">
        <div class="metric-icon-box purple">
          <el-icon><DataAnalysis /></el-icon>
        </div>
        <div class="metric-content">
          <div class="metric-label">动态参数插槽覆盖</div>
          <div class="metric-value text-purple">{{ totalVariablesCount }} <span class="unit">槽位</span></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Plus, Refresh, CollectionTag, Cpu, ChatDotRound, DataAnalysis } from '@element-plus/icons-vue';

defineProps<{
  loading: boolean;
  promptListLength: number;
  publishedCount: number;
  ragCount: number;
  totalVariablesCount: number;
}>();

defineEmits<{
  create: [];
  reload: [];
}>();
</script>

<style scoped lang="scss">
.prompt-hero-card {
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #FFFFFF 0%, #F8FAFF 55%, #F5F3FF 100%);
  border: 1px solid #E2E8F0;
  border-radius: 20px;
  padding: 24px 28px 20px;
  box-shadow: 0 8px 32px rgba(22, 119, 255, 0.05);

  .hero-bg-glow {
    position: absolute;
    border-radius: 50%;
    pointer-events: none;
    filter: blur(60px);
    opacity: 0.35;

    &--blue {
      width: 240px;
      height: 240px;
      background: #BFDBFE;
      top: -90px;
      right: 140px;
    }

    &--purple {
      width: 180px;
      height: 180px;
      background: #DDD6FE;
      bottom: -70px;
      left: 50px;
    }
  }

  .hero-header-row {
    position: relative;
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 20px;
    z-index: 1;

    .hero-title-area {
      max-width: 820px;

      .hero-eyebrow {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        font-size: 11px;
        font-weight: 700;
        letter-spacing: 0.06em;
        text-transform: uppercase;
        color: #1677FF;
        background: rgba(22, 119, 255, 0.08);
        border: 1px solid rgba(22, 119, 255, 0.15);
        padding: 3px 12px;
        border-radius: 9999px;
        margin-bottom: 8px;

        .eyebrow-chip {
          color: #1677FF;
          font-weight: 700;
        }
        .eyebrow-divider {
          color: #93C5FD;
        }
        .eyebrow-sub {
          color: #2563EB;
        }
      }

      .hero-title {
        margin: 0 0 6px 0;
        font-size: 23px;
        font-weight: 800;
        letter-spacing: -0.02em;
        color: #0F172A;
      }

      .hero-desc {
        margin: 0;
        font-size: 13px;
        line-height: 1.55;
        color: #64748B;
      }
    }

    .hero-action-area {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-shrink: 0;

      .create-btn {
        background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
        border: none;
        box-shadow: 0 4px 12px rgba(37, 99, 235, 0.28);
        font-weight: 600;
        padding: 8px 18px;
        border-radius: 999px !important;
        color: #FFFFFF;
        transition: background-color 0.15s ease, box-shadow 0.15s ease;

        &:hover {
          box-shadow: 0 6px 16px rgba(37, 99, 235, 0.38);
        }

        &:active {
          box-shadow: 0 2px 6px rgba(37, 99, 235, 0.25);
        }
      }

      .refresh-btn {
        background: #FFFFFF;
        border: 1px solid #BFDBFE;
        color: #2563EB;
        border-radius: 999px !important;
        padding: 8px 16px;
        font-weight: 600;
        box-shadow: 0 2px 8px rgba(37, 99, 235, 0.06);

        &:hover {
          background: #EFF6FF;
          border-color: #93C5FD;
          color: #1D4ED8;
        }

        &:active {
          background: #DBEAFE;
        }
      }
    }
  }

  .metrics-grid {
    position: relative;
    z-index: 1;
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 14px;
    margin-top: 18px;
    padding-top: 16px;
    border-top: 1px solid rgba(226, 232, 240, 0.8);

    @media (max-width: 900px) {
      grid-template-columns: repeat(2, 1fr);
    }

    .metric-card {
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      border-radius: 14px;
      padding: 12px 16px;
      display: flex;
      align-items: center;
      gap: 12px;
      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.03);
      transition: border-color 0.2s ease, box-shadow 0.2s ease;

      &:hover {
        border-color: #93C5FD;
        box-shadow: 0 4px 14px rgba(37, 99, 235, 0.08);
      }

      .metric-icon-box {
        width: 38px;
        height: 38px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 18px;
        position: relative;
        flex-shrink: 0;

        &.blue {
          background: #EFF6FF;
          color: #2563EB;
          border: 1px solid #DBEAFE;
        }
        &.green {
          background: #ECFDF5;
          color: #059669;
          border: 1px solid #A7F3D0;
        }
        &.cyan {
          background: #F0F9FF;
          color: #0284C7;
          border: 1px solid #BAE6FD;
        }
        &.purple {
          background: #FAF5FF;
          color: #9333EA;
          border: 1px solid #E9D5FF;
        }

        .online-pulse-dot {
          position: absolute;
          top: 2px;
          right: 2px;
          width: 8px;
          height: 8px;
          border-radius: 50%;
          background: #10B981;
          box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.35);
          animation: pulseGlow 2s infinite;
        }
      }

      .metric-content {
        display: flex;
        flex-direction: column;

        .metric-label {
          font-size: 12px;
          color: #64748B;
          font-weight: 500;
        }

        .metric-value {
          font-size: 19px;
          font-weight: 800;
          color: #0F172A;
          margin-top: 1px;
          letter-spacing: -0.01em;

          .unit {
            font-size: 11px;
            color: #94A3B8;
            font-weight: normal;
            margin-left: 2px;
          }

          &.text-green {
            color: #059669;
          }
          &.text-cyan {
            color: #0284C7;
          }
          &.text-purple {
            color: #7C3AED;
          }
        }
      }
    }
  }
}

@keyframes pulseGlow {
  0% {
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(16, 185, 129, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0);
  }
}
</style>
