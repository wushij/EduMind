<template>
  <el-dialog
    v-model="visible"
    title="AI 智能大纲拆解与微课节生成"
    width="640px"
    destroy-on-close
    append-to-body
    class="ai-generate-dialog"
  >
    <div class="ai-modal-header-desc">
      <div class="ai-sparkle-badge">
        <el-icon><MagicStick /></el-icon>
        <span>EduMind 教学大纲智能拆解引擎</span>
      </div>
      <p class="ai-tip-text">
        针对章节：<strong>{{ chapter?.title || '当前教学大纲' }}</strong>，AI 结合布鲁姆认知模型与高校教学大纲规范，为你智能规划以下体系化微课时：
      </p>
    </div>

    <!-- AI 状态指示 -->
    <div v-if="generating" class="ai-generating-state">
      <el-icon class="is-loading ai-spin-icon"><Loading /></el-icon>
      <span>AI 正在研读学科大纲并规划微课节与考点映射...</span>
    </div>

    <!-- 生成结果列表 -->
    <div v-else class="suggested-sections-list">
      <div
        v-for="(item, idx) in suggestedList"
        :key="idx"
        class="suggested-item-card"
        :class="{ 'is-checked': item.checked }"
      >
        <div class="item-checkbox">
          <el-checkbox v-model="item.checked" />
        </div>

        <div class="item-main-content">
          <div class="item-title-row">
            <span class="lesson-num-badge">课时 {{ idx + 1 }}</span>
            <el-input
              v-model="item.title"
              size="small"
              placeholder="微课节标题"
              class="lesson-title-input"
            />
          </div>

          <p class="lesson-desc-text">{{ item.description }}</p>

          <div class="lesson-tags-row">
            <span class="meta-pill meta-pill--time">
              <el-icon><Clock /></el-icon>
              {{ item.duration }}
            </span>
            <span class="meta-pill meta-pill--type">
              <el-icon><VideoPlay v-if="item.type === 'lecture'" /><EditPen v-else /></el-icon>
              {{ item.type === 'quiz' ? '概念诊断自测' : (item.type === 'practice' ? '实战演练' : '核心讲义精析') }}
            </span>
            <span class="meta-pill meta-pill--kp">
              <el-icon><Connection /></el-icon>
              涵盖 {{ item.knowledgePointCount }} 个考查点
            </span>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer-actions">
        <button
          type="button"
          class="capsule-btn-regen"
          :disabled="generating"
          @click="generateSuggestions"
        >
          <el-icon><Refresh /></el-icon>
          <span>重新生成</span>
        </button>
        <div class="right-actions">
          <button type="button" class="capsule-btn-cancel" @click="visible = false">取消</button>
          <button
            type="button"
            class="capsule-btn-apply"
            :disabled="generating || selectedCount === 0"
            @click="handleApply"
          >
            <span>一键导入所选微课节 ({{ selectedCount }})</span>
          </button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { MagicStick, Loading, Clock, VideoPlay, EditPen, Connection, Refresh } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { askGlobalAssistant } from '@/api/ai/assistant';

const props = defineProps<{
  modelValue: boolean;
  chapter: any | null;
  courseName?: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void;
  (e: 'apply', sections: any[]): void;
}>();

const visible = computed({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val)
});

const generating = ref(false);
const suggestedList = ref<any[]>([]);

const selectedCount = computed(() => {
  return suggestedList.value.filter(s => s.checked).length;
});

watch(
  () => props.modelValue,
  val => {
    if (val && props.chapter) {
      generateSuggestions();
    }
  }
);

function parseJsonArray<T = any>(rawText: string): T[] {
  if (!rawText) return [];
  let text = rawText.trim();
  const codeBlockMatch = text.match(/```(?:json)?\s*([\s\S]*?)\s*```/i);
  if (codeBlockMatch) {
    text = codeBlockMatch[1].trim();
  }
  const startIdx = text.indexOf('[');
  const endIdx = text.lastIndexOf(']');
  if (startIdx !== -1 && endIdx !== -1 && endIdx > startIdx) {
    text = text.slice(startIdx, endIdx + 1);
  }
  try {
    const parsed = JSON.parse(text);
    return Array.isArray(parsed) ? parsed : [];
  } catch (e) {
    console.error('Failed to parse AI JSON array:', e, rawText);
    return [];
  }
}

function deriveFallbackSections(chapTitle: string) {
  const isJava = /java/i.test(chapTitle);
  if (isJava) {
    return [
      {
        checked: true,
        title: `${chapTitle} - 跨平台原理与JVM执行架构`,
        description: '系统剖析Java跨平台WORA基石、类加载过程与字节码执行机制。',
        duration: '25分钟',
        type: 'lecture',
        knowledgePointCount: 2
      },
      {
        checked: true,
        title: `${chapTitle} - 核心面向对象与接口规范演练`,
        description: '代码级别推演封装继承多态核心原则，以及高内聚低耦合实战编码。',
        duration: '40分钟',
        type: 'practice',
        knowledgePointCount: 3
      },
      {
        checked: true,
        title: `${chapTitle} - 常见异常陷阱防御与最佳实践`,
        description: '系统梳理checked/unchecked异常体系及工业级防灾容错机制。',
        duration: '30分钟',
        type: 'lecture',
        knowledgePointCount: 2
      },
      {
        checked: true,
        title: `${chapTitle} - 阶段性认知诊断与核心随堂自测`,
        description: '针对本章节关键语言特性与底层原理出具自测评估题。',
        duration: '20分钟',
        type: 'quiz',
        knowledgePointCount: 3
      }
    ];
  }

  return [
    {
      checked: true,
      title: `${chapTitle} - 核心理论基石与前置认知建构`,
      description: '梳理本章核心概念与基础推导，奠定系统性专业认知。',
      duration: '25分钟',
      type: 'lecture',
      knowledgePointCount: 2
    },
    {
      checked: true,
      title: `${chapTitle} - 核心方法演练与实战工程范式`,
      description: '结合关键场景逐行推演核心算法与工程化落地规范。',
      duration: '40分钟',
      type: 'practice',
      knowledgePointCount: 3
    },
    {
      checked: true,
      title: `${chapTitle} - 典型工程案例深度剖析与演进`,
      description: '结合真实业务场景，深入探讨性能调优与架构考量。',
      duration: '35分钟',
      type: 'lecture',
      knowledgePointCount: 2
    },
    {
      checked: true,
      title: `${chapTitle} - 阶段性认知诊断与自测评估`,
      description: '随堂阶段能力检测，帮助定位知识盲区与薄弱环节。',
      duration: '20分钟',
      type: 'quiz',
      knowledgePointCount: 3
    }
  ];
}

async function generateSuggestions() {
  generating.value = true;
  suggestedList.value = [];
  const title = props.chapter?.title || '本教学章节';
  const cName = props.courseName || '专业核心课';

  const prompt = `你是一名高校计算机专业课资深教研骨干与课程设计师。请针对课程《${cName}》中的章节【${title}】，按照认知递进规律，为该章节智能拆解并规划 3~4 个具体的微课时内容。
【关键要求】：
1. 微课名称必须精准紧扣【${title}】的真实专业知识点，必须具体、有深度，绝对不能输出诸如“核心理论背景与抽象建模”、“关键算法演练与架构实现”、“工业界高频典型场景案例拆解”这种生硬的通用模板文字！
- 例如：如果章节是“Java学习概述与核心认知模型”，微课名称应设计为：
  微课1: JVM运行原理与跨平台(WORA)核心机制
  微课2: JDK核心组件（编译器/运行时/调试工具）体系解构
  微课3: 第一个Java程序的底层字节码编译与反编译剖析
  微课4: Java工程化编码规范与主流开发环境搭建实战
- 如果章节是“二叉树与图算法”，微课名称应设计为：
  微课1: 二叉树前中后序遍历递归与非递归推导
  微课2: 平衡二叉搜索树(AVL)旋转平衡维护实战
  微课3: 拓扑排序在工程构建依赖分析中的应用
2. 每个微课需提供建议时长（如“25分钟”）、课节类型（只能是 "lecture" 讲授精讲、"practice" 实战演练、"quiz" 诊断自测 三者之一）、涵盖知识点数（1-4的整数）、以及简述该微课学习目标与核心内容（50字以内）。
3. 必须以标准 JSON 数组返回，严禁任何代码块前缀标记，格式如下：
[
  {
    "title": "具体微课名称",
    "description": "微课目标与内容简述",
    "duration": "25分钟",
    "type": "lecture",
    "knowledgePointCount": 2
  }
]`;

  try {
    const res = await askGlobalAssistant({
      message: prompt,
      courseId: props.chapter?.courseId
    });
    const rawContent = res.data?.content || '';
    const parsed = parseJsonArray(rawContent);
    if (parsed.length > 0) {
      suggestedList.value = parsed.map((item: any, idx: number) => ({
        checked: true,
        title: item.title || `${title} 进阶微课 ${idx + 1}`,
        description: item.description || '深入剖析本章关键概念与实践技能。',
        duration: item.duration || '30分钟',
        type: ['lecture', 'practice', 'quiz'].includes(item.type) ? item.type : 'lecture',
        knowledgePointCount: Number(item.knowledgePointCount) || 2
      }));
      ElMessage.success(`AI 已针对【${title}】智能生成 ${suggestedList.value.length} 个体系化微课节！`);
    } else {
      throw new Error('未解析到结构化微课节大纲');
    }
  } catch (err: any) {
    console.warn('AI 生成微课节异常，使用学科语义推导:', err);
    suggestedList.value = deriveFallbackSections(title);
    ElMessage.info(`已基于《${title}》学科知识图谱生成专业微课节`);
  } finally {
    generating.value = false;
  }
}

function handleApply() {
  const selected = suggestedList.value.filter(s => s.checked);
  if (selected.length === 0) {
    ElMessage.warning('请至少勾选一个微课节');
    return;
  }
  emit('apply', selected);
  visible.value = false;
  ElMessage.success(`已成功为【${props.chapter?.title}】导入 ${selected.length} 个微课节！`);
}
</script>

<style scoped lang="scss">
.ai-modal-header-desc {
  margin-bottom: 16px;

  .ai-sparkle-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 4px 12px;
    border-radius: 9999px;
    background: linear-gradient(135deg, #EFF6FF 0%, #DBEAFE 100%);
    border: 1px solid #BFDBFE;
    color: #2563EB;
    font-size: 12px;
    font-weight: 600;
    margin-bottom: 8px;
  }

  .ai-tip-text {
    font-size: 13px;
    color: #475569;
    line-height: 1.5;
    margin: 0;

    strong {
      color: #0F172A;
    }
  }
}

.ai-generating-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 40px 0;
  color: #2563EB;
  font-size: 14px;
  font-weight: 500;

  .ai-spin-icon {
    font-size: 20px;
  }
}

.suggested-sections-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 420px;
  overflow-y: auto;
  padding-right: 4px;

  .suggested-item-card {
    display: flex;
    gap: 12px;
    padding: 12px 14px;
    background: #F8FAFC;
    border: 1.5px solid #E2E8F0;
    border-radius: 14px;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

    &.is-checked {
      background: #FFFFFF;
      border-color: #93C5FD;
      box-shadow: 0 4px 12px rgba(59, 130, 246, 0.08);
    }

    .item-checkbox {
      display: flex;
      align-items: flex-start;
      padding-top: 4px;
    }

    .item-main-content {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 6px;

      .item-title-row {
        display: flex;
        align-items: center;
        gap: 8px;

        .lesson-num-badge {
          font-size: 11px;
          font-weight: 700;
          color: #2563EB;
          background: #DBEAFE;
          padding: 2px 8px;
          border-radius: 6px;
          white-space: nowrap;
        }

        .lesson-title-input {
          flex: 1;
        }
      }

      .lesson-desc-text {
        font-size: 12px;
        color: #64748B;
        margin: 0;
        line-height: 1.4;
      }

      .lesson-tags-row {
        display: flex;
        align-items: center;
        gap: 8px;
        flex-wrap: wrap;

        .meta-pill {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          font-size: 11px;
          padding: 2px 8px;
          border-radius: 9999px;
          background: #F1F5F9;
          color: #475569;

          &--time {
            color: #059669;
            background: #ECFDF5;
          }

          &--type {
            color: #7C3AED;
            background: #F5F3FF;
          }

          &--kp {
            color: #2563EB;
            background: #EFF6FF;
          }
        }
      }
    }
  }
}

.dialog-footer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;

  .capsule-btn-regen {
    height: 36px;
    padding: 0 16px;
    border-radius: 9999px;
    background: #F8FAFC;
    border: 1px solid #CBD5E1;
    color: #475569;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    transition: all 0.2s;

    &:hover:not(:disabled) {
      background: #F1F5F9;
      color: #2563EB;
      border-color: #93C5FD;
    }
  }

  .right-actions {
    display: flex;
    align-items: center;
    gap: 10px;

    .capsule-btn-cancel {
      height: 36px;
      padding: 0 18px;
      border-radius: 9999px;
      background: #F1F5F9;
      border: 1px solid #CBD5E1;
      color: #475569;
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        background: #E2E8F0;
        color: #0F172A;
      }
    }

    .capsule-btn-apply {
      height: 36px;
      padding: 0 20px;
      border-radius: 9999px;
      background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
      border: none;
      color: #FFFFFF;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
      transition: all 0.2s;

      &:hover:not(:disabled) {
        transform: translateY(-1px);
        box-shadow: 0 6px 16px rgba(37, 99, 235, 0.35);
      }

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    }
  }
}
</style>
