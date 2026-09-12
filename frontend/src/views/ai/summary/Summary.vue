<template>
  <div class="summary-page">
    <h2>AI 课程总结</h2>
    <el-card shadow="never">
      <el-form label-width="100px">
        <el-form-item label="文档 ID">
          <el-input-number v-model="documentId" :min="1" />
        </el-form-item>
        <el-form-item label="或粘贴文本">
          <el-input v-model="content" type="textarea" :rows="6" placeholder="可直接粘贴课件或讲义文本" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSummarize">生成总结</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card v-if="result" class="result-card" shadow="never">
      <pre class="summary-output">{{ result }}</pre>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { generateSummary } from '@/api/ai/summary';

const documentId = ref<number | undefined>();
const content = ref('');
const loading = ref(false);
const result = ref('');

async function handleSummarize() {
  loading.value = true;
  try {
    const res = await generateSummary({
      documentId: documentId.value,
      content: content.value || undefined
    });
    result.value = res?.data?.content || '';
    ElMessage.success('总结生成完成');
  } catch {
    ElMessage.error('总结生成失败');
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped lang="scss">
.summary-page {
  padding: 24px;

  h2 {
    margin: 0 0 16px;
  }

  .result-card {
    margin-top: 16px;
  }

  .summary-output {
    white-space: pre-wrap;
    line-height: 1.7;
    margin: 0;
  }
}
</style>
