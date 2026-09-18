<template>
  <el-dropdown
    trigger="click"
    placement="bottom-start"
    popper-class="lesson-editor-ai-dropdown"
    @command="(cmd: string) => emit('command', cmd)"
  >
    <button type="button" class="lesson-ai-trigger" title="课节备课 AI 能力">
      <el-icon class="lesson-ai-trigger__icon"><EditPen /></el-icon>
      <span class="lesson-ai-trigger__text">AI 辅助</span>
      <el-icon class="lesson-ai-trigger__arrow"><ArrowDown /></el-icon>
    </button>
    <template #dropdown>
      <el-dropdown-menu class="lesson-ai-menu">
        <el-dropdown-item disabled class="lesson-ai-menu__header">
          <span class="lesson-ai-menu__header-title">课节备课助手</span>
        </el-dropdown-item>

        <el-dropdown-item
          v-for="item in bodyItems"
          :key="item.command"
          :command="item.command"
          class="lesson-ai-menu__item"
        >
          <el-icon class="lesson-ai-menu__item-icon"><component :is="item.icon" /></el-icon>
          <div class="lesson-ai-menu__item-body">
            <span class="lesson-ai-menu__item-label">{{ item.label }}</span>
            <span class="lesson-ai-menu__item-desc">{{ item.desc }}</span>
          </div>
        </el-dropdown-item>

        <el-dropdown-item divided disabled class="lesson-ai-menu__group">
          课节属性
        </el-dropdown-item>
        <el-dropdown-item
          v-for="item in metaItems"
          :key="item.command"
          :command="item.command"
          class="lesson-ai-menu__item"
        >
          <el-icon class="lesson-ai-menu__item-icon"><EditPen /></el-icon>
          <div class="lesson-ai-menu__item-body">
            <span class="lesson-ai-menu__item-label">{{ item.label }}</span>
            <span class="lesson-ai-menu__item-desc">{{ item.desc }}</span>
          </div>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import type { Component } from 'vue';
import {
  ArrowDown,
  EditPen,
  Document,
  Right,
  View,
  DataLine,
  Share
} from '@element-plus/icons-vue';

const emit = defineEmits<{
  command: [string];
}>();

type MenuItem = {
  command: string;
  label: string;
  desc: string;
  icon: Component;
};

const bodyItems: MenuItem[] = [
  {
    command: 'generate',
    label: '一键生成课节正文',
    desc: '在右侧全局助教生成正文，支持插入编辑器',
    icon: Document
  },
  {
    command: 'continue',
    label: '承接上文续写',
    desc: '优先使用选区，否则从正文末尾续写',
    icon: Right
  },
  {
    command: 'polish',
    label: '润色正文',
    desc: '润色选区或全文，保持教学结构',
    icon: EditPen
  },
  {
    command: 'outline',
    label: '生成小节大纲',
    desc: '输出 Markdown 标题层级与要点提纲',
    icon: DataLine
  },
  {
    command: 'review',
    label: '教学审查',
    desc: '检查难度、案例与课堂互动是否充分',
    icon: View
  },
  {
    command: 'mermaid',
    label: '生成流程图',
    desc: '将选区或正文转为 Mermaid 代码块',
    icon: Share
  }
];

const metaItems = [
  { command: 'description', label: '提炼课节导读', desc: '依据课节标题生成，写入右侧「导读」' },
  { command: 'objective', label: '生成学习目标', desc: '依据课节标题生成，写入右侧学习目标卡' },
  { command: 'knowledge', label: '推荐考查考点', desc: '匹配课程知识点并关联' }
];
</script>

<style scoped lang="scss">
.lesson-ai-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 999px;
  border: 1px solid rgba(59, 130, 246, 0.35);
  background: linear-gradient(180deg, #f8fbff 0%, #eff6ff 100%);
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  line-height: 1;
  transition: background 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease;

  &:hover {
    background: #dbeafe;
    border-color: rgba(37, 99, 235, 0.5);
    box-shadow: 0 2px 8px rgba(37, 99, 235, 0.12);
  }

  &__icon {
    font-size: 14px;
  }

  &__arrow {
    font-size: 12px;
    opacity: 0.75;
  }
}
</style>

<style lang="scss">
.lesson-editor-ai-dropdown.el-popper {
  padding: 0;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.12);
  overflow: hidden;
}

.lesson-editor-ai-dropdown .lesson-ai-menu {
  padding: 6px 0 8px;
  min-width: 280px;
}

.lesson-editor-ai-dropdown .lesson-ai-menu__header {
  padding: 10px 14px 8px !important;
  margin: 0;
  border-bottom: 1px solid #f1f5f9;
  cursor: default;
  opacity: 1 !important;
  height: auto;
}

.lesson-editor-ai-dropdown .lesson-ai-menu__header-title {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}

.lesson-editor-ai-dropdown .lesson-ai-menu__group {
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8 !important;
  cursor: default;
  padding: 6px 14px 4px !important;
  opacity: 1 !important;
}

.lesson-editor-ai-dropdown .lesson-ai-menu__item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 8px 14px !important;
  line-height: 1.35;
}

.lesson-editor-ai-dropdown .lesson-ai-menu__item-icon {
  margin-top: 2px;
  font-size: 16px;
  color: #2563eb;
  flex-shrink: 0;
}

.lesson-editor-ai-dropdown .lesson-ai-menu__item-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.lesson-editor-ai-dropdown .lesson-ai-menu__item-label {
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
}

.lesson-editor-ai-dropdown .lesson-ai-menu__item-desc {
  font-size: 11px;
  color: #64748b;
  white-space: normal;
}

.lesson-editor-ai-dropdown .el-dropdown-menu__item:not(.is-disabled):hover {
  background: #f0f9ff;
  color: inherit;
}
</style>
