<template>
  <div class="question-preview-page">
    <!-- 顶部状态与批处理动作栏 -->
    <div class="preview-top-bar">
      <div class="summary-col">
        <div class="title-row">
          <h1 class="page-title">AI 智能命题精修工作台</h1>
          <span class="pill-count-badge">已就绪 {{ generatedQuestions.length }} 道结构化试题</span>
          <span class="pill-score-badge">卷面参考总分：{{ totalScore }} 分</span>
          <span class="pill-course-badge">{{ selectedCourseName }}</span>
        </div>
        <p class="page-subtitle">
          题目已由大模型按照布鲁姆认知模型与大纲知识点结构化组织。支持单题原位微调、单题一键 AI 换一题与变式，满意后可直接批量入库或转入 AI 组卷。
        </p>
      </div>

      <div class="action-buttons-row">
        <button
          type="button"
          class="capsule-action-btn capsule-action-btn--default"
          @click="router.push('/ai/question/generate')"
        >
          <el-icon><ArrowLeft /></el-icon>
          <span>返回调整参数</span>
        </button>

        <button
          type="button"
          class="capsule-action-btn capsule-action-btn--default"
          :disabled="generatedQuestions.length === 0"
          @click="exportMarkdown"
        >
          <el-icon><Download /></el-icon>
          <span>导出 Markdown 试卷</span>
        </button>

        <button
          type="button"
          class="capsule-action-btn capsule-action-btn--secondary"
          :disabled="generatedQuestions.length === 0"
          @click="router.push('/ai/exam/generate')"
        >
          <el-icon><Tickets /></el-icon>
          <span>流转 AI 组卷</span>
        </button>

        <button
          type="button"
          class="capsule-action-btn capsule-action-btn--primary"
          :disabled="generatedQuestions.length === 0 || saving"
          @click="handleBatchSave"
        >
          <el-icon v-if="!saving"><Check /></el-icon>
          <span v-if="!saving">批量保存入库 ({{ generatedQuestions.length }} 题)</span>
          <span v-else>正在持久化入库...</span>
        </button>
      </div>
    </div>

    <!-- 题目卡片流式列表 -->
    <div v-if="generatedQuestions.length > 0" class="questions-stream-container">
      <QuestionCard
        v-for="(q, index) in generatedQuestions"
        :key="q.id || index"
        :question="q"
        :index="index"
        allow-regenerate
        :is-regenerating="regeneratingIndex === index"
        @delete="handleDeleteQuestion"
        @update="handleUpdateQuestion(index, $event)"
        @regenerate="handleRegenerateQuestion(index)"
      />
    </div>

    <!-- 若被全部剔除时的空状态 -->
    <div v-else class="empty-questions-box">
      <el-icon class="empty-icon"><EditPen /></el-icon>
      <h3>当前暂无可预览试题</h3>
      <p>所有题目已被剔除，可点击下方返回重新配置生成</p>
      <button
        type="button"
        class="capsule-action-btn capsule-action-btn--primary"
        @click="router.push('/ai/question/generate')"
      >
        返回重新配置出题
      </button>
    </div>

    <!-- 底部悬浮长圆快捷操作栏 -->
    <div v-if="generatedQuestions.length > 0" class="floating-dock-bar">
      <div class="dock-inner">
        <span class="dock-summary">
          当前共 <strong>{{ generatedQuestions.length }}</strong> 道高契合度试题，卷面总分 <strong>{{ totalScore }}</strong> 分
        </span>
        <div class="dock-btns">
          <button
            type="button"
            class="dock-secondary-btn"
            @click="exportMarkdown"
          >
            <el-icon><Download /></el-icon>
            <span>导出试卷</span>
          </button>
          <button
            type="button"
            class="dock-save-btn"
            :disabled="saving"
            @click="handleBatchSave"
          >
            <span>一键批量入库</span>
            <el-icon class="btn-check-icon"><Check /></el-icon>
          </button>
        </div>
      </div>
    </div>

    <!-- 单题 AI 换一题：与命题页相同的推演引擎弹窗（计时 + 中止） -->
    <QuestionGenerateEngineDialog
      :visible="regeneratingIndex !== null"
      @abort="abortGeneration"
    />

    <!-- 智能入库与归档引导弹窗 -->
    <el-dialog
      v-model="saveDialogVisible"
      title="试题批量入库与归档"
      width="580px"
      destroy-on-close
      class="custom-save-dialog"
    >
      <div class="save-dialog-hero-banner">
        <div class="banner-icon-box">
          <el-icon><FolderAdd /></el-icon>
        </div>
        <div class="banner-text-box">
          <h4 class="banner-title">已就绪 {{ generatedQuestions.length }} 道结构化试题（卷面总分：{{ totalScore }} 分）</h4>
          <p class="banner-desc">建议创建或关联专属题库，便于后续整套试卷导出、按章节抽调复习与一键快捷组卷。</p>
        </div>
      </div>

      <el-form label-position="top" class="save-form">
        <el-form-item label="归档目标与组织方式">
          <el-radio-group v-model="archiveMode" class="archive-mode-radios">
            <el-radio-button label="NEW_BANK">
              <el-icon class="mr-1"><FolderAdd /></el-icon>
              创建专属题库并归入 (推荐)
            </el-radio-button>
            <el-radio-button label="EXISTING_BANK">
              <el-icon class="mr-1"><FolderOpened /></el-icon>
              追加存入已有题库
            </el-radio-button>
            <el-radio-button label="RAW_POOL">
              <el-icon class="mr-1"><Reading /></el-icon>
              仅存入公共散题池
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <!-- 模式 1：新建题库选项 -->
        <template v-if="archiveMode === 'NEW_BANK'">
          <el-form-item label="题库名称" required>
            <el-input
              v-model="newBankName"
              placeholder="例如：Java学习 - AI智能命题专项集"
              maxlength="50"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="题库说明">
            <el-input
              v-model="newBankDescription"
              type="textarea"
              :rows="2"
              placeholder="说明本题库的考查范围与重点章节..."
              maxlength="150"
              show-word-limit
            />
          </el-form-item>
        </template>

        <!-- 模式 2：追加已有题库选项 -->
        <template v-else-if="archiveMode === 'EXISTING_BANK'">
          <el-form-item label="选择目标课程题库" required>
            <el-select
              v-model="selectedExistingBankId"
              placeholder="请选择要追加的课程题库"
              filterable
              class="w-full"
            >
              <el-option
                v-for="b in existingCourseBanks"
                :key="b.id"
                :label="`${b.name} (${b.questionCount || 0} 题)`"
                :value="b.id"
              />
            </el-select>
            <div v-if="existingCourseBanks.length === 0" class="no-banks-hint">
              该课程当前暂无已建题库，建议切换选择“创建专属题库并归入”。
            </div>
          </el-form-item>
        </template>

        <!-- 模式 3：散题池提示 -->
        <template v-else>
          <div class="raw-pool-hint">
            <el-icon class="mr-1"><InfoFilled /></el-icon>
            <span>试题将作为原子题目直接保存至题目大池（/question/list），不会归纳进独立题库。</span>
          </div>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="saveDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="saving"
          @click="confirmBatchSave"
        >
          确认保存入库并前往
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import {
  EditPen,
  Check,
  Download,
  ArrowLeft,
  Tickets,
  FolderAdd,
  FolderOpened,
  Reading,
  InfoFilled
} from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import QuestionCard from '@/components/question/QuestionCard.vue';
import QuestionGenerateEngineDialog from '@/components/ai/generation/QuestionGenerateEngineDialog.vue';
import { useQuestionGenerate } from '@/composables/ai/useQuestionGenerate';
import type { Question } from '@/types/question/question';

const router = useRouter();
const {
  courses,
  generatedQuestions,
  selectedCourseName,
  existingCourseBanks,
  regeneratingIndex,
  deleteQuestion,
  updateQuestion,
  regenerateSingleQuestion,
  abortGeneration,
  batchSave,
  exportMarkdown,
  loadCourseOptions,
  loadExistingBanksForCourse,
  formState
} = useQuestionGenerate();

onMounted(async () => {
  if (courses.value.length === 0) {
    await loadCourseOptions();
  }
});

const saving = ref(false);
const saveDialogVisible = ref(false);
const archiveMode = ref<'NEW_BANK' | 'EXISTING_BANK' | 'RAW_POOL'>('NEW_BANK');
const newBankName = ref('');
const newBankDescription = ref('');
const selectedExistingBankId = ref<number | string | undefined>(undefined);

const totalScore = computed(() => {
  return generatedQuestions.value.reduce((acc, cur) => acc + (cur.score || 0), 0);
});

function handleDeleteQuestion(id: number | string) {
  deleteQuestion(id);
}

function handleUpdateQuestion(index: number, updated: Question) {
  updateQuestion(index, updated);
}

function handleRegenerateQuestion(index: number) {
  regenerateSingleQuestion(index);
}

async function handleBatchSave() {
  if (generatedQuestions.value.length === 0) {
    ElMessage.warning('当前暂无可保存入库试题');
    return;
  }
  const dateStr = new Date().toLocaleDateString();
  newBankName.value = `${selectedCourseName.value} - AI命题专项集 (${dateStr})`;
  newBankDescription.value = `由 AI 智能命题推演生成，收录 ${generatedQuestions.value.length} 道高契合度精选题。`;

  await loadExistingBanksForCourse();

  if (formState.targetBankId) {
    archiveMode.value = 'EXISTING_BANK';
    selectedExistingBankId.value = formState.targetBankId;
  } else {
    archiveMode.value = 'NEW_BANK';
    selectedExistingBankId.value = undefined;
    if (existingCourseBanks.value.length > 0) {
      selectedExistingBankId.value = existingCourseBanks.value[0].id;
    }
  }
  saveDialogVisible.value = true;
}

async function confirmBatchSave() {
  if (archiveMode.value === 'NEW_BANK' && !newBankName.value.trim()) {
    ElMessage.warning('请输入题库名称');
    return;
  }
  if (archiveMode.value === 'EXISTING_BANK' && !selectedExistingBankId.value) {
    ElMessage.warning('请选择目标题库');
    return;
  }

  saving.value = true;
  try {
    await batchSave({
      bankMode: archiveMode.value,
      bankName: newBankName.value.trim(),
      bankDescription: newBankDescription.value.trim(),
      existingBankId: selectedExistingBankId.value
    });
    saveDialogVisible.value = false;
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped lang="scss">
.question-preview-page {
  width: 100%;
  padding: 24px;
  background: #f8fafc;
  box-sizing: border-box;
  padding-bottom: 90px;

  .preview-top-bar {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 20px;
    margin-bottom: 24px;
    background: #FFFFFF;
    border-radius: 20px;
    padding: 24px 28px;
    border: 1px solid #EBF1F7;
    box-shadow: 0 4px 20px rgba(30, 80, 160, 0.04);
    flex-wrap: wrap;

    .summary-col {
      flex: 1;

      .title-row {
        display: flex;
        align-items: center;
        gap: 10px;
        flex-wrap: wrap;
        margin-bottom: 8px;

        .page-title {
          margin: 0;
          font-size: 22px;
          font-weight: 800;
          color: #0F172A;
        }

        .pill-count-badge {
          padding: 3px 11px;
          border-radius: 9999px;
          background: #EAF3FF;
          color: #1677FF;
          font-size: 12px;
          font-weight: 600;
        }

        .pill-score-badge {
          padding: 3px 11px;
          border-radius: 9999px;
          background: #FEF3C7;
          color: #D97706;
          font-size: 12px;
          font-weight: 600;
        }

        .pill-course-badge {
          padding: 3px 11px;
          border-radius: 9999px;
          background: #F1F5F9;
          color: #475569;
          font-size: 12px;
          font-weight: 600;
        }
      }

      .page-subtitle {
        margin: 0;
        font-size: 13px;
        color: #64748B;
        line-height: 1.5;
        max-width: 780px;
      }
    }

    .action-buttons-row {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
      flex-shrink: 0;
    }
  }

  .capsule-action-btn {
    height: 38px;
    padding: 0 16px;
    border-radius: 9999px;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    display: inline-flex;
    align-items: center;
    gap: 6px;
    white-space: nowrap;

    &--default {
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      color: #475569;

      &:hover:not(:disabled) {
        border-color: #1677FF;
        color: #1677FF;
        background: #F8FAFC;
      }
    }

    &--secondary {
      background: #EEF2FF;
      border: 1px solid #C7D2FE;
      color: #4F46E5;

      &:hover:not(:disabled) {
        background: #E0E7FF;
        color: #4338CA;
      }
    }

    &--primary {
      background: linear-gradient(135deg, #1677FF 0%, #3B82F6 100%);
      border: none;
      color: #FFFFFF;
      box-shadow: 0 4px 14px rgba(22, 119, 255, 0.28);

      &:hover:not(:disabled) {
        transform: translateY(-1px);
        box-shadow: 0 6px 18px rgba(22, 119, 255, 0.38);
      }

      &:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }
    }
  }

  .questions-stream-container {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .empty-questions-box {
    padding: 80px 0;
    text-align: center;
    background: #FFFFFF;
    border-radius: 20px;
    border: 1px solid #EBF1F7;

    .empty-icon {
      font-size: 48px;
      color: #94A3B8;
      display: inline-flex;
      margin-bottom: 14px;
    }

    h3 {
      margin: 0 0 8px 0;
      font-size: 18px;
      color: #1E293B;
    }

    p {
      margin: 0 0 20px 0;
      font-size: 13.5px;
      color: #94A3B8;
    }
  }

  // 底部长圆快捷 Dock
  .floating-dock-bar {
    position: fixed;
    bottom: 24px;
    left: 50%;
    transform: translateX(-50%);
    z-index: 100;

    .dock-inner {
      display: flex;
      align-items: center;
      gap: 20px;
      padding: 8px 10px 8px 24px;
      border-radius: 9999px;
      background: rgba(15, 23, 42, 0.92);
      backdrop-filter: blur(14px);
      box-shadow: 0 12px 36px rgba(0, 0, 0, 0.28);
      border: 1px solid rgba(255, 255, 255, 0.15);

      .dock-summary {
        font-size: 13.5px;
        color: #F8FAFC;

        strong {
          color: #38BDF8;
        }
      }

      .dock-btns {
        display: flex;
        align-items: center;
        gap: 8px;

        .dock-secondary-btn {
          height: 38px;
          padding: 0 16px;
          border-radius: 9999px;
          background: rgba(255, 255, 255, 0.15);
          color: #FFFFFF;
          font-size: 13px;
          font-weight: 600;
          border: 1px solid rgba(255, 255, 255, 0.2);
          cursor: pointer;
          display: inline-flex;
          align-items: center;
          gap: 5px;
          transition: all 0.2s;

          &:hover {
            background: rgba(255, 255, 255, 0.25);
          }
        }

        .dock-save-btn {
          height: 38px;
          padding: 0 20px;
          border-radius: 9999px;
          background: #1677FF;
          color: #FFFFFF;
          font-size: 13.5px;
          font-weight: 600;
          border: none;
          cursor: pointer;
          display: inline-flex;
          align-items: center;
          gap: 6px;
          transition: all 0.2s;

          &:hover:not(:disabled) {
            background: #4096FF;
            transform: translateY(-1px);
          }

          &:disabled {
            opacity: 0.6;
            cursor: wait;
          }
        }
      }
    }
  }

  .mr-1 {
    margin-right: 4px;
  }

  .w-full {
    width: 100%;
  }

  .save-dialog-hero-banner {
    display: flex;
    align-items: flex-start;
    gap: 14px;
    background: #f0fdf4;
    border: 1px solid #bbf7d0;
    border-radius: 14px;
    padding: 14px 18px;
    margin-bottom: 20px;

    .banner-icon-box {
      width: 36px;
      height: 36px;
      border-radius: 10px;
      background: #dcfce7;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #16a34a;
      font-size: 20px;
      flex-shrink: 0;
    }

    .banner-text-box {
      .banner-title {
        margin: 0 0 4px 0;
        font-size: 14px;
        font-weight: 700;
        color: #15803d;
      }

      .banner-desc {
        margin: 0;
        font-size: 12.5px;
        color: #166534;
        line-height: 1.5;
      }
    }
  }

  .archive-mode-radios {
    width: 100%;
    display: flex;

    :deep(.el-radio-button) {
      flex: 1;

      .el-radio-button__inner {
        width: 100%;
        padding: 9px 12px;
        font-size: 13px;
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }
  }

  .no-banks-hint {
    margin-top: 6px;
    font-size: 12px;
    color: #f59e0b;
  }

  .raw-pool-hint {
    display: flex;
    align-items: center;
    gap: 8px;
    background: #f8fafc;
    border: 1px dashed #cbd5e1;
    border-radius: 10px;
    padding: 12px 16px;
    font-size: 13px;
    color: #64748b;
  }
}
</style>
