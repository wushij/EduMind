<template>
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
        :ref="bindFormRef"
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
                @change="onCourseChange"
              >
                <el-option
                  v-for="c in courses"
                  :key="c.id"
                  :label="c.title || c.name"
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
              <el-radio-group
                :model-value="sourceMode"
                size="default"
                @update:model-value="$emit('update:sourceMode', $event)"
              >
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
                @change="onExamSelected"
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
          <el-button size="large" @click="$emit('cancel')">取消</el-button>
          <el-button
            type="primary"
            size="large"
            :loading="submitting"
            class="publish-btn"
            @click="$emit('publish')"
          >
            <el-icon class="btn-icon"><Promotion /></el-icon>
            <span>确认并正式发布作业</span>
          </el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus';
import { Promotion, Service, Timer, View } from '@element-plus/icons-vue';
import type { Course } from '@/types/course/course';
import type { QuestionItem, QuestionType } from '@/types/question/question';

const props = defineProps<{
  setFormRef: (el: FormInstance | undefined) => void;
  formData: {
    title: string;
    courseId?: number;
    deadline: string;
    totalScore: number;
    passScore: number;
    examId?: number;
    aiGradingEnabled: boolean;
    allowLate: boolean;
    instantFeedback: boolean;
  };
  rules: FormRules;
  courses: Course[];
  examOptions: any[];
  sourceMode: 'EXAM' | 'QUESTIONS';
  selectedQuestions: QuestionItem[];
  totalCalculatedScore: number;
  submitting: boolean;
  onCourseChange: (courseId?: number) => void;
  onExamSelected: (id?: number) => void;
  getTypeLabel: (type: QuestionType | string) => string;
  getTypeTagType: (type: QuestionType | string) => string;
}>();

defineEmits<{
  cancel: [];
  publish: [];
  'update:sourceMode': [value: 'EXAM' | 'QUESTIONS'];
}>();

function bindFormRef(el: FormInstance | null) {
  props.setFormRef(el ?? undefined);
}
</script>

<style scoped lang="scss">
.form-wrapper {
  width: 100%;
  max-width: none;

  .main-card {
    background: #ffffff;
    border-radius: 20px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 4px 20px rgba(30, 80, 150, 0.04);
    padding: 24px 28px;

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
</style>
