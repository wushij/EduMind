<template>
  <el-drawer
    v-model="visible"
    :title="`试题 ${item?.questionId ?? ''} 错题穿透与学情诊断详情`"
    size="680px"
    class="wrong-detail-drawer"
    append-to-body
    destroy-on-close
  >
    <div v-if="item" class="drawer-detail-body">
      <!-- 1. 顶部题目核心标签栏 -->
      <div class="meta-tag-bar">
        <span class="type-badge">{{ item.typeName || '选择题' }}</span>
        <span class="difficulty-badge">难度 {{ '★'.repeat(Math.min(5, item.difficulty || 2)) }}</span>
        <span v-if="item.knowledgePointName" class="kp-badge">{{ item.knowledgePointName }}</span>
        <span class="fail-rate-badge" :class="errorRateClass">
          班级错误率 {{ item.errorRate ?? 0 }}% ({{ item.wrongStudentCount ?? 0 }}/{{ item.classStudentCount ?? 0 }}人)
        </span>
      </div>

      <!-- 2. 题干卡片 (支持 KaTeX 与 Markdown 公式) -->
      <div class="detail-section">
        <h4 class="section-title">
          <svg viewBox="0 0 24 24" class="sec-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            <polyline points="14 2 14 8 20 8" />
            <line x1="16" y1="13" x2="8" y2="13" />
            <line x1="16" y1="17" x2="8" y2="17" />
          </svg>
          <span>完整试题题干</span>
        </h4>
        <div class="stem-box">
          <MathText :text="item.stem || '暂无题目内容'" />
        </div>
      </div>

      <!-- 3. 选项列表 (如果包含选项) -->
      <div v-if="parsedOptions.length > 0" class="detail-section">
        <h4 class="section-title">
          <svg viewBox="0 0 24 24" class="sec-svg" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 11 12 14 22 4" />
            <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
          </svg>
          <span>选项与标准对照</span>
        </h4>
        <div class="options-list">
          <div
            v-for="opt in parsedOptions"
            :key="opt.key"
            class="option-item"
            :class="{ 'is-correct': opt.key === item.answer }"
          >
            <span class="opt-key">{{ opt.key }}</span>
            <div class="opt-content">
              <MathText :text="opt.content" />
            </div>
            <span v-if="opt.key === item.answer" class="opt-correct-tag">标准答案</span>
          </div>
        </div>
      </div>

      <!-- 4. 参考解析卡片 -->
      <div v-if="item.analysis" class="detail-section">
        <h4 class="section-title">
          <svg viewBox="0 0 24 24" class="sec-svg" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="16" x2="12" y2="12" />
            <line x1="12" y1="8" x2="12.01" y2="8" />
          </svg>
          <span>官方标准解析</span>
        </h4>
        <div class="analysis-box">
          <MathText :text="item.analysis" />
        </div>
      </div>

      <!-- 5. AI 智能归因结论卡片 -->
      <div class="detail-section">
        <h4 class="section-title">
          <svg viewBox="0 0 24 24" class="sec-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2L14.4 9.6L22 12L14.4 14.4L12 22L9.6 14.4L2 12L9.6 9.6L12 2Z" />
          </svg>
          <span>AI 错因深度诊断与建议</span>
          <span class="source-tag" :class="item.diagnosisSource === 'AI' ? 'source-tag--ai' : 'source-tag--legacy'">
            {{ item.diagnosisSource === 'AI' ? '大模型实时推演' : '循证教学预置' }}
          </span>
        </h4>
        <div class="diagnosis-callout">
          <div
            class="diagnosis-text structured-proposal-container"
            v-html="formatStructuredProposal(item.diagnosis || '暂无深度归因建议')"
          ></div>
          <div class="error-types-line">
            <span class="error-types-label">失分诱因：</span>
            <span
              v-for="(label, idx) in item.errorTypeLabels"
              :key="idx"
              class="error-type-tag"
            >
              {{ label }}
            </span>
            <!-- 无归因结论时不再兜底显示「概念偏差」，如实标注待归因 -->
            <span v-if="!item.errorTypeLabels?.length" class="error-type-tag error-type-tag--pending">
              待归因
            </span>
          </div>
        </div>
      </div>

      <!-- 6. 做错学生作答明细与学情穿透表格 -->
      <div class="detail-section">
        <div class="section-title-between">
          <h4 class="section-title">
            <svg viewBox="0 0 24 24" class="sec-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
              <path d="M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
            <span>做错学生学情穿透 ({{ item.studentWrongList?.length ?? 0 }} 人)</span>
          </h4>
        </div>

        <div v-if="item.studentWrongList && item.studentWrongList.length > 0" class="student-table-wrap">
          <el-table :data="item.studentWrongList" size="small" stripe style="width: 100%">
            <el-table-column prop="studentName" label="学员姓名" width="120">
              <template #default="{ row }">
                <span class="student-name-cell">{{ row.studentName }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="studentNo" label="学号/账号" width="130" />
            <el-table-column label="错误提交作答" min-width="150">
              <template #default="{ row }">
                <span v-if="row.lastStudentAnswer" class="wrong-ans-badge">
                  {{ row.lastStudentAnswer }}
                </span>
                <span v-else class="text-muted">（未记录作答）</span>
              </template>
            </el-table-column>
            <el-table-column prop="wrongCount" label="失分次数" width="90" align="center">
              <template #default="{ row }">
                <span class="count-tag">{{ row.wrongCount }} 次</span>
              </template>
            </el-table-column>
            <el-table-column prop="updateTime" label="最近作答时间" width="150">
              <template #default="{ row }">
                <span class="time-cell">{{ formatTime(row.updateTime) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div v-else class="empty-student-notice">
          <span>暂无该题目的学生作答记录</span>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import MathText from '@/components/common/MathText.vue';
import { formatStructuredProposal } from '@/utils/format/structured-text';
import type { WrongQuestionItemVO } from '@/types/analytics/mastery';

const props = defineProps<{
  modelValue: boolean;
  item: WrongQuestionItemVO | null;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void;
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

const errorRateClass = computed(() => {
  const rate = props.item?.errorRate ?? 0;
  if (rate >= 50) return 'fail-rate--high';
  if (rate >= 25) return 'fail-rate--medium';
  return 'fail-rate--low';
});

const parsedOptions = computed(() => {
  if (!props.item?.options) return [];
  try {
    const raw = props.item.options.trim();
    if (raw.startsWith('[') || raw.startsWith('{')) {
      const data = JSON.parse(raw);
      if (Array.isArray(data)) {
        return data.map((d: any) => ({
          key: d.key || d.optionKey || '',
          content: d.content || d.text || ''
        }));
      }
    }
  } catch {
    // 降级文本解析
  }
  return [];
});

function formatTime(t?: string) {
  if (!t) return '—';
  return t.replace('T', ' ').substring(0, 16);
}
</script>

<style scoped lang="scss">
.wrong-detail-drawer {
  :deep(.el-drawer__header) {
    margin-bottom: 14px;
    font-weight: 700;
    font-size: 17px;
    color: #0f172a;
    border-bottom: 1px solid #f1f5f9;
    padding-bottom: 14px;
  }
}

.drawer-detail-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-bottom: 30px;

  .meta-tag-bar {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;

    .type-badge {
      background: #eff6ff;
      color: #2563eb;
      font-size: 12px;
      font-weight: 600;
      padding: 3px 10px;
      border-radius: 8px;
    }

    .difficulty-badge {
      background: #fffbeb;
      color: #d97706;
      font-size: 12px;
      font-weight: 600;
      padding: 3px 10px;
      border-radius: 8px;
    }

    .kp-badge {
      background: #f1f5f9;
      color: #475569;
      font-size: 12px;
      font-weight: 600;
      padding: 3px 10px;
      border-radius: 8px;
    }

    .fail-rate-badge {
      font-size: 12px;
      font-weight: 700;
      padding: 3px 10px;
      border-radius: 8px;

      &.fail-rate--high {
        background: #fef2f2;
        color: #ef4444;
        border: 1px solid #fee2e2;
      }
      &.fail-rate--medium {
        background: #fffbeb;
        color: #f59e0b;
        border: 1px solid #fef3c7;
      }
      &.fail-rate--low {
        background: #f0fdf4;
        color: #10b981;
        border: 1px solid #dcfce7;
      }
    }
  }

  .detail-section {
    display: flex;
    flex-direction: column;
    gap: 10px;

    .section-title {
      margin: 0;
      display: flex;
      align-items: center;
      gap: 7px;
      font-size: 14px;
      font-weight: 700;
      color: #1e293b;

      .sec-svg {
        width: 16px;
        height: 16px;
        color: #3b82f6;
      }

      .source-tag {
        font-size: 11px;
        padding: 2px 7px;
        border-radius: 6px;
        font-weight: 500;
        margin-left: 6px;

        &--ai {
          background: #eff6ff;
          color: #2563eb;
        }

        &--legacy {
          background: #f1f5f9;
          color: #64748b;
        }
      }
    }

    .stem-box {
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      padding: 16px;
      font-size: 14px;
      line-height: 1.7;
      color: #0f172a;
    }

    .options-list {
      display: flex;
      flex-direction: column;
      gap: 8px;

      .option-item {
        display: flex;
        align-items: center;
        gap: 12px;
        background: #ffffff;
        border: 1px solid #e2e8f0;
        border-radius: 10px;
        padding: 10px 14px;
        transition: all 0.2s;

        &.is-correct {
          background: #f0fdf4;
          border-color: #86efac;
        }

        .opt-key {
          width: 24px;
          height: 24px;
          border-radius: 50%;
          background: #e2e8f0;
          color: #334155;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 12px;
          font-weight: 700;
          flex-shrink: 0;
        }

        &.is-correct .opt-key {
          background: #22c55e;
          color: #ffffff;
        }

        .opt-content {
          flex: 1;
          font-size: 13px;
          color: #334155;
        }

        .opt-correct-tag {
          font-size: 11px;
          font-weight: 600;
          background: #22c55e;
          color: #ffffff;
          padding: 2px 8px;
          border-radius: 6px;
        }
      }
    }

    .analysis-box {
      background: #faf5ff;
      border: 1px solid #f3e8ff;
      border-radius: 12px;
      padding: 14px 16px;
      font-size: 13px;
      line-height: 1.6;
      color: #581c87;
    }

    .diagnosis-callout {
      background: linear-gradient(135deg, #f0fdf4 0%, #eff6ff 100%);
      border: 1px solid #bfdbfe;
      border-radius: 14px;
      padding: 16px 18px;
      display: flex;
      flex-direction: column;
      gap: 10px;

      .diagnosis-text {
        margin: 0;
        font-size: 13px;
        color: #1e3a8a;
        line-height: 1.6;

        :deep(.structured-point-card) {
          margin-top: 10px;
          padding: 12px 14px;
          background: rgba(255, 255, 255, 0.85);
          border: 1px solid #bfdbfe;
          border-left: 4px solid #2563eb;
          border-radius: 10px;

          &:first-child {
            margin-top: 0;
          }

          .point-badge-header {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 6px;

            .point-num-pill {
              display: inline-flex;
              align-items: center;
              justify-content: center;
              min-width: 22px;
              height: 22px;
              padding: 0 7px;
              font-size: 12px;
              font-weight: 700;
              color: #ffffff;
              background: #2563eb;
              border-radius: 11px;
            }

            .point-title-text {
              font-size: 13.5px;
              font-weight: 700;
              color: #0f172a;
            }
          }

          .point-body-text {
            font-size: 13px;
            line-height: 1.75;
            color: #334155;
            word-break: break-word;

            :deep(.katex) {
              font-size: 1.05em;
            }

            :deep(.katex-display) {
              margin: 8px 0;
              padding: 8px 12px;
              background: #f8fafc;
              border-radius: 8px;
              border: 1px solid #e2e8f0;
              overflow-x: auto;
            }
          }
        }

        :deep(.structured-intro-p) {
          font-size: 13px;
          line-height: 1.7;
          color: #1e3a8a;
          word-break: break-word;

          :deep(.katex) {
            font-size: 1.05em;
          }
        }
      }

      .error-types-line {
        display: flex;
        align-items: center;
        gap: 6px;
        flex-wrap: wrap;
        font-size: 12px;

        .error-types-label {
          color: #475569;
          font-weight: 600;
        }

        .error-type-tag {
          background: #ffffff;
          border: 1px solid #93c5fd;
          color: #1d4ed8;
          padding: 2px 8px;
          border-radius: 6px;
          font-weight: 600;
          font-size: 11px;
        }

        // 待归因：中性灰，避免与任何一种真实错因混淆
        .error-type-tag--pending {
          background: #f8fafc;
          border-color: #cbd5e1;
          color: #64748b;
          font-weight: 500;
        }
      }
    }

    .student-table-wrap {
      border: 1px solid #e2e8f0;
      border-radius: 12px;
      overflow: hidden;

      .student-name-cell {
        font-weight: 600;
        color: #0f172a;
      }

      .wrong-ans-badge {
        display: inline-block;
        background: #fee2e2;
        color: #b91c1c;
        padding: 2px 8px;
        border-radius: 6px;
        font-weight: 600;
        font-size: 12px;
      }

      .count-tag {
        font-weight: 600;
        color: #ea580c;
      }

      .time-cell {
        color: #64748b;
        font-size: 12px;
      }
    }

    .empty-student-notice {
      padding: 20px;
      text-align: center;
      color: #94a3b8;
      font-size: 13px;
      border: 1px dashed #e2e8f0;
      border-radius: 10px;
    }
  }
}
</style>
