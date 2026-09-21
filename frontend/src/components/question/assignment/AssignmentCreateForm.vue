<template>
  <div class="assignment-create-flow">
    <!-- 顶部现代化 Hero 头部卡片（严格对标图 2 题库详情设计语言） -->
    <div class="assignment-hero-card">
      <!-- 顶部导航条与面包屑 -->
      <div class="header-nav-bar">
        <button type="button" class="back-btn" @click="$emit('cancel')">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回作业列表</span>
        </button>
        <el-divider direction="vertical" class="nav-divider" />
        <el-breadcrumb separator="/" class="header-breadcrumb">
          <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item :to="{ path: '/question/assignments' }">作业管理</el-breadcrumb-item>
          <el-breadcrumb-item>发布新作业 / 课后评测</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <!-- 头部主体信息与快捷操作 -->
      <div class="header-main-section">
        <div class="header-info-col">
          <div class="assignment-avatar-orb">
            <el-icon class="assignment-icon"><Promotion /></el-icon>
            <span class="orb-glow-ring" />
          </div>

          <div class="assignment-meta-content">
            <div class="assignment-title-line">
              <h1 class="assignment-title">{{ formData.title || '发布新作业 / 课后评测' }}</h1>
              <span class="course-badge">
                <el-icon><Collection /></el-icon>
                {{ selectedCourseName || '请选择关联课程' }}
              </span>
            </div>

            <p class="assignment-description">
              为班级学生布置在线课后作业或章节阶段考核，支持从标准化试卷库关联或自主题库抽题，并全程开启 AI 智能评阅辅助。
            </p>

            <div class="assignment-time-meta">
              <span class="time-item">
                <el-icon><Clock /></el-icon>
                截止时间：{{ formData.deadline ? formatDeadline(formData.deadline) : '尚未设定' }}
              </span>
              <span class="meta-dot">·</span>
              <span class="status-item">
                <span class="status-indicator-dot" />
                测评布置工作台就绪
              </span>
            </div>
          </div>
        </div>

        <div class="header-actions-col">
          <div class="actions-row actions-row--primary">
            <button
              type="button"
              class="action-pill action-pill--brand"
              :disabled="submitting"
              @click="$emit('publish')"
            >
              <el-icon><Promotion /></el-icon>
              <span>{{ submitting ? '发布中...' : '确认并正式发布作业' }}</span>
              <span class="ai-spark-chip">AI 批改</span>
            </button>
          </div>

          <div class="actions-row actions-row--secondary">
            <button
              type="button"
              class="action-pill action-pill--ghost"
              @click="$emit('cancel')"
            >
              <el-icon><Close /></el-icon>
              <span>放弃并返回</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 底部 4 维微看板（1:1 模仿图 2 的收录试题总量、卷面总分、难度结构、交付状态设计） -->
      <div class="stats-micro-bar">
        <!-- 指标卡 1：装载试题总量与构成 -->
        <div class="stat-card stat-card--blue">
          <div class="stat-card__icon-box">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__label">装载试题总量</span>
            <div class="stat-card__value-row">
              <strong class="stat-card__num">{{ selectedQuestions.length }}</strong>
              <span class="stat-card__unit">道题目</span>
            </div>
            <div class="stat-card__sub-hint">
              <span>{{ questionsTypeSummary || '等待装载试卷或试题' }}</span>
            </div>
          </div>
        </div>

        <!-- 指标卡 2：卷面配置总分与基准线 -->
        <div class="stat-card stat-card--emerald">
          <div class="stat-card__icon-box">
            <el-icon><Tickets /></el-icon>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__label">卷面设定总分</span>
            <div class="stat-card__value-row">
              <strong class="stat-card__num">{{ formData.totalScore }}</strong>
              <span class="stat-card__unit">分</span>
            </div>
            <div class="stat-card__sub-hint">
              <span>合格基准线设定 <strong>{{ formData.passScore }}</strong> 分</span>
            </div>
          </div>
        </div>

        <!-- 指标卡 3：试题难度结构分析 -->
        <div class="stat-card stat-card--amber">
          <div class="stat-card__icon-box">
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__label">综合难度系数</span>
            <div class="stat-card__value-row">
              <strong class="stat-card__num text-amber-600">{{ difficultyLevelText }}</strong>
            </div>
            <div class="stat-card__sub-hint">
              <span>{{ difficultyBreakdown }}</span>
            </div>
          </div>
        </div>

        <!-- 指标卡 4：AI 批改与提交模式 -->
        <div class="stat-card stat-card--indigo">
          <div class="stat-card__icon-box">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__label">智能服务交付</span>
            <div class="stat-card__value-row">
              <span v-if="formData.aiGradingEnabled" class="ready-badge">AI 批改已就绪</span>
              <span v-else class="manual-badge">人工批改模式</span>
            </div>
            <div class="stat-card__sub-hint">
              <span>{{ formData.instantFeedback ? '客观题交卷立显解析' : '交卷暂隐客观题答案' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 核心表单区域 -->
    <el-form
      :ref="bindFormRef"
      :model="formData"
      :rules="rules"
      label-position="top"
      class="assignment-form-body"
    >
      <!-- 区域 1：基础配置面板 -->
      <section class="config-card">
        <div class="section-badge-header">
          <div class="section-title-wrap">
            <div class="section-step-badge">1</div>
            <div>
              <h2 class="section-title">作业基础配置</h2>
              <p class="section-desc">设定作业的核心考核标题、修读班级/所属课程、截止提交时限与分值合格线</p>
            </div>
          </div>
        </div>

        <div class="card-inner-form">
          <el-row :gutter="24">
            <el-col :xs="24" :md="16">
              <el-form-item label="作业名称 / 考核标题" prop="title">
                <el-input
                  v-model="formData.title"
                  placeholder="例如：第三章：树与二叉树遍历课后巩固测试"
                  size="large"
                  class="custom-large-input"
                  clearable
                >
                  <template #prefix>
                    <el-icon class="input-icon"><EditPen /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
            </el-col>

            <el-col :xs="24" :md="8">
              <el-form-item label="所属课程" prop="courseId">
                <el-select
                  v-model="formData.courseId"
                  placeholder="请选择关联课程"
                  size="large"
                  class="w-full custom-select"
                  @change="onCourseChange"
                >
                  <el-option
                    v-for="c in courses"
                    :key="c.id"
                    :label="c.title || c.name"
                    :value="Number(c.id)"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :xs="24" :md="10">
              <el-form-item label="截止提交时间" prop="deadline">
                <el-date-picker
                  v-model="formData.deadline"
                  type="datetime"
                  placeholder="请选择截止提交日期与具体时间"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  size="large"
                  class="w-full custom-datepicker"
                />
              </el-form-item>
            </el-col>

            <el-col :xs="12" :md="7">
              <el-form-item label="卷面总分 (分)">
                <div class="score-input-wrapper">
                  <el-input-number
                    v-model="formData.totalScore"
                    :min="1"
                    :max="300"
                    size="large"
                    class="w-full custom-number-input"
                  />
                </div>
              </el-form-item>
            </el-col>

            <el-col :xs="12" :md="7">
              <el-form-item label="合格标准分 (分)">
                <div class="score-input-wrapper">
                  <el-input-number
                    v-model="formData.passScore"
                    :min="1"
                    :max="formData.totalScore"
                    size="large"
                    class="w-full custom-number-input"
                  />
                </div>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </section>

      <!-- 区域 2：考题内容装载来源 -->
      <section class="config-card">
        <div class="section-badge-header">
          <div class="section-title-wrap">
            <div class="section-step-badge">2</div>
            <div>
              <h2 class="section-title">考题内容装载来源</h2>
              <p class="section-desc">可直接关联已有标准化试卷导入完整考题，或自由勾选组合自定义作业试题</p>
            </div>
          </div>

          <!-- 模式切换胶囊按钮（对标图 2 筛选胶囊） -->
          <div class="source-mode-toggle">
            <button
              type="button"
              class="capsule-tab-btn"
              :class="{ active: sourceMode === 'EXAM' }"
              @click="$emit('update:sourceMode', 'EXAM')"
            >
              <el-icon><DocumentCopy /></el-icon>
              <span>关联已有标准化试卷</span>
            </button>
            <button
              type="button"
              class="capsule-tab-btn"
              :class="{ active: sourceMode === 'QUESTIONS' }"
              @click="$emit('update:sourceMode', 'QUESTIONS')"
            >
              <el-icon><Tickets /></el-icon>
              <span>自选题库快速装配</span>
            </button>
          </div>
        </div>

        <div class="card-inner-form">
          <!-- 关联试卷选择框 -->
          <div v-if="sourceMode === 'EXAM'" class="source-select-row">
            <el-form-item label="选择引用的目标试卷" prop="examId" class="w-full">
              <el-select
                v-model="formData.examId"
                placeholder="请选择已发布的完整考核试卷"
                size="large"
                class="w-full custom-select"
                filterable
                @change="onExamSelected"
              >
                <el-option
                  v-for="e in examOptions"
                  :key="e.id"
                  :label="e.title"
                  :value="Number(e.id)"
                >
                  <div class="exam-option-slot">
                    <span class="opt-title">{{ e.title }}</span>
                    <span class="opt-score-badge">{{ e.totalScore || 100 }} 分</span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
          </div>

          <!-- 已装载试题清单卡片（富化卡片式设计，支持展开与清晰题干预览） -->
          <div class="preview-deck">
            <div class="preview-deck-header">
              <div class="deck-header-left">
                <span class="deck-title">已装载试题清单</span>
                <span class="deck-counter-badge">共 {{ selectedQuestions.length }} 道</span>
              </div>
              <div class="deck-header-right">
                <span class="deck-score-hint">
                  试题分值总计：<strong class="score-highlight">{{ totalCalculatedScore }}</strong> 分
                </span>
                <button
                  v-if="selectedQuestions.length > 0"
                  type="button"
                  class="expand-all-btn"
                  @click="toggleAllQuestions"
                >
                  <el-icon><Operation /></el-icon>
                  <span>{{ allExpanded ? '收起解析' : '展开题干与知识点' }}</span>
                </button>
              </div>
            </div>

            <!-- 试题流列表（对标图 2 题库试题展示） -->
            <div v-if="selectedQuestions.length > 0" class="questions-stream-list">
              <div
                v-for="(q, idx) in pagedQuestions"
                :key="q.id || idx"
                class="question-deck-item"
              >
                <div class="deck-item-top">
                  <div class="item-meta-left">
                    <span class="question-number">#{{ (currentPage - 1) * pageSize + idx + 1 }}</span>
                    <el-tag size="small" :type="getTypeTagType(q.type)" class="type-pill">
                      {{ getTypeLabel(q.type) }}
                    </el-tag>
                    <span class="score-chip">{{ q.score || 5 }} 分</span>
                    <span v-if="q.difficulty" class="diff-tag" :class="`diff-tag--${(q.difficulty || '').toLowerCase()}`">
                      {{ getDifficultyText(q.difficulty) }}
                    </span>
                    <span v-if="q.knowledgePointNames?.[0]" class="kp-pill">
                      <el-icon><CollectionTag /></el-icon>
                      {{ q.knowledgePointNames[0] }}
                    </span>
                  </div>
                </div>

                <div class="deck-item-body">
                  <MathText tag="div" class="question-stem-text" :text="q.stem" />

                  <!-- 展开后的选项预览 -->
                  <div v-if="allExpanded && q.options && q.options.length" class="question-options-preview">
                    <div
                      v-for="opt in q.options"
                      :key="opt.key"
                      class="preview-opt"
                      :class="{ 'preview-opt--correct': opt.isCorrect }"
                    >
                      <span class="preview-opt-key">{{ opt.key }}.</span>
                      <MathText tag="span" class="preview-opt-text" :text="opt.content" />
                    </div>
                  </div>
                </div>
              </div>

              <!-- 分页器组件 -->
              <div class="deck-pagination-row">
                <AppPagination
                  v-model:page-num="currentPage"
                  v-model:page-size="pageSize"
                  :total="selectedQuestions.length"
                  :page-sizes="[5, 10, 20]"
                />
              </div>
            </div>

            <div v-else class="preview-empty-state">
              <div class="empty-icon-wrap">
                <el-icon><DocumentAdd /></el-icon>
              </div>
              <p class="empty-title">当前暂未装载作业试题</p>
              <p class="empty-desc">请在上方选择已有试卷，或切换至自选题库快速装载试题。</p>
            </div>
          </div>
        </div>
      </section>

      <!-- 区域 3：智能化与批改规则 -->
      <section class="config-card">
        <div class="section-badge-header">
          <div class="section-title-wrap">
            <div class="section-step-badge">3</div>
            <div>
              <h2 class="section-title">智能评阅与提交行为规则</h2>
              <p class="section-desc">配置大模型 AI 辅助批改、逾期补交策略以及学生交卷即时反馈行为</p>
            </div>
          </div>
        </div>

        <div class="card-inner-form">
          <div class="rules-card-grid">
            <div class="rule-box" :class="{ 'rule-box--active': formData.aiGradingEnabled }">
              <div class="rule-icon-orb rule-icon-orb--blue">
                <el-icon><Service /></el-icon>
              </div>
              <div class="rule-content">
                <div class="rule-headline">
                  <span class="rule-title">启用 AI 智能自动预批改</span>
                  <el-switch v-model="formData.aiGradingEnabled" />
                </div>
                <p class="rule-desc">
                  学生提交主观题或简答题后，自动调用大模型根据参考答案及采分点生成评分建议、错因剖析与个性化评语。
                </p>
              </div>
            </div>

            <div class="rule-box" :class="{ 'rule-box--active': formData.allowLate }">
              <div class="rule-icon-orb rule-icon-orb--amber">
                <el-icon><Timer /></el-icon>
              </div>
              <div class="rule-content">
                <div class="rule-headline">
                  <span class="rule-title">允许逾期迟交答卷</span>
                  <el-switch v-model="formData.allowLate" />
                </div>
                <p class="rule-desc">
                  在截止时间到达后，系统仍允许学生补交作业答卷，批改管理端将明显标注“迟交”状态以供教师审阅。
                </p>
              </div>
            </div>

            <div class="rule-box" :class="{ 'rule-box--active': formData.instantFeedback }">
              <div class="rule-icon-orb rule-icon-orb--emerald">
                <el-icon><View /></el-icon>
              </div>
              <div class="rule-content">
                <div class="rule-headline">
                  <span class="rule-title">交卷立即可见客观题得分与解析</span>
                  <el-switch v-model="formData.instantFeedback" />
                </div>
                <p class="rule-desc">
                  学生完成交卷后，立即对其开放单选、多选与判断题等客观题的标准解答、解析与系统评分，促进即时正向反馈。
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 底部发布操作停靠栏（现代悬浮质感） -->
      <div class="actions-dock-card">
        <div class="dock-summary">
          <span class="dock-summary-label">待发布考核：</span>
          <span class="dock-summary-name">{{ formData.title || '（未命名作业）' }}</span>
          <span class="dock-summary-stats">共 {{ selectedQuestions.length }} 题 · {{ formData.totalScore }} 分</span>
        </div>

        <div class="dock-buttons">
          <button type="button" class="dock-btn dock-btn--cancel" @click="$emit('cancel')">
            取消发布
          </button>
          <button
            type="button"
            class="dock-btn dock-btn--primary"
            :disabled="submitting"
            @click="$emit('publish')"
          >
            <el-icon class="btn-icon"><Promotion /></el-icon>
            <span>{{ submitting ? '正在发布...' : '确认并正式发布作业' }}</span>
          </button>
        </div>
      </div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import MathText from '@/components/common/MathText.vue';
import AppPagination from '@/components/common/AppPagination.vue';
import {
  Promotion,
  Service,
  Timer,
  View,
  ArrowLeft,
  Collection,
  Clock,
  Close,
  Document,
  Tickets,
  DataAnalysis,
  CircleCheck,
  EditPen,
  DocumentCopy,
  Operation,
  CollectionTag,
  DocumentAdd
} from '@element-plus/icons-vue';
import type { Course } from '@/types/course/course';
import type { QuestionItem, QuestionType } from '@/types/question/question';

const props = defineProps<{
  setFormRef: (el: FormInstance | undefined) => void;
  formData: {
    title: string;
    courseId?: number;
    deadline: string;
    totalScore: number;
    passScore: number;
    examId?: number;
    aiGradingEnabled: boolean;
    allowLate: boolean;
    instantFeedback: boolean;
  };
  rules: FormRules;
  courses: Course[];
  examOptions: any[];
  sourceMode: 'EXAM' | 'QUESTIONS';
  selectedQuestions: QuestionItem[];
  totalCalculatedScore: number;
  submitting: boolean;
  onCourseChange: (courseId?: number) => void;
  onExamSelected: (id?: number) => void;
  getTypeLabel: (type: QuestionType | string) => string;
  getTypeTagType: (type: QuestionType | string) => string;
}>();

defineEmits<{
  cancel: [];
  publish: [];
  'update:sourceMode': [value: 'EXAM' | 'QUESTIONS'];
}>();

const allExpanded = ref(false);

function toggleAllQuestions() {
  allExpanded.value = !allExpanded.value;
}

const currentPage = ref(1);
const pageSize = ref(10);

watch(
  () => props.selectedQuestions.length,
  () => {
    currentPage.value = 1;
  }
);

const pagedQuestions = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return props.selectedQuestions.slice(start, start + pageSize.value);
});

function bindFormRef(el: FormInstance | null) {
  props.setFormRef(el ?? undefined);
}

const selectedCourseName = computed(() => {
  if (!props.formData.courseId) return '';
  const c = props.courses.find(item => Number(item.id) === Number(props.formData.courseId));
  return c?.title || c?.name || '';
});

function formatDeadline(timeStr: string): string {
  if (!timeStr) return '';
  return timeStr.replace('T', ' ').slice(0, 16);
}

function getDifficultyText(diff?: string): string {
  if (diff === 'EASY') return '基础';
  if (diff === 'HARD') return '困难';
  return '中等';
}

const questionsTypeSummary = computed(() => {
  if (!props.selectedQuestions || props.selectedQuestions.length === 0) return '';
  const single = props.selectedQuestions.filter(q => q.type === 'SINGLE_CHOICE').length;
  const multi = props.selectedQuestions.filter(q => q.type === 'MULTIPLE_CHOICE').length;
  const tf = props.selectedQuestions.filter(
    q => q.type === 'TRUE_FALSE' || (q.type as any) === 'JUDGMENT'
  ).length;
  const parts: string[] = [];
  if (single > 0) parts.push(`单选 ${single}`);
  if (multi > 0) parts.push(`多选 ${multi}`);
  if (tf > 0) parts.push(`判断 ${tf}`);
  const other = props.selectedQuestions.length - (single + multi + tf);
  if (other > 0) parts.push(`主观 ${other}`);
  return parts.join(' · ');
});

const difficultyLevelText = computed(() => {
  if (!props.selectedQuestions || props.selectedQuestions.length === 0) return '标准适中';
  const easy = props.selectedQuestions.filter(q => q.difficulty === 'EASY').length;
  const hard = props.selectedQuestions.filter(q => q.difficulty === 'HARD').length;
  if (hard > easy) return '高阶进阶';
  if (easy > hard) return '基础巩固';
  return '标准适中';
});

const difficultyBreakdown = computed(() => {
  if (!props.selectedQuestions || props.selectedQuestions.length === 0) return '等待装入试题';
  const easy = props.selectedQuestions.filter(q => q.difficulty === 'EASY').length;
  const med = props.selectedQuestions.filter(q => q.difficulty === 'MEDIUM' || !q.difficulty).length;
  const hard = props.selectedQuestions.filter(q => q.difficulty === 'HARD').length;
  return `基础 ${easy} · 适中 ${med} · 难点 ${hard}`;
});
</script>

<style scoped lang="scss">
.assignment-create-flow {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 顶部现代化 Hero 卡片 */
.assignment-hero-card {
  position: relative;
  overflow: hidden;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 24px rgba(30, 80, 160, 0.05);
  background: #ffffff;
  background-image:
    linear-gradient(90deg, #2563eb 0%, #6366f1 50%, #7c3aed 100%),
    linear-gradient(145deg, #ffffff 0%, #f8fafc 55%, #f1f7ff 100%);
  background-size: 100% 3px, 100% calc(100% - 3px);
  background-position: 0 0, 0 3px;
  background-repeat: no-repeat;
  padding: 16px 28px 20px;

  .header-nav-bar {
    display: flex;
    align-items: center;
    gap: 12px;
    padding-bottom: 14px;
    border-bottom: 1px solid #f1f5f9;
    margin-bottom: 18px;

    .back-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 0;
      border: none;
      background: transparent;
      font-size: 13px;
      font-weight: 600;
      color: #2563eb;
      cursor: pointer;
      white-space: nowrap;
      transition: color 0.15s ease;

      .el-icon {
        font-size: 14px;
      }

      &:hover {
        color: #1d4ed8;
      }
    }

    .nav-divider {
      margin: 0 4px;
      height: 14px;
      border-color: #e2e8f0;
    }

    .header-breadcrumb {
      flex: 1;

      :deep(.el-breadcrumb__inner) {
        font-size: 12.5px;
        color: #94a3b8;
      }

      :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
        color: #475569;
        font-weight: 600;
      }
    }
  }

  .header-main-section {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 28px;
    flex-wrap: wrap;

    @media (max-width: 1080px) {
      flex-direction: column;
    }
  }

  .header-info-col {
    display: flex;
    align-items: flex-start;
    gap: 18px;
    flex: 1;
    min-width: 320px;

    .assignment-avatar-orb {
      position: relative;
      flex-shrink: 0;
      width: 58px;
      height: 58px;
      border-radius: 16px;
      background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
      border: 1.5px solid #bfdbfe;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 8px 20px rgba(37, 99, 235, 0.14);

      .assignment-icon {
        font-size: 28px;
        color: #2563eb;
      }

      .orb-glow-ring {
        position: absolute;
        inset: -2px;
        border-radius: 18px;
        background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.8), transparent 70%);
        pointer-events: none;
      }
    }

    .assignment-meta-content {
      display: flex;
      flex-direction: column;
      gap: 8px;
      min-width: 0;

      .assignment-title-line {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;

        .assignment-title {
          margin: 0;
          font-size: 22px;
          font-weight: 800;
          color: #0f172a;
          letter-spacing: -0.02em;
          line-height: 1.25;
        }

        .course-badge {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          padding: 3px 12px;
          border-radius: 9999px;
          background: #eff6ff;
          border: 1px solid #bfdbfe;
          color: #2563eb;
          font-size: 12px;
          font-weight: 600;
          white-space: nowrap;

          .el-icon {
            font-size: 13px;
          }
        }
      }

      .assignment-description {
        margin: 0;
        font-size: 13.5px;
        color: #64748b;
        line-height: 1.6;
        max-width: 720px;
      }

      .assignment-time-meta {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 12.5px;
        color: #94a3b8;

        .time-item {
          display: inline-flex;
          align-items: center;
          gap: 5px;

          .el-icon {
            font-size: 13px;
          }
        }

        .meta-dot {
          color: #cbd5e1;
        }

        .status-item {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          color: #059669;
          font-weight: 500;

          .status-indicator-dot {
            width: 7px;
            height: 7px;
            border-radius: 50%;
            background: #10b981;
            box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
          }
        }
      }
    }
  }

  .header-actions-col {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 10px;
    flex-shrink: 0;

    @media (max-width: 1080px) {
      align-items: flex-start;
      width: 100%;
    }

    .actions-row {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;

      &--secondary {
        justify-content: flex-end;
      }
    }

    .action-pill {
      display: inline-flex;
      align-items: center;
      gap: 7px;
      height: 38px;
      padding: 0 16px;
      border-radius: 9999px;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      border: none;
      outline: none;
      user-select: none;
      white-space: nowrap;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

      .el-icon {
        font-size: 14px;
      }

      &--brand {
        background: linear-gradient(135deg, #1677ff 0%, #2563eb 100%);
        color: #ffffff;
        box-shadow: 0 4px 14px rgba(22, 119, 255, 0.25);

        &:hover:not(:disabled) {
          transform: translateY(-2px);
          box-shadow: 0 6px 18px rgba(22, 119, 255, 0.35);
          background: linear-gradient(135deg, #0958d9 0%, #1d4ed8 100%);
        }

        &:disabled {
          opacity: 0.65;
          cursor: not-allowed;
        }

        .ai-spark-chip {
          padding: 1px 7px;
          border-radius: 9999px;
          background: rgba(255, 255, 255, 0.24);
          font-size: 11px;
          font-weight: 700;
          letter-spacing: 0.02em;
        }
      }

      &--ghost {
        background: #ffffff;
        color: #475569;
        border: 1px solid #cbd5e1;

        &:hover {
          background: #f8fafc;
          border-color: #94a3b8;
          color: #1e293b;
          transform: translateY(-1px);
        }
      }
    }
  }

  /* 4 维微看板 */
  .stats-micro-bar {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
    width: 100%;
    margin-top: 18px;
    padding-top: 18px;
    border-top: 1px solid rgba(226, 232, 240, 0.8);

    @media (max-width: 1100px) {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    @media (max-width: 640px) {
      grid-template-columns: 1fr;
    }

    .stat-card {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 12px 16px;
      border-radius: 14px;
      background: #ffffff;
      border: 1px solid #e2e8f0;
      box-shadow: 0 2px 10px rgba(15, 23, 42, 0.02);
      transition: all 0.2s ease;

      &:hover {
        border-color: #cbd5e1;
        transform: translateY(-1px);
        box-shadow: 0 4px 14px rgba(15, 23, 42, 0.05);
      }

      &__icon-box {
        width: 42px;
        height: 42px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;

        .el-icon {
          font-size: 20px;
        }
      }

      &__content {
        display: flex;
        flex-direction: column;
        min-width: 0;
        flex: 1;
      }

      &__label {
        font-size: 12px;
        color: #64748b;
        font-weight: 500;
        line-height: 1.3;
      }

      &__value-row {
        display: flex;
        align-items: baseline;
        gap: 6px;
        margin-top: 2px;
      }

      &__num {
        font-size: 20px;
        font-weight: 800;
        color: #0f172a;
        line-height: 1.2;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      }

      &__unit {
        font-size: 12px;
        color: #64748b;
        font-weight: 600;
      }

      &__sub-hint {
        font-size: 11.5px;
        color: #94a3b8;
        margin-top: 2px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;

        strong {
          color: #475569;
        }
      }

      .ready-badge {
        display: inline-flex;
        align-items: center;
        padding: 2px 8px;
        border-radius: 9999px;
        background: #ecfdf5;
        color: #059669;
        font-size: 12px;
        font-weight: 700;
        border: 1px solid #a7f3d0;
      }

      .manual-badge {
        display: inline-flex;
        align-items: center;
        padding: 2px 8px;
        border-radius: 9999px;
        background: #f8fafc;
        color: #64748b;
        font-size: 12px;
        font-weight: 600;
        border: 1px solid #e2e8f0;
      }

      &--blue {
        .stat-card__icon-box {
          background: #eff6ff;
          color: #2563eb;
          border: 1px solid #bfdbfe;
        }
      }

      &--emerald {
        .stat-card__icon-box {
          background: #ecfdf5;
          color: #059669;
          border: 1px solid #a7f3d0;
        }
      }

      &--amber {
        .stat-card__icon-box {
          background: #fffbeb;
          color: #d97706;
          border: 1px solid #fde68a;
        }
      }

      &--indigo {
        .stat-card__icon-box {
          background: #f5f3ff;
          color: #7c3aed;
          border: 1px solid #ddd6fe;
        }
      }
    }
  }
}

/* 核心配置表单与卡片样式 */
.assignment-form-body {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .config-card {
    background: #ffffff;
    border-radius: 20px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 4px 20px rgba(30, 80, 150, 0.03);
    padding: 24px 28px;

    .section-badge-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      padding-bottom: 18px;
      border-bottom: 1px solid #f1f5f9;
      margin-bottom: 22px;
      flex-wrap: wrap;

      .section-title-wrap {
        display: flex;
        align-items: center;
        gap: 14px;

        .section-step-badge {
          width: 32px;
          height: 32px;
          border-radius: 10px;
          background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
          border: 1.5px solid #bfdbfe;
          color: #2563eb;
          font-weight: 800;
          font-size: 15px;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
        }

        .section-title {
          margin: 0;
          font-size: 16px;
          font-weight: 700;
          color: #0f172a;
          letter-spacing: -0.01em;
        }

        .section-desc {
          margin: 3px 0 0;
          font-size: 12.5px;
          color: #64748b;
        }
      }
    }

    .card-inner-form {
      :deep(.el-form-item__label) {
        font-size: 13px;
        font-weight: 600;
        color: #334155;
        padding-bottom: 6px;
      }
    }
  }

  /* 输入控件精致圆角 */
  .custom-large-input {
    :deep(.el-input__wrapper) {
      border-radius: 12px;
      padding: 4px 14px;
      border-color: #cbd5e1;
      transition: all 0.2s ease;

      &.is-focus {
        border-color: #2563eb;
        box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
      }
    }

    .input-icon {
      font-size: 16px;
      color: #94a3b8;
      margin-right: 6px;
    }
  }

  .custom-select {
    :deep(.el-select__wrapper) {
      border-radius: 12px;
      padding: 4px 14px;
      min-height: 42px;
    }
  }

  .custom-datepicker {
    :deep(.el-input__wrapper) {
      border-radius: 12px;
      padding: 4px 14px;
      min-height: 42px;
    }
  }

  .score-input-wrapper {
    position: relative;
    display: flex;
    align-items: center;

    .custom-number-input {
      width: 100%;

      :deep(.el-input__wrapper) {
        border-radius: 12px;
        padding: 4px 14px;
        min-height: 42px;
      }
    }

    .score-unit {
      position: absolute;
      right: 48px;
      font-size: 11px;
      font-weight: 700;
      color: #94a3b8;
      letter-spacing: 0.05em;
      pointer-events: none;
    }
  }

  /* 来源模式胶囊切换器 */
  .source-mode-toggle {
    display: inline-flex;
    align-items: center;
    padding: 3px;
    background: #f1f5f9;
    border-radius: 9999px;
    border: 1px solid #e2e8f0;

    .capsule-tab-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 6px 14px;
      border-radius: 9999px;
      border: none;
      background: transparent;
      font-size: 12.5px;
      font-weight: 600;
      color: #64748b;
      cursor: pointer;
      transition: all 0.2s ease;

      .el-icon {
        font-size: 14px;
      }

      &.active {
        background: #2563eb;
        color: #ffffff;
        box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
      }

      &:hover:not(.active) {
        color: #1e293b;
      }
    }
  }

  .source-select-row {
    margin-bottom: 18px;

    .exam-option-slot {
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;

      .opt-title {
        font-size: 13px;
        color: #1e293b;
        font-weight: 500;
      }

      .opt-score-badge {
        font-size: 11.5px;
        font-weight: 700;
        color: #2563eb;
        background: #eff6ff;
        padding: 1px 8px;
        border-radius: 9999px;
      }
    }
  }

  /* 试题清单 Deck 卡片 */
  .preview-deck {
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 16px;
    padding: 18px 22px;

    .preview-deck-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      margin-bottom: 14px;
      flex-wrap: wrap;

      .deck-header-left {
        display: flex;
        align-items: center;
        gap: 10px;

        .deck-title {
          font-size: 14px;
          font-weight: 700;
          color: #1e293b;
        }

        .deck-counter-badge {
          display: inline-flex;
          align-items: center;
          padding: 2px 10px;
          border-radius: 9999px;
          background: #e0f2fe;
          color: #0284c7;
          font-size: 11.5px;
          font-weight: 700;
        }
      }

      .deck-header-right {
        display: flex;
        align-items: center;
        gap: 14px;

        .deck-score-hint {
          font-size: 13px;
          color: #64748b;

          .score-highlight {
            font-size: 15px;
            color: #2563eb;
            font-weight: 800;
          }
        }

        .expand-all-btn {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          padding: 4px 12px;
          border-radius: 8px;
          border: 1px solid #cbd5e1;
          background: #ffffff;
          font-size: 12px;
          font-weight: 600;
          color: #475569;
          cursor: pointer;
          transition: all 0.15s ease;

          &:hover {
            color: #2563eb;
            border-color: #93c5fd;
            background: #eff6ff;
          }
        }
      }
    }

    .questions-stream-list {
      display: flex;
      flex-direction: column;
      gap: 10px;

      .question-deck-item {
        background: #ffffff;
        border: 1px solid #eef2f6;
        border-radius: 12px;
        padding: 14px 18px;
        box-shadow: 0 1px 4px rgba(15, 23, 42, 0.02);
        transition: all 0.2s ease;

        &:hover {
          border-color: #cbd5e1;
          box-shadow: 0 4px 12px rgba(15, 23, 42, 0.04);
        }

        .deck-item-top {
          display: flex;
          align-items: center;
          justify-content: space-between;
          margin-bottom: 8px;

          .item-meta-left {
            display: flex;
            align-items: center;
            gap: 8px;
            flex-wrap: wrap;

            .question-number {
              font-size: 13px;
              font-weight: 800;
              color: #475569;
            }

            .type-pill {
              font-weight: 600;
              border-radius: 6px;
            }

            .score-chip {
              font-size: 12px;
              font-weight: 700;
              color: #2563eb;
              background: #eff6ff;
              padding: 1px 8px;
              border-radius: 9999px;
            }

            .diff-tag {
              font-size: 11px;
              font-weight: 600;
              padding: 1px 7px;
              border-radius: 6px;

              &--easy {
                background: #ecfdf5;
                color: #059669;
              }

              &--medium {
                background: #eff6ff;
                color: #2563eb;
              }

              &--hard {
                background: #fef2f2;
                color: #dc2626;
              }
            }

            .kp-pill {
              display: inline-flex;
              align-items: center;
              gap: 4px;
              font-size: 11.5px;
              color: #64748b;
              background: #f1f5f9;
              padding: 2px 8px;
              border-radius: 6px;

              .el-icon {
                font-size: 12px;
              }
            }
          }
        }

        .deck-item-body {
          .question-stem-text {
            margin: 0;
            font-size: 13.5px;
            color: #1e293b;
            line-height: 1.6;
          }

          .question-options-preview {
            margin-top: 10px;
            padding-top: 10px;
            border-top: 1px dashed #f1f5f9;
            display: flex;
            flex-direction: column;
            gap: 6px;

            .preview-opt {
              display: flex;
              align-items: center;
              gap: 8px;
              font-size: 12.5px;
              color: #475569;

              .preview-opt-key {
                font-weight: 700;
                color: #64748b;
              }

              &--correct {
                color: #059669;
                font-weight: 600;

                .preview-opt-key {
                  color: #059669;
                }
              }
            }
          }
        }
      }

      .deck-pagination-row {
        margin-top: 14px;
        display: flex;
        justify-content: flex-start;
        padding-top: 12px;
        border-top: 1px dashed #e2e8f0;

        :deep(.el-pagination) {
          justify-content: flex-start;
          width: 100%;
        }
      }
    }

    .preview-empty-state {
      text-align: center;
      padding: 36px 20px;
      color: #94a3b8;

      .empty-icon-wrap {
        width: 52px;
        height: 52px;
        border-radius: 50%;
        background: #ffffff;
        border: 1px solid #e2e8f0;
        display: flex;
        align-items: center;
        justify-content: center;
        margin: 0 auto 12px;

        .el-icon {
          font-size: 24px;
          color: #94a3b8;
        }
      }

      .empty-title {
        font-size: 14px;
        font-weight: 700;
        color: #475569;
        margin: 0 0 4px;
      }

      .empty-desc {
        font-size: 12.5px;
        color: #94a3b8;
        margin: 0;
      }
    }
  }

  /* 规则网格 */
  .rules-card-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 16px;

    @media (max-width: 960px) {
      grid-template-columns: 1fr;
    }

    .rule-box {
      background: #f8fafc;
      border: 1.5px solid #e2e8f0;
      border-radius: 16px;
      padding: 18px 20px;
      display: flex;
      gap: 14px;
      transition: all 0.2s ease;

      &--active {
        background: #ffffff;
        border-color: #bfdbfe;
        box-shadow: 0 4px 16px rgba(37, 99, 235, 0.06);
      }

      .rule-icon-orb {
        width: 40px;
        height: 40px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;

        .el-icon {
          font-size: 20px;
        }

        &--blue {
          background: #eff6ff;
          color: #2563eb;
          border: 1px solid #bfdbfe;
        }

        &--amber {
          background: #fffbeb;
          color: #d97706;
          border: 1px solid #fde68a;
        }

        &--emerald {
          background: #ecfdf5;
          color: #059669;
          border: 1px solid #a7f3d0;
        }
      }

      .rule-content {
        flex: 1;
        min-width: 0;

        .rule-headline {
          display: flex;
          align-items: center;
          justify-content: space-between;
          gap: 8px;

          .rule-title {
            font-size: 14px;
            font-weight: 700;
            color: #1e293b;
          }
        }

        .rule-desc {
          margin: 6px 0 0;
          font-size: 12px;
          color: #64748b;
          line-height: 1.55;
        }
      }
    }
  }

  /* 底部悬浮停靠栏 */
  .actions-dock-card {
    background: #ffffff;
    border-radius: 18px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 4px 20px rgba(30, 80, 150, 0.05);
    padding: 16px 28px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 18px;
    flex-wrap: wrap;

    .dock-summary {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 13.5px;
      flex-wrap: wrap;

      .dock-summary-label {
        color: #64748b;
      }

      .dock-summary-name {
        font-weight: 700;
        color: #0f172a;
      }

      .dock-summary-stats {
        padding: 2px 10px;
        border-radius: 9999px;
        background: #f1f5f9;
        color: #475569;
        font-size: 12px;
        font-weight: 600;
      }
    }

    .dock-buttons {
      display: flex;
      align-items: center;
      gap: 12px;

      .dock-btn {
        display: inline-flex;
        align-items: center;
        gap: 7px;
        height: 42px;
        padding: 0 24px;
        border-radius: 9999px;
        font-size: 13.5px;
        font-weight: 600;
        cursor: pointer;
        outline: none;
        user-select: none;
        white-space: nowrap;
        transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

        &--cancel {
          background: #ffffff;
          border: 1.5px solid #cbd5e1;
          color: #475569;

          &:hover {
            background: #f8fafc;
            border-color: #94a3b8;
            color: #0f172a;
          }
        }

        &--primary {
          background: linear-gradient(135deg, #1677ff 0%, #2563eb 100%);
          border: none;
          color: #ffffff;
          box-shadow: 0 4px 14px rgba(22, 119, 255, 0.25);

          &:hover:not(:disabled) {
            transform: translateY(-1px);
            box-shadow: 0 6px 18px rgba(22, 119, 255, 0.35);
            background: linear-gradient(135deg, #0958d9 0%, #1d4ed8 100%);
          }

          &:disabled {
            opacity: 0.65;
            cursor: not-allowed;
          }
        }
      }
    }
  }
}
</style>
