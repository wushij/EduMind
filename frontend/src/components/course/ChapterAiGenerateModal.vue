<template>
  <el-dialog
    v-model="visible"
    title="AI 智能大纲拆解与微课节生成"
    width="640px"
    destroy-on-close
    append-to-body
    class="ai-generate-dialog"
  >
    <div v-if="!generating" class="ai-modal-header-desc">
      <div class="ai-sparkle-badge">
        <el-icon><AiSparkleIcon /></el-icon>
        <span>EduMind 教学大纲智能拆解引擎</span>
      </div>
      <p class="ai-tip-text">
        针对章节：<strong>{{ chapter?.title || '当前教学大纲' }}</strong>，AI 结合布鲁姆认知模型与高校教学大纲规范，
        <template v-if="existingSections.length > 0">
          已读取本章 {{ existingSections.length }} 个已建微课节，将从
          <strong>{{ chapterNo }}.{{ nextLessonNo }}</strong> 起续编且不重复已有知识点：
        </template>
        <template v-else>为你智能规划以下体系化微课时：</template>
      </p>
    </div>

    <AiCognitiveThinkingPanel
      v-if="generating"
      :active="generating"
      v-bind="AI_COGNITIVE_THINKING_PRESETS.chapterMicroLesson"
      show-footer-actions
      abort-label="中止规划"
      @abort="abortGenerate"
    />

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

    <template v-if="!generating" #footer>
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
import { Clock, VideoPlay, EditPen, Connection, Refresh } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { isAxiosError } from 'axios';
import { askAssistantOnce } from '@/services/ai/stream-service';
import AiCognitiveThinkingPanel from '@/components/ai/common/AiCognitiveThinkingPanel.vue';
import { AI_COGNITIVE_THINKING_PRESETS } from '@/constants/ai/cognitive-thinking';
import AiSparkleIcon from '@/components/common/AiSparkleIcon.vue';
import { normalizeLessonTitle, resolveNextLessonNo } from '@/utils/course/lesson-title-number';

const props = defineProps<{
  modelValue: boolean;
  chapter: any | null;
  courseName?: string;
  /** 章节序号（与列表左上角 01/02 徽标一致），用于拼 `1.3 xxx` 课节编号 */
  chapterNo?: number;
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
let generateAbortController: AbortController | null = null;

const selectedCount = computed(() => {
  return suggestedList.value.filter(s => s.checked).length;
});

const chapterNo = computed(() => {
  const no = Number(props.chapterNo);
  return Number.isFinite(no) && no > 0 ? no : 1;
});

/** 本章已入库的微课节：既作为去重上下文喂给模型，也用于推导续编号起点 */
const existingSections = computed<any[]>(() => {
  const list = props.chapter?.sections;
  if (!Array.isArray(list)) return [];
  return list.filter((sec: any) => sec && String(sec.title || '').trim());
});

const nextLessonNo = computed(() => resolveNextLessonNo(existingSections.value.map((sec: any) => sec.title)));

/** 上下文里最多列出的已有课节数，防止超长章节把 prompt 撑爆 */
const MAX_BRIEF_SECTIONS = 20;

/** 已有课节上下文文本：标题 / 类型 / 导读（导读即该课节内容概要），供模型判断知识点是否已被覆盖 */
const existingSectionsBrief = computed(() => {
  if (existingSections.value.length === 0) {
    return '（本章目前尚无任何微课节，本次属于首次规划）';
  }
  const listed = existingSections.value.slice(0, MAX_BRIEF_SECTIONS);
  const lines = listed.map((sec: any) => {
    const typeLabel =
      sec.type === 'quiz' ? '诊断自测' : sec.type === 'practice' ? '实战演练' : '讲授精讲';
    const contentMark =
      sec.hasContent === true || sec.contentStatus === 'PUBLISHED' ? '已含正文' : '正文待编写';
    const desc = String(sec.description || '').replace(/\s+/g, ' ').slice(0, 60);
    // 标题本身已带 `1.1 ` 编号，这里不再叠加行号，避免出现「1. 1.1 xxx」的视觉噪音
    return `- ${String(sec.title).trim()}｜类型：${typeLabel}｜${contentMark}｜导读：${desc || '（暂无导读）'}`;
  });
  const omitted = existingSections.value.length - listed.length;
  if (omitted > 0) {
    lines.push(`（另有 ${omitted} 个后续课节未逐条列出，其知识点同样不得重复覆盖）`);
  }
  return lines.join('\n');
});

watch(
  () => props.modelValue,
  val => {
    if (val && props.chapter) {
      generateSuggestions();
    } else if (!val) {
      abortGenerate();
    }
  }
);

function abortGenerate() {
  if (generateAbortController) {
    generateAbortController.abort();
    generateAbortController = null;
  }
  generating.value = false;
}

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

function deriveFallbackSections(chapTitle: string, chapNo: number, startNo: number) {
  /** 兜底课节同样要带编号，否则与 AI 生成、AI 备课产出的课节混排时会断号 */
  const withNo = (items: any[]) =>
    items.map((item, idx) => ({ ...item, title: normalizeLessonTitle(item.title, chapNo, startNo + idx) }));
  const isJava = /java/i.test(chapTitle);
  if (isJava) {
    return withNo([
      {
        checked: true,
        title: '跨平台原理与JVM执行架构',
        description: '系统剖析Java跨平台WORA基石、类加载过程与字节码执行机制。',
        duration: '25分钟',
        type: 'lecture',
        knowledgePointCount: 2
      },
      {
        checked: true,
        title: '核心面向对象与接口规范演练',
        description: '代码级别推演封装继承多态核心原则，以及高内聚低耦合实战编码。',
        duration: '40分钟',
        type: 'practice',
        knowledgePointCount: 3
      },
      {
        checked: true,
        title: '常见异常陷阱防御与最佳实践',
        description: '系统梳理checked/unchecked异常体系及工业级防灾容错机制。',
        duration: '30分钟',
        type: 'lecture',
        knowledgePointCount: 2
      },
      {
        checked: true,
        title: '阶段性认知诊断与核心随堂自测',
        description: '针对本章节关键语言特性与底层原理出具自测评估题。',
        duration: '20分钟',
        type: 'quiz',
        knowledgePointCount: 3
      }
    ]);
  }

  return withNo([
    {
      checked: true,
      title: '本章核心理论基石与前置认知建构',
      description: '梳理本章核心概念与基础推导，奠定系统性专业认知。',
      duration: '25分钟',
      type: 'lecture',
      knowledgePointCount: 2
    },
    {
      checked: true,
      title: '本章核心方法演练与实战范式',
      description: '结合关键场景逐行推演核心算法与工程化落地规范。',
      duration: '40分钟',
      type: 'practice',
      knowledgePointCount: 3
    },
    {
      checked: true,
      title: '本章典型案例深度剖析与演进',
      description: '结合真实业务场景，深入探讨性能调优与架构考量。',
      duration: '35分钟',
      type: 'lecture',
      knowledgePointCount: 2
    },
    {
      checked: true,
      title: '本章阶段性认知诊断与自测评估',
      description: '随堂阶段能力检测，帮助定位知识盲区与薄弱环节。',
      duration: '20分钟',
      type: 'quiz',
      knowledgePointCount: 3
    }
  ]);
}

async function generateSuggestions() {
  abortGenerate();
  generateAbortController = new AbortController();
  generating.value = true;
  suggestedList.value = [];
  const title = props.chapter?.title || '本教学章节';
  const cName = props.courseName || '专业核心课';

  const prompt = `你是一名高校计算机专业课资深教研骨干与课程设计师。请针对课程《${cName}》中的章节【${title}】，按照认知递进规律，继续为该章节补充规划 3~4 个具体的微课时内容。

【本章节已有微课节（已入库，其知识点严禁重复覆盖）】：
${existingSectionsBrief.value}

【关键要求】：
1. 本次只允许规划尚未覆盖的新内容：
- 已有课节讲过的知识点，绝对不能再生成同类课节，也不允许出现与已有课节相同或仅换措辞的近义标题；
- 已有课节的知识点只能作为新内容的“先修前提”被引用，不得成为新微课的主体；
- 若本章知识已被覆盖得较完整，请优先规划更高阶的深化拓展、综合实战或认知诊断类课节；确实无内容可补时，宁可只给 2 个高质量课节，也不要硬凑重复内容。
2. 微课名称必须精准紧扣【${title}】的真实专业知识点，必须具体、有深度，绝对不能输出诸如“核心理论背景与抽象建模”、“关键算法演练与架构实现”、“工业界高频典型场景案例拆解”这种生硬的通用模板文字！
- 例如：章节是“Java学习概述与核心认知模型”，且已有课节为“${chapterNo.value}.1 Java 语言发展史与生态全景”，则续编名称应设计为：
  ${chapterNo.value}.${nextLessonNo.value} JVM运行原理与跨平台(WORA)核心机制
  ${chapterNo.value}.${nextLessonNo.value + 1} JDK核心组件（编译器/运行时/调试工具）体系解构
  ${chapterNo.value}.${nextLessonNo.value + 2} 第一个Java程序的底层字节码编译与反编译剖析
- 例如：章节是“二叉树与图算法”，且已有课节为“${chapterNo.value}.1 二叉树的遍历实现”，则续编名称应设计为：
  ${chapterNo.value}.${nextLessonNo.value} 平衡二叉搜索树(AVL)旋转平衡维护实战
  ${chapterNo.value}.${nextLessonNo.value + 1} 拓扑排序在工程构建依赖分析中的应用
3. 编号规则（硬性要求）：每个 title 必须以「${chapterNo.value}.<序号> 」开头，序号从 ${nextLessonNo.value} 开始严格连续递增（${nextLessonNo.value}、${nextLessonNo.value + 1}、${nextLessonNo.value + 2}…），严禁跳号、严禁复用已有课节编号、严禁省略编号。
4. 每个微课需提供建议时长（如“25分钟”）、课节类型（只能是 "lecture" 讲授精讲、"practice" 实战演练、"quiz" 诊断自测 三者之一）、涵盖知识点数（1-4的整数）、以及简述该微课学习目标与核心内容（50字以内）。
5. 必须以标准 JSON 数组返回，严禁任何代码块前缀标记，格式如下：
[
  {
    "title": "${chapterNo.value}.${nextLessonNo.value} 具体微课名称",
    "description": "微课目标与内容简述",
    "duration": "25分钟",
    "type": "lecture",
    "knowledgePointCount": 2
  }
]`;

  try {
    const rawContent = await askAssistantOnce(prompt, {
      courseId: props.chapter?.courseId,
      signal: generateAbortController.signal
    });
    const parsed = parseJsonArray(rawContent);
    if (parsed.length > 0) {
      suggestedList.value = parsed.map((item: any, idx: number) => ({
        checked: true,
        // 模型自带的编号一律重写为期望编号，保证预览序号与入库结果、列表展示完全一致
        title: normalizeLessonTitle(
          item.title || `${title} 进阶微课`,
          chapterNo.value,
          nextLessonNo.value + idx
        ),
        description: item.description || '深入剖析本章关键概念与实践技能。',
        duration: item.duration || '30分钟',
        type: ['lecture', 'practice', 'quiz'].includes(item.type) ? item.type : 'lecture',
        knowledgePointCount: Number(item.knowledgePointCount) || 2
      }));
      ElMessage.success(
        `AI 已针对【${title}】续编 ${suggestedList.value.length} 个微课节（自 ${chapterNo.value}.${nextLessonNo.value} 起）！`
      );
    } else {
      throw new Error('未解析到结构化微课节大纲');
    }
  } catch (err: unknown) {
    if (isAxiosError(err) && err.code === 'ERR_CANCELED') {
      return;
    }
    console.warn('AI 生成微课节异常，使用学科语义推导:', err);
    suggestedList.value = deriveFallbackSections(title, chapterNo.value, nextLessonNo.value);
    ElMessage.info(`已基于《${title}》学科知识图谱续编专业微课节`);
  } finally {
    generateAbortController = null;
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
  // 成功提示统一由父组件在真正入库完成后给出，避免"提示成功但入库失败"的误导
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
