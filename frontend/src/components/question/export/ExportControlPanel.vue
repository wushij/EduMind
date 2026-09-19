<template>
  <div class="control-panel-card no-print">
    <div class="panel-header">
      <div class="panel-title-group">
        <el-icon class="panel-icon"><Operation /></el-icon>
        <h3>排版参数与考务规制</h3>
      </div>
      <span class="panel-live-dot" title="修改会同步到右侧预览">预览同步</span>
    </div>

    <div class="panel-tab-bar" role="tablist" aria-label="排版配置分类">
      <button
        v-for="tab in panelTabs"
        :key="tab.name"
        type="button"
        role="tab"
        class="panel-tab-btn"
        :class="{ 'is-active': activeTab === tab.name }"
        :aria-selected="activeTab === tab.name"
        @click="activeTab = tab.name"
      >
        {{ tab.label }}
      </button>
    </div>

    <div class="panel-tab-body">
      <el-form
        v-show="activeTab === 'exam'"
        :model="configForm"
        label-position="top"
        class="config-form"
      >
        <el-form-item label="关联试卷">
          <el-select
            v-model="configForm.examId"
            class="field-full"
            filterable
            placeholder="选择要排版的试卷"
            :loading="examsLoading"
            no-data-text="暂无试卷，请先在试卷管理中组卷"
          >
            <el-option
              v-for="exam in examOptions"
              :key="exam.id"
              :value="exam.id"
              :label="exam.title"
            >
              <span class="exam-opt-title">{{ exam.title }}</span>
              <span v-if="exam.courseName" class="exam-opt-course">{{ exam.courseName }}</span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="主标题">
          <el-input v-model="configForm.paperTitle" placeholder="如：2026 学年第一学期期中检测" />
        </el-form-item>

        <el-form-item label="副标题">
          <el-input v-model="configForm.paperSubtitle" placeholder="科目 · 满分 · 考试时长" />
        </el-form-item>

        <div class="field-row-2">
          <el-form-item label="保密标识">
            <el-select v-model="configForm.confidentialLevel" class="field-full">
              <el-option label="绝密 ★ 启用前" value="绝密 ★ 启用前" />
              <el-option label="机密 ★ 启用前" value="机密 ★ 启用前" />
              <el-option label="内部资料" value="内部教学诊断资料" />
              <el-option label="不标注" value="" />
            </el-select>
          </el-form-item>
          <el-form-item label="满分 / 时长">
            <el-input v-model="examTimeScore" placeholder="100 分 / 90 分钟" />
          </el-form-item>
        </div>

        <div class="sub-section-title">卷面规范部件</div>

        <div class="option-card-list">
          <label v-for="item in examPartOptions" :key="item.key" class="option-card">
            <el-checkbox v-model="configForm[item.key]" />
            <span class="option-card-text">
              <span class="option-card-title">{{ item.title }}</span>
              <span class="option-card-desc">{{ item.desc }}</span>
            </span>
          </label>
        </div>
      </el-form>

      <el-form
        v-show="activeTab === 'paper'"
        :model="configForm"
        label-position="top"
        class="config-form"
      >
        <el-form-item label="纸张规格">
          <div class="option-grid option-grid--2">
            <button
              type="button"
              class="option-tile"
              :class="{ 'is-active': configForm.paperSize === 'A4' }"
              @click="configForm.paperSize = 'A4'"
            >
              <span class="tile-title">A4 单栏</span>
              <span class="tile-desc">标准考务 · 推荐</span>
            </button>
            <button
              type="button"
              class="option-tile"
              :class="{ 'is-active': configForm.paperSize === 'B4' }"
              @click="configForm.paperSize = 'B4'"
            >
              <span class="tile-title">B4 双栏</span>
              <span class="tile-desc">8 开 · 题量较大</span>
            </button>
          </div>
        </el-form-item>

        <el-form-item label="正文字体">
          <el-select v-model="configForm.fontFamily" class="field-full">
            <el-option label="宋体（考务常用）" value="SimSun" />
            <el-option label="华文中宋" value="STZhongsong" />
            <el-option label="楷体 GB2312" value="KaiTi" />
            <el-option label="黑体（简洁）" value="sans-serif" />
          </el-select>
        </el-form-item>

        <el-form-item label="行距密度">
          <div class="option-grid option-grid--3">
            <button
              v-for="opt in lineSpacingOptions"
              :key="opt.value"
              type="button"
              class="option-tile option-tile--compact"
              :class="{ 'is-active': configForm.lineSpacing === opt.value }"
              @click="configForm.lineSpacing = opt.value"
            >
              <span class="tile-title">{{ opt.label }}</span>
            </button>
          </div>
        </el-form-item>

        <el-form-item label="选择题选项排布">
          <el-select v-model="configForm.optionLayout" class="field-full">
            <el-option label="单行横排（A B C D）" value="horizontal" />
            <el-option label="双列网格" value="grid" />
            <el-option label="单列竖排（长题干）" value="vertical" />
          </el-select>
        </el-form-item>

        <el-form-item label="小题分值">
          <div class="switch-row">
            <el-switch v-model="configForm.showPointBadge" />
            <span class="switch-hint">在题干末尾显示「（5 分）」</span>
          </div>
        </el-form-item>
      </el-form>

      <el-form
        v-show="activeTab === 'security'"
        :model="configForm"
        label-position="top"
        class="config-form"
      >
        <el-form-item label="防伪水印">
          <div class="switch-row">
            <el-switch v-model="configForm.showWatermark" />
            <span class="switch-hint">页面半透明倾斜水印</span>
          </div>
        </el-form-item>

        <el-form-item v-if="configForm.showWatermark" label="水印文案">
          <el-input
            v-model="configForm.watermarkText"
            placeholder="如：智教云 · 内部试卷 · 严禁翻印"
          />
        </el-form-item>

        <div class="sub-section-title">附录页面</div>

        <div class="option-card-list">
          <label v-for="item in appendixOptions" :key="item.key" class="option-card">
            <el-checkbox v-model="configForm[item.key]" />
            <span class="option-card-text">
              <span class="option-card-title">{{ item.title }}</span>
              <span class="option-card-desc">{{ item.desc }}</span>
            </span>
          </label>
        </div>
      </el-form>
    </div>

    <div class="studio-action-dock">
      <button type="button" class="dock-btn dock-btn--print" @click="handlePrintDirect">
        <el-icon><Printer /></el-icon>
        <span>纯净打印</span>
      </button>
      <div class="dock-btn-row">
        <button
          type="button"
          class="dock-btn dock-btn--primary"
          :disabled="exporting"
          @click="handleCreateExportTask"
        >
          <el-icon v-if="exporting" class="is-loading"><Loading /></el-icon>
          <el-icon v-else><Download /></el-icon>
          <span>{{ exporting ? '排版生成中…' : '生成 PDF' }}</span>
        </button>
        <button type="button" class="dock-btn dock-btn--secondary" @click="handleExportWord">
          <el-icon><Document /></el-icon>
          <span>生成 Word</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Download, Printer, Document, Operation, Loading } from '@element-plus/icons-vue';
import type { ExportConfigForm } from '@/composables/question/useExport';
import type { ExamOptionItem } from '@/types/question/export';

defineProps<{
  configForm: ExportConfigForm;
  examOptions: ExamOptionItem[];
  examsLoading?: boolean;
  exporting: boolean;
  handlePrintDirect: () => void;
  handleCreateExportTask: () => void;
  handleExportWord: () => void;
}>();

const activeTab = defineModel<string>('activeTab', { required: true });
const examTimeScore = defineModel<string>('examTimeScore', { required: true });

const panelTabs = [
  { name: 'exam', label: '卷面考务' },
  { name: 'paper', label: '纸张排版' },
  { name: 'security', label: '安全附录' }
];

const lineSpacingOptions = [
  { value: 'compact', label: '紧凑' },
  { value: 'normal', label: '标准' },
  { value: 'relaxed', label: '宽松' }
];

type ExamPartKey = 'showSealingLine' | 'showStudentInfo' | 'showScoreGrid' | 'showNoticeBar';
type AppendixKey = 'showAnswerSheet' | 'showAnalysis';

const examPartOptions: { key: ExamPartKey; title: string; desc: string }[] = [
  { key: 'showSealingLine', title: '左侧密封线', desc: '学校、姓名、班级、考场、座位号' },
  { key: 'showStudentInfo', title: '考生信息区', desc: '姓名、准考证格或条形码贴区' },
  { key: 'showScoreGrid', title: '大题得分表', desc: '题号、得分、满分、评卷人' },
  { key: 'showNoticeBar', title: '考生须知', desc: '作答规范与填涂说明' }
];

const appendixOptions: { key: AppendixKey; title: string; desc: string }[] = [
  { key: 'showAnswerSheet', title: '配套机读答题卡', desc: '按本卷题量生成填涂区与作答框（仿高考样式）' },
  { key: 'showAnalysis', title: '参考答案', desc: '含解析，供教师用卷' }
];
</script>

<style scoped lang="scss">
.control-panel-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  padding: 18px 18px 16px;
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.04);
  display: flex;
  flex-direction: column;
  min-height: 0;

  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .panel-title-group {
      display: flex;
      align-items: center;
      gap: 8px;
      min-width: 0;

      .panel-icon {
        font-size: 17px;
        color: #2563eb;
        flex-shrink: 0;
      }

      h3 {
        font-size: 15px;
        font-weight: 600;
        color: #0f172a;
        margin: 0;
        letter-spacing: 0.02em;
      }
    }

    .panel-live-dot {
      flex-shrink: 0;
      font-size: 11px;
      font-weight: 600;
      color: #059669;
      padding: 3px 10px;
      border-radius: 999px;
      background: #ecfdf5;
      border: 1px solid #a7f3d0;
    }
  }

  .panel-tab-bar {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 4px;
    padding: 4px;
    background: #f1f5f9;
    border-radius: 12px;
    border: 1px solid #e2e8f0;
    margin-bottom: 14px;

    .panel-tab-btn {
      border: none;
      background: transparent;
      padding: 8px 6px;
      font-size: 12px;
      font-weight: 600;
      color: #64748b;
      border-radius: 8px;
      cursor: pointer;
      transition: background 0.15s ease, color 0.15s ease, box-shadow 0.15s ease;

      &:hover {
        color: #334155;
      }

      &.is-active {
        background: #ffffff;
        color: #1d4ed8;
        box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
      }
    }
  }

  .panel-tab-body {
    flex: 1;
    min-height: 0;
    max-height: 520px;
    overflow-y: auto;
    padding-right: 2px;

    &::-webkit-scrollbar {
      width: 5px;
    }

    &::-webkit-scrollbar-thumb {
      background: #cbd5e1;
      border-radius: 4px;
    }
  }

  .config-form {
    :deep(.el-form-item) {
      margin-bottom: 14px;
    }

    :deep(.el-form-item__label) {
      font-size: 12px;
      font-weight: 600;
      color: #475569;
      line-height: 1.3;
      padding-bottom: 6px;
    }

    :deep(.el-input__wrapper),
    :deep(.el-select__wrapper) {
      border-radius: 10px;
      box-shadow: 0 0 0 1px #e2e8f0 inset;
    }

    .field-full {
      width: 100%;
    }

    .field-row-2 {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 10px;
    }

    .exam-opt-title {
      display: block;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      max-width: 220px;
    }

    .exam-opt-course {
      float: right;
      color: #94a3b8;
      font-size: 12px;
      margin-left: 8px;
    }

    .sub-section-title {
      font-size: 11px;
      font-weight: 700;
      color: #64748b;
      text-transform: none;
      letter-spacing: 0.04em;
      margin: 4px 0 10px;
    }

    .option-grid {
      display: grid;
      gap: 8px;
      width: 100%;

      &--2 {
        grid-template-columns: 1fr 1fr;
      }

      &--3 {
        grid-template-columns: repeat(3, minmax(0, 1fr));
      }
    }

    .option-tile {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 2px;
      padding: 10px 12px;
      border-radius: 10px;
      border: 1px solid #e2e8f0;
      background: #f8fafc;
      cursor: pointer;
      text-align: left;
      transition: border-color 0.15s ease, background 0.15s ease, box-shadow 0.15s ease;

      &:hover {
        border-color: #cbd5e1;
        background: #ffffff;
      }

      &.is-active {
        border-color: #2563eb;
        background: #eff6ff;
        box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.15);
      }

      &--compact {
        align-items: center;
        padding: 10px 8px;

        .tile-title {
          font-size: 12px;
        }
      }

      .tile-title {
        font-size: 13px;
        font-weight: 600;
        color: #1e293b;
        line-height: 1.3;
      }

      .tile-desc {
        font-size: 11px;
        color: #94a3b8;
        line-height: 1.3;
      }
    }

    .switch-row {
      display: flex;
      align-items: center;
      gap: 10px;
      min-height: 32px;

      .switch-hint {
        font-size: 12px;
        color: #64748b;
      }
    }

    .option-card-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .option-card {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      padding: 10px 12px;
      border-radius: 10px;
      border: 1px solid #e2e8f0;
      background: #fafbfc;
      cursor: pointer;
      transition: border-color 0.15s ease, background 0.15s ease;

      &:hover {
        border-color: #cbd5e1;
        background: #ffffff;
      }

      :deep(.el-checkbox) {
        height: auto;
        margin-top: 2px;
      }

      .option-card-text {
        display: flex;
        flex-direction: column;
        gap: 2px;
        min-width: 0;
      }

      .option-card-title {
        font-size: 13px;
        font-weight: 600;
        color: #1e293b;
        line-height: 1.35;
      }

      .option-card-desc {
        font-size: 11px;
        color: #94a3b8;
        line-height: 1.4;
      }
    }
  }

  .studio-action-dock {
    margin-top: 16px;
    padding-top: 14px;
    border-top: 1px solid #f1f5f9;
    display: flex;
    flex-direction: column;
    gap: 8px;

    .dock-btn-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 8px;
    }

    .dock-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      height: 40px;
      padding: 0 16px;
      border-radius: 999px;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      border: none;
      transition: background 0.15s ease, border-color 0.15s ease, opacity 0.15s ease;

      &:disabled {
        opacity: 0.65;
        cursor: not-allowed;
      }

      &--print {
        width: 100%;
        background: #059669;
        color: #ffffff;

        &:hover:not(:disabled) {
          background: #047857;
        }
      }

      &--primary,
      &--secondary {
        width: 100%;
        min-height: 40px;
      }

      &--primary {
        background: #1677ff;
        color: #ffffff;

        &:hover:not(:disabled) {
          background: #0958d9;
        }
      }

      &--secondary {
        background: #ffffff;
        color: #1e293b;
        border: 1.5px solid #cbd5e1;

        &:hover:not(:disabled) {
          color: #1677ff;
          border-color: #93c5fd;
          background: #f8fafc;
        }
      }
    }
  }
}
</style>
