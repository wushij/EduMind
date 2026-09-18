<template>
  <div class="not-found-page">
    <div class="page-glow page-glow--blue" aria-hidden="true" />
    <div class="page-glow page-glow--purple" aria-hidden="true" />

    <div class="not-found-shell">
      <section class="hero-panel">
        <div class="hero-panel__glow hero-panel__glow--blue" aria-hidden="true" />
        <div class="hero-panel__glow hero-panel__glow--purple" aria-hidden="true" />

        <div class="hero-grid">
          <div class="hero-copy">
            <span class="status-chip">HTTP 404</span>
            <h1 class="hero-title">页面不存在或已迁移</h1>
            <p class="hero-desc">
              链接可能已失效、权限不足，或菜单结构调整后路径变更。你可以返回工作台，或使用下方快捷入口继续教学与学习任务。
            </p>

            <div class="hero-actions">
              <button type="button" class="hero-pill-btn is-primary" @click="router.push('/dashboard')">
                <el-icon><HomeFilled /></el-icon>
                返回工作台首页
              </button>
              <button type="button" class="hero-pill-btn is-outline" @click="router.back()">
                <el-icon><Back /></el-icon>
                返回上一页
              </button>
              <button type="button" class="hero-pill-btn is-outline" @click="router.push('/ai/marketplace/all')">
                <el-icon><Shop /></el-icon>
                AI 工具广场
              </button>
            </div>
          </div>

          <div class="hero-visual" aria-hidden="true">
            <div class="code-ring">
              <span class="code-digit">4</span>
              <div class="code-center">
                <el-icon class="code-icon"><Compass /></el-icon>
                <span class="code-label">NOT FOUND</span>
              </div>
              <span class="code-digit">4</span>
            </div>
          </div>
        </div>
      </section>

      <section class="quick-section">
        <header class="quick-header">
          <span class="quick-title">常用入口</span>
          <span class="quick-sub">一键回到核心业务模块</span>
        </header>
        <div class="quick-grid">
          <button type="button" class="quick-card" @click="router.push('/course')">
            <span class="quick-card__icon quick-card__icon--blue">
              <el-icon><FolderOpened /></el-icon>
            </span>
            <span class="quick-card__text">
              <strong>课程中心</strong>
              <small>浏览与管理课程</small>
            </span>
            <el-icon class="quick-card__arrow"><ArrowRight /></el-icon>
          </button>
          <button type="button" class="quick-card" @click="router.push('/ai/question/generate')">
            <span class="quick-card__icon quick-card__icon--violet">
              <el-icon><EditPen /></el-icon>
            </span>
            <span class="quick-card__text">
              <strong>AI 智能出题</strong>
              <small>快速生成试题</small>
            </span>
            <el-icon class="quick-card__arrow"><ArrowRight /></el-icon>
          </button>
          <button type="button" class="quick-card" @click="router.push('/ai/exam/generate')">
            <span class="quick-card__icon quick-card__icon--teal">
              <el-icon><Document /></el-icon>
            </span>
            <span class="quick-card__text">
              <strong>AI 智能组卷</strong>
              <small>组卷与导出</small>
            </span>
            <el-icon class="quick-card__arrow"><ArrowRight /></el-icon>
          </button>
          <button type="button" class="quick-card" @click="router.push('/learning/home')">
            <span class="quick-card__icon quick-card__icon--green">
              <el-icon><Aim /></el-icon>
            </span>
            <span class="quick-card__text">
              <strong>自适应学习</strong>
              <small>学习中心首页</small>
            </span>
            <el-icon class="quick-card__arrow"><ArrowRight /></el-icon>
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import {
  HomeFilled,
  Back,
  Shop,
  FolderOpened,
  EditPen,
  Document,
  Aim,
  Compass,
  ArrowRight
} from '@element-plus/icons-vue';

const router = useRouter();
</script>

<style scoped lang="scss">
.not-found-page {
  position: relative;
  width: 100%;
  min-height: calc(100vh - 64px - 48px - 80px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 0 32px;
  box-sizing: border-box;
  background: transparent;
  overflow: hidden;
}

.page-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  pointer-events: none;
  opacity: 0.45;

  &--blue {
    width: 420px;
    height: 420px;
    top: -120px;
    left: 8%;
    background: #bfdbfe;
  }

  &--purple {
    width: 360px;
    height: 360px;
    bottom: -100px;
    right: 10%;
    background: #ddd6fe;
  }
}

.not-found-shell {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 920px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-panel {
  position: relative;
  overflow: hidden;
  border-radius: 24px;
  border: 1px solid #e2e8f0;
  background: linear-gradient(135deg, #ffffff 0%, #f8faff 55%, #f5f3ff 100%);
  box-shadow: 0 8px 32px rgba(22, 119, 255, 0.06);
  padding: 32px 36px;

  &__glow {
    position: absolute;
    border-radius: 50%;
    pointer-events: none;
    filter: blur(60px);
    opacity: 0.35;

    &--blue {
      width: 220px;
      height: 220px;
      background: #bfdbfe;
      top: -80px;
      right: 80px;
    }

    &--purple {
      width: 160px;
      height: 160px;
      background: #ddd6fe;
      bottom: -60px;
      left: 24px;
    }
  }
}

.hero-grid {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 0.85fr);
  gap: 28px;
  align-items: center;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.hero-copy {
  min-width: 0;
  text-align: left;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: #2563eb;
  background: rgba(37, 99, 235, 0.08);
  border: 1px solid rgba(37, 99, 235, 0.14);
}

.hero-title {
  margin: 14px 0 10px;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.03em;
  color: #0f172a;
  line-height: 1.25;
}

.hero-desc {
  margin: 0;
  max-width: 520px;
  font-size: 14px;
  line-height: 1.65;
  color: #64748b;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
}

.hero-pill-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  border-radius: 9999px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;

  &.is-primary {
    border: none;
    color: #ffffff;
    background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
    box-shadow: 0 4px 14px rgba(37, 99, 235, 0.28);

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 18px rgba(37, 99, 235, 0.36);
    }
  }

  &.is-outline {
    color: #334155;
    background: #ffffff;
    border-color: #cbd5e1;
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);

    &:hover {
      color: #2563eb;
      border-color: #93c5fd;
      background: #f8fafc;
    }
  }
}

.hero-visual {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 220px;
}

.code-ring {
  display: flex;
  align-items: center;
  gap: 10px;
}

.code-digit {
  font-size: clamp(72px, 12vw, 96px);
  font-weight: 900;
  line-height: 1;
  letter-spacing: -0.04em;
  background: linear-gradient(160deg, #2563eb 0%, #6366f1 55%, #9333ea 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  user-select: none;
}

.code-center {
  width: 88px;
  height: 88px;
  border-radius: 24px;
  border: 1.5px solid #e2e8f0;
  background: #ffffff;
  box-shadow: 0 8px 28px rgba(37, 99, 235, 0.12);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.code-icon {
  font-size: 28px;
  color: #2563eb;
}

.code-label {
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.12em;
  color: #94a3b8;
}

.quick-section {
  border-radius: 24px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  box-shadow: 0 4px 20px rgba(30, 80, 150, 0.04);
  padding: 22px 24px 24px;
}

.quick-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.quick-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.quick-sub {
  font-size: 12px;
  color: #94a3b8;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 14px 16px;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
  background: linear-gradient(160deg, #ffffff 0%, #f8fafc 100%);
  cursor: pointer;
  text-align: left;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s;

  &:hover {
    border-color: #93c5fd;
    box-shadow: 0 6px 20px rgba(37, 99, 235, 0.08);
    transform: translateY(-1px);

    .quick-card__arrow {
      color: #2563eb;
      transform: translateX(2px);
    }
  }

  &__icon {
    width: 44px;
    height: 44px;
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    font-size: 20px;

    &--blue {
      color: #2563eb;
      background: #eff6ff;
      border: 1px solid #bfdbfe;
    }

    &--violet {
      color: #7c3aed;
      background: #f5f3ff;
      border: 1px solid #ddd6fe;
    }

    &--teal {
      color: #0d9488;
      background: #ecfdf5;
      border: 1px solid #99f6e4;
    }

    &--green {
      color: #059669;
      background: #ecfdf5;
      border: 1px solid #a7f3d0;
    }
  }

  &__text {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;

    strong {
      font-size: 14px;
      font-weight: 700;
      color: #0f172a;
    }

    small {
      font-size: 12px;
      color: #64748b;
    }
  }

  &__arrow {
    flex-shrink: 0;
    color: #cbd5e1;
    transition: color 0.2s, transform 0.2s;
  }
}

@media (max-width: 640px) {
  .hero-panel {
    padding: 24px 20px;
  }

  .hero-title {
    font-size: 24px;
  }

  .hero-actions {
    flex-direction: column;
    align-items: stretch;

    .hero-pill-btn {
      justify-content: center;
    }
  }

  .hero-visual {
    min-height: 160px;
  }
}
</style>
