<template>
  <el-dialog
    :model-value="modelValue"
    width="520px"
    destroy-on-close
    append-to-body
    class="slider-captcha-dialog"
    :close-on-click-modal="false"
    :show-close="true"
    @update:model-value="$emit('update:modelValue', $event)"
    @closed="handleClosed"
  >
    <template #header>
      <div class="slider-modal-header">
        <div class="slider-header-icon" aria-hidden="true">
          <el-icon><Lock /></el-icon>
        </div>
        <div class="slider-header-text">
          <h3 class="slider-title">安全验证</h3>
          <p class="slider-subtitle">拖动下方滑块，将拼图块对准缺口即可完成验证</p>
        </div>
      </div>
    </template>

    <div class="slider-accent-strip" aria-hidden="true" />

    <div class="slider-captcha-body" :class="{ shake: shaking, success: verified }">
      <div
        class="captcha-stage"
        :style="{
          width: `${displayWidth}px`,
          height: `${displayHeight}px`
        }"
      >
        <img
          v-if="background"
          :src="background"
          class="captcha-bg"
          alt="验证码背景"
          draggable="false"
        />
        <img
          v-if="piece"
          :src="piece"
          class="captcha-piece"
          :style="{
            width: `${pieceWidthPercent}%`,
            height: `${pieceHeightPercent}%`,
            top: `${pieceTopPercent}%`,
            left: `${pieceLeftPercent}%`
          }"
          alt="滑块拼图"
          draggable="false"
        />
        <div v-if="loading" class="captcha-loading">
          <el-icon class="is-loading loading-spin"><Loading /></el-icon>
          <span>正在加载验证图片...</span>
        </div>
        <div v-if="verified" class="captcha-success-mask">
          <div class="success-badge">
            <el-icon class="success-icon"><CircleCheck /></el-icon>
            <span>验证通过</span>
          </div>
        </div>
      </div>

      <div class="slider-track-wrap" :style="{ width: `${displayWidth}px` }">
        <div class="slider-track" :class="{ verified }" aria-label="拖动滑块完成验证">
          <div class="slider-track-fill" :style="{ width: `${scaledOffsetX + handleSize / 2}px` }" />
          <div
            class="slider-handle"
            :class="{ dragging, verified }"
            :style="{ left: `${3 + scaledOffsetX}px` }"
            @pointerdown="onPointerDown"
          >
            <el-icon v-if="!verified"><DArrowRight /></el-icon>
            <el-icon v-else class="success-icon-inline"><CircleCheck /></el-icon>
          </div>
          <span v-if="!dragging && !verified && offsetX === 0" class="slider-hint">向右滑动完成验证</span>
        </div>
      </div>

      <div class="slider-footer" :style="{ width: `${displayWidth}px` }">
        <span class="slider-tip">
          <el-icon><InfoFilled /></el-icon>
          验证通过后自动继续登录
        </span>
        <button type="button" class="refresh-btn" :disabled="loading || verifying" @click="loadChallenge">
          <el-icon><Refresh /></el-icon>
          换一张
        </button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import {
  CircleCheck,
  DArrowRight,
  InfoFilled,
  Loading,
  Lock,
  Refresh
} from '@element-plus/icons-vue';
import { createSliderChallenge, verifySliderCaptcha } from '@/api/auth/auth';
import { createTrackRecorder } from '@/utils/captcha-track';

/** 展示宽度：在服务端 320px 逻辑画布上放大，兼顾清晰度与弹层比例 (对齐 Code Compass) */
const DISPLAY_TARGET_WIDTH = 440;

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    operation?: string;
    username?: string;
  }>(),
  {
    operation: 'LOGIN',
    username: ''
  }
);

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  success: [payload: { captchaToken: string }];
}>();

const loading = ref(false);
const verifying = ref(false);
const verified = ref(false);
const shaking = ref(false);
const dragging = ref(false);
const background = ref('');
const piece = ref('');
const challengeId = ref('');
const pieceSize = ref(48);
const pieceY = ref(0);
const stageWidth = ref(320);
const stageHeight = ref(160);
const maxOffset = ref(260);
const offsetX = ref(0);
const handleSize = 46;

const displayScale = computed(() => {
  if (stageWidth.value <= 0) return 1;
  return DISPLAY_TARGET_WIDTH / stageWidth.value;
});
const displayWidth = computed(() => Math.round(stageWidth.value * displayScale.value));
const displayHeight = computed(() => Math.round(stageHeight.value * displayScale.value));
const scaleX = computed(() => displayWidth.value / stageWidth.value);

const scaledOffsetX = computed(() => Math.round(offsetX.value * scaleX.value));
const pieceLeftPercent = computed(() => (offsetX.value / stageWidth.value) * 100);
const pieceTopPercent = computed(() => (pieceY.value / stageHeight.value) * 100);
const pieceWidthPercent = computed(() => (pieceSize.value / stageWidth.value) * 100);
const pieceHeightPercent = computed(() => (pieceSize.value / stageHeight.value) * 100);

let requestSeq = 0;
let track = createTrackRecorder();
let startClientY = 0;
let trackBaseX = 0;

const canDrag = computed(() => !loading.value && !verifying.value && !verified.value && !!challengeId.value);

async function loadChallenge() {
  const seq = ++requestSeq;
  loading.value = true;
  verified.value = false;
  offsetX.value = 0;
  background.value = '';
  piece.value = '';
  challengeId.value = '';

  try {
    const res = await createSliderChallenge(props.operation, props.username);
    if (seq !== requestSeq) return;
    const data = res.data;
    challengeId.value = data.challengeId;
    background.value = data.background;
    piece.value = data.piece;
    pieceSize.value = data.pieceSize;
    pieceY.value = data.pieceY;
    stageWidth.value = data.width;
    stageHeight.value = data.height;
    maxOffset.value = Math.max(0, data.width - pieceSize.value);
  } catch (err: any) {
    if (seq !== requestSeq) return;
    ElMessage.error(err?.message || '验证码加载失败，请稍后重试');
  } finally {
    if (seq === requestSeq) loading.value = false;
  }
}

function onPointerDown(e: PointerEvent) {
  if (!canDrag.value) return;
  e.preventDefault();
  dragging.value = true;
  track = createTrackRecorder();
  startClientY = e.clientY;
  trackBaseX = offsetX.value;
  track.start(trackBaseX, 0);

  const onMove = (ev: PointerEvent) => {
    if (!dragging.value) return;
    const delta = (ev.clientX - e.clientX) / scaleX.value;
    const next = Math.round(Math.min(maxOffset.value, Math.max(0, trackBaseX + delta)));
    offsetX.value = next;
    track.move(next, ev.clientY - startClientY);
  };

  const onUp = async () => {
    if (!dragging.value) return;
    dragging.value = false;
    window.removeEventListener('pointermove', onMove);
    window.removeEventListener('pointerup', onUp);
    window.removeEventListener('pointercancel', onUp);
    await submitVerify();
  };

  window.addEventListener('pointermove', onMove);
  window.addEventListener('pointerup', onUp);
  window.addEventListener('pointercancel', onUp);
}

async function submitVerify() {
  if (!challengeId.value || verifying.value) return;
  verifying.value = true;
  const { events, durationMs, offsetX: finalX } = track.end();

  try {
    const res = await verifySliderCaptcha({
      challengeId: challengeId.value,
      offsetX: finalX,
      durationMs,
      events,
      username: props.username
    });
    verified.value = true;
    emit('success', { captchaToken: res.data.captchaToken });
    window.setTimeout(() => {
      emit('update:modelValue', false);
    }, 500);
  } catch (err: any) {
    shaking.value = true;
    offsetX.value = 0;
    ElMessage.error(err?.message || '验证失败，请重试');
    window.setTimeout(() => {
      shaking.value = false;
    }, 320);
    await loadChallenge();
  } finally {
    verifying.value = false;
  }
}

function handleClosed() {
  verified.value = false;
  offsetX.value = 0;
  dragging.value = false;
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      void loadChallenge();
    }
  }
);
</script>

<style scoped>
.slider-modal-header {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding-right: 28px;
}

.slider-header-icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #1677ff;
  background: #eaf3ff;
  border: 1px solid rgba(22, 119, 255, 0.18);
  box-shadow: 0 4px 14px rgba(22, 119, 255, 0.1);
  flex-shrink: 0;
}

.slider-title {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.02em;
}

.slider-subtitle {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: #64748b;
}

.slider-accent-strip {
  height: 3px;
  margin: -4px 0 20px;
  border-radius: 999px;
  background: linear-gradient(90deg, #1677ff 0%, #4096ff 50%, #722ed1 100%);
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.15);
}

.slider-captcha-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
}

.captcha-stage {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 20px rgba(30, 80, 150, 0.06);
}

.captcha-bg {
  position: absolute;
  inset: 0;
  display: block;
  width: 100%;
  height: 100%;
  object-fit: fill;
  user-select: none;
  pointer-events: none;
}

.captcha-piece {
  position: absolute;
  display: block;
  object-fit: fill;
  z-index: 2;
  user-select: none;
  pointer-events: none;
}

.captcha-loading {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(4px);
  color: #1677ff;
  font-size: 14px;
}

.loading-spin {
  font-size: 28px;
}

.captcha-success-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(34, 197, 94, 0.12);
  backdrop-filter: blur(2px);
  z-index: 5;
}

.success-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  border-radius: 999px;
  background: rgba(34, 197, 94, 0.18);
  border: 1px solid rgba(34, 197, 94, 0.45);
  color: #4ade80;
  font-size: 14px;
  font-weight: 700;
}

.success-icon {
  font-size: 22px;
}

.slider-track-wrap {
  margin: 0 auto;
}

.slider-track {
  position: relative;
  height: 52px;
  border-radius: 999px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  overflow: hidden;
  box-shadow: inset 0 1px 3px rgba(15, 23, 42, 0.04);
  transition: border-color 0.2s ease;
}

.slider-track.verified {
  border-color: rgba(34, 197, 94, 0.45);
}

.slider-track-fill {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  border-radius: 999px 0 0 999px;
  background: linear-gradient(90deg, rgba(22, 119, 255, 0.08), rgba(22, 119, 255, 0.2));
  pointer-events: none;
}

.slider-handle {
  position: absolute;
  top: 3px;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: #ffffff;
  border: 1.5px solid rgba(22, 119, 255, 0.35);
  box-shadow: 0 4px 14px rgba(22, 119, 255, 0.16);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1677ff;
  font-size: 18px;
  cursor: grab;
  touch-action: none;
  z-index: 2;
  transition: box-shadow 0.15s ease, border-color 0.15s ease;
}

.slider-handle.dragging {
  cursor: grabbing;
  box-shadow: 0 6px 18px rgba(22, 119, 255, 0.28);
  border-color: #1677ff;
}

.slider-handle.verified {
  border-color: rgba(34, 197, 94, 0.6);
  color: #22c55e;
}

.success-icon-inline {
  font-size: 20px;
  color: #22c55e;
}

.slider-hint {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding-left: 52px;
  color: #94a3b8;
  font-size: 14px;
  pointer-events: none;
  user-select: none;
}

.slider-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.slider-tip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #64748b;
}

.refresh-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid rgba(22, 119, 255, 0.28);
  background: #eaf3ff;
  color: #1677ff;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.2s ease;
}

.refresh-btn:hover:not(:disabled) {
  background: #dbeafe;
  border-color: rgba(22, 119, 255, 0.45);
}

.refresh-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.slider-captcha-body.shake {
  animation: captcha-shake 0.32s ease;
}

@keyframes captcha-shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-6px); }
  40% { transform: translateX(6px); }
  60% { transform: translateX(-4px); }
  80% { transform: translateX(4px); }
}
</style>

<style>
.slider-captcha-dialog.el-dialog {
  width: 520px !important;
  max-width: 94vw !important;
  border-radius: 22px !important;
  background: #ffffff !important;
  border: 1px solid #e2e8f0 !important;
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.12) !important;
  overflow: hidden !important;
}

.slider-captcha-dialog .el-dialog__header {
  padding: 24px 28px 8px !important;
  margin-right: 0 !important;
  border-bottom: none !important;
}

.slider-captcha-dialog .el-dialog__body {
  padding: 0 28px 28px !important;
}

.slider-captcha-dialog .el-dialog__headerbtn {
  top: 18px !important;
  right: 18px !important;
  width: 32px !important;
  height: 32px !important;
  font-size: 16px !important;
  color: #94a3b8 !important;
}

.slider-captcha-dialog .el-dialog__headerbtn:hover {
  color: #475569 !important;
  background: #f1f5f9 !important;
  border-radius: 50%;
}
</style>
