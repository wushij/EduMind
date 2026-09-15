<template>
  <div class="control-panel-card no-print">
    <div class="panel-header">
      <div class="panel-title-group">
        <el-icon class="panel-icon"><Operation /></el-icon>
        <h3>排版参数与考务规制</h3>
      </div>
      <el-tag size="small" type="success" effect="light">实时仿真同步</el-tag>
    </div>

    <el-tabs v-model="activeTab" class="config-tabs">
      <el-tab-pane label="卷面考务" name="exam">
        <el-form :model="configForm" label-position="top" class="config-form" size="small">
          <el-form-item label="关联课程试卷">
            <el-select v-model="configForm.examId" style="width: 100%;">
              <el-option :value="101" label="2026年高三开学数学综合调研模考卷" />
              <el-option :value="102" label="高等数学期中阶段拔高模拟测试卷" />
              <el-option :value="103" label="高一物理必修第一册牛顿定律专项卷" />
              <el-option :value="104" label="高考化学工艺流程与反应原理攻坚卷" />
            </el-select>
          </el-form-item>

          <el-form-item label="试卷主标题">
            <el-input v-model="configForm.paperTitle" placeholder="如：2026年普通高等学校招生全国统一考试预测卷" />
          </el-form-item>

          <el-form-item label="试卷副标题">
            <el-input v-model="configForm.paperSubtitle" placeholder="如：理科数学 (全卷共4页 满分150分)" />
          </el-form-item>

          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item label="保密级别标示">
                <el-select v-model="configForm.confidentialLevel" style="width: 100%;">
                  <el-option label="绝密 ★ 启用前" value="绝密 ★ 启用前" />
                  <el-option label="机密 ★ 启用前" value="机密 ★ 启用前" />
                  <el-option label="内部教学诊断资料" value="内部教学诊断资料" />
                  <el-option label="不标注保密等级" value="" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="试卷满分 / 时长">
                <el-input v-model="examTimeScore" placeholder="150分 / 120分钟" />
              </el-form-item>
            </el-col>
          </el-row>

          <div class="sub-section-title">
            <span>考场标准化规范部件</span>
          </div>

          <div class="checkbox-list">
            <el-checkbox v-model="configForm.showSealingLine">
              <div class="checkbox-label-block">
                <span class="label-text">左侧密封装订线</span>
                <span class="label-desc">含考生考号、姓名、班级、考场及密封提醒</span>
              </div>
            </el-checkbox>

            <el-checkbox v-model="configForm.showStudentInfo">
              <div class="checkbox-label-block">
                <span class="label-text">考生准考证与条形码贴区</span>
                <span class="label-desc">符合现代考场机读扫描定位标准</span>
              </div>
            </el-checkbox>

            <el-checkbox v-model="configForm.showScoreGrid">
              <div class="checkbox-label-block">
                <span class="label-text">大题赋分网格统分栏</span>
                <span class="label-desc">一、二、三...总分、评卷人、复核人表格</span>
              </div>
            </el-checkbox>

            <el-checkbox v-model="configForm.showNoticeBar">
              <div class="checkbox-label-block">
                <span class="label-text">试卷作答注意事项说明框</span>
                <span class="label-desc">规范作答黑色中性笔与2B铅笔填涂提示</span>
              </div>
            </el-checkbox>
          </div>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="纸张排版" name="paper">
        <el-form :model="configForm" label-position="top" class="config-form" size="small">
          <el-form-item label="纸张规格与分栏">
            <el-radio-group v-model="configForm.paperSize" style="width: 100%;">
              <el-radio-button label="A4" style="width: 50%;">A4 标准单栏 (推荐)</el-radio-button>
              <el-radio-button label="B4" style="width: 50%;">B4/8开 双栏中缝</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="卷面主文字体">
            <el-select v-model="configForm.fontFamily" style="width: 100%;">
              <el-option label="标准书宋体 (教育部考试中心官方标准)" value="SimSun" />
              <el-option label="华文中宋 (粗实庄重)" value="STZhongsong" />
              <el-option label="楷体·GB2312 (清晰柔和适合初高中)" value="KaiTi" />
              <el-option label="现代无衬线黑体 (现代极简风格)" value="sans-serif" />
            </el-select>
          </el-form-item>

          <el-form-item label="题项排版密度与行距">
            <el-radio-group v-model="configForm.lineSpacing" style="width: 100%;">
              <el-radio-button label="compact" style="width: 33.33%;">紧凑省纸</el-radio-button>
              <el-radio-button label="normal" style="width: 33.33%;">标准规范</el-radio-button>
              <el-radio-button label="relaxed" style="width: 33.33%;">留白充裕</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="选择题选项排布方式">
            <el-select v-model="configForm.optionLayout" style="width: 100%;">
              <el-option label="4 选单行横向对齐 (A B C D 水平分布)" value="horizontal" />
              <el-option label="2 选双行网格对齐 (两行各两项)" value="grid" />
              <el-option label="单列垂直排列 (公式较长题型推荐)" value="vertical" />
            </el-select>
          </el-form-item>

          <el-form-item label="题目分值展示">
            <el-switch v-model="configForm.showPointBadge" active-text="在小题末尾标示分值 (本题5分)" />
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="安全与附录" name="security">
        <el-form :model="configForm" label-position="top" class="config-form" size="small">
          <el-form-item label="防扩散版权水印">
            <el-switch v-model="configForm.showWatermark" active-text="开启页面半透明倾斜防伪水印" />
          </el-form-item>

          <el-form-item label="水印文字内容" v-if="configForm.showWatermark">
            <el-input
              v-model="configForm.watermarkText"
              placeholder="如：智教云示范第一中学 · 内部教学试卷 · 严禁翻印"
            />
          </el-form-item>

          <div class="sub-section-title">
            <span>配套教辅与答卷附录</span>
          </div>

          <div class="checkbox-list">
            <el-checkbox v-model="configForm.showAnswerSheet">
              <div class="checkbox-label-block">
                <span class="label-text">附带机读答题卡页 (第3页)</span>
                <span class="label-desc">含标准 2B 铅笔选择题填涂点与主观题作答框</span>
              </div>
            </el-checkbox>

            <el-checkbox v-model="configForm.showAnalysis">
              <div class="checkbox-label-block">
                <span class="label-text">附带名师参考答案与踩分细则 (第4页)</span>
                <span class="label-desc">分步赋分标记、解题突破口与常见误区提示</span>
              </div>
            </el-checkbox>
          </div>
        </el-form>
      </el-tab-pane>
    </el-tabs>

    <div class="studio-action-dock">
      <el-button type="primary" class="primary-print-btn" @click="handlePrintDirect">
        <el-icon><Printer /></el-icon>
        <span>考务纯净打印 (Ctrl+P)</span>
      </el-button>
      <div class="secondary-btn-row">
        <el-button type="primary" plain :loading="exporting" @click="handleCreateExportTask">
          <el-icon><Download /></el-icon>
          <span>生成高保真 PDF</span>
        </el-button>
        <el-button plain @click="handleExportWord">
          <el-icon><Document /></el-icon>
          <span>导出 Word</span>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Download, Printer, Document, Operation } from '@element-plus/icons-vue';
import type { ExportConfigForm } from '@/composables/question/useExport';

defineProps<{
  configForm: ExportConfigForm;
  exporting: boolean;
  handlePrintDirect: () => void;
  handleCreateExportTask: () => void;
  handleExportWord: () => void;
}>();

const activeTab = defineModel<string>('activeTab', { required: true });
const examTimeScore = defineModel<string>('examTimeScore', { required: true });
</script>

<style scoped lang="scss">
.control-panel-card {
  background: #FFFFFF;
  border-radius: 16px;
  border: 1px solid #E2E8F0;
  padding: 20px;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);

  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 14px;

    .panel-title-group {
      display: flex;
      align-items: center;
      gap: 8px;

      .panel-icon {
        font-size: 18px;
        color: #2563EB;
      }

      h3 {
        font-size: 15px;
        font-weight: 600;
        color: #0F172A;
        margin: 0;
      }
    }
  }

  .config-tabs {
    :deep(.el-tabs__item) {
      font-size: 13px;
      font-weight: 500;
      padding: 0 14px;
    }
  }

  .config-form {
    margin-top: 10px;

    .sub-section-title {
      font-size: 12px;
      font-weight: 600;
      color: #475569;
      margin: 16px 0 10px;
      padding-bottom: 4px;
      border-bottom: 1px dashed #E2E8F0;
    }

    .checkbox-list {
      display: flex;
      flex-direction: column;
      gap: 12px;

      :deep(.el-checkbox) {
        align-items: flex-start;
        margin-right: 0;
        white-space: normal;
        height: auto;
      }

      .checkbox-label-block {
        display: flex;
        flex-direction: column;
        gap: 2px;
        margin-left: 4px;

        .label-text {
          font-size: 13px;
          font-weight: 500;
          color: #1E293B;
        }

        .label-desc {
          font-size: 11px;
          color: #94A3B8;
        }
      }
    }
  }

  .studio-action-dock {
    margin-top: 24px;
    display: flex;
    flex-direction: column;
    gap: 10px;

    .primary-print-btn {
      width: 100%;
      height: 40px;
      background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%);
      border: none;
      border-radius: 10px;
      font-weight: 600;
      font-size: 14px;
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
      transition: all 0.2s ease;

      &:hover {
        opacity: 0.92;
        transform: translateY(-1px);
      }
    }

    .secondary-btn-row {
      display: flex;
      gap: 10px;

      .el-button {
        flex: 1;
        border-radius: 8px;
      }
    }
  }
}
</style>
