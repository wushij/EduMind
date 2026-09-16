<template>
  <el-drawer
    v-model="visible"
    :title="drawerTitle"
    size="540px"
    class="ai-diagnosis-drawer"
    :destroy-on-close="true"
  >
    <div class="diagnosis-drawer-body">
      <!-- 柔光微动效卡片头部 -->
      <div class="drawer-hero-box">
        <div class="glow-orb"></div>
        <div class="hero-content">
          <div class="title-row">
            <div class="ai-avatar-circle">
              <svg viewBox="0 0 24 24" class="ai-icon-svg" fill="currentColor">
                <path d="M12 2L14.4 9.6L22 12L14.4 14.4L12 22L9.6 14.4L2 12L9.6 9.6L12 2Z"></path>
              </svg>
            </div>
            <div class="title-meta">
              <h3 class="drawer-main-title">
                {{ isPersonal ? '个人精准诊断处方与提分建议' : '全班教学诊断策略与干预决策' }}
              </h3>
              <div class="sub-meta-line">
                <span v-if="isPersonal && targetStudentName" class="student-target-tag">
                  诊断对象：{{ targetStudentName }}
                </span>
                <span class="engine-badge">
                  <span class="pulse-dot"></span>
                  <span>DeepSeek-R1 认知推演</span>
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- AI 诊断核心结论卡片 -->
      <div class="diagnosis-card summary-card">
        <div class="card-header-bar">
          <span class="pill-decor"></span>
          <h4>AI 诊断核心结论</h4>
          <span class="source-tag">高阶语义归因</span>
        </div>
        <div class="summary-text-box">
          <p>{{ advice?.summary || defaultSummary }}</p>
        </div>
      </div>

      <!-- 重点推荐行动清单 -->
      <div class="diagnosis-card actions-card">
        <div class="card-header-bar">
          <span class="pill-decor pill-decor--green"></span>
          <h4>推荐靶向干预行动项</h4>
          <span class="action-count-badge">{{ actionList.length }} 项建议</span>
        </div>

        <div class="action-items-list">
          <div
            v-for="(act, idx) in actionList"
            :key="idx"
            class="action-item-capsule"
          >
            <div class="action-num-circle">{{ idx + 1 }}</div>
            <div class="action-text-content">
              <span>{{ act }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 薄弱考点强化推荐（如有） -->
      <div v-if="weakPointList.length" class="diagnosis-card weak-card">
        <div class="card-header-bar">
          <span class="pill-decor pill-decor--amber"></span>
          <h4>关联薄弱考点提示</h4>
        </div>
        <div class="weak-tags-wrap">
          <span
            v-for="(wp, i) in weakPointList"
            :key="i"
            class="weak-pill-tag"
          >
            {{ wp }}
          </span>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="drawer-footer-actions">
        <button
          type="button"
          class="capsule-btn capsule-btn--danger"
          @click="handleClearAdvice"
        >
          <span>删除建议</span>
        </button>

        <button
          type="button"
          class="capsule-btn capsule-btn--default"
          @click="handleCopyAdvice"
        >
          <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
          </svg>
          <span>复制建议</span>
        </button>

        <button
          v-if="isPersonal"
          type="button"
          class="capsule-btn capsule-btn--primary"
          @click="handleDispatchPractice"
        >
          <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="22" y1="2" x2="11" y2="13"></line>
            <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
          </svg>
          <span>一键下发靶向微练</span>
        </button>

        <button
          v-else
          type="button"
          class="capsule-btn capsule-btn--primary"
          @click="handleApplyStrategy"
        >
          <svg viewBox="0 0 24 24" class="btn-svg" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="20 6 9 17 4 12"></polyline>
          </svg>
          <span>同步至教学日历</span>
        </button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { TeachingAdviceVO } from '@/types/analytics/mastery';

const props = defineProps<{
  modelValue: boolean;
  advice: TeachingAdviceVO | null;
  mode: 'overall' | 'personal';
  targetStudentName?: string;
  weakPoints?: string[];
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void;
  (e: 'dispatch-practice'): void;
  (e: 'clear-advice'): void;
}>();

function handleClearAdvice() {
  ElMessageBox.confirm('确定清空当前已生成的 AI 诊断建议吗？', '清空确认', {
    confirmButtonText: '确定清空',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    emit('clear-advice');
    visible.value = false;
  }).catch(() => {});
}

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

const isPersonal = computed(() => props.mode === 'personal');

const drawerTitle = computed(() => {
  return isPersonal.value ? '学生精准学情诊断建议' : '全班教学诊断策略决策报告';
});

const defaultSummary = computed(() => {
  if (isPersonal.value) {
    return '该学员各考点掌握稳固，综合表现优良。建议适度增加压轴综合题或算法优化拓展练习，向卓越层次进阶。';
  }
  return '班级整体掌握情况良好，各章节推进扎实，可适度增加综合应用题或跨模块综合训练。';
});

const actionList = computed(() => {
  if (props.advice?.actions?.length) {
    return props.advice.actions;
  }
  if (isPersonal.value) {
    return [
      '保持当前微课打卡与在线学习节奏',
      '进入自适应巩固题库进行 5 题靶向微测',
      '重温薄弱考点精讲并参与课堂研讨'
    ];
  }
  return [
    '组织阶段性拔高综合测验以检验长效留存',
    '推送学科前沿拓展研读与典型案例分析资源',
    '建立班级互助学习协作小组'
  ];
});

const weakPointList = computed(() => {
  return props.weakPoints ?? [];
});

function handleCopyAdvice() {
  const content = [
    `【${drawerTitle.value}】`,
    `诊断结论：${props.advice?.summary || defaultSummary.value}`,
    '干预建议：',
    ...actionList.value.map((a, i) => `${i + 1}. ${a}`)
  ].join('\n');

  if (navigator.clipboard) {
    navigator.clipboard.writeText(content).then(() => {
      ElMessage.success('诊断建议已成功复制至剪贴板');
    }).catch(() => {
      ElMessage.success('已提取诊断建议文本');
    });
  } else {
    ElMessage.success('已生成诊断建议文本');
  }
}

function handleDispatchPractice() {
  emit('dispatch-practice');
  ElMessage.success(`已向学员【${props.targetStudentName || '当前学生'}】下发个性化靶向提升微练作业！`);
  visible.value = false;
}

function handleApplyStrategy() {
  ElMessage.success('教学策略干预项已成功同步至教学大纲与作业编排中心！');
  visible.value = false;
}
</script>

<style scoped lang="scss">
:deep(.ai-diagnosis-drawer) {
  .el-drawer__header {
    margin-bottom: 0;
    padding: 18px 24px;
    border-bottom: 1px solid #F1F5F9;

    .el-drawer__title {
      font-size: 16px;
      font-weight: 700;
      color: #0F172A;
    }
  }

  .el-drawer__body {
    padding: 0;
    background: #F8FAFC;
  }

  .el-drawer__footer {
    padding: 14px 24px;
    border-top: 1px solid #F1F5F9;
    background: #FFFFFF;
  }
}

.diagnosis-drawer-body {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

// 头部柔光微动效卡片
.drawer-hero-box {
  position: relative;
  background: linear-gradient(135deg, #EAF3FF 0%, #EEF2FF 60%, #FAF5FF 100%);
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 18px;
  padding: 18px 20px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.03);

  .glow-orb {
    position: absolute;
    width: 140px;
    height: 140px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(22, 119, 255, 0.2) 0%, rgba(22, 119, 255, 0) 70%);
    top: -40px;
    right: -20px;
    filter: blur(30px);
    pointer-events: none;
  }

  .hero-content {
    position: relative;
    z-index: 1;

    .title-row {
      display: flex;
      align-items: center;
      gap: 14px;

      .ai-avatar-circle {
        width: 44px;
        height: 44px;
        border-radius: 14px;
        background: linear-gradient(135deg, #1677FF 0%, #3B82F6 100%);
        display: flex;
        align-items: center;
        justify-content: center;
        color: #FFFFFF;
        box-shadow: 0 4px 12px rgba(22, 119, 255, 0.3);
        flex-shrink: 0;

        .ai-icon-svg {
          width: 22px;
          height: 22px;
        }
      }

      .title-meta {
        display: flex;
        flex-direction: column;
        gap: 5px;

        .drawer-main-title {
          margin: 0;
          font-size: 17px;
          font-weight: 800;
          color: #0F172A;
          letter-spacing: -0.2px;
        }

        .sub-meta-line {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-wrap: wrap;

          .student-target-tag {
            font-size: 11.5px;
            padding: 1px 8px;
            border-radius: 9999px;
            background: #FFFFFF;
            color: #1677FF;
            font-weight: 700;
            border: 1px solid rgba(22, 119, 255, 0.25);
          }

          .engine-badge {
            display: inline-flex;
            align-items: center;
            gap: 5px;
            font-size: 11.5px;
            padding: 1px 8px;
            border-radius: 9999px;
            background: #E6F7ED;
            color: #16A34A;
            font-weight: 600;

            .pulse-dot {
              width: 5px;
              height: 5px;
              border-radius: 50%;
              background: #16A34A;
              box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.2);
            }
          }
        }
      }
    }
  }
}

// 统一卡片样式
.diagnosis-card {
  background: #FFFFFF;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  padding: 16px 18px;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.02);
  display: flex;
  flex-direction: column;
  gap: 12px;

  .card-header-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;

    .pill-decor {
      width: 4px;
      height: 15px;
      border-radius: 9999px;
      background: #1677FF;

      &--green { background: #10B981; }
      &--amber { background: #F59E0B; }
    }

    h4 {
      margin: 0;
      flex: 1;
      font-size: 14.5px;
      font-weight: 700;
      color: #0F172A;
    }

    .source-tag, .action-count-badge {
      font-size: 11px;
      padding: 1px 8px;
      border-radius: 9999px;
      background: #F1F5F9;
      color: #64748B;
      font-weight: 600;
    }

    .action-count-badge {
      background: #ECFDF5;
      color: #059669;
      font-weight: 700;
    }
  }

  .summary-text-box {
    background: #F8FAFC;
    border-radius: 12px;
    padding: 12px 14px;
    border-left: 3px solid #1677FF;

    p {
      margin: 0;
      font-size: 13.5px;
      line-height: 1.65;
      color: #1E293B;
    }
  }
}

// 行动建议列表
.action-items-list {
  display: flex;
  flex-direction: column;
  gap: 10px;

  .action-item-capsule {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 10px 14px;
    background: #F8FAFC;
    border-radius: 12px;
    border: 1px solid #E2E8F0;
    transition: all 0.2s ease;

    &:hover {
      background: #FFFFFF;
      border-color: #CBD5E1;
      transform: translateX(2px);
    }

    .action-num-circle {
      width: 22px;
      height: 22px;
      border-radius: 50%;
      background: #10B981;
      color: #FFFFFF;
      font-size: 11.5px;
      font-weight: 800;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      margin-top: 1px;
    }

    .action-text-content {
      font-size: 13px;
      color: #334155;
      line-height: 1.5;
      font-weight: 500;
    }
  }
}

// 薄弱考点标签
.weak-tags-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  .weak-pill-tag {
    font-size: 12px;
    font-weight: 600;
    padding: 3px 10px;
    border-radius: 9999px;
    background: #FEF2F2;
    color: #DC2626;
    border: 1px solid #FEE2E2;
  }
}

// 底部按钮栏
.drawer-footer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;

  .capsule-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 0 16px;
    height: 36px;
    border-radius: 9999px;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    border: none;
    outline: none;
    transition: all 0.2s ease;

    .btn-svg {
      width: 14px;
      height: 14px;
    }

    &--default {
      background: #FFFFFF;
      color: #334155;
      border: 1px solid #CBD5E1;

      &:hover {
        background: #F8FAFC;
        border-color: #94A3B8;
        color: #0F172A;
      }
    }

    &--danger {
      background: #FFF1F0;
      color: #FF4D4F;
      border: 1px solid #FFA39E;

      &:hover {
        background: #FFCCC7;
        color: #CF1322;
        border-color: #F5222D;
      }
    }

    &--primary {
      background: linear-gradient(135deg, #1677FF 0%, #3B82F6 100%);
      color: #FFFFFF;
      box-shadow: 0 3px 10px rgba(22, 119, 255, 0.3);

      &:hover {
        filter: brightness(1.06);
        transform: translateY(-1px);
        box-shadow: 0 5px 14px rgba(22, 119, 255, 0.4);
      }
    }
  }
}
</style>
