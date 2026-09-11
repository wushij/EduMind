<template>
  <div
    class="page-hero-banner"
    :class="[
      `page-hero-banner--${size}`,
      `page-hero-banner--${backgroundVariant}`
    ]"
    :style="bannerStyle"
  >
    <!-- 装饰性光斑背景微动效 -->
    <div class="glow-orb glow-orb--left"></div>
    <div class="glow-orb glow-orb--right"></div>

    <div class="hero-inner-container">
      <!-- 左侧文案与主操作区 -->
      <div class="hero-content">
        <div class="hero-title-group">
          <h1 class="hero-title">{{ title }}</h1>
          <p v-if="subtitle" class="hero-subtitle">{{ subtitle }}</p>
        </div>

        <!-- 长圆胶囊搜索框 (若启用) -->
        <div v-if="showSearch" class="hero-search-wrapper">
          <div class="capsule-search-box">
            <span class="search-prefix-icon">
              <svg viewBox="0 0 24 24" class="search-svg" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
            </span>
            <input
              v-model="searchValue"
              type="text"
              class="capsule-search-input"
              :placeholder="searchPlaceholder"
              @keyup.enter="handleSearch"
            />
            <button
              v-if="searchValue"
              type="button"
              class="search-clear-btn"
              @click="clearSearch"
            >
              <el-icon><Close /></el-icon>
            </button>
            <button
              type="button"
              class="capsule-search-submit"
              @click="handleSearch"
            >
              搜索
            </button>
          </div>
        </div>

        <!-- 按钮或外部操作区插槽 -->
        <div v-if="$slots.actions" class="hero-actions">
          <slot name="actions" />
        </div>
      </div>

      <!-- 右侧插画区域 -->
      <div v-if="showIllustrationEffective" class="hero-illustration">
        <slot name="illustration">
          <!-- 内置默认高质感 AI 科技插画 (SVG) -->
          <div class="default-illustration">
            <svg class="robot-tech-svg" viewBox="0 0 260 180" fill="none" xmlns="http://www.w3.org/2000/svg">
              <!-- 浮空圆环基底 -->
              <ellipse cx="130" cy="155" rx="80" ry="14" fill="#1677FF" fill-opacity="0.12" />
              <ellipse cx="130" cy="155" rx="55" ry="9" fill="#1677FF" fill-opacity="0.2" />

              <!-- 悬浮微芯片卡片 -->
              <g class="float-chip" transform="translate(18, 25)">
                <rect width="64" height="42" rx="12" fill="#FFFFFF" fill-opacity="0.9" filter="drop-shadow(0 6px 16px rgba(22, 119, 255, 0.15))" />
                <rect x="8" y="10" width="22" height="6" rx="3" fill="#1677FF" />
                <rect x="8" y="20" width="38" height="4" rx="2" fill="#93C5FD" />
                <rect x="8" y="28" width="28" height="4" rx="2" fill="#CBD5E1" />
                <circle cx="50" cy="13" r="5" fill="#52C41A" fill-opacity="0.8" />
              </g>

              <!-- 悬浮智慧星芒卡片 -->
              <g class="float-ai-badge" transform="translate(180, 40)">
                <rect width="58" height="38" rx="10" fill="#FFFFFF" fill-opacity="0.92" filter="drop-shadow(0 6px 14px rgba(114, 46, 209, 0.15))" />
                <circle cx="20" cy="19" r="9" fill="#722ED1" fill-opacity="0.15" />
                <path d="M20 14L21.5 17.5L25 19L21.5 20.5L20 24L18.5 20.5L15 19L18.5 17.5L20 14Z" fill="#722ED1" />
                <rect x="34" y="14" width="16" height="4" rx="2" fill="#C4B5FD" />
                <rect x="34" y="22" width="12" height="3" rx="1.5" fill="#E2E8F0" />
              </g>

              <!-- 中心 AI 智能体头盔/机体 -->
              <g class="robot-main" transform="translate(85, 45)">
                <!-- 身体底盘 -->
                <rect x="15" y="65" width="60" height="40" rx="18" fill="url(#robot-body-grad)" />
                <rect x="25" y="75" width="40" height="8" rx="4" fill="#FFFFFF" fill-opacity="0.4" />

                <!-- 头部 -->
                <rect x="5" y="10" width="80" height="56" rx="24" fill="#FFFFFF" filter="drop-shadow(0 8px 24px rgba(22, 119, 255, 0.2))" />
                <!-- 头盔面罩 (深蓝渐变) -->
                <rect x="12" y="16" width="66" height="42" rx="18" fill="url(#visor-grad)" />
                <!-- 灵动双目 (青蓝光斑) -->
                <ellipse cx="32" cy="36" rx="8" ry="6" fill="#38BDF8" />
                <circle cx="34" cy="34" r="2.5" fill="#FFFFFF" />
                <ellipse cx="58" cy="36" rx="8" ry="6" fill="#38BDF8" />
                <circle cx="60" cy="34" r="2.5" fill="#FFFFFF" />
                <!-- 头部天线 -->
                <circle cx="45" cy="4" r="4" fill="#1677FF" />
                <line x1="45" y1="4" x2="45" y2="10" stroke="#1677FF" stroke-width="2.5" stroke-linecap="round" />
                <!-- 耳部光环 -->
                <rect x="0" y="28" width="6" height="18" rx="3" fill="#1677FF" />
                <rect x="84" y="28" width="6" height="18" rx="3" fill="#1677FF" />
              </g>

              <!-- 渐变定义 -->
              <defs>
                <linearGradient id="robot-body-grad" x1="15" y1="65" x2="75" y2="105" gradientUnits="userSpaceOnUse">
                  <stop stop-color="#1677FF" />
                  <stop offset="1" stop-color="#0958D9" />
                </linearGradient>
                <linearGradient id="visor-grad" x1="12" y1="16" x2="78" y2="58" gradientUnits="userSpaceOnUse">
                  <stop stop-color="#0A1B39" />
                  <stop offset="1" stop-color="#1E293B" />
                </linearGradient>
              </defs>
            </svg>
          </div>
        </slot>
      </div>
    </div>

    <!-- 底部扩展区插槽 (如分类 Tabs、统计药丸指标等) -->
    <div v-if="$slots.extra" class="hero-extra">
      <slot name="extra" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { Close } from '@element-plus/icons-vue';

const props = withDefaults(
  defineProps<{
    title: string;
    subtitle?: string;
    backgroundImage?: string;
    backgroundVariant?: 'default' | 'ai' | 'learning' | 'knowledge';
    size?: 'normal' | 'large';
    showSearch?: boolean;
    searchPlaceholder?: string;
    showIllustration?: boolean;
    modelValue?: string;
  }>(),
  {
    subtitle: '',
    backgroundImage: '',
    backgroundVariant: 'default',
    size: 'normal',
    showSearch: false,
    searchPlaceholder: '搜索...',
    showIllustration: true,
    modelValue: ''
  }
);

const emit = defineEmits<{
  (e: 'update:modelValue', val: string): void;
  (e: 'search', val: string): void;
}>();

const searchValue = computed({
  get: () => props.modelValue,
  set: (val: string) => emit('update:modelValue', val)
});

/** 已设置背景图时默认隐藏右侧插画，避免与 Banner 主视觉重叠 */
const showIllustrationEffective = computed(
  () => props.showIllustration && !props.backgroundImage
);

const bannerStyle = computed(() => {
  if (props.backgroundImage) {
    return {
      backgroundImage: `url(${props.backgroundImage})`,
      backgroundSize: 'cover',
      backgroundPosition: 'right center'
    };
  }
  return {};
});

function handleSearch() {
  emit('search', searchValue.value);
}

function clearSearch() {
  searchValue.value = '';
  emit('search', '');
}
</script>

<style scoped lang="scss">
.page-hero-banner {
  position: relative;
  width: 100%;
  border-radius: 18px;
  padding: 32px 36px 28px;
  margin-bottom: 22px;
  overflow: hidden;
  box-sizing: border-box;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 8px 30px rgba(22, 119, 255, 0.06), 0 1px 3px rgba(0, 0, 0, 0.02);
  transition: all 0.3s ease;

  // 1. 预设高雅渐变背景
  &--default {
    background: linear-gradient(135deg, #EAF3FF 0%, #F1F6FF 45%, #E5EFFF 100%);
  }

  &--ai {
    background: linear-gradient(135deg, #EEF4FF 0%, #F5F3FF 50%, #E8F0FE 100%);
  }

  &--learning {
    background: linear-gradient(135deg, #E8F8F5 0%, #EEF6FF 50%, #EBF4FE 100%);
  }

  &--knowledge {
    background: linear-gradient(135deg, #EFF6FF 0%, #FAF5FF 50%, #F0F9FF 100%);
  }

  // 尺寸变体
  &--large {
    padding: 38px 44px 34px;
    .hero-title {
      font-size: 28px !important;
      letter-spacing: -0.3px;
    }
  }

  // 2. 梦幻柔和科技光晕 (Orb)
  .glow-orb {
    position: absolute;
    border-radius: 50%;
    pointer-events: none;
    filter: blur(55px);
    z-index: 0;

    &--left {
      top: -40px;
      left: 10%;
      width: 180px;
      height: 180px;
      background: rgba(22, 119, 255, 0.12);
    }

    &--right {
      bottom: -40px;
      right: 15%;
      width: 220px;
      height: 220px;
      background: rgba(114, 46, 209, 0.08);
    }
  }

  .hero-inner-container {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 28px;
  }

  // 3. 文案排版
  .hero-content {
    flex: 1;
    max-width: 680px;

    .hero-title-group {
      .hero-title {
        font-size: 24px;
        font-weight: 700;
        color: #0F172A;
        margin: 0 0 8px 0;
        line-height: 1.3;
      }

      .hero-subtitle {
        font-size: 14.5px;
        color: #475569;
        margin: 0;
        line-height: 1.6;
      }
    }

    // 4. 纯正长圆胶囊搜索框 (Capsule Search)
    .hero-search-wrapper {
      margin-top: 18px;

      .capsule-search-box {
        display: flex;
        align-items: center;
        width: 100%;
        max-width: 520px;
        height: 46px;
        background: #FFFFFF;
        border: 1.5px solid #E2E8F0;
        border-radius: 9999px; // 纯正长圆跑道
        padding: 3px 4px 3px 18px;
        box-shadow: 0 4px 16px rgba(22, 119, 255, 0.07);
        box-sizing: border-box;
        transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

        &:hover {
          border-color: #CBD5E1;
        }

        &:focus-within {
          border-color: #1677FF;
          box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.16);
        }

        .search-prefix-icon {
          display: flex;
          align-items: center;
          color: #94A3B8;
          margin-right: 10px;

          .search-svg {
            width: 18px;
            height: 18px;
          }
        }

        .capsule-search-input {
          flex: 1;
          height: 100%;
          border: none;
          outline: none;
          background: transparent;
          font-size: 14px;
          color: #1E293B;

          &::placeholder {
            color: #94A3B8;
            font-size: 13.5px;
          }
        }

        .search-clear-btn {
          background: transparent;
          border: none;
          color: #94A3B8;
          font-size: 12px;
          width: 22px;
          height: 22px;
          border-radius: 50%;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-right: 4px;

          &:hover {
            background: #F1F5F9;
            color: #64748B;
          }
        }

        .capsule-search-submit {
          height: 38px;
          padding: 0 22px;
          border-radius: 9999px; // 胶囊搜索按钮
          background: #1677FF;
          color: #FFFFFF;
          font-size: 14px;
          font-weight: 500;
          border: none;
          cursor: pointer;
          transition: all 0.2s;
          white-space: nowrap;

          &:hover {
            background: #4096FF;
            box-shadow: 0 2px 8px rgba(22, 119, 255, 0.3);
          }

          &:active {
            background: #0958D9;
          }
        }
      }
    }

    .hero-actions {
      margin-top: 18px;
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }

  // 5. 右侧插画
  .hero-illustration {
    position: relative;
    z-index: 1;
    flex-shrink: 0;

    .default-illustration {
      width: 240px;
      height: 150px;
      display: flex;
      align-items: center;
      justify-content: center;

      .robot-tech-svg {
        width: 100%;
        height: 100%;
        overflow: visible;

        .float-chip {
          animation: floatY 4s ease-in-out infinite alternate;
        }

        .float-ai-badge {
          animation: floatY 4.5s ease-in-out 1s infinite alternate;
        }

        .robot-main {
          animation: robotHover 3.5s ease-in-out infinite alternate;
        }
      }
    }
  }

  // 6. 下方扩展区 (如分类 Tabs)
  .hero-extra {
    position: relative;
    z-index: 1;
    margin-top: 22px;
    padding-top: 18px;
    border-top: 1px solid rgba(226, 232, 240, 0.6);
  }
}

// 动画
@keyframes floatY {
  0% { transform: translateY(0); }
  100% { transform: translateY(-8px); }
}

@keyframes robotHover {
  0% { transform: translate(85px, 45px); }
  100% { transform: translate(85px, 39px); }
}

// 响应式
@media (max-width: 1024px) {
  .page-hero-banner {
    padding: 24px 24px 20px;

    .hero-illustration {
      display: none;
    }

    .hero-content {
      max-width: 100%;
    }
  }
}
</style>
