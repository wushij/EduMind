<template>
  <div
    class="page-hero-banner"
    :class="[
      `page-hero-banner--${size}`,
      `page-hero-banner--${backgroundVariant}`
    ]"
    :style="bannerStyle"
  >
    <!-- 装饰性柔光背景微动效 -->
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

        <!-- 按钮或外部操作区插槽（标题下方，左对齐） -->
        <div v-if="$slots.actions" class="hero-actions">
          <slot name="actions" />
        </div>
      </div>

      <!-- 右侧操作区（与标题同行，右对齐） -->
      <div v-if="$slots.toolbar" class="hero-toolbar">
        <slot name="toolbar" />
      </div>

      <!-- 右侧自定义插画区 (仅在外部显式传入插槽时呈现，默认不展示 AI logo) -->
      <div v-if="$slots.illustration" class="hero-illustration">
        <slot name="illustration" />
      </div>
    </div>

    <!-- 底部扩展区插槽 (如分类 Tabs、统计药丸指标等) -->
    <div v-if="$slots.extra" class="hero-extra">
      <slot name="extra" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Close } from '@element-plus/icons-vue';

const props = withDefaults(
  defineProps<{
    title: string;
    subtitle?: string;
    backgroundImage?: string;
    backgroundVariant?: 'default' | 'ai' | 'learning' | 'knowledge' | 'system' | 'analytics' | 'question' | 'course';
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
    showIllustration: false,
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
  border-radius: 24px; // 柔和长圆大边框
  padding: 18px 28px 16px; // 精致紧凑内边距，大幅减少垂直空间占用
  margin-bottom: 18px;
  overflow: hidden;
  box-sizing: border-box;
  border: 1px solid rgba(255, 255, 255, 0.85);
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04), 0 1px 3px rgba(0, 0, 0, 0.02);
  transition: all 0.3s ease;

  // 1. 预设高雅渐变背景 (保留原设计底色)
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

  &--system {
    background: linear-gradient(135deg, #EEF2FF 0%, #F8FAFC 45%, #E0E7FF 100%);
  }

  &--analytics {
    background: linear-gradient(135deg, #ECFDF5 0%, #F0F9FF 50%, #EEF2FF 100%);
  }

  &--question {
    background: linear-gradient(135deg, #FFF7ED 0%, #FEF3C7 40%, #FFFBEB 100%);
  }

  &--course {
    background: linear-gradient(135deg, #EAF3FF 0%, #EEF2FF 45%, #E0E7FF 100%);
  }

  // 尺寸变体
  &--large {
    padding: 24px 32px 20px;
    .hero-title {
      font-size: 22px !important;
      letter-spacing: -0.2px;
    }
  }

  // 2. 梦幻柔和科技光晕 (Orb)
  .glow-orb {
    position: absolute;
    border-radius: 50%;
    pointer-events: none;
    filter: blur(50px);
    z-index: 0;

    &--left {
      top: -30px;
      left: 8%;
      width: 140px;
      height: 140px;
      background: rgba(22, 119, 255, 0.1);
    }

    &--right {
      bottom: -30px;
      right: 12%;
      width: 160px;
      height: 160px;
      background: rgba(114, 46, 209, 0.06);
    }
  }

  .hero-inner-container {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 20px;
  }

  // 3. 文案排版
  .hero-content {
    flex: 1;

    .hero-title-group {
      .hero-title {
        font-size: 19px;
        font-weight: 700;
        color: #0F172A;
        margin: 0 0 4px 0;
        line-height: 1.35;
      }

      .hero-subtitle {
        font-size: 13px;
        color: #475569;
        margin: 0;
        line-height: 1.55;
      }
    }

    // 4. 纯正长圆胶囊搜索框 (Capsule Search)
    .hero-search-wrapper {
      margin-top: 14px;

      .capsule-search-box {
        display: flex;
        align-items: center;
        width: 100%;
        max-width: 480px;
        height: 40px;
        background: #FFFFFF;
        border: 1.5px solid #E2E8F0;
        border-radius: 9999px; // 纯正长圆跑道
        padding: 2px 4px 2px 16px;
        box-shadow: 0 2px 10px rgba(22, 119, 255, 0.06);
        box-sizing: border-box;
        transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

        &:hover {
          border-color: #CBD5E1;
        }

        &:focus-within {
          border-color: #1677FF;
          box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.14);
        }

        .search-prefix-icon {
          display: flex;
          align-items: center;
          color: #94A3B8;
          margin-right: 8px;

          .search-svg {
            width: 16px;
            height: 16px;
          }
        }

        .capsule-search-input {
          flex: 1;
          height: 100%;
          border: none;
          outline: none;
          background: transparent;
          font-size: 13.5px;
          color: #1E293B;

          &::placeholder {
            color: #94A3B8;
            font-size: 13px;
          }
        }

        .search-clear-btn {
          background: transparent;
          border: none;
          color: #94A3B8;
          font-size: 12px;
          width: 20px;
          height: 20px;
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
          height: 32px;
          padding: 0 18px;
          border-radius: 9999px; // 胶囊搜索按钮
          background: #1677FF;
          color: #FFFFFF;
          font-size: 13px;
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
      margin-top: 14px;
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }

  // 5. 右侧操作工具栏（与标题同行）
  .hero-toolbar {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 12px;
    flex-wrap: wrap;
    padding-top: 2px;
  }

  // 6. 右侧插画 (仅在显式传入 slot 时渲染)
  .hero-illustration {
    position: relative;
    z-index: 1;
    flex-shrink: 0;
    max-height: 100px;
    display: flex;
    align-items: center;
  }

  // 7. 下方扩展区 (如分类 Tabs、统计药丸指标等)
  .hero-extra {
    position: relative;
    z-index: 1;
    margin-top: 14px;
    padding-top: 12px;
    border-top: 1px solid rgba(226, 232, 240, 0.55);
  }
}

// 响应式
@media (max-width: 1024px) {
  .page-hero-banner {
    padding: 16px 20px 14px;

    .hero-illustration {
      display: none;
    }

    .hero-inner-container {
      flex-direction: column;
      align-items: stretch;
    }

    .hero-toolbar {
      width: 100%;
      justify-content: flex-end;
      padding-top: 0;
    }

    .hero-content {
      max-width: 100%;
    }
  }
}
</style>
