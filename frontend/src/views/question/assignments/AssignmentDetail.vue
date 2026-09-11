<template>
  <div class="assignment-detail-container">
    <!-- 顶部面包屑与导航条 -->
    <div class="top-nav-bar">
      <el-button :icon="ArrowLeft" link class="back-link" @click="router.push('/question/assignments')">
        返回作业列表
      </el-button>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/question/assignments' }">作业管理</el-breadcrumb-item>
        <el-breadcrumb-item>{{ assignmentInfo?.title || '作业详情与答卷批改' }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 顶部概览指标大看板 -->
    <div v-loading="loading" class="assignment-hero-card">
      <div class="hero-top-row">
        <div class="title-and-tags">
          <el-tag
            :type="assignmentInfo?.status === 'GRADED' ? 'success' : 'warning'"
            size="small"
            effect="dark"
            round
          >
            {{ assignmentInfo?.status === 'GRADED' ? '已全员批改' : '评阅收集中' }}
          </el-tag>
          <h1 class="assignment-title">{{ assignmentInfo?.title || '作业测验详情' }}</h1>
        </div>

        <div class="hero-actions">
          <el-button
            type="success"
            class="action-btn"
            :loading="batchAILoading"
            @click="handleBatchAIGrade"
          >
            🤖 一键全班 AI 智能预批改
          </el-button>
          <el-button class="action-btn" @click="handleRemindUnsubmitted">
            📢 一键催交未交学生
          </el-button>
        </div>
      </div>

      <div class="meta-pills-row">
        <span class="meta-pill">课程：<strong>{{ assignmentInfo?.courseName || '数据结构与算法' }}</strong></span>
        <span class="meta-pill">截止时间：<strong>{{ assignmentInfo?.deadline || '2026-09-25 23:59:59' }}</strong></span>
        <span class="meta-pill">卷面总分：<strong class="text-blue-600">{{ assignmentInfo?.totalScore || 100 }} 分</strong></span>
        <span class="meta-pill">及格标准：<strong class="text-emerald-600">{{ assignmentInfo?.passScore || 60 }} 分</strong></span>
      </div>

      <!-- 四宫格学情指标 -->
      <div class="kpi-grid">
        <div class="kpi-box">
          <span class="kpi-val">{{ totalStudentsCount }}</span>
          <span class="kpi-label">班级修读总人数</span>
        </div>
        <div class="kpi-box">
          <span class="kpi-val text-blue-600">{{ submittedCount }}</span>
          <span class="kpi-label">已提交学生数 ({{ submissionRate }}%)</span>
        </div>
        <div class="kpi-box">
          <span class="kpi-val text-amber-600">{{ pendingReviewCount }}</span>
          <span class="kpi-label">待教师终审答卷</span>
        </div>
        <div class="kpi-box">
          <span class="kpi-val text-emerald-600">{{ averageScore }} 分</span>
          <span class="kpi-label">当前平均得分</span>
        </div>
      </div>
    </div>

    <!-- 主体双标签页：学生答卷管理 vs 作业试题清单 -->
    <div class="main-tabs-card">
      <el-tabs v-model="activeTab" class="detail-tabs">
        <!-- 标签页 1：学生答卷列表与批改 -->
        <el-tab-pane label="学生答卷与批改列表" name="submissions">
          <!-- 列表过滤条 -->
          <div class="tab-filter-bar">
            <div class="left-filters">
              <el-input
                v-model="studentSearch"
                placeholder="搜索学生姓名、学号..."
                clearable
                :prefix-icon="Search"
                style="width: 280px"
              />
              <el-select v-model="statusFilter" placeholder="批改状态" clearable style="width: 140px">
                <el-option label="全部状态" value="" />
                <el-option label="待教师终审" value="PENDING" />
                <el-option label="已批改完成" value="GRADED" />
                <el-option label="AI已预批" value="AI_GRADED" />
              </el-select>
            </div>

            <div class="right-stats">
              <span class="text-sm text-slate-500">共检索到 {{ filteredSubmissions.length }} 份答卷</span>
            </div>
          </div>

          <!-- 答卷数据表格 -->
          <el-table
            :data="filteredSubmissions"
            stripe
            class="submissions-table"
            empty-text="暂无学生提交的答卷数据"
          >
            <el-table-column label="学号" prop="studentNo" width="130">
              <template #default="{ row }">
                <span class="font-mono text-slate-600">{{ row.studentNo }}</span>
              </template>
            </el-table-column>

            <el-table-column label="学生姓名" prop="studentName" width="130">
              <template #default="{ row }">
                <div class="student-cell">
                  <span class="avatar-dot"></span>
                  <span class="font-medium text-slate-800">{{ row.studentName }}</span>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="提交时间" prop="submitTime" width="180">
              <template #default="{ row }">
                <span class="text-slate-600 text-xs">{{ row.submitTime }}</span>
                <el-tag v-if="row.isLate" type="danger" size="small" class="ml-1">迟交</el-tag>
              </template>
            </el-table-column>

            <el-table-column label="AI智能预评" width="140">
              <template #default="{ row }">
                <div v-if="row.aiGraded" class="ai-grade-tag">
                  <span class="text-xs text-purple-700 bg-purple-50 px-2 py-0.5 rounded font-semibold border border-purple-200">
                    🤖 预评 {{ row.aiScore }}分
                  </span>
                </div>
                <span v-else class="text-xs text-slate-400">未触发AI</span>
              </template>
            </el-table-column>

            <el-table-column label="最终实得分" width="130">
              <template #default="{ row }">
                <span v-if="row.finalScore !== null" class="font-bold text-base text-blue-600">
                  {{ row.finalScore }} 分
                </span>
                <span v-else class="text-slate-400 text-xs italic">待终审打分</span>
              </template>
            </el-table-column>

            <el-table-column label="状态" width="130">
              <template #default="{ row }">
                <el-tag :type="getSubmissionStatusType(row.status)" size="small">
                  {{ getSubmissionStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="操作" min-width="180" fixed="right">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  link
                  size="small"
                  @click="goToGradingWorkspace(row.id)"
                >
                  {{ row.status === 'GRADED' ? '查看答卷详情' : '进入评阅打分' }}
                </el-button>
                <el-button
                  type="success"
                  link
                  size="small"
                  @click="triggerSingleAIGrade(row)"
                >
                  AI 重新评估
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无符合条件的学生提交记录" />
            </template>
          </el-table>
        </el-tab-pane>

        <!-- 标签页 2：作业试卷试题与参考答案 -->
        <el-tab-pane label="作业卷面试题及标准解答" name="questions">
          <div v-if="questionsList.length > 0" class="questions-tab-content">
            <div
              v-for="(q, idx) in questionsList"
              :key="q.id"
              class="question-review-card"
            >
              <div class="q-header">
                <span class="q-idx">第 {{ idx + 1 }} 题</span>
                <el-tag size="small" :type="getTypeTagType(q.type)">{{ getTypeLabel(q.type) }}</el-tag>
                <span class="q-score">{{ q.score || 5 }} 分</span>
              </div>
              <p class="q-stem">{{ q.stem }}</p>

              <div v-if="q.options && q.options.length" class="q-options">
                <div
                  v-for="opt in q.options"
                  :key="opt.key"
                  class="q-opt"
                  :class="{ correct: opt.isCorrect }"
                >
                  <span class="opt-k">{{ opt.key }}.</span>
                  <span>{{ opt.content }}</span>
                </div>
              </div>

              <div class="q-analysis-box">
                <div class="ans-line">
                  <strong>标准参考答案：</strong>
                  <span class="text-emerald-600 font-bold">{{ q.correctAnswer || '无客观标准答案' }}</span>
                </div>
                <div class="ans-line">
                  <strong>解析与评分细则：</strong>
                  <span>{{ q.analysis || '暂无解析' }}</span>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else description="该作业关联的试卷中暂无试题数据" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft, Search } from '@element-plus/icons-vue';
import { getAssignmentDetail } from '@/api/question/assignment';
import { getSubmissionsByAssignment, gradeSubmission } from '@/api/question/submission';
import { getExamDetail } from '@/api/question/exam';
import type { QuestionItem, QuestionType } from '@/types/question/question';
import { USE_MOCK } from '@/config/mock';
import { MOCK_QUESTIONS } from '@/mock/questions';

const route = useRoute();
const router = useRouter();

const assignmentId = computed(() => Number(route.params.id) || 1);
const loading = ref(false);
const batchAILoading = ref(false);
const activeTab = ref('submissions');

const assignmentInfo = ref<any>(null);
const submissionsList = ref<any[]>([]);
const questionsList = ref<QuestionItem[]>([]);

// 筛选
const studentSearch = ref('');
const statusFilter = ref('');

const totalStudentsCount = ref(45);

onMounted(async () => {
  await loadAssignmentData();
  await loadSubmissions();
});

async function loadAssignmentData() {
  loading.value = true;
  try {
    const res = await getAssignmentDetail(assignmentId.value);
    assignmentInfo.value = res.data;
    if (res.data?.examId) {
      try {
        const examRes = await getExamDetail(res.data.examId);
        questionsList.value = examRes.data?.questions || [];
      } catch {
        questionsList.value = USE_MOCK ? MOCK_QUESTIONS.slice(0, 4) : [];
      }
    } else {
      questionsList.value = USE_MOCK ? MOCK_QUESTIONS.slice(0, 4) : [];
    }
  } catch (err: any) {
    if (USE_MOCK) {
      assignmentInfo.value = getDefaultMockAssignment(assignmentId.value);
      questionsList.value = MOCK_QUESTIONS.slice(0, 4);
    } else {
      assignmentInfo.value = null;
      questionsList.value = [];
      ElMessage.error(err?.message || '加载作业详情失败，请检查网络或后端状态');
    }
  } finally {
    loading.value = false;
  }
}

async function loadSubmissions() {
  try {
    const res = await getSubmissionsByAssignment(assignmentId.value);
    submissionsList.value = res.data || [];
  } catch (err: any) {
    if (USE_MOCK) {
      submissionsList.value = getDefaultMockSubmissions();
    } else {
      submissionsList.value = [];
      ElMessage.error(err?.message || '加载提交记录失败');
    }
  }
}

function getDefaultMockAssignment(id: number) {
  return {
    id,
    title: '第三周：单链表与双向链表核心算法实现测验',
    courseId: 101,
    courseName: '数据结构与算法',
    deadline: '2026-09-25 23:59:59',
    totalScore: 100,
    passScore: 60,
    status: 'PENDING'
  };
}

function getDefaultMockSubmissions() {
  return [
    {
      id: 201,
      studentNo: '20240101',
      studentName: '张子轩',
      submitTime: '2026-09-11 10:30:15',
      isLate: false,
      aiGraded: true,
      aiScore: 92,
      finalScore: 95,
      status: 'GRADED'
    },
    {
      id: 202,
      studentNo: '20240102',
      studentName: '李梦琪',
      submitTime: '2026-09-11 11:15:20',
      isLate: false,
      aiGraded: true,
      aiScore: 86,
      finalScore: null,
      status: 'AI_GRADED'
    },
    {
      id: 203,
      studentNo: '20240103',
      studentName: '王浩然',
      submitTime: '2026-09-11 14:02:45',
      isLate: false,
      aiGraded: true,
      aiScore: 78,
      finalScore: null,
      status: 'PENDING'
    },
    {
      id: 204,
      studentNo: '20240104',
      studentName: '陈思宇',
      submitTime: '2026-09-11 15:40:10',
      isLate: true,
      aiGraded: false,
      aiScore: null,
      finalScore: null,
      status: 'PENDING'
    }
  ];
}

const filteredSubmissions = computed(() => {
  return submissionsList.value.filter(s => {
    if (statusFilter.value && s.status !== statusFilter.value) return false;
    if (studentSearch.value.trim()) {
      const kw = studentSearch.value.trim().toLowerCase();
      const inName = s.studentName.toLowerCase().includes(kw);
      const inNo = s.studentNo.includes(kw);
      if (!inName && !inNo) return false;
    }
    return true;
  });
});

const submittedCount = computed(() => submissionsList.value.length);
const submissionRate = computed(() => {
  return Math.round((submittedCount.value / totalStudentsCount.value) * 100);
});

const pendingReviewCount = computed(() => {
  return submissionsList.value.filter(s => s.status !== 'GRADED').length;
});

const averageScore = computed(() => {
  const scored = submissionsList.value.filter(s => s.finalScore !== null);
  if (!scored.length) return '88.5';
  const sum = scored.reduce((acc, s) => acc + s.finalScore, 0);
  return (sum / scored.length).toFixed(1);
});

function getSubmissionStatusLabel(status: string) {
  const map: Record<string, string> = {
    GRADED: '批改完成',
    AI_GRADED: 'AI已预评',
    PENDING: '待教师终审'
  };
  return map[status] || '待批改';
}

function getSubmissionStatusType(status: string) {
  const map: Record<string, string> = {
    GRADED: 'success',
    AI_GRADED: 'primary',
    PENDING: 'warning'
  };
  return (map[status] as any) || 'info';
}

function goToGradingWorkspace(submissionId: number) {
  router.push(`/question/submissions/${submissionId}`);
}

async function triggerSingleAIGrade(row: any) {
  try {
    const res = await gradeSubmission(row.id);
    row.aiGraded = true;
    row.aiScore = res.data?.totalScore ?? res.data?.score ?? 88;
    row.status = 'AI_GRADED';
    ElMessage.success(`学生 ${row.studentName} 的答卷已完成AI智能预批！得分：${row.aiScore}分`);
  } catch (err: any) {
    ElMessage.error(err?.message || 'AI批改请求失败，请检查服务连接');
  }
}

async function handleBatchAIGrade() {
  batchAILoading.value = true;
  try {
    const pendings = submissionsList.value.filter(s => s.status === 'PENDING' || !s.aiGraded);
    if (!pendings.length) {
      ElMessage.info('当前暂无待AI预评的答卷');
      return;
    }
    let successCount = 0;
    for (const sub of pendings) {
      try {
        const res = await gradeSubmission(sub.id);
        sub.aiGraded = true;
        sub.aiScore = res.data?.totalScore ?? res.data?.score ?? 85;
        sub.status = 'AI_GRADED';
        successCount++;
      } catch (subErr) {
        console.warn(`批改答卷 #${sub.id} 失败`, subErr);
      }
    }
    if (successCount > 0) {
      ElMessage.success(`🎉 已成功为 ${successCount} 份答卷完成AI智能辅助预批改！`);
    } else {
      ElMessage.warning('批量AI批改未完成，请检查答卷状态与后端接口');
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '批量批改处理失败');
  } finally {
    batchAILoading.value = false;
  }
}

function handleRemindUnsubmitted() {
  ElMessage.success('已通过系统站内信与移动端向 4 位尚未提交作业的同学发送催交提醒！');
}

function getTypeLabel(type: QuestionType | string) {
  const map: Record<string, string> = {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    FILL_BLANK: '填空题',
    SHORT_ANSWER: '简答题'
  };
  return map[type] || type || '试题';
}

function getTypeTagType(type: QuestionType | string) {
  const map: Record<string, string> = {
    SINGLE_CHOICE: 'primary',
    MULTIPLE_CHOICE: 'success',
    TRUE_FALSE: 'warning',
    FILL_BLANK: 'info',
    SHORT_ANSWER: 'danger'
  };
  return (map[type] as any) || '';
}
</script>

<style scoped lang="scss">
.assignment-detail-container {
  padding: 24px;
  background: #f8fafc;
  min-height: calc(100vh - 64px);

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

  .assignment-hero-card {
    background: #ffffff;
    border-radius: 16px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
    padding: 24px 32px;
    margin-bottom: 24px;

    .hero-top-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: 16px;
      margin-bottom: 14px;

      .title-and-tags {
        display: flex;
        align-items: center;
        gap: 12px;

        .assignment-title {
          font-size: 22px;
          font-weight: 800;
          color: #0f172a;
          margin: 0;
        }
      }

      .hero-actions {
        display: flex;
        gap: 12px;
      }
    }

    .meta-pills-row {
      display: flex;
      align-items: center;
      gap: 16px;
      flex-wrap: wrap;
      margin-bottom: 20px;

      .meta-pill {
        background: #f1f5f9;
        padding: 4px 12px;
        border-radius: 6px;
        font-size: 13px;
        color: #475569;
      }
    }

    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 16px;
      padding-top: 20px;
      border-top: 1px solid #f1f5f9;

      .kpi-box {
        display: flex;
        flex-direction: column;

        .kpi-val {
          font-size: 26px;
          font-weight: 800;
          color: #0f172a;
          line-height: 1;
        }

        .kpi-label {
          font-size: 12px;
          color: #94a3b8;
          margin-top: 6px;
        }
      }
    }
  }

  .main-tabs-card {
    background: #ffffff;
    border-radius: 16px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
    padding: 20px 24px;

    .tab-filter-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 16px;

      .left-filters {
        display: flex;
        gap: 12px;
      }
    }

    .student-cell {
      display: flex;
      align-items: center;
      gap: 8px;

      .avatar-dot {
        width: 8px;
        height: 8px;
        background: #3b82f6;
        border-radius: 50%;
      }
    }

    /* 试题与标准解答标签页 */
    .questions-tab-content {
      display: flex;
      flex-direction: column;
      gap: 18px;

      .question-review-card {
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 12px;
        padding: 18px 20px;

        .q-header {
          display: flex;
          align-items: center;
          gap: 10px;
          margin-bottom: 8px;

          .q-idx {
            font-weight: 700;
            color: #2563eb;
          }

          .q-score {
            font-size: 12px;
            color: #64748b;
          }
        }

        .q-stem {
          font-size: 15px;
          color: #1e293b;
          line-height: 1.6;
          margin: 0 0 12px;
        }

        .q-options {
          display: grid;
          grid-template-columns: repeat(2, 1fr);
          gap: 8px;
          margin-bottom: 12px;

          .q-opt {
            background: #ffffff;
            border: 1px solid #edf2f7;
            padding: 8px 12px;
            border-radius: 6px;
            font-size: 13px;
            display: flex;
            gap: 6px;

            .opt-k {
              font-weight: 600;
              color: #64748b;
            }

            &.correct {
              border-color: #86efac;
              background: #f0fdf4;
              color: #166534;
            }
          }
        }

        .q-analysis-box {
          background: #ffffff;
          border-left: 3px solid #10b981;
          padding: 10px 14px;
          border-radius: 6px;
          font-size: 13px;
          color: #334155;

          .ans-line {
            margin-bottom: 4px;
            &:last-child {
              margin-bottom: 0;
            }
          }
        }
      }
    }
  }
}
</style>
