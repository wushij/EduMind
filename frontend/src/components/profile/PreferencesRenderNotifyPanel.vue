<template>
  <div class="pref-render-notify-stack">
    <!-- 分区 3: 教学符号与智能渲染引擎 -->
    <div class="pref-card">
      <div class="card-header-bar">
        <div class="section-icon-badge math-badge">
          <el-icon><Operation /></el-icon>
        </div>
        <div class="section-title-wrap">
          <h3 class="card-section-title">教学符号与智能渲染引擎</h3>
          <span class="section-subtitle">配置数学公式 KaTeX 排版渲染、代码高亮与动态知识图谱编译</span>
        </div>
        <el-tag size="small" effect="plain" type="warning" class="section-pill-tag">渲染核心</el-tag>
      </div>

      <div class="options-grid">
        <!-- 自动解析渲染 LaTeX 数学公式 -->
        <div class="pref-row">
          <div class="row-info">
            <div class="row-title-line">
              <span class="row-title">自动解析并渲染 LaTeX 数学公式</span>
              <span class="math-demo-preview">如：$f(x)=\lim_{t \to 0}\frac{\sin t}{t}$</span>
            </div>
            <span class="row-desc">
              在试题正文、教学解析与 AI 答疑中，自动将 LaTeX 表达式编译为矢量级平滑公式。
            </span>
          </div>
          <el-switch v-model="preferenceStore.preferences.katexEnabled" class="custom-switch" />
        </div>

        <!-- 代码块语法高亮与行号 -->
        <div class="pref-row">
          <div class="row-info">
            <span class="row-title">代码块语法着色高亮与快捷复制</span>
            <span class="row-desc">
              自动检测 Python、C++、Java 等编程语言，渲染语法高亮色彩并提供一键复制代码块。
            </span>
          </div>
          <el-switch v-model="preferenceStore.preferences.codeHighlightEnabled" class="custom-switch" />
        </div>

        <!-- Mermaid 拓扑图表解析 -->
        <div class="pref-row">
          <div class="row-info">
            <span class="row-title">课程拓扑知识图谱与 Mermaid 图表解析</span>
            <span class="row-desc">
              自动将知识库输出的流程图、章节拓扑结构代码编译为动态交互式 SVG 图表。
            </span>
          </div>
          <el-switch v-model="preferenceStore.preferences.mermaidEnabled" class="custom-switch" />
        </div>
      </div>
    </div>

    <!-- 分区 4: 教学活动提醒与消息通知触达 -->
    <div class="pref-card">
      <div class="card-header-bar">
        <div class="section-icon-badge notify-badge">
          <el-icon><Bell /></el-icon>
        </div>
        <div class="section-title-wrap">
          <h3 class="card-section-title">教学协同提醒与消息触达通道</h3>
          <span class="section-subtitle">定制师生答卷提交、批量智能预批改与知识库向量化状态推送</span>
        </div>
        <el-tag size="small" effect="plain" type="info" class="section-pill-tag">即时通信</el-tag>
      </div>

      <div class="options-grid">
        <!-- 学生作业提交提醒 -->
        <div class="pref-row">
          <div class="row-info">
            <span class="row-title">学生作业与考核提交即时提醒</span>
            <span class="row-desc">
              当授课班级学生完成并提交在线作业或随堂考核时，在右上角站内信推送即时动态。
            </span>
          </div>
          <el-switch v-model="preferenceStore.preferences.notifySubmission" class="custom-switch" />
        </div>

        <!-- AI 评阅完成通知 -->
        <div class="pref-row">
          <div class="row-info">
            <span class="row-title">全班 AI 批量智能预批改完成通知</span>
            <span class="row-desc">
              全班答卷由 AI 大模型批量打分、错题归因与评语生成完毕后，推送班级学情大盘就绪通知。
            </span>
          </div>
          <el-switch v-model="preferenceStore.preferences.notifyGrading" class="custom-switch" />
        </div>

        <!-- 知识库向量化就绪 -->
        <div class="pref-row">
          <div class="row-info">
            <span class="row-title">校本教材切片向量化 (Embedding) 就绪通知</span>
            <span class="row-desc">
              教研组上传大型教材课件文档后，文本向量化切片与知识索引构建完成时发送就绪通知。
            </span>
          </div>
          <el-switch v-model="preferenceStore.preferences.notifyVector" class="custom-switch" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Operation, Bell } from '@element-plus/icons-vue';
import type { usePreferenceStore } from '@/stores/user/preference';

defineProps<{
  preferenceStore: ReturnType<typeof usePreferenceStore>;
}>();
</script>

<style scoped lang="scss">
.pref-render-notify-stack {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.pref-card {
  background: var(--el-bg-color, #ffffff);
  border-radius: 24px;
  border: 1px solid var(--el-border-color-lighter, #e2e8f0);
  padding: 24px 28px;
  box-shadow: 0 4px 20px rgba(30, 80, 150, 0.04);
  transition: all 0.3s ease;

  &:hover {
    border-color: rgba(37, 99, 235, 0.25);
    box-shadow: 0 6px 20px rgba(15, 23, 42, 0.06);
  }

  .card-header-bar {
    display: flex;
    align-items: center;
    gap: 14px;
    margin-bottom: 20px;
    padding-bottom: 14px;
    border-bottom: 1px solid var(--el-border-color-extra-light, #f1f5f9);

    .section-icon-badge {
      width: 38px;
      height: 38px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
      flex-shrink: 0;

      &.math-badge {
        background: #ccfbf1;
        color: #0d9488;
      }

      &.notify-badge {
        background: #fef3c7;
        color: #d97706;
      }
    }

    .section-title-wrap {
      display: flex;
      flex-direction: column;
      gap: 2px;
      flex: 1;

      .card-section-title {
        font-size: 16px;
        font-weight: 700;
        color: var(--el-text-color-primary, #0f172a);
        margin: 0;
      }

      .section-subtitle {
        font-size: 12px;
        color: var(--el-text-color-secondary, #64748b);
      }
    }

    .section-pill-tag {
      border-radius: 9999px;
      font-weight: 600;
      font-size: 11.5px;
      padding: 2px 10px;
    }
  }

  .options-grid {
    display: flex;
    flex-direction: column;
    gap: 12px;

    .pref-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 14px 18px;
      background: var(--el-fill-color-light, #f8fafc);
      border-radius: 14px;
      border: 1px solid var(--el-border-color-extra-light, #edf2f7);
      gap: 20px;
      flex-wrap: wrap;
      transition: all 0.2s ease;

      &:hover {
        background: var(--el-fill-color, #f1f5f9);
        border-color: var(--el-border-color-lighter, #e2e8f0);
      }

      .row-info {
        display: flex;
        flex-direction: column;
        gap: 4px;
        flex: 1;
        min-width: 0;

        .row-title {
          font-size: 14px;
          font-weight: 700;
          line-height: 1.45;
          color: var(--el-text-color-primary, #1e293b);
        }

        .row-title-line {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-wrap: wrap;

          .math-demo-preview {
            font-size: 12px;
            font-family: 'Times New Roman', serif;
            font-style: italic;
            color: #2563eb;
            background: #eff6ff;
            padding: 1px 8px;
            border-radius: 6px;
          }
        }

        .row-desc {
          font-size: 12px;
          color: var(--el-text-color-secondary, #64748b);
          line-height: 1.5;
        }
      }
    }
  }
}

:deep(.custom-switch) {
  --el-switch-on-color: #2563eb;
}
</style>
