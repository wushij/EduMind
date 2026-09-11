<template>
  <div class="ai-marketplace-page">
    <AIMarketplaceHero
      v-model:search-keyword="searchKeyword"
      active-category="RECOMMENDED"
      @search="handleSearch"
    />

    <div class="tools-grid-section">
      <div v-if="filteredTools.length > 0" class="tools-grid">
        <AIToolCard
          v-for="tool in filteredTools"
          :key="tool.id"
          :tool="tool"
          @open-detail="openDetail"
        />
      </div>

      <div v-else class="empty-box">
        <el-icon class="empty-icon-svg"><Star /></el-icon>
        <h4>暂无推荐类 AI 工具</h4>
        <p>可尝试切换分类或搜索其他工具</p>
      </div>
    </div>

    <AIToolDetailDrawer
      v-model="drawerVisible"
      :tool="selectedTool"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { Star } from '@element-plus/icons-vue';
import AIMarketplaceHero from '@/components/ai/AIMarketplaceHero.vue';
import AIToolCard from '@/components/ai/AIToolCard.vue';
import AIToolDetailDrawer from '@/components/ai/AIToolDetailDrawer.vue';
import { AITool } from '@/types/ai/tool';
import { useAITools } from '@/composables/ai/useAITools';

const { tools, fetchTools } = useAITools();
const searchKeyword = ref('');
const drawerVisible = ref(false);
const selectedTool = ref<AITool | null>(null);

onMounted(() => fetchTools('RECOMMENDED'));
watch(searchKeyword, (kw) => fetchTools('RECOMMENDED', kw.trim() || undefined));

const filteredTools = computed(() => {
  const list = tools.value.filter(t => t.isRecommended);
  if (!searchKeyword.value.trim()) return list;
  const kw = searchKeyword.value.trim().toLowerCase();
  return list.filter(
    t => t.name.toLowerCase().includes(kw) ||
         t.description.toLowerCase().includes(kw) ||
         t.tags.some(tag => tag.toLowerCase().includes(kw))
  );
});

function handleSearch(val: string) {
  searchKeyword.value = val;
  fetchTools('RECOMMENDED', val.trim() || undefined);
}

function openDetail(tool: AITool) {
  selectedTool.value = tool;
  drawerVisible.value = true;
}
</script>

<style scoped lang="scss">
.ai-marketplace-page {
  width: 100%;

  .tools-grid-section {
    min-height: 400px;

    .tools-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 20px;
    }

    .empty-box {
      padding: 60px 0;
      text-align: center;
      background: #FFFFFF;
      border-radius: 18px;
      border: 1px solid #EBF1F7;

      .empty-icon-svg {
        font-size: 38px;
        color: #94A3B8;
        display: block;
        margin: 0 auto 12px;
      }

      h4 {
        margin: 0 0 6px 0;
        font-size: 16px;
        color: #1E293B;
      }

      p {
        margin: 0;
        font-size: 13px;
        color: #94A3B8;
      }
    }
  }
}
</style>
