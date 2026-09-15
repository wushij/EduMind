<template>
  <div class="assignment-create-container">
    <!-- 顶部返回与标题 -->
    <div class="top-nav-bar">
      <el-button :icon="ArrowLeft" link class="back-link" @click="router.push('/question/assignments')">
        返回作业列表
      </el-button>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/question/assignments' }">作业管理</el-breadcrumb-item>
        <el-breadcrumb-item>发布新作业</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 表单卡片 -->
    <div class="form-wrapper">
      <el-card shadow="never" class="main-card">
        <template #header>
          <div class="card-header-title">
            <el-icon class="icon"><Promotion /></el-icon>
            <div>
              <h3>发布新作业 / 课后评测</h3>
              <p>为班级学生布置在线课后作业或章节阶段考核，支持从试卷库关联或自主题库抽题，并可开启AI辅助批改。</p>
            </div>
          </div>
        </template>

        <el-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-position="top"
          class="assignment-form"
        >
          <!-- 区域 1：基本信息 -->
          <div class="form-section-title">1. 作业基础配置</div>
          <el-row :gutter="24">
            <el-col :span="16">
              <el-form-item label="作业名称 / 考核标题" prop="title">
                <el-input
                  v-model="formData.title"
                  placeholder="例如：第三章：树与二叉树遍历课后巩固测试"
                  size="large"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="所属课程" prop="courseId">
                <el-select
                  v-model="formData.courseId"
                  placeholder="请选择课程"
                  size="large"
                  class="w-full"
                  @change="handleCourseChange"
                >
                  <el-option
                    v-for="c in courses"
                    :key="c.id"
                    :label="c.title"
                    :value="c.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="截止提交时间" prop="deadline">
                <el-date-picker
                  v-model="formData.deadline"
                  type="datetime"
                  placeholder="选择截止提交日期与时间"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  size="large"
                  class="w-full"
                />
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="卷面总分">
                <el-input-number
                  v-model="formData.totalScore"
                  :min="1"
                  :max="200"
                  size="large"
                  class="w-full"
                />
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="合格标准分">
                <el-input-number
                  v-model="formData.passScore"
                  :min="1"
                  :max="formData.totalScore"
                  size="large"
                  class="w-full"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <!-- 区域 2：作业试题内容来源 -->
          <div class="form-section-title">2. 考题内容装载来源</div>
          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="内容装载模式">
                <el-radio-group v-model="sourceMode" size="default">
                  <el-radio-button label="EXAM">关联已有标准化试卷</el-radio-button>
                  <el-radio-button label="QUESTIONS">自定义勾选入题</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-col>

            <el-col v-if="sourceMode === 'EXAM'" :span="12">
              <el-form-item label="选择引用的试卷" prop="examId">
                <el-select
                  v-model="formData.examId"
                  placeholder="选择已发布的完整试卷"
                  size="default"
                  class="w-full"
                  @change="handleExamSelected"
                >
                  <el-option
                    v-for="e in examOptions"
                    :key="e.id"
                    :label="`${e.title} (${e.totalScore}分)`"
                    :value="e.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <!-- 预览所选试题 -->
          <div class="selected-questions-preview-box">
            <div class="preview-header">
              <span>已装载试题清单 (共 {{ selectedQuestions.length }} 道)</span>
              <span class="score-sum">总计：{{ totalCalculatedScore }} 分</span>
            </div>

            <div v-if="selectedQuestions.length > 0" class="questions-compact-list">
              <div
                v-for="(q, idx) in selectedQuestions"
                :key="q.id"
                class="compact-question-item"
              >
                <span class="q-num">{{ idx + 1 }}.</span>
                <div class="q-detail">
                  <div class="top-tag-line">
                    <el-tag size="small" :type="getTypeTagType(q.type)">{{ getTypeLabel(q.type) }}</el-tag>
                    <span class="q-score">{{ q.score || 5 }}分</span>
                    <span class="q-kp">{{ q.knowledgePointNames?.[0] }}</span>
                  </div>
                  <p class="stem-preview">{{ q.stem }}</p>
                </div>
              </div>
            </div>

            <div v-else class="preview-empty">
              当前暂未装载试题，请从上方选择试卷或配置试题。
            </div>
          </div>

          <!-- 区域 3：智能化与批改规则 -->
          <div class="form-section-title">3. 智能评阅与提交行为规则</div>
          <div class="rules-switches-grid">
            <div class="switch-item">
              <div class="switch-meta">
                <span class="switch-title">
                  <el-icon class="switch-icon"><Service /></el-icon>
                  <span>启用 AI 智能自动预批改</span>
                </span>
                <span class="switch-desc">学生提交主观题后，自动调用大模型根据参考答案进行评分、错因分析与评语建议。</span>
              </div>
              <el-switch v-model="formData.aiGradingEnabled" />
            </div>

            <div class="switch-item">
              <div class="switch-meta">
                <span class="switch-title">
                  <el-icon class="switch-icon"><Timer /></el-icon>
                  <span>允许逾期迟交</span>
                </span>
                <span class="switch-desc">超过截止时间后仍允许学生补交答卷，并在批改列表中标记“迟交”。</span>
              </div>
              <el-switch v-model="formData.allowLate" />
            </div>

            <div class="switch-item">
              <div class="switch-meta">
                <span class="switch-title">
                  <el-icon class="switch-icon"><View /></el-icon>
                  <span>提交后立即可见客观题得分与解析</span>
                </span>
                <span class="switch-desc">学生提交后立即展示客观题（单选、多选、判断）对错反馈与文字解析。</span>
              </div>
              <el-switch v-model="formData.instantFeedback" />
            </div>
          </div>

          <!-- 底部提交操作栏 -->
          <div class="form-actions-dock">
            <el-button size="large" @click="router.push('/question/assignments')">取消</el-button>
            <el-button
              type="primary"
              size="large"
              :loading="submitting"
              class="publish-btn"
              @click="handlePublishAssignment"
            >
              <el-icon class="btn-icon"><Promotion /></el-icon>
              <span>确认并正式发布作业</span>
            </el-button>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { ArrowLeft, Promotion, Service, Timer, View } from '@element-plus/icons-vue';
import { createAssignment, publishAssignment } from '@/api/question/assignment';
import { getCourseList } from '@/api/course/course';
import { getExams } from '@/api/question/exam';
import type { Course } from '@/types/course/course';
import type { QuestionItem, QuestionType } from '@/types/question/question';
import { normalizeQuestionList } from '@/utils/question/normalize-question';

const router = useRouter();
const route = useRoute();

const formRef = ref<FormInstance>();
const submitting = ref(false);
const courses = ref<Course[]>([]);
const examOptions = ref<any[]>([]);
const sourceMode = ref<'EXAM' | 'QUESTIONS'>('EXAM');

const formData = reactive({
  title: '',
  courseId: 101 as number | undefined,
  deadline: '2026-09-30 23:59:59',
  totalScore: 100,
  passScore: 60,
  examId: undefined as number | undefined,
  aiGradingEnabled: true,
  allowLate: false,
  instantFeedback: true
});

const rules = reactive<FormRules>({
  title: [{ required: true, message: '请输入作业名称', trigger: 'blur' }],
  courseId: [{ required: true, message: '请选择所属课程', trigger: 'change' }],
  deadline: [{ required: true, message: '请选择截止时间', trigger: 'change' }]
});

// 装载试题
const selectedQuestions = ref<QuestionItem[]>([]);

onMounted(async () => {
  await loadCourses();
  await loadExams();

  // 若通过路由传参自动填充
  if (route.query.examId) {
    formData.examId = Number(route.query.examId);
    if (route.query.courseId) formData.courseId = Number(route.query.courseId);
    if (route.query.title) formData.title = `${route.query.title} - 课堂作业测验`;
    handleExamSelected(formData.examId);
  } else {
    // 默认选用第一个试卷作为预填演示
    if (examOptions.value.length > 0) {
      formData.examId = examOptions.value[0].id;
      formData.title = '第三周：二叉树遍历与递归算法平时作业';
      handleExamSelected(formData.examId);
    }
  }
});

async function loadCourses() {
  try {
    const res = await getCourseList({ page: 1, pageSize: 50 });
    courses.value = res.data?.list || [];
  } catch (err: any) {
    courses.value = [];
    ElMessage.error(err?.message || '获取课程列表失败');
  }
}

async function loadExams() {
  try {
    const res = await getExams({ pageSize: 50 });
    examOptions.value = res.data?.list || [];
  } catch (err: any) {
    examOptions.value = [];
    ElMessage.error(err?.message || '获取试卷列表失败');
  }
}

function handleCourseChange(courseId?: number) {
  // 可按课程联动试卷
}

function handleExamSelected(id?: number) {
  const e = examOptions.value.find(item => item.id === id);
  if (e) {
    formData.totalScore = e.totalScore || 100;
    formData.passScore = e.passScore || 60;
    selectedQuestions.value = e.questions?.length
      ? normalizeQuestionList(e.questions as Record<string, unknown>[])
      : [];
  }
}

const totalCalculatedScore = computed(() => {
  return selectedQuestions.value.reduce((acc, q) => acc + (q.score || 5), 0);
});

async function handlePublishAssignment() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    if (selectedQuestions.value.length === 0) {
      ElMessage.warning('作业中至少需要包含一道试题！');
      return;
    }

    submitting.value = true;
    try {
      const payload = {
        title: formData.title,
        courseId: formData.courseId,
        deadline: formData.deadline,
        totalScore: formData.totalScore,
        passScore: formData.passScore,
        examId: formData.examId,
        settings: {
          aiGradingEnabled: formData.aiGradingEnabled,
          allowLate: formData.allowLate,
          instantFeedback: formData.instantFeedback
        },
        questionIds: selectedQuestions.value.map(q => q.id)
      };

      const res = await createAssignment(payload);
      const assignmentId = res.data;
      if (assignmentId) {
        await publishAssignment(assignmentId);
      }
      ElMessage.success('作业已成功发布，学生现已可在学生端查收并作答！');
      router.push('/question/assignments');
    } catch (err: any) {
      console.error('发布作业失败:', err);
      ElMessage.error(err?.message || '作业发布失败，请检查网络或后端接口状态');
    } finally {
      submitting.value = false;
    }
  });
}

function getTypeLabel(type: QuestionType | string) {
  const map: Record<string, string> = {
    SINGLE_CHOICE: '单选',
    MULTIPLE_CHOICE: '多选',
    TRUE_FALSE: '判断',
    FILL_BLANK: '填空',
    SHORT_ANSWER: '简答'
  };
  return map[type] || type || '单选';
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
.assignment-create-container {
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

  .form-wrapper {
    max-width: 900px;
    margin: 0 auto;

    .main-card {
      background: #ffffff;
      border-radius: 16px;
      border: 1px solid #e2e8f0;
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
      padding: 16px 24px;

      .card-header-title {
        display: flex;
        align-items: center;
        gap: 16px;

        .icon {
          font-size: 28px;
          color: #2563eb;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        h3 {
          margin: 0;
          font-size: 18px;
          font-weight: 700;
          color: #0f172a;
        }

        p {
          margin: 4px 0 0;
          font-size: 13px;
          color: #64748b;
        }
      }

      .assignment-form {
        margin-top: 20px;

        .form-section-title {
          font-size: 15px;
          font-weight: 700;
          color: #1e293b;
          border-left: 3px solid #2563eb;
          padding-left: 10px;
          margin: 28px 0 16px;

          &:first-child {
            margin-top: 8px;
          }
        }

        .selected-questions-preview-box {
          background: #f8fafc;
          border: 1px solid #e2e8f0;
          border-radius: 12px;
          padding: 16px 20px;
          margin-bottom: 24px;

          .preview-header {
            display: flex;
            justify-content: space-between;
            font-size: 14px;
            font-weight: 600;
            color: #334155;
            margin-bottom: 12px;

            .score-sum {
              color: #2563eb;
            }
          }

          .questions-compact-list {
            display: flex;
            flex-direction: column;
            gap: 10px;

            .compact-question-item {
              background: #ffffff;
              border: 1px solid #eef2f6;
              border-radius: 8px;
              padding: 10px 14px;
              display: flex;
              gap: 10px;

              .q-num {
                font-weight: 700;
                color: #64748b;
                font-size: 13px;
              }

              .q-detail {
                flex: 1;

                .top-tag-line {
                  display: flex;
                  align-items: center;
                  gap: 8px;
                  margin-bottom: 4px;

                  .q-score {
                    font-size: 12px;
                    color: #2563eb;
                    font-weight: 600;
                  }

                  .q-kp {
                    font-size: 11px;
                    color: #94a3b8;
                  }
                }

                .stem-preview {
                  font-size: 13px;
                  color: #334155;
                  line-height: 1.5;
                  margin: 0;
                }
              }
            }
          }

          .preview-empty {
            text-align: center;
            padding: 24px;
            color: #94a3b8;
            font-size: 13px;
          }
        }

        .rules-switches-grid {
          display: flex;
          flex-direction: column;
          gap: 14px;
          background: #f8fafc;
          border-radius: 12px;
          padding: 16px 20px;
          border: 1px solid #e2e8f0;

          .switch-item {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 20px;
            padding-bottom: 12px;
            border-bottom: 1px solid #edf2f7;

            &:last-child {
              padding-bottom: 0;
              border-bottom: none;
            }

            .switch-meta {
              display: flex;
              flex-direction: column;

              .switch-title {
                display: flex;
                align-items: center;
                gap: 6px;
                font-size: 14px;
                font-weight: 600;
                color: #1e293b;

                .switch-icon {
                  font-size: 16px;
                  color: #2563eb;
                }
              }

              .switch-desc {
                font-size: 12px;
                color: #64748b;
                margin-top: 2px;
              }
            }
          }
        }

        .form-actions-dock {
          display: flex;
          justify-content: flex-end;
          gap: 16px;
          margin-top: 32px;
          padding-top: 20px;
          border-top: 1px solid #f1f5f9;

          .publish-btn {
            background: #2563eb;
            border-color: #2563eb;
            font-weight: 600;
            padding: 10px 28px;
          }
        }
      }
    }
  }
}
</style>
