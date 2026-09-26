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

      <div class="drawer-section">
        <h4 class="section-title">绑定模型</h4>
        <!-- 工具不单独绑模型：modelId 由后端按「场景路由 → 平台默认模型」解析回填；
             拿不到解析结果（如本地 Mock 兜底）时如实说明，不展示假的模型名 -->
        <p class="section-p" :class="{ 'model-id': !!tool.modelId }">
          {{ tool.modelId || '跟随平台默认模型（运行时由 AI 网关解析）' }}
        </p>
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
import ColorIcon from '@/components/common/ColorIcon.vue';
import type { IconTheme } from '@/types/common/icon';
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

const TOOL_HINTS: Record<string, { input: string; output: string }> = {
  tool_chat: {
    input: '在课程空间内选择目标课程，直接输入学术疑问、概念难点或代码调试问题。',
    output: '实时流式输出结合课程知识库的权威解答，支持 Markdown、LaTeX 公式与代码语法高亮。'
  },
  tool_question_gen: {
    input: '选择课程及章节知识点范围，配置目标题型、难度等级、知识侧重与题目生成数量。',
    output: '批量生成包含题干、选项、答案与解析的结构化试题，支持在线微调并一键入库试题中心。'
  },
  tool_exam_gen: {
    input: '设定试卷总分、考试时长、知识覆盖面及单选/多选/简答等各类题型的数量与分值配比。',
    output: '自动装配标准化期中/期末试卷，支持全局难易度预览、一键换题调分与试卷保存复用。'
  },
  tool_grading: {
    input: '选择学生提交的作业或考试答卷，系统自动关联对应的标准参考答案与评分细则。',
    output: '客观题秒级判分，主观题多维度评分并生成针对性错因分析与评语，支持教师复核。'
  },
  tool_summary: {
    input: '选择目标课程或上传章节课件/长篇资料，设定知识提炼深度与复习备考重点。',
    output: '提炼章节核心要点脉络、公式定律清单与易错点陷阱，生成考前精要复习指南。'
  },
  tool_practice: {
    input: '自动结合学生薄弱知识点画像与错题记录，也可由学生主动勾选需要巩固的章节模块。',
    output: '自适应推送难度阶梯递进的针对性练习题包，实时反馈作答正误并提供变式解析。'
  },
  tool_lesson_prep: {
    input: '先在「创建新课程空间」完成课程初始化与教学大纲，再进入课节的教案工作台；填写课节标题、课时与导读（导读可用「AI 提炼」，从教情报告进入时会自动带入薄弱考点），系统自动关联课程知识库。',
    output: '依据教学设计目标与 RAG 检索资料生成结构化课节教案：教学目标、重难点、师生活动与板书建议，在右侧助教确认后可直接插入课节正文并落库保存、继续编辑。'
  }
};

const inputHint = computed(() => {
  if (!props.tool) return '';
  const custom = TOOL_HINTS[props.tool.id];
  if (custom) return custom.input;
  if (props.tool.executionMode === 'V05_NOTICE') {
    return '该工具规划中：未来将支持在抽屉内直接输入文本并流式返回结果。';
  }
  return '点击「立即使用」进入专用工作台，按页面向导填写相关参数后提交。';
});

const outputHint = computed(() => {
  if (!props.tool) return '';
  const custom = TOOL_HINTS[props.tool.id];
  if (custom) return custom.output;
  if (props.tool.executionMode === 'V05_NOTICE') {
    return '该工具规划中：未来将输出结构化结果并支持导出。';
  }
  return '生成对应的结构化业务成果，可在专用工作台内预览、编辑与保存。';
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
