<template>
  <aside class="exam-sealing-strip" aria-label="考场密封装订线">
    <div class="strip-inner">
      <div v-for="row in rows" :key="row.label" class="strip-row">
        <span class="strip-label">{{ row.label }}</span>
        <span class="strip-blank"></span>
      </div>
      <p class="strip-warning">密封线内严禁答题</p>
    </div>
  </aside>
</template>

<script setup lang="ts">
const rows = [
  { label: '学校' },
  { label: '姓名' },
  { label: '班级' },
  { label: '考场' },
  { label: '座位号' }
];
</script>

<style scoped lang="scss">
.exam-sealing-strip {
  width: 76px;
  flex-shrink: 0;
  border-right: 1px dashed #000;
  margin-right: 12px;
  padding: 8px 6px 8px 0;
  box-sizing: border-box;

  .strip-inner {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: flex-start;
    gap: 10px;
    padding-top: 6px;
    height: auto;
  }

  .strip-row {
    writing-mode: vertical-rl;
    text-orientation: mixed;
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    letter-spacing: 2px;
    color: #000;
  }

  .strip-label {
    font-weight: 600;
  }

  .strip-blank {
    display: inline-block;
    width: 1px;
    height: 36px;
    border-left: 1px solid #333;
  }

  .strip-warning {
    writing-mode: vertical-rl;
    text-orientation: mixed;
    margin: 8px 0 0;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 3px;
    color: #000;
  }
}
</style>
