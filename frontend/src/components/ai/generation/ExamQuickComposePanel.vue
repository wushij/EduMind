<template>
  <div class="quick-compose-wrapper">
    <!-- 1. 基础考纲与课程定位卡片 -->
    <div class="panel-card config-card">
      <div class="card-header-bar">
        <div class="header-left">
          <span class="step-num-pill">01</span>
          <h3 class="card-title">课程考核范围与卷面指标</h3>
        </div>
        <!-- 真实题库存量长圆胶囊提示 -->
        <div class="bank-live-stats-pill">
          <span class="dot is-live"></span>
          <span>
            本课真实题库：单选 <strong>{{ bankTypeStats?.SINGLE_CHOICE || 0 }}</strong> ·
            多选 <strong>{{ bankTypeStats?.MULTIPLE_CHOICE || 0 }}</strong> ·
            判断 <strong>{{ bankTypeStats?.JUDGE || 0 }}</strong> ·
            解答 <strong>{{ bankTypeStats?.SHORT_ANSWER || 0 }}</strong> 题
          </span>
        </div>
      </div>

      <!-- 三列完全严格等高布局 (42px) -->
      <div class="form-grid-3">
        <div class="form-item-box">
          <label class="item-label">
            <span>目标专业课程</span>
            <span class="required-star">*</span>
          </label>
          <el-select
            v-model="examForm.courseId"
            class="w-100 round-select"
            placeholder="请选择目标课程"
            @change="handleCourseChange"
          >
            <el-option
              v-for="c in displayCourses"
              :key="c.id"
              :label="`${c.code || ('CS' + c.id)} · ${c.name || c.title}`"
              :value="c.id"
            >
              <div class="course-opt-item">
                <span class="course-name">{{ c.name || c.title }}</span>
                <span class="course-code-badge">{{ c.code || ('CS' + c.id) }}</span>
              </div>
            </el-option>
          </el-select>
        </div>

        <div class="form-item-box">
          <label class="item-label">
            <span>预定抽题总量</span>
          </label>
          <div class="capsule-stepper">
            <button
              type="button"
              class="step-circle-btn"
              :disabled="quickCountModel <= 5"
              @click="quickCountModel = Math.max(5, quickCountModel - 1)"
            >
              -
            </button>
            <div class="stepper-center-val">
              <input
                v-model.number="quickCountModel"
                type="number"
                min="5"
                max="50"
                class="num-input"
              />
              <span class="unit-text">道题</span>
            </div>
            <button
              type="button"
              class="step-circle-btn"
              :disabled="quickCountModel >= 50"
              @click="quickCountModel = Math.min(50, quickCountModel + 1)"
            >
              +
            </button>
          </div>
        </div>

        <div class="form-item-box">
          <label class="item-label">
            <span>卷面目标满分</span>
          </label>
          <div class="capsule-stepper">
            <button
              type="button"
              class="step-circle-btn"
              :disabled="quickTotalScoreModel <= 50"
              @click="quickTotalScoreModel = Math.max(50, quickTotalScoreModel - 10)"
            >
              -
            </button>
            <div class="stepper-center-val">
              <input
                v-model.number="quickTotalScoreModel"
                type="number"
                min="50"
                max="150"
                step="10"
                class="num-input"
              />
              <span class="unit-text">分</span>
            </div>
            <button
              type="button"
              class="step-circle-btn"
              :disabled="quickTotalScoreModel >= 150"
              @click="quickTotalScoreModel = Math.min(150, quickTotalScoreModel + 10)"
            >
              +
            </button>
          </div>
        </div>
      </div>

      <!-- 快捷规格推荐长圆选择条（独立成行，彻底解决高低错位问题） -->
      <div class="quick-preset-spec-row">
        <span class="spec-label">推荐规格一键设定：</span>
        <div class="spec-chips">
          <button
            type="button"
            class="spec-chip-btn"
            :class="{ active: quickCountModel === 10 && quickTotalScoreModel === 100 }"
            @click="applySpec(10, 100)"
          >
            随堂达标测验 (10题 · 100分)
          </button>
          <button
            type="button"
            class="spec-chip-btn"
            :class="{ active: quickCountModel === 15 && quickTotalScoreModel === 100 }"
            @click="applySpec(15, 100)"
          >
            阶段单元测试 (15题 · 100分)
          </button>
          <button
            type="button"
            class="spec-chip-btn"
            :class="{ active: quickCountModel === 20 && quickTotalScoreModel === 100 }"
            @click="applySpec(20, 100)"
          >
            期末标准综合卷 (20题 · 100分)
          </button>
          <button
            type="button"
            class="spec-chip-btn"
            :class="{ active: quickCountModel === 25 && quickTotalScoreModel === 120 }"
            @click="applySpec(25, 120)"
          >
            高阶拔高冲刺卷 (25题 · 120分)
          </button>
        </div>
      </div>

      <!-- 试卷名称自定义（长圆输入框） -->
      <div class="title-input-row">
        <label class="item-label">
          <span>试卷标题</span>
          <span class="hint-text">(将呈现在试卷卷头及学生答卷分析报告单中)</span>
        </label>
        <el-input
          v-model="examForm.title"
          placeholder="例如：2025-2026学年第二学期期末统一水平评估综合测试卷"
          clearable
          class="round-text-input"
        />
      </div>
    </div>

    <!-- 2. 难度分布正态梯度模型卡片 -->
    <div class="panel-card difficulty-card">
      <div class="card-header-bar">
        <div class="header-left">
          <span class="step-num-pill">02</span>
          <h3 class="card-title">难度梯度分布模型</h3>
        </div>
        <span class="sub-hint">算法将按照目标比例求解单选、多选与综合题难度矩阵</span>
      </div>

      <div class="difficulty-grid">
        <!-- 基础巩固型 -->
        <div
          class="diff-card-item"
          :class="{ active: difficultyModelBinding === 'FOUNDATION' }"
          @click="difficultyModelBinding = 'FOUNDATION'"
        >
          <div class="diff-card-top">
            <span class="diff-title">基础巩固型</span>
            <span class="ratio-pill">5 : 4 : 1</span>
          </div>
          <div class="diff-progress-bar">
            <span class="bar-seg is-easy" style="width: 50%" title="基础题 50%"></span>
            <span class="bar-seg is-medium" style="width: 40%" title="中等题 40%"></span>
            <span class="bar-seg is-hard" style="width: 10%" title="难题 10%"></span>
          </div>
          <p class="diff-desc">偏重核心概念与基础识记，适合单元阶段达标与学期初基线摸底。</p>
        </div>

        <!-- 均衡标准型 -->
        <div
          class="diff-card-item"
          :class="{ active: difficultyModelBinding === 'NORMAL' }"
          @click="difficultyModelBinding = 'NORMAL'"
        >
          <div class="diff-card-top">
            <span class="diff-title">均衡标准型</span>
            <span class="ratio-pill is-primary">3 : 5 : 2 (推荐)</span>
          </div>
          <div class="diff-progress-bar">
            <span class="bar-seg is-easy" style="width: 30%" title="基础题 30%"></span>
            <span class="bar-seg is-medium" style="width: 50%" title="中等题 50%"></span>
            <span class="bar-seg is-hard" style="width: 20%" title="难题 20%"></span>
          </div>
          <p class="diff-desc">符合课程大纲标准难度梯度，区分度良好，适合期中期末标准学业考核。</p>
        </div>

        <!-- 综合拔高型 -->
        <div
          class="diff-card-item"
          :class="{ active: difficultyModelBinding === 'ADVANCED' }"
          @click="difficultyModelBinding = 'ADVANCED'"
        >
          <div class="diff-card-top">
            <span class="diff-title">综合拔高型</span>
            <span class="ratio-pill is-purple">1 : 4 : 5</span>
          </div>
          <div class="diff-progress-bar">
            <span class="bar-seg is-easy" style="width: 10%" title="基础题 10%"></span>
            <span class="bar-seg is-medium" style="width: 40%" title="中等题 40%"></span>
            <span class="bar-seg is-hard" style="width: 50%" title="难题 50%"></span>
          </div>
          <p class="diff-desc">加大复杂综合分析与高阶推导演练，适合竞赛选拔与优生培优测评。</p>
        </div>
      </div>
    </div>

    <!-- 3. AI 专属命题智囊与提示词注入卡片 -->
    <div class="panel-card ai-strategy-card">
      <div class="card-header-bar">
        <div class="header-left">
          <span class="step-num-pill">03</span>
          <h3 class="card-title">AI 智囊命题策略与专属提示词</h3>
        </div>
        <span class="ai-sparkle-pill">
          <el-icon><MagicStick /></el-icon>
          大模型认知指导
        </span>
      </div>

      <div class="strategy-body">
        <div class="prompt-input-box">
          <label class="item-label">
            <span>教师专属命题指令 / 教学提示词要求</span>
            <span class="hint-text">(大模型将严格遵从指令优化题目情境、设问深度与考核偏好)</span>
          </label>
          <el-input
            v-model="examForm.promptDirective"
            type="textarea"
            :rows="3"
            placeholder="例如：请多结合实际工程落地案例，加强对异常边界处理与复杂推导分析的考查，避免简单死记硬背..."
            class="directive-textarea"
          />

          <!-- 快捷提示词长圆词签 -->
          <div class="prompt-quick-tags">
            <span class="tag-title">灵感标签：</span>
            <button
              v-for="tag in promptPresets"
              :key="tag"
              type="button"
              class="prompt-chip-btn"
              @click="appendDirective(tag)"
            >
              + {{ tag }}
            </button>
          </div>
        </div>

        <div class="ai-toggle-bar">
          <div class="toggle-info">
            <span class="toggle-title">题库不足时自动由 AI 原创命题补全</span>
            <span class="toggle-desc">当课程本地题库无法满足考点覆盖或难度配比时，自动激活大模型原创命题补齐</span>
          </div>
          <el-switch
            v-model="examForm.aiGenerateFillShortfall"
            size="large"
            active-text="启用 AI 补全"
          />
        </div>
      </div>
    </div>

    <!-- 4. 启动控制台（长圆跑道边框） -->
    <div class="launch-action-bar">
      <div class="launch-meta">
        <div class="meta-item">
          <span class="m-label">组卷规格：</span>
          <span class="m-val">{{ quickCountModel }} 题 · 卷面满分 {{ quickTotalScoreModel }} 分</span>
        </div>
        <div class="meta-item">
          <span class="m-label">难度梯度：</span>
          <span class="m-val">{{ difficultySummaryText }}</span>
        </div>
        <div class="meta-item">
          <span class="m-label">题目来源：</span>
          <span class="m-val">{{ examForm.aiGenerateFillShortfall ? '题库优先匹配 · 题量不足时 AI 原创补齐' : '仅使用当前课程题库题目' }}</span>
        </div>
      </div>

      <el-button
        type="primary"
        size="large"
        round
        class="launch-capsule-btn"
        :loading="composing"
        @click="$emit('quick-compose')"
      >
        <el-icon class="btn-icon"><Lightning /></el-icon>
        <span>生成试卷方案</span>
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Lightning, MagicStick } from '@element-plus/icons-vue';
import type { ExamFormState } from './exam-generate-types';

const props = defineProps<{
  examForm: ExamFormState;
  displayCourses: any[];
  composing: boolean;
  bankTypeStats?: Record<string, number>;
}>();

const emit = defineEmits<{
  (e: 'quick-compose'): void;
  (e: 'course-change', courseId: number): void;
}>();

const quickCountModel = defineModel<number>('quickCount', { required: true });
const quickTotalScoreModel = defineModel<number>('quickTotalScore', { required: true });
const difficultyModelBinding = defineModel<'FOUNDATION' | 'NORMAL' | 'ADVANCED'>('difficultyModel', { required: true });

const promptPresets = [
  '偏重实际工程与代码推导演练',
  '针对学生常见思维误区设计干扰项',
  '引入复杂高可用生产实战案例',
  '考查边界异常处理与容灾机制',
  '强化核心概念对比辨析与推导'
];

function appendDirective(tagText: string) {
  if (!props.examForm.promptDirective) {
    props.examForm.promptDirective = tagText;
  } else if (!props.examForm.promptDirective.includes(tagText)) {
    props.examForm.promptDirective += `；${tagText}`;
  }
}

function applySpec(count: number, score: number) {
  quickCountModel.value = count;
  quickTotalScoreModel.value = score;
}

function handleCourseChange(courseId: number) {
  emit('course-change', courseId);
}

const difficultySummaryText = computed(() => {
  switch (difficultyModelBinding.value) {
    case 'FOUNDATION':
      return '基础夯实型 (易 50% · 中 40% · 难 10%)';
    case 'ADVANCED':
      return '拔高进阶型 (易 10% · 中 40% · 难 50%)';
    case 'NORMAL':
    default:
      return '均衡标准型 (易 30% · 中 50% · 难 20%)';
  }
});
</script>

<style scoped lang="scss">
.quick-compose-wrapper {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.panel-card {
  background: #ffffff;
  border-radius: 24px;
  padding: 26px 32px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 20px rgba(30, 80, 150, 0.04);
  transition: all 0.25s ease;

  &:hover {
    box-shadow: 0 8px 28px rgba(30, 80, 150, 0.07);
    border-color: #cbd5e1;
  }
}

.card-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f1f5f9;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .step-num-pill {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      background: #eff6ff;
      color: #2563eb;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 13px;
      font-weight: 800;
    }

    .card-title {
      margin: 0;
      font-size: 17px;
      font-weight: 700;
      color: #0f172a;
    }
  }

  .bank-live-stats-pill {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 6px 16px;
    border-radius: 9999px; // 长圆边框
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    font-size: 12px;
    color: #475569;

    .dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #10b981;
      box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
    }

    strong {
      color: #2563eb;
      font-weight: 700;
    }
  }
}

.item-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13.5px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 8px;

  .required-star {
    color: #ef4444;
  }

  .hint-text {
    font-size: 12px;
    font-weight: 400;
    color: #94a3b8;
  }
}

/* 完美齐平的三列网格 */
.form-grid-3 {
  display: grid;
  grid-template-columns: 1.2fr 1fr 1fr;
  gap: 22px;
  align-items: flex-start;
}

.round-select {
  :deep(.el-select__wrapper) {
    height: 44px !important;
    border-radius: 9999px !important; // 长圆边框
    padding-left: 18px;
    box-shadow: 0 0 0 1px #cbd5e1 inset !important;

    &:hover {
      box-shadow: 0 0 0 1px #93c5fd inset !important;
    }
  }
}

/* 统一的高颜值长圆胶囊步进器 (44px) */
.capsule-stepper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 44px;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 9999px; // 长圆边框
  padding: 0 5px;
  transition: all 0.2s;

  &:hover {
    border-color: #93c5fd;
    background: #ffffff;
  }

  .step-circle-btn {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    border: none;
    background: #ffffff;
    color: #475569;
    font-size: 16px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
    transition: all 0.15s;

    &:hover:not(:disabled) {
      background: #eff6ff;
      color: #2563eb;
      transform: scale(1.06);
    }

    &:disabled {
      opacity: 0.35;
      cursor: not-allowed;
    }
  }

  .stepper-center-val {
    display: flex;
    align-items: baseline;
    gap: 4px;

    .num-input {
      width: 52px;
      border: none;
      background: transparent;
      text-align: center;
      font-size: 16px;
      font-weight: 700;
      color: #0f172a;
      outline: none;

      /* 隐藏原生 number 箭头 */
      &::-webkit-outer-spin-button,
      &::-webkit-inner-spin-button {
        -webkit-appearance: none;
        margin: 0;
      }
    }

    .unit-text {
      font-size: 13px;
      color: #64748b;
      font-weight: 500;
    }
  }
}

/* 独立一行的推荐规格栏（解决错位问题） */
.quick-preset-spec-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
  padding: 10px 16px;
  background: #f8fafc;
  border-radius: 9999px; // 长圆边框
  border: 1px solid #e2e8f0;
  flex-wrap: wrap;

  .spec-label {
    font-size: 12.5px;
    font-weight: 600;
    color: #475569;
  }

  .spec-chips {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;

    .spec-chip-btn {
      border: 1px solid #e2e8f0;
      background: #ffffff;
      padding: 4px 14px;
      border-radius: 9999px; // 长圆胶囊
      font-size: 12px;
      font-weight: 600;
      color: #64748b;
      cursor: pointer;
      transition: all 0.2s;

      &:hover:not(.active) {
        background: #f1f5f9;
        color: #1e293b;
      }

      &.active {
        background: #eff6ff;
        border-color: #93c5fd;
        color: #2563eb;
        box-shadow: 0 1px 4px rgba(37, 99, 235, 0.1);
      }
    }
  }
}

.round-text-input {
  :deep(.el-input__wrapper) {
    height: 44px !important;
    border-radius: 9999px !important; // 长圆边框
    padding-left: 18px;
  }
}

.course-opt-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;

  .course-name {
    font-weight: 600;
    color: #1e293b;
  }

  .course-code-badge {
    font-size: 11.5px;
    color: #64748b;
    background: #f1f5f9;
    padding: 2px 8px;
    border-radius: 9999px;
  }
}

.title-input-row {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px dashed #f1f5f9;
}

/* 难度卡片 */
.difficulty-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;

  .diff-card-item {
    border: 2px solid #f1f5f9;
    border-radius: 20px;
    padding: 20px;
    background: #ffffff;
    cursor: pointer;
    transition: all 0.25s ease;
    display: flex;
    flex-direction: column;
    gap: 12px;

    &:hover {
      border-color: #cbd5e1;
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(15, 23, 42, 0.05);
    }

    &.active {
      border-color: #3b82f6;
      background: #f8faff;
      box-shadow: 0 8px 24px rgba(59, 130, 246, 0.12);

      .diff-card-top .diff-title {
        color: #1d4ed8;
      }
    }

    .diff-card-top {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .diff-title {
        font-size: 15.5px;
        font-weight: 700;
        color: #1e293b;
      }

      .ratio-pill {
        font-size: 12px;
        font-weight: 700;
        padding: 3px 10px;
        border-radius: 9999px; // 长圆
        background: #f1f5f9;
        color: #475569;

        &.is-primary {
          background: #dbeafe;
          color: #1e40af;
        }

        &.is-purple {
          background: #f3e8ff;
          color: #6b21a8;
        }
      }
    }

    .diff-progress-bar {
      display: flex;
      height: 8px;
      border-radius: 9999px;
      overflow: hidden;
      background: #e2e8f0;

      .bar-seg {
        height: 100%;
        transition: width 0.3s;

        &.is-easy { background: #22c55e; }
        &.is-medium { background: #f59e0b; }
        &.is-hard { background: #ef4444; }
      }
    }

    .diff-desc {
      margin: 0;
      font-size: 12.5px;
      line-height: 1.5;
      color: #64748b;
    }
  }
}

/* AI 智囊卡片 */
.ai-strategy-card {
  .directive-textarea {
    :deep(.el-textarea__inner) {
      border-radius: 16px;
      border: 1px solid #e2e8f0;
      padding: 12px 16px;
      font-size: 13.5px;
      line-height: 1.6;
      transition: all 0.2s;

      &:focus {
        border-color: #3b82f6;
        box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
      }
    }
  }

  .prompt-quick-tags {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 10px;

    .tag-title {
      font-size: 12px;
      color: #64748b;
      font-weight: 600;
    }

    .prompt-chip-btn {
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      color: #334155;
      font-size: 12px;
      padding: 5px 14px;
      border-radius: 9999px; // 长圆胶囊
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        background: #e0f2fe;
        color: #0369a1;
        border-color: #7dd3fc;
        transform: translateY(-1px);
      }
    }
  }

  .ai-toggle-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 18px;
    padding-top: 18px;
    border-top: 1px solid #f1f5f9;

    .toggle-info {
      display: flex;
      flex-direction: column;
      gap: 3px;

      .toggle-title {
        font-size: 14px;
        font-weight: 600;
        color: #1e293b;
      }

      .toggle-desc {
        font-size: 12.5px;
        color: #94a3b8;
      }
    }
  }
}

/* 启动动作条 */
.launch-action-bar {
  background: #ffffff;
  border-radius: 24px;
  padding: 24px 32px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 10px 30px rgba(37, 99, 235, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;

  .launch-meta {
    display: flex;
    flex-direction: column;
    gap: 6px;

    .meta-item {
      font-size: 13px;

      .m-label { color: #64748b; }
      .m-val { color: #0f172a; font-weight: 600; }
    }
  }

  .launch-capsule-btn {
    height: 48px;
    padding: 0 36px;
    border-radius: 9999px; // 长圆边框
    font-size: 15.5px;
    font-weight: 700;
    letter-spacing: 0.3px;
    background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
    box-shadow: 0 6px 20px rgba(37, 99, 235, 0.35);
    border: none;
    transition: all 0.25s ease;

    .btn-icon {
      font-size: 17px;
      margin-right: 6px;
    }

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 10px 26px rgba(37, 99, 235, 0.45);
      background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
    }
  }
}
</style>
