<template>
  <div class="knowledge-mastery-page" v-loading="loading">
    <!-- 顶部长圆美学科技质感 Hero Banner (模仿学情概览与学情分析界面顶部) -->
    <KnowledgeMasteryHero
      :course-id="courseId"
      :course-options="courseOptions"
      :student-id="studentId"
      :student-list="masteryData?.students || []"
      :range="range"
      :loading="loading"
      :advice-loading="adviceLoading"
      :scope="masteryData?.scope || 'CLASS'"
      :total-points="heroMetrics.totalPoints"
      :class-avg-score="heroMetrics.classAvgScore"
      :mastered-count="heroMetrics.masteredCount"
      :warning-count="heroMetrics.warningCount"
      :student-count="heroMetrics.studentCount"
      :class-student-count="heroMetrics.classStudentCount"
      @change-course="handleCourseChange"
      @change-student="handleStudentChange"
      @change-range="handleRangeChange"
      @refresh="reload"
      @export-report="handleExportMatrix"
      @open-ai-diagnosis="handleOpenAiAdvice"
    />

    <!-- Mock 降级通知 (如未启用 Mock 则不显示) -->
    <div v-if="usedMockFallback" class="mock-fallback-pill">
      <svg viewBox="0 0 24 24" class="pill-svg" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10" />
        <line x1="12" y1="16" x2="12" y2="12" />
        <line x1="12" y1="8" x2="12.01" y2="8" />
      </svg>
      <span>当前数据由前端智能模拟引擎兜底呈现（服务接口离线或 VITE_USE_MOCK=true）</span>
    </div>

    <!-- 班级知识点全景掌握度热力矩阵 (V1.2 精准学情下钻) -->
    <KnowledgeHeatmap
      :course-id="courseId"
      :selected-student-id="studentId"
    />

    <!-- 下方核心透视看盘：对比雷达图 vs 薄弱考点攻坚榜 -->
    <div class="content-grid">
      <!-- 维度 1: 掌握度多维对比雷达图 -->
      <el-card shadow="never" class="dashboard-card radar-card">
        <template #header>
          <div class="card-header-flex">
            <div class="card-title-group">
              <span class="card-title-indicator" />
              <span class="card-title">{{ radarTitle }}</span>
            </div>
            <span class="card-subtitle">
              {{ studentId ? '紫色代表个人实测，绿色代表全班平均' : '映射考点知识图谱平均掌握度' }}
            </span>
          </div>
        </template>
        <KnowledgeRadar :data="masteryData" height="340px" />
      </el-card>

      <!-- 维度 2: 薄弱知识考点重点攻坚榜 -->
      <el-card shadow="never" class="dashboard-card weak-card">
        <template #header>
          <div class="card-header-flex">
            <div class="card-title-group">
              <span class="card-title-indicator indicator--danger" />
              <span class="card-title">待攻坚薄弱考点排行</span>
              <el-tag size="small" type="danger" effect="plain" class="weak-count-tag">
                {{ masteryData?.weakPoints?.length ?? 0 }} 处预警
              </el-tag>
            </div>
            <span class="card-subtitle">
              {{ masteryData?.scope === 'STUDENT' ? '该学员掌握度低于 70% 的考点集' : '全班掌握度低于 70% 的考点集' }}
            </span>
          </div>
        </template>

        <div v-if="masteryData?.weakPoints?.length" class="weak-list">
          <div
            v-for="item in masteryData.weakPoints"
            :key="item.knowledgePointId"
            class="weak-item"
          >
            <div class="weak-top">
              <div class="weak-title-wrap">
                <span class="weak-title">{{ item.title }}</span>
                <span v-if="item.chapterName" class="weak-chapter-tag">{{ item.chapterName }}</span>
              </div>
              <div class="weak-meta-right">
                <span class="weak-score-label">掌握率</span>
                <el-tag
                  :type="item.mastery < 0.55 ? 'danger' : 'warning'"
                  size="small"
                  class="font-bold"
                >
                  {{ Math.round(item.mastery <= 1.0 ? item.mastery * 100 : item.mastery) }}%
                </el-tag>
              </div>
            </div>

            <!-- 掌握度进度条 -->
            <div class="weak-progress-bar">
              <div
                class="progress-fill"
                :style="{
                  width: `${Math.round(item.mastery <= 1.0 ? item.mastery * 100 : item.mastery)}%`,
                  backgroundColor: item.mastery < 0.55 ? '#EF4444' : '#F59E0B'
                }"
              />
            </div>

            <p class="weak-suggestion">{{ item.suggestion }}</p>

            <div class="weak-bottom-bar">
              <div class="weak-stat-tags">
                <span v-if="item.wrongCount" class="stat-badge">
                  失分累积: <strong>{{ item.wrongCount }}</strong> 次
                </span>
                <span v-if="item.affectedStudentCount" class="stat-badge">
                  受影响: <strong>{{ item.affectedStudentCount }}</strong> 人
                </span>
                <span
                  class="stat-badge stat-badge--source"
                  :class="`source--${(item.dataConfidence || 'ESTIMATED').toLowerCase()}`"
                  :title="confidenceTitle(item)"
                >
                  {{ confidenceText(item) }}
                </span>
              </div>
              <button
                type="button"
                class="action-link-btn"
                @click="handleDispatchPractice(item)"
              >
                <span>定向推送变式题</span>
                <svg viewBox="0 0 24 24" class="arrow-svg" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M5 12h14M12 5l7 7-7 7" />
                </svg>
              </button>
            </div>
          </div>
        </div>
        <el-empty v-else description="全班各项核心考点掌握良好，暂无薄弱项" :image-size="80" />
      </el-card>
    </div>

    <!-- AI 智教认知推演引擎弹窗 (与其他模块完全对齐) -->
    <el-dialog
      v-model="aiThinkingModalVisible"
      title="AI 知识图谱考点教学质效评估推演引擎"
      width="640px"
      class="ai-teaching-engine-dialog"
      destroy-on-close
      :close-on-click-modal="false"
      :show-close="true"
      append-to-body
      @close="handleCloseAiAdvice"
    >
      <AiCognitiveThinkingPanel
        :active="aiThinkingModalVisible"
        v-bind="AI_COGNITIVE_THINKING_PRESETS.analyticsTeachingAdvice"
        show-footer-actions
        abort-label="中止推演"
        @abort="handleCloseAiAdvice"
      />
    </el-dialog>

    <!-- AI 学情诊断决策抽屉 (长圆药丸美学 + 靶向干预) -->
    <AiDiagnosisDrawer
      v-model="aiDrawerVisible"
      :advice="teachingAdvice"
      :mode="studentId ? 'personal' : 'overall'"
      :target-student-name="selectedStudentName"
      :weak-points="weakPointTitles"
      @dispatch-practice="handleDispatchPracticeFromAi"
      @clear-advice="handleClearAdvice"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import KnowledgeMasteryHero from '@/components/analytics/KnowledgeMasteryHero.vue';
import KnowledgeRadar from '@/components/analytics/KnowledgeRadar.vue';
import KnowledgeHeatmap from '@/components/analytics/KnowledgeHeatmap.vue';
import AiCognitiveThinkingPanel from '@/components/ai/common/AiCognitiveThinkingPanel.vue';
import AiDiagnosisDrawer from '@/components/analytics/AiDiagnosisDrawer.vue';
import { AI_COGNITIVE_THINKING_PRESETS } from '@/constants/ai/cognitive-thinking';
import { useLearningAnalytics } from '@/composables/analytics/useLearningAnalytics';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';
import type { WeakPointVO } from '@/types/analytics/mastery';

const router = useRouter();
const { courseOptions, courseId } = useTeacherCourses();
const studentId = ref<number | undefined>(undefined);
const range = ref<string>('30d');
const aiThinkingModalVisible = ref(false);
const aiDrawerVisible = ref(false);
let isThinkingAborted = false;
let isThinkingInProgress = false;

const {
  loading,
  adviceLoading,
  usedMockFallback,
  masteryData,
  teachingAdvice,
  fetchMastery,
  fetchTeachingAdvice,
  stopTeachingAdvice,
  clearTeachingAdvice
} = useLearningAnalytics();

const selectedStudentName = computed(() => {
  if (!studentId.value) return '';
  const stu = masteryData.value?.students?.find((s) => s.id === studentId.value);
  return stu?.name || '';
});

const weakPointTitles = computed(() => {
  return masteryData.value?.weakPoints?.map((w) => w.title) || [];
});

const radarTitle = computed(() => {
  if (!studentId.value) {
    return '班级核心知识维度掌握基准雷达';
  }
  const curStu = masteryData.value?.students?.find((s) => s.id === studentId.value);
  return curStu ? `【${curStu.name}】掌握度对比 (个人表现 vs 班级平均)` : '学员多维掌握度对比画像';
});

/** 后端字段缺失时的降级算法（正常情况下以后端返回为准） */
function computeAvgClassScore(): number {
  if (!masteryData.value?.classAvg?.length) return 0;
  const sum = masteryData.value.classAvg.reduce((a, b) => a + b, 0);
  return Math.round((sum / masteryData.value.classAvg.length) * 10) / 10;
}

function countMastered(): number {
  if (!masteryData.value?.classAvg?.length) return 0;
  return masteryData.value.classAvg.filter((v) => v >= 85).length;
}

/** 薄弱考点结论的可信度短标签：让教师一眼看出该结论有没有实测支撑 */
function confidenceText(item: WeakPointVO): string {
  const measured = item.measuredStudentCount ?? 0;
  const total = item.classStudentCount ?? 0;
  if (item.dataConfidence === 'MEASURED') return `实测可信 ${measured}/${total}`;
  if (item.dataConfidence === 'MIXED') return `部分实测 ${measured}/${total}`;
  return '暂无实测';
}

function confidenceTitle(item: WeakPointVO): string {
  const measured = item.measuredStudentCount ?? 0;
  const total = item.classStudentCount ?? 0;
  if (item.dataConfidence === 'MEASURED') {
    return `该考点已有 ${measured}/${total} 名学员产生真实测评记录，结论可信度高`;
  }
  if (item.dataConfidence === 'MIXED') {
    return `该考点仅 ${measured}/${total} 名学员有真实测评记录，其余分值由作业均分与错题记录推算`;
  }
  return '该考点尚无任何学员产生独立测评记录，当前掌握率完全由作业均分与错题记录推算，建议安排一次课内小测';
}

/**
 * 指标条口径。
 *
 * 修复要点：
 * 1. 不再用 `||` 兜底，`0` 是合法统计结果（例如「精熟考点 0 个」），
 *    历史实现会把 0 误判为「无数据」而切换到本地重算，导致指标条与榜单互相打架；
 * 2. 聚焦学员时切换为个人口径，与后端 weakPoints 榜单同源，
 *    避免出现「指标条说 3 个薄弱考点、右侧榜单却说 0 处预警」。
 */
const heroMetrics = computed(() => {
  const data = masteryData.value;
  const isStudentScope = data?.scope === 'STUDENT';
  const scopeAvg = isStudentScope ? data?.focusAvgMastery : data?.classAvgMastery;
  return {
    totalPoints: data?.totalKnowledgePoints ?? data?.dimensions?.length ?? 0,
    classAvgScore: scopeAvg || computeAvgClassScore(),
    masteredCount: data?.masteredCount ?? countMastered(),
    warningCount: data?.warningCount ?? data?.weakPoints?.length ?? 0,
    studentCount: data?.studentCount ?? data?.students?.length ?? 0,
    classStudentCount: data?.classStudentCount ?? data?.students?.length ?? 0
  };
});

async function reload() {
  if (!courseId.value || courseId.value <= 0) return;
  await fetchMastery(courseId.value, studentId.value);
}

function handleCourseChange(newCourseId: number) {
  courseId.value = newCourseId;
  studentId.value = undefined;
  reload();
}

function handleStudentChange(newStudentId?: number) {
  studentId.value = newStudentId;
  reload();
}

function handleRangeChange(newRange: string) {
  range.value = newRange;
  reload();
}

async function handleOpenAiAdvice() {
  if (!courseId.value || courseId.value <= 0) {
    ElMessage.warning('请先选择需要诊断评估的课程');
    return;
  }
  isThinkingAborted = false;
  isThinkingInProgress = true;
  aiThinkingModalVisible.value = true;

  try {
    const focusKpIds = masteryData.value?.weakPoints?.map((w) => w.knowledgePointId) || [];
    const fetchPromise = fetchTeachingAdvice({
      courseId: courseId.value,
      studentId: studentId.value,
      focusKnowledgePointIds: focusKpIds.length ? focusKpIds : undefined
    });

    // 保持沉浸式推演流水线动画体验 (至少 1.8 秒)
    const [advice] = await Promise.all([
      fetchPromise,
      new Promise((resolve) => setTimeout(resolve, 1800))
    ]);

    if (isThinkingAborted || !advice) {
      isThinkingInProgress = false;
      return;
    }

    // 标记正常推演完成，避免 el-dialog 的 @close 事件触发误报
    isThinkingInProgress = false;
    aiThinkingModalVisible.value = false;

    ElMessage.success({
      message: studentId.value
        ? `已成功生成针对学员「${selectedStudentName.value || '个体'}」的精准考点诊断建议！`
        : '已成功生成全班知识考点教学质效评估决策报告！',
      duration: 3000
    });
    aiDrawerVisible.value = true;
  } catch {
    isThinkingInProgress = false;
    if (!isThinkingAborted) {
      aiThinkingModalVisible.value = false;
      ElMessage.error('生成考点教学质效诊断建议失败，请稍后重试');
    }
  }
}

function handleCloseAiAdvice() {
  if (isThinkingInProgress) {
    isThinkingInProgress = false;
    isThinkingAborted = true;
    stopTeachingAdvice();
    aiThinkingModalVisible.value = false;
    ElMessage.info('已中止本次 AI 教学质效推演');
  }
}

function handleClearAdvice() {
  if (courseId.value) {
    clearTeachingAdvice(courseId.value, studentId.value || null);
    ElMessage.success('已清空当前 AI 诊断策略缓存');
  }
}

function handleDispatchPracticeFromAi(topic?: string) {
  aiDrawerVisible.value = false;
  ElMessage.success(`已为知识考点【${topic || '薄弱项'}】启动自适应定向变式训练计划`);
  const query: Record<string, string> = {
    mode: 'WEAK_POINT',
    courseId: String(courseId.value || '')
  };
  // 聚焦某位学员时同步带上学生 ID，练习页才能按人下发
  if (studentId.value) {
    query.studentId = String(studentId.value);
  }
  router.push({ path: '/learning/practice', query });
}

function handleDispatchPractice(item: WeakPointVO) {
  ElMessage.success(`已为考点【${item.title}】启动自适应定向变式题训练计划`);
  const query: Record<string, string> = {
    mode: 'WEAK_POINT',
    courseId: String(courseId.value || ''),
    knowledgePointId: String(item.knowledgePointId)
  };
  if (studentId.value) {
    query.studentId = String(studentId.value);
  }
  router.push({ path: '/learning/practice', query });
}

function handleExportMatrix() {
  const data = masteryData.value;
  if (!data?.dimensions?.length) {
    ElMessage.warning('暂无考点掌握度数据可导出');
    return;
  }

  // 导出内容与页面口径保持一致，并显式标注每个考点的实测支撑度，
  // 避免教师把推算出的 72% 当成真实测评成绩
  const weakByTitle = new Map((data.weakPoints || []).map((w) => [w.title, w]));
  const scopeLabel = data.scope === 'STUDENT' ? '聚焦学员掌握率(%)' : '班级平均掌握率(%)';
  const headers = ['知识考点', scopeLabel, '数据可信度', '实测支撑(人/总)'];
  const rows = data.dimensions.map((dim, i) => {
    const avg = data.classAvg?.[i] ?? 0;
    const weak = weakByTitle.get(dim);
    const confidence = weak ? confidenceText(weak) : '掌握良好';
    const measured = weak?.measuredStudentCount ?? 0;
    const total = weak?.classStudentCount ?? data.studentCount ?? 0;
    return `"${dim}",${avg},"${confidence}","${measured}/${total}"`;
  });
  const csvContent = '\uFEFF' + [headers.join(','), ...rows].join('\n');
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.setAttribute('href', url);
  link.setAttribute('download', `课程考点掌握度全景矩阵_${courseId.value || 'course'}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  ElMessage.success('已导出知识点全景掌握度报表');
}

watch(courseId, () => {
  reload();
});

onMounted(() => {
  reload();
});
</script>

<style scoped lang="scss">
.knowledge-mastery-page {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .mock-fallback-pill {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 6px 14px;
    border-radius: 20px;
    background: #EFF6FF;
    border: 1px solid #BFDBFE;
    color: #1D4ED8;
    font-size: 12px;
    font-weight: 500;
    align-self: flex-start;

    .pill-svg {
      width: 14px;
      height: 14px;
      flex-shrink: 0;
    }
  }

  .content-grid {
    display: grid;
    grid-template-columns: 1.15fr 0.85fr;
    gap: 20px;

    @media (max-width: 1024px) {
      grid-template-columns: 1fr;
    }
  }

  .dashboard-card {
    border-radius: 16px;
    border: 1px solid #E2E8F0;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);

    :deep(.el-card__header) {
      padding: 16px 20px;
      border-bottom: 1px solid #F1F5F9;
    }

    :deep(.el-card__body) {
      padding: 20px;
    }
  }

  .card-header-flex {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;

    .card-title-group {
      display: flex;
      align-items: center;
      gap: 8px;

      .card-title-indicator {
        width: 4px;
        height: 16px;
        border-radius: 2px;
        background: #2563EB;

        &--danger {
          background: #EF4444;
        }
      }

      .card-title {
        font-size: 15px;
        font-weight: 700;
        color: #0F172A;
      }

      .weak-count-tag {
        font-weight: 600;
      }
    }

    .card-subtitle {
      font-size: 12px;
      color: #94A3B8;
    }
  }

  .weak-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
    max-height: 380px;
    overflow-y: auto;
    padding-right: 4px;

    &::-webkit-scrollbar {
      width: 6px;
    }
    &::-webkit-scrollbar-thumb {
      background: #CBD5E1;
      border-radius: 3px;
    }
  }

  .weak-item {
    padding: 14px 16px;
    border-radius: 12px;
    background: #F8FAFC;
    border: 1px solid #E2E8F0;
    transition: all 0.2s ease;

    &:hover {
      background: #FFFFFF;
      box-shadow: 0 4px 12px rgba(15, 23, 42, 0.05);
      border-color: #CBD5E1;
      transform: translateY(-1px);
    }

    .weak-top {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 10px;
      margin-bottom: 8px;

      .weak-title-wrap {
        display: flex;
        align-items: center;
        gap: 8px;
        flex-wrap: wrap;

        .weak-title {
          font-size: 14px;
          font-weight: 700;
          color: #1E293B;
        }

        .weak-chapter-tag {
          font-size: 11px;
          color: #64748B;
          background: #EEF2F6;
          padding: 1px 6px;
          border-radius: 4px;
        }
      }

      .weak-meta-right {
        display: flex;
        align-items: center;
        gap: 6px;

        .weak-score-label {
          font-size: 11px;
          color: #94A3B8;
        }
      }
    }

    .weak-progress-bar {
      width: 100%;
      height: 4px;
      background: #E2E8F0;
      border-radius: 2px;
      overflow: hidden;
      margin-bottom: 8px;

      .progress-fill {
        height: 100%;
        border-radius: 2px;
        transition: width 0.4s ease;
      }
    }

    .weak-suggestion {
      margin: 0 0 10px;
      font-size: 12px;
      color: #64748B;
      line-height: 1.5;
    }

    .weak-bottom-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-top: 8px;
      border-top: 1px dashed #E2E8F0;

      .weak-stat-tags {
        display: flex;
        align-items: center;
        gap: 8px;
        flex-wrap: wrap;

        .stat-badge {
          font-size: 11px;
          color: #64748B;

          strong {
            color: #0F172A;
          }

          &--source {
            padding: 1px 6px;
            border-radius: 4px;
            border: 1px solid transparent;

            &.source--measured {
              color: #047857;
              background: #ECFDF5;
              border-color: #A7F3D0;
            }

            &.source--mixed {
              color: #B45309;
              background: #FFFBEB;
              border-color: #FDE68A;
            }

            &.source--estimated {
              color: #64748B;
              background: #F1F5F9;
              border-color: #CBD5E1;
            }
          }
        }
      }

      .action-link-btn {
        border: none;
        background: transparent;
        display: inline-flex;
        align-items: center;
        gap: 4px;
        color: #2563EB;
        font-size: 12px;
        font-weight: 600;
        cursor: pointer;
        padding: 0;
        transition: color 0.2s ease;

        .arrow-svg {
          width: 13px;
          height: 13px;
          transition: transform 0.2s ease;
        }

        &:hover {
          color: #1D4ED8;
          .arrow-svg {
            transform: translateX(2px);
          }
        }
      }
    }
  }
}
</style>
