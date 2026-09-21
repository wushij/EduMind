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

        <!-- 按新图 2508×627 像素坐标精确对齐：胶囊外圈 x:230~1179 (9.171%~37.839%), y:342~433 (54.545%~14.514%) -->
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
            @keyup.enter="emitSearch"
          />
          <!-- 右侧蓝色按钮点击热区：点击才开始搜索 -->
          <button
            type="button"
            class="banner-search-submit"
            aria-label="搜索"
            @click="emitSearch"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import marketplaceBannerImg from '@/assets/images/ai广场.png';

const props = defineProps<{
  searchKeyword?: string;
  activeCategory?: string;
}>();

const emit = defineEmits<{
  (e: 'update:searchKeyword', val: string): void;
  (e: 'update:activeCategory', val: string): void;
  (e: 'search', val: string): void;
  (e: 'category-change', val: string): void;
}>();

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

// 只有点击搜索按钮或回车才触发搜索
function emitSearch() {
  const val = inputValue.value.trim();
  emit('update:searchKeyword', val);
  emit('search', val);
}
</script>

<style scoped lang="scss">
.ai-marketplace-hero-wrapper {
  margin-bottom: 24px;
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
  aspect-ratio: 2508 / 627;

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
    left: 9.171%;
    top: 54.545%;
    width: 37.839%;
    height: 14.514%;
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
    padding-left: 10%;
    padding-right: 25%;
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
    right: 0.6%;
    top: 5.5%;
    width: 24%;
    height: 90.1%;
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
</style>
