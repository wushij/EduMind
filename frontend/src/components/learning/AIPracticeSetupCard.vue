<template>
  <div class="setup-layout">
    <div class="setup-card" v-loading="snapshotLoading">
      <!-- 1. 顶部 Header -->
      <div class="setup-header">
        <div class="header-left">
          <div class="header-avatar-badge">
            <el-icon><Operation /></el-icon>
          </div>
          <div class="header-text-group">
            <div class="header-title-row">
              <h3 class="setup-title">靶向练习生成配置</h3>
              <span class="mode-pill-badge">
                <el-icon class="mr-1"><Aim /></el-icon>
                训练模式：{{ currentModeName }}
              </span>
            </div>
            <p class="setup-desc">
              结合布鲁姆认知分层与个人学情画像，实时动态调度变式题库，生成自适应靶向训练集。
            </p>
          </div>
        </div>
      </div>

      <!-- 2. 主体网格：左侧精细化表单 + 右侧学情透视与策略看板 -->
      <div class="setup-grid">
        <!-- 左侧配置主表单 -->
        <div class="setup-form-panel">
          <!-- 分区 1：核心目标范围 -->
          <div class="config-section">
            <div class="section-label">
              <span class="section-badge">1</span>
              <span class="section-title">课程与考点范围</span>
            </div>

            <div class="scope-selectors-grid">
              <div class="selector-item">
                <span class="field-title">
                  <el-icon class="field-icon"><Reading /></el-icon>
                  目标课程
                </span>
                <el-select
                  :model-value="teacherCourseId"
                  placeholder="选择修读课程"
                  class="styled-select w-full"
                  @update:model-value="onTeacherCourseIdChange"
                >
                  <el-option
                    v-for="c in courseOptions"
                    :key="c.id"
                    :label="c.name"
                    :value="c.id"
                  />
                </el-select>
              </div>

              <div class="selector-item">
                <span class="field-title">
                  <el-icon class="field-icon"><CollectionTag /></el-icon>
                  核心考点范围
                </span>
                <el-select
                  :model-value="selectedKpId"
                  placeholder="全范围自适应 / 指定考点"
                  class="styled-select w-full"
                  @update:model-value="onSelectedKpIdChange"
                >
                  <el-option
                    v-for="kp in kpOptions"
                    :key="kp.id"
                    :label="kp.name"
                    :value="kp.id"
                  />
                </el-select>
              </div>
            </div>
          </div>

          <!-- 分区 2：布鲁姆认知偏好分层 -->
          <div class="config-section">
            <div class="section-label">
              <span class="section-badge">2</span>
              <span class="section-title">布鲁姆认知分层偏好</span>
              <span class="section-subtitle">动态调控练习集试题在各认知维度的深度分布</span>
            </div>

            <div class="cognitive-cards-grid">
              <div
                v-for="opt in cognitiveOptions"
                :key="opt.value"
                class="cognitive-option-card"
                :class="{ active: cognitiveLevel === opt.value }"
                @click="onCognitiveLevelChange(opt.value)"
              >
                <div class="card-radio-indicator">
                  <span class="dot"></span>
                </div>
                <div class="card-content">
                  <span class="card-title">{{ opt.label }}</span>
                  <span class="card-desc">{{ opt.desc }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 分区 3：题量与反馈模式 -->
          <div class="config-section">
            <div class="section-label">
              <span class="section-badge">3</span>
              <span class="section-title">练习题量与即时反馈</span>
            </div>

            <div class="pacing-grid">
              <!-- 题量卡片单选 -->
              <div class="pacing-col">
                <span class="field-title">
                  <el-icon class="field-icon"><Opportunity /></el-icon>
                  训练题量
                </span>
                <div class="count-cards-group">
                  <div
                    v-for="item in questionCountOptions"
                    :key="item.value"
                    class="count-card-item"
                    :class="{
                      active: questionCount === item.value,
                      'is-recommended': item.recommended
                    }"
                    @click="onQuestionCountChange(item.value)"
                  >
                    <div class="count-num">{{ item.value }} 题</div>
                    <div class="count-meta">
                      <span class="count-type">{{ item.label }}</span>
                      <span class="count-time">{{ item.time }}</span>
                    </div>
                    <span v-if="item.recommended" class="recommend-tag">推荐</span>
                  </div>
                </div>
              </div>

              <!-- 反馈模式卡片 -->
              <div class="pacing-col">
                <span class="field-title">
                  <el-icon class="field-icon"><Lightning /></el-icon>
                  即时辅导模式
                </span>
                <div
                  class="feedback-mode-card"
                  :class="{ 'is-instant': instantFeedback }"
                  @click="onInstantFeedbackChange(!instantFeedback)"
                >
                  <div class="feedback-card-left">
                    <div class="feedback-icon-box">
                      <el-icon v-if="instantFeedback"><Lightning /></el-icon>
                      <el-icon v-else><TrendCharts /></el-icon>
                    </div>
                    <div class="feedback-info">
                      <span class="feedback-main-title">
                        {{ instantFeedback ? '实时即刻批改 & AI 认知解析' : '作答结束统一出具报告' }}
                      </span>
                      <span class="feedback-sub-text">
                        {{ instantFeedback ? '每作答一题即刻获取答案比对、思路启发与前驱薄弱归因' : '模拟真实限时考场环境，交卷后统一生成完整学情分析' }}
                      </span>
                    </div>
                  </div>
                  <el-switch
                    :model-value="instantFeedback"
                    class="feedback-switch"
                    @click.stop
                    @update:model-value="onInstantFeedbackChange"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- 错误提醒 -->
          <div v-if="loadError" class="load-error-banner">
            <el-icon class="error-icon"><WarningFilled /></el-icon>
            <span>{{ loadError }}</span>
          </div>

          <!-- 底部生成按钮 -->
          <div class="submit-action-box">
            <button
              type="button"
              class="generate-action-btn"
              :disabled="generating"
              @click="onStartPractice"
            >
              <el-icon v-if="generating" class="is-loading"><Compass /></el-icon>
              <el-icon v-else><Compass /></el-icon>
              <span>{{ generating ? 'AI 正在分析学情并生成练习集...' : 'AI 智能生成自适应练习集' }}</span>
            </button>
          </div>
        </div>

        <!-- 右侧学情透视与自适应看板 -->
        <aside class="snapshot-aside-panel">
          <div class="aside-title-row">
            <div class="aside-title-badge">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="aside-title-text">
              <h4 class="aside-h4">学情透视看板</h4>
              <span class="aside-sub">实时同步课程学习数据</span>
            </div>
          </div>

          <!-- 3 组数据微卡 -->
          <div class="snapshot-stats-cards">
            <div class="stat-cube stat-cube--weak">
              <span class="stat-number">{{ sessionMeta.weakKnowledgePointCount }}</span>
              <span class="stat-desc">薄弱考点</span>
            </div>
            <div class="stat-cube stat-cube--wrong">
              <span class="stat-number">{{ sessionMeta.pendingWrongQuestionCount }}</span>
              <span class="stat-desc">待练错题</span>
            </div>
            <div class="stat-cube stat-cube--time">
              <span class="stat-number">{{ sessionMeta.estimatedMinutes }}</span>
              <span class="stat-desc">预计分钟</span>
            </div>
          </div>

          <!-- 学情提示或正向激励 -->
          <div v-if="sessionMeta.weakPointHint" class="aside-hint-box">
            <el-icon class="hint-icon"><WarningFilled /></el-icon>
            <p class="hint-text">{{ sessionMeta.weakPointHint }}</p>
          </div>
          <div v-else class="aside-positive-box">
            <el-icon class="positive-icon"><Check /></el-icon>
            <p class="positive-text">
              当前课程掌握状况优良，AI 将结合全局综合考点为您生成进阶拓宽练习。
            </p>
          </div>

          <!-- 薄弱考点榜单（若有） -->
          <div v-if="weakKpPreview.length" class="weak-kps-section">
            <span class="kps-section-title">高频优先强化考点</span>
            <div class="kps-progress-list">
              <div v-for="kp in weakKpPreview" :key="kp.id" class="kp-progress-item">
                <div class="kp-label-row">
                  <span class="kp-name" :title="kp.name">{{ kp.name }}</span>
                  <span class="kp-rate">{{ kp.mastery ?? 0 }}%</span>
                </div>
                <el-progress
                  :percentage="kp.mastery ?? 0"
                  :stroke-width="5"
                  :show-text="false"
                  :color="(kp.mastery ?? 0) < 50 ? '#ef4444' : '#f59e0b'"
                />
              </div>
            </div>
          </div>

          <!-- 底部 AI 引擎特性说明 -->
          <div class="engine-features-card">
            <div class="engine-header">
              <el-icon><Lightning /></el-icon>
              <span>AI 自适应出题特性</span>
            </div>
            <ul class="engine-points">
              <li>考点图谱前驱弱项关联溯源</li>
              <li>动态变式干扰项参数自适应重构</li>
              <li>实时作答表现即时反哺画像模型</li>
            </ul>
          </div>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import {
  Operation,
  Compass,
  WarningFilled,
  Reading,
  CollectionTag,
  Aim,
  TrendCharts,
  Lightning,
  Check,
  Opportunity
} from '@element-plus/icons-vue';

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
  { value: 'ALL', label: '均衡演练', desc: '全认知维度兼顾' },
  { value: 'UNDERSTAND', label: '识记理解', desc: '概念与基准定理' },
  { value: 'APPLY', label: '综合应用', desc: '典型题型推演' },
  { value: 'EVALUATE', label: '高阶探究', desc: '压轴与综合贯通' }
];

const questionCountOptions = [
  { value: 3, label: '快速微测', time: '约 5 分钟', recommended: false },
  { value: 5, label: '标准专项', time: '约 12 分钟', recommended: true },
  { value: 10, label: '深度攻坚', time: '约 25 分钟', recommended: false }
];

const weakKpPreview = computed(() =>
  props.kpOptions
    .filter((k) => k.id > 0 && k.mastery != null && k.mastery < 70)
    .slice(0, 3)
);
</script>

<style scoped lang="scss">
.setup-layout {
  width: 100%;
}

.setup-card {
  background: #ffffff;
  border-radius: 20px;
  border: 1px solid #ebf1f7;
  box-shadow: 0 6px 24px rgba(30, 80, 150, 0.05);
  padding: 24px 28px;
  transition: all 0.25s ease;
}

// 1. 顶部 Header
.setup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  padding-bottom: 20px;
  margin-bottom: 24px;
  border-bottom: 1px solid #f1f5f9;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    .header-avatar-badge {
      width: 46px;
      height: 46px;
      border-radius: 14px;
      background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
      color: #2563eb;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 22px;
      border: 1px solid rgba(37, 99, 235, 0.15);
      box-shadow: 0 2px 8px rgba(37, 99, 235, 0.08);
      flex-shrink: 0;
    }

    .header-text-group {
      .header-title-row {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;
        margin-bottom: 4px;

        .setup-title {
          margin: 0;
          font-size: 18px;
          font-weight: 700;
          color: #0f172a;
          letter-spacing: -0.2px;
        }

        .mode-pill-badge {
          display: inline-flex;
          align-items: center;
          padding: 3px 12px;
          border-radius: 9999px;
          font-size: 12px;
          font-weight: 600;
          background: #f0fdf4;
          color: #16a34a;
          border: 1px solid rgba(22, 163, 74, 0.18);
        }
      }

      .setup-desc {
        margin: 0;
        font-size: 13px;
        color: #64748b;
        line-height: 1.4;
      }
    }
  }
}

// 2. 主体双栏网格
.setup-grid {
  display: grid;
  grid-template-columns: 1fr 310px;
  gap: 28px;
  align-items: start;

  @media (max-width: 1024px) {
    grid-template-columns: 1fr;
  }
}

// 左侧表单主体
.setup-form-panel {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.config-section {
  background: #f8fafc;
  border-radius: 16px;
  padding: 18px 20px;
  border: 1px solid #f1f5f9;

  .section-label {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;
    flex-wrap: wrap;

    .section-badge {
      width: 22px;
      height: 22px;
      border-radius: 50%;
      background: #2563eb;
      color: #ffffff;
      font-size: 11.5px;
      font-weight: 700;
      display: inline-flex;
      align-items: center;
      justify-content: center;
    }

    .section-title {
      font-size: 14.5px;
      font-weight: 700;
      color: #1e293b;
    }

    .section-subtitle {
      font-size: 12px;
      color: #94a3b8;
    }
  }
}

// 作用域下拉网格
.scope-selectors-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }

  .selector-item {
    display: flex;
    flex-direction: column;
    gap: 7px;

    .field-title {
      font-size: 13px;
      font-weight: 600;
      color: #475569;
      display: inline-flex;
      align-items: center;
      gap: 5px;

      .field-icon {
        color: #2563eb;
      }
    }
  }
}

// 布鲁姆认知偏好卡片
.cognitive-cards-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;

  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }

  .cognitive-option-card {
    position: relative;
    background: #ffffff;
    border: 1.5px solid #e2e8f0;
    border-radius: 12px;
    padding: 12px 14px;
    cursor: pointer;
    transition: all 0.2s ease;
    display: flex;
    align-items: flex-start;
    gap: 10px;

    .card-radio-indicator {
      width: 16px;
      height: 16px;
      border-radius: 50%;
      border: 1.5px solid #cbd5e1;
      margin-top: 2px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      transition: all 0.2s;

      .dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: transparent;
        transition: all 0.2s;
      }
    }

    .card-content {
      display: flex;
      flex-direction: column;
      gap: 3px;

      .card-title {
        font-size: 13.5px;
        font-weight: 700;
        color: #1e293b;
      }

      .card-desc {
        font-size: 11.5px;
        color: #64748b;
      }
    }

    &:hover {
      border-color: #93c5fd;
      background: #fcfdff;
      transform: translateY(-1px);
    }

    &.active {
      border-color: #2563eb;
      background: #eff6ff;
      box-shadow: 0 2px 8px rgba(37, 99, 235, 0.12);

      .card-radio-indicator {
        border-color: #2563eb;
        .dot {
          background: #2563eb;
        }
      }

      .card-title {
        color: #1d4ed8;
      }
    }
  }
}

// 题量与交付节奏
.pacing-grid {
  display: grid;
  grid-template-columns: 1fr 1.15fr;
  gap: 18px;

  @media (max-width: 860px) {
    grid-template-columns: 1fr;
  }

  .pacing-col {
    display: flex;
    flex-direction: column;
    gap: 8px;

    .field-title {
      font-size: 13px;
      font-weight: 600;
      color: #475569;
      display: inline-flex;
      align-items: center;
      gap: 5px;

      .field-icon {
        color: #2563eb;
      }
    }
  }
}

.count-cards-group {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;

  .count-card-item {
    position: relative;
    background: #ffffff;
    border: 1.5px solid #e2e8f0;
    border-radius: 12px;
    padding: 12px 10px;
    text-align: center;
    cursor: pointer;
    transition: all 0.2s ease;

    .count-num {
      font-size: 16px;
      font-weight: 800;
      color: #0f172a;
      margin-bottom: 2px;
    }

    .count-meta {
      display: flex;
      flex-direction: column;
      gap: 1px;

      .count-type {
        font-size: 11.5px;
        font-weight: 600;
        color: #475569;
      }

      .count-time {
        font-size: 10.5px;
        color: #94a3b8;
      }
    }

    .recommend-tag {
      position: absolute;
      top: -8px;
      right: 6px;
      background: linear-gradient(135deg, #ea580c, #f97316);
      color: #ffffff;
      font-size: 9.5px;
      font-weight: 700;
      padding: 1px 6px;
      border-radius: 9999px;
      box-shadow: 0 2px 4px rgba(234, 88, 12, 0.25);
    }

    &:hover {
      border-color: #93c5fd;
      transform: translateY(-1px);
    }

    &.active {
      border-color: #2563eb;
      background: #eff6ff;
      box-shadow: 0 2px 8px rgba(37, 99, 235, 0.12);

      .count-num {
        color: #1d4ed8;
      }
    }
  }
}

.feedback-mode-card {
  background: #ffffff;
  border: 1.5px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  min-height: 80px;

  .feedback-card-left {
    display: flex;
    align-items: center;
    gap: 12px;

    .feedback-icon-box {
      width: 38px;
      height: 38px;
      border-radius: 10px;
      background: #f1f5f9;
      color: #64748b;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
      flex-shrink: 0;
      transition: all 0.2s;
    }

    .feedback-info {
      display: flex;
      flex-direction: column;
      gap: 3px;

      .feedback-main-title {
        font-size: 13.5px;
        font-weight: 700;
        color: #1e293b;
      }

      .feedback-sub-text {
        font-size: 11.5px;
        color: #64748b;
        line-height: 1.35;
      }
    }
  }

  &:hover {
    border-color: #93c5fd;
    transform: translateY(-1px);
  }

  &.is-instant {
    border-color: #3b82f6;
    background: #fbfdff;

    .feedback-icon-box {
      background: #eff6ff;
      color: #2563eb;
    }

    .feedback-main-title {
      color: #1d4ed8;
    }
  }
}

// 错误横幅
.load-error-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  color: #c2410c;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 13px;

  .error-icon {
    font-size: 16px;
  }
}

// 底部生成按钮
.submit-action-box {
  display: flex;
  justify-content: center;
  padding-top: 6px;

  .generate-action-btn {
    height: 48px;
    padding: 0 42px;
    border-radius: 9999px;
    background: linear-gradient(135deg, #1d4ed8 0%, #2563eb 50%, #7c3aed 100%);
    color: #ffffff;
    font-size: 15px;
    font-weight: 700;
    border: none;
    cursor: pointer;
    display: inline-flex;
    align-items: center;
    gap: 9px;
    box-shadow: 0 4px 16px rgba(37, 99, 235, 0.3);
    transition: all 0.25s ease;

    &:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(37, 99, 235, 0.4);
    }

    &:active:not(:disabled) {
      transform: translateY(0);
    }

    &:disabled {
      opacity: 0.7;
      cursor: not-allowed;
    }
  }
}

// 右侧学情透视面板
.snapshot-aside-panel {
  background: #f8fafc;
  border: 1px solid #ebf1f7;
  border-radius: 16px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;

  .aside-title-row {
    display: flex;
    align-items: center;
    gap: 10px;

    .aside-title-badge {
      width: 32px;
      height: 32px;
      border-radius: 9px;
      background: #eff6ff;
      color: #2563eb;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 16px;
    }

    .aside-title-text {
      .aside-h4 {
        margin: 0;
        font-size: 14px;
        font-weight: 700;
        color: #0f172a;
      }
      .aside-sub {
        font-size: 11px;
        color: #94a3b8;
      }
    }
  }
}

.snapshot-stats-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;

  .stat-cube {
    background: #ffffff;
    border-radius: 12px;
    padding: 12px 6px;
    text-align: center;
    border: 1px solid #f1f5f9;
    box-shadow: 0 2px 6px rgba(15, 23, 42, 0.02);

    .stat-number {
      display: block;
      font-size: 20px;
      font-weight: 800;
      line-height: 1.1;
      margin-bottom: 3px;
    }

    .stat-desc {
      font-size: 11px;
      color: #64748b;
      font-weight: 600;
    }

    &--weak .stat-number {
      color: #ea580c;
    }

    &--wrong .stat-number {
      color: #dc2626;
    }

    &--time .stat-number {
      color: #2563eb;
    }
  }
}

.aside-hint-box {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  background: #fff7ed;
  border: 1px solid #ffedd5;
  border-radius: 10px;
  padding: 10px 12px;

  .hint-icon {
    color: #f97316;
    margin-top: 2px;
    flex-shrink: 0;
  }

  .hint-text {
    margin: 0;
    font-size: 12px;
    color: #9a3412;
    line-height: 1.45;
  }
}

.aside-positive-box {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  background: #f0fdf4;
  border: 1px solid #dcfce7;
  border-radius: 10px;
  padding: 10px 12px;

  .positive-icon {
    color: #16a34a;
    margin-top: 2px;
    flex-shrink: 0;
  }

  .positive-text {
    margin: 0;
    font-size: 12px;
    color: #15803d;
    line-height: 1.45;
  }
}

.weak-kps-section {
  display: flex;
  flex-direction: column;
  gap: 10px;

  .kps-section-title {
    font-size: 12.5px;
    font-weight: 700;
    color: #334155;
  }

  .kps-progress-list {
    display: flex;
    flex-direction: column;
    gap: 8px;

    .kp-progress-item {
      display: flex;
      flex-direction: column;
      gap: 3px;

      .kp-label-row {
        display: flex;
        justify-content: space-between;
        font-size: 11.5px;

        .kp-name {
          color: #475569;
          max-width: 180px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .kp-rate {
          color: #64748b;
          font-weight: 600;
        }
      }
    }
  }
}

.engine-features-card {
  background: #ffffff;
  border: 1px dashed #cbd5e1;
  border-radius: 12px;
  padding: 12px 14px;

  .engine-header {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    font-weight: 700;
    color: #475569;
    margin-bottom: 6px;

    .el-icon {
      color: #7c3aed;
    }
  }

  .engine-points {
    margin: 0;
    padding-left: 16px;
    font-size: 11px;
    color: #64748b;
    line-height: 1.6;
  }
}
</style>
