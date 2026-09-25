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
      :total-points="masteryData?.totalKnowledgePoints || masteryData?.dimensions?.length || 0"
      :class-avg-score="masteryData?.classAvgMastery || computeAvgClassScore()"
      :mastered-count="masteryData?.masteredCount || countMastered()"
      :warning-count="masteryData?.warningCount || (masteryData?.weakPoints?.length || 0)"
      :student-count="masteryData?.studentCount || (masteryData?.students?.length || 0)"
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
                {{ masteryData?.weakPoints?.length || 0 }} 处预警
              </el-tag>
            </div>
            <span class="card-subtitle">掌握度低于 70% 考点集</span>
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
        abort-label="完成推演"
        @abort="handleCloseAiAdvice"
      />
    </el-dialog>
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
import { AI_COGNITIVE_THINKING_PRESETS } from '@/constants/ai/cognitive-thinking';
import { useLearningAnalytics } from '@/composables/analytics/useLearningAnalytics';
import { useTeacherCourses } from '@/composables/course/useTeacherCourses';
import type { WeakPointVO } from '@/types/analytics/mastery';

const router = useRouter();
const { courseOptions, courseId } = useTeacherCourses();
const studentId = ref<number | undefined>(undefined);
const range = ref<string>('30d');
const aiThinkingModalVisible = ref(false);
const adviceLoading = ref(false);

const { loading, usedMockFallback, masteryData, fetchMastery } = useLearningAnalytics();

const radarTitle = computed(() => {
  if (!studentId.value) {
    return '班级核心知识维度掌握基准雷达';
  }
  const curStu = masteryData.value?.students?.find((s) => s.id === studentId.value);
  return curStu ? `【${curStu.name}】掌握度对比 (个人表现 vs 班级平均)` : '学员多维掌握度对比画像';
});

function computeAvgClassScore(): number {
  if (!masteryData.value?.classAvg?.length) return 75;
  const sum = masteryData.value.classAvg.reduce((a, b) => a + b, 0);
  return Math.round((sum / masteryData.value.classAvg.length) * 10) / 10;
}

function countMastered(): number {
  if (!masteryData.value?.classAvg?.length) return 0;
  return masteryData.value.classAvg.filter((v) => v >= 85).length;
}

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

function handleOpenAiAdvice() {
  aiThinkingModalVisible.value = true;
  adviceLoading.value = true;
  setTimeout(() => {
    adviceLoading.value = false;
  }, 3500);
}

function handleCloseAiAdvice() {
  aiThinkingModalVisible.value = false;
  adviceLoading.value = false;
}

function handleDispatchPractice(item: WeakPointVO) {
  ElMessage.success(`已为考点【${item.title}】启动自适应定向变式题训练计划`);
  router.push({
    path: '/learning/practice',
    query: {
      mode: 'WEAK_POINT',
      courseId: String(courseId.value || ''),
      knowledgePointId: String(item.knowledgePointId)
    }
  });
}

function handleExportMatrix() {
  if (!masteryData.value?.dimensions?.length) {
    ElMessage.warning('暂无考点掌握度数据可导出');
    return;
  }
  const headers = ['知识考点', '班级平均掌握率(%)'];
  const rows = masteryData.value.dimensions.map((dim, i) => {
    const avg = masteryData.value?.classAvg[i] ?? 0;
    return `"${dim}",${avg}`;
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
        gap: 8px;

        .stat-badge {
          font-size: 11px;
          color: #64748B;

          strong {
            color: #0F172A;
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
