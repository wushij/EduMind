<template>
  <el-dialog
    v-model="visible"
    :title="`教材出处引用 · ${citation?.docTitle || citation?.documentName || '课程资料'}`"
    width="min(640px, 92vw)"
    append-to-body
    destroy-on-close
    class="citation-modal"
  >
    <div v-if="citation" class="modal-inner">
      <div class="meta-row">
        <span class="tag page">页码：P.{{ citation.page || citation.pageNo || 1 }}</span>
        <span class="tag score">
          相关度：{{
            citation.score != null
              ? formatCitationMatchLabel(citation.score, peerScores)
              : '—'
          }}
        </span>
        <span class="tag hint">以下为知识库切片原文（已排版）</span>
      </div>
      <div
        ref="modalBodyRef"
        class="modal-quote-box markdown-body chat-md-content citation-excerpt-md"
        v-html="activeExcerptHtml"
      />
    </div>

    <template #footer>
      <div class="citation-modal-footer">
        <div class="footer-left">
          <el-button
            v-if="canJump"
            link
            type="primary"
            class="jump-link-btn"
            @click="handleJump"
          >
            <el-icon class="jump-icon"><TopRight /></el-icon>
            <span>{{ jumpLabel }}</span>
          </el-button>
        </div>
        <div class="footer-right">
          <el-button type="primary" @click="handleClose">确定</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, inject } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { TopRight } from '@element-plus/icons-vue';
import type { CitationItem } from '@/types/ai/assistant';
import { useTeachingCopilotStore } from '@/stores/ai/teaching-copilot-context';
import { courseAiUiKey } from '@/components/course/course-ai/course-ai-ui-key';
import { formatCitationMatchLabel } from '@/utils/ai/citation-score';
import {
  citationExcerptSource,
  prepareCitationMarkdown
} from '@/utils/ai/citation-excerpt';
import { bindMarkdownCodeCopy, renderChatMarkdown } from '@/utils/ai/chat-markdown';

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    citation: CitationItem | null;
    peerScores?: number[];
    courseId?: number;
    hasJumpHandler?: boolean;
  }>(),
  {
    peerScores: () => [],
    courseId: undefined,
    hasJumpHandler: false
  }
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'jump', citation: CitationItem): void;
}>();

const router = useRouter();
const route = useRoute();
const teachingCopilotStore = useTeachingCopilotStore();
const courseAiCtx = inject(courseAiUiKey, null);

const modalBodyRef = ref<HTMLElement | null>(null);

const visible = computed({
  get: () => props.modelValue,
  set: (val: boolean) => emit('update:modelValue', val)
});

const activeExcerptHtml = computed(() => {
  if (!props.citation) return '';
  const raw = citationExcerptSource(props.citation);
  if (!raw) return '';
  return renderChatMarkdown(prepareCitationMarkdown(raw));
});

interface TargetAction {
  type: 'lesson' | 'knowledge' | 'course';
  label: string;
  run: () => void;
}

const resolvedCourseId = computed<number | undefined>(() => {
  if (props.courseId) return props.courseId;
  const fromWorkspace = courseAiCtx?.currentCourseIdNum.value;
  if (fromWorkspace && fromWorkspace > 0) return fromWorkspace;
  const fromCtx = teachingCopilotStore.activeContext?.courseId;
  if (fromCtx && fromCtx > 0) return fromCtx;
  const fromRoute = Number(route.params.id);
  if (fromRoute && !Number.isNaN(fromRoute) && fromRoute > 0) return fromRoute;
  return undefined;
});

const targetInfo = computed<TargetAction | null>(() => {
  const c = props.citation;
  if (!c) return null;

  const ctx = teachingCopilotStore.activeContext;
  const lessonId = c.lessonChapterId ?? ctx?.lessonChapterId;
  const courseId = resolvedCourseId.value;

  // 1. 优先定位对应课程的课节学习页并精准滚动到出处
  if (courseId && lessonId) {
    return {
      type: 'lesson',
      label: '跳转至出处课节',
      run: () => {
        const hash = c.anchor ? `#${encodeURIComponent(c.anchor)}` : '';
        const cleanExcerpt = (c.snippet || c.excerpt || '')
          .replace(/[#*`$\\]/g, '')
          .replace(/\s+/g, ' ')
          .trim()
          .slice(0, 36);

        router.push({
          path: `/course/${courseId}/learn/${lessonId}`,
          hash,
          query: {
            ...(c.anchor ? { anchor: c.anchor } : {}),
            ...(c.chunkId ? { chunkId: String(c.chunkId) } : {}),
            ...(cleanExcerpt ? { excerpt: cleanExcerpt } : {})
          }
        });
      }
    };
  }

  // 2. 次优定位知识库切片页
  const kbId = c.knowledgeBaseId;
  const chunkId = c.chunkId ?? c.id;
  const docId = c.documentId;
  if (kbId && chunkId) {
    return {
      type: 'knowledge',
      label: '跳转至出处切片',
      run: () => {
        router.push({
          path: `/knowledge/${kbId}/chunks`,
          query: {
            ...(docId ? { docId: String(docId) } : {}),
            chunkId: String(chunkId)
          }
        });
      }
    };
  }

  // 3. 关联知识库（无精确切片 ID 时前往该知识库）
  if (kbId) {
    return {
      type: 'knowledge',
      label: '前往出处知识库',
      run: () => {
        router.push(`/knowledge/${kbId}/documents`);
      }
    };
  }

  // 4. 关联课程（无课节 ID 时前往该课程学习主页）
  if (courseId) {
    return {
      type: 'course',
      label: '前往关联课程',
      run: () => {
        router.push(`/course/${courseId}`);
      }
    };
  }

  return null;
});

// 跳转链接始终常驻显示（统一侧边栏 AI 与课程 AI 交互风格）
const canJump = computed(() => true);

const jumpLabel = computed(() => {
  if (targetInfo.value?.label) {
    return targetInfo.value.label;
  }
  return '跳转至出处';
});

function handleClose() {
  visible.value = false;
}

function handleJump() {
  if (!props.citation) return;
  const item = props.citation;

  // 优先触发父级监听的自定义跳转事件（如全局助手侧边栏需要联动收起抽屉）
  if (props.hasJumpHandler) {
    visible.value = false;
    emit('jump', item);
    return;
  }

  // 否则执行默认内置跳转
  if (targetInfo.value) {
    visible.value = false;
    targetInfo.value.run();
    return;
  }

  // 兜底弱提示并关闭弹窗
  visible.value = false;
  ElMessage.info(`参考来源：${item.documentName || item.docTitle || '课程知识切片'}`);
}

watch(visible, (open) => {
  if (open) {
    nextTick(() => {
      if (modalBodyRef.value) {
        bindMarkdownCodeCopy(modalBodyRef.value);
      }
    });
  }
});
</script>

<style scoped lang="scss">
.modal-inner {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .meta-row {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;

    .tag {
      font-size: 12px;
      padding: 2px 8px;
      border-radius: 4px;

      &.page {
        background: #EFF6FF;
        color: #2563EB;
        font-weight: 600;
      }

      &.score {
        background: #ECFDF5;
        color: #059669;
      }

      &.hint {
        background: #F1F5F9;
        color: #64748B;
        font-weight: 400;
      }
    }
  }

  .modal-quote-box {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 10px;
    padding: 14px 16px;
    max-height: min(52vh, 420px);
    overflow-y: auto;
  }

  .citation-excerpt-md {
    font-size: 13px;
    line-height: 1.65;
    color: #1e293b;

    :deep(h2),
    :deep(h3),
    :deep(h4) {
      font-size: 14px;
      font-weight: 600;
      margin: 12px 0 8px;
      color: #0f172a;
    }

    :deep(p) {
      margin: 0 0 8px;
    }

    :deep(ul),
    :deep(ol) {
      margin: 0 0 10px;
      padding-left: 1.25em;
    }

    :deep(.code-block-wrapper) {
      margin: 10px 0;
    }

    :deep(pre.hljs) {
      font-size: 12px;
      border-radius: 8px;
    }
  }
}

.citation-modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;

  .footer-left {
    display: flex;
    align-items: center;

    .jump-link-btn {
      font-size: 13px;
      font-weight: 500;
      color: #2563eb;
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 4px 6px;
      transition: color 0.15s, opacity 0.15s;

      &:hover {
        color: #1d4ed8;
        text-decoration: underline;
      }

      .jump-icon {
        font-size: 14px;
      }
    }
  }

  .footer-right {
    display: flex;
    align-items: center;
  }
}
</style>
