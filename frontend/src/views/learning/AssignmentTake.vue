<template>
  <div class="assignment-take-page" :class="[`font-size-${fontSize}`, { 'is-fullscreen': isFullscreen }]" v-loading="loading">
    <!-- 顶部吸顶控制条 (Sticky Examination Header Bar) -->
    <header class="take-sticky-header">
      <div class="header-inner">
        <!-- 左侧：返回与考试基本信息 (自适应弹性缩短，防挤压) -->
        <div class="header-left">
          <el-button class="btn-back" plain size="small" @click="handleConfirmExit">
            <el-icon class="mr-1"><ArrowLeft /></el-icon>
            返回
          </el-button>
          <div class="exam-title-block">
            <div class="title-row">
              <h2 class="exam-title" :title="paper?.title || '在线作业作答'">
                {{ paper?.title || '在线作业测验' }}
              </h2>
              <el-tag size="small" effect="plain" type="primary" class="exam-tag">课堂测验</el-tag>
            </div>
            <div class="exam-sub-meta">
              <span class="meta-item"><el-icon><Trophy /></el-icon> 满分 {{ paper?.totalScore ?? 100 }}分</span>
              <span v-if="paper?.deadline" class="meta-item" :title="`截止时间：${formatDeadlineText(paper.deadline)}`">
                <el-icon><Calendar /></el-icon> 截止：{{ formatShortDeadline(paper.deadline) }}
              </span>
            </div>
          </div>
        </div>

        <!-- 右侧：作答状态与操作控制台 (同流平铺，彻底杜绝元素相互遮挡) -->
        <div class="header-right">
          <!-- 作答进度微徽章 -->
          <div class="progress-pill">
            <span class="progress-label">进度</span>
            <span class="progress-ratio"><strong>{{ answeredCount }}</strong>/{{ totalQuestions }}</span>
            <el-progress
              :percentage="progressPercent"
              :stroke-width="5"
              :show-text="false"
              class="progress-mini-bar"
              color="#2563eb"
            />
          </div>

          <!-- 计时器胶囊 -->
          <div class="timer-badge">
            <el-icon class="timer-icon"><Timer /></el-icon>
            <span class="timer-time">{{ formattedElapsed }}</span>
          </div>

          <!-- 云端草稿状态 -->
          <div class="draft-indicator" :class="{ 'is-saving': draftSaving }" :title="draftSaving ? '草稿保存中...' : '草稿已暂存'">
            <span class="status-dot"></span>
            <span class="status-text">{{ draftSaving ? '保存中' : '已暂存' }}</span>
          </div>

          <!-- 竖向微细分隔线 -->
          <div class="control-divider"></div>

          <!-- 字号调节 -->
          <el-tooltip content="字号调节" placement="bottom">
            <el-dropdown trigger="click" @command="handleFontSizeChange">
              <el-button size="small" circle class="tool-btn">
                <span class="font-scale-icon">A</span>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="small" :class="{ 'is-active': fontSize === 'small' }">标准字号</el-dropdown-item>
                  <el-dropdown-item command="medium" :class="{ 'is-active': fontSize === 'medium' }">适中字号</el-dropdown-item>
                  <el-dropdown-item command="large" :class="{ 'is-active': fontSize === 'large' }">特大字号</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </el-tooltip>

          <!-- 全屏切换 -->
          <el-tooltip :content="isFullscreen ? '退出全屏' : '全屏专注作答'" placement="bottom">
            <el-button size="small" circle class="tool-btn" @click="toggleFullscreen">
              <el-icon><FullScreen v-if="!isFullscreen" /><Aim v-else /></el-icon>
            </el-button>
          </el-tooltip>

          <!-- 快速定位未做题 -->
          <el-button
            v-if="unansweredIndices.length > 0"
            size="small"
            plain
            type="warning"
            class="locate-btn"
            @click="scrollToFirstUnanswered"
          >
            定位首道未答
          </el-button>

          <!-- 提交答卷 -->
          <el-button
            type="primary"
            size="default"
            class="submit-paper-btn"
            :loading="submitting"
            @click="handleSubmit"
          >
            <el-icon class="mr-1"><Checked /></el-icon>
            交卷并智能评阅
          </el-button>
        </div>
      </div>
    </header>

    <!-- 主作答双栏区域 -->
    <main v-if="paper" class="take-main-container">
      <!-- 左侧：题目列表 -->
      <div class="questions-stream">
        <div
          v-for="(item, idx) in paper.questions || []"
          :key="item.questionId"
          :id="`question-anchor-${item.questionId}`"
          class="question-card"
          :class="{
            'is-answered': isQuestionAnswered(item.questionId, item.question?.type),
            'is-marked': reviewedQuestions.has(item.questionId)
          }"
        >
          <!-- 题目头部栏 -->
          <div class="card-head">
            <div class="head-left">
              <span class="q-seq">第 {{ idx + 1 }} 题</span>
              <el-tag size="small" :type="getTypeTagType(item.question?.type)" effect="light" class="type-pill">
                {{ formatTypeLabel(item.question?.type) }}
              </el-tag>
              <span class="q-score-badge">{{ item.score || 5 }} 分</span>
            </div>

            <div class="head-right">
              <!-- 疑难标记按钮 -->
              <el-button
                size="small"
                text
                class="mark-btn"
                :class="{ 'is-active': reviewedQuestions.has(item.questionId) }"
                @click="toggleReviewMark(item.questionId)"
              >
                <el-icon class="mr-1">
                  <Flag v-if="reviewedQuestions.has(item.questionId)" />
                  <Star v-else />
                </el-icon>
                {{ reviewedQuestions.has(item.questionId) ? '已标记疑难' : '标记疑难' }}
              </el-button>
            </div>
          </div>

          <!-- 题干区域 -->
          <div class="q-stem-body">
            <MathText class="q-stem-math" :text="item.question?.stem" />
          </div>

          <!-- 选项/作答交互区 -->
          <div class="q-action-area">
            <!-- 单项选择题 / 判断题 -->
            <div
              v-if="item.question?.type === 'SINGLE_CHOICE' || item.question?.type === 'TRUE_FALSE'"
              class="choice-options-grid"
            >
              <div
                v-for="opt in getParsedOptions(item.question?.options)"
                :key="opt.key"
                class="option-card"
                :class="{ 'is-selected': answers[item.questionId] === opt.key }"
                @click="selectSingleAnswer(item.questionId, opt.key)"
              >
                <div class="option-key-badge">{{ opt.key }}</div>
                <div class="option-content">
                  <MathText :text="opt.content" />
                </div>
                <div class="option-check-circle">
                  <el-icon v-if="answers[item.questionId] === opt.key"><Check /></el-icon>
                </div>
              </div>
            </div>

            <!-- 多项选择题 -->
            <div
              v-else-if="item.question?.type === 'MULTIPLE_CHOICE'"
              class="choice-options-grid is-multiple"
            >
              <div
                v-for="opt in getParsedOptions(item.question?.options)"
                :key="opt.key"
                class="option-card is-multi"
                :class="{ 'is-selected': (multiAnswers[item.questionId] || []).includes(opt.key) }"
                @click="toggleMultipleAnswer(item.questionId, opt.key)"
              >
                <div class="option-key-badge">{{ opt.key }}</div>
                <div class="option-content">
                  <MathText :text="opt.content" />
                </div>
                <div class="option-checkbox-box">
                  <el-icon v-if="(multiAnswers[item.questionId] || []).includes(opt.key)"><Check /></el-icon>
                </div>
              </div>
            </div>

            <!-- 填空题 -->
            <div v-else-if="item.question?.type === 'FILL_BLANK'" class="fill-blank-area">
              <el-input
                v-model="answers[item.questionId]"
                placeholder="请输入你的解答内容（如为数学公式可输入数值或标准表达式）"
                clearable
                size="large"
                class="blank-input"
                @input="handleAnswerChange"
              >
                <template #prefix>
                  <el-icon><EditPen /></el-icon>
                </template>
              </el-input>
            </div>

            <!-- 简答题 / 计算解答题 -->
            <div v-else-if="item.question?.type === 'SHORT_ANSWER'" class="short-answer-area">
              <el-input
                v-model="answers[item.questionId]"
                type="textarea"
                :rows="6"
                placeholder="请详细书写你的解题步骤、推导过程与最终结论..."
                maxlength="2000"
                show-word-limit
                class="essay-textarea"
                @input="handleAnswerChange"
              />
            </div>

            <!-- 其他未知类型兜底 -->
            <div v-else class="generic-answer-area">
              <el-input
                v-model="answers[item.questionId]"
                placeholder="请输入作答内容"
                size="large"
                @input="handleAnswerChange"
              />
            </div>
          </div>

          <!-- 底部快捷操作条 -->
          <div class="card-footer">
            <span class="status-summary">
              <span v-if="isQuestionAnswered(item.questionId, item.question?.type)" class="text-answered">
                <el-icon><Check /></el-icon> 该题已完成作答
              </span>
              <span v-else class="text-unanswered">
                <el-icon><InfoFilled /></el-icon> 尚未作答
              </span>
            </span>
            <div class="footer-actions">
              <el-button
                v-if="idx > 0"
                size="small"
                plain
                @click="paper?.questions?.[idx - 1] && scrollToQuestion(paper.questions[idx - 1].questionId)"
              >
                上一题
              </el-button>
              <el-button
                v-if="paper?.questions && idx < (paper.questions.length - 1)"
                size="small"
                type="primary"
                plain
                @click="paper?.questions?.[idx + 1] && scrollToQuestion(paper.questions[idx + 1].questionId)"
              >
                下一题
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：浮动吸顶答题卡 (Floating Sticky Question Sheet Navigator) -->
      <aside class="sheet-sidebar">
        <div class="navigator-card">
          <div class="nav-card-head">
            <h3 class="nav-title">
              <el-icon class="mr-1 text-primary"><DocumentChecked /></el-icon>
              答题卡导航
            </h3>
            <span class="nav-stats-ratio">
              {{ answeredCount }} / {{ totalQuestions }}
            </span>
          </div>

          <!-- 状态图例 -->
          <div class="nav-legend">
            <div class="legend-item">
              <span class="legend-dot dot-answered"></span>
              <span>已答 ({{ answeredCount }})</span>
            </div>
            <div class="legend-item">
              <span class="legend-dot dot-unanswered"></span>
              <span>未答 ({{ totalQuestions - answeredCount }})</span>
            </div>
            <div class="legend-item">
              <span class="legend-dot dot-marked"></span>
              <span>疑难 ({{ reviewedQuestions.size }})</span>
            </div>
          </div>

          <el-divider class="my-2" />

          <!-- 题号网格 -->
          <div class="question-nav-grid">
            <button
              v-for="(item, qIdx) in paper.questions || []"
              :key="item.questionId"
              type="button"
              class="nav-grid-btn"
              :class="{
                'is-answered': isQuestionAnswered(item.questionId, item.question?.type),
                'is-marked': reviewedQuestions.has(item.questionId)
              }"
              :title="`第 ${qIdx + 1} 题 - ${formatTypeLabel(item.question?.type)}`"
              @click="scrollToQuestion(item.questionId)"
            >
              <span class="nav-num">{{ qIdx + 1 }}</span>
              <span v-if="reviewedQuestions.has(item.questionId)" class="mark-star">★</span>
            </button>
          </div>

          <el-divider class="my-3" />

          <!-- 底部提示与提交入口 -->
          <div class="nav-card-bottom">
            <div class="exam-guide-tip">
              <el-icon class="mr-1 text-info"><Warning /></el-icon>
              作答过程由 AI 云端实时防丢保护。
            </div>
            <el-button
              type="primary"
              size="large"
              class="nav-submit-btn"
              :loading="submitting"
              @click="handleSubmit"
            >
              确认交卷
            </el-button>
          </div>
        </div>
      </aside>
    </main>

    <!-- AI 智能评阅沉浸式动效弹窗 (含实时计时、四阶段诊断流转、中止响应) -->
    <AssignmentAIGradingModal
      :visible="aiGradingModalVisible"
      :finished="aiGradingFinished"
      @abort="handleAbortGrading"
    />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  ArrowLeft,
  Trophy,
  Calendar,
  Timer,
  FullScreen,
  Aim,
  Checked,
  Flag,
  Star,
  Check,
  EditPen,
  InfoFilled,
  DocumentChecked,
  Warning
} from '@element-plus/icons-vue';
import MathText from '@/components/common/MathText.vue';
import AssignmentAIGradingModal from '@/components/learning/AssignmentAIGradingModal.vue';
import { useStudentAssignments } from '@/composables/learning/useStudentAssignments';

const route = useRoute();
const router = useRouter();
const assignmentId = Number(route.params.id);

const { loading, paper, loadPaper, submitPaper } = useStudentAssignments();
const answers = reactive<Record<number, string>>({});
const multiAnswers = reactive<Record<number, string[]>>({});
const reviewedQuestions = ref<Set<number>>(new Set());
const submitting = ref(false);
const draftSaving = ref(false);
const fontSize = ref<'small' | 'medium' | 'large'>('medium');
const isFullscreen = ref(false);

// 答题计时器 (正向计时)
const elapsedSeconds = ref(0);
let timerInterval: ReturnType<typeof setInterval> | null = null;

const formattedElapsed = computed(() => {
  const m = Math.floor(elapsedSeconds.value / 60);
  const s = elapsedSeconds.value % 60;
  const mm = String(m).padStart(2, '0');
  const ss = String(s).padStart(2, '0');
  return `${mm}:${ss}`;
});

// 总题数
const totalQuestions = computed(() => paper.value?.questions?.length || 0);

// 已作答题数计算
const answeredCount = computed(() => {
  if (!paper.value?.questions) return 0;
  let count = 0;
  for (const item of paper.value.questions) {
    if (isQuestionAnswered(item.questionId, item.question?.type)) {
      count++;
    }
  }
  return count;
});

// 进度百分比
const progressPercent = computed(() => {
  if (!totalQuestions.value) return 0;
  return Math.min(100, Math.round((answeredCount.value / totalQuestions.value) * 100));
});

// 未作答题目序号集合
const unansweredIndices = computed(() => {
  if (!paper.value?.questions) return [];
  const indices: number[] = [];
  paper.value.questions.forEach((item, idx) => {
    if (!isQuestionAnswered(item.questionId, item.question?.type)) {
      indices.push(idx);
    }
  });
  return indices;
});

// 判断某题是否作答
function isQuestionAnswered(questionId: number, type?: string): boolean {
  if (type === 'MULTIPLE_CHOICE') {
    return Array.isArray(multiAnswers[questionId]) && multiAnswers[questionId].length > 0;
  }
  return String(answers[questionId] || '').trim().length > 0;
}

// 标签映射
function formatTypeLabel(type?: string): string {
  switch (type) {
    case 'SINGLE_CHOICE':
      return '单项选择题';
    case 'MULTIPLE_CHOICE':
      return '多项选择题';
    case 'TRUE_FALSE':
      return '判断题';
    case 'FILL_BLANK':
      return '填空题';
    case 'SHORT_ANSWER':
      return '综合解答题';
    default:
      return '试题';
  }
}

function getTypeTagType(type?: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  switch (type) {
    case 'SINGLE_CHOICE':
      return '';
    case 'MULTIPLE_CHOICE':
      return 'warning';
    case 'TRUE_FALSE':
      return 'success';
    case 'FILL_BLANK':
      return 'info';
    case 'SHORT_ANSWER':
      return 'danger';
    default:
      return 'info';
  }
}

function formatDeadlineText(dl?: string): string {
  if (!dl) return '无限制';
  return dl.replace('T', ' ').slice(0, 16);
}

function formatShortDeadline(dl?: string): string {
  if (!dl) return '无限制';
  const full = dl.replace('T', ' ').slice(0, 16);
  const currentYear = new Date().getFullYear().toString();
  if (full.startsWith(`${currentYear}-`)) {
    return full.slice(5);
  }
  return full;
}

/**
 * 健壮的选项解析器 (Robust Option Parser)
 * 彻底解决 LaTeX 数学公式中的反斜杠导致的 JSON.parse 崩溃并吞没选项的严重问题
 */
function getParsedOptions(raw?: unknown): Array<{ key: string; content: string }> {
  if (!raw) return [];

  // 1. 如果已是数组结构
  if (Array.isArray(raw)) {
    return raw.map((item, idx) => {
      if (typeof item === 'string') {
        return { key: String.fromCharCode(65 + idx), content: item };
      }
      return {
        key: String(item.key || item.label || String.fromCharCode(65 + idx)),
        content: String(item.content ?? item.text ?? item.value ?? '')
      };
    });
  }

  // 2. 如果是键值对对象
  if (typeof raw === 'object') {
    return Object.entries(raw as Record<string, unknown>).map(([k, v]) => ({
      key: k.toUpperCase(),
      content: String(v ?? '')
    }));
  }

  // 3. 如果是字符串
  if (typeof raw === 'string') {
    const trimmed = raw.trim();
    if (!trimmed) return [];

    // 第一重：尝试正常解析
    try {
      const parsed = JSON.parse(trimmed);
      return getParsedOptions(parsed);
    } catch {
      // 第二重：对非法转义的反斜杠自动双转义（尤其是 \sin, \ln, \cos, \lim 等数学公式）
      try {
        const sanitized = trimmed.replace(/\\(?!["\\/bfnrt]|u[0-9a-fA-F]{4})/g, '\\\\');
        const parsed = JSON.parse(sanitized);
        return getParsedOptions(parsed);
      } catch {
        // 第三重：正则智能抽取 key 和 content
        try {
          const keyRegex = /"key"\s*:\s*"([A-Za-z0-9]+)"\s*,\s*"content"\s*:\s*"([^"]*)"/g;
          const extracted: Array<{ key: string; content: string }> = [];
          let match;
          while ((match = keyRegex.exec(trimmed)) !== null) {
            extracted.push({ key: match[1], content: match[2] });
          }
          if (extracted.length > 0) return extracted;
        } catch {
          return [];
        }
      }
    }
  }

  return [];
}

// 单选题选择
function selectSingleAnswer(questionId: number, key: string) {
  answers[questionId] = key;
  handleAnswerChange();
}

// 多选题选择/反选
function toggleMultipleAnswer(questionId: number, key: string) {
  if (!multiAnswers[questionId]) {
    multiAnswers[questionId] = [];
  }
  const current = [...multiAnswers[questionId]];
  const index = current.indexOf(key);
  if (index > -1) {
    current.splice(index, 1);
  } else {
    current.push(key);
    current.sort();
  }
  multiAnswers[questionId] = current;
  handleAnswerChange();
}

// 疑难标记切换
function toggleReviewMark(questionId: number) {
  if (reviewedQuestions.value.has(questionId)) {
    reviewedQuestions.value.delete(questionId);
  } else {
    reviewedQuestions.value.add(questionId);
  }
  saveDraftToStorage();
}

// 平滑滚动定位到题目
function scrollToQuestion(questionId: number) {
  const el = document.getElementById(`question-anchor-${questionId}`);
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    el.classList.add('is-highlight-flash');
    setTimeout(() => {
      el.classList.remove('is-highlight-flash');
    }, 1200);
  }
}

// 定位到第一个未答题
function scrollToFirstUnanswered() {
  if (unansweredIndices.value.length === 0 || !paper.value?.questions) return;
  const firstIdx = unansweredIndices.value[0];
  const targetQ = paper.value.questions[firstIdx];
  if (targetQ) {
    scrollToQuestion(targetQ.questionId);
  }
}

// 字号切换
function handleFontSizeChange(size: 'small' | 'medium' | 'large') {
  fontSize.value = size;
}

// 同步原生全屏状态（支持Esc按键监听、F11监听等）
function syncFullscreenState() {
  isFullscreen.value = Boolean(
    document.fullscreenElement ||
    (document as unknown as { webkitFullscreenElement?: Element }).webkitFullscreenElement ||
    (document as unknown as { mozFullScreenElement?: Element }).mozFullScreenElement ||
    (document as unknown as { msFullscreenElement?: Element }).msFullscreenElement
  );
}

// 全屏模式切换
function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen().then(() => {
      isFullscreen.value = true;
    }).catch(() => {
      ElMessage.warning('当前环境不支持全屏');
    });
  } else {
    if (document.exitFullscreen) {
      document.exitFullscreen().then(() => {
        isFullscreen.value = false;
      }).catch(() => {
        syncFullscreenState();
      });
    }
  }
}

// 本地草稿自动存储机制
let saveTimeout: ReturnType<typeof setTimeout> | null = null;
const STORAGE_KEY = computed(() => `edumind_exam_draft_${assignmentId}`);

function handleAnswerChange() {
  draftSaving.value = true;
  if (saveTimeout) clearTimeout(saveTimeout);
  saveTimeout = setTimeout(() => {
    saveDraftToStorage();
    draftSaving.value = false;
  }, 400);
}

function saveDraftToStorage() {
  try {
    const draftData = {
      answers: { ...answers },
      multiAnswers: { ...multiAnswers },
      reviewedQuestions: Array.from(reviewedQuestions.value),
      elapsedSeconds: elapsedSeconds.value,
      timestamp: Date.now()
    };
    localStorage.setItem(STORAGE_KEY.value, JSON.stringify(draftData));
  } catch (e) {
    console.warn('Failed to save exam draft to storage', e);
  }
}

function restoreDraftFromStorage() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY.value);
    if (!raw) return;
    const draft = JSON.parse(raw);
    if (draft && typeof draft === 'object') {
      if (draft.answers) Object.assign(answers, draft.answers);
      if (draft.multiAnswers) Object.assign(multiAnswers, draft.multiAnswers);
      if (Array.isArray(draft.reviewedQuestions)) {
        reviewedQuestions.value = new Set(draft.reviewedQuestions);
      }
      if (typeof draft.elapsedSeconds === 'number') {
        elapsedSeconds.value = draft.elapsedSeconds;
      }
      ElMessage.info('已为您恢复上次未完成的作答进度草稿');
    }
  } catch (e) {
    console.warn('Failed to restore exam draft from storage', e);
  }
}

function clearDraftFromStorage() {
  try {
    localStorage.removeItem(STORAGE_KEY.value);
  } catch {}
}

// 退出确认
async function handleConfirmExit() {
  try {
    await ElMessageBox.confirm('当前答卷尚未提交，返回后作答进度将保存在本地草稿中。确定离开吗？', '离开确认', {
      type: 'info',
      confirmButtonText: '确定离开',
      cancelButtonText: '继续作答'
    });
    router.back();
  } catch {}
}

// 组装提交负载
function buildPayload() {
  const list: Array<{ questionId: number; answer: string }> = [];
  for (const item of paper.value?.questions || []) {
    const qid = item.questionId;
    let answer = answers[qid] || '';
    if (item.question?.type === 'MULTIPLE_CHOICE') {
      answer = (multiAnswers[qid] || []).sort().join(',');
    }
    list.push({ questionId: qid, answer });
  }
  return list;
}

// AI 智能评阅弹窗状态与中止控制器
const aiGradingModalVisible = ref(false);
const aiGradingFinished = ref(false);
let submitAbortController: AbortController | null = null;

function handleAbortGrading() {
  if (submitAbortController) {
    submitAbortController.abort();
    submitAbortController = null;
  }
  submitting.value = false;
  aiGradingModalVisible.value = false;
  aiGradingFinished.value = false;
  ElMessage.warning('已中止当前智能评阅，作答进度已自动保存在草稿中');
}

// 提交答卷
async function handleSubmit() {
  const unanswered = unansweredIndices.value.length;
  let confirmMessage = '确认提交当前答卷？提交后将进入 AI 智能评阅与学情诊断流程。';
  if (unanswered > 0) {
    confirmMessage = `您还有 ${unanswered} 道题目尚未作答，确定要提前交卷吗？提交后将无法修改作答。`;
  }

  await ElMessageBox.confirm(confirmMessage, '提交确认', {
    type: unanswered > 0 ? 'warning' : 'info',
    confirmButtonText: '确认交卷',
    cancelButtonText: '再检查一下'
  });

  submitting.value = true;
  aiGradingModalVisible.value = true;
  aiGradingFinished.value = false;
  submitAbortController = new AbortController();

  try {
    await submitPaper(assignmentId, buildPayload());
    aiGradingFinished.value = true;
    clearDraftFromStorage();
    ElMessage.success('智能评阅与学情诊断完成！');
    setTimeout(() => {
      aiGradingModalVisible.value = false;
      router.replace(`/learning/assignments/${assignmentId}/result`);
    }, 850);
  } catch (err: unknown) {
    const errorMsg = err instanceof Error ? err.message : String(err || '');
    if (errorMsg.includes('canceled') || errorMsg.includes('AbortError') || (err as { code?: string })?.code === 'ERR_CANCELED') {
      return;
    }
    aiGradingModalVisible.value = false;
    ElMessage.error(err instanceof Error ? err.message : '提交评阅失败，请稍后重试');
  } finally {
    submitting.value = false;
  }
}

onMounted(async () => {
  document.addEventListener('fullscreenchange', syncFullscreenState);
  document.addEventListener('webkitfullscreenchange', syncFullscreenState);
  document.addEventListener('mozfullscreenchange', syncFullscreenState);
  document.addEventListener('MSFullscreenChange', syncFullscreenState);

  const p = await loadPaper(assignmentId);
  if (p?.mySubmissionStatus === 'GRADED' || p?.mySubmissionStatus === 'SUBMITTED') {
    router.replace(`/learning/assignments/${assignmentId}/result`);
    return;
  }

  // 恢复本地草稿
  restoreDraftFromStorage();

  // 启动作答计时器
  timerInterval = setInterval(() => {
    elapsedSeconds.value++;
  }, 1000);
});

onBeforeUnmount(() => {
  document.removeEventListener('fullscreenchange', syncFullscreenState);
  document.removeEventListener('webkitfullscreenchange', syncFullscreenState);
  document.removeEventListener('mozfullscreenchange', syncFullscreenState);
  document.removeEventListener('MSFullscreenChange', syncFullscreenState);

  if (timerInterval) clearInterval(timerInterval);
  if (saveTimeout) clearTimeout(saveTimeout);
});
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.assignment-take-page {
  position: relative;
  min-height: 100vh;
  background: #f8fafc;
  padding-bottom: 60px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
  color: #1e293b;

  // 字号系统
  &.font-size-small {
    .q-stem-math { font-size: 15px; }
    .option-content { font-size: 14px; }
  }
  &.font-size-medium {
    .q-stem-math { font-size: 16px; }
    .option-content { font-size: 15px; }
  }
  &.font-size-large {
    .q-stem-math { font-size: 18px; }
    .option-content { font-size: 17px; }
  }

  &.is-fullscreen {
    padding: 0 0 40px;
    background: #f1f5f9;
  }
}

// 顶部吸顶条 (真正贴顶、无任何上方空白)
.take-sticky-header {
  position: sticky;
  top: 0;
  z-index: 100;
  width: 100%;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid #e2e8f0;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
  transition: all 0.25s ease;

  .header-inner {
    width: 100%;
    max-width: 100%;
    box-sizing: border-box;
    padding: 10px 24px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
  }

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
    flex: 1 1 auto;
    min-width: 0;

    .btn-back {
      font-weight: 500;
      border-radius: 9999px;
      flex-shrink: 0;
    }

    .exam-title-block {
      min-width: 0;
      flex: 1 1 auto;
      display: flex;
      flex-direction: column;
      gap: 3px;

      .title-row {
        display: flex;
        align-items: center;
        gap: 8px;
        min-width: 0;

        .exam-title {
          margin: 0;
          font-size: 15px;
          font-weight: 600;
          color: #0f172a;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .exam-tag {
          font-size: 11px;
          border-radius: 4px;
          flex-shrink: 0;
        }
      }

      .exam-sub-meta {
        display: flex;
        align-items: center;
        gap: 12px;
        font-size: 12px;
        color: #64748b;
        white-space: nowrap;
        flex-shrink: 0;

        .meta-item {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          white-space: nowrap;
          flex-shrink: 0;
        }
      }
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-shrink: 0;

    .progress-pill {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 4px 10px;
      background: #f1f5f9;
      border-radius: 9999px;
      border: 1px solid #e2e8f0;
      font-size: 12px;
      color: #475569;
      white-space: nowrap;

      .progress-label {
        color: #64748b;
        font-size: 11px;
      }

      .progress-ratio strong {
        color: #2563eb;
        font-size: 13px;
      }

      .progress-mini-bar {
        width: 48px;
        margin: 0;
      }
    }

    .timer-badge {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      padding: 5px 12px;
      background: #f1f5f9;
      border-radius: 9999px;
      border: 1px solid #e2e8f0;
      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
      font-size: 13px;
      font-weight: 600;
      color: #1e293b;
      white-space: nowrap;

      .timer-icon {
        color: #2563eb;
      }
    }

    .draft-indicator {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      padding: 4px 8px;
      font-size: 12px;
      color: #64748b;
      white-space: nowrap;

      .status-dot {
        width: 7px;
        height: 7px;
        border-radius: 50%;
        background: #10b981;
        transition: background 0.3s;
      }

      &.is-saving .status-dot {
        background: #f59e0b;
        animation: pulse-dot 1s infinite alternate;
      }
    }

    .control-divider {
      width: 1px;
      height: 18px;
      background: #e2e8f0;
      margin: 0 2px;
    }

    .tool-btn {
      color: #475569;
      border-color: #cbd5e1;

      &:hover {
        color: #2563eb;
        border-color: #93c5fd;
        background: #eff6ff;
      }

      .font-scale-icon {
        font-weight: 700;
        font-size: 13px;
      }
    }

    .locate-btn {
      font-weight: 500;
      border-radius: 9999px;
    }

    .submit-paper-btn {
      font-weight: 600;
      border-radius: 9999px;
      padding: 0 16px;
      height: 34px;
      background: linear-gradient(135deg, #1677ff 0%, #3b82f6 100%) !important;
      color: #ffffff !important;
      border: none !important;
      box-shadow: 0 4px 14px rgba(22, 119, 255, 0.28);
      transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);

      &:hover,
      &:focus {
        background: linear-gradient(135deg, #0958d9 0%, #2563eb 100%) !important;
        color: #ffffff !important;
        border-color: transparent !important;
        transform: translateY(-1px);
        box-shadow: 0 6px 18px rgba(22, 119, 255, 0.38);
      }

      &:active {
        transform: translateY(0);
        background: #0958d9 !important;
      }
    }
  }
}

// 主双栏内容区
.take-main-container {
  max-width: 1440px;
  margin: 20px auto 0;
  padding: 0 24px 60px;
  display: flex;
  gap: 24px;
  align-items: flex-start;

  // 左侧题目流
  .questions-stream {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  // 题目卡片
  .question-card {
    background: #ffffff;
    border-radius: 14px;
    border: 1px solid #e2e8f0;
    padding: 24px;
    box-shadow: 0 2px 6px rgba(15, 23, 42, 0.03);
    transition: all 0.25s ease;
    scroll-margin-top: 80px;

    &:hover {
      border-color: #cbd5e1;
      box-shadow: 0 4px 14px rgba(15, 23, 42, 0.06);
    }

    &.is-answered {
      border-left: 4px solid #2563eb;
    }

    &.is-marked {
      border-right: 4px solid #f59e0b;
      background: linear-gradient(to right, #ffffff, #fffdf8);
    }

    &.is-highlight-flash {
      animation: highlight-pulse 1.2s ease-in-out;
    }

    .card-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 16px;
      padding-bottom: 12px;
      border-bottom: 1px dashed #f1f5f9;

      .head-left {
        display: flex;
        align-items: center;
        gap: 10px;

        .q-seq {
          font-size: 16px;
          font-weight: 700;
          color: #0f172a;
        }

        .type-pill {
          font-weight: 500;
          border-radius: 6px;
        }

        .q-score-badge {
          font-size: 13px;
          color: #64748b;
          background: #f8fafc;
          padding: 2px 8px;
          border-radius: 4px;
          border: 1px solid #e2e8f0;
        }
      }

      .head-right {
        .mark-btn {
          color: #64748b;
          border-radius: 6px;

          &:hover {
            color: #d97706;
            background: #fef3c7;
          }

          &.is-active {
            color: #b45309;
            background: #fde68a;
            font-weight: 600;
          }
        }
      }
    }

    .q-stem-body {
      margin-bottom: 20px;
      line-height: 1.7;
      color: #1e293b;

      .q-stem-math {
        display: block;
        word-break: break-word;
      }
    }

    // 现代化选择题选项网格
    .choice-options-grid {
      display: flex;
      flex-direction: column;
      gap: 12px;
      margin-bottom: 20px;

      .option-card {
        display: flex;
        align-items: center;
        gap: 14px;
        padding: 14px 18px;
        background: #f8fafc;
        border: 1.5px solid #e2e8f0;
        border-radius: 12px;
        cursor: pointer;
        transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
        user-select: none;

        &:hover {
          background: #ffffff;
          border-color: #93c5fd;
          transform: translateY(-1px);
          box-shadow: 0 4px 12px rgba(37, 99, 235, 0.06);

          .option-key-badge {
            background: #dbeafe;
            color: #1d4ed8;
          }
        }

        &.is-selected {
          background: #eff6ff;
          border-color: #2563eb;
          box-shadow: 0 4px 14px rgba(37, 99, 235, 0.12);

          .option-key-badge {
            background: #2563eb;
            color: #ffffff;
            box-shadow: 0 2px 6px rgba(37, 99, 235, 0.3);
          }

          .option-content {
            color: #1e3a8a;
            font-weight: 500;
          }

          .option-check-circle {
            background: #2563eb;
            border-color: #2563eb;
            color: #ffffff;
          }

          .option-checkbox-box {
            background: #2563eb;
            border-color: #2563eb;
            color: #ffffff;
          }
        }

        .option-key-badge {
          width: 32px;
          height: 32px;
          border-radius: 50%;
          background: #e2e8f0;
          color: #475569;
          font-weight: 700;
          font-size: 14px;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
          transition: all 0.2s ease;
        }

        .option-content {
          flex: 1;
          min-width: 0;
          line-height: 1.6;
          color: #334155;
          word-break: break-word;
        }

        .option-check-circle {
          width: 20px;
          height: 20px;
          border-radius: 50%;
          border: 1.5px solid #cbd5e1;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
          font-size: 12px;
          transition: all 0.2s ease;
        }

        .option-checkbox-box {
          width: 20px;
          height: 20px;
          border-radius: 6px;
          border: 1.5px solid #cbd5e1;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
          font-size: 12px;
          transition: all 0.2s ease;
        }
      }
    }

    // 填空与简答输入
    .fill-blank-area {
      margin-bottom: 20px;

      .blank-input {
        --el-input-border-radius: 10px;
      }
    }

    .short-answer-area {
      margin-bottom: 20px;

      .essay-textarea {
        --el-input-border-radius: 10px;
        font-family: inherit;
        line-height: 1.6;
      }
    }

    // 题目底部状态
    .card-footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding-top: 14px;
      border-top: 1px solid #f1f5f9;
      font-size: 13px;

      .status-summary {
        .text-answered {
          color: #10b981;
          display: inline-flex;
          align-items: center;
          gap: 4px;
          font-weight: 500;
        }

        .text-unanswered {
          color: #94a3b8;
          display: inline-flex;
          align-items: center;
          gap: 4px;
        }
      }

      .footer-actions {
        display: flex;
        gap: 8px;
      }
    }
  }

  // 右侧吸顶答题卡
  .sheet-sidebar {
    width: 300px;
    flex-shrink: 0;
    position: sticky;
    top: 80px;
    max-height: calc(100vh - 100px);
    overflow-y: auto;

    .navigator-card {
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 14px;
      padding: 20px;
      box-shadow: 0 4px 16px rgba(15, 23, 42, 0.05);

      .nav-card-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 12px;

        .nav-title {
          margin: 0;
          font-size: 15px;
          font-weight: 700;
          color: #0f172a;
          display: flex;
          align-items: center;
        }

        .nav-stats-ratio {
          font-size: 13px;
          font-weight: 600;
          color: #2563eb;
          background: #eff6ff;
          padding: 2px 8px;
          border-radius: 12px;
        }
      }

      .nav-legend {
        display: flex;
        align-items: center;
        justify-content: space-between;
        font-size: 12px;
        color: #64748b;
        margin-bottom: 8px;

        .legend-item {
          display: flex;
          align-items: center;
          gap: 5px;

          .legend-dot {
            width: 9px;
            height: 9px;
            border-radius: 3px;

            &.dot-answered {
              background: #2563eb;
            }
            &.dot-unanswered {
              background: #f1f5f9;
              border: 1px solid #cbd5e1;
            }
            &.dot-marked {
              background: #f59e0b;
            }
          }
        }
      }

      // 题号网格按钮
      .question-nav-grid {
        display: grid;
        grid-template-columns: repeat(5, 1fr);
        gap: 8px;
        margin: 12px 0;

        .nav-grid-btn {
          position: relative;
          aspect-ratio: 1;
          display: flex;
          align-items: center;
          justify-content: center;
          background: #f8fafc;
          border: 1px solid #e2e8f0;
          border-radius: 8px;
          font-size: 13px;
          font-weight: 600;
          color: #475569;
          cursor: pointer;
          transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

          &:hover {
            border-color: #93c5fd;
            color: #2563eb;
            transform: scale(1.05);
          }

          &.is-answered {
            background: #2563eb;
            border-color: #2563eb;
            color: #ffffff;
            box-shadow: 0 2px 6px rgba(37, 99, 235, 0.25);
          }

          &.is-marked {
            border-color: #f59e0b;
            box-shadow: 0 0 0 1px #f59e0b;

            .mark-star {
              position: absolute;
              top: 1px;
              right: 2px;
              font-size: 9px;
              color: #f59e0b;
            }

            &.is-answered .mark-star {
              color: #ffffff;
            }
          }
        }
      }

      .nav-card-bottom {
        display: flex;
        flex-direction: column;
        gap: 12px;

        .exam-guide-tip {
          font-size: 12px;
          color: #64748b;
          display: flex;
          align-items: center;
          background: #f8fafc;
          padding: 8px 10px;
          border-radius: 6px;
        }

        .nav-submit-btn {
          width: 100%;
          height: 40px;
          font-weight: 600;
          border-radius: 9999px;
          background: linear-gradient(135deg, #1677ff 0%, #3b82f6 100%) !important;
          color: #ffffff !important;
          border: none !important;
          box-shadow: 0 4px 14px rgba(22, 119, 255, 0.28);
          transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);

          &:hover,
          &:focus {
            background: linear-gradient(135deg, #0958d9 0%, #2563eb 100%) !important;
            color: #ffffff !important;
            border-color: transparent !important;
            transform: translateY(-1px);
            box-shadow: 0 6px 18px rgba(22, 119, 255, 0.38);
          }

          &:active {
            transform: translateY(0);
            background: #0958d9 !important;
          }
        }
      }
    }
  }
}

// 动画
@keyframes pulse-dot {
  from { opacity: 0.4; }
  to { opacity: 1; }
}

@keyframes highlight-pulse {
  0% { box-shadow: 0 0 0 0 rgba(37, 99, 235, 0.6); }
  50% { box-shadow: 0 0 0 10px rgba(37, 99, 235, 0); }
  100% { box-shadow: 0 0 0 0 rgba(37, 99, 235, 0); }
}

@media (max-width: 1024px) {
  .take-main-container {
    flex-direction: column;

    .sheet-sidebar {
      width: 100%;
      position: static;
      max-height: none;
    }
  }

  .take-sticky-header .header-inner {
    flex-wrap: wrap;
  }
}
</style>
