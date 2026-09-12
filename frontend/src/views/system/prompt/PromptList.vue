<template>
  <div class="prompt-list-page">
    <!-- 头部横幅卡片 -->
    <div class="prompt-header-card">
      <div class="header-left">
        <h2>Prompt 提示词模板中心</h2>
        <p>统一纳管教学问答、向导出题、客观/主观批改及 Agent 编排的高质量提示词工程资产</p>
      </div>
      <div class="header-right">
        <el-button type="primary" :icon="Plus" @click="router.push('/system/prompts/editor')">
          新建 Prompt 模板
        </el-button>
      </div>
    </div>

    <!-- 检索与分类过滤器 -->
    <div class="filter-bar-card">
      <el-radio-group v-model="selectedCategory" @change="fetchPrompts(selectedCategory)">
        <el-radio-button label="ALL">全部模板</el-radio-button>
        <el-radio-button label="rag">课程问答 (RAG)</el-radio-button>
        <el-radio-button label="question">智能命题 (Exam)</el-radio-button>
        <el-radio-button label="grading">智能批改 (Grading)</el-radio-button>
        <el-radio-button label="teaching">教案备课</el-radio-button>
      </el-radio-group>

      <el-input
        v-model="searchKeyword"
        placeholder="按模板名称或特征检索..."
        style="width: 260px"
        clearable
        :prefix-icon="Search"
      />
    </div>

    <!-- 模板卡片网格 -->
    <div v-loading="loading" class="prompt-grid">
      <div
        v-for="item in filteredPrompts"
        :key="item.id"
        class="prompt-card"
        @click="router.push(`/system/prompts/editor/${item.id}`)"
      >
        <div class="card-header">
          <div class="header-main">
            <span class="prompt-code">{{ item.code }}</span>
            <span
              class="status-tag"
              :class="item.status === 'PUBLISHED' ? 'status-published' : 'status-draft'"
            >
              {{ item.status === 'PUBLISHED' ? '运行中 (Online)' : '草稿 (Draft)' }}
            </span>
          </div>
          <span class="version-badge">{{ item.version }}</span>
        </div>

        <h3 class="prompt-name" :title="item.name">{{ item.name }}</h3>
        <p class="prompt-desc" :title="item.description">{{ item.description }}</p>

        <!-- 变量标签展示 -->
        <div class="variables-preview">
          <span class="v-label">注入参数：</span>
          <span
            v-for="v in item.variables.slice(0, 3)"
            :key="v.name"
            class="v-chip"
          >
            &#123;&#123;{{ v.name }}&#125;&#125;
          </span>
          <span v-if="item.variables.length > 3" class="v-more">
            +{{ item.variables.length - 3 }}
          </span>
        </div>

        <!-- 底部元信息与快捷操作 -->
        <div class="card-footer">
          <div class="meta-left">
            <span class="model-badge">{{ item.boundModel }}</span>
            <span class="call-count">{{ item.callCount }} 次调用</span>
          </div>
          <el-button link type="primary" size="small">编辑与测试 →</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { usePrompt } from '@/composables/system/usePrompt';
import { Plus, Search } from '@element-plus/icons-vue';

const router = useRouter();
const selectedCategory = ref('ALL');
const searchKeyword = ref('');

const { loading, promptList, fetchPrompts } = usePrompt();

const filteredPrompts = computed(() => {
  let list = promptList.value;
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.toLowerCase();
    list = list.filter(p => p.name.toLowerCase().includes(kw) || p.code.toLowerCase().includes(kw));
  }
  return list;
});

onMounted(() => {
  fetchPrompts();
});
</script>

<style scoped lang="scss">
.prompt-list-page {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .prompt-header-card {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 12px;
    padding: 20px 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 2px 8px rgba(30, 80, 150, 0.03);

    .header-left {
      h2 {
        margin: 0;
        font-size: 20px;
        font-weight: 700;
        color: #0F172A;
      }
      p {
        margin: 4px 0 0 0;
        font-size: 13px;
        color: #64748B;
      }
    }
  }

  .filter-bar-card {
    background: #FFFFFF;
    border: 1px solid #E2E8F0;
    border-radius: 10px;
    padding: 12px 18px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .prompt-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
    min-height: 400px;

    @media (max-width: 1280px) {
      grid-template-columns: repeat(2, 1fr);
    }
    @media (max-width: 800px) {
      grid-template-columns: 1fr;
    }

    .prompt-card {
      background: #FFFFFF;
      border: 1px solid #E2E8F0;
      border-radius: 12px;
      padding: 18px;
      cursor: pointer;
      display: flex;
      flex-direction: column;
      gap: 10px;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

      &:hover {
        border-color: #93C5FD;
        box-shadow: 0 6px 18px rgba(37, 99, 235, 0.08);
        transform: translateY(-2px);
      }

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .header-main {
          display: flex;
          align-items: center;
          gap: 8px;

          .prompt-code {
            font-family: ui-monospace, monospace;
            font-size: 11px;
            font-weight: 700;
            color: #2563EB;
            background: #EFF6FF;
            padding: 2px 6px;
            border-radius: 4px;
          }

          .status-tag {
            font-size: 11px;
            padding: 1px 6px;
            border-radius: 4px;
            font-weight: 600;

            &.status-published {
              background: #ECFDF5;
              color: #059669;
            }
            &.status-draft {
              background: #FFFBEB;
              color: #D97706;
            }
          }
        }

        .version-badge {
          font-family: ui-monospace, monospace;
          font-size: 11px;
          color: #64748B;
          background: #F1F5F9;
          padding: 2px 6px;
          border-radius: 4px;
        }
      }

      .prompt-name {
        margin: 0;
        font-size: 15px;
        font-weight: 700;
        color: #1E293B;
      }

      .prompt-desc {
        margin: 0;
        font-size: 12.5px;
        color: #64748B;
        line-height: 1.55;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .variables-preview {
        display: flex;
        align-items: center;
        gap: 6px;
        flex-wrap: wrap;
        margin-top: 4px;

        .v-label {
          font-size: 11px;
          color: #94A3B8;
        }

        .v-chip {
          font-family: ui-monospace, monospace;
          font-size: 10.5px;
          background: #F8FAFC;
          border: 1px solid #E2E8F0;
          color: #475569;
          padding: 1px 6px;
          border-radius: 4px;
        }

        .v-more {
          font-size: 10.5px;
          color: #94A3B8;
        }
      }

      .card-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-top: 1px solid #F1F5F9;
        padding-top: 10px;
        margin-top: auto;

        .meta-left {
          display: flex;
          align-items: center;
          gap: 8px;

          .model-badge {
            font-size: 11px;
            color: #475569;
            background: #F1F5F9;
            padding: 2px 6px;
            border-radius: 4px;
          }

          .call-count {
            font-size: 11.5px;
            color: #94A3B8;
          }
        }
      }
    }
  }
}
</style>
