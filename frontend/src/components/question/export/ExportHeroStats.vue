<template>
  <div class="hero-stats-row">
    <div class="hero-stat-card">
      <span class="stat-num text-primary">A4 / B4</span>
      <span class="stat-label">考场排版规范</span>
    </div>
    <div class="hero-stat-card">
      <span class="stat-num text-success">300 DPI</span>
      <span class="stat-label">矢量印刷级渲染</span>
    </div>
    <div class="hero-stat-card">
      <span class="stat-num text-warning">动态水印</span>
      <span class="stat-label">版权防泄密保护</span>
    </div>
    <div class="hero-stat-card">
      <span class="stat-num text-info">100%</span>
      <span class="stat-label">LaTeX 公式保真</span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.hero-stats-row {
  display: flex;
  gap: 16px;

  .hero-stat-card {
    background: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(8px);
    padding: 10px 18px;
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(15, 23, 42, 0.05);
    display: flex;
    flex-direction: column;
    align-items: center;

    .stat-num {
      font-size: 20px;
      font-weight: 700;
      line-height: 1.2;

      &.text-primary { color: #2563EB; }
      &.text-success { color: #16A34A; }
      &.text-warning { color: #D97706; }
      &.text-info { color: #0284C7; }
    }

    .stat-label {
      font-size: 11px;
      color: #64748B;
      margin-top: 4px;
    }
  }
}
</style>
