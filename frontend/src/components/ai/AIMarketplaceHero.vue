<template>
  <div class="ai-marketplace-hero-wrapper">
    <div class="marketplace-banner-stage">
      <div class="banner-ratio-box">
        <img
          class="banner-image"
          :src="marketplaceBannerImg"
          alt="AI 工具广场"
          draggable="false"
        />

        <!-- 按原图 2172×724 像素坐标精确对齐：胶囊外圈 x:149~1024 (6.860%~40.285%), y:383~472 (52.901%~12.293%) -->
        <div
          class="banner-search-hitbox"
          :class="{ 'is-active': isFocused || !!inputValue }"
        >
          <input
            ref="searchInputRef"
            v-model="inputValue"
            type="text"
            class="banner-search-input"
            aria-label="搜索 AI 工具"
            placeholder="搜索AI工具、应用场景、教学资源..."
            @focus="isFocused = true"
            @blur="isFocused = false"
            @input="handleInput"
            @keyup.enter="emitSearch(true)"
          />
          <!-- 右侧蓝色按钮点击热区：点击才开始搜索 -->
          <button
            type="button"
            class="banner-search-submit"
            aria-label="搜索"
            @click="emitSearch(true)"
          />
        </div>
      </div>
    </div>

    <!-- 分类药丸切换栏 (全面使用 Element Plus 官方矢量组件，彻底告别 Emoji) -->
    <div class="marketplace-filter-dock">
      <div class="pill-category-tabs">
        <button
          v-for="cat in visibleFilterCategories"
          :key="cat.value"
          type="button"
          class="pill-cat-btn"
          :class="{ active: currentCategory === cat.value }"
          @click="handleCategoryClick(cat)"
        >
          <el-icon class="cat-icon"><component :is="cat.icon" /></el-icon>
          <span>{{ cat.label }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, type Component } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { Grid, School, Reading, Star, StarFilled } from '@element-plus/icons-vue';
import { useAuthStore } from '@/stores/auth/auth';
import { canAccessMarketplaceCategory } from '@/utils/ai/tool-role-access';
import marketplaceBannerImg from '@/assets/images/ai工具广场的banner1.png';

type CategoryItem = {
  label: string;
  value: string;
  route: string;
  icon: Component;
};

const props = defineProps<{
  searchKeyword?: string;
  activeCategory?: string;
}>();

const emit = defineEmits<{
  (e: 'update:searchKeyword', val: string): void;
  (e: 'update:activeCategory', val: string): void;
  (e: 'search', val: string): void;
  (e: 'instant-search', val: string): void;
  (e: 'category-change', val: string): void;
}>();

let debounceTimer: ReturnType<typeof setTimeout> | null = null;

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const searchInputRef = ref<HTMLInputElement | null>(null);
const isFocused = ref(false);
const inputValue = ref(props.searchKeyword ?? '');

// 外部搜索词变化时同步内部值
watch(
  () => props.searchKeyword,
  (newVal) => {
    inputValue.value = newVal ?? '';
  }
);

// 全面采用 Element Plus 官方图标组件，杜绝原生 Emoji
const filterCategories: CategoryItem[] = [
  { label: '全部', value: 'ALL', icon: Grid, route: '/ai/marketplace' },
  { label: '教师提效', value: 'TEACHER', icon: School, route: '/ai/marketplace/teacher' },
  { label: '学生助学', value: 'STUDENT', icon: Reading, route: '/ai/marketplace/student' },
  { label: '推荐工具', value: 'RECOMMENDED', icon: Star, route: '/ai/marketplace/recommended' },
  { label: '我的常用', value: 'MY_TOOLS', icon: StarFilled, route: '/ai/marketplace/my-tools' }
];

const visibleFilterCategories = computed(() =>
  filterCategories.filter((cat) =>
    canAccessMarketplaceCategory(cat.value, authStore.currentRole)
  )
);

const currentCategory = computed(() => {
  if (props.activeCategory) return props.activeCategory;
  if (route.path.endsWith('/teacher')) return 'TEACHER';
  if (route.path.endsWith('/student')) return 'STUDENT';
  if (route.path.endsWith('/recommended')) return 'RECOMMENDED';
  if (route.path.endsWith('/my-tools')) return 'MY_TOOLS';
  return 'ALL';
});

function handleInput() {
  const val = inputValue.value;
  emit('update:searchKeyword', val);
  emit('instant-search', val);
  if (debounceTimer) clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
    emit('search', val.trim());
  }, 300);
}

function emitSearch(triggerBackend = true) {
  const val = inputValue.value.trim();
  emit('update:searchKeyword', val);
  emit('instant-search', val);
  if (triggerBackend) {
    emit('search', val);
  }
}

function handleCategoryClick(cat: CategoryItem) {
  emit('update:activeCategory', cat.value);
  emit('category-change', cat.value);
  if (route.path !== cat.route) {
    router.push(cat.route);
  }
}
</script>

<style scoped lang="scss">
.ai-marketplace-hero-wrapper {
  margin-bottom: 20px;
}

.marketplace-banner-stage {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(30, 80, 150, 0.06);
  background: #eef6ff;
}

.banner-ratio-box {
  position: relative;
  width: 100%;
  aspect-ratio: 2172 / 724;

  .banner-image {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    display: block;
    user-select: none;
    pointer-events: none;
  }

  .banner-search-hitbox {
    position: absolute;
    left: 6.860%;
    top: 52.901%;
    width: 40.285%;
    height: 12.293%;
    display: flex;
    align-items: center;
    border-radius: 999px;
    overflow: hidden;
    z-index: 2;
    transition: box-shadow 0.2s ease;

    &.is-active {
      box-shadow: 0 4px 16px rgba(22, 119, 255, 0.2);
    }
  }

  .banner-search-input {
    width: 100%;
    height: 100%;
    padding-left: 10.5%;
    padding-right: 24%;
    margin: 0;
    border: none;
    outline: none;
    background: transparent;
    color: #1e293b;
    font-size: clamp(12px, 0.95vw, 15px);
    font-weight: 500;
    line-height: normal;
    caret-color: #1677ff;
    cursor: text;

    &::placeholder {
      color: #94a3b8;
      font-size: clamp(12px, 0.88vw, 14px);
      font-weight: 400;
    }

    &:focus {
      outline: none;
    }
  }

  .banner-search-submit {
    position: absolute;
    right: 0;
    top: 3.5%;
    width: 23.9%;
    height: 93%;
    border: none;
    outline: none;
    padding: 0;
    margin: 0;
    background: transparent;
    cursor: pointer;
    border-radius: 999px;
    transition: background 0.15s ease;
    z-index: 2;

    &:focus,
    &:focus-visible {
      outline: none;
    }

    &:hover {
      background: rgba(255, 255, 255, 0.15);
    }

    &:active {
      background: rgba(0, 0, 0, 0.1);
    }
  }
}

.marketplace-filter-dock {
  margin-top: 14px;
  line-height: normal;

  .pill-category-tabs {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    flex-wrap: nowrap;
    background: rgba(255, 255, 255, 0.94);
    backdrop-filter: blur(10px);
    padding: 4px 6px;
    border-radius: 9999px;
    border: 1px solid rgba(226, 232, 240, 0.92);
    box-shadow: 0 2px 12px rgba(30, 80, 150, 0.06);

    .pill-cat-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 32px;
      padding: 0 14px;
      border-radius: 9999px;
      border: none;
      background: transparent;
      color: #475569;
      font-size: 12.5px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s ease;
      white-space: nowrap;

      .cat-icon {
        font-size: 14px;
        transition: transform 0.2s ease;
      }

      &:hover {
        color: #1677ff;

        .cat-icon {
          transform: scale(1.1);
        }
      }

      &.active {
        background: #1677ff;
        color: #ffffff;
        font-weight: 600;
        box-shadow: 0 2px 10px rgba(22, 119, 255, 0.32);

        .cat-icon {
          color: #ffffff;
        }
      }
    }
  }
}

@media (max-width: 1200px) {
  .marketplace-filter-dock .pill-category-tabs .pill-cat-btn {
    padding: 0 11px;
    font-size: 12px;
  }
}

@media (max-width: 768px) {
  .marketplace-filter-dock {
    max-width: 100%;
    overflow-x: auto;

    .pill-category-tabs {
      width: max-content;
      max-width: 100%;
    }
  }
}
</style>
