<template>
  <div class="user-ai-usage-page">
    <div class="usage-hero-card">
      <div class="hero-left">
        <h2>个人 AI 使用量与配额仪表盘</h2>
        <p>实时监控个人大模型调用频次、Token 消耗明细及今日可用额度</p>
      </div>
      <div class="hero-right">
        <el-tag type="success" size="large" effect="dark" class="quota-badge">
          今日额度充足 (剩余 82%)
        </el-tag>
      </div>
    </div>

    <!-- 个人核心数据看板 -->
    <div class="metric-cards-grid">
      <div class="metric-card">
        <div class="card-icon icon-blue">
          <el-icon><DataAnalysis /></el-icon>
        </div>
        <div class="card-info">
          <span class="label">今日已消耗 Token</span>
          <span class="val text-blue">18,240 <span class="unit">toks</span></span>
          <span class="sub">限额 100,000 toks / 日</span>
        </div>
      </div>

      <div class="metric-card">
        <div class="card-icon icon-purple">
          <el-icon><ChatDotRound /></el-icon>
        </div>
        <div class="card-info">
          <span class="label">累计智能问答与出题</span>
          <span class="val text-purple">146 <span class="unit">次</span></span>
          <span class="sub">平均耗时 320ms</span>
        </div>
      </div>

      <div class="metric-card">
        <div class="card-icon icon-amber">
          <el-icon><Coin /></el-icon>
        </div>
        <div class="card-info">
          <span class="label">本学期预估成本</span>
          <span class="val text-amber">¥ 3.86</span>
          <span class="sub">由教学平台全额资助</span>
        </div>
      </div>
    </div>

    <!-- 个人近 7 天调用记录 -->
    <el-card shadow="never" class="log-card">
      <template #header>
        <div class="card-header-row">
          <span class="title">近期个人 AI 交互记录</span>
        </div>
      </template>

      <el-table
        :data="personalLogs"
        stripe
        style="width: 100%"
        :header-cell-style="{ background: '#F8FAFC', color: '#475569', fontWeight: '600' }"
      >
        <el-table-column prop="tool" label="使用功能 / 场景" min-width="180">
          <template #default="{ row }">
            <span class="tool-title">{{ row.tool }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="model" label="基座模型" width="160">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.model }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tokens" label="消耗 Token" width="140" align="center">
          <template #default="{ row }">
            <span class="tokens-pill">{{ row.tokens }} toks</span>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="交互时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { DataAnalysis, ChatDotRound, Coin } from '@element-plus/icons-vue';

const personalLogs = ref([
  { tool: '课程 AI 智能助教 (RAG 问答)', model: 'DeepSeek-V3', tokens: 1240, time: '2026-09-11 11:25:35' },
  { tool: 'Java面向对象知识点切片答疑', model: 'DeepSeek-V3', tokens: 980, time: '2026-09-11 10:14:02' },
  { tool: 'AI 自适应练习生成 (AVL树)', model: 'Qwen-Plus', tokens: 1850, time: '2026-09-10 16:40:18' },
  { tool: '期末复习重点章节摘要提炼', model: 'DeepSeek-V3', tokens: 2460, time: '2026-09-10 14:10:50' },
  { tool: '主观代码题智能批改与纠错', model: 'Qwen-Plus', tokens: 1680, time: '2026-09-09 20:05:12' }
]);
</script>

<style scoped lang="scss">
.user-ai-usage-page {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .usage-hero-card {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    padding: 20px 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .hero-left {
      h2 { margin: 0; font-size: 20px; font-weight: 700; color: #0F172A; }
      p { margin: 4px 0 0 0; font-size: 13px; color: #64748B; }
    }
  }

  .metric-cards-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;

    @media (max-width: 900px) {
      grid-template-columns: 1fr;
    }

    .metric-card {
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      border-radius: 12px;
      padding: 18px 20px;
      display: flex;
      align-items: center;
      gap: 16px;

      .card-icon {
        width: 48px;
        height: 48px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 24px;

        &.icon-blue { background: #EFF6FF; color: #2563EB; }
        &.icon-purple { background: #FAF5FF; color: #9333EA; }
        &.icon-amber { background: #FFFBEB; color: #D97706; }
      }

      .card-info {
        display: flex;
        flex-direction: column;
        gap: 3px;

        .label { font-size: 12px; color: #64748B; }
        .val {
          font-size: 20px;
          font-weight: 700;
          color: #0F172A;

          &.text-blue { color: #2563EB; }
          &.text-purple { color: #9333EA; }
          &.text-amber { color: #D97706; }

          .unit { font-size: 12px; font-weight: normal; color: #94A3B8; }
        }
        .sub { font-size: 11px; color: #94A3B8; }
      }
    }
  }

  .log-card {
    border-radius: 12px;
    border-color: #E2E8F0;

    .card-header-row {
      .title { font-size: 14px; font-weight: 700; color: #1E293B; }
    }

    .tool-title { font-size: 13.5px; font-weight: 600; color: #1E293B; }

    .tokens-pill {
      font-family: ui-monospace, monospace;
      font-size: 12px;
      color: #2563EB;
      background: #EFF6FF;
      padding: 2px 8px;
      border-radius: 4px;
    }
  }
}
</style>
