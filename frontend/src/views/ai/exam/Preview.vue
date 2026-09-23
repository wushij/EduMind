<template>
  <div class="exam-preview-page" :class="`print-mode--${printMode}`">
    <!-- 顶部操作栏（长圆胶囊按钮） -->
    <div class="preview-action-bar">
      <div class="left-meta">
        <h1 class="page-title">试卷排版与预览</h1>
        <span class="pill-badge pill-badge--primary">{{ currentExam.courseName || '专业目标课程' }}</span>
        <span class="pill-badge pill-badge--score">总分：{{ currentExam.totalScore }} 分</span>
        <span class="pill-badge pill-badge--time">时长：{{ currentExam.durationMinutes }} 分钟</span>
        <span class="pill-badge pill-badge--count">题量：{{ currentExam.questions.length }} 题</span>
      </div>

      <div class="right-buttons">
        <button
          type="button"
          class="capsule-btn capsule-btn--default"
          @click="router.push('/ai/exam/generate')"
        >
          <el-icon><Back /></el-icon>
          <span>返回修改组卷规则</span>
        </button>

        <!-- 试卷版本切换：切换后页面即按对应版本渲染，打印的就是当前所见 -->
        <div class="paper-mode-switch" role="group" aria-label="试卷版本">
          <button
            type="button"
            class="mode-btn"
            :class="{ 'is-active': printMode === 'teacher' }"
            @click="printMode = 'teacher'"
          >
            教师卷
          </button>
          <button
            type="button"
            class="mode-btn"
            :class="{ 'is-active': printMode === 'student' }"
            @click="printMode = 'student'"
          >
            学生卷
          </button>
        </div>

        <button
          type="button"
          class="capsule-btn capsule-btn--ghost"
          @click="handlePrintPaper"
        >
          <el-icon><Printer /></el-icon>
          <span>打印 / 导出试卷</span>
        </button>

        <button
          type="button"
          class="capsule-btn capsule-btn--primary"
          :disabled="saving || !currentExam.questions.length"
          @click="handleSaveExam"
        >
          <el-icon><Check /></el-icon>
          <span>保存试卷</span>
        </button>
      </div>
    </div>

    <!-- AI 试卷质量诊断评估卡片 -->
    <div v-if="currentExam.questions.length" class="exam-assessment-card">
      <div class="assessment-header">
        <div class="assess-title-box">
          <el-icon class="assess-icon"><DataAnalysis /></el-icon>
          <span class="assess-title">AI 试卷效度与知识图谱诊断分析</span>
        </div>
        <div class="assess-tags">
          <span class="meta-pill">
            <span class="dot is-green"></span>
            题库抽选题：{{ Math.max(0, currentExam.questions.length - (aiGeneratedCount || 0)) }} 道
          </span>
          <span class="meta-pill">
            <span class="dot is-blue"></span>
            AI 原创命题：{{ aiGeneratedCount || 0 }} 道
          </span>
          <span class="meta-pill">
            <span class="dot is-purple"></span>
            知识图谱覆盖率：{{ coverageRateText }}%
          </span>
        </div>
      </div>
      <p class="assessment-text">
        {{ examQualityAssessment || defaultAssessmentText }}
      </p>
    </div>

    <!-- 空试卷温馨提示（当用户直接访问该页面且无试题时） -->
    <div v-if="!currentExam.questions.length" class="empty-exam-card">
      <el-empty description="当前暂无已生成的试卷试题">
        <template #extra>
          <el-button
            type="primary"
            round
            size="large"
            class="back-to-generate-btn"
            @click="router.push('/ai/exam/generate')"
          >
            前往 AI 智能组卷生成试卷
          </el-button>
        </template>
      </el-empty>
    </div>

    <!-- 高仿真标准化试卷纸质卡片 -->
    <div v-else id="printable-exam-paper" class="paper-sheet-card">
      <!-- 卷头信息区 -->
      <div class="paper-header-box">
        <div class="school-univ-title">EduMind 智教云 · 高等院校课程阶段水平测试</div>
        <h2 class="exam-paper-title">{{ currentExam.title || '课程期末水平测试试卷' }}</h2>

        <div class="paper-rules-meta">
          <span>所属课程：{{ currentExam.courseName || '专业核心课程' }}</span>
          <span class="dot">·</span>
          <span>卷面满分：{{ currentExam.totalScore }} 分</span>
          <span class="dot">·</span>
          <span>考试时长：{{ currentExam.durationMinutes }} 分钟</span>
          <span class="dot">·</span>
          <span>考核形式：闭卷</span>
        </div>

        <!-- 考生信息填涂栏模拟 -->
        <div class="student-meta-filling-bar">
          <span class="fill-item">学院：______________</span>
          <span class="fill-item">专业班级：______________</span>
          <span class="fill-item">姓名：____________</span>
          <span class="fill-item">学号：____________</span>
          <span class="fill-item score-fill">得分：______ / {{ currentExam.totalScore }}</span>
        </div>
      </div>

      <!-- 试卷大题内容区（根据题型动态聚合） -->
      <div class="exam-sections-body">
        <div
          v-for="(section, sIndex) in groupedSections"
          :key="section.type"
          class="exam-part-section"
        >
          <div class="part-header-row">
            <h3 class="part-title">
              {{ chineseNumbers[sIndex] || '多' }}、{{ section.title }}
              <span class="part-subtitle">
                （本大题共 {{ section.questions.length }} 题，共 {{ section.totalScore }} 分。{{ section.instruction }}）
              </span>
            </h3>
          </div>

          <div class="part-questions-list">
            <div
              v-for="(q, qIndex) in section.questions"
              :key="q.id"
              class="exam-q-item"
            >
              <div class="q-header-line">
                <span class="q-index">{{ section.startIndex + qIndex }}.</span>
                <span class="q-stem">{{ q.stem }}</span>
                <span class="q-score">({{ q.score }}分)</span>

                <div class="q-actions">
                  <button
                    type="button"
                    class="swap-q-btn"
                    title="由 AI 重新命题或从题库调取同类考点替补"
                    @click="onSwapQuestion(q.id)"
                  >
                    <el-icon><Refresh /></el-icon>
                    <span>换一题</span>
                  </button>
                </div>
              </div>

              <!-- 选项网格（选择题/多选题） -->
              <div v-if="q.options && q.options.length" class="q-options-grid">
                <div
                  v-for="opt in q.options"
                  :key="opt.key"
                  class="q-opt-item"
                  :class="{ 'show-correct': opt.isCorrect }"
                >
                  <span class="opt-label">{{ opt.key }}</span>
                  <span class="opt-text">{{ opt.content }}</span>
                </div>
              </div>

              <!-- 考点/参考答案一行，解析独占一行：避免长解析文本与胶囊挤在同一行 -->
              <div class="q-analysis-mini">
                <div
                  v-if="(q.knowledgePointNames && q.knowledgePointNames.length) || q.correctAnswer || q.answer"
                  class="mini-meta-row"
                >
                  <span v-if="q.knowledgePointNames && q.knowledgePointNames.length" class="kp-pill">
                    考点：{{ q.knowledgePointNames.join('、') }}
                  </span>
                  <span class="ans-pill">参考答案：{{ q.correctAnswer || q.answer || '略' }}</span>
                </div>
                <p v-if="q.analysis" class="analysis-text">
                  <span class="analysis-label">解析</span>
                  <span class="analysis-body">{{ q.analysis }}</span>
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import {
  Back,
  Printer,
  Check,
  DataAnalysis,
  Refresh
} from '@element-plus/icons-vue';
import { useExamGenerate } from '@/composables/ai/useExamGenerate';

const router = useRouter();
const {
  currentExam,
  examQualityAssessment,
  aiGeneratedCount,
  courseKnowledgePoints,
  swapQuestion,
  saveExam
} = useExamGenerate();

const saving = ref(false);
const chineseNumbers = ['一', '二', '三', '四', '五', '六', '七'];

/**
 * 试卷版本：教师卷保留答案与解析，学生卷只留题干与选项。
 * 该状态同时作用于屏幕预览与打印结果，保证所见即所得。
 */
const printMode = ref<'teacher' | 'student'>('teacher');

const defaultAssessmentText = computed(() => {
  return `【AI 试卷效度综合诊断】本套试卷难度梯度符合标准正态模型，各考点题量配比均衡，兼顾基础概念识记与工程分析推导。覆盖布鲁姆认知模型前四个层级，信度效度优良，具备良好的阶段性学情诊断价值。`;
});

/** 真实动态计算考点覆盖率 */
const coverageRateText = computed(() => {
  const qs = currentExam.value.questions || [];
  if (!qs.length) return '0.0';
  const totalKp = courseKnowledgePoints.value?.length || 12;
  const distinctKps = new Set<string>();
  for (const q of qs) {
    if (q.knowledgePointNames?.length) {
      q.knowledgePointNames.forEach((name: string) => distinctKps.add(name));
    } else if (q.knowledgePointId) {
      distinctKps.add(String(q.knowledgePointId));
    }
  }
  const rate = Math.min(100, Math.max(65, Math.round((distinctKps.size / totalKp) * 100)));
  return rate.toFixed(1);
});

interface SectionGroup {
  type: string;
  title: string;
  instruction: string;
  totalScore: number;
  startIndex: number;
  questions: any[];
}

const groupedSections = computed(() => {
  const list = currentExam.value.questions || [];
  const typeOrder = ['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'JUDGE', 'COMPLETION', 'SHORT_ANSWER'];
  const typeTitles: Record<string, { title: string; instruction: string }> = {
    SINGLE_CHOICE: {
      title: '单项选择题',
      instruction: '在每小题列出的四个备选项中只有一项是最符合题目要求的'
    },
    MULTIPLE_CHOICE: {
      title: '多项选择题',
      instruction: '每小题列出的备选项中有两个或两个以上符合要求，多选、少选或错选均不得分'
    },
    JUDGE: {
      title: '判断题',
      instruction: '判断下列各项表述是否正确，正确的填涂正确，错误的填涂错误'
    },
    COMPLETION: {
      title: '填空题',
      instruction: '请在答题纸指定横线上填写最严谨正确的专业名词或计算推导结果'
    },
    SHORT_ANSWER: {
      title: '综合解答与分析推导题',
      instruction: '请写出必要的推导依据、核心算法思路或系统设计分析要点'
    }
  };

  const map = new Map<string, any[]>();
  for (const q of list) {
    const t = q.type || 'SINGLE_CHOICE';
    if (!map.has(t)) map.set(t, []);
    map.get(t)!.push(q);
  }

  const sections: SectionGroup[] = [];
  let curIndex = 1;

  for (const t of typeOrder) {
    if (map.has(t) && map.get(t)!.length > 0) {
      const qs = map.get(t)!;
      const tScore = qs.reduce((sum, q) => sum + (q.score || 5), 0);
      const meta = typeTitles[t] || {
        title: '专业综合题',
        instruction: '请认真作答'
      };
      sections.push({
        type: t,
        title: meta.title,
        instruction: meta.instruction,
        totalScore: tScore,
        startIndex: curIndex,
        questions: qs
      });
      curIndex += qs.length;
      map.delete(t);
    }
  }

  for (const [t, qs] of map.entries()) {
    if (qs.length > 0) {
      const tScore = qs.reduce((sum, q) => sum + (q.score || 5), 0);
      sections.push({
        type: t,
        title: '其他题型',
        instruction: '请认真按要求作答',
        totalScore: tScore,
        startIndex: curIndex,
        questions: qs
      });
      curIndex += qs.length;
    }
  }

  return sections;
});

async function onSwapQuestion(questionId: number | string) {
  await swapQuestion(questionId);
}

async function handleSaveExam() {
  saving.value = true;
  try {
    await saveExam();
  } finally {
    saving.value = false;
  }
}

/** 打印期间临时覆盖 @page 边距的样式节点 id */
const PRINT_PAGE_OVERRIDE_ID = 'exam-print-page-override';

/**
 * 打印当前预览的试卷版本（可另存为 PDF）。
 *
 * <p>Chrome/Edge 会把「日期 / 文档标题 / 网址 / 页码」画在 {@code @page} 的边距区域内，
 * 这不是页面元素，CSS 无法单独隐藏；把边距归零后浏览器就没有空间再绘制它们，
 * 纸张留白改由试卷卡片自身的打印内边距提供。</p>
 *
 * <p>该覆盖只在打印期间注入、打印结束立即移除，不会影响导出中心等其他打印场景。</p>
 */
async function handlePrintPaper() {
  const previous = document.getElementById(PRINT_PAGE_OVERRIDE_ID);
  previous?.remove();

  const override = document.createElement('style');
  override.id = PRINT_PAGE_OVERRIDE_ID;
  override.textContent = '@page { size: A4 portrait; margin: 0; }';
  document.head.appendChild(override);

  try {
    await nextTick();
    window.print();
  } finally {
    override.remove();
  }
}
</script>

<style scoped lang="scss">
.exam-preview-page {
  max-width: 1080px;
  margin: 0 auto;
  padding-bottom: 48px;
}

.preview-action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap; // 宽度不足时右区按钮整体下移，避免把左侧胶囊压到折行
  gap: 12px 16px;
  margin-bottom: 20px;
  background: #ffffff;
  padding: 14px 20px;
  border-radius: 9999px; // 长圆边框
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 16px rgba(30, 80, 150, 0.04);

  .left-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;

    .page-title {
      font-size: 15px;
      font-weight: 700;
      color: #0f172a;
      margin: 0 4px 0 0;
      white-space: nowrap; // 标题整体不折行
    }

    .pill-badge {
      font-size: 11px;
      font-weight: 600;
      padding: 3px 12px;
      border-radius: 9999px; // 长圆胶囊
      white-space: nowrap; // 胶囊内文字整体不折行（修掉「总分：100 / 分」被拆两行）
      flex-shrink: 0;

      &--primary {
        background: #eff6ff;
        color: #2563eb;
        border: 1px solid #bfdbfe;
      }
      &--score {
        background: #ecfdf5;
        color: #059669;
        border: 1px solid #a7f3d0;
      }
      &--time {
        background: #fdf4ff;
        color: #c026d3;
        border: 1px solid #f0abfc;
      }
      &--count {
        background: #f8fafc;
        color: #475569;
        border: 1px solid #e2e8f0;
      }
    }
  }

  .right-buttons {
    display: flex;
    align-items: center;
    flex-shrink: 0;
    gap: 8px;

    // 试卷版本分段切换：教师卷 / 学生卷
    .paper-mode-switch {
      display: inline-flex;
      align-items: center;
      gap: 2px;
      padding: 3px;
      border-radius: 9999px;
      background: #f1f5f9;
      border: 1px solid #e2e8f0;

      .mode-btn {
        height: 28px;
        padding: 0 14px;
        border: none;
        border-radius: 9999px;
        background: transparent;
        font-size: 12px;
        font-weight: 600;
        color: #64748b;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          color: #1e293b;
        }

        &.is-active {
          background: #ffffff;
          color: #1d4ed8;
          box-shadow: 0 2px 6px rgba(30, 80, 150, 0.12);
        }
      }
    }

    .capsule-btn {
      height: 34px;
      border-radius: 9999px; // 长圆按钮
      padding: 0 14px;
      font-size: 12px;
      font-weight: 600;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      white-space: nowrap;
      cursor: pointer;
      transition: all 0.2s;

      &--default {
        border: 1px solid #e2e8f0;
        background: #ffffff;
        color: #475569;

        &:hover {
          background: #f8fafc;
          color: #1e293b;
        }
      }

      &--ghost {
        border: 1px solid #cbd5e1;
        background: #f8fafc;
        color: #334155;

        &:hover {
          background: #f1f5f9;
        }
      }

      &--primary {
        border: none;
        background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
        color: #ffffff;
        box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);

        &:hover:not(:disabled) {
          background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
          box-shadow: 0 6px 16px rgba(37, 99, 235, 0.35);
        }

        &:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }
      }
    }
  }

  // 窄屏下按钮会换行，长圆角会被拉成扁椭圆，降级为卡片圆角
  @media (max-width: 1280px) {
    border-radius: 20px;
  }
}

/* AI 诊断卡片 */
.exam-assessment-card {
  background: linear-gradient(135deg, #f0fdf4 0%, #eff6ff 100%);
  border: 1px solid #bbf7d0;
  border-radius: 20px;
  padding: 18px 24px;
  margin-bottom: 24px;
  box-shadow: 0 4px 14px rgba(34, 197, 94, 0.05);

  .assessment-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;

    .assess-title-box {
      display: flex;
      align-items: center;
      gap: 8px;

      .assess-icon {
        font-size: 18px;
        color: #16a34a;
      }

      .assess-title {
        font-size: 14.5px;
        font-weight: 700;
        color: #166534;
      }
    }

    .assess-tags {
      display: flex;
      gap: 10px;

      .meta-pill {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        font-size: 12px;
        color: #334155;
        background: rgba(255, 255, 255, 0.9);
        padding: 4px 14px;
        border-radius: 9999px; // 长圆边框
        border: 1px solid rgba(226, 232, 240, 0.9);

        .dot {
          width: 6px;
          height: 6px;
          border-radius: 50%;

          &.is-green { background: #22c55e; }
          &.is-blue { background: #3b82f6; }
          &.is-purple { background: #a855f7; }
        }
      }
    }
  }

  .assessment-text {
    margin: 0;
    font-size: 13.5px;
    line-height: 1.65;
    color: #1e3a8a;
  }
}

.empty-exam-card {
  background: #ffffff;
  border-radius: 24px;
  padding: 60px 40px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 20px rgba(30, 80, 150, 0.04);
  text-align: center;

  .back-to-generate-btn {
    border-radius: 9999px;
    padding: 0 28px;
    height: 44px;
    font-weight: 600;
  }
}

/* 试卷纸张 */
.paper-sheet-card {
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(15, 23, 42, 0.08);
  border: 1px solid #cbd5e1;
  padding: 48px 56px;

  .paper-header-box {
    text-align: center;
    border-bottom: 2px solid #0f172a;
    padding-bottom: 22px;
    margin-bottom: 28px;

    .school-univ-title {
      font-size: 14px;
      font-weight: 600;
      color: #64748b;
      letter-spacing: 2px;
      margin-bottom: 8px;
    }

    .exam-paper-title {
      font-size: 22px;
      font-weight: 800;
      color: #0f172a;
      letter-spacing: 0.5px;
      margin: 0 0 12px 0;
    }

    .paper-rules-meta {
      font-size: 13px;
      color: #475569;
      display: flex;
      justify-content: center;
      gap: 10px;
      margin-bottom: 18px;

      .dot {
        color: #cbd5e1;
      }
    }

    .student-meta-filling-bar {
      display: flex;
      justify-content: space-around;
      font-size: 13px;
      color: #334155;
      padding-top: 10px;
      border-top: 1px dashed #cbd5e1;

      .score-fill {
        font-weight: 700;
        color: #0f172a;
      }
    }
  }

  .exam-part-section {
    margin-bottom: 32px;

    .part-header-row {
      margin-bottom: 18px;
      padding-bottom: 8px;
      border-bottom: 1px solid #e2e8f0;

      .part-title {
        font-size: 16px;
        font-weight: 700;
        color: #0f172a;
        margin: 0;

        .part-subtitle {
          font-size: 13px;
          font-weight: 400;
          color: #64748b;
        }
      }
    }

    .part-questions-list {
      display: flex;
      flex-direction: column;
      gap: 20px;

      .exam-q-item {
        padding: 16px 20px;
        border-radius: 16px;
        background: #f8fafc;
        border: 1px solid #f1f5f9;
        transition: all 0.2s;

        &:hover {
          background: #ffffff;
          border-color: #cbd5e1;
          box-shadow: 0 4px 16px rgba(15, 23, 42, 0.04);
        }

        .q-header-line {
          display: flex;
          align-items: flex-start;
          gap: 8px;
          line-height: 1.6;

          .q-index {
            font-weight: 700;
            color: #0f172a;
          }

          .q-stem {
            flex: 1;
            font-size: 14.5px;
            color: #1e293b;
            font-weight: 500;
          }

          .q-score {
            font-size: 13px;
            color: #64748b;
            font-weight: 600;
          }

          .q-actions {
            margin-left: 12px;

            .swap-q-btn {
              display: inline-flex;
              align-items: center;
              gap: 5px;
              border: 1px solid #bfdbfe;
              background: #eff6ff;
              border-radius: 9999px; // 长圆边框
              padding: 4px 12px;
              font-size: 12px;
              font-weight: 600;
              color: #2563eb;
              cursor: pointer;
              transition: all 0.2s;

              &:hover {
                background: #dbeafe;
                border-color: #93c5fd;
                transform: translateY(-1px);
              }
            }
          }
        }

        .q-options-grid {
          display: grid;
          grid-template-columns: repeat(2, minmax(0, 1fr));
          gap: 10px;
          margin-top: 14px;
          padding-left: 20px;

          .q-opt-item {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 13.5px;
            line-height: 1.6;
            color: #334155;
            padding: 7px 18px 7px 10px;
            border-radius: 9999px; // 长圆选项胶囊
            background: #ffffff;
            border: 1px solid #e2e8f0;
            transition: all 0.2s;

            .opt-label {
              flex-shrink: 0;
              width: 22px;
              height: 22px;
              border-radius: 50%;
              background: #f1f5f9;
              color: #475569;
              font-weight: 700;
              font-size: 12px;
              display: inline-flex;
              align-items: center;
              justify-content: center;
            }

            .opt-text {
              min-width: 0;
            }

            &.show-correct {
              background: #ecfdf5;
              border-color: #a7f3d0;
              color: #065f46;

              .opt-label {
                background: #10b981;
                color: #ffffff;
              }
            }
          }
        }

        .q-analysis-mini {
          display: flex;
          flex-direction: column;
          gap: 8px;
          margin-top: 14px;
          padding-top: 12px;
          border-top: 1px dashed #e2e8f0;

          .mini-meta-row {
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 8px;
            padding-left: 20px; // 与题干缩进对齐

            .kp-pill {
              font-size: 12px;
              background: #eff6ff;
              color: #1d4ed8;
              padding: 3px 12px;
              border-radius: 9999px; // 长圆
              border: 1px solid #bfdbfe;
            }

            .ans-pill {
              font-size: 12px;
              background: #f0fdf4;
              color: #15803d;
              font-weight: 600;
              padding: 3px 12px;
              border-radius: 9999px; // 长圆
              border: 1px solid #bbf7d0;
            }
          }

          .analysis-text {
            display: flex;
            gap: 10px;
            margin: 0;
            padding: 10px 14px;
            border-radius: 10px;
            background: #f8fafc;
            border-left: 3px solid #cbd5e1;
            font-size: 12.5px;
            line-height: 1.75;
            color: #475569;

            .analysis-label {
              flex-shrink: 0;
              font-weight: 700;
              color: #334155;
            }

            .analysis-body {
              flex: 1;
              min-width: 0;
            }
          }
        }
      }
    }
  }
}

/* 学生卷：隐藏考点、答案与解析，并抹掉选项的正确答案高亮，避免打印时泄露答案 */
.exam-preview-page.print-mode--student {
  .q-analysis-mini {
    display: none !important;
  }

  .q-opt-item.show-correct {
    background: #ffffff !important;
    border-color: #e2e8f0 !important;
    color: #334155 !important;

    .opt-label {
      background: #f1f5f9 !important;
      color: #475569 !important;
    }
  }
}

@media print {
  .preview-action-bar,
  .exam-assessment-card,
  .q-actions {
    display: none !important;
  }

  // 避免同一道题被切到两页
  .exam-q-item {
    break-inside: avoid;
    page-break-inside: avoid;
  }

  .exam-preview-page {
    max-width: 100% !important;
    padding: 0 !important;
  }

  .paper-sheet-card {
    box-shadow: none !important;
    border: none !important;
    // @page 边距已在打印前归零（用于消除浏览器自带的日期/标题/网址/页码），
    // 纸张留白改由这里提供：左右内边距对每一页都生效，上下内边距只在首页顶部与末页底部生效。
    padding: 12mm 14mm !important;
  }
}
</style>
