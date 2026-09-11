<template>
  <div class="learning-path-panel">
    <div class="panel-header">
      <h4>自适应推荐学习路径</h4>
      <el-tag size="small" type="success">AI 动态生成</el-tag>
    </div>
    <div class="path-timeline">
      <el-timeline>
        <el-timeline-item
          v-for="(step, idx) in pathSteps"
          :key="idx"
          :type="step.status === 'COMPLETED' ? 'primary' : step.status === 'CURRENT' ? 'success' : 'info'"
          :hollow="step.status === 'PENDING'"
          :timestamp="step.estimatedTime"
        >
          <div class="timeline-content">
            <span class="step-title">{{ step.title }}</span>
            <p class="step-desc">{{ step.description }}</p>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

interface PathStep {
  title: string;
  description: string;
  estimatedTime: string;
  status: 'COMPLETED' | 'CURRENT' | 'PENDING';
}

const pathSteps = ref<PathStep[]>([
  {
    title: '知识点前置摸底：微积分极限定理',
    description: '通过 3 道基础辨析题测试对 $\\epsilon-\\delta$ 极限定义的认知。',
    estimatedTime: '已耗时 15 分钟',
    status: 'COMPLETED'
  },
  {
    title: '核心攻坚：导数几何意义与切线方程',
    description: '结合 AI 助教交互式问答，深度解析多项式与反函数导数求法。',
    estimatedTime: '正在学习 · 建议 25 分钟',
    status: 'CURRENT'
  },
  {
    title: '针对性巩固：易错题变式拔高练习',
    description: 'AI 根据历史错题库推送 2 道洛必达法则综合计算题。',
    estimatedTime: '待完成 · 预计 20 分钟',
    status: 'PENDING'
  }
]);
</script>

<style scoped lang="scss">
.learning-path-panel {
  padding: 16px;
  background: #FFFFFF;
  border-radius: 12px;

  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h4 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      color: #1E293B;
    }
  }

  .path-timeline {
    .timeline-content {
      .step-title {
        font-weight: 600;
        font-size: 14px;
        color: #1E293B;
      }
      .step-desc {
        margin: 4px 0 0 0;
        font-size: 13px;
        color: #64748B;
      }
    }
  }
}
</style>
