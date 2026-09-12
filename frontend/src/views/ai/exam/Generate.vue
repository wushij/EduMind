<template>
  <div class="exam-generate-page">
    <!-- 顶部专属 3D 视觉大 Banner (ai组卷.png，比例 2508×627) -->
    <div class="exam-banner-stage">
      <div class="banner-ratio-box">
        <img
          class="banner-image"
          :src="examBannerImg"
          alt="AI 智能组卷"
          draggable="false"
        />
        <div class="banner-float-actions">
          <button type="button" class="capsule-back-btn" @click="router.push('/ai/marketplace')">
            <span>← 返回 AI 广场</span>
          </button>
        </div>
      </div>
    </div>

    <div class="mode-switch-bar">
      <el-radio-group v-model="composeMode">
        <el-radio-button label="quick">快速智能组卷</el-radio-button>
        <el-radio-button label="full">完整试卷编排</el-radio-button>
      </el-radio-group>
    </div>

    <div v-if="composeMode === 'quick'" class="config-section-card quick-compose-card">
      <h3 class="section-title">快速智能组卷</h3>
      <div class="form-grid-row">
        <div class="form-item-col">
          <label class="form-label">适用课程</label>
          <el-select v-model="examForm.courseId" size="large" class="w-100">
            <el-option
              v-for="c in displayCourses"
              :key="c.id"
              :label="`${c.code || ('CS' + c.id)} · ${c.title}`"
              :value="c.id"
            />
          </el-select>
        </div>
        <div class="form-item-col">
          <label class="form-label">抽题数量</label>
          <el-input-number v-model="quickCount" :min="5" :max="50" size="large" />
        </div>
      </div>
      <div v-if="composePreview" class="compose-preview">
        <el-alert
          :title="`知识点覆盖率 ${(composePreview.coverageRate * 100).toFixed(1)}% · 已选 ${composePreview.selectedCount} 题 · 覆盖 ${composePreview.distinctKnowledgePointCount} 个知识点`"
          type="success"
          :closable="false"
          show-icon
        />
      </div>
      <el-button type="primary" :loading="composing" @click="handleQuickCompose">
        启动快速组卷
      </el-button>
    </div>

    <!-- 试卷基本信息配置卡片 -->
    <div v-else class="config-section-card">
      <h3 class="section-title">1. 试卷基本信息</h3>
      <div class="form-grid-row">
        <div class="form-item-col">
          <label class="form-label">适用课程空间</label>
          <el-select
            v-model="examForm.courseId"
            size="large"
            class="w-100"
          >
            <el-option
              v-for="c in displayCourses"
              :key="c.id"
              :label="`${c.code || ('CS' + c.id)} · ${c.title}`"
              :value="c.id"
            />
          </el-select>
        </div>

        <div class="form-item-col form-item-col--wide">
          <label class="form-label">试卷名称</label>
          <el-input
            v-model="examForm.title"
            placeholder="例如：2026秋季学期高等数学期中统一水平测试卷"
            size="large"
          />
        </div>
      </div>

      <div class="form-grid-row meta-numbers-row">
        <div class="number-stepper-box">
          <span class="num-title">目标卷面总分</span>
          <div class="stepper-wrap">
            <input v-model.number="examForm.totalScore" type="number" class="capsule-num-input" />
            <span class="unit">分</span>
          </div>
        </div>

        <div class="number-stepper-box">
          <span class="num-title">考试建议时长</span>
          <div class="stepper-wrap">
            <input v-model.number="examForm.durationMinutes" type="number" class="capsule-num-input" />
            <span class="unit">分钟</span>
          </div>
        </div>

        <div class="number-stepper-box">
          <span class="num-title">合格通过线 (60%)</span>
          <div class="stepper-wrap">
            <span class="fixed-score">{{ Math.round(examForm.totalScore * 0.6) }}</span>
            <span class="unit">分</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 题型题量与分值配置卡片 -->
    <div v-if="composeMode === 'full'" class="config-section-card">
      <div class="section-header-flex">
        <div>
          <h3 class="section-title">2. 题型配比与分值精细规划</h3>
          <p class="section-desc">调节各题型题量与每题单价，确保右侧实时计算的总分严格对齐目标总分：</p>
        </div>

        <!-- 实时总分校验胶囊指示器 -->
        <div
          class="total-score-pill-indicator"
          :class="{ 'is-matched': isScoreMatched, 'is-mismatch': !isScoreMatched }"
        >
          <el-icon class="indicator-icon">
            <Check v-if="isScoreMatched" />
            <Warning v-else />
          </el-icon>
          <span class="indicator-text">
            当前规划总分：<strong>{{ calculatedTotalScore }}</strong> / {{ examForm.totalScore }} 分
          </span>
        </div>
      </div>

      <!-- 题型规则卡片网格 -->
      <div class="rules-table-list">
        <div
          v-for="(rule, idx) in examForm.rules"
          :key="rule.type"
          class="rule-row-card"
        >
          <div class="rule-type-col">
            <span class="rule-index">0{{ idx + 1 }}</span>
            <strong class="rule-name">{{ rule.label }}</strong>
          </div>

          <div class="rule-stepper-col">
            <span class="col-label">题量：</span>
            <div class="mini-stepper">
              <button
                type="button"
                class="step-btn"
                :disabled="rule.count <= 0"
                @click="rule.count--"
              >
                -
              </button>
              <span class="step-num">{{ rule.count }}</span>
              <button
                type="button"
                class="step-btn"
                :disabled="rule.count >= 50"
                @click="rule.count++"
              >
                +
              </button>
            </div>
            <span class="unit-text">题</span>
          </div>

          <div class="rule-stepper-col">
            <span class="col-label">每题分值：</span>
            <div class="mini-stepper">
              <button
                type="button"
                class="step-btn"
                :disabled="rule.scoreEach <= 1"
                @click="rule.scoreEach--"
              >
                -
              </button>
              <span class="step-num">{{ rule.scoreEach }}</span>
              <button
                type="button"
                class="step-btn"
                :disabled="rule.scoreEach >= 50"
                @click="rule.scoreEach++"
              >
                +
              </button>
            </div>
            <span class="unit-text">分</span>
          </div>

          <div class="rule-subtotal-col">
            <span class="subtotal-label">该大题小计：</span>
            <strong class="subtotal-val">{{ rule.count * rule.scoreEach }} 分</strong>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部生成按钮栏 -->
    <div v-if="composeMode === 'full'" class="launch-exam-footer">
      <button
        type="button"
        class="capsule-generate-btn"
        :disabled="generating || !isScoreMatched"
        @click="handleGenerateExam"
      >
        <span v-if="!generating" class="btn-inner-content">
          <el-icon><Lightning /></el-icon>
          <span>启动 AI 智能组卷并排版预览</span>
        </span>
        <span v-else>正在智能抽取题目并编排试卷...</span>
      </button>
      <p v-if="!isScoreMatched" class="mismatch-warning">
        <el-icon class="mr-1"><Warning /></el-icon>
        <span>需调整各题型分值，使得小计总和等于目标设定总分（当前相差 {{ Math.abs(calculatedTotalScore - examForm.totalScore) }} 分）</span>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Check, Warning, Lightning } from '@element-plus/icons-vue';
import { useExamGenerate } from '@/composables/ai/useExamGenerate';
import { composeSmartPaper } from '@/api/ai/paper-compose';
import type { SmartPaperComposeVO } from '@/types/ai/paper-compose';
import { getCourseList } from '@/api/course/course';
import { USE_MOCK } from '@/config/mock';
import { MOCK_COURSES } from '@/mock/courses';
import { normalizeQuestion } from '@/utils/question/normalize-question';
import { ElMessage } from 'element-plus';
import examBannerImg from '@/assets/images/ai组卷.png';

const router = useRouter();
const composeMode = ref<'quick' | 'full'>('quick');
const quickCount = ref(10);
const composing = ref(false);
const composePreview = ref<SmartPaperComposeVO | null>(null);

const {
  examForm,
  currentExam,
  generating,
  calculatedTotalScore,
  isScoreMatched,
  generateExam
} = useExamGenerate();

const courses = ref<any[]>([]);

onMounted(async () => {
  try {
    const res = await getCourseList({ page: 1, pageSize: 50 });
    if (res.data?.list?.length) {
      courses.value = res.data.list;
      if (!courses.value.some(c => c.id === examForm.courseId)) {
        examForm.courseId = courses.value[0].id;
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

async function handleGenerateExam() {
  await generateExam();
}

async function handleQuickCompose() {
  composing.value = true;
  try {
    const res = await composeSmartPaper({
      courseId: examForm.courseId,
      totalCount: quickCount.value
    });
    composePreview.value = res.data ?? null;
    if (!composePreview.value?.questions?.length) {
      ElMessage.warning('未抽到题目，请检查题库');
      return;
    }
    currentExam.value = {
      id: Date.now(),
      courseId: examForm.courseId,
      courseName: displayCourses.value.find((c) => c.id === examForm.courseId)?.title ?? '',
      title: examForm.title || '智能组卷试卷',
      semester: '',
      totalScore: composePreview.value.questions.reduce((sum: number, q: any) => sum + (q.score ?? 5), 0),
      durationMinutes: examForm.durationMinutes,
      passScore: 60,
      rules: [],
      questions: composePreview.value.questions.map((q: any) =>
        normalizeQuestion({
          ...q,
          courseId: examForm.courseId,
          score: q.score ?? 5
        })
      ),
      createdAt: new Date().toISOString().split('T')[0]
    };
    ElMessage.success('快速组卷成功');
    router.push('/ai/exam/preview');
  } catch {
    ElMessage.error('快速组卷失败');
  } finally {
    composing.value = false;
  }
}
</script>

<style scoped lang="scss">
.exam-generate-page {
  width: 100%;
  padding: 24px;
  background: #f8fafc;
  min-height: calc(100vh - 64px);
  box-sizing: border-box;

  // 顶部专属 3D 视觉大 Banner (比例 2508×627)
  .exam-banner-stage {
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
    }
  }

  .mode-switch-bar {
    margin-bottom: 16px;
  }

  .quick-compose-card {
    margin-bottom: 20px;

    .compose-preview {
      margin: 16px 0;
    }
  }

  // 配置卡片
  .config-section-card {
    background: #FFFFFF;
    border-radius: 20px;
    padding: 28px 32px;
    border: 1px solid #EBF1F7;
    box-shadow: 0 4px 20px rgba(30, 80, 150, 0.05);
    margin-bottom: 22px;

    .section-title {
      margin: 0 0 16px 0;
      font-size: 16.5px;
      font-weight: 700;
      color: #0F172A;
    }

    .section-header-flex {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 20px;

      .section-desc {
        margin: 0;
        font-size: 13px;
        color: #64748B;
      }

      .total-score-pill-indicator {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        height: 36px;
        padding: 0 16px;
        border-radius: 9999px; // 长圆指示条
        font-size: 13px;
        font-weight: 500;
        white-space: nowrap;

        &.is-matched {
          background: #ECFDF5;
          border: 1px solid #A7F3D0;
          color: #059669;

          strong {
            color: #059669;
            font-size: 15px;
          }
        }

        &.is-mismatch {
          background: #FFFBEB;
          border: 1px solid #FDE68A;
          color: #D97706;

          strong {
            color: #DC2626;
            font-size: 15px;
          }
        }
      }
    }

    .form-grid-row {
      display: flex;
      gap: 20px;
      margin-bottom: 18px;

      .form-item-col {
        flex: 1;

        &--wide {
          flex: 2;
        }

        .form-label {
          display: block;
          font-size: 13.5px;
          font-weight: 600;
          color: #334155;
          margin-bottom: 8px;
        }

        .w-100 {
          width: 100%;
        }
      }
    }

    .meta-numbers-row {
      background: #F8FAFC;
      border-radius: 16px;
      padding: 16px 20px;
      margin-bottom: 0;
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 20px;

      .number-stepper-box {
        display: flex;
        flex-direction: column;
        align-items: center;

        .num-title {
          font-size: 12.5px;
          color: #64748B;
          margin-bottom: 6px;
        }

        .stepper-wrap {
          display: flex;
          align-items: baseline;
          gap: 6px;

          .capsule-num-input {
            width: 70px;
            height: 36px;
            text-align: center;
            border-radius: 9999px;
            border: 1px solid #CBD5E1;
            font-size: 16px;
            font-weight: 700;
            color: #1677FF;
            background: #FFFFFF;
            outline: none;

            &:focus {
              border-color: #1677FF;
              box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.16);
            }
          }

          .fixed-score {
            font-size: 18px;
            font-weight: 700;
            color: #10B981;
          }

          .unit {
            font-size: 13px;
            color: #64748B;
          }
        }
      }
    }

    // 题型行卡片
    .rules-table-list {
      display: flex;
      flex-direction: column;
      gap: 12px;

      .rule-row-card {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 14px 20px;
        background: #F8FAFC;
        border-radius: 16px;
        border: 1px solid #EDF2F7;
        transition: all 0.2s;

        &:hover {
          background: #FFFFFF;
          border-color: #DBEAFE;
          box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
        }

        .rule-type-col {
          display: flex;
          align-items: center;
          gap: 12px;
          width: 180px;

          .rule-index {
            font-size: 12px;
            font-weight: 700;
            color: #94A3B8;
          }

          .rule-name {
            font-size: 14px;
            color: #0F172A;
          }
        }

        .rule-stepper-col {
          display: flex;
          align-items: center;
          gap: 8px;

          .col-label {
            font-size: 12.5px;
            color: #64748B;
          }

          .mini-stepper {
            display: inline-flex;
            align-items: center;
            background: #FFFFFF;
            border: 1px solid #CBD5E1;
            border-radius: 9999px; // 长圆步进器
            padding: 2px;

            .step-btn {
              width: 26px;
              height: 26px;
              border-radius: 50%;
              border: none;
              background: transparent;
              color: #475569;
              font-size: 14px;
              cursor: pointer;

              &:hover:not(:disabled) {
                background: #EAF3FF;
                color: #1677FF;
              }

              &:disabled {
                opacity: 0.3;
                cursor: not-allowed;
              }
            }

            .step-num {
              min-width: 32px;
              text-align: center;
              font-size: 13.5px;
              font-weight: 700;
              color: #1E293B;
            }
          }

          .unit-text {
            font-size: 12px;
            color: #94A3B8;
          }
        }

        .rule-subtotal-col {
          width: 140px;
          text-align: right;

          .subtotal-label {
            font-size: 11.5px;
            color: #94A3B8;
          }

          .subtotal-val {
            display: block;
            font-size: 15px;
            font-weight: 700;
            color: #1677FF;
          }
        }
      }
    }
  }

  // 底部按钮
  .launch-exam-footer {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;

    .capsule-generate-btn {
      width: 100%;
      max-width: 520px;
      height: 48px;
      border-radius: 9999px; // 长圆跑道
      background: linear-gradient(135deg, #1677FF 0%, #722ED1 100%);
      color: #FFFFFF;
      border: none;
      font-size: 15.5px;
      font-weight: 600;
      cursor: pointer;
      box-shadow: 0 4px 20px rgba(22, 119, 255, 0.35);
      transition: all 0.22s ease;

      .btn-inner-content {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 6px;
      }

      &:hover:not(:disabled) {
        transform: translateY(-2px);
        box-shadow: 0 8px 26px rgba(114, 46, 209, 0.45);
      }

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    }

    .mismatch-warning {
      margin: 0;
      font-size: 12.5px;
      color: #D97706;
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }
  }
}
</style>
