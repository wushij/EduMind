<template>
  <div class="question-generate-page">
    <!-- 顶部专属 3D 视觉大 Banner (ai智能出题.png，比例 2508×627) -->
    <div class="question-banner-stage">
      <div class="banner-ratio-box">
        <img
          class="banner-image"
          :src="questionBannerImg"
          alt="AI 智能出题"
          draggable="false"
        />
        <div class="banner-float-actions">
          <button type="button" class="capsule-back-btn" @click="router.push('/ai/marketplace')">
            <span>← 返回 AI 广场</span>
          </button>
        </div>

        <!-- 5 步向导步骤进度条 (嵌入 Banner 内部左下方，紧凑长圆跑道胶囊) -->
        <div class="banner-wizard-dock">
          <div
            v-for="step in steps"
            :key="step.index"
            class="wizard-step-item"
            :class="{
              active: currentStep === step.index,
              completed: currentStep > step.index
            }"
            @click="goToStep(step.index)"
          >
            <span class="step-num">
              <el-icon v-if="currentStep > step.index"><Check /></el-icon>
              <span v-else>{{ step.index }}</span>
            </span>
            <span class="step-name">{{ step.name }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 向导表单主体卡片 -->
    <div class="wizard-body-card">
      <!-- 步骤 1：选择课程 -->
      <div v-show="currentStep === 1" class="step-content-pane">
        <h3 class="pane-title">第 1 步：选择关联教学课程空间</h3>
        <p class="pane-desc">AI 将以所选课程的大纲知识树与已沉淀的课件知识库作为出题依据：</p>

        <div v-if="displayCourses.length > 0" class="courses-picker-grid">
          <div
            v-for="course in displayCourses"
            :key="course.id"
            class="course-picker-item"
            :class="{ active: formState.courseId === course.id }"
            @click="formState.courseId = course.id"
          >
            <div class="picker-header">
              <span class="course-code">{{ course.code || ('CS' + course.id) }}</span>
              <span v-if="formState.courseId === course.id" class="active-badge">
                <el-icon><Check /></el-icon> 已选
              </span>
            </div>
            <h4 class="course-name">{{ course.title }}</h4>
            <p class="course-meta">主讲：{{ course.teacherName || '任课教师' }} · {{ course.chapterCount || 6 }} 章节</p>
          </div>
        </div>
        <el-empty v-else description="暂无课程数据，请先创建课程" />
      </div>

      <!-- 步骤 2：选择考察章节与知识点范围 -->
      <div v-show="currentStep === 2" class="step-content-pane">
        <h3 class="pane-title">第 2 步：划定考察章节与核心考点范围</h3>
        <p class="pane-desc">多选需要考察的章节范围，AI 将对重点知识点进行题目覆盖：</p>

        <div class="chapters-selection-list">
          <label
            v-for="ch in currentCourseChapters"
            :key="ch.id"
            class="chapter-check-row"
            :class="{ checked: formState.chapterIds.includes(ch.id) }"
          >
            <input
              type="checkbox"
              :value="ch.id"
              :checked="formState.chapterIds.includes(ch.id)"
              class="hidden-checkbox"
              @change="toggleChapterSelect(ch.id)"
            />
            <div class="check-box-circle">
              <el-icon v-if="formState.chapterIds.includes(ch.id)"><Check /></el-icon>
            </div>
            <div class="chapter-label-col">
              <strong class="ch-title">{{ ch.title }}</strong>
              <span class="ch-desc">{{ ch.description }}</span>
            </div>
          </label>
        </div>

        <div class="kp-chips-section">
          <span class="kp-tips-title">
            <el-icon class="mr-1 text-amber-500"><Opportunity /></el-icon>
            重点命题知识点倾向标签（点击切换高优考察）：
          </span>
          <div class="kp-pills-row">
            <span
              v-for="kp in availableKnowledgePoints"
              :key="kp"
              class="pill-selectable-kp"
              :class="{ selected: formState.knowledgePointNames.includes(kp) }"
              @click="toggleKpSelect(kp)"
            >
              <span>{{ kp }}</span>
              <el-icon v-if="formState.knowledgePointNames.includes(kp)"><Check /></el-icon>
            </span>
          </div>
        </div>
      </div>

      <!-- 步骤 3：题型与难度设定 -->
      <div v-show="currentStep === 3" class="step-content-pane">
        <h3 class="pane-title">第 3 步：设定目标题型与难度系数</h3>
        <p class="pane-desc">可选择混合多种题型生成，或单一题型批量命题：</p>

        <div class="setting-group-box">
          <label class="setting-label">目标生成题型 (支持多选)</label>
          <div class="type-pills-selector">
            <div
              v-for="t in typeOptions"
              :key="t.type"
              class="type-pill-card"
              :class="{ active: formState.questionTypes.includes(t.type) }"
              @click="toggleTypeSelect(t.type)"
            >
              <el-icon class="type-icon" :style="{ color: t.color }">
                <component :is="t.icon" />
              </el-icon>
              <span class="type-label">{{ t.label }}</span>
              <el-icon v-if="formState.questionTypes.includes(t.type)" class="type-check">
                <Check />
              </el-icon>
            </div>
          </div>
        </div>

        <div class="setting-group-box">
          <label class="setting-label">整体考卷预期难度</label>
          <div class="diff-radios-row">
            <div
              v-for="d in difficultyOptions"
              :key="d.val"
              class="diff-pill-radio"
              :class="[d.colorClass, { active: formState.difficulty === d.val }]"
              @click="formState.difficulty = d.val"
            >
              <span class="diff-dot"></span>
              <span>{{ d.label }}</span>
              <span class="diff-desc">({{ d.desc }})</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 步骤 4：题量与分值配置 -->
      <div v-show="currentStep === 4" class="step-content-pane">
        <h3 class="pane-title">第 4 步：题量规模与单题分值</h3>
        <p class="pane-desc">配置本次生成题目的总数以及默认赋分标准：</p>

        <div class="stepper-config-grid">
          <div class="config-pill-card">
            <span class="config-title">生成题目总数量</span>
            <div class="stepper-controls">
              <button
                type="button"
                class="stepper-btn"
                :disabled="formState.count <= 1"
                @click="formState.count--"
              >
                -
              </button>
              <span class="stepper-val">{{ formState.count }} <small>道题目</small></span>
              <button
                type="button"
                class="stepper-btn"
                :disabled="formState.count >= 20"
                @click="formState.count++"
              >
                +
              </button>
            </div>
            <span class="config-tip">单次向导建议生成 3 ~ 10 道题目以保证大模型高思维链质量</span>
          </div>

          <div class="config-pill-card">
            <span class="config-title">每题参考建议分值</span>
            <div class="stepper-controls">
              <button
                type="button"
                class="stepper-btn"
                :disabled="formState.scorePerQuestion <= 1"
                @click="formState.scorePerQuestion--"
              >
                -
              </button>
              <span class="stepper-val">{{ formState.scorePerQuestion }} <small>分 / 题</small></span>
              <button
                type="button"
                class="stepper-btn"
                :disabled="formState.scorePerQuestion >= 50"
                @click="formState.scorePerQuestion++"
              >
                +
              </button>
            </div>
            <span class="config-tip">保存入库后依然可在试卷组装时微调单题分值</span>
          </div>
        </div>
      </div>

      <!-- 步骤 5：最终参数确认与生成 -->
      <div v-show="currentStep === 5" class="step-content-pane">
        <h3 class="pane-title">第 5 步：确认出题指令参数并启动生成</h3>
        <p class="pane-desc">请核对以下出题约束，点击按钮后 AI 将立即调用 DeepSeek 引擎进行题目编排：</p>

        <div class="summary-check-panel">
          <div class="summary-item">
            <span class="s-label">课程空间：</span>
            <strong class="s-val">{{ selectedCourseName }}</strong>
          </div>
          <div class="summary-item">
            <span class="s-label">包含章节：</span>
            <span class="s-val">已选择 {{ formState.chapterIds.length }} 个大章节</span>
          </div>
          <div class="summary-item">
            <span class="s-label">核心考点：</span>
            <span class="s-val">{{ formState.knowledgePointNames.join('、') }}</span>
          </div>
          <div class="summary-item">
            <span class="s-label">命题难度：</span>
            <strong class="s-val">{{ difficultyLabel }}</strong>
          </div>
          <div class="summary-item">
            <span class="s-label">生成规模：</span>
            <strong class="s-val">{{ formState.count }} 道题目（每题 {{ formState.scorePerQuestion }} 分）</strong>
          </div>
        </div>

        <div class="launch-generate-box">
          <button
            type="button"
            class="capsule-generate-btn"
            :disabled="generating"
            @click="handleGenerateSubmit"
          >
            <span v-if="!generating" class="btn-inner-content">
              <el-icon><Lightning /></el-icon>
              <span>启动 AI 智能出题（跳转卡片预览）</span>
            </span>
            <span v-else class="generating-text">
              <span class="spinner"></span> 正在运用 DeepSeek 深度命题与构建解析中...
            </span>
          </button>
        </div>
      </div>

      <!-- 底部步骤切换长圆按钮栏 -->
      <div class="wizard-footer-nav">
        <button
          type="button"
          class="capsule-nav-btn capsule-nav-btn--prev"
          :disabled="currentStep === 1"
          @click="prevStep"
        >
          <el-icon><ArrowLeft /></el-icon>
          <span>上一步</span>
        </button>

        <button
          v-if="currentStep < 5"
          type="button"
          class="capsule-nav-btn capsule-nav-btn--next"
          @click="nextStep"
        >
          <span>下一步</span>
          <el-icon><ArrowRight /></el-icon>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, markRaw } from 'vue';
import { useRouter } from 'vue-router';
import {
  Check,
  Opportunity,
  Lightning,
  ArrowLeft,
  ArrowRight,
  CircleCheck,
  Finished,
  ScaleToOriginal,
  EditPen,
  Document
} from '@element-plus/icons-vue';
import { useQuestionGenerate } from '@/composables/ai/useQuestionGenerate';
import { getCourseList } from '@/api/course/course';
import { USE_MOCK } from '@/config/mock';
import { MOCK_COURSES } from '@/mock/courses';
import { MOCK_CHAPTERS } from '@/mock/chapters';
import { QuestionType, Difficulty } from '@/mock/questions';
import questionBannerImg from '@/assets/images/ai智能出题.png';

const router = useRouter();
const { currentStep, generating, formState, nextStep, prevStep, generate } = useQuestionGenerate();

const courses = ref<any[]>([]);

onMounted(async () => {
  try {
    const res = await getCourseList({ page: 1, pageSize: 50 });
    if (res.data?.list?.length) {
      courses.value = res.data.list;
      if (!courses.value.some(c => c.id === formState.courseId)) {
        formState.courseId = courses.value[0].id;
      }
    } else if (USE_MOCK) {
      courses.value = [...MOCK_COURSES];
    } else {
      courses.value = [];
    }
  } catch {
    if (USE_MOCK) {
      courses.value = [...MOCK_COURSES];
    } else {
      courses.value = [];
    }
  }
});

const displayCourses = computed(() => {
  return courses.value.length ? courses.value : (USE_MOCK ? MOCK_COURSES : []);
});

const steps = [
  { index: 1, name: '选择课程' },
  { index: 2, name: '考察范围' },
  { index: 3, name: '题型难度' },
  { index: 4, name: '题量分值' },
  { index: 5, name: '确认生成' }
];

const typeOptions: { type: QuestionType; label: string; icon: any; color: string }[] = [
  { type: 'SINGLE_CHOICE', label: '单选题', icon: markRaw(CircleCheck), color: '#1677FF' },
  { type: 'MULTIPLE_CHOICE', label: '多选题', icon: markRaw(Finished), color: '#0284C7' },
  { type: 'TRUE_FALSE', label: '判断题', icon: markRaw(ScaleToOriginal), color: '#10B981' },
  { type: 'FILL_BLANK', label: '填空题', icon: markRaw(EditPen), color: '#F59E0B' },
  { type: 'SHORT_ANSWER', label: '简答分析题', icon: markRaw(Document), color: '#8B5CF6' }
];

const difficultyOptions: { val: Difficulty; label: string; desc: string; colorClass: string }[] = [
  { val: 'EASY', label: '简单', desc: '考查基础定义与概念识记', colorClass: 'diff-easy' },
  { val: 'MEDIUM', label: '中等', desc: '考查综合运用与定理推导', colorClass: 'diff-medium' },
  { val: 'HARD', label: '困难', desc: '考查难题辨析与复杂建模', colorClass: 'diff-hard' }
];

const availableKnowledgePoints = [
  '极限性质与保号性',
  '等价无穷小代换',
  '左右导数与连续性',
  '复合函数链式法则',
  '拉格朗日中值定理',
  '泰勒公式近似展开',
  '反常积分收敛判别',
  '循环队列队满判断'
];

const selectedCourseName = computed(() => {
  const found = displayCourses.value.find(c => c.id === formState.courseId);
  return found?.title || '未指定课程';
});

const currentCourseChapters = computed(() => {
  return MOCK_CHAPTERS[formState.courseId] || MOCK_CHAPTERS[101];
});

const difficultyLabel = computed(() => {
  const found = difficultyOptions.find(d => d.val === formState.difficulty);
  return found?.label || '中等难度';
});

function goToStep(idx: number) {
  if (idx < currentStep.value) {
    currentStep.value = idx;
  }
}

function toggleChapterSelect(id: number) {
  const idx = formState.chapterIds.indexOf(id);
  if (idx > -1) {
    formState.chapterIds.splice(idx, 1);
  } else {
    formState.chapterIds.push(id);
  }
}

function toggleKpSelect(kp: string) {
  const idx = formState.knowledgePointNames.indexOf(kp);
  if (idx > -1) {
    formState.knowledgePointNames.splice(idx, 1);
  } else {
    formState.knowledgePointNames.push(kp);
  }
}

function toggleTypeSelect(type: QuestionType) {
  const idx = formState.questionTypes.indexOf(type);
  if (idx > -1) {
    if (formState.questionTypes.length > 1) {
      formState.questionTypes.splice(idx, 1);
    }
  } else {
    formState.questionTypes.push(type);
  }
}

async function handleGenerateSubmit() {
  await generate();
}
</script>

<style scoped lang="scss">
.question-generate-page {
  width: 100%;
  padding: 24px;
  background: #f8fafc;
  box-sizing: border-box;

  // 顶部专属 3D 视觉大 Banner (比例 2508×627)
  .question-banner-stage {
    width: 100%;
    margin-bottom: 20px;

    .banner-ratio-box {
      position: relative;
      width: 100%;
      aspect-ratio: 2508 / 627;
      border-radius: 16px;
      overflow: hidden;
      box-shadow: 0 6px 24px rgba(22, 119, 255, 0.08);
      border: 1px solid #E2E8F0;

      .banner-image {
        position: absolute;
        inset: 0;
        width: 100%;
        height: 100%;
        object-fit: cover;
        display: block;
        user-select: none;
      }

      .banner-float-actions {
        position: absolute;
        top: 16px;
        right: 20px;
        z-index: 2;

        .capsule-back-btn {
          height: 34px;
          padding: 0 16px;
          border-radius: 9999px;
          background: rgba(255, 255, 255, 0.9);
          backdrop-filter: blur(8px);
          border: 1px solid rgba(226, 232, 240, 0.85);
          color: #334155;
          font-size: 13px;
          font-weight: 500;
          cursor: pointer;
          transition: all 0.2s;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

          &:hover {
            color: #1677FF;
            background: #FFFFFF;
            border-color: #93C5FD;
          }
        }
      }

      // 2. 嵌入 Banner 内部左下方的 5 步向导紧凑长圆跑道指示条
      .banner-wizard-dock {
          position: absolute;
          left: 4.27%;
          top: 83%;
          z-index: 2;
          display: inline-flex;
          align-items: center;
          gap: 4px;
          padding: 4px 6px;
          border-radius: 9999px;
          background: rgba(255, 255, 255, 0.94);
          backdrop-filter: blur(12px);
          -webkit-backdrop-filter: blur(12px);
          border: 1px solid rgba(255, 255, 255, 0.9);
          box-shadow: 0 4px 18px rgba(22, 119, 255, 0.12);

          .wizard-step-item {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 6px 14px;
            border-radius: 9999px;
            font-size: 13px;
            font-weight: 500;
            color: #64748B;
            cursor: pointer;
            transition: all 0.2s ease;
            white-space: nowrap;

            .step-num {
              width: 20px;
              height: 20px;
              border-radius: 50%;
              background: #F1F5F9;
              color: #475569;
              font-size: 11.5px;
              font-weight: 700;
              display: flex;
              align-items: center;
              justify-content: center;
              transition: all 0.2s ease;
            }

            &:hover:not(.active) {
              color: #1677FF;
              background: rgba(239, 246, 255, 0.6);
            }

            &.active {
              background: #1677FF;
              color: #FFFFFF;
              font-weight: 600;
              box-shadow: 0 2px 8px rgba(22, 119, 255, 0.28);

              .step-num {
                background: #FFFFFF;
                color: #1677FF;
              }
            }

            &.completed {
              color: #1677FF;

              .step-num {
                background: #EFF6FF;
                color: #1677FF;
              }
            }
          }
        }

        @media (max-width: 1400px) {
          .banner-wizard-dock {
            top: 81%;
            gap: 2px;
            padding: 3px 4px;

            .wizard-step-item {
              padding: 5px 10px;
              font-size: 12px;
              gap: 5px;

              .step-num {
                width: 18px;
                height: 18px;
                font-size: 11px;
              }
            }
          }
        }

        @media (max-width: 1100px) {
          .banner-wizard-dock {
            top: 79%;
            left: 4.27%;
            gap: 2px;
            padding: 2px 4px;

            .wizard-step-item {
              padding: 4px 8px;
              font-size: 11px;
              gap: 4px;

              .step-num {
                width: 16px;
                height: 16px;
                font-size: 10px;
              }
            }
          }
        }
      }
    }

  // 2. 主体卡片
  .wizard-body-card {
    background: #FFFFFF;
    border-radius: 20px;
    padding: 36px 40px;
    border: 1px solid #EBF1F7;
    box-shadow: 0 4px 20px rgba(30, 80, 150, 0.05);

    .step-content-pane {
      .pane-title {
        margin: 0 0 6px 0;
        font-size: 18px;
        font-weight: 700;
        color: #0F172A;
      }

      .pane-desc {
        margin: 0 0 24px 0;
        font-size: 13.5px;
        color: #64748B;
      }
    }

    // 步骤 1 课程网格
    .courses-picker-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 16px;

      .course-picker-item {
        border-radius: 16px;
        border: 2px solid #E2E8F0;
        padding: 16px 18px;
        cursor: pointer;
        transition: all 0.22s ease;

        .picker-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          margin-bottom: 8px;

          .course-code {
            font-size: 12px;
            font-weight: 700;
            color: #64748B;
            background: #F1F5F9;
            padding: 2px 8px;
            border-radius: 9999px;
          }

          .active-badge {
            font-size: 11px;
            font-weight: 600;
            color: #1677FF;
          }
        }

        .course-name {
          margin: 0 0 6px 0;
          font-size: 14.5px;
          font-weight: 700;
          color: #1E293B;
        }

        .course-meta {
          margin: 0;
          font-size: 12px;
          color: #94A3B8;
        }

        &:hover {
          border-color: #93C5FD;
          transform: translateY(-2px);
        }

        &.active {
          border-color: #1677FF;
          background: #F0F7FF;
        }
      }
    }

    // 步骤 2 章节选择
    .chapters-selection-list {
      display: flex;
      flex-direction: column;
      gap: 10px;
      margin-bottom: 24px;

      .chapter-check-row {
        display: flex;
        align-items: flex-start;
        gap: 12px;
        padding: 14px 18px;
        border-radius: 14px;
        border: 1px solid #E2E8F0;
        cursor: pointer;
        transition: all 0.2s;

        .hidden-checkbox {
          display: none;
        }

        .check-box-circle {
          width: 22px;
          height: 22px;
          border-radius: 50%;
          border: 2px solid #CBD5E1;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 12px;
          color: #FFFFFF;
          margin-top: 2px;
          flex-shrink: 0;
        }

        .chapter-label-col {
          .ch-title {
            font-size: 14px;
            color: #0F172A;
            display: block;
          }
          .ch-desc {
            font-size: 12px;
            color: #64748B;
            margin-top: 2px;
            display: block;
          }
        }

        &.checked {
          border-color: #1677FF;
          background: #F0F7FF;

          .check-box-circle {
            background: #1677FF;
            border-color: #1677FF;
          }
        }
      }
    }

    .kp-chips-section {
      padding-top: 14px;
      border-top: 1px solid #F1F5F9;

      .kp-tips-title {
        display: block;
        font-size: 13px;
        font-weight: 600;
        color: #334155;
        margin-bottom: 10px;
      }

      .kp-pills-row {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;

        .pill-selectable-kp {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          padding: 6px 14px;
          border-radius: 9999px; // 长圆药丸
          border: 1px solid #E2E8F0;
          background: #FFFFFF;
          color: #475569;
          font-size: 12.5px;
          cursor: pointer;
          transition: all 0.2s;

          &:hover {
            border-color: #93C5FD;
            color: #1677FF;
          }

          &.selected {
            border-color: #1677FF;
            background: #EAF3FF;
            color: #1677FF;
            font-weight: 600;
          }
        }
      }
    }

    // 步骤 3 题型与难度
    .setting-group-box {
      margin-bottom: 24px;

      .setting-label {
        display: block;
        font-size: 14px;
        font-weight: 600;
        color: #1E293B;
        margin-bottom: 12px;
      }

      .type-pills-selector {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
        gap: 12px;

        .type-pill-card {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 12px 18px;
          border-radius: 9999px; // 长圆跑道
          border: 1.5px solid #E2E8F0;
          cursor: pointer;
          transition: all 0.2s;

          .type-icon {
            font-size: 16px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
          }

          .type-label {
            font-size: 13.5px;
            font-weight: 500;
            color: #334155;
            flex: 1;
          }

          .type-check {
            color: #1677FF;
            font-weight: 700;
          }

          &.active {
            border-color: #1677FF;
            background: #F0F7FF;
            .type-label {
              color: #1677FF;
              font-weight: 600;
            }
          }
        }
      }

      .diff-radios-row {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 14px;

        .diff-pill-radio {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 12px 16px;
          border-radius: 9999px; // 长圆单选
          border: 1.5px solid #E2E8F0;
          cursor: pointer;
          transition: all 0.2s;
          font-size: 13.5px;
          color: #334155;

          .diff-dot {
            width: 10px;
            height: 10px;
            border-radius: 50%;
            background: #CBD5E1;
          }

          .diff-desc {
            font-size: 11px;
            color: #94A3B8;
          }

          &.active {
            border-color: #1677FF;
            background: #F0F7FF;
            color: #1677FF;
            font-weight: 600;

            .diff-dot {
              background: #1677FF;
            }
          }
        }
      }
    }

    // 步骤 4 步进器
    .stepper-config-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;

      .config-pill-card {
        border-radius: 18px;
        border: 1px solid #E2E8F0;
        background: #F8FAFC;
        padding: 24px;
        display: flex;
        flex-direction: column;
        align-items: center;

        .config-title {
          font-size: 14.5px;
          font-weight: 600;
          color: #0F172A;
          margin-bottom: 16px;
        }

        .stepper-controls {
          display: flex;
          align-items: center;
          gap: 16px;
          margin-bottom: 14px;

          .stepper-btn {
            width: 38px;
            height: 38px;
            border-radius: 50%;
            border: 1px solid #CBD5E1;
            background: #FFFFFF;
            font-size: 18px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s;

            &:hover:not(:disabled) {
              border-color: #1677FF;
              color: #1677FF;
            }

            &:disabled {
              opacity: 0.4;
              cursor: not-allowed;
            }
          }

          .stepper-val {
            font-size: 24px;
            font-weight: 700;
            color: #1677FF;
            min-width: 90px;
            text-align: center;

            small {
              font-size: 13px;
              color: #64748B;
              font-weight: 400;
            }
          }
        }

        .config-tip {
          font-size: 12px;
          color: #94A3B8;
          text-align: center;
        }
      }
    }

    // 步骤 5 最终确认
    .summary-check-panel {
      background: #F8FAFC;
      border-radius: 16px;
      padding: 20px 24px;
      border: 1px solid #E2E8F0;
      display: flex;
      flex-direction: column;
      gap: 12px;
      margin-bottom: 28px;

      .summary-item {
        display: flex;
        align-items: center;
        font-size: 14px;

        .s-label {
          color: #64748B;
          width: 90px;
          flex-shrink: 0;
        }

        .s-val {
          color: #0F172A;
        }
      }
    }

    .launch-generate-box {
      display: flex;
      justify-content: center;

      .capsule-generate-btn {
        width: 100%;
        max-width: 480px;
        height: 50px;
        border-radius: 9999px; // 长圆跑道
        background: linear-gradient(135deg, #1677FF 0%, #722ED1 100%);
        color: #FFFFFF;
        border: none;
        font-size: 16px;
        font-weight: 600;
        cursor: pointer;
        box-shadow: 0 4px 20px rgba(22, 119, 255, 0.35);
        transition: all 0.25s ease;

        &:hover:not(:disabled) {
          transform: translateY(-2px);
          box-shadow: 0 8px 28px rgba(114, 46, 209, 0.45);
        }

        &:disabled {
          opacity: 0.75;
          cursor: wait;
        }

        .generating-text {
          display: inline-flex;
          align-items: center;
          gap: 10px;

          .spinner {
            width: 18px;
            height: 18px;
            border: 2.5px solid rgba(255, 255, 255, 0.3);
            border-top-color: #FFFFFF;
            border-radius: 50%;
            animation: spin 0.8s linear infinite;
          }
        }
      }
    }

    // 底部导航
    .wizard-footer-nav {
      margin-top: 36px;
      padding-top: 24px;
      border-top: 1px solid #F1F5F9;
      display: flex;
      justify-content: space-between;
      align-items: center;

      .capsule-nav-btn {
        height: 42px;
        padding: 0 24px;
        border-radius: 9999px; // 长圆跑道
        font-size: 14px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s;

        &--prev {
          background: #FFFFFF;
          border: 1px solid #E2E8F0;
          color: #64748B;

          &:hover:not(:disabled) {
            color: #1E293B;
            border-color: #CBD5E1;
          }

          &:disabled {
            opacity: 0.4;
            cursor: not-allowed;
          }
        }

        &--next {
          background: #1677FF;
          border: none;
          color: #FFFFFF;
          display: inline-flex;
          align-items: center;
          gap: 6px;
          box-shadow: 0 2px 8px rgba(22, 119, 255, 0.25);

          &:hover {
            background: #4096FF;
            transform: translateY(-1px);
          }
        }
      }
    }
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
