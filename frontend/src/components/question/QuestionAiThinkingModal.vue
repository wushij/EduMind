<template>
  <el-dialog
    :model-value="visible"
    :title="dialogTitle"
    width="640px"
    class="question-ai-thinking-dialog"
    destroy-on-close
    :close-on-click-modal="false"
    :show-close="true"
    append-to-body
    @close="handleClose"
  >
    <!-- 阶段 1：认知推演流水线与雷达计时面板 (完全对标图2项目规范) -->
    <div v-if="phase === 'thinking'" class="thinking-stage-container">
      <AiCognitiveThinkingPanel
        :active="visible && phase === 'thinking'"
        :title="currentPreset.title"
        :steps="currentPreset.steps"
        show-footer-actions
        abort-label="中止生成"
        @abort="handleAbort"
      />
    </div>

    <!-- 阶段 2：AI 命题助教教研诊断与辅导建议（无需跳转侧边栏，弹窗内直达） -->
    <div v-else-if="phase === 'tutor-result'" class="tutor-result-stage">
      <div class="tutor-score-banner">
        <div class="banner-badge">
          <el-icon><Trophy /></el-icon>
          <span>命题质量综合评级</span>
        </div>
        <div class="banner-score">
          <span class="score-number">{{ tutorReport.score }}</span>
          <span class="score-label">/ 100 优良</span>
        </div>
      </div>

      <div class="tutor-section">
        <h5 class="section-heading">
          <el-icon class="text-blue-600"><Compass /></el-icon>
          <span>教研诊断意见</span>
        </h5>
        <div class="diagnosis-card">
          <p class="diagnosis-text">{{ tutorReport.diagnosis }}</p>
        </div>
      </div>

      <div class="tutor-section">
        <h5 class="section-heading">
          <el-icon class="text-emerald-600"><Tickets /></el-icon>
          <span>命题优化建议要点</span>
        </h5>
        <ul class="suggestion-list">
          <li v-for="(item, idx) in tutorReport.suggestions" :key="idx" class="suggestion-item">
            <span class="item-dot">{{ idx + 1 }}</span>
            <span class="item-text">{{ item }}</span>
          </li>
        </ul>
      </div>

      <div class="tutor-actions-footer">
        <button type="button" class="btn-cancel" @click="handleClose">
          暂不采纳，保留原样
        </button>
        <button type="button" class="btn-adopt" @click="handleAdopt">
          <el-icon><Check /></el-icon>
          <span>采纳优化建议并更新试题</span>
        </button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { Trophy, Compass, Tickets, Check } from '@element-plus/icons-vue';
import { isAxiosError } from 'axios';
import { resolveApiErrorMessage } from '@/core/http/api-error-message';
import AiCognitiveThinkingPanel from '@/components/ai/common/AiCognitiveThinkingPanel.vue';
import { AI_COGNITIVE_THINKING_PRESETS } from '@/constants/ai/cognitive-thinking';
import { generateQuestions, cancelQuestionGenerate } from '@/api/ai/generation';
import type { Question } from '@/types/question/question';
import { buildQuestionFormAiPromptDirective, type QuestionFormAiThinkingMode } from '@/utils/question/question-form-ai-prompt';
import { normalizeQuestion, hasFilledQuestionOptions } from '@/utils/question/normalize-question';
import { isGarbageQuestionStem } from '@/utils/question/is-garbage-question-stem';

export type ThinkingMode = QuestionFormAiThinkingMode;

const props = defineProps<{
  visible: boolean;
  mode: ThinkingMode;
  form: Partial<Question>;
}>();

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void;
  (e: 'finish', updatedForm: Partial<Question>): void;
  (e: 'abort'): void;
}>();

const phase = ref<'thinking' | 'tutor-result'>('thinking');
let aiAbortController: AbortController | null = null;
/** 递增后忽略仍在飞行中的旧请求结果 */
let aiRunSeq = 0;
let userAborted = false;

async function releaseBackendGenerateLock() {
  try {
    await cancelQuestionGenerate();
  } catch {
    // 静默：锁释放失败时 TTL 仍会过期
  }
}

function abortExecution(notifyBackend = true) {
  userAborted = true;
  aiRunSeq += 1;
  if (aiAbortController) {
    aiAbortController.abort();
    aiAbortController = null;
  }
  if (notifyBackend) {
    void releaseBackendGenerateLock();
  }
}

const currentPreset = computed(() => {
  switch (props.mode) {
    case 'fullAuto':
      return AI_COGNITIVE_THINKING_PRESETS.questionFullAuto;
    case 'tutor':
      return AI_COGNITIVE_THINKING_PRESETS.questionAiTutor;
    case 'options':
      return AI_COGNITIVE_THINKING_PRESETS.questionOptions;
    case 'analysis':
      return AI_COGNITIVE_THINKING_PRESETS.questionAnalysis;
    case 'polish':
      return AI_COGNITIVE_THINKING_PRESETS.questionPolish;
    default:
      return AI_COGNITIVE_THINKING_PRESETS.questionGenerate;
  }
});

const dialogTitle = computed(() => {
  switch (props.mode) {
    case 'fullAuto':
      return 'AI 命题引擎 · 全套智能出题装配';
    case 'tutor':
      return phase.value === 'tutor-result' ? 'AI 命题助教 · 教研质量诊断报告' : 'AI 命题助教 · 教学诊断与辅导推演';
    case 'options':
      return 'AI 命题引擎 · 推演选项与干扰项';
    case 'analysis':
      return 'AI 教学引擎 · 推演考点深度解析';
    case 'polish':
      return 'AI 语言引擎 · 学术规范化题干润色';
    default:
      return 'AI 智能题库引擎';
  }
});

// 助教诊断报告数据（根据大模型真实推演结果动态生成）
const tutorReport = ref({
  score: 95,
  diagnosis: '',
  suggestions: [] as string[],
  optimizedStem: '',
  optimizedAnalysis: ''
});

watch(
  () => props.visible,
  (val) => {
    if (val) {
      phase.value = 'thinking';
      userAborted = false;
      executeRealAiProcess();
    } else {
      abortExecution();
    }
  }
);

function parseTutorScore(analysis: string): number {
  const m = analysis.match(/(?:综合评分|评分|得分)[：:\s]*(\d{1,3})\s*(?:分|\/100|\/\s*100)?/);
  if (m) {
    const n = Number(m[1]);
    if (Number.isFinite(n)) return Math.min(100, Math.max(0, n));
  }
  return 85;
}

function extractTutorDiagnosis(analysis: string, stem?: string): string {
  const trimmed = analysis.trim();
  if (!trimmed) {
    return stem ? '已完成基础质检，详见下方优化建议。' : '题干考察知识点针对性强，设问符合教学规范要求。';
  }
  const firstBlock = trimmed.split(/\n【/)[0]?.trim();
  return firstBlock.length > 280 ? `${firstBlock.slice(0, 277)}…` : firstBlock;
}

function extractTutorSuggestions(analysis: string): string[] {
  const lines = analysis
    .split(/\n/)
    .map((l) => l.replace(/^[\d一二三四五六七八九十]+[、.)．]\s*/, '').trim())
    .filter((l) => l.length > 4);
  const fromBullets = lines.filter((l) => /^[-*•]/.test(l) || l.startsWith('建议'));
  const picked = (fromBullets.length ? fromBullets : lines).slice(0, 5);
  if (picked.length >= 2) return picked;
  return [
    '核对题干设问是否单一明确，选择/判断题建议使用全角括号「（ ）」。',
    '检查干扰项是否覆盖常见概念混淆点，避免明显错误选项。',
    '在解析中补充分步推导与易错避坑，便于学生复盘。'
  ];
}

/**
 * 真实调用后端 AI 试题大模型引擎（与课程中心规范完全统一）
 */
async function executeRealAiProcess() {
  if (aiAbortController) {
    aiAbortController.abort();
    aiAbortController = null;
  }
  const runSeq = ++aiRunSeq;
  userAborted = false;
  aiAbortController = new AbortController();

  const courseId = Number(props.form.courseId);
  if (!Number.isFinite(courseId) || courseId <= 0) {
    ElMessage.warning('请先选择所属课程后再使用 AI 辅助');
    emit('update:visible', false);
    return;
  }
  const stem = (props.form.stem || '').trim();
  const type = props.form.type || 'SINGLE_CHOICE';
  const courseLabel =
    props.form.courseName?.trim() ||
    (props.form as { title?: string }).title?.trim() ||
    `课程 #${courseId}`;

  const promptDirective = buildQuestionFormAiPromptDirective(props.mode, props.form, courseLabel);

  function shouldIgnoreResult(): boolean {
    return userAborted || runSeq !== aiRunSeq;
  }

  try {
    const res = await generateQuestions(
      {
        courseId,
        questionTypes: [type],
        count: 1,
        difficulty: props.form.difficulty || 'MEDIUM',
        scorePerQuestion: props.form.score || 5,
        promptDirective,
        knowledgePointNames: props.form.knowledgePointNames
      },
      {
        signal: aiAbortController.signal
      }
    );

    if (shouldIgnoreResult()) {
      return;
    }

    const questions = res?.data || [];
    const generated = normalizeQuestion(questions[0] || {});

    if (!generated.stem || isGarbageQuestionStem(generated.stem)) {
      ElMessage.warning('AI 命题模型未返回有效试题，请稍后重试');
      emit('update:visible', false);
      return;
    }

    const applyOptionsPatch = (patch: Partial<Question>) => {
      if (!hasFilledQuestionOptions(generated.options)) {
        return false;
      }
      patch.options = generated.options.map((o) => ({ ...o }));
      patch.correctAnswer =
        generated.correctAnswer || generated.options.find((o) => o.isCorrect)?.key || 'A';
      return true;
    };

    // 针对不同模式装配真实大模型返回数据
    const patch: Partial<Question> = {};

    if (props.mode === 'polish') {
      patch.stem = generated.stem || stem;
    } else if (props.mode === 'options') {
      if (!applyOptionsPatch(patch)) {
        ElMessage.warning('AI 未返回可写入的选项内容（options 为空或格式不对），请重试或检查模型 JSON 输出');
        emit('update:visible', false);
        return;
      }
    } else if (props.mode === 'analysis') {
      patch.analysis = generated.analysis || '';
      if (!patch.analysis || patch.analysis.length < 10) {
        ElMessage.warning('AI 未返回有效解析内容');
        emit('update:visible', false);
        return;
      }
    } else if (props.mode === 'fullAuto') {
      if (generated.stem) patch.stem = generated.stem;
      const optionsOk = applyOptionsPatch(patch);
      if (generated.analysis) patch.analysis = generated.analysis;
      if (generated.difficulty) patch.difficulty = generated.difficulty;
      if (generated.knowledgePointNames?.length) {
        patch.knowledgePointNames = generated.knowledgePointNames;
      }
      if (
        !optionsOk &&
        (props.form.type === 'SINGLE_CHOICE' || props.form.type === 'MULTIPLE_CHOICE')
      ) {
        ElMessage.warning('解析/题干已更新，但选项未写入：模型未返回有效 options，请单独点击「AI 自动生成选项」');
      }
    } else if (props.mode === 'tutor') {
      tutorReport.value.score = parseTutorScore(generated.analysis);
      tutorReport.value.diagnosis = extractTutorDiagnosis(generated.analysis, generated.stem);
      tutorReport.value.suggestions = extractTutorSuggestions(generated.analysis);
      tutorReport.value.optimizedStem = generated.stem && generated.stem !== stem ? generated.stem : stem;
      tutorReport.value.optimizedAnalysis = generated.analysis || props.form.analysis || '';
      phase.value = 'tutor-result';
      return;
    }

    emit('finish', patch);
    emit('update:visible', false);
  } catch (err: unknown) {
    if (isAxiosError(err) && err.code === 'ERR_CANCELED') {
      void releaseBackendGenerateLock();
      return;
    }
    if (shouldIgnoreResult()) {
      return;
    }
    const msg = resolveApiErrorMessage(err, 'AI 命题生成服务异常，请稍后重试');
    ElMessage.error(msg);
    emit('update:visible', false);
  } finally {
    aiAbortController = null;
  }
}

function handleAdopt() {
  const updated: Partial<Question> = {};
  if (tutorReport.value.optimizedStem) {
    updated.stem = tutorReport.value.optimizedStem;
  }
  if (tutorReport.value.optimizedAnalysis) {
    updated.analysis = tutorReport.value.optimizedAnalysis;
  }
  emit('finish', updated);
  emit('update:visible', false);
}

function handleAbort() {
  abortExecution();
  emit('abort');
  emit('update:visible', false);
  ElMessage.info('已中止本次 AI 生成，可立即重新发起');
}

function handleClose() {
  if (phase.value === 'thinking') {
    abortExecution();
  } else {
    abortExecution(false);
  }
  emit('update:visible', false);
}
</script>

<style scoped lang="scss">
.question-ai-thinking-dialog {
  :deep(.el-dialog) {
    border-radius: 24px;
    overflow: hidden;
    box-shadow: 0 24px 64px rgba(15, 23, 42, 0.18);
    background: #ffffff;
  }

  :deep(.el-dialog__header) {
    padding: 20px 24px 14px;
    margin-right: 0;
    border-bottom: 1px solid #f1f5f9;

    .el-dialog__title {
      font-size: 16px;
      font-weight: 700;
      color: #0f172a;
    }
  }

  :deep(.el-dialog__body) {
    padding: 10px 24px 24px;
  }

  .thinking-stage-container {
    width: 100%;
    padding: 8px 0;
  }

  .tutor-result-stage {
    display: flex;
    flex-direction: column;
    gap: 16px;
    padding-top: 10px;

    .tutor-score-banner {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 14px 20px;
      background: linear-gradient(135deg, #eff6ff 0%, #e0e7ff 100%);
      border: 1px solid #bfdbfe;
      border-radius: 16px;

      .banner-badge {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        font-size: 14px;
        font-weight: 700;
        color: #1e40af;

        .el-icon {
          font-size: 18px;
          color: #2563eb;
        }
      }

      .banner-score {
        .score-number {
          font-size: 26px;
          font-weight: 800;
          color: #2563eb;
        }
        .score-label {
          font-size: 13px;
          color: #475569;
          margin-left: 4px;
          font-weight: 600;
        }
      }
    }

    .tutor-section {
      .section-heading {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 13.5px;
        font-weight: 700;
        color: #1e293b;
        margin: 0 0 8px 0;

        .el-icon {
          font-size: 15px;
        }
      }

      .diagnosis-card {
        padding: 12px 16px;
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 12px;

        .diagnosis-text {
          font-size: 13.5px;
          line-height: 1.6;
          color: #334155;
          margin: 0;
        }
      }

      .suggestion-list {
        list-style: none;
        padding: 0;
        margin: 0;
        display: flex;
        flex-direction: column;
        gap: 8px;

        .suggestion-item {
          display: flex;
          align-items: flex-start;
          gap: 10px;
          padding: 10px 14px;
          background: #f8fafc;
          border: 1px solid #e2e8f0;
          border-radius: 10px;

          .item-dot {
            width: 20px;
            height: 20px;
            border-radius: 50%;
            background: #e2e8f0;
            color: #475569;
            font-size: 11px;
            font-weight: 700;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            flex-shrink: 0;
            margin-top: 1px;
          }

          .item-text {
            font-size: 13px;
            line-height: 1.5;
            color: #334155;
          }
        }
      }
    }

    .tutor-actions-footer {
      display: flex;
      gap: 12px;
      margin-top: 10px;

      .btn-cancel {
        flex: 1;
        height: 42px;
        border-radius: 9999px;
        border: 1px solid #cbd5e1;
        background: #ffffff;
        color: #475569;
        font-weight: 600;
        font-size: 13px;
        cursor: pointer;
        transition: all 0.15s ease;

        &:hover {
          background: #f1f5f9;
          color: #1e293b;
        }
      }

      .btn-adopt {
        flex: 1.8;
        height: 42px;
        border-radius: 9999px;
        border: none;
        background: linear-gradient(135deg, #1677ff 0%, #2563eb 100%);
        color: #ffffff;
        font-weight: 700;
        font-size: 13.5px;
        cursor: pointer;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 6px;
        box-shadow: 0 4px 14px rgba(22, 119, 255, 0.28);
        transition: all 0.2s ease;

        &:hover {
          background: linear-gradient(135deg, #0958d9 0%, #1d4ed8 100%);
          transform: translateY(-1px);
        }
      }
    }
  }
}
</style>
