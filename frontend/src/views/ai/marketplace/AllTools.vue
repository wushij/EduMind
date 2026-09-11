<template>
  <div class="ai-marketplace-page">
    <!-- 5 页共用同一精美 Hero Banner (对标页面Banner规范与长圆搜索) -->
    <AIMarketplaceHero
      v-model:search-keyword="searchKeyword"
      active-category="ALL"
      @search="handleSearch"
    />

    <!-- 工具卡片网格列表 -->
    <div class="tools-grid-section">
      <div v-if="filteredTools.length > 0" class="tools-grid">
        <AIToolCard
          v-for="tool in filteredTools"
          :key="tool.id"
          :tool="tool"
          @open-detail="openDetail"
        />
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-box">
        <el-icon class="empty-icon-svg"><Search /></el-icon>
        <h4>未找到符合条件的 AI 工具</h4>
        <p>请尝试搜索“出题”、“批改”、“助教”等关键词</p>
        <button type="button" class="reset-btn" @click="searchKeyword = ''">
          清空搜索关键词
        </button>
      </div>
    </div>

    <!-- 工具详情抽屉 -->
    <AIToolDetailDrawer
      v-model="drawerVisible"
      :tool="selectedTool"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { Search } from '@element-plus/icons-vue';
import AIMarketplaceHero from '@/components/ai/AIMarketplaceHero.vue';
import AIToolCard from '@/components/ai/AIToolCard.vue';
import AIToolDetailDrawer from '@/components/ai/AIToolDetailDrawer.vue';
import { AITool } from '@/types/ai/tool';
import { useAITools } from '@/composables/ai/useAITools';

const { tools, fetchTools } = useAITools();
const searchKeyword = ref('');
const drawerVisible = ref(false);
const selectedTool = ref<AITool | null>(null);

onMounted(() => fetchTools('ALL'));

watch(searchKeyword, (kw) => {
  fetchTools('ALL', kw.trim() || undefined);
});

const filteredTools = computed(() => {
  if (!searchKeyword.value.trim()) return tools.value;
  const kw = searchKeyword.value.trim().toLowerCase();
  return tools.value.filter(
    t => t.name.toLowerCase().includes(kw) ||
         t.description.toLowerCase().includes(kw) ||
         t.tags.some(tag => tag.toLowerCase().includes(kw))
  );
});

function handleSearch(val: string) {
  searchKeyword.value = val;
  fetchTools('ALL', val.trim() || undefined);
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
        margin: 0 0 16px 0;
        font-size: 13px;
        color: #94A3B8;
      }

      .reset-btn {
        height: 36px;
        padding: 0 20px;
        border-radius: 9999px;
        background: #F1F5F9;
        border: 1px solid #E2E8F0;
        color: #475569;
        font-size: 13px;
        cursor: pointer;

        &:hover {
          color: #1677FF;
          border-color: #CBD5E1;
        }
      }
    }
  }
}
</style>
