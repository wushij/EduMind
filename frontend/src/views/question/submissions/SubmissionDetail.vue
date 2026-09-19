<template>
  <div class="question-module-page submission-detail-page">
    <SubmissionDetailHeroSection
      :router="router"
      :loading="loading"
      :submission-data="submissionData"
      :total-score="calculatedTotalScore"
      :question-count="gradingItems.length"
    />

    <div class="submission-detail-main">
      <div v-loading="loading" class="submission-grading-stream">
        <template v-if="gradingItems.length > 0">
        <div
          v-for="(item, idx) in gradingItems"
          :key="item.questionId"
          class="grading-question-card"
        >
          <div class="card-top-bar">
            <div class="left-meta">
              <span class="index-badge">{{ String(idx + 1).padStart(2, '0') }}</span>
              <span class="pill-badge pill-badge--type">{{ getTypeLabel(item.type) }}</span>
              <span class="pill-badge pill-badge--score">满分 {{ item.maxScore }} 分</span>
              <span class="pill-badge pill-badge--ai">
                AI 建议 {{ item.aiScore ?? '—' }} 分
              </span>
            </div>
            <button type="button" class="module-capsule-btn module-capsule-btn--secondary" @click="adoptSingleAIScore(item)">
              <el-icon><Select /></el-icon>
              <span>采纳 AI 得分</span>
            </button>
          </div>

          <div class="q-stem-body">{{ item.stem }}</div>

          <div class="answer-block answer-block--student">
            <div class="block-title">
              <span class="title-left">
                <el-icon><User /></el-icon>
                <span>考生提交作答</span>
              </span>
              <span
                v-if="item.isObjective"
                class="objective-tag"
                :class="{ 'objective-tag--pass': item.isCorrect }"
              >
                {{ item.isCorrect ? '客观比对正确' : '客观比对错误' }}
              </span>
            </div>
            <div class="block-body">{{ item.studentAnswer || '（考生未作答）' }}</div>
          </div>

          <div class="answer-block answer-block--standard">
            <div class="block-title">
              <span class="title-left">
                <el-icon><Reading /></el-icon>
                <span>标准参考答案</span>
              </span>
            </div>
            <div class="block-body">
              <div><strong class="text-emerald-700">参考答案：</strong>{{ item.standardAnswer }}</div>
              <div v-if="item.analysis" class="mt-2 text-slate-600">
                <strong>解析：</strong>{{ item.analysis }}
              </div>
            </div>
          </div>

          <div v-if="item.aiComment" class="answer-block answer-block--ai">
            <div class="block-title">
              <span class="title-left">
                <el-icon><MagicStick /></el-icon>
                <span>AI 评阅说明</span>
              </span>
            </div>
            <div class="block-body">{{ item.aiComment }}</div>
          </div>

          <div class="teacher-grading-dock">
            <div class="score-input-group">
              <span class="input-label">本题最终得分</span>
              <el-input-number
                v-model="item.teacherScore"
                :min="0"
                :max="item.maxScore"
                :step="1"
                class="score-input-number"
              />
              <span class="unit">/ {{ item.maxScore }} 分</span>
            </div>
            <div class="comment-input-group">
              <el-input
                v-model="item.teacherComment"
                placeholder="填写个性化评阅批语（选填）"
                clearable
              />
            </div>
          </div>
        </div>
      </template>

      <div v-else class="questions-list-card submission-empty-card">
        <div class="empty-questions-panel">
        <div class="empty-icon">
          <el-icon><MagicStick /></el-icon>
        </div>
        <h3 class="empty-title">尚未生成评阅明细</h3>
        <p class="empty-text">点击下方按钮触发 AI 深度评阅，系统将按题输出建议得分与评阅说明。</p>
        <button
          type="button"
          class="module-capsule-btn module-capsule-btn--ai"
          :disabled="gradingInProgress"
          @click="handleTriggerGradeNow"
        >
          <el-icon><MagicStick /></el-icon>
          <span>{{ gradingInProgress ? 'AI 评阅中…' : '立即执行 AI 智能批改' }}</span>
        </button>
        </div>
      </div>
      </div>

      <footer class="submission-bottom-bar">
        <div class="bar-left">
          <button type="button" class="module-capsule-btn module-capsule-btn--secondary" @click="adoptAllAIScores">
            <el-icon><Select /></el-icon>
            <span>一键采纳全卷 AI 得分</span>
          </button>
        </div>

        <div class="bar-total">
          当前累计总分
          <strong>{{ calculatedTotalScore }}</strong>
          / {{ submissionData?.totalScore || 100 }} 分
        </div>

        <div class="bar-right">
          <button
            type="button"
            class="module-capsule-btn module-capsule-btn--primary"
            :disabled="saving"
            @click="handleSaveGrading"
          >
            <el-icon><Finished /></el-icon>
            <span>{{ saving ? '保存中…' : '保存并确认最终成绩' }}</span>
          </button>
        </div>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { User, Reading, MagicStick, Select, Finished } from '@element-plus/icons-vue';
import SubmissionDetailHeroSection from '@/components/question/submission/SubmissionDetailHeroSection.vue';
import { useSubmission } from '@/composables/question/useSubmission';

const {
  router,
  loading,
  saving,
  gradingInProgress,
  submissionData,
  gradingItems,
  calculatedTotalScore,
  handleTriggerGradeNow,
  adoptSingleAIScore,
  adoptAllAIScores,
  handleSaveGrading,
  getTypeLabel
} = useSubmission();
</script>

<style scoped lang="scss">
@use '@/styles/question/module-page-shell.scss';
@use '@/styles/question/question-list-panel.scss';
@use '@/styles/question/submission-panel.scss';

.submission-detail-page {
  padding-bottom: 8px;
}
</style>
