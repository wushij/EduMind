<template>
  <div class="setup-layout">
    <el-card shadow="never" class="setup-card">
      <template #header>
        <div class="setup-header">
          <div class="setup-title">
            <el-icon class="title-icon"><Operation /></el-icon>
            <span>靶向练习生成配置</span>
          </div>
          <el-tag type="primary" effect="plain">当前模式：{{ currentModeName }}</el-tag>
        </div>
      </template>

      <div class="setup-grid" v-loading="snapshotLoading">
        <el-form label-position="top" class="setup-form setup-form-main">
          <el-row :gutter="20">
            <el-col :xs="24" :md="12">
              <el-form-item label="目标课程">
                <el-select
                  :model-value="teacherCourseId"
                  placeholder="选择课程"
                  class="w-full"
                  @update:model-value="onTeacherCourseIdChange"
                >
                  <el-option
                    v-for="c in courseOptions"
                    :key="c.id"
                    :label="c.name"
                    :value="c.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="核心考点范围">
                <el-select
                  :model-value="selectedKpId"
                  placeholder="全范围自适应 / 指定考点"
                  class="w-full"
                  @update:model-value="onSelectedKpIdChange"
                >
                  <el-option
                    v-for="kp in kpOptions"
                    :key="kp.id"
                    :label="kp.name"
                    :value="kp.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="布鲁姆认知分层偏好">
            <div class="cognitive-pills">
              <button
                v-for="opt in cognitiveOptions"
                :key="opt.value"
                type="button"
                class="cognitive-pill"
                :class="{ active: cognitiveLevel === opt.value }"
                @click="onCognitiveLevelChange(opt.value)"
              >
                {{ opt.label }}
              </button>
            </div>
          </el-form-item>

          <el-row :gutter="20">
            <el-col :xs="24" :md="12">
              <el-form-item label="练习题量">
                <el-radio-group
                  :model-value="questionCount"
                  @update:model-value="onQuestionCountChange"
                >
                  <el-radio :label="3">3 题 (快速微测 · 5分钟)</el-radio>
                  <el-radio :label="5">5 题 (标准专项 · 12分钟)</el-radio>
                  <el-radio :label="10">10 题 (深度攻坚 · 25分钟)</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="即时反馈模式">
                <el-switch
                  :model-value="instantFeedback"
                  active-text="每题答完后立即展示 AI 认知解析与评分"
                  inactive-text="全部提交后统一生成分析报告"
                  @update:model-value="onInstantFeedbackChange"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <div v-if="loadError" class="load-error">
            <el-icon><WarningFilled /></el-icon>
            <span>{{ loadError }}</span>
          </div>

          <div class="submit-action-row">
            <el-button
              type="primary"
              size="large"
              class="generate-btn"
              :loading="generating"
              @click="onStartPractice"
            >
              <el-icon><Compass /></el-icon>
              <span>AI 智能生成自适应练习集</span>
            </el-button>
          </div>
        </el-form>

        <aside class="snapshot-panel">
          <h4 class="snapshot-title">学情快照</h4>
          <p v-if="sessionMeta.weakPointHint" class="snapshot-hint">{{ sessionMeta.weakPointHint }}</p>
          <div class="snapshot-stats">
            <div class="snap-stat">
              <span class="snap-value">{{ sessionMeta.weakKnowledgePointCount }}</span>
              <span class="snap-label">薄弱考点</span>
            </div>
            <div class="snap-stat">
              <span class="snap-value">{{ sessionMeta.pendingWrongQuestionCount }}</span>
              <span class="snap-label">待练错题</span>
            </div>
            <div class="snap-stat">
              <span class="snap-value">{{ sessionMeta.estimatedMinutes }}</span>
              <span class="snap-label">预计分钟</span>
            </div>
          </div>
          <div v-if="weakKpPreview.length" class="weak-kp-list">
            <div v-for="kp in weakKpPreview" :key="kp.id" class="weak-kp-row">
              <span class="weak-kp-name">{{ kp.name }}</span>
              <el-progress
                :percentage="kp.mastery ?? 0"
                :stroke-width="6"
                :color="(kp.mastery ?? 0) < 50 ? '#F5222D' : '#FA8C16'"
              />
            </div>
          </div>
        </aside>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Operation, Compass, WarningFilled } from '@element-plus/icons-vue';

const props = defineProps<{
  teacherCourseId: number;
  courseOptions: Array<{ id: number; name: string }>;
  kpOptions: Array<{ id: number; name: string; mastery?: number }>;
  selectedKpId: number;
  cognitiveLevel: string;
  questionCount: number;
  instantFeedback: boolean;
  generating: boolean;
  snapshotLoading: boolean;
  loadError: string | null;
  currentModeName: string;
  sessionMeta: {
    weakPointHint: string;
    estimatedMinutes: number;
    weakKnowledgePointCount: number;
    pendingWrongQuestionCount: number;
  };
  onTeacherCourseIdChange: (id: number) => void;
  onSelectedKpIdChange: (id: number) => void;
  onCognitiveLevelChange: (level: string) => void;
  onQuestionCountChange: (count: number) => void;
  onInstantFeedbackChange: (value: boolean) => void;
  onStartPractice: () => void;
}>();

const cognitiveOptions = [
  { value: 'ALL', label: '均衡演练' },
  { value: 'UNDERSTAND', label: '识记理解' },
  { value: 'APPLY', label: '综合应用' },
  { value: 'EVALUATE', label: '高阶探究' }
];

const weakKpPreview = computed(() =>
  props.kpOptions
    .filter((k) => k.id > 0 && k.mastery != null && k.mastery < 70)
    .slice(0, 4)
);
</script>

<style scoped lang="scss">
.setup-card {
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  background: linear-gradient(135deg, #fff 0%, #f8fbff 100%);

  .setup-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;

    .setup-title {
      font-size: 16px;
      font-weight: 700;
      color: #0f172a;
      display: flex;
      align-items: center;
      gap: 8px;

      .title-icon {
        color: #1677ff;
      }
    }
  }

  .setup-grid {
    display: grid;
    grid-template-columns: 1fr 280px;
    gap: 24px;

    @media (max-width: 960px) {
      grid-template-columns: 1fr;
    }
  }

  .cognitive-pills {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;

    .cognitive-pill {
      border: 1px solid #cbd5e1;
      background: #fff;
      border-radius: 9999px;
      padding: 8px 16px;
      font-size: 13px;
      font-weight: 600;
      color: #475569;
      cursor: pointer;
      transition: all 0.2s;

      &.active {
        border-color: #1677ff;
        background: #eff6ff;
        color: #1677ff;
      }

      &:hover {
        border-color: #93c5fd;
      }
    }
  }

  .load-error {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #fa8c16;
    font-size: 13px;
    margin-bottom: 12px;
  }

  .submit-action-row {
    display: flex;
    justify-content: center;
    margin-top: 16px;

    .generate-btn {
      padding: 14px 36px;
      font-size: 16px;
      font-weight: 700;
      border-radius: 9999px;
      background: linear-gradient(135deg, #1677ff 0%, #722ed1 100%);
      border: none;
      box-shadow: 0 6px 20px rgba(22, 119, 255, 0.25);
    }
  }

  .snapshot-panel {
    background: rgba(255, 255, 255, 0.85);
    border: 1px solid #e2e8f0;
    border-radius: 14px;
    padding: 16px;

    .snapshot-title {
      margin: 0 0 8px;
      font-size: 14px;
      font-weight: 700;
      color: #0f172a;
    }

    .snapshot-hint {
      margin: 0 0 12px;
      font-size: 12px;
      line-height: 1.5;
      color: #64748b;
    }

    .snapshot-stats {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 8px;
      margin-bottom: 16px;

      .snap-stat {
        text-align: center;
        background: #f8fafc;
        border-radius: 10px;
        padding: 10px 6px;

        .snap-value {
          display: block;
          font-size: 20px;
          font-weight: 800;
          color: #1677ff;
        }

        .snap-label {
          font-size: 11px;
          color: #64748b;
        }
      }
    }

    .weak-kp-list {
      display: flex;
      flex-direction: column;
      gap: 10px;

      .weak-kp-name {
        font-size: 12px;
        color: #334155;
        margin-bottom: 4px;
        display: block;
      }
    }
  }
}
</style>
