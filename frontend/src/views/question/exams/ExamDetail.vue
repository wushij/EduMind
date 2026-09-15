<template>
  <div class="exam-detail-container">
    <!-- 顶部面包屑与导航 -->
    <div class="top-nav-bar">
      <el-button :icon="ArrowLeft" link class="back-link" @click="router.push('/question/exams')">
        返回试卷列表
      </el-button>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/question/exams' }">试卷管理</el-breadcrumb-item>
        <el-breadcrumb-item>{{ examData?.title || '试卷详情' }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 试卷顶部信息卡片 -->
    <div v-loading="loading" class="exam-hero-card">
      <div class="hero-left">
        <div class="status-and-title">
          <el-tag :type="getStatusTagType(examData?.status)" effect="dark" size="small" round>
            {{ getStatusLabel(examData?.status) }}
          </el-tag>
          <h1 class="exam-main-title">{{ examData?.title || '考核试卷详情' }}</h1>
        </div>

        <div class="meta-pills-row">
          <span class="meta-pill">
            <span class="label">所属课程：</span>
            <strong>{{ examData?.courseName || '未指定课程' }}</strong>
          </span>
          <span class="meta-pill">
            <span class="label">考核学期：</span>
            <strong>{{ examData?.semester || '2025-2026-2' }}</strong>
          </span>
          <span class="meta-pill">
            <span class="label">考试时限：</span>
            <strong>{{ examData?.durationMinutes || 120 }} 分钟</strong>
          </span>
          <span class="meta-pill">
            <span class="label">试卷总分：</span>
            <strong class="text-blue-600">{{ examData?.totalScore || 100 }} 分</strong>
          </span>
          <span class="meta-pill">
            <span class="label">及格合格线：</span>
            <strong class="text-emerald-600">{{ examData?.passScore || 60 }} 分</strong>
          </span>
        </div>
      </div>

      <!-- 右侧操作区 -->
      <div class="hero-right-actions">
        <el-button type="primary" class="action-btn" @click="handlePublishAsAssignment">
          <el-icon class="mr-1"><Promotion /></el-icon> 一键发布为在线作业/测验
        </el-button>
        <el-button class="action-btn" @click="handleExportPaper">
          <el-icon class="mr-1"><Download /></el-icon> 导出试卷 (JSON / 打印)
        </el-button>
      </div>
    </div>

    <!-- 主体区域：左侧纸质卷面展示 + 右侧试卷知识点与难度分布 -->
    <div class="main-content-layout">
      <!-- 卷面主区域 -->
      <div class="paper-display-area">
        <!-- 模式切换与工具条 -->
        <div class="mode-switch-dock">
          <div class="left-toggles">
            <span class="dock-label">卷面视图模式：</span>
            <el-radio-group v-model="viewMode" size="small">
              <el-radio-button label="PAPER">
                <el-icon class="mr-1"><Document /></el-icon>
                <span>标准考生纸质试卷</span>
              </el-radio-button>
              <el-radio-button label="ANSWER_KEY">
                <el-icon class="mr-1"><View /></el-icon>
                <span>含参考答案及评分细则</span>
              </el-radio-button>
            </el-radio-group>
          </div>

          <div class="right-quick">
            <span class="q-count-hint">本试卷共 {{ totalQuestionsCount }} 道题目</span>
            <el-button link type="primary" size="small" @click="printPaper">
              <el-icon class="mr-1"><Printer /></el-icon> 打印试卷
            </el-button>
          </div>
        </div>

        <!-- 试卷纸张 -->
        <div id="printable-exam-paper" class="exam-paper-sheet">
          <div class="sheet-head">
            <div class="school-header">EduMind 智教云 · 全数字化教学与智能评阅中心</div>
            <h2 class="exam-title-text">{{ examData?.title }}</h2>
            <div class="exam-subtitle-meta">
              <span>课程代码/名称：{{ examData?.courseName }}</span>
              <span>学期：{{ examData?.semester }}</span>
              <span>考试时限：{{ examData?.durationMinutes }}分钟</span>
              <span>满分：{{ examData?.totalScore }}分</span>
            </div>

            <div class="exam-seal-info-bar">
              <span>班级：____________________</span>
              <span>学号：____________________</span>
              <span>姓名：____________________</span>
              <span>考场座位号：________</span>
            </div>

            <div class="exam-instructions">
              <strong>考生答题规范与须知：</strong>
              <span>本试卷共 {{ groupedSections.length }} 个大题，请考生仔细核对试卷完整性，在规定答题纸或作答区域内规范作答。</span>
            </div>
          </div>

          <!-- 各大题试题流 -->
          <div class="sheet-body">
            <template v-if="groupedSections.length > 0">
              <div
                v-for="(sec, sIdx) in groupedSections"
                :key="sec.type"
                class="sheet-section-block"
              >
                <div class="section-title-line">
                  <span class="sec-number">{{ getChineseNumber(sIdx + 1) }}、{{ sec.title }}</span>
                  <span class="sec-score-info">
                    （共 {{ sec.questions.length }} 小题，合计 {{ sec.totalScore }} 分）
                  </span>
                </div>

                <div class="section-questions">
                  <div
                    v-for="(q, qIdx) in sec.questions"
                    :key="q.id"
                    class="paper-question-card"
                  >
                    <div class="stem-line">
                      <span class="q-index">{{ qIdx + 1 }}.</span>
                      <span class="q-stem-text">{{ q.stem }}</span>
                      <span class="q-score-tag">（{{ q.score || 5 }}分）</span>
                    </div>

                    <!-- 选项列表 -->
                    <div v-if="q.options && q.options.length" class="options-grid">
                      <div
                        v-for="opt in q.options"
                        :key="opt.key"
                        class="option-item"
                        :class="{
                          'is-correct-answer': viewMode === 'ANSWER_KEY' && (opt.isCorrect || opt.key === q.correctAnswer)
                        }"
                      >
                        <span class="opt-key">{{ opt.key }}.</span>
                        <span class="opt-content">{{ opt.content }}</span>
                        <span
                          v-if="viewMode === 'ANSWER_KEY' && (opt.isCorrect || opt.key === q.correctAnswer)"
                          class="correct-badge"
                        >
                          <el-icon><Check /></el-icon> 正确选项
                        </span>
                      </div>
                    </div>

                    <!-- 教师答案及解析模式 -->
                    <div v-if="viewMode === 'ANSWER_KEY'" class="answer-key-box">
                      <div class="ans-row">
                        <span class="ans-title">【标准答案】：</span>
                        <span class="ans-text text-emerald-600 font-bold">{{ q.correctAnswer || '略' }}</span>
                      </div>
                      <div class="ans-row">
                        <span class="ans-title">【试题解析】：</span>
                        <span class="ans-text">{{ q.analysis || '暂无详细文字解析' }}</span>
                      </div>
                      <div v-if="q.knowledgePointNames && q.knowledgePointNames.length" class="ans-row">
                        <span class="ans-title">【考查考点】：</span>
                        <div class="kps-tags">
                          <span v-for="kp in q.knowledgePointNames" :key="kp" class="kp-pill">{{ kp }}</span>
                        </div>
                      </div>
                    </div>

                    <!-- 考生答题留白（非答案模式且是简答填空） -->
                    <div
                      v-else-if="q.type === 'SHORT_ANSWER' || q.type === 'FILL_BLANK'"
                      class="student-blank-area"
                    >
                      <div class="answer-guide">考生答题区：</div>
                      <div class="ruled-lines">
                        <div class="line"></div>
                        <div class="line"></div>
                        <div class="line"></div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </template>
            <el-empty v-else description="当前试卷暂无试题数据，可前往重新组卷或挑选题目" />
          </div>
        </div>
      </div>

      <!-- 右侧试卷统计与考点覆盖 (Sticky) -->
      <div class="paper-sidebar-area">
        <el-card shadow="never" class="sidebar-card">
          <h3 class="side-card-title">
            <el-icon class="title-icon"><TrendCharts /></el-icon>
            <span>试卷难度与考点画像</span>
          </h3>

          <div class="side-stat-row">
            <span class="label">试卷大题类别</span>
            <span class="val">{{ groupedSections.length }} 类</span>
          </div>
          <div class="side-stat-row">
            <span class="label">入卷试题总量</span>
            <span class="val font-bold text-blue-600">{{ totalQuestionsCount }} 道</span>
          </div>
          <div class="side-stat-row">
            <span class="label">及格要求比例</span>
            <span class="val font-semibold text-emerald-600">
              {{ Math.round(((examData?.passScore || 60) / (examData?.totalScore || 100)) * 100) }}%
            </span>
          </div>

          <el-divider class="my-3" />

          <!-- 难度配比 -->
          <div class="difficulty-breakdown">
            <span class="sub-label">难度梯度构成</span>
            <div class="diff-bars-stack">
              <div class="diff-bar-item">
                <div class="diff-header">
                  <span class="diff-name text-emerald-600">简单题</span>
                  <span class="diff-count">{{ difficultyCounts.EASY }} 题 ({{ difficultyPercentages.EASY }}%)</span>
                </div>
                <el-progress :percentage="difficultyPercentages.EASY" color="#10b981" :show-text="false" />
              </div>

              <div class="diff-bar-item">
                <div class="diff-header">
                  <span class="diff-name text-amber-600">中等题</span>
                  <span class="diff-count">{{ difficultyCounts.MEDIUM }} 题 ({{ difficultyPercentages.MEDIUM }}%)</span>
                </div>
                <el-progress :percentage="difficultyPercentages.MEDIUM" color="#f59e0b" :show-text="false" />
              </div>

              <div class="diff-bar-item">
                <div class="diff-header">
                  <span class="diff-name text-red-600">困难题</span>
                  <span class="diff-count">{{ difficultyCounts.HARD }} 题 ({{ difficultyPercentages.HARD }}%)</span>
                </div>
                <el-progress :percentage="difficultyPercentages.HARD" color="#ef4444" :show-text="false" />
              </div>
            </div>
          </div>

          <el-divider class="my-3" />

          <!-- 覆盖核心知识点清单 -->
          <div class="kps-breakdown">
            <span class="sub-label">覆盖核心知识点 ({{ coveredKnowledgePoints.length }} 个)</span>
            <div class="kps-chips-flow">
              <span v-for="kp in coveredKnowledgePoints" :key="kp" class="kp-badge-chip">
                {{ kp }}
              </span>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 导出试卷弹窗 -->
    <el-dialog v-model="exportDialogVisible" title="试卷数据导出" width="580px" destroy-on-close>
      <div v-loading="exportLoading" class="export-dialog-body">
        <p class="export-tip">
          您可以复制以下标准化试卷 JSON 结构用于系统间数据迁移或在线题库交换：
        </p>
        <el-input
          type="textarea"
          :rows="12"
          readonly
          :value="exportDataJson"
        />
      </div>
      <template #footer>
        <el-button @click="exportDialogVisible = false">关闭</el-button>
        <el-button type="primary" :disabled="!exportDataJson" @click="handleCopyExportJson">
          复制 JSON 到剪贴板
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  ArrowLeft,
  Promotion,
  Download,
  Printer,
  Document,
  View,
  TrendCharts,
  Check
} from '@element-plus/icons-vue';
import { getExamDetail, exportExam } from '@/api/question/exam';
import type { ExamPaper } from '@/types/question/exam';
import type { QuestionItem, QuestionType } from '@/types/question/question';

const route = useRoute();
const router = useRouter();

const examId = computed(() => Number(route.params.id) || 1);
const loading = ref(false);
const examData = ref<ExamPaper | null>(null);
const viewMode = ref<'PAPER' | 'ANSWER_KEY'>('PAPER');
const exportDialogVisible = ref(false);
const exportLoading = ref(false);
const exportDataJson = ref('');

onMounted(async () => {
  await loadExam();
});

async function loadExam() {
  loading.value = true;
  try {
    const res = await getExamDetail(examId.value);
    examData.value = res.data;
    if (examData.value && (!examData.value.questions || examData.value.questions.length === 0)) {
      examData.value.questions = [];
    }
  } catch (err: any) {
    examData.value = null;
    ElMessage.error(err?.message || '加载试卷详情失败，请检查网络或后端状态');
  } finally {
    loading.value = false;
  }
}

// 按题型将试题归类到大题
interface GroupedSection {
  type: QuestionType;
  title: string;
  totalScore: number;
  questions: QuestionItem[];
}

const groupedSections = computed<GroupedSection[]>(() => {
  if (!examData.value?.questions) return [];
  const map = new Map<string, GroupedSection>();

  const titleMap: Record<string, string> = {
    SINGLE_CHOICE: '单项选择题',
    MULTIPLE_CHOICE: '多项选择题',
    TRUE_FALSE: '判断题',
    FILL_BLANK: '填空题',
    SHORT_ANSWER: '综合应用与简答题'
  };

  examData.value.questions.forEach(q => {
    const type = q.type || 'SINGLE_CHOICE';
    if (!map.has(type)) {
      map.set(type, {
        type,
        title: titleMap[type] || '综合试题',
        totalScore: 0,
        questions: []
      });
    }
    const sec = map.get(type)!;
    sec.questions.push(q);
    sec.totalScore += q.score || 5;
  });

  return Array.from(map.values());
});

const totalQuestionsCount = computed(() => {
  return examData.value?.questions?.length || 0;
});

// 统计难度分布
const difficultyCounts = computed(() => {
  const counts = { EASY: 0, MEDIUM: 0, HARD: 0 };
  examData.value?.questions?.forEach(q => {
    const diff = (q.difficulty || 'MEDIUM') as 'EASY' | 'MEDIUM' | 'HARD';
    if (counts[diff] !== undefined) {
      counts[diff]++;
    }
  });
  return counts;
});

const difficultyPercentages = computed(() => {
  const total = totalQuestionsCount.value || 1;
  return {
    EASY: Math.round((difficultyCounts.value.EASY / total) * 100),
    MEDIUM: Math.round((difficultyCounts.value.MEDIUM / total) * 100),
    HARD: Math.round((difficultyCounts.value.HARD / total) * 100)
  };
});

// 收集知识点
const coveredKnowledgePoints = computed(() => {
  const set = new Set<string>();
  examData.value?.questions?.forEach(q => {
    q.knowledgePointNames?.forEach(kp => set.add(kp));
  });
  return Array.from(set);
});

// 一键发布为作业
function handlePublishAsAssignment() {
  router.push({
    path: '/question/assignments/create',
    query: {
      examId: examId.value,
      courseId: examData.value?.courseId,
      title: examData.value?.title
    }
  });
}

// 导出试卷
async function handleExportPaper() {
  exportDialogVisible.value = true;
  exportLoading.value = true;
  try {
    const res = await exportExam(examId.value);
    exportDataJson.value = JSON.stringify(res.data ?? examData.value, null, 2);
  } catch (err: any) {
    ElMessage.error(err?.message || '导出试卷失败');
    exportDataJson.value = '';
  } finally {
    exportLoading.value = false;
  }
}

function handleCopyExportJson() {
  navigator.clipboard.writeText(exportDataJson.value);
  ElMessage.success('试卷配置 JSON 已成功复制到剪贴板！');
  exportDialogVisible.value = false;
}

// 打印试卷
function printPaper() {
  window.print();
}

function getStatusLabel(status?: string) {
  const map: Record<string, string> = {
    PUBLISHED: '正式发布',
    DRAFT: '草稿暂存',
    ARCHIVED: '已归档'
  };
  return map[status || 'PUBLISHED'] || '正式发布';
}

function getStatusTagType(status?: string) {
  const map: Record<string, string> = {
    PUBLISHED: 'success',
    DRAFT: 'info',
    ARCHIVED: 'warning'
  };
  return (map[status || 'PUBLISHED'] as any) || 'success';
}

function getChineseNumber(num: number) {
  const cn = ['零', '一', '二', '三', '四', '五', '六', '七', '八', '九', '十'];
  return cn[num] || String(num);
}
</script>

<style scoped lang="scss">
.exam-detail-container {
  padding: 24px;
  background: #f8fafc;
  .top-nav-bar {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;

    .back-link {
      font-size: 14px;
      font-weight: 500;
      color: #3b82f6;
    }
  }

  .exam-hero-card {
    background: #ffffff;
    border-radius: 16px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
    padding: 24px 32px;
    margin-bottom: 24px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 24px;

    .hero-left {
      .status-and-title {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 12px;

        .exam-main-title {
          font-size: 22px;
          font-weight: 800;
          color: #0f172a;
          margin: 0;
        }
      }

      .meta-pills-row {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 12px;

        .meta-pill {
          background: #f1f5f9;
          border-radius: 8px;
          padding: 4px 12px;
          font-size: 13px;
          color: #475569;

          .label {
            color: #64748b;
          }
        }
      }
    }

    .hero-right-actions {
      display: flex;
      gap: 12px;

      .action-btn {
        border-radius: 8px;
        font-weight: 500;
      }
    }
  }

  .main-content-layout {
    display: flex;
    gap: 24px;
    align-items: flex-start;

    .paper-display-area {
      flex: 1;

      .mode-switch-dock {
        background: #ffffff;
        border: 1px solid #e2e8f0;
        border-radius: 12px;
        padding: 12px 20px;
        margin-bottom: 16px;
        display: flex;
        align-items: center;
        justify-content: space-between;

        .left-toggles {
          display: flex;
          align-items: center;
          gap: 12px;

          .dock-label {
            font-size: 13px;
            font-weight: 600;
            color: #334155;
          }
        }

        .right-quick {
          display: flex;
          align-items: center;
          gap: 16px;

          .q-count-hint {
            font-size: 13px;
            color: #64748b;
          }
        }
      }

      .exam-paper-sheet {
        background: #ffffff;
        border: 1px solid #d1d5db;
        border-radius: 8px;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
        padding: 48px 56px;
        font-family: 'Times New Roman', SimSun, serif;

        .sheet-head {
          text-align: center;
          border-bottom: 2px solid #1e293b;
          padding-bottom: 20px;
          margin-bottom: 24px;

          .school-header {
            font-size: 14px;
            letter-spacing: 2px;
            font-weight: 600;
            color: #4b5563;
          }

          .exam-title-text {
            font-size: 24px;
            font-weight: 800;
            color: #111827;
            margin: 10px 0;
          }

          .exam-subtitle-meta {
            display: flex;
            justify-content: center;
            gap: 20px;
            font-size: 13px;
            color: #4b5563;
            margin-bottom: 16px;
          }

          .exam-seal-info-bar {
            display: flex;
            justify-content: space-around;
            background: #f9fafb;
            border: 1px dashed #9ca3af;
            padding: 8px;
            border-radius: 4px;
            font-size: 13px;
            color: #374151;
            margin-bottom: 12px;
          }

          .exam-instructions {
            text-align: left;
            font-size: 13px;
            color: #6b7280;
            background: #f8fafc;
            padding: 8px 12px;
            border-radius: 4px;
          }
        }

        .sheet-body {
          .sheet-section-block {
            margin-bottom: 28px;

            .section-title-line {
              font-size: 17px;
              font-weight: 700;
              color: #111827;
              border-bottom: 1px solid #e5e7eb;
              padding-bottom: 6px;
              margin-bottom: 16px;

              .sec-score-info {
                font-size: 13px;
                font-weight: normal;
                color: #4b5563;
              }
            }

            .section-questions {
              display: flex;
              flex-direction: column;
              gap: 18px;

              .paper-question-card {
                .stem-line {
                  font-size: 15px;
                  line-height: 1.6;
                  color: #1f2937;

                  .q-index {
                    font-weight: 700;
                    margin-right: 4px;
                  }

                  .q-score-tag {
                    color: #6b7280;
                    font-size: 13px;
                  }
                }

                .options-grid {
                  display: grid;
                  grid-template-columns: repeat(2, 1fr);
                  gap: 8px 24px;
                  margin-top: 10px;
                  padding-left: 18px;

                  .option-item {
                    font-size: 14px;
                    color: #374151;
                    display: flex;
                    align-items: center;
                    gap: 6px;

                    .opt-key {
                      font-weight: 600;
                    }

                    &.is-correct-answer {
                      color: #15803d;
                      font-weight: 600;

                      .correct-badge {
                        font-size: 11px;
                        background: #dcfce7;
                        color: #166534;
                        padding: 1px 6px;
                        border-radius: 4px;
                      }
                    }
                  }
                }

                .answer-key-box {
                  background: #f8fafc;
                  border-radius: 6px;
                  padding: 10px 14px;
                  margin-top: 10px;
                  border-left: 3px solid #10b981;
                  font-size: 13px;

                  .ans-row {
                    margin-bottom: 4px;
                    line-height: 1.5;

                    &:last-child {
                      margin-bottom: 0;
                    }

                    .ans-title {
                      font-weight: 600;
                      color: #334155;
                    }

                    .kps-tags {
                      display: inline-flex;
                      gap: 6px;

                      .kp-pill {
                        background: #e2e8f0;
                        color: #475569;
                        font-size: 11px;
                        padding: 1px 6px;
                        border-radius: 4px;
                      }
                    }
                  }
                }

                .student-blank-area {
                  margin-top: 10px;
                  padding-left: 18px;

                  .answer-guide {
                    font-size: 12px;
                    color: #9ca3af;
                    margin-bottom: 6px;
                  }

                  .ruled-lines {
                    display: flex;
                    flex-direction: column;
                    gap: 16px;

                    .line {
                      border-bottom: 1px dashed #e5e7eb;
                      height: 1px;
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    .paper-sidebar-area {
      width: 320px;
      flex-shrink: 0;

      .sidebar-card {
        position: sticky;
        top: 80px;
        background: #ffffff;
        border-radius: 16px;
        border: 1px solid #e2e8f0;
        box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
        padding: 16px;

        .side-card-title {
          font-size: 15px;
          font-weight: 700;
          color: #0f172a;
          margin: 0 0 16px;
          display: flex;
          align-items: center;
          gap: 6px;

          .title-icon {
            font-size: 17px;
            color: #2563eb;
          }
        }

        .side-stat-row {
          display: flex;
          justify-content: space-between;
          font-size: 13px;
          margin-bottom: 8px;

          .label {
            color: #64748b;
          }

          .val {
            color: #1e293b;
          }
        }

        .difficulty-breakdown {
          .sub-label {
            font-size: 13px;
            font-weight: 600;
            color: #334155;
            display: block;
            margin-bottom: 10px;
          }

          .diff-bars-stack {
            display: flex;
            flex-direction: column;
            gap: 10px;

            .diff-bar-item {
              .diff-header {
                display: flex;
                justify-content: space-between;
                font-size: 12px;
                margin-bottom: 4px;
              }
            }
          }
        }

        .kps-breakdown {
          .sub-label {
            font-size: 13px;
            font-weight: 600;
            color: #334155;
            display: block;
            margin-bottom: 10px;
          }

          .kps-chips-flow {
            display: flex;
            flex-wrap: wrap;
            gap: 6px;

            .kp-badge-chip {
              background: #eff6ff;
              color: #2563eb;
              font-size: 12px;
              padding: 3px 8px;
              border-radius: 6px;
              font-weight: 500;
            }
          }
        }
      }
    }
  }

  .export-dialog-body {
    .export-tip {
      font-size: 13px;
      color: #64748b;
      margin-bottom: 12px;
    }
  }
}

@media print {
  body * {
    visibility: hidden;
  }
  #printable-exam-paper, #printable-exam-paper * {
    visibility: visible;
  }
  #printable-exam-paper {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    box-shadow: none !important;
    border: none !important;
  }
}
</style>
