<template>
  <div class="exam-banner-stage">
    <div class="banner-ratio-box">
      <img
        class="banner-image"
        :src="bannerImg"
        alt="AI 智能组卷"
        draggable="false"
      />
      <div class="banner-float-actions">
        <button type="button" class="capsule-back-btn" @click="$emit('back')">
          <span>← 返回 AI 广场</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  bannerImg: string;
}>();

defineEmits<{
  back: [];
}>();
</script>

<style scoped lang="scss">
.exam-banner-stage {
  width: 100%;
  margin-bottom: 20px;

  .banner-ratio-box {
    position: relative;
    width: 100%;
    aspect-ratio: 2508 / 627;
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 6px 24px rgba(22, 119, 255, 0.08);
    border: 1px solid #E2E8F0;

    .banner-image {
      position: absolute;
      inset: 0;
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
      user-select: none;
    }

    .banner-float-actions {
      position: absolute;
      top: 16px;
      right: 20px;
      z-index: 2;

      .capsule-back-btn {
        height: 34px;
        padding: 0 16px;
        border-radius: 9999px;
        background: rgba(255, 255, 255, 0.9);
        backdrop-filter: blur(8px);
        border: 1px solid rgba(226, 232, 240, 0.85);
        color: #334155;
        font-size: 13px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

        &:hover {
          color: #1677FF;
          background: #FFFFFF;
          border-color: #93C5FD;
        }
      }
    }
  }
}
</style>
