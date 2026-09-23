<template>
  <div class="ai-grading-container ai-teaching-page-shell">
    <!-- 统一现代顶栏 Hero 区域 -->
    <ProfilePageHero
      title="AI 智能批改控制台"
      subtitle="全自动并发调度作业答卷评阅队列，支持自适应模型选择、严格度标定与全流程可控推演。"
    >
      <template #actions>
        <div class="hero-action-row">
          <button type="button" class="hero-pill-btn is-outline" @click="router.push('/ai/marketplace')">
            返回 AI 广场
          </button>
          <el-button
            type="primary"
            class="grading-run-btn"
            :icon="Lightning"
            :loading="batchRunning || gradingLoading"
            @click="handleRunAllPending"
          >
            启动全队列并发批改
          </el-button>
        </div>
      </template>
    </ProfilePageHero>

    <!-- 顶栏指标卡 (4 维微看板风格) -->
    <div class="metrics-row">
      <div class="metric-card metric-card--blue">
        <div class="metric-icon">
          <el-icon><Tickets /></el-icon>
        </div>
        <div class="metric-info">
          <span class="val">{{ totalSubmissionsCount }}</span>
          <span class="label">已收录作业答卷总量</span>
        </div>
      </div>

      <div class="metric-card metric-card--emerald">
        <div class="metric-icon">
          <el-icon><Aim /></el-icon>
        </div>
        <div class="metric-info">
          <span class="val">{{ aiGradedCount }}</span>
          <span class="label">已完成 AI 智能预评</span>
        </div>
      </div>

      <div class="metric-card metric-card--purple">
        <div class="metric-icon">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="metric-info">
          <span class="val">{{ pendingReviewCount }}</span>
          <span class="label">待教师确认（已 AI 预评）</span>
        </div>
      </div>

      <div class="metric-card metric-card--amber">
        <div class="metric-icon">
          <el-icon><Clock /></el-icon>
        </div>
        <div class="metric-info">
          <span class="val">{{ reviewedCount }}</span>
          <span class="label">教师已复核发布</span>
        </div>
      </div>
    </div>

    <!-- AI 批改配置与真实大模型控制台 (杜绝静态写死假数据) -->
    <el-card shadow="never" class="settings-card">
      <div class="settings-grid">
        <div class="setting-item">
          <span class="setting-label">批改推理模型：</span>
          <el-select
            v-model="selectedModel"
            size="default"
            style="width: 260px"
            :loading="loadingModels"
            placeholder="正在拉取可用模型..."
          >
            <template v-if="availableModels.length > 0">
              <el-option
                v-for="m in availableModels"
                :key="m.modelKey"
                :label="`${m.name || m.modelKey} (${m.provider || 'AI Gateway'})`"
                :value="m.modelKey"
              />
            </template>
            <template v-else>
              <el-option label="默认网关模型 (系统内置)" value="default-gateway" />
            </template>
          </el-select>
          <span v-if="availableModels.length === 0 && !loadingModels" class="model-hint-link">
            暂无已启用大模型，可前往
            <router-link to="/system/models" class="text-blue-600 hover:underline">模型管理</router-link>
            配置
          </span>
        </div>

        <div class="setting-item">
          <span class="setting-label">评分严格倾向：</span>
          <el-select v-model="strictness" size="default" style="width: 170px">
            <el-option label="严格严谨（重点考察细节）" value="STRICT" />
            <el-option label="标准平衡（平衡步骤与结果）" value="NORMAL" />
            <el-option label="包容鼓励（按步给分优先）" value="LENIENT" />
          </el-select>
        </div>

        <div class="setting-item">
          <span class="setting-label">自动生成评语：</span>
          <el-switch v-model="autoFeedback" active-text="开启" inactive-text="关闭" />
        </div>
      </div>
    </el-card>

    <!-- 正在进行与待处理的批改任务列表 -->
    <div v-loading="loadingTasks" class="tasks-table-card">
      <div class="table-header-line">
        <div class="header-left-col">
          <h3 class="table-title">当前作业批改任务队列</h3>
          <span class="header-subtip">支持实时监测并发批改进度与多维度学情结果复核</span>
        </div>
        <el-button link :icon="Refresh" :loading="loadingTasks" @click="loadRealTasks">
          刷新队列
        </el-button>
      </div>

      <el-table :data="gradingTasks" stripe class="main-table">
        <template #empty>
          <el-empty description="当前暂无作业批改任务队列" />
        </template>
        <el-table-column label="作业标题" prop="title" min-width="260">
          <template #default="{ row }">
            <div class="title-cell">
              <span class="title-text">{{ row.title }}</span>
              <span class="course-text">{{ row.courseName }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="答卷总数" prop="submissionCount" width="130">
          <template #default="{ row }">
            <span class="font-semibold text-slate-700">{{ row.submissionCount }} 份</span>
          </template>
        </el-table-column>

        <el-table-column label="AI 批改进度" width="240">
          <template #default="{ row }">
            <div class="prog-cell">
              <el-progress
                :percentage="row.progress"
                :status="row.progress === 100 ? 'success' : undefined"
              />
              <span class="prog-text">
                {{ row.progress === 100 ? '已全部完成' : `正在评阅第 ${Math.round((row.submissionCount * row.progress) / 100)} 份...` }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="row.status === 'DONE' ? 'success' : 'warning'" size="small">
              {{ row.status === 'DONE' ? '评阅就绪' : '待处理' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              size="small"
              @click="viewGradingResults(row)"
            >
              <span>查看诊断报告</span>
              <el-icon class="ml-1"><ArrowRight /></el-icon>
            </el-button>
            <el-button
              type="success"
              link
              size="small"
              :loading="row.running"
              @click="reRunTask(row)"
            >
              触发 AI 评阅
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- AI 智能阅卷认知推演弹窗（雷达脉冲环、秒级实时计时、流水线与随时中止控制） -->
    <AssignmentGradingEngineDialog
      :visible="isThinkingModalVisible"
      :title="thinkingDialogTitle"
      @abort="handleAbortThinking"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  Lightning,
  Tickets,
  Aim,
  CircleCheck,
  Clock,
  ArrowRight,
  Refresh
} from '@element-plus/icons-vue';
import { useGrading, type GradingTaskRow } from '@/composables/ai/useGrading';
import ProfilePageHero from '@/components/profile/ProfilePageHero.vue';
import AssignmentGradingEngineDialog from '@/components/question/assignment/AssignmentGradingEngineDialog.vue';

const router = useRouter();
const {
  startGrading,
  batchGrade,
  loadGradingTasks,
  loadAvailableModels,
  loadOverviewStats,
  createAbortSignal,
  abortGrading,
  availableModels,
  selectedModel,
  loadingModels,
  overviewStats,
  isThinkingModalVisible,
  loading: gradingLoading
} = useGrading();

const strictness = ref('NORMAL');
const autoFeedback = ref(true);
const batchRunning = ref(false);
const loadingTasks = ref(false);
const thinkingDialogTitle = ref('AI 智能阅卷引擎正在深度评阅作业答卷...');

const gradingTasks = ref<GradingTaskRow[]>([]);

onMounted(async () => {
  await Promise.all([
    loadRealTasks(),
    loadOverviewStats(),
    loadAvailableModels()
  ]);
});

async function loadRealTasks() {
  loadingTasks.value = true;
  try {
    gradingTasks.value = await loadGradingTasks();
  } catch (err: any) {
    ElMessage.error(err?.message || '加载作业批改队列失败');
    gradingTasks.value = [];
  } finally {
    loadingTasks.value = false;
  }
}

const totalSubmissionsCount = computed(() => {
  return gradingTasks.value.reduce((acc, t) => acc + (t.submissionCount || 0), 0);
});

const aiGradedCount = computed(() => overviewStats.value.gradedCount ?? 0);
const pendingReviewCount = computed(() => overviewStats.value.submittedCount ?? 0);
const reviewedCount = computed(() => overviewStats.value.reviewedCount ?? 0);

async function handleRunAllPending() {
  const allPendingIds: number[] = [];
  for (const t of gradingTasks.value) {
    if (t.submissions && t.submissions.length > 0) {
      for (const s of t.submissions) {
        if (s.status !== 'GRADED' && s.status !== 'REVIEWED') {
          allPendingIds.push(s.id);
        }
      }
    }
  }

  if (allPendingIds.length === 0) {
    ElMessage.info('当前暂无待批改的学生答卷');
    return;
  }

  thinkingDialogTitle.value = `AI 智能批改引擎正在并发处理全队列（共 ${allPendingIds.length} 份答卷）...`;
  isThinkingModalVisible.value = true;
  batchRunning.value = true;
  const signal = createAbortSignal();

  try {
    const successCount = await batchGrade(allPendingIds, { signal });
    if (!signal.aborted) {
      await Promise.all([loadRealTasks(), loadOverviewStats()]);
      ElMessage.success(`已成功为 ${successCount} 份答卷完成 AI 智能预审评阅！`);
    }
  } catch (err: any) {
    if (err?.name !== 'CanceledError' && err?.name !== 'AbortError') {
      ElMessage.error(err?.message || '批量并发评阅失败，请重试');
    }
  } finally {
    batchRunning.value = false;
    isThinkingModalVisible.value = false;
  }
}

async function reRunTask(row: GradingTaskRow) {
  const subIds = row.submissions?.map((s) => s.id) || [];
  if (subIds.length === 0) {
    ElMessage.warning(`【${row.title}】暂无学生提交答卷`);
    return;
  }

  thinkingDialogTitle.value = `AI 智能评阅引擎正在复评【${row.title}】（共 ${subIds.length} 份）...`;
  isThinkingModalVisible.value = true;
  row.running = true;
  const signal = createAbortSignal();

  try {
    for (const sid of subIds) {
      if (signal.aborted) break;
      await startGrading(sid, { signal });
    }
    if (!signal.aborted) {
      row.progress = 100;
      row.status = 'DONE';
      await Promise.all([loadRealTasks(), loadOverviewStats()]);
      ElMessage.success(`【${row.title}】已完成全新一轮大模型深度复评！`);
    }
  } catch (err: any) {
    if (err?.name !== 'CanceledError' && err?.name !== 'AbortError') {
      ElMessage.error(err?.message || `评阅任务【${row.title}】执行异常`);
    }
  } finally {
    row.running = false;
    isThinkingModalVisible.value = false;
  }
}

function handleAbortThinking() {
  abortGrading();
  batchRunning.value = false;
  gradingTasks.value.forEach((r) => (r.running = false));
}

function viewGradingResults(row: GradingTaskRow) {
  const firstSubId = row.submissions?.[0]?.id;
  router.push({
    path: '/ai/grading/result',
    query: {
      assignmentId: String(row.id),
      submissionId: firstSubId ? String(firstSubId) : undefined,
      title: row.title,
      model: selectedModel.value || undefined
    }
  });
}
</script>

<style scoped lang="scss">
.ai-grading-container {
  padding: 24px;
  background-color: #f8fafc;
  min-height: calc(100vh - 64px);

  .hero-action-row {
    display: flex;
    align-items: center;
    gap: 12px;

    .hero-pill-btn {
      padding: 8px 18px;
      border-radius: 9999px;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      border: 1px solid #cbd5e1;
      background: #ffffff;
      color: #334155;
      transition: all 0.2s ease;

      &:hover {
        background: #f1f5f9;
        border-color: #94a3b8;
      }
    }

    .grading-run-btn {
      border-radius: 9999px;
      padding: 9px 22px;
      font-weight: 600;
      background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
      border: none;
      box-shadow: 0 4px 14px rgba(37, 99, 235, 0.28);
      transition: all 0.2s;

      &:hover {
        transform: translateY(-1px);
        box-shadow: 0 6px 18px rgba(37, 99, 235, 0.35);
      }
    }
  }

  .metrics-row {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    margin-top: 20px;
    margin-bottom: 20px;

    .metric-card {
      background: #ffffff;
      border-radius: 16px;
      padding: 18px 20px;
      display: flex;
      align-items: center;
      gap: 16px;
      border: 1px solid #e2e8f0;
      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.03);
      transition: all 0.25s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
      }

      .metric-icon {
        width: 48px;
        height: 48px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 22px;
        flex-shrink: 0;
      }

      &--blue {
        .metric-icon {
          background: #eff6ff;
          color: #2563eb;
        }
      }

      &--emerald {
        .metric-icon {
          background: #ecfdf5;
          color: #059669;
        }
      }

      &--purple {
        .metric-icon {
          background: #f5f3ff;
          color: #7c3aed;
        }
      }

      &--amber {
        .metric-icon {
          background: #fffbeb;
          color: #d97706;
        }
      }

      .metric-info {
        display: flex;
        flex-direction: column;

        .val {
          font-size: 24px;
          font-weight: 800;
          color: #0f172a;
          line-height: 1.2;
        }

        .label {
          font-size: 12px;
          color: #64748b;
          margin-top: 4px;
        }
      }
    }
  }

  .settings-card {
    border-radius: 16px;
    border: 1px solid #e2e8f0;
    margin-bottom: 20px;
    background: #ffffff;

    .settings-grid {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 28px;
      padding: 6px 10px;

      .setting-item {
        display: flex;
        align-items: center;
        gap: 10px;

        .setting-label {
          font-size: 13px;
          font-weight: 600;
          color: #334155;
          white-space: nowrap;
        }

        .model-hint-link {
          font-size: 12px;
          color: #64748b;
          margin-left: 6px;
        }
      }
    }
  }

  .tasks-table-card {
    background: #ffffff;
    border-radius: 16px;
    border: 1px solid #e2e8f0;
    padding: 20px 24px;
    box-shadow: 0 2px 12px rgba(15, 23, 42, 0.02);

    .table-header-line {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 16px;

      .header-left-col {
        display: flex;
        align-items: baseline;
        gap: 12px;

        .table-title {
          font-size: 16px;
          font-weight: 700;
          color: #0f172a;
          margin: 0;
        }

        .header-subtip {
          font-size: 12px;
          color: #64748b;
        }
      }
    }

    .main-table {
      width: 100%;

      .title-cell {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .title-text {
          font-weight: 600;
          color: #1e293b;
        }

        .course-text {
          font-size: 12px;
          color: #64748b;
        }
      }

      .prog-cell {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .prog-text {
          font-size: 12px;
          color: #94a3b8;
        }
      }
    }
  }
}

@media (max-width: 1024px) {
  .ai-grading-container {
    .metrics-row {
      grid-template-columns: repeat(2, 1fr);
    }
  }
}
</style>
