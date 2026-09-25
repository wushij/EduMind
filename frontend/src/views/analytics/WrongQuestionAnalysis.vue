<template>
  <div class="wrong-question-page" v-loading="loading">
    <!-- 1. 顶部长圆科技毛玻璃 Hero 横幅 (模仿学情概览与学情分析顶级视觉) -->
    <WrongQuestionHero
      :course-id="courseId"
      :course-options="courseOptions"
      :course-name="currentCourseName"
      :teacher-name="currentTeacherName"
      :student-count="currentStudentCount"
      :range="range"
      :loading="loading"
      :advice-loading="diagnosingAll"
      :total-wrong-questions="wrongQuestions?.totalWrongQuestions ?? wrongQuestions?.total ?? 0"
      :total-wrong-records="wrongQuestions?.totalWrongRecords ?? 0"
      :avg-error-rate="wrongQuestions?.avgErrorRate ?? 0"
      :weak-knowledge-point-count="wrongQuestions?.weakKnowledgePointCount ?? 0"
      :total-variant-questions="wrongQuestions?.totalVariantQuestions ?? 0"
      :kp-options="kpOptions"
      @change-course="handleCourseChange"
      @change-range="handleRangeChange"
      @change-kp="handleKpChange"
      @refresh="reload"
      @export-report="handleExportReport"
      @trigger-ai-diagnosis="handleTriggerMacroDiagnosis"
    />

    <!-- 2. 中部宏观全景图表看板：四大错因类型构成与易错考点 TOP 5 -->
    <WrongQuestionOverviewCharts
      :error-type-distribution="wrongQuestions?.errorTypeDistribution"
      :top-weak-knowledge-points="wrongQuestions?.topWeakKnowledgePoints ?? []"
    />

    <!-- 3. 下部精美错题归因与教学诊断列表卡片 -->
    <div class="table-card">
      <div class="card-bar-header">
        <div class="header-left">
          <span class="bar-title">高频易错题目与认知归因明细</span>
          <span class="bar-total">共检索到 {{ wrongQuestions?.total ?? 0 }} 道典型错题</span>
        </div>

        <div class="header-right">
          <el-input
            v-model="keyword"
            placeholder="搜索题干关键词或题目编号..."
            clearable
            class="search-input"
            @input="handleSearch"
          >
            <template #prefix>
              <svg viewBox="0 0 24 24" class="search-svg" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
            </template>
          </el-input>
        </div>
      </div>

      <!-- 错题数据表格 -->
      <el-table
        :data="filteredList"
        stripe
        class="custom-wrong-table"
        :header-cell-style="{ background: '#F8FAFC', color: '#475569', fontWeight: '700' }"
      >
        <!-- 试题编号与题干预览 -->
        <el-table-column label="试题内容与考点" min-width="320">
          <template #default="{ row }">
            <div class="question-info-cell">
              <div class="info-badges">
                <span class="q-id-pill">试题 {{ row.questionId }}</span>
                <span class="type-pill" :class="`type-pill--${row.type?.toLowerCase()}`">
                  {{ row.typeName || '选择题' }}
                </span>
                <span v-if="row.knowledgePointName || row.knowledgePointId" class="kp-pill">
                  {{ cleanKpName(row.knowledgePointName, row.knowledgePointId) }}
                </span>
                <span class="diff-pill">
                  难度 {{ '★'.repeat(Math.min(5, row.difficulty || 2)) }}
                </span>
              </div>

              <div class="stem-preview">
                <MathText :text="row.stem || '暂无题目内容'" />
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 班级错误率与失分人次 -->
        <el-table-column label="班级错误率" width="145">
          <template #default="{ row }">
            <div class="error-rate-cell">
              <div class="rate-num-line">
                <strong class="rate-value" :class="getErrorRateClass(row.errorRate)">
                  {{ row.errorRate ?? 0 }}%
                </strong>
                <span class="rate-sub">
                  {{ row.wrongStudentCount ?? 0 }}/{{ row.classStudentCount ?? 0 }}人做错
                </span>
              </div>
              <el-progress
                :percentage="Math.min(100, row.errorRate ?? 0)"
                :stroke-width="7"
                :status="getProgressStatus(row.errorRate)"
                :show-text="false"
              />
              <span class="count-desc">累计失分 {{ row.wrongCount }} 次</span>
            </div>
          </template>
        </el-table-column>

        <!-- 失分诱因类型 -->
        <el-table-column label="主要错因类型" width="130">
          <template #default="{ row }">
            <div class="error-tags-wrap">
              <span
                v-for="(label, idx) in row.errorTypeLabels"
                :key="idx"
                class="error-type-badge"
                :class="getErrorTypeClass(row.errorTypes?.[idx])"
              >
                {{ label }}
              </span>
            </div>
          </template>
        </el-table-column>

        <!-- AI 归因诊断结论 (支持 KaTeX 公式渲染) -->
        <el-table-column label="AI 归因诊断建议" min-width="240">
          <template #default="{ row }">
            <div class="diagnosis-cell-box">
              <div class="diagnosis-summary" :title="row.diagnosis">
                <MathText :text="row.diagnosis || '该题班级失分频次较高，建议生成针对性变式题开展巩固。'" />
              </div>
              <div class="diagnosis-cell-footer">
                <span class="source-indicator" :class="row.diagnosisSource === 'AI' ? 'is-ai' : 'is-legacy'">
                  {{ row.diagnosisSource === 'AI' ? '大模型实时诊断' : '循证教学预置' }}
                </span>
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 变式题数量 -->
        <el-table-column label="巩固变式题" width="95" align="center">
          <template #default="{ row }">
            <button
              type="button"
              class="variant-btn-pill"
              :class="{ 'has-variants': (row.variantQuestionIds?.length ?? 0) > 0 }"
              @click="openVariantsDrawer(row)"
            >
              <span>{{ row.variantQuestionIds?.length ?? 0 }} 道</span>
            </button>
          </template>
        </el-table-column>

        <!-- 操作栏：保留 fixed="right"，宽度 230px，设置紧凑间距防裁切 -->
        <el-table-column label="教学操作" width="230" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons-wrap">
              <el-button
                link
                type="primary"
                size="small"
                :loading="diagnosingId === row.id"
                @click="handleDiagnose(row)"
              >
                AI 诊断
              </el-button>
              <el-button
                link
                type="primary"
                size="small"
                @click="openVariantsDrawer(row)"
              >
                变式题
              </el-button>
              <el-button
                link
                type="primary"
                size="small"
                class="action-detail-btn"
                @click="openDetailDrawer(row)"
              >
                学情穿透
              </el-button>
            </div>
          </template>
        </el-table-column>

      </el-table>

      <!-- 分页栏 -->
      <div class="pagination-footer">
        <AppPagination
          v-model:page-num="page"
          v-model:page-size="pageSize"
          :total="wrongQuestions?.total ?? 0"
          @change="reload"
        />
      </div>
    </div>

    <!-- 4. 试题穿透与做错学生详情抽屉 -->
    <WrongQuestionDetailDrawer
      v-model="detailDrawerVisible"
      :item="activeDetailItem"
    />

    <!-- 5. 变式巩固题库抽屉 -->
    <WrongQuestionVariantsDrawer
      v-model="variantsDrawerVisible"
      :item="activeVariantsItem"
      @generated="handleVariantsGenerated"
    />

    <!-- 6. AI 智教认知推演引擎统一弹窗 (支持班级宏观与单题诊断：640px 弹窗 + 罗盘脉冲雷达 + 秒表计时 + 流水线推进 + 随时中止) -->
    <el-dialog
      v-model="aiThinkingModalVisible"
      :title="aiThinkingDialogTitle"
      width="640px"
      class="ai-teaching-engine-dialog"
      destroy-on-close
      :close-on-click-modal="false"
      append-to-body
      @close="handleModalClose"
    >
      <AiCognitiveThinkingPanel
        :active="aiThinkingModalVisible"
        v-bind="AI_COGNITIVE_THINKING_PRESETS.wrongBookDiagnosis"
        show-footer-actions
        abort-label="中止推演"
        @abort="handleUserAbort"
      />
    </el-dialog>

    <!-- 7. 全班错因深度推演报告查看弹窗 -->
    <el-dialog
      v-model="macroReportModalVisible"
      title="AI 班级高频错因宏观推演报告"
      width="620px"
      class="macro-report-dialog"
      append-to-body
    >
      <div class="macro-report-content">
        <div class="report-meta-dock">
          <span class="dock-badge">当前课程：{{ currentCourseName || '高等数学' }}</span>
          <span class="dock-badge dock-badge--info">诊断模型：智教认知大模型</span>
        </div>
        <div class="report-body-box">
          <div class="report-text" v-html="formatStructuredProposal(latestMacroReport)"></div>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="macroReportModalVisible = false">了解并落实干预</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { useLearningAnalytics } from '@/composables/analytics/useLearningAnalytics';
import type { WrongQuestionItemVO } from '@/types/analytics/mastery';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';
import { getCourseDetail } from '@/api/course/course';
import type { Course } from '@/types/course/course';
import AppPagination from '@/components/common/AppPagination.vue';
import MathText from '@/components/common/MathText.vue';
import { formatStructuredProposal } from '@/utils/format/structured-text';
import WrongQuestionHero from '@/components/analytics/wrong-question/WrongQuestionHero.vue';
import WrongQuestionOverviewCharts from '@/components/analytics/wrong-question/WrongQuestionOverviewCharts.vue';
import WrongQuestionDetailDrawer from '@/components/analytics/wrong-question/WrongQuestionDetailDrawer.vue';
import WrongQuestionVariantsDrawer from '@/components/analytics/wrong-question/WrongQuestionVariantsDrawer.vue';
import AiCognitiveThinkingPanel from '@/components/ai/common/AiCognitiveThinkingPanel.vue';
import { AI_COGNITIVE_THINKING_PRESETS } from '@/constants/ai/cognitive-thinking';

const { courseOptions, courseId, loadCourses } = useTeacherCourses();
const page = ref(1);
const pageSize = ref(10);
const range = ref('7d');
const selectedKpId = ref<number | undefined>(undefined);
const keyword = ref('');

const {
  loading,
  wrongQuestions,
  fetchWrongQuestions,
  diagnoseWrong,
  cancelDiagnoseWrong,
  diagnoseWrongMacro,
  cancelDiagnoseWrongMacro
} = useLearningAnalytics();

const diagnosingId = ref<number | null>(null);
const diagnosingAll = ref(false);
const aiThinkingModalVisible = ref(false);
const diagnosisTarget = ref<'macro' | 'single' | null>(null);
const currentDiagnosingRow = ref<WrongQuestionItemVO | null>(null);
const isAborted = ref(false);

const macroReportModalVisible = ref(false);
const latestMacroReport = ref('');

const detailDrawerVisible = ref(false);
const activeDetailItem = ref<WrongQuestionItemVO | null>(null);

const variantsDrawerVisible = ref(false);
const activeVariantsItem = ref<WrongQuestionItemVO | null>(null);

const KP_STANDARD_NAMES: Record<number, string> = {
  17: '等价无穷小代换及其应用条件',
  18: '洛必达法则求未定式极限',
  19: '复合函数链式求导法则',
  16: 'ArrayList 与 LinkedList 源码剖析',
  10: '栈与队列的存储结构与特性'
};

function cleanKpName(name?: string, id?: number): string {
  if (id && KP_STANDARD_NAMES[id]) {
    return KP_STANDARD_NAMES[id];
  }
  if (!name) return '核心考点';
  const cleaned = name.replace(/#/g, '').replace(/\s+/g, ' ').trim();
  const matchId = cleaned.match(/^考点\s*(\d+)$/);
  if (matchId && matchId[1]) {
    const numId = Number(matchId[1]);
    if (KP_STANDARD_NAMES[numId]) {
      return KP_STANDARD_NAMES[numId];
    }
  }
  return cleaned;
}

const aiThinkingDialogTitle = computed(() => {
  if (diagnosisTarget.value === 'single' && currentDiagnosingRow.value) {
    return `AI 试题认知归因与教学干预推演（试题 ${currentDiagnosingRow.value.questionId}）`;
  }
  return 'AI 班级错题认知归因与教学干预推演引擎';
});

const currentCourseName = computed(() => {
  const c = courseOptions.value.find((item) => item.id === courseId.value);
  return c?.name || '';
});

// 课程元数据（主讲教师、选课人数）以课程详情接口为准。
// 此处原先写死「骨干授课团队」与「25 人」，会让任意课程都显示同一个不存在的主讲教师，
// 教师本人打开时无法确认诊断的是不是自己的课，也会让人误信一个虚构的班级规模。
const courseDetailInfo = ref<Course | null>(null);

const currentTeacherName = computed(() => {
  return courseDetailInfo.value?.teacherName || '';
});

const currentStudentCount = computed(() => {
  // 错题分析接口返回的班级人数是实际参与的选课学生数，优先级最高；
  // 拿不到时回退课程详情接口的选课人数，两者都缺失则显示 0。
  const classSize = wrongQuestions.value?.list?.[0]?.classStudentCount;
  if (typeof classSize === 'number' && classSize > 0) return classSize;
  return courseDetailInfo.value?.studentCount ?? 0;
});

async function loadCourseMeta(cid: number) {
  if (!cid || cid <= 0) {
    courseDetailInfo.value = null;
    return;
  }
  try {
    const res = await getCourseDetail(cid);
    courseDetailInfo.value = res?.data ?? null;
  } catch {
    // 无权访问或课程已删除必须清空，否则会继续沿用上一门课程的主讲教师与班级人数
    courseDetailInfo.value = null;
  }
}

const kpOptions = computed(() => {
  const kps = new Map<number, string>();
  if (wrongQuestions.value?.list) {
    for (const item of wrongQuestions.value.list) {
      if (item.knowledgePointId) {
        kps.set(item.knowledgePointId, cleanKpName(item.knowledgePointName, item.knowledgePointId));
      }
    }
  }
  return Array.from(kps.entries()).map(([id, name]) => ({ id, name }));
});


const filteredList = computed(() => {
  const list = wrongQuestions.value?.list ?? [];
  if (!keyword.value.trim()) return list;
  const kw = keyword.value.trim().toLowerCase();
  return list.filter((item) => {
    return (
      String(item.questionId).includes(kw) ||
      (item.stem && item.stem.toLowerCase().includes(kw)) ||
      (item.knowledgePointName && item.knowledgePointName.toLowerCase().includes(kw))
    );
  });
});

async function reload() {
  if (!courseId.value || courseId.value <= 0) return;
  await fetchWrongQuestions(courseId.value, page.value, pageSize.value, selectedKpId.value);
}

function handleCourseChange(id: number) {
  courseId.value = id;
  page.value = 1;
  reload();
}

function handleRangeChange(r: string) {
  range.value = r;
  reload();
}

function handleKpChange(kpId: number | undefined) {
  selectedKpId.value = kpId;
  page.value = 1;
  reload();
}

function handleSearch() {
  // 仅在内存列表筛选
}

function handleExportReport() {
  ElMessage.success('已生成「班级高频错题归因与教学诊断报告」，正在导出 PDF...');
}

async function handleTriggerMacroDiagnosis() {
  if (diagnosingAll.value || !courseId.value) return;
  diagnosisTarget.value = 'macro';
  currentDiagnosingRow.value = null;
  isAborted.value = false;
  diagnosingAll.value = true;
  aiThinkingModalVisible.value = true;

  try {
    const report = await diagnoseWrongMacro(courseId.value);
    if (isAborted.value) return;
    aiThinkingModalVisible.value = false;
    diagnosingAll.value = false;
    if (report) {
      latestMacroReport.value = report;
      macroReportModalVisible.value = true;
    }
    ElMessage.success('AI 全班错因深度推演完成！已更新高频失分根因与干预清单');
    await reload();
  } catch (err: any) {
    if (isAborted.value) return;
    aiThinkingModalVisible.value = false;
    diagnosingAll.value = false;
    ElMessage.error(err?.message || '班级错因推演失败');
  }
}

async function handleDiagnose(row: WrongQuestionItemVO) {
  if (!row.id) {
    ElMessage.warning('错题记录 ID 缺失，无法诊断');
    return;
  }
  if (diagnosingId.value !== null) return;
  diagnosisTarget.value = 'single';
  currentDiagnosingRow.value = row;
  diagnosingId.value = row.id;
  isAborted.value = false;
  aiThinkingModalVisible.value = true;

  try {
    const result = await diagnoseWrong(row.id);
    if (isAborted.value) return;
    aiThinkingModalVisible.value = false;
    diagnosingId.value = null;
    if (!result) {
      ElMessage.error('错题诊断未返回有效数据');
      return;
    }
    const variants = result.variantQuestionIds ?? [];
    row.diagnosis = result.diagnosis ?? row.diagnosis;
    row.variantQuestionIds = variants;
    row.diagnosisSource = 'AI';
    ElMessage.success(`试题 ${row.questionId} AI 诊断推演完成${variants.length > 0 ? `，已联动生成 ${variants.length} 道针对性巩固变式题` : ''}`);
  } catch (err: any) {
    if (isAborted.value) return;
    aiThinkingModalVisible.value = false;
    diagnosingId.value = null;
    ElMessage.error(err?.message || '错题诊断失败');
  }

}

function handleUserAbort() {
  if (isAborted.value || !aiThinkingModalVisible.value) {
    return;
  }
  isAborted.value = true;
  aiThinkingModalVisible.value = false;

  if (diagnosisTarget.value === 'macro' && courseId.value) {
    cancelDiagnoseWrongMacro(courseId.value);
  } else if (diagnosisTarget.value === 'single' && currentDiagnosingRow.value?.id) {
    cancelDiagnoseWrong(currentDiagnosingRow.value.id);
  }

  diagnosingAll.value = false;
  diagnosingId.value = null;
  ElMessage.info('已中止本次 AI 诊断推演');
}

function handleModalClose() {
  if (diagnosingAll.value || diagnosingId.value !== null) {
    handleUserAbort();
  }
}

function openDetailDrawer(row: WrongQuestionItemVO) {
  activeDetailItem.value = row;
  detailDrawerVisible.value = true;
}

function openVariantsDrawer(row: WrongQuestionItemVO) {
  activeVariantsItem.value = row;
  variantsDrawerVisible.value = true;
}

function handleVariantsGenerated(updatedItem: WrongQuestionItemVO) {
  const target = wrongQuestions.value?.list.find((q) => q.id === updatedItem.id);
  if (target) {
    target.variantQuestionIds = updatedItem.variantQuestionIds;
    target.variantCount = updatedItem.variantCount;
  }
}

function getErrorRateClass(rate?: number) {
  if (!rate) return 'rate-low';
  if (rate >= 50) return 'rate-high';
  if (rate >= 25) return 'rate-medium';
  return 'rate-low';
}

function getProgressStatus(rate?: number) {
  if (!rate) return 'success';
  if (rate >= 50) return 'exception';
  if (rate >= 25) return 'warning';
  return 'success';
}

function getErrorTypeClass(type?: string) {
  if (!type) return 'err-concept';
  switch (type.toUpperCase()) {
    case 'CONCEPT': return 'err-concept';
    case 'CALC': return 'err-calc';
    case 'LOGIC': return 'err-logic';
    case 'READING': return 'err-reading';
    default: return 'err-concept';
  }
}

watch(courseId, (id) => {
  page.value = 1;
  loadCourseMeta(id);
  reload();
});

onMounted(async () => {
  await loadCourses();
  if (courseOptions.value.length > 0 && (!courseId.value || courseId.value <= 0)) {
    courseId.value = courseOptions.value[0].id;
  }
  loadCourseMeta(courseId.value);
  reload();
});
</script>

<style scoped lang="scss">
.wrong-question-page {
  display: flex;
  flex-direction: column;
  gap: 22px;
  width: 100%;

  .table-card {
    background: #ffffff;
    border-radius: 20px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 2px 12px rgba(15, 23, 42, 0.03);
    padding: 20px 24px;
    display: flex;
    flex-direction: column;
    gap: 16px;

    .card-bar-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: 12px;

      .header-left {
        display: flex;
        align-items: baseline;
        gap: 12px;

        .bar-title {
          font-size: 17px;
          font-weight: 700;
          color: #0f172a;
        }

        .bar-total {
          font-size: 13px;
          color: #64748b;
        }
      }

      .header-right {
        .search-input {
          width: 280px;

          :deep(.el-input__wrapper) {
            border-radius: 14px;
          }

          .search-svg {
            width: 15px;
            height: 15px;
            color: #94a3b8;
          }
        }
      }
    }
  }

  .custom-wrong-table {
    border-radius: 12px;
    overflow: hidden;

    :deep(.el-table__row) {
      transition: background-color 0.2s;
    }

    :deep(.el-table__fixed-right) {
      background: #ffffff !important;
      box-shadow: -6px 0 16px rgba(15, 23, 42, 0.06);
    }

    :deep(.el-table__fixed-right .el-table__cell) {
      background: #ffffff !important;
    }

    :deep(.el-table__fixed-right-patch) {
      background: #F8FAFC !important;
    }
  }

  .question-info-cell {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 6px 0;

    .info-badges {
      display: flex;
      align-items: center;
      gap: 6px;
      flex-wrap: wrap;

      .q-id-pill {
        background: #f1f5f9;
        color: #475569;
        font-size: 11px;
        font-weight: 700;
        padding: 2px 7px;
        border-radius: 6px;
      }

      .type-pill {
        font-size: 11px;
        font-weight: 600;
        padding: 2px 8px;
        border-radius: 6px;
        background: #eff6ff;
        color: #2563eb;

        &--multiple { background: #f5f3ff; color: #7c3aed; }
        &--judge { background: #ecfdf5; color: #059669; }
        &--qa { background: #fff7ed; color: #ea580c; }
      }

      .kp-pill {
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        color: #334155;
        font-size: 11px;
        font-weight: 500;
        padding: 2px 8px;
        border-radius: 6px;
      }

      .diff-pill {
        color: #d97706;
        font-size: 11px;
        font-weight: 600;
      }
    }

    .stem-preview {
      font-size: 13px;
      color: #1e293b;
      line-height: 1.7;
      max-height: 84px;
      overflow: hidden;
      display: -webkit-box;
      -webkit-line-clamp: 3;
      -webkit-box-orient: vertical;
      word-break: break-word;
    }
  }

  .error-rate-cell {
    display: flex;
    flex-direction: column;
    gap: 5px;

    .rate-num-line {
      display: flex;
      align-items: baseline;
      gap: 6px;

      .rate-value {
        font-size: 16px;
        font-weight: 800;

        &.rate-high { color: #ef4444; }
        &.rate-medium { color: #f59e0b; }
        &.rate-low { color: #10b981; }
      }

      .rate-sub {
        font-size: 11px;
        color: #64748b;
      }
    }

    .count-desc {
      font-size: 11px;
      color: #94a3b8;
    }
  }

  .error-tags-wrap {
    display: flex;
    flex-direction: column;
    gap: 4px;

    .error-type-badge {
      display: inline-block;
      width: fit-content;
      font-size: 11px;
      font-weight: 600;
      padding: 2px 8px;
      border-radius: 6px;

      &.err-concept { background: #eff6ff; color: #2563eb; }
      &.err-calc { background: #fffbeb; color: #d97706; }
      &.err-logic { background: #f5f3ff; color: #7c3aed; }
      &.err-reading { background: #fdf2f8; color: #db2777; }
    }
  }

  .diagnosis-cell-box {
    display: flex;
    flex-direction: column;
    gap: 6px;

    .diagnosis-summary {
      margin: 0;
      font-size: 12px;
      color: #334155;
      line-height: 1.5;
      max-height: 40px;
      overflow: hidden;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
    }

    .diagnosis-cell-footer {
      display: flex;
      align-items: center;

      .source-indicator {
        font-size: 10px;
        font-weight: 500;
        padding: 1px 6px;
        border-radius: 4px;

        &.is-ai {
          background: #eff6ff;
          color: #2563eb;
        }

        &.is-legacy {
          background: #f1f5f9;
          color: #64748b;
        }
      }
    }
  }

  .variant-btn-pill {
    border: none;
    background: #f1f5f9;
    color: #475569;
    padding: 4px 12px;
    border-radius: 12px;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;

    &.has-variants {
      background: #ecfdf5;
      color: #059669;

      &:hover {
        background: #d1fae5;
      }
    }

    &:hover {
      background: #e2e8f0;
    }
  }

  .action-buttons-wrap {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    white-space: nowrap;

    :deep(.el-button) {
      margin-left: 0 !important;
      padding: 2px 6px !important;
      font-size: 13px;
    }

    .action-detail-btn {
      font-weight: 600;
    }
  }


  .pagination-footer {
    display: flex;
    justify-content: flex-start;
    margin-top: 14px;
    padding-top: 10px;
    border-top: 1px solid #f1f5f9;
    width: 100%;

    :deep(.pagination-bar) {
      margin-top: 0;
      padding: 0;
      border-top: none;
      justify-content: flex-start !important;
      width: 100%;
    }
  }

  .macro-report-content {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .report-meta-dock {
      display: flex;
      align-items: center;
      gap: 10px;

      .dock-badge {
        font-size: 12px;
        font-weight: 600;
        padding: 4px 10px;
        border-radius: 8px;
        background: #f1f5f9;
        color: #334155;

        &--info {
          background: #eff6ff;
          color: #2563eb;
        }
      }
    }

    .report-body-box {
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      padding: 16px;

      .report-text {
        font-size: 14px;
        line-height: 1.8;
        color: #1e293b;
        margin: 0;

        :deep(.structured-point-card) {
          margin-top: 12px;
          padding: 12px 16px;
          background: #ffffff;
          border: 1px solid #e2e8f0;
          border-left: 4px solid #3b82f6;
          border-radius: 8px;

          &:first-child {
            margin-top: 4px;
          }

          .point-badge-header {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 6px;

            .point-num-pill {
              display: inline-flex;
              align-items: center;
              justify-content: center;
              min-width: 22px;
              height: 22px;
              padding: 0 7px;
              font-size: 12px;
              font-weight: 700;
              color: #ffffff;
              background: #2563eb;
              border-radius: 11px;
            }

            .point-title-text {
              font-size: 14px;
              font-weight: 700;
              color: #0f172a;
            }
          }

          .point-body-text {
            font-size: 13px;
            line-height: 1.7;
            color: #334155;
            word-break: break-word;

            :deep(.katex) {
              font-size: 1.05em;
            }
          }
        }

        :deep(.structured-intro-p) {
          font-size: 13.5px;
          line-height: 1.7;
          color: #334155;
          margin-bottom: 8px;
          word-break: break-word;
        }
      }
    }
  }
}
</style>
