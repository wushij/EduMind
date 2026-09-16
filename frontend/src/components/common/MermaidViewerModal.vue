<template>
  <Teleport to="body">
    <Transition name="edumind-mermaid-fade">
      <div
        v-if="visible"
        ref="overlayRef"
        class="mermaid-modal-overlay"
        tabindex="-1"
        @click.self="close"
        @keydown.esc="close"
      >
        <div class="mermaid-modal-header">
          <div class="header-left">
            <span class="header-title">课程拓扑知识图谱</span>
            <span class="header-badge">{{ zoomPercentage }}%</span>
          </div>
          <div class="header-right">
            <button type="button" class="modal-close-btn" title="关闭 (Esc)" @click="close">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </div>
        </div>

        <div
          ref="viewportRef"
          class="mermaid-canvas-viewport"
          :class="{ 'is-panning': isPanning }"
          @wheel.prevent="handleWheel"
          @mousedown="startPan"
          @dblclick="handleDoubleClick"
        >
          <div ref="contentRef" class="mermaid-canvas-content" :style="canvasStyle" v-html="svgContent" />
        </div>

        <div class="mermaid-modal-toolbar">
          <button type="button" class="tool-action-btn" title="缩小显示比例" @click="zoomOut">
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
              <line x1="8" y1="11" x2="14" y2="11" />
            </svg>
            <span>缩小</span>
          </button>
          <span class="toolbar-divider" />
          <button type="button" class="tool-action-btn" title="放大显示比例" @click="zoomIn">
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
              <line x1="11" y1="8" x2="11" y2="14" />
              <line x1="8" y1="11" x2="14" y2="11" />
            </svg>
            <span>放大</span>
          </button>
          <span class="toolbar-divider" />
          <button type="button" class="tool-action-btn" title="恢复 100% 并居中" @click="resetZoom">
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8" />
              <path d="M3 3v5h5" />
            </svg>
            <span>重置</span>
          </button>
          <span class="toolbar-divider" />
          <button type="button" class="tool-action-btn" title="整图适应窗口可见范围" @click="fitToScreen">
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3" />
            </svg>
            <span>自适应</span>
          </button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue';

const visible = ref(false);
const svgContent = ref('');
const scale = ref(1);
const panX = ref(0);
const panY = ref(0);
const isPanning = ref(false);

const overlayRef = ref<HTMLElement | null>(null);
const viewportRef = ref<HTMLElement | null>(null);
const contentRef = ref<HTMLElement | null>(null);

let startMouseX = 0;
let startMouseY = 0;
let startPanX = 0;
let startPanY = 0;

const zoomPercentage = computed(() => Math.round(scale.value * 100));

const canvasStyle = computed(() => ({
  transform: `translate(${panX.value}px, ${panY.value}px) scale(${scale.value})`,
  transformOrigin: 'center center',
  transition: isPanning.value ? 'none' : 'transform 0.15s cubic-bezier(0.2, 0, 0, 1)'
}));

function open(payload: { svgHtml: string }) {
  svgContent.value = payload.svgHtml || '';
  scale.value = 1;
  panX.value = 0;
  panY.value = 0;
  visible.value = true;
  document.body.style.overflow = 'hidden';

  nextTick(() => {
    overlayRef.value?.focus();
    window.setTimeout(() => fitToScreen(), 60);
  });
}

function close() {
  visible.value = false;
  document.body.style.overflow = '';
}

function zoomIn() {
  scale.value = Math.min(scale.value + 0.25, 5);
}

function zoomOut() {
  scale.value = Math.max(scale.value - 0.25, 0.25);
}

function resetZoom() {
  scale.value = 1;
  panX.value = 0;
  panY.value = 0;
}

function normalizeSvgForViewer(svgEl: SVGSVGElement): { width: number; height: number } {
  svgEl.style.maxWidth = 'none';
  svgEl.style.maxHeight = 'none';
  svgEl.style.width = '';
  svgEl.style.height = '';

  const viewBox = svgEl.viewBox?.baseVal;
  if (viewBox && viewBox.width > 0 && viewBox.height > 0) {
    svgEl.setAttribute('width', String(viewBox.width));
    svgEl.setAttribute('height', String(viewBox.height));
    return { width: viewBox.width, height: viewBox.height };
  }

  const width =
    parseFloat(svgEl.getAttribute('width') || '') || svgEl.getBoundingClientRect().width || 800;
  const height =
    parseFloat(svgEl.getAttribute('height') || '') || svgEl.getBoundingClientRect().height || 600;
  return { width, height };
}

function fitToScreen() {
  if (!viewportRef.value || !contentRef.value) return;
  const svgEl = contentRef.value.querySelector('svg');
  if (!svgEl) return;

  const { width: naturalWidth, height: naturalHeight } = normalizeSvgForViewer(svgEl as SVGSVGElement);
  const vpRect = viewportRef.value.getBoundingClientRect();
  const availableWidth = vpRect.width * 0.92;
  const availableHeight = vpRect.height * 0.88;
  const fitScale = Math.min(availableWidth / naturalWidth, availableHeight / naturalHeight);

  scale.value = Number(Math.min(Math.max(fitScale, 0.1), 5).toFixed(2));
  panX.value = 0;
  panY.value = 0;
}

function handleDoubleClick() {
  if (scale.value !== 1) {
    resetZoom();
  } else {
    fitToScreen();
  }
}

function handleWheel(e: WheelEvent) {
  const zoomFactor = e.deltaY < 0 ? 1.12 : 0.9;
  scale.value = Number(Math.min(Math.max(scale.value * zoomFactor, 0.25), 4).toFixed(2));
}

function startPan(e: MouseEvent) {
  if (e.button !== 0) return;
  isPanning.value = true;
  startMouseX = e.clientX;
  startMouseY = e.clientY;
  startPanX = panX.value;
  startPanY = panY.value;
  window.addEventListener('mousemove', onPanning);
  window.addEventListener('mouseup', stopPan);
}

function onPanning(e: MouseEvent) {
  if (!isPanning.value) return;
  panX.value = startPanX + (e.clientX - startMouseX);
  panY.value = startPanY + (e.clientY - startMouseY);
}

function stopPan() {
  isPanning.value = false;
  window.removeEventListener('mousemove', onPanning);
  window.removeEventListener('mouseup', stopPan);
}

function handleKeydown(e: KeyboardEvent) {
  if (!visible.value) return;
  if (e.key === 'Escape') close();
  else if (e.key === '+' || e.key === '=') zoomIn();
  else if (e.key === '-' || e.key === '_') zoomOut();
  else if (e.key === '0') resetZoom();
}

function openFromZoomButton(btnEl: HTMLElement) {
  const wrap = btnEl.closest('.mermaid-diagram-wrapper');
  if (!wrap) return;
  const diagEl = wrap.querySelector('.mermaid-diagram');
  const svgEl = diagEl?.querySelector('svg');
  if (!svgEl) return;
  open({ svgHtml: svgEl.outerHTML });
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown);
  window.__openMermaidViewer = openFromZoomButton;
});

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown);
  document.body.style.overflow = '';
  delete window.__openMermaidViewer;
});

defineExpose({ open, close });
</script>

<style scoped lang="scss">
.mermaid-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 100000;
  background: rgba(15, 23, 42, 0.82);
  backdrop-filter: blur(12px);
  display: flex;
  flex-direction: column;
  outline: none;
  user-select: none;
}

.mermaid-modal-header {
  height: 56px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(148, 163, 184, 0.25);
  background: rgba(255, 255, 255, 0.96);
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.header-right {
  flex-shrink: 0;
}

.header-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.header-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(22, 119, 255, 0.1);
  color: #1677ff;
  border: 1px solid rgba(22, 119, 255, 0.25);
}

.modal-close-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  color: #475569;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(239, 68, 68, 0.1);
    border-color: rgba(239, 68, 68, 0.35);
    color: #dc2626;
  }
}

.mermaid-canvas-viewport {
  flex: 1;
  width: 100%;
  min-height: 0;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  background: radial-gradient(circle at 50% 40%, rgba(22, 119, 255, 0.06), transparent 55%);

  &.is-panning {
    cursor: grabbing;
  }
}

.mermaid-canvas-content {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  pointer-events: none;

  :deep(svg) {
    max-width: none !important;
    max-height: none !important;
    display: block;
    filter: drop-shadow(0 8px 24px rgba(15, 23, 42, 0.12));
  }
}

.mermaid-modal-toolbar {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  height: 46px;
  padding: 0 10px;
  border-radius: 23px;
  display: flex;
  align-items: center;
  gap: 4px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(22, 119, 255, 0.22);
  box-shadow: 0 8px 28px rgba(15, 23, 42, 0.12);
  z-index: 10;
}

.tool-action-btn {
  height: 34px;
  padding: 0 12px;
  border-radius: 17px;
  border: 1px solid transparent;
  background: transparent;
  color: #334155;
  font-size: 13px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(22, 119, 255, 0.08);
    border-color: rgba(22, 119, 255, 0.25);
    color: #1677ff;
  }
}

.toolbar-divider {
  width: 1px;
  height: 18px;
  background: #e2e8f0;
  margin: 0 2px;
}

.edumind-mermaid-fade-enter-active,
.edumind-mermaid-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.edumind-mermaid-fade-enter-from,
.edumind-mermaid-fade-leave-to {
  opacity: 0;
  transform: scale(0.98);
}
</style>
