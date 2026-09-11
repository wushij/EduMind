<template>
  <div class="exam-create-container">
    <!-- 顶部导航栏 -->
    <div class="top-nav-bar">
      <el-button :icon="ArrowLeft" link class="back-link" @click="handleCancel">
        返回试卷列表
      </el-button>
      <div class="step-progress-wrapper">
        <el-steps :active="currentStep" finish-status="success" simple>
          <el-step title="1. 试卷基本信息" :icon="Document" />
          <el-step title="2. 编排大题与选题" :icon="Setting" />
          <el-step title="3. 卷面审阅与发布" :icon="Finished" />
        </el-steps>
      </div>
    </div>

    <!-- 步骤一：试卷基本信息 -->
    <div v-show="currentStep === 0" class="step-content-box">
      <el-card shadow="never" class="form-card">
        <template #header>
          <div class="card-header-title">
            <span class="icon">📝</span>
            <div>
              <h3>第一步：设置试卷基本规范</h3>
              <p>请填写本次测验或期末考试的名称、所属课程与考场基准时间规则。</p>
            </div>
          </div>
        </template>

        <el-form
          ref="step1FormRef"
          :model="examForm"
          :rules="step1Rules"
          label-position="top"
          class="step-form-grid"
        >
          <el-row :gutter="24">
            <el-col :span="16">
              <el-form-item label="试卷名称 / 标题" prop="title">
                <el-input
                  v-model="examForm.title"
                  placeholder="例如：2025-2026学年第二学期《数据结构》期末统一考核试卷 (A卷)"
                  size="large"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="考试学期" prop="semester">
                <el-select v-model="examForm.semester" size="large" class="w-full">
                  <el-option label="2025-2026 第二学期" value="2025-2026-2" />
                  <el-option label="2025-2026 第一学期" value="2025-2026-1" />
                  <el-option label="2024-2025 第二学期" value="2024-2025-2" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="8">
              <el-form-item label="所属课程" prop="courseId">
                <el-select
                  v-model="examForm.courseId"
                  placeholder="选择课程"
                  size="large"
                  class="w-full"
                  @change="handleCourseChange"
                >
                  <el-option
                    v-for="c in courses"
                    :key="c.id"
                    :label="c.title"
                    :value="c.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="考试限时（分钟）" prop="durationMinutes">
                <el-input-number
                  v-model="examForm.durationMinutes"
                  :min="10"
                  :max="300"
                  :step="10"
                  size="large"
                  class="w-full"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="及格分线 (及格线)" prop="passScore">
                <el-input-number
                  v-model="examForm.passScore"
                  :min="1"
                  :max="examForm.totalScore || 100"
                  size="large"
                  class="w-full"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="试卷卷首考生须知 / 考试说明">
            <el-input
              v-model="examForm.description"
              type="textarea"
              :rows="3"
              placeholder="例如：本试卷满分100分，答题时间120分钟。请在答题卡规定区域内作答，严禁使用通讯工具及外附存储设备。"
            />
          </el-form-item>
        </el-form>

        <div class="step-footer-bar">
          <div></div>
          <el-button type="primary" size="large" @click="goToStep2">
            下一步：编排大题与选题 ➔
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 步骤二：编排大题与选题 -->
    <div v-show="currentStep === 1" class="step-content-box composition-workspace">
      <!-- 左侧大题面板 -->
      <div class="sections-column">
        <div class="section-actions-bar">
          <div class="bar-title">
            <span class="badge">结构列表</span>
            <span>试卷大题大纲（共 {{ sections.length }} 个大题）</span>
          </div>
          <el-dropdown @command="handleAddSection">
            <el-button type="primary" plain size="small">
              <el-icon class="mr-1"><Plus /></el-icon> 增加大题类型
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="SINGLE_CHOICE">单项选择题</el-dropdown-item>
                <el-dropdown-item command="MULTIPLE_CHOICE">多项选择题</el-dropdown-item>
                <el-dropdown-item command="TRUE_FALSE">判断题</el-dropdown-item>
                <el-dropdown-item command="FILL_BLANK">填空题</el-dropdown-item>
                <el-dropdown-item command="SHORT_ANSWER">简答与问答题</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>

        <div class="sections-stack">
          <div
            v-for="(sec, sIdx) in sections"
            :key="sec.id"
            class="section-block-card"
          >
            <!-- 大题标题与设置行 -->
            <div class="section-card-header">
              <div class="sec-title-area">
                <span class="sec-order-tag">第{{ sIdx + 1 }}大题</span>
                <el-input
                  v-model="sec.title"
                  placeholder="大题名称"
                  class="sec-title-input"
                  size="small"
                />
                <el-tag size="small" :type="getTypeTagType(sec.type)">
                  {{ getTypeLabel(sec.type) }}
                </el-tag>
              </div>

              <div class="sec-meta-area">
                <span class="meta-item">
                  每题默认
                  <el-input-number
                    v-model="sec.defaultScore"
                    :min="1"
                    :max="100"
                    size="small"
                    class="score-input"
                    @change="updateSectionDefaultScore(sec)"
                  />
                  分
                </span>
                <el-button
                  type="primary"
                  link
                  size="small"
                  @click="openQuestionPicker(sec)"
                >
                  ➕ 选题入卷 ({{ sec.questions.length }})
                </el-button>
                <el-popconfirm
                  title="确认删除该大题及其包含的所有试题吗？"
                  @confirm="removeSection(sIdx)"
                >
                  <template #reference>
                    <el-button type="danger" link size="small">删除大题</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </div>

            <!-- 该大题下的试题清单 -->
            <div class="section-questions-list">
              <div
                v-for="(q, qIdx) in sec.questions"
                :key="q.id"
                class="paper-question-row"
              >
                <span class="q-seq">{{ qIdx + 1 }}.</span>
                <div class="q-content">
                  <p class="q-stem">{{ q.stem }}</p>
                  <div class="q-inline-tags">
                    <span class="q-diff-badge" :class="q.difficulty?.toLowerCase()">
                      {{ getDifficultyLabel(q.difficulty) }}
                    </span>
                    <span v-for="kp in q.knowledgePointNames" :key="kp" class="q-kp-badge">
                      {{ kp }}
                    </span>
                  </div>
                </div>

                <div class="q-score-adjust">
                  <el-input-number
                    v-model="q.score"
                    :min="1"
                    :max="50"
                    size="small"
                    class="item-score-input"
                    @change="calculateScores"
                  />
                  <span class="unit">分</span>
                  <el-button
                    type="danger"
                    link
                    size="small"
                    :icon="Delete"
                    @click="removeQuestionFromSection(sec, qIdx)"
                  />
                </div>
              </div>

              <div v-if="sec.questions.length === 0" class="sec-empty-hint">
                暂未选择试题，请点击右上角「➕ 选题入卷」从课程题库中挑选试题。
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧试卷概览看板 (Sticky) -->
      <div class="summary-column">
        <el-card shadow="never" class="summary-sticky-card">
          <div class="summary-card-title">
            <span>试卷实时指标</span>
            <el-tag type="success" size="small">动态核算</el-tag>
          </div>

          <div class="score-display-box">
            <span class="score-number">{{ currentTotalScore }}</span>
            <span class="score-unit">/ {{ examForm.totalScore }} 分</span>
          </div>

          <div class="progress-bar-wrap">
            <el-progress
              :percentage="Math.min(100, Math.round((currentTotalScore / examForm.totalScore) * 100))"
              :color="currentTotalScore === examForm.totalScore ? '#10b981' : '#3b82f6'"
            />
            <span class="progress-tip">
              {{ currentTotalScore === examForm.totalScore ? '✅ 已达到预期满分' : `还差 ${examForm.totalScore - currentTotalScore} 分达到试卷满分` }}
            </span>
          </div>

          <div class="summary-stats-list">
            <div class="stat-row">
              <span class="label">已选大题数</span>
              <span class="val">{{ sections.length }} 个</span>
            </div>
            <div class="stat-row">
              <span class="label">已录入总题数</span>
              <span class="val">{{ currentTotalQuestions }} 道</span>
            </div>
            <div class="stat-row">
              <span class="label">预估考试时长</span>
              <span class="val">{{ examForm.durationMinutes }} 分钟</span>
            </div>
            <div class="stat-row">
              <span class="label">及格合格线</span>
              <span class="val">{{ examForm.passScore }} 分</span>
            </div>
          </div>

          <!-- 题型分布占比 -->
          <div class="distribution-section">
            <span class="dist-title">题型分值分布</span>
            <div class="dist-bars">
              <div
                v-for="sec in sections"
                :key="sec.id"
                class="dist-item"
              >
                <div class="dist-label">
                  <span>{{ sec.title }} ({{ sec.questions.length }}题)</span>
                  <span>{{ getSectionScore(sec) }} 分</span>
                </div>
                <div class="dist-mini-bar">
                  <div
                    class="fill"
                    :style="{ width: currentTotalScore ? `${(getSectionScore(sec) / currentTotalScore) * 100}%` : '0%' }"
                  />
                </div>
              </div>
            </div>
          </div>

          <div class="summary-actions">
            <el-button class="w-full mb-2" @click="currentStep = 0">
              上一步：修改基本信息
            </el-button>
            <el-button type="primary" class="w-full" size="large" @click="goToStep3">
              下一步：审阅与发布 ➔
            </el-button>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 步骤三：审阅与发布 -->
    <div v-show="currentStep === 2" class="step-content-box">
      <div class="review-paper-wrapper">
        <!-- 仿真实考试纸质卷面抬头 -->
        <div class="paper-sheet">
          <div class="paper-header-section">
            <div class="school-mark">EduMind 智教云 · 教学与考核标准化测试卷</div>
            <h2 class="paper-main-title">{{ examForm.title }}</h2>
            <div class="paper-sub-meta">
              <span>课程：{{ getCourseName(examForm.courseId) }}</span>
              <span>学期：{{ examForm.semester }}</span>
              <span>考试时限：{{ examForm.durationMinutes }} 分钟</span>
              <span>试卷总分：{{ currentTotalScore }} 分</span>
            </div>

            <!-- 考生装订信息栏 -->
            <div class="student-meta-seal-bar">
              <div class="seal-item">班级：__________________</div>
              <div class="seal-item">学号：__________________</div>
              <div class="seal-item">姓名：__________________</div>
              <div class="seal-item">考场座位：__________</div>
            </div>

            <div class="paper-notice-box">
              <strong>考生须知：</strong>{{ examForm.description || '请认真审题，在规定时间内独立作答，字迹清晰规范。' }}
            </div>
          </div>

          <!-- 各大题渲染 -->
          <div class="paper-sections-body">
            <div
              v-for="(sec, sIdx) in sections"
              :key="sec.id"
              class="paper-section-block"
            >
              <h3 class="paper-sec-heading">
                {{ getChineseNumber(sIdx + 1) }}、{{ sec.title }}
                <span class="sec-score-sum">（本大题共 {{ sec.questions.length }} 小题，共 {{ getSectionScore(sec) }} 分）</span>
              </h3>

              <div class="paper-questions-flow">
                <div
                  v-for="(q, qIdx) in sec.questions"
                  :key="q.id"
                  class="paper-question-unit"
                >
                  <div class="unit-stem">
                    <span class="unit-index">{{ qIdx + 1 }}.</span>
                    <span class="unit-text">{{ q.stem }}</span>
                    <span class="unit-score">（{{ q.score }}分）</span>
                  </div>

                  <!-- 选项展示 -->
                  <div v-if="q.options && q.options.length > 0" class="unit-options-grid">
                    <div
                      v-for="opt in q.options"
                      :key="opt.key"
                      class="unit-option-item"
                    >
                      <span class="key">{{ opt.key }}.</span>
                      <span class="content">{{ opt.content }}</span>
                    </div>
                  </div>

                  <!-- 主观/问答题留白 -->
                  <div
                    v-if="q.type === 'SHORT_ANSWER' || q.type === 'FILL_BLANK'"
                    class="paper-answer-blank"
                  >
                    <div class="blank-lines">
                      <div class="line"></div>
                      <div class="line"></div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 审阅底部发布操作条 -->
        <div class="review-bottom-dock">
          <div class="dock-left">
            <el-radio-group v-model="publishStatus">
              <el-radio-button label="DRAFT">暂存为草稿 (后续可继续修改)</el-radio-button>
              <el-radio-button label="PUBLISHED">正式发布入库 (立即可用于测验)</el-radio-button>
            </el-radio-group>
          </div>
          <div class="dock-right">
            <el-button size="large" @click="currentStep = 1">返回选题编排</el-button>
            <el-button
              type="primary"
              size="large"
              :loading="saving"
              class="submit-publish-btn"
              @click="handleSaveExam"
            >
              {{ publishStatus === 'PUBLISHED' ? '🚀 确认并正式发布试卷' : '💾 保存试卷草稿' }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 弹窗：从题库中挑选试题入卷 -->
    <el-dialog
      v-model="pickerVisible"
      :title="`为【${activeSection?.title || '大题'}】挑选入卷试题`"
      width="780px"
      destroy-on-close
    >
      <div class="picker-modal-inner">
        <div class="picker-filter-row">
          <el-input
            v-model="pickerKeyword"
            placeholder="搜索试题关键词、知识点..."
            clearable
            :prefix-icon="Search"
            class="flex-1"
          />
          <el-select v-model="pickerDifficulty" placeholder="难度" clearable style="width: 120px">
            <el-option label="全部难度" value="" />
            <el-option label="简单" value="EASY" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="困难" value="HARD" />
          </el-select>
        </div>

        <div class="picker-list-area">
          <div
            v-for="q in filteredCandidateQuestions"
            :key="q.id"
            class="picker-question-item"
            :class="{ selected: selectedPickerIds.includes(q.id) }"
            @click="togglePickerItem(q.id)"
          >
            <el-checkbox
              :model-value="selectedPickerIds.includes(q.id)"
              @click.stop
              @change="togglePickerItem(q.id)"
            />
            <div class="picker-item-main">
              <div class="badge-line">
                <el-tag size="small" :type="getTypeTagType(q.type)">{{ getTypeLabel(q.type) }}</el-tag>
                <el-tag size="small" :type="getDifficultyTagType(q.difficulty)">{{ getDifficultyLabel(q.difficulty) }}</el-tag>
                <span class="picker-score">建议 {{ q.score || 5 }} 分</span>
              </div>
              <p class="picker-stem">{{ q.stem }}</p>
            </div>
          </div>

          <div v-if="filteredCandidateQuestions.length === 0" class="picker-empty">
            没有符合条件的该题型备选试题。
          </div>
        </div>
      </div>

      <template #footer>
        <div class="picker-footer">
          <span>已选中 <strong>{{ selectedPickerIds.length }}</strong> 道试题</span>
          <div>
            <el-button @click="pickerVisible = false">取消</el-button>
            <el-button
              type="primary"
              :disabled="selectedPickerIds.length === 0"
              @click="confirmAddPickedQuestions"
            >
              添加选中的题目到本大题
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import {
  ArrowLeft,
  Document,
  Setting,
  Finished,
  Plus,
  Delete,
  Search
} from '@element-plus/icons-vue';
import { getCourseList } from '@/api/course/course';
import { createExam } from '@/api/question/exam';
import { getQuestions } from '@/api/question/question';
import type { Course } from '@/types/course/course';
import type { QuestionItem, QuestionType, Difficulty } from '@/types/question/question';
import { USE_MOCK } from '@/config/mock';
import { MOCK_QUESTIONS } from '@/mock/questions';

const router = useRouter();
const route = useRoute();

const currentStep = ref(0);
const saving = ref(false);
const courses = ref<Course[]>([]);
const publishStatus = ref<'DRAFT' | 'PUBLISHED'>('PUBLISHED');

// 表单步骤 1 数据
const step1FormRef = ref<FormInstance>();
const examForm = reactive({
  title: '2025-2026学年第二学期期末综合测试试卷',
  semester: '2025-2026-2',
  courseId: 101 as number | undefined,
  courseName: '数据结构与算法',
  durationMinutes: 120,
  totalScore: 100,
  passScore: 60,
  description: '本试卷满分100分，答题时间120分钟。请在规定区域内规范书写，独立完成作答。'
});

const step1Rules = reactive<FormRules>({
  title: [{ required: true, message: '请输入试卷名称', trigger: 'blur' }],
  semester: [{ required: true, message: '请选择学期', trigger: 'change' }],
  courseId: [{ required: true, message: '请选择所属课程', trigger: 'change' }]
});

// 大题配置数据结构
interface ExamSection {
  id: string;
  type: QuestionType;
  title: string;
  defaultScore: number;
  questions: QuestionItem[];
}

const sections = ref<ExamSection[]>([
  {
    id: 'sec-1',
    type: 'SINGLE_CHOICE',
    title: '单项选择题',
    defaultScore: 5,
    questions: []
  },
  {
    id: 'sec-2',
    type: 'MULTIPLE_CHOICE',
    title: '多项选择题',
    defaultScore: 5,
    questions: []
  },
  {
    id: 'sec-3',
    type: 'SHORT_ANSWER',
    title: '综合应用与简答题',
    defaultScore: 10,
    questions: []
  }
]);

// 题库挑选弹窗
const pickerVisible = ref(false);
const activeSection = ref<ExamSection | null>(null);
const pickerKeyword = ref('');
const pickerDifficulty = ref('');
const selectedPickerIds = ref<number[]>([]);
const poolQuestions = ref<QuestionItem[]>([]);

onMounted(async () => {
  await loadCourses();
  await loadPoolQuestions();

  // 若路由携带了 courseId 或 bankId，自动带入
  if (route.query.courseId) {
    examForm.courseId = Number(route.query.courseId);
    handleCourseChange(examForm.courseId);
  }

  // 预填默认演示题目到第一大题
  if (sections.value[0].questions.length === 0 && poolQuestions.value.length > 0) {
    const singleChoices = poolQuestions.value.filter(q => q.type === 'SINGLE_CHOICE').slice(0, 3);
    sections.value[0].questions = singleChoices.map(q => ({ ...q, score: 5 }));
  }
});

async function loadCourses() {
  try {
    const res = await getCourseList({ page: 1, pageSize: 50 });
    courses.value = res.data?.list?.length
      ? res.data.list
      : (USE_MOCK ? [
          { id: 101, title: '数据结构与算法' } as any,
          { id: 102, title: 'Java程序设计' } as any,
          { id: 103, title: '高等数学（上）' } as any
        ] : []);
  } catch (err: any) {
    if (USE_MOCK) {
      courses.value = [
        { id: 101, title: '数据结构与算法' } as any,
        { id: 102, title: 'Java程序设计' } as any,
        { id: 103, title: '高等数学（上）' } as any
      ];
    } else {
      courses.value = [];
      ElMessage.error(err?.message || '获取课程列表失败');
    }
  }
}

async function loadPoolQuestions() {
  try {
    const res = await getQuestions({ pageSize: 50 });
    poolQuestions.value = res.data?.list?.length
      ? res.data.list
      : (USE_MOCK ? MOCK_QUESTIONS : []);
  } catch (err: any) {
    if (USE_MOCK) {
      poolQuestions.value = MOCK_QUESTIONS;
    } else {
      poolQuestions.value = [];
      ElMessage.error(err?.message || '获取试题池失败');
    }
  }
}

function handleCourseChange(val?: number) {
  const c = courses.value.find(item => item.id === val);
  if (c) {
    examForm.courseName = c.title;
  }
}

function getCourseName(courseId?: number) {
  const c = courses.value.find(item => item.id === courseId);
  return c?.title || examForm.courseName || '未指定课程';
}

// 统计当前总分
const currentTotalScore = computed(() => {
  let sum = 0;
  for (const sec of sections.value) {
    for (const q of sec.questions) {
      sum += q.score || 0;
    }
  }
  return sum;
});

// 统计当前总题数
const currentTotalQuestions = computed(() => {
  return sections.value.reduce((acc, s) => acc + s.questions.length, 0);
});

function getSectionScore(sec: ExamSection) {
  return sec.questions.reduce((acc, q) => acc + (q.score || 0), 0);
}

// 切换大题默认分数时更新试题
function updateSectionDefaultScore(sec: ExamSection) {
  for (const q of sec.questions) {
    q.score = sec.defaultScore;
  }
}

function calculateScores() {
  // trigger reactivity
}

// 增加新大题
function handleAddSection(type: QuestionType) {
  const labels: Record<string, string> = {
    SINGLE_CHOICE: '单项选择题',
    MULTIPLE_CHOICE: '多项选择题',
    TRUE_FALSE: '判断题',
    FILL_BLANK: '填空题',
    SHORT_ANSWER: '简答与分析题'
  };
  const scores: Record<string, number> = {
    SINGLE_CHOICE: 5,
    MULTIPLE_CHOICE: 5,
    TRUE_FALSE: 3,
    FILL_BLANK: 4,
    SHORT_ANSWER: 10
  };

  sections.value.push({
    id: `sec-${Date.now()}`,
    type,
    title: labels[type] || '试题大题',
    defaultScore: scores[type] || 5,
    questions: []
  });
  ElMessage.success(`已添加新的大题模块：${labels[type]}`);
}

function removeSection(idx: number) {
  sections.value.splice(idx, 1);
  ElMessage.info('已移除该大题');
}

function removeQuestionFromSection(sec: ExamSection, qIdx: number) {
  sec.questions.splice(qIdx, 1);
}

// 开启选题对话框
function openQuestionPicker(sec: ExamSection) {
  activeSection.value = sec;
  pickerKeyword.value = '';
  pickerDifficulty.value = '';
  selectedPickerIds.value = [];
  pickerVisible.value = true;
}

// 候选题目池筛选
const filteredCandidateQuestions = computed(() => {
  if (!activeSection.value) return [];
  const currentSectionIds = new Set(activeSection.value.questions.map(q => q.id));

  return poolQuestions.value.filter(q => {
    // 限制必须符合该大题的题型
    if (q.type !== activeSection.value?.type) return false;
    // 排除已经在本大题中的题目
    if (currentSectionIds.has(q.id)) return false;
    // 难度筛选
    if (pickerDifficulty.value && q.difficulty !== pickerDifficulty.value) return false;
    // 关键词筛选
    if (pickerKeyword.value.trim()) {
      const kw = pickerKeyword.value.trim().toLowerCase();
      const inStem = q.stem.toLowerCase().includes(kw);
      const inKp = q.knowledgePointNames?.some(k => k.toLowerCase().includes(kw));
      if (!inStem && !inKp) return false;
    }
    return true;
  });
});

function togglePickerItem(id: number) {
  const idx = selectedPickerIds.value.indexOf(id);
  if (idx > -1) {
    selectedPickerIds.value.splice(idx, 1);
  } else {
    selectedPickerIds.value.push(id);
  }
}

function confirmAddPickedQuestions() {
  if (!activeSection.value) return;
  const pickedList = poolQuestions.value.filter(q => selectedPickerIds.value.includes(q.id));
  const newQuestions = pickedList.map(q => ({
    ...q,
    score: activeSection.value?.defaultScore || q.score || 5
  }));
  activeSection.value.questions.push(...newQuestions);
  pickerVisible.value = false;
  ElMessage.success(`成功添加 ${newQuestions.length} 道题目到【${activeSection.value.title}】`);
}

// 步骤切换校验
async function goToStep2() {
  if (!step1FormRef.value) return;
  await step1FormRef.value.validate((valid) => {
    if (valid) {
      currentStep.value = 1;
    }
  });
}

function goToStep3() {
  if (sections.value.length === 0) {
    ElMessage.warning('试卷至少需要包含一个大题！');
    return;
  }
  if (currentTotalQuestions.value === 0) {
    ElMessage.warning('试卷尚未挑选任何试题，请至少添加试题后再审阅！');
    return;
  }
  currentStep.value = 2;
}

// 保存/发布试卷
async function handleSaveExam() {
  saving.value = true;
  try {
    const allQuestions: QuestionItem[] = [];
    sections.value.forEach(sec => {
      allQuestions.push(...sec.questions);
    });

    const payload = {
      title: examForm.title,
      courseId: examForm.courseId || 101,
      courseName: examForm.courseName,
      semester: examForm.semester,
      totalScore: currentTotalScore.value,
      passScore: examForm.passScore,
      durationMinutes: examForm.durationMinutes,
      status: publishStatus.value,
      rules: sections.value.map(s => ({
        type: s.type,
        label: s.title,
        count: s.questions.length,
        scoreEach: s.defaultScore
      })),
      questions: allQuestions
    };

    await createExam(payload as any);
    ElMessage.success(publishStatus.value === 'PUBLISHED' ? '🎉 试卷正式发布成功！' : '试卷草稿已成功保存！');
    router.push('/question/exams');
  } catch (err: any) {
    console.error('保存试卷失败', err);
    ElMessage.error(err?.message || '试卷保存失败，请检查网络或后端接口状态');
  } finally {
    saving.value = false;
  }
}

function handleCancel() {
  router.push('/question/exams');
}

// 辅助转换中文数字
function getChineseNumber(num: number) {
  const cn = ['零', '一', '二', '三', '四', '五', '六', '七', '八', '九', '十'];
  return cn[num] || String(num);
}

function getTypeLabel(type: QuestionType | string) {
  const map: Record<string, string> = {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    FILL_BLANK: '填空题',
    SHORT_ANSWER: '简答题'
  };
  return map[type] || type || '单选题';
}

function getTypeTagType(type: QuestionType | string) {
  const map: Record<string, string> = {
    SINGLE_CHOICE: 'primary',
    MULTIPLE_CHOICE: 'success',
    TRUE_FALSE: 'warning',
    FILL_BLANK: 'info',
    SHORT_ANSWER: 'danger'
  };
  return (map[type] as any) || '';
}

function getDifficultyLabel(diff: Difficulty | string) {
  const map: Record<string, string> = {
    EASY: '简单',
    MEDIUM: '中等',
    HARD: '困难'
  };
  return map[diff] || '中等';
}

function getDifficultyTagType(diff: Difficulty | string) {
  const map: Record<string, string> = {
    EASY: 'success',
    MEDIUM: 'warning',
    HARD: 'danger'
  };
  return (map[diff] as any) || '';
}
</script>

<style scoped lang="scss">
.exam-create-container {
  padding: 24px;
  background: #f8fafc;
  min-height: calc(100vh - 64px);

  .top-nav-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    margin-bottom: 24px;

    .back-link {
      font-size: 14px;
      font-weight: 500;
      color: #3b82f6;
    }

    .step-progress-wrapper {
      flex: 1;
      max-width: 680px;

      :deep(.el-steps--simple) {
        background: #ffffff;
        border: 1px solid #e2e8f0;
        border-radius: 12px;
        padding: 10px 16px;
      }
    }
  }

  .step-content-box {
    .form-card {
      background: #ffffff;
      border-radius: 16px;
      border: 1px solid #e2e8f0;
      padding: 16px 24px;

      .card-header-title {
        display: flex;
        align-items: center;
        gap: 16px;

        .icon {
          font-size: 32px;
        }

        h3 {
          margin: 0;
          font-size: 18px;
          font-weight: 700;
          color: #0f172a;
        }

        p {
          margin: 4px 0 0;
          font-size: 13px;
          color: #64748b;
        }
      }

      .step-form-grid {
        margin-top: 24px;
      }

      .step-footer-bar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-top: 32px;
        padding-top: 20px;
        border-top: 1px solid #f1f5f9;
      }
    }
  }

  /* 步骤二：编排左右双栏布局 */
  .composition-workspace {
    display: flex;
    gap: 24px;
    align-items: flex-start;

    .sections-column {
      flex: 1;

      .section-actions-bar {
        background: #ffffff;
        border: 1px solid #e2e8f0;
        border-radius: 12px;
        padding: 14px 20px;
        margin-bottom: 16px;
        display: flex;
        align-items: center;
        justify-content: space-between;

        .bar-title {
          display: flex;
          align-items: center;
          gap: 10px;
          font-size: 15px;
          font-weight: 600;
          color: #1e293b;

          .badge {
            background: #eff6ff;
            color: #2563eb;
            font-size: 12px;
            padding: 2px 8px;
            border-radius: 4px;
          }
        }
      }

      .sections-stack {
        display: flex;
        flex-direction: column;
        gap: 16px;

        .section-block-card {
          background: #ffffff;
          border: 1px solid #e2e8f0;
          border-radius: 14px;
          padding: 18px 20px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);

          .section-card-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding-bottom: 14px;
            border-bottom: 1px solid #f1f5f9;
            margin-bottom: 14px;
            flex-wrap: wrap;
            gap: 12px;

            .sec-title-area {
              display: flex;
              align-items: center;
              gap: 10px;

              .sec-order-tag {
                font-weight: 700;
                color: #2563eb;
                font-size: 14px;
              }

              .sec-title-input {
                width: 160px;
              }
            }

            .sec-meta-area {
              display: flex;
              align-items: center;
              gap: 14px;

              .meta-item {
                font-size: 13px;
                color: #64748b;
                display: inline-flex;
                align-items: center;
                gap: 6px;

                .score-input {
                  width: 90px;
                }
              }
            }
          }

          .section-questions-list {
            display: flex;
            flex-direction: column;
            gap: 10px;

            .paper-question-row {
              background: #f8fafc;
              border: 1px solid #edf2f7;
              border-radius: 8px;
              padding: 10px 14px;
              display: flex;
              align-items: flex-start;
              gap: 12px;

              .q-seq {
                font-weight: 700;
                color: #64748b;
                font-size: 14px;
                margin-top: 2px;
              }

              .q-content {
                flex: 1;

                .q-stem {
                  margin: 0 0 6px;
                  font-size: 14px;
                  line-height: 1.5;
                  color: #1e293b;
                }

                .q-inline-tags {
                  display: flex;
                  gap: 6px;

                  .q-diff-badge {
                    font-size: 11px;
                    padding: 1px 6px;
                    border-radius: 4px;

                    &.easy { background: #dcfce7; color: #166534; }
                    &.medium { background: #fef9c3; color: #854d0e; }
                    &.hard { background: #fee2e2; color: #991b1b; }
                  }

                  .q-kp-badge {
                    background: #e2e8f0;
                    color: #475569;
                    font-size: 11px;
                    padding: 1px 6px;
                    border-radius: 4px;
                  }
                }
              }

              .q-score-adjust {
                display: flex;
                align-items: center;
                gap: 6px;

                .item-score-input {
                  width: 90px;
                }

                .unit {
                  font-size: 12px;
                  color: #64748b;
                }
              }
            }

            .sec-empty-hint {
              padding: 24px;
              text-align: center;
              color: #94a3b8;
              font-size: 13px;
              background: #fafafa;
              border-radius: 8px;
              border: 1px dashed #e2e8f0;
            }
          }
        }
      }
    }

    .summary-column {
      width: 320px;
      flex-shrink: 0;

      .summary-sticky-card {
        position: sticky;
        top: 80px;
        background: #ffffff;
        border-radius: 16px;
        border: 1px solid #e2e8f0;
        box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
        padding: 16px;

        .summary-card-title {
          display: flex;
          align-items: center;
          justify-content: space-between;
          font-weight: 700;
          font-size: 15px;
          color: #0f172a;
          margin-bottom: 16px;
        }

        .score-display-box {
          text-align: center;
          margin-bottom: 12px;

          .score-number {
            font-size: 40px;
            font-weight: 800;
            color: #2563eb;
            line-height: 1;
          }

          .score-unit {
            font-size: 16px;
            color: #64748b;
            margin-left: 4px;
          }
        }

        .progress-bar-wrap {
          margin-bottom: 20px;

          .progress-tip {
            display: block;
            margin-top: 6px;
            font-size: 12px;
            color: #64748b;
            text-align: center;
          }
        }

        .summary-stats-list {
          display: flex;
          flex-direction: column;
          gap: 8px;
          padding-bottom: 16px;
          border-bottom: 1px solid #f1f5f9;
          margin-bottom: 16px;

          .stat-row {
            display: flex;
            justify-content: space-between;
            font-size: 13px;

            .label {
              color: #64748b;
            }

            .val {
              font-weight: 600;
              color: #1e293b;
            }
          }
        }

        .distribution-section {
          margin-bottom: 20px;

          .dist-title {
            font-size: 13px;
            font-weight: 600;
            color: #334155;
            display: block;
            margin-bottom: 10px;
          }

          .dist-bars {
            display: flex;
            flex-direction: column;
            gap: 8px;

            .dist-item {
              .dist-label {
                display: flex;
                justify-content: space-between;
                font-size: 12px;
                color: #64748b;
                margin-bottom: 3px;
              }

              .dist-mini-bar {
                height: 6px;
                background: #f1f5f9;
                border-radius: 9999px;
                overflow: hidden;

                .fill {
                  height: 100%;
                  background: #3b82f6;
                  border-radius: 9999px;
                }
              }
            }
          }
        }

        .summary-actions {
          display: flex;
          flex-direction: column;
        }
      }
    }
  }

  /* 步骤三：纸质试卷排版与预览 */
  .review-paper-wrapper {
    max-width: 960px;
    margin: 0 auto;

    .paper-sheet {
      background: #ffffff;
      border: 1px solid #d1d5db;
      box-shadow: 0 8px 30px rgba(0, 0, 0, 0.06);
      padding: 48px 64px;
      border-radius: 8px;
      font-family: 'Times New Roman', SimSun, serif;

      .paper-header-section {
        text-align: center;
        border-bottom: 2px solid #1e293b;
        padding-bottom: 24px;
        margin-bottom: 24px;

        .school-mark {
          font-size: 14px;
          font-weight: 600;
          color: #4b5563;
          letter-spacing: 2px;
          margin-bottom: 6px;
        }

        .paper-main-title {
          font-size: 24px;
          font-weight: 800;
          color: #111827;
          margin: 10px 0;
        }

        .paper-sub-meta {
          display: flex;
          justify-content: center;
          gap: 24px;
          font-size: 14px;
          color: #4b5563;
          margin: 8px 0 16px;
        }

        .student-meta-seal-bar {
          display: flex;
          justify-content: space-around;
          background: #f9fafb;
          border: 1px dashed #9ca3af;
          padding: 10px;
          border-radius: 6px;
          font-size: 13px;
          color: #374151;
          margin-bottom: 14px;
        }

        .paper-notice-box {
          font-size: 13px;
          color: #6b7280;
          text-align: left;
          background: #f8fafc;
          padding: 8px 12px;
          border-radius: 6px;
        }
      }

      .paper-sections-body {
        .paper-section-block {
          margin-bottom: 28px;

          .paper-sec-heading {
            font-size: 17px;
            font-weight: 700;
            color: #111827;
            border-bottom: 1px solid #e5e7eb;
            padding-bottom: 6px;
            margin-bottom: 14px;

            .sec-score-sum {
              font-size: 14px;
              font-weight: normal;
              color: #4b5563;
            }
          }

          .paper-questions-flow {
            display: flex;
            flex-direction: column;
            gap: 16px;

            .paper-question-unit {
              .unit-stem {
                font-size: 15px;
                line-height: 1.6;
                color: #1f2937;

                .unit-index {
                  font-weight: 700;
                  margin-right: 4px;
                }

                .unit-score {
                  color: #6b7280;
                  font-size: 13px;
                }
              }

              .unit-options-grid {
                display: grid;
                grid-template-columns: repeat(2, 1fr);
                gap: 8px 24px;
                margin-top: 8px;
                padding-left: 18px;

                .unit-option-item {
                  font-size: 14px;
                  color: #374151;

                  .key {
                    font-weight: 600;
                    margin-right: 4px;
                  }
                }
              }

              .paper-answer-blank {
                margin-top: 12px;
                padding-left: 18px;

                .blank-lines {
                  display: flex;
                  flex-direction: column;
                  gap: 18px;

                  .line {
                    border-bottom: 1px dashed #d1d5db;
                    height: 1px;
                  }
                }
              }
            }
          }
        }
      }
    }

    .review-bottom-dock {
      margin-top: 24px;
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      padding: 16px 24px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);

      .dock-right {
        display: flex;
        gap: 12px;
      }
    }
  }

  /* 挑选弹窗 */
  .picker-modal-inner {
    .picker-filter-row {
      display: flex;
      gap: 12px;
      margin-bottom: 16px;
    }

    .picker-list-area {
      max-height: 460px;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
      gap: 10px;

      .picker-question-item {
        border: 1px solid #e2e8f0;
        border-radius: 8px;
        padding: 12px 14px;
        display: flex;
        gap: 12px;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          background: #f8fafc;
          border-color: #cbd5e1;
        }

        &.selected {
          border-color: #3b82f6;
          background: #eff6ff;
        }

        .picker-item-main {
          flex: 1;

          .badge-line {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 6px;

            .picker-score {
              font-size: 12px;
              color: #2563eb;
              font-weight: 600;
            }
          }

          .picker-stem {
            font-size: 14px;
            color: #1e293b;
            line-height: 1.5;
            margin: 0;
          }
        }
      }

      .picker-empty {
        padding: 40px;
        text-align: center;
        color: #94a3b8;
        font-size: 14px;
      }
    }
  }

  .picker-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;

    strong {
      color: #2563eb;
      font-size: 16px;
    }
  }
}
</style>
