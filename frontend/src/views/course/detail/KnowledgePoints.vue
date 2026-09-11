<template>
  <div class="kp-tab-container">
    <!-- 工具筛选条 -->
    <div class="kp-toolbar-card">
      <div class="toolbar-left">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索知识点名称、编码或核心概念..."
          clearable
          :prefix-icon="Search"
          style="width: 280px"
        />
        <el-select
          v-model="selectedChapterId"
          placeholder="全部章节"
          clearable
          style="width: 220px"
          @change="loadKnowledgePoints"
        >
          <el-option label="全部章节目录" :value="undefined" />
          <el-option
            v-for="chap in chapters"
            :key="chap.id"
            :label="chap.title"
            :value="chap.id"
          />
        </el-select>
        <el-select v-model="selectedLevel" placeholder="认知维度" clearable style="width: 140px">
          <el-option label="全部维度" value="" />
          <el-option label="识记 (Remember)" value="REMEMBER" />
          <el-option label="理解 (Understand)" value="UNDERSTAND" />
          <el-option label="应用 (Apply)" value="APPLY" />
          <el-option label="综合探究 (Analyze)" value="ANALYZE" />
        </el-select>
      </div>

      <div class="toolbar-right">
        <el-button type="primary" :icon="Plus" @click="showCreateDrawer = true">
          新增知识点
        </el-button>
      </div>
    </div>

    <!-- 知识点卡片与网格列表 -->
    <div v-loading="loading" class="kp-grid-wrapper">
      <div v-if="filteredPoints.length > 0" class="kp-cards-grid">
        <div
          v-for="(kp, index) in filteredPoints"
          :key="kp.id || index"
          class="kp-card-item"
        >
          <div class="card-head">
            <div class="badge-row">
              <span class="kp-code font-mono">{{ kp.code || `KP-${100 + (kp.id || index)}` }}</span>
              <el-tag size="small" :type="getLevelTagType(kp.cognitiveDimension)">
                {{ getLevelLabel(kp.cognitiveDimension) }}
              </el-tag>
            </div>
            <div class="star-rating">
              <span v-for="s in (kp.importance || 3)" :key="s" class="star">
                <el-icon><StarFilled /></el-icon>
              </span>
            </div>
          </div>

          <h3 class="kp-name">{{ kp.title || kp.name }}</h3>
          <p class="kp-desc">{{ kp.description || '本知识点为课程大纲的核心重点内容，涉及算法设计、概念理解与综合实战考查。' }}</p>

          <div v-if="kp.prerequisites && kp.prerequisites.length" class="prereq-row">
            <span class="prereq-label">前置要求：</span>
            <div class="prereq-tags">
              <span v-for="pre in kp.prerequisites" :key="pre" class="pre-tag">{{ pre }}</span>
            </div>
          </div>

          <div class="card-footer">
            <span class="chapter-hint">所属：{{ getChapterTitle(kp.chapterId) }}</span>
            <div class="actions">
              <el-button type="primary" link size="small" @click="handleExploreGraph(kp)">
                关联图谱
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state-panel">
        <div class="empty-icon">
          <el-icon><Opportunity /></el-icon>
        </div>
        <h3>当前章节暂无录入的知识点</h3>
        <p>您可以点击上方“新增知识点”，丰富课程的知识体系拓扑结构。</p>
      </div>
    </div>

    <!-- 新增知识点抽屉 -->
    <el-drawer v-model="showCreateDrawer" title="录入课程新知识点" size="520px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="知识点名称" required>
          <el-input v-model="newKp.title" placeholder="例如：双向链表插入与删除节点算法" />
        </el-form-item>
        <el-form-item label="所属课程章节" required>
          <el-select v-model="newKp.chapterId" placeholder="选择章节" class="w-full">
            <el-option
              v-for="chap in chapters"
              :key="chap.id"
              :label="chap.title"
              :value="chap.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="认知目标维度">
          <el-select v-model="newKp.cognitiveDimension" class="w-full">
            <el-option label="识记 (Remember)" value="REMEMBER" />
            <el-option label="理解 (Understand)" value="UNDERSTAND" />
            <el-option label="应用 (Apply)" value="APPLY" />
            <el-option label="综合探究 (Analyze)" value="ANALYZE" />
          </el-select>
        </el-form-item>
        <el-form-item label="重要度星级">
          <el-rate v-model="newKp.importance" :max="5" />
        </el-form-item>
        <el-form-item label="核心考点与概念阐述">
          <el-input
            v-model="newKp.description"
            type="textarea"
            :rows="4"
            placeholder="详细说明该知识点需掌握的概念、代码要求与易错细节..."
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateDrawer = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleSaveNewKp">确认添加</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Search, Plus, StarFilled, Opportunity } from '@element-plus/icons-vue';
import { getCourseKnowledgePoints, createKnowledgePoint } from '@/api/course/knowledge-point';
import { getChapters } from '@/api/course/chapter';

const route = useRoute();
const courseId = computed(() => Number(route.params.id) || 101);

const loading = ref(false);
const creating = ref(false);
const showCreateDrawer = ref(false);

const searchKeyword = ref('');
const selectedChapterId = ref<number | undefined>(undefined);
const selectedLevel = ref('');

const chapters = ref<any[]>([]);
const knowledgePoints = ref<any[]>([]);

const newKp = reactive({
  title: '',
  chapterId: 1,
  cognitiveDimension: 'APPLY',
  importance: 4,
  description: ''
});

onMounted(async () => {
  await Promise.all([loadChapters(), loadKnowledgePoints()]);
});

async function loadChapters() {
  try {
    const res = await getChapters(courseId.value);
    chapters.value = res.data || [];
  } catch (err) {
    console.warn('加载课程章节失败:', err);
    chapters.value = [];
  }
}

async function loadKnowledgePoints() {
  loading.value = true;
  try {
    const res = await getCourseKnowledgePoints(courseId.value, selectedChapterId.value);
    knowledgePoints.value = res.data || [];
  } catch (err: any) {
    ElMessage.error(err?.message || '获取课程知识点失败');
    knowledgePoints.value = [];
  } finally {
    loading.value = false;
  }
}

const filteredPoints = computed(() => {
  return knowledgePoints.value.filter(kp => {
    if (selectedChapterId.value && kp.chapterId !== selectedChapterId.value) return false;
    if (selectedLevel.value && kp.cognitiveDimension !== selectedLevel.value) return false;
    if (searchKeyword.value.trim()) {
      const kw = searchKeyword.value.trim().toLowerCase();
      const inTitle = (kp.title || kp.name || '').toLowerCase().includes(kw);
      const inCode = (kp.code || '').toLowerCase().includes(kw);
      if (!inTitle && !inCode) return false;
    }
    return true;
  });
});

function getChapterTitle(chapterId?: number) {
  const c = chapters.value.find(item => item.id === chapterId);
  return c?.title || '通用教学大纲';
}

function getLevelLabel(level?: string) {
  const map: Record<string, string> = {
    REMEMBER: '识记概念',
    UNDERSTAND: '理解领会',
    APPLY: '实践应用',
    ANALYZE: '综合探究'
  };
  return map[level || 'APPLY'] || '核心要点';
}

function getLevelTagType(level?: string) {
  const map: Record<string, string> = {
    REMEMBER: 'info',
    UNDERSTAND: 'primary',
    APPLY: 'success',
    ANALYZE: 'warning'
  };
  return (map[level || 'APPLY'] as any) || '';
}

async function handleSaveNewKp() {
  if (!newKp.title.trim()) {
    ElMessage.warning('知识点名称不能为空');
    return;
  }
  creating.value = true;
  try {
    await createKnowledgePoint(courseId.value, {
      chapterId: newKp.chapterId,
      title: newKp.title.trim(),
      sortOrder: newKp.importance || 0
    });
    ElMessage.success('知识点录入成功并已持久化入库！');
    showCreateDrawer.value = false;
    newKp.title = '';
    newKp.description = '';
    await loadKnowledgePoints();
  } catch (err: any) {
    ElMessage.error(err?.message || '新增知识点失败，请稍后重试');
  } finally {
    creating.value = false;
  }
}

function handleExploreGraph(kp: any) {
  ElMessage.info(`正在聚焦知识图谱节点：${kp.title}`);
}
</script>

<style scoped lang="scss">
.kp-tab-container {
  padding: 8px 0;

  .kp-toolbar-card {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    padding: 14px 20px;
    margin-bottom: 20px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 16px;

    .toolbar-left {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }
  }

  .kp-grid-wrapper {
    .kp-cards-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 18px;

      .kp-card-item {
        background: #ffffff;
        border: 1px solid #e2e8f0;
        border-radius: 14px;
        padding: 20px;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        transition: all 0.2s ease;

        &:hover {
          border-color: #cbd5e1;
          box-shadow: 0 4px 14px rgba(0, 0, 0, 0.04);
        }

        .card-head {
          display: flex;
          align-items: center;
          justify-content: space-between;
          margin-bottom: 10px;

          .badge-row {
            display: flex;
            align-items: center;
            gap: 8px;

            .kp-code {
              font-size: 11px;
              color: #64748b;
              background: #f1f5f9;
              padding: 2px 6px;
              border-radius: 4px;
            }
          }

          .star-rating {
            color: #f59e0b;
            font-size: 14px;
          }
        }

        .kp-name {
          font-size: 16px;
          font-weight: 700;
          color: #0f172a;
          margin: 0 0 8px;
          line-height: 1.4;
        }

        .kp-desc {
          font-size: 13px;
          color: #64748b;
          line-height: 1.6;
          margin: 0 0 14px;
          display: -webkit-box;
          -webkit-line-clamp: 3;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }

        .prereq-row {
          display: flex;
          align-items: center;
          gap: 6px;
          margin-bottom: 12px;
          font-size: 12px;

          .prereq-label {
            color: #94a3b8;
          }

          .prereq-tags {
            display: flex;
            gap: 4px;

            .pre-tag {
              background: #eff6ff;
              color: #2563eb;
              padding: 1px 6px;
              border-radius: 4px;
            }
          }
        }

        .card-footer {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding-top: 12px;
          border-top: 1px solid #f1f5f9;

          .chapter-hint {
            font-size: 12px;
            color: #94a3b8;
          }
        }
      }
    }

    .empty-state-panel {
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 14px;
      padding: 60px 24px;
      text-align: center;

      .empty-icon {
        font-size: 44px;
        margin-bottom: 12px;
      }

      h3 {
        font-size: 18px;
        font-weight: 700;
        color: #1e293b;
        margin-bottom: 6px;
      }

      p {
        font-size: 14px;
        color: #64748b;
      }
    }
  }
}
</style>
