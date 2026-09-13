<template>
  <transition name="banner-slide">
    <div v-if="marqueeItem" class="marquee-banner-wrap">
      <div class="marquee-container">
        <div class="banner-left">
          <span class="live-dot" />
          <el-icon class="broadcast-icon"><Notification /></el-icon>
          <span class="banner-tag">紧急公告</span>
        </div>

        <div class="marquee-content-box">
          <div class="marquee-inner">
            <span class="banner-title">{{ marqueeItem.title }}：</span>
            <span class="banner-body">{{ marqueeItem.content }}</span>
          </div>
        </div>

        <button
          type="button"
          class="banner-close-btn"
          title="关闭公告并标记已读"
          @click="dismissMarquee"
        >
          <el-icon><Close /></el-icon>
        </button>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { Close, Notification } from '@element-plus/icons-vue';
import { useBroadcastPush } from '@/composables/notification/useBroadcastPush';

const { marqueeItem, dismissMarquee } = useBroadcastPush();
</script>

<style scoped lang="scss">
.marquee-banner-wrap {
  width: 100%;
  background: linear-gradient(90deg, #ef4444 0%, #f97316 50%, #ea580c 100%);
  color: #fff;
  position: relative;
  z-index: 999;
  box-shadow: 0 2px 10px rgba(239, 68, 68, 0.3);
}

.marquee-container {
  max-width: 1280px;
  margin: 0 auto;
  padding: 8px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
}

.banner-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.live-dot {
  width: 8px;
  height: 8px;
  border-radius: 4px;
  background-color: #fff;
  animation: pulse-glow 1.5s infinite;
}

@keyframes pulse-glow {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(1.3);
  }
}

.banner-tag {
  font-weight: 700;
  font-size: 12px;
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 9999px;
}

.marquee-content-box {
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
}

.marquee-inner {
  display: inline-block;
  line-height: 1.4;
}

.banner-title {
  font-weight: 700;
}

.banner-close-btn {
  background: transparent;
  border: none;
  color: #fff;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.8;

  &:hover {
    opacity: 1;
    background: rgba(255, 255, 255, 0.2);
  }
}

.banner-slide-enter-active,
.banner-slide-leave-active {
  transition: all 0.3s ease;
}

.banner-slide-enter-from,
.banner-slide-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}
</style>
