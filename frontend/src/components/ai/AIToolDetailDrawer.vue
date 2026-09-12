<template>
  <el-drawer
    v-model="visible"
    :title="tool?.name || 'AI 工具详情'"
    size="460px"
    direction="rtl"
    class="ai-tool-drawer"
  >
    <div v-if="tool" class="drawer-content">
      <div class="drawer-header-box">
        <ColorIcon
          :name="tool.iconName || 'MagicStick'"
          :theme="(tool.iconTheme as IconTheme) || 'blue'"
          :custom-bg="tool.iconBg"
          size="lg"
          rounded="xl"
        />
        <div class="drawer-header-meta">
          <h3 class="drawer-title">{{ tool.name }}</h3>
          <div class="drawer-tags-row">
            <span class="pill-badge pill-badge--cat">{{ tool.categoryLabel }}</span>
            <span v-if="tool.isHot" class="pill-badge pill-badge--fire">热门</span>
            <span v-if="tool.isRecommended" class="pill-badge pill-badge--rec">平台精选</span>
          </div>
          <p class="usage-line">{{ tool.usageCount }} 次调用</p>
        </div>
      </div>

      <div class="drawer-section">
        <h4 class="section-title">功能场景与特点</h4>
        <p class="section-p">{{ tool.detailedIntro || tool.description }}</p>
      </div>

      <div v-if="tool.modelId" class="drawer-section">
        <h4 class="section-title">绑定模型</h4>
        <p class="section-p model-id">{{ tool.modelId }}</p>
      </div>

      <div class="drawer-section">
        <h4 class="section-title">适用场景与标签</h4>
        <div class="drawer-tags-grid">
          <span v-for="tag in tool.tags" :key="tag" class="pill-tag-large">
            # {{ tag }}
          </span>
        </div>
      </div>

      <div class="drawer-section">
        <h4 class="section-title">输入说明</h4>
        <p class="section-p">{{ inputHint }}</p>
      </div>

      <div class="drawer-section">
        <h4 class="section-title">输出说明</h4>
        <p class="section-p">{{ outputHint }}</p>
      </div>

      <div v-if="tool.executionMode === 'V05_NOTICE'" class="drawer-section">
        <el-alert
          title="该工具完整能力将于 V0.5 正式上线，当前可先使用下方替代入口体验相关能力。"
          type="warning"
          effect="light"
          :closable="false"
          show-icon
        />
      </div>

      <div class="drawer-footer">
        <button type="button" class="capsule-launch-btn" @click="handleLaunch">
          <span>{{ tool.executionMode === 'V05_NOTICE' ? '查看 V0.5 规划说明' : '立即启动使用该工具' }}</span>
          <span>→</span>
        </button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import ColorIcon, { type IconTheme } from '@/components/common/ColorIcon.vue';
import type { AITool } from '@/types/ai/tool';
import { launchAITool } from '@/utils/ai/launch-tool';

const props = defineProps<{
  modelValue: boolean;
  tool?: AITool | null;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void;
}>();

const router = useRouter();

const visible = computed({
  get: () => props.modelValue,
  set: (val: boolean) => emit('update:modelValue', val)
});

const inputHint = computed(() => {
  if (!props.tool) return '';
  if (props.tool.executionMode === 'V05_NOTICE') {
    return 'V0.5 将支持在抽屉内直接输入教学文本、课件摘要或学习问题，并流式返回结果。';
  }
  return '点击「立即使用」进入专用工作台，按页面向导填写课程、章节、题型等参数后提交。';
});

const outputHint = computed(() => {
  if (!props.tool) return '';
  if (props.tool.executionMode === 'V05_NOTICE') {
    return 'V0.5 将输出结构化 Markdown 结果，支持复制、导出与一键入库。';
  }
  return '生成结构化题目、试卷、批改结果或对话内容，可在页面内预览、编辑并保存。';
});

function handleLaunch() {
  if (props.tool) {
    visible.value = false;
    launchAITool(props.tool, router);
  }
}
</script>

<style scoped lang="scss">
.ai-tool-drawer {
  .drawer-content {
    display: flex;
    flex-direction: column;
    gap: 20px;
    padding: 10px 4px 40px;

    .drawer-header-box {
      display: flex;
      align-items: center;
      gap: 16px;
      padding-bottom: 20px;
      border-bottom: 1px solid #f1f5f9;

      .drawer-header-meta {
        .drawer-title {
          margin: 0 0 6px;
          font-size: 18px;
          font-weight: 700;
          color: #0f172a;
        }

        .drawer-tags-row {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-wrap: wrap;

          .pill-badge {
            padding: 2px 10px;
            border-radius: 9999px;
            font-size: 11.5px;
            font-weight: 600;

            &--cat {
              background: #eff6ff;
              color: #1677ff;
            }

            &--fire {
              background: #fff7ed;
              color: #ea580c;
            }

            &--rec {
              background: #fee2e2;
              color: #ef4444;
            }
          }
        }

        .usage-line {
          margin: 8px 0 0;
          font-size: 12px;
          color: #94a3b8;
        }
      }
    }

    .drawer-section {
      .section-title {
        margin: 0 0 10px;
        font-size: 14.5px;
        font-weight: 600;
        color: #1e293b;
      }

      .section-p {
        margin: 0;
        font-size: 13.5px;
        line-height: 1.7;
        color: #475569;

        &.model-id {
          font-family: ui-monospace, monospace;
          color: #1677ff;
        }
      }

      .drawer-tags-grid {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;

        .pill-tag-large {
          padding: 4px 12px;
          border-radius: 9999px;
          background: #f8fafc;
          border: 1px solid #e2e8f0;
          font-size: 12.5px;
          color: #475569;
        }
      }
    }

    .drawer-footer {
      margin-top: 12px;

      .capsule-launch-btn {
        width: 100%;
        height: 44px;
        border-radius: 9999px;
        background: #1677ff;
        color: #ffffff;
        border: none;
        font-size: 14.5px;
        font-weight: 600;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        box-shadow: 0 4px 14px rgba(22, 119, 255, 0.3);
        transition: all 0.22s ease;

        &:hover {
          background: #4096ff;
          transform: translateY(-1px);
        }
      }
    }
  }
}
</style>
