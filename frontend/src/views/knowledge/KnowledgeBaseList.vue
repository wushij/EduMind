<template>
  <div class="knowledge-base-page-container">
    <!-- 1. 顶部专属 3D 视觉大 Banner (知识库banner.png，比例 2508×627) -->
    <div class="knowledge-banner-stage">
      <div class="banner-ratio-box">
        <img
          class="banner-image"
          :src="kbBannerImg"
          alt="知识库管理"
          draggable="false"
        />
        <div class="banner-float-actions">
          <button
            type="button"
            class="capsule-nav-btn capsule-nav-btn--primary"
            @click="showCreateDialog = true"
          >
            <el-icon><Plus /></el-icon>
            <span>新建知识库</span>
          </button>
        </div>
        <!-- 2. 全局统计指标长圆条 (嵌入 Banner 内部：位于图2“文档管理”四大能力胶囊正下方) -->
        <div class="banner-stats-dock">
          <div class="stat-item-pill">
            <el-icon class="pill-icon pill-icon--blue"><Collection /></el-icon>
            <span class="pill-label">知识库总数：</span>
            <strong class="pill-val">{{ knowledgeBases.length }} 个</strong>
          </div>

          <div class="stat-item-pill">
            <el-icon class="pill-icon pill-icon--emerald"><Document /></el-icon>
            <span class="pill-label">入库文档规模：</span>
            <strong class="pill-val">{{ totalDocs }} 篇</strong>
          </div>

          <div class="stat-item-pill">
            <el-icon class="pill-icon pill-icon--amber"><Coin /></el-icon>
            <span class="pill-label">向量切片总量：</span>
            <strong class="pill-val">{{ totalChunks }} 个</strong>
          </div>

          <div class="stat-item-pill stat-item-pill--green">
            <span class="status-pulse-dot"></span>
            <span class="pill-label">Milvus / PgVector 索引状态：</span>
            <strong class="pill-val">正常运行 (99.4% 命中率)</strong>
          </div>
        </div>
      </div>
    </div>

    <!-- 3. 长圆跑道分类 Tabs (Pill Tabs) -->
    <div class="kb-tabs-filter-bar">
      <div class="pill-tabs-track">
        <button
          v-for="tab in categoryTabs"
          :key="tab.value"
          type="button"
          class="pill-tab-item"
          :class="{ active: selectedCategory === tab.value }"
          @click="selectedCategory = tab.value"
        >
          <el-icon v-if="tab.icon" class="tab-icon"><component :is="tab.icon" /></el-icon>
          <span>{{ tab.label }}</span>
          <span class="tab-count-pill">{{ getCategoryCount(tab.value) }}</span>
        </button>
      </div>

      <div class="filter-right-actions">
        <input
          v-model="searchKeyword"
          type="text"
          class="kb-inline-search"
          placeholder="搜索知识库、文档或课程..."
        />
        <span class="total-hint">共展示 {{ filteredList.length }} 个知识库</span>
      </div>
    </div>

    <!-- 4. 知识库卡片响应式网格 -->
    <div v-if="filteredList.length > 0" class="knowledge-cards-grid">
      <KnowledgeBaseCard
        v-for="item in filteredList"
        :key="item.id"
        :item="item"
        @open="handleOpenDetail"
        @upload="handleUploadDoc"
        @delete="handleDeleteKb"
      />
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-kb-panel">
      <el-icon class="empty-icon"><Search /></el-icon>
      <h3 class="empty-title">未找到匹配的知识库</h3>
      <p class="empty-hint">建议调整上方搜索关键字或分类筛选条件，也可以点击上方按钮快速创建。</p>
      <button
        type="button"
        class="capsule-reset-btn"
        @click="resetFilters"
      >
        <span>重置筛选</span>
      </button>
    </div>

    <!-- 5. 创建新知识库长圆弹窗 (Pill Modal) -->
    <el-dialog
      v-model="showCreateDialog"
      title="创建新课程教学知识库"
      width="560px"
      class="capsule-custom-dialog"
      :show-close="true"
      destroy-on-close
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-position="top"
        class="capsule-dialog-form"
      >
        <el-form-item label="知识库名称" prop="name">
          <input
            v-model="createForm.name"
            type="text"
            class="capsule-form-input"
            placeholder="例如：大学物理（下）微课与习题知识库"
          />
        </el-form-item>

        <el-form-item label="所属学科/类别" prop="category">
          <div class="category-pill-selector">
            <span
              v-for="cat in categoryTabs.filter(t => t.value !== 'ALL')"
              :key="cat.value"
              class="cat-opt-pill"
              :class="{ active: createForm.category === cat.value }"
              @click="createForm.category = cat.value"
            >
              <el-icon v-if="cat.icon" class="cat-opt-icon"><component :is="cat.icon" /></el-icon>
              <span>{{ cat.label }}</span>
            </span>
          </div>
        </el-form-item>

        <el-form-item label="关联课程大纲" prop="courseName">
          <select v-model="createForm.courseName" class="capsule-form-select">
            <option value="高等数学（上）">高等数学（上）</option>
            <option value="数据结构与算法">数据结构与算法</option>
            <option value="大学物理">大学物理</option>
            <option value="人工智能导论">人工智能导论</option>
            <option value="线性代数">线性代数</option>
            <option value="不关联特定课程（通用）">不关联特定课程（通用）</option>
          </select>
        </el-form-item>

        <el-form-item label="向量嵌入模型 (Embedding Model)" prop="embeddingModel">
          <select v-model="createForm.embeddingModel" class="capsule-form-select">
            <option value="bge-large-zh-v1.5 (1024维)">bge-large-zh-v1.5 (1024维 - 推荐中文教学)</option>
            <option value="text-embedding-3-small (1536维)">text-embedding-3-small (1536维 - 多语言均衡)</option>
            <option value="bge-m3 (多模态高密度嵌入)">bge-m3 (多模态高密度嵌入)</option>
          </select>
        </el-form-item>

        <el-form-item label="知识库简介与入库文档说明" prop="description">
          <textarea
            v-model="createForm.description"
            rows="3"
            class="capsule-form-textarea"
            placeholder="简述该知识库收录的课件、习题或文献范围，以便 AI 助教优先检索..."
          ></textarea>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-actions-dock">
          <button
            type="button"
            class="capsule-modal-btn capsule-modal-btn--cancel"
            @click="showCreateDialog = false"
          >
            <span>取消</span>
          </button>
          <button
            type="button"
            class="capsule-modal-btn capsule-modal-btn--confirm"
            @click="handleConfirmCreate"
          >
            <span>立即创建知识库</span>
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  Collection,
  Document,
  Coin,
  Monitor,
  Reading,
  Files,
  Search,
  Grid,
  Plus
} from '@element-plus/icons-vue';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import KnowledgeBaseCard from '@/components/knowledge/KnowledgeBaseCard.vue';
import { useKnowledgeBase } from '@/composables/knowledge/useKnowledgeBase';
import type { KnowledgeBase } from '@/types/knowledge/knowledge-base';
import kbBannerImg from '@/assets/images/知识库banner.png';

const router = useRouter();
const { knowledgeBases, fetchKnowledgeBases, create, remove } = useKnowledgeBase();

onMounted(async () => {
  try {
    await fetchKnowledgeBases();
  } catch {
    // axios 拦截器已弹出错误提示
  }
});
const selectedCategory = ref<string>('ALL');
const searchKeyword = ref<string>('');
const showCreateDialog = ref(false);

const categoryTabs = [
  { label: '全部知识库', value: 'ALL', icon: Grid },
  { label: '专业核心', value: 'MAJOR', icon: Monitor },
  { label: '公卡通识', value: 'COMMON', icon: Reading },
  { label: '历年真题', value: 'EXAM', icon: Files }
];

const totalDocs = computed(() => {
  return knowledgeBases.value.reduce((sum, item) => sum + item.documentCount, 0);
});

const totalChunks = computed(() => {
  return knowledgeBases.value.reduce((sum, item) => sum + item.chunkCount, 0);
});

function getCategoryCount(cat: string) {
  if (cat === 'ALL') return knowledgeBases.value.length;
  return knowledgeBases.value.filter((item) => item.category === cat).length;
}

const filteredList = computed(() => {
  let list = [...knowledgeBases.value];

  if (selectedCategory.value !== 'ALL') {
    list = list.filter((item) => item.category === selectedCategory.value);
  }

  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase();
    list = list.filter(
      (item) =>
        item.name.toLowerCase().includes(kw) ||
        item.description.toLowerCase().includes(kw) ||
        (item.courseName && item.courseName.toLowerCase().includes(kw))
    );
  }

  return list;
});

function resetFilters() {
  selectedCategory.value = 'ALL';
  searchKeyword.value = '';
}

function handleOpenDetail(item: KnowledgeBase) {
  router.push(`/knowledge/${item.id}/documents`);
}

function handleUploadDoc(item: KnowledgeBase) {
  router.push(`/knowledge/${item.id}/documents`);
}

async function handleDeleteKb(item: KnowledgeBase) {
  try {
    await remove(item.id);
    ElMessage.success(`知识库《${item.name}》已移除`);
  } catch {
    ElMessage.error('删除知识库失败，请稍后重试');
  }
}

// 创建知识库表单
const createForm = reactive({
  name: '',
  category: 'MAJOR',
  courseName: '高等数学（上）',
  embeddingModel: 'bge-large-zh-v1.5 (1024维)',
  description: ''
});

const createRules = {
  name: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }]
};

async function handleConfirmCreate() {
  if (!createForm.name.trim()) {
    ElMessage.warning('请输入知识库名称');
    return;
  }

  const courseIdMap: Record<string, number> = {
    '高等数学（上）': 101,
    '数据结构与算法': 102
  };
  try {
    await create({
      name: createForm.name.trim(),
      description: createForm.description.trim(),
      courseId: courseIdMap[createForm.courseName]
    });
    await fetchKnowledgeBases();
    showCreateDialog.value = false;
    ElMessage.success('知识库创建成功');
    createForm.name = '';
    createForm.description = '';
  } catch {
    ElMessage.error('创建知识库失败，请稍后重试');
  }
}
</script>

<style scoped lang="scss">
.knowledge-base-page-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;

  // 顶部专属 3D 视觉大 Banner (比例 2508×627)
  .knowledge-banner-stage {
    width: 100%;
    margin-bottom: 2px;

    .banner-ratio-box {
      position: relative;
      width: 100%;
      aspect-ratio: 2508 / 627;
      border-radius: 16px;
      overflow: hidden;
      box-shadow: 0 6px 24px rgba(22, 119, 255, 0.08);
      border: 1px solid #E2E8F0;

      .banner-image {
        position: absolute;
        inset: 0;
        width: 100%;
        height: 100%;
        object-fit: cover;
        display: block;
        user-select: none;
      }

      .banner-float-actions {
        position: absolute;
        top: 20px;
        right: 24px;
        z-index: 2;

        .capsule-nav-btn {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          height: 38px;
          padding: 0 20px;
          border-radius: 9999px;
          font-size: 13.5px;
          font-weight: 600;
          cursor: pointer;
          border: none;
          background: #1677FF;
          color: #FFFFFF;
          box-shadow: 0 4px 16px rgba(22, 119, 255, 0.35);
          transition: all 0.2s ease;

          &:hover {
            background: #0958d9;
            transform: translateY(-1px);
          }
        }
      }

      // 2. 悬浮在 Banner 内部左下方：位于“文档管理”等四大胶囊正下方的统计条
      .banner-stats-dock {
        position: absolute;
        left: 4.4%;
        top: 84%;
        z-index: 2;
        display: flex;
        align-items: center;
        gap: 10px;
        flex-wrap: nowrap;
        max-width: 92%;

        .stat-item-pill {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          height: 34px;
          padding: 0 14px;
          border-radius: 9999px; // 长圆药丸
          background: rgba(255, 255, 255, 0.92);
          backdrop-filter: blur(8px);
          -webkit-backdrop-filter: blur(8px);
          border: 1px solid rgba(255, 255, 255, 0.85);
          box-shadow: 0 4px 14px rgba(22, 119, 255, 0.08);
          font-size: 12px;
          color: #475569;
          white-space: nowrap;
          flex-shrink: 0;
          transition: all 0.2s ease;

          &:hover {
            background: #FFFFFF;
            box-shadow: 0 6px 18px rgba(22, 119, 255, 0.14);
            transform: translateY(-1px);
          }

          .pill-icon {
            font-size: 14px;
            display: inline-flex;
            align-items: center;
            justify-content: center;

            &--blue { color: #2563EB; }
            &--emerald { color: #059669; }
            &--amber { color: #D97706; }
          }

          .pill-val {
            color: #0F172A;
            font-weight: 700;
          }

          &--green {
            background: rgba(240, 253, 244, 0.95);
            border-color: rgba(167, 243, 208, 0.9);
            color: #059669;
            box-shadow: 0 4px 14px rgba(16, 185, 129, 0.1);

            .pill-val {
              color: #059669;
            }

            .status-pulse-dot {
              width: 7px;
              height: 7px;
              border-radius: 50%;
              background: #10B981;
              box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.25);
            }
          }
        }
      }

      @media (max-width: 1400px) {
        .banner-stats-dock {
          top: 83%;
          gap: 8px;

          .stat-item-pill {
            height: 30px;
            padding: 0 10px;
            font-size: 11.5px;

            .pill-icon {
              font-size: 13px;
            }
          }
        }
      }

      @media (max-width: 1100px) {
        .banner-stats-dock {
          top: 81.5%;
          gap: 6px;

          .stat-item-pill {
            height: 28px;
            padding: 0 8px;
            font-size: 11px;
          }
        }
      }
    }
  }

  // 3. 分类选项卡条
  .kb-tabs-filter-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: #FFFFFF;
    border-radius: 18px;
    padding: 8px 14px;
    border: 1px solid #E2E8F0;
    box-shadow: 0 2px 12px rgba(30, 80, 150, 0.03);

    .pill-tabs-track {
      display: flex;
      align-items: center;
      gap: 8px;
      overflow-x: auto;

      .pill-tab-item {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        height: 38px;
        padding: 0 16px;
        border-radius: 9999px; // 纯正长圆
        background: #F8FAFC;
        border: 1px solid #E2E8F0;
        color: #64748B;
        font-size: 13px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s ease;
        white-space: nowrap;

        .tab-icon {
          font-size: 14px;
        }

        .tab-count-pill {
          padding: 1px 8px;
          border-radius: 9999px;
          background: #E2E8F0;
          color: #475569;
          font-size: 11px;
          font-weight: 600;
        }

        &:hover {
          color: #1677FF;
          background: #EFF6FF;
        }

        &.active {
          background: #1677FF;
          border-color: #1677FF;
          color: #FFFFFF;
          font-weight: 600;
          box-shadow: 0 3px 10px rgba(22, 119, 255, 0.25);

          .tab-count-pill {
            background: rgba(255, 255, 255, 0.25);
            color: #FFFFFF;
          }
        }
      }
    }

    .filter-right-actions {
      display: flex;
      align-items: center;
      gap: 12px;

      .kb-inline-search {
        width: 220px;
        height: 34px;
        padding: 0 14px;
        border-radius: 9999px;
        border: 1px solid #E2E8F0;
        background: #F8FAFC;
        font-size: 13px;
        color: #334155;
        outline: none;

        &:focus {
          border-color: #1677FF;
          background: #FFFFFF;
        }
      }

      .total-hint {
        font-size: 12.5px;
        color: #94A3B8;
      }
    }
  }

  // 4. 卡片网格
  .knowledge-cards-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
  }

  // 5. 空状态
  .empty-kb-panel {
    background: #FFFFFF;
    border-radius: 18px;
    border: 1px dashed #CBD5E1;
    padding: 60px 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;

    .empty-icon {
      font-size: 44px;
      color: #94A3B8;
      margin-bottom: 12px;
    }

    .empty-title {
      font-size: 16px;
      font-weight: 700;
      color: #1E293B;
      margin: 0 0 8px 0;
    }

    .empty-hint {
      font-size: 13px;
      color: #94A3B8;
      margin: 0 0 18px 0;
    }

    .capsule-reset-btn {
      height: 38px;
      padding: 0 24px;
      border-radius: 9999px;
      background: #1677FF;
      color: #FFFFFF;
      font-size: 13.5px;
      font-weight: 600;
      border: none;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        background: #4096FF;
      }
    }
  }
}

// 弹窗样式
.capsule-dialog-form {
  .capsule-form-input,
  .capsule-form-select {
    width: 100%;
    height: 42px;
    border-radius: 9999px; // 长圆输入框
    border: 1px solid #E2E8F0;
    background: #FFFFFF;
    padding: 0 18px;
    font-size: 13.5px;
    color: #1E293B;
    outline: none;
    box-sizing: border-box;
    transition: all 0.2s;

    &:focus {
      border-color: #1677FF;
      box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.16);
    }
  }

  .capsule-form-textarea {
    width: 100%;
    border-radius: 14px;
    border: 1px solid #E2E8F0;
    background: #FFFFFF;
    padding: 12px 16px;
    font-size: 13.5px;
    color: #1E293B;
    outline: none;
    box-sizing: border-box;
    resize: vertical;
    font-family: inherit;

    &:focus {
      border-color: #1677FF;
      box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.16);
    }
  }

  .category-pill-selector {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;

    .cat-opt-pill {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 6px 16px;
      border-radius: 9999px; // 长圆单选药丸
      border: 1px solid #E2E8F0;
      background: #F8FAFC;
      color: #64748B;
      font-size: 12.5px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s;

      .cat-opt-icon {
        font-size: 13px;
      }

      &:hover {
        border-color: #93C5FD;
        color: #1677FF;
      }

      &.active {
        background: #EFF6FF;
        border-color: #1677FF;
        color: #1677FF;
        font-weight: 600;
      }
    }
  }
}

.dialog-actions-dock {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;

  .capsule-modal-btn {
    height: 40px;
    padding: 0 24px;
    border-radius: 9999px; // 长圆按钮
    font-size: 13.5px;
    font-weight: 600;
    cursor: pointer;
    border: none;
    transition: all 0.2s;

    &--cancel {
      background: #F1F5F9;
      color: #64748B;

      &:hover {
        background: #E2E8F0;
      }
    }

    &--confirm {
      background: #1677FF;
      color: #FFFFFF;
      box-shadow: 0 3px 10px rgba(22, 119, 255, 0.28);

      &:hover {
        background: #4096FF;
      }
    }
  }
}

@media (max-width: 1280px) {
  .knowledge-cards-grid {
    grid-template-columns: repeat(2, 1fr) !important;
  }
}

@media (max-width: 768px) {
  .knowledge-cards-grid {
    grid-template-columns: 1fr !important;
  }
}
</style>
