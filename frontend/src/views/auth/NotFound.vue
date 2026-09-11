<template>
  <div class="not-found-page">
    <div class="glow-orb glow-orb--left"></div>
    <div class="glow-orb glow-orb--right"></div>

    <div class="not-found-card">
      <!-- 科技感 404 浮空插画 -->
      <div class="illustration-box">
        <svg class="space-svg" viewBox="0 0 320 220" fill="none" xmlns="http://www.w3.org/2000/svg">
          <ellipse cx="160" cy="195" rx="110" ry="18" fill="#1677FF" fill-opacity="0.08" />
          <ellipse cx="160" cy="195" rx="70" ry="10" fill="#722ED1" fill-opacity="0.12" />

          <!-- 悬浮小行星 -->
          <circle cx="60" cy="65" r="16" fill="url(#planet-grad-1)" filter="drop-shadow(0 4px 12px rgba(22, 119, 255, 0.2))" />
          <ellipse cx="60" cy="65" rx="24" ry="6" stroke="#93C5FD" stroke-width="1.5" stroke-dasharray="3 3" />

          <circle cx="265" cy="110" r="12" fill="url(#planet-grad-2)" filter="drop-shadow(0 4px 10px rgba(114, 46, 209, 0.25))" />

          <!-- 中心大 404 文字与智能体头盔 -->
          <g class="float-agent">
            <text x="50" y="150" font-size="82" font-weight="900" fill="url(#text-grad)" font-family="system-ui, sans-serif" letter-spacing="4">4</text>
            <text x="210" y="150" font-size="82" font-weight="900" fill="url(#text-grad)" font-family="system-ui, sans-serif" letter-spacing="4">4</text>

            <!-- 中心 0 替换为科技头盔机器人 -->
            <g transform="translate(125, 68)">
              <!-- 身体 -->
              <rect x="12" y="52" width="46" height="30" rx="14" fill="#1677FF" />
              <!-- 头部外壳 -->
              <rect x="0" y="4" width="70" height="50" rx="20" fill="#FFFFFF" filter="drop-shadow(0 8px 20px rgba(22, 119, 255, 0.25))" />
              <!-- 面罩 -->
              <rect x="6" y="10" width="58" height="38" rx="15" fill="#0F172A" />
              <!-- 眨眼光点 (迷茫表情) -->
              <ellipse cx="26" cy="29" rx="5" ry="3" fill="#38BDF8" />
              <ellipse cx="44" cy="29" rx="5" ry="3" fill="#38BDF8" />
              <!-- 天线 -->
              <circle cx="35" cy="0" r="3.5" fill="#1677FF" />
              <line x1="35" y1="0" x2="35" y2="4" stroke="#1677FF" stroke-width="2" />
            </g>
          </g>

          <defs>
            <linearGradient id="text-grad" x1="50" y1="70" x2="270" y2="160" gradientUnits="userSpaceOnUse">
              <stop stop-color="#1677FF" />
              <stop offset="1" stop-color="#722ED1" />
            </linearGradient>
            <linearGradient id="planet-grad-1" x1="44" y1="49" x2="76" y2="81" gradientUnits="userSpaceOnUse">
              <stop stop-color="#38BDF8" />
              <stop offset="1" stop-color="#1677FF" />
            </linearGradient>
            <linearGradient id="planet-grad-2" x1="253" y1="98" x2="277" y2="122" gradientUnits="userSpaceOnUse">
              <stop stop-color="#C084FC" />
              <stop offset="1" stop-color="#722ED1" />
            </linearGradient>
          </defs>
        </svg>
      </div>

      <!-- 文字信息区 -->
      <h1 class="error-title">抱歉，您访问的页面已漫游至未知星域</h1>
      <p class="error-desc">
        目标页面可能已被重新调整教学大纲归属，或当前 URL 地址有误。别担心，平台 AI 智能体可以随时引导你重返核心航道。
      </p>

      <!-- 核心胶囊操作按钮 (1:1 胶囊标准) -->
      <div class="actions-row">
        <button
          type="button"
          class="capsule-btn capsule-btn--primary"
          @click="router.push('/dashboard')"
        >
          <span>🏠 返回工作台首页</span>
        </button>

        <button
          type="button"
          class="capsule-btn capsule-btn--secondary"
          @click="router.back()"
        >
          <span>⬅️ 返回上一页</span>
        </button>

        <button
          type="button"
          class="capsule-btn capsule-btn--ai"
          @click="router.push('/ai/marketplace/all')"
        >
          <span>🤖 探索 AI 工具广场</span>
        </button>
      </div>

      <!-- 快捷导航药丸组合 (Quick Navigation Pills) -->
      <div class="quick-nav-section">
        <span class="quick-nav-label">快捷直达推荐：</span>
        <div class="pills-list">
          <span class="nav-pill" @click="router.push('/course')">📚 课程中心</span>
          <span class="nav-pill" @click="router.push('/ai/question/generate')">✍️ AI 智能出题</span>
          <span class="nav-pill" @click="router.push('/ai/exam/generate')">📑 AI 智能组卷</span>
          <span class="nav-pill" @click="router.push('/learning/home')">🎯 自适应学习</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();
</script>

<style scoped lang="scss">
.not-found-page {
  position: relative;
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #F0F7FF 0%, #FAF5FF 100%);
  overflow: hidden;
  padding: 24px;
  box-sizing: border-box;

  .glow-orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(70px);
    pointer-events: none;

    &--left {
      top: -60px;
      left: 10%;
      width: 320px;
      height: 320px;
      background: rgba(22, 119, 255, 0.15);
    }

    &--right {
      bottom: -60px;
      right: 12%;
      width: 360px;
      height: 360px;
      background: rgba(114, 46, 209, 0.12);
    }
  }

  .not-found-card {
    position: relative;
    z-index: 1;
    width: 100%;
    max-width: 660px;
    background: rgba(255, 255, 255, 0.92);
    backdrop-filter: blur(16px);
    border: 1px solid rgba(255, 255, 255, 0.8);
    border-radius: 24px;
    padding: 44px 48px;
    box-shadow: 0 16px 40px rgba(22, 119, 255, 0.08), 0 2px 6px rgba(0, 0, 0, 0.03);
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;

    .illustration-box {
      width: 300px;
      height: 190px;
      margin-bottom: 8px;

      .space-svg {
        width: 100%;
        height: 100%;
        overflow: visible;

        .float-agent {
          animation: floatY 3.6s ease-in-out infinite alternate;
        }
      }
    }

    .error-title {
      font-size: 22px;
      font-weight: 700;
      color: #0F172A;
      margin: 0 0 10px 0;
      letter-spacing: -0.3px;
    }

    .error-desc {
      font-size: 13.5px;
      color: #64748B;
      line-height: 1.6;
      margin: 0 0 26px 0;
      max-width: 500px;
    }

    // 胶囊主按钮组
    .actions-row {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
      justify-content: center;
      margin-bottom: 28px;

      .capsule-btn {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        height: 42px;
        padding: 0 22px;
        border-radius: 9999px; // 纯正长圆跑道
        font-size: 13.5px;
        font-weight: 600;
        cursor: pointer;
        border: none;
        transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);

        &--primary {
          background: #1677FF;
          color: #FFFFFF;
          box-shadow: 0 3px 12px rgba(22, 119, 255, 0.28);

          &:hover {
            background: #4096FF;
            transform: translateY(-2px);
            box-shadow: 0 6px 18px rgba(22, 119, 255, 0.38);
          }
        }

        &--secondary {
          background: #FFFFFF;
          color: #334155;
          border: 1.5px solid #CBD5E1;

          &:hover {
            background: #F8FAFC;
            color: #1677FF;
            border-color: #93C5FD;
            transform: translateY(-2px);
          }
        }

        &--ai {
          background: linear-gradient(135deg, #1677FF 0%, #722ED1 100%);
          color: #FFFFFF;
          box-shadow: 0 3px 12px rgba(114, 46, 209, 0.28);

          &:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 18px rgba(114, 46, 209, 0.38);
          }
        }
      }
    }

    // 快捷导航药丸
    .quick-nav-section {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 12px;
      flex-wrap: wrap;
      justify-content: center;
      padding-top: 20px;
      border-top: 1px solid #F1F5F9;
      width: 100%;

      .quick-nav-label {
        color: #94A3B8;
      }

      .pills-list {
        display: flex;
        align-items: center;
        gap: 8px;
        flex-wrap: wrap;

        .nav-pill {
          padding: 3px 12px;
          border-radius: 9999px; // 长圆药丸
          background: #F1F5F9;
          color: #475569;
          font-weight: 500;
          cursor: pointer;
          transition: all 0.2s;

          &:hover {
            background: #EFF6FF;
            color: #1677FF;
            transform: translateY(-1px);
          }
        }
      }
    }
  }
}

@keyframes floatY {
  0% { transform: translateY(0); }
  100% { transform: translateY(-8px); }
}

@media (max-width: 640px) {
  .not-found-card {
    padding: 32px 20px !important;

    .illustration-box {
      width: 240px !important;
      height: 150px !important;
    }

    .actions-row {
      flex-direction: column;
      width: 100%;

      .capsule-btn {
        width: 100%;
        justify-content: center;
      }
    }
  }
}
</style>
