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
        <el-button class="capsule-secondary-btn" @click="openGraphDrawer(null)">
          <el-icon><Connection /></el-icon>
          <span>查看课程全景知识拓扑</span>
        </el-button>
        <el-button type="primary" :icon="Plus" class="capsule-primary-btn" @click="showCreateDrawer = true">
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
              <el-button type="primary" link size="small" @click="openGraphDrawer(kp)">
                关联图谱
              </el-button>
              <el-button type="primary" link size="small" @click="handleAskAi(kp)">
                AI解析
              </el-button>
              <el-popconfirm title="确定删除此知识点吗？" @confirm="handleDeleteKp(kp)">
                <template #reference>
                  <el-button type="danger" link size="small">删除</el-button>
                </template>
              </el-popconfirm>
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

    <!-- 知识拓扑全景抽屉 -->
    <el-drawer
      v-model="showGraphDrawer"
      title="课程核心知识拓扑全景图谱"
      size="680px"
      destroy-on-close
    >
      <div class="graph-drawer-content">
        <div class="graph-info-header">
          <span class="badge-pill">拓扑节点总数：{{ knowledgePoints.length + chapters.length + 1 }}</span>
          <span class="badge-pill">大纲章节：{{ chapters.length }}</span>
          <span class="badge-pill">核心考点：{{ knowledgePoints.length }}</span>
        </div>

        <!-- 交互式拓扑图模拟树 -->
        <div class="topology-tree-container">
          <!-- 根节点：课程核心 -->
          <div class="topo-root-node">
            <div class="topo-node-card is-root">
              <el-icon><Connection /></el-icon>
              <span>课程知识根基空间</span>
            </div>
          </div>

          <!-- 章节分支 -->
          <div class="topo-branches">
            <div
              v-for="chap in chapters"
              :key="chap.id"
              class="topo-branch-column"
            >
              <div class="topo-chapter-node">
                <span class="chapter-tag">{{ chap.title }}</span>
              </div>
              <div class="topo-kp-leaves">
                <div
                  v-for="point in getPointsForChapter(chap.id)"
                  :key="point.id"
                  class="topo-kp-leaf"
                  :class="{ active: selectedGraphKp?.id === point.id }"
                  @click="selectedGraphKp = point"
                >
                  <span class="leaf-dot"></span>
                  <span class="leaf-title">{{ point.title }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 当前选中知识点详情面板 -->
        <div v-if="selectedGraphKp" class="active-node-panel">
          <div class="active-header">
            <h4>{{ selectedGraphKp.title }}</h4>
            <el-tag size="small" :type="getLevelTagType(selectedGraphKp.cognitiveDimension)">
              {{ getLevelLabel(selectedGraphKp.cognitiveDimension) }}
            </el-tag>
          </div>
          <p class="active-desc">
            {{ selectedGraphKp.description || '该知识点为所在章节的核心考查要点，直接关联课程期末诊断与作业训练。' }}
          </p>
          <div class="active-actions">
            <button
              type="button"
              class="capsule-btn-action capsule-btn-action--ai"
              @click="handleAskAi(selectedGraphKp)"
            >
              向 AI 助教提问此考点
            </button>
            <button
              type="button"
              class="capsule-btn-action capsule-btn-action--quiz"
              @click="handleGenerateQuizForKp(selectedGraphKp)"
            >
              针对该考点出题练习
            </button>
          </div>
        </div>
      </div>
    </el-drawer>

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
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Search, Plus, StarFilled, Opportunity, Connection } from '@element-plus/icons-vue';
import { getCourseKnowledgePoints, createKnowledgePoint, deleteKnowledgePoint } from '@/api/course/knowledge-point';
import { getChapters } from '@/api/course/chapter';

const route = useRoute();
const router = useRouter();
const courseId = computed(() => Number(route.params.id) || 101);

const loading = ref(false);
const creating = ref(false);
const showCreateDrawer = ref(false);
const showGraphDrawer = ref(false);
const selectedGraphKp = ref<any>(null);

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

function getPointsForChapter(chapterId: number) {
  return knowledgePoints.value.filter(k => k.chapterId === chapterId || (!k.chapterId && chapterId === 1));
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

function openGraphDrawer(kp: any) {
  selectedGraphKp.value = kp || knowledgePoints.value[0] || null;
  showGraphDrawer.value = true;
}

function handleAskAi(kp: any) {
  router.push({
    path: `/course/${courseId.value}/ai`,
    query: { prompt: `请结合本课程知识图谱，详细讲解核心考点【${kp.title || kp.name}】的定义、推导与常见考查题型。` }
  });
}

function handleGenerateQuizForKp(kp: any) {
  router.push(`/ai/question/generate?courseId=${courseId.value}&kp=${encodeURIComponent(kp.title || kp.name)}`);
}

async function handleDeleteKp(kp: any) {
  try {
    await deleteKnowledgePoint(courseId.value, kp.id);
    ElMessage.success(`知识点【${kp.title || kp.name}】已成功删除`);
    await loadKnowledgePoints();
  } catch (err: any) {
    ElMessage.error(err?.message || '删除知识点失败');
  }
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

.capsule-secondary-btn {
  border-radius: 9999px !important;
  background: #F1F5F9 !important;
  color: #334155 !important;
  border: 1px solid #E2E8F0 !important;

  &:hover {
    background: #EAF3FF !important;
    color: #1677FF !important;
    border-color: #BFDBFE !important;
  }
}

.capsule-primary-btn {
  border-radius: 9999px !important;
}

// 知识拓扑抽屉样式
.graph-drawer-content {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .graph-info-header {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;

    .badge-pill {
      padding: 4px 12px;
      border-radius: 9999px;
      background: #F1F5F9;
      color: #475569;
      font-size: 12px;
      font-weight: 500;
    }
  }

  .topology-tree-container {
    background: #0F172A;
    border-radius: 18px;
    padding: 24px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 20px;
    min-height: 360px;
    box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.4);

    .topo-root-node {
      .topo-node-card.is-root {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        padding: 8px 20px;
        border-radius: 9999px;
        background: linear-gradient(135deg, #1677FF 0%, #722ED1 100%);
        color: #FFFFFF;
        font-size: 13.5px;
        font-weight: 600;
        box-shadow: 0 4px 14px rgba(22, 119, 255, 0.4);
      }
    }

    .topo-branches {
      display: flex;
      flex-wrap: wrap;
      justify-content: center;
      gap: 16px;
      width: 100%;

      .topo-branch-column {
        background: rgba(255, 255, 255, 0.05);
        border: 1px solid rgba(255, 255, 255, 0.1);
        border-radius: 14px;
        padding: 12px 14px;
        display: flex;
        flex-direction: column;
        gap: 10px;
        min-width: 180px;
        max-width: 260px;
        flex: 1;

        .topo-chapter-node {
          .chapter-tag {
            font-size: 12px;
            font-weight: 600;
            color: #93C5FD;
            display: block;
            border-bottom: 1px solid rgba(255, 255, 255, 0.08);
            padding-bottom: 6px;
          }
        }

        .topo-kp-leaves {
          display: flex;
          flex-direction: column;
          gap: 6px;

          .topo-kp-leaf {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 6px 10px;
            border-radius: 8px;
            background: rgba(255, 255, 255, 0.04);
            cursor: pointer;
            transition: all 0.2s ease;

            .leaf-dot {
              width: 6px;
              height: 6px;
              border-radius: 50%;
              background: #60A5FA;
              flex-shrink: 0;
            }

            .leaf-title {
              font-size: 12px;
              color: #E2E8F0;
              line-height: 1.3;
            }

            &:hover {
              background: rgba(22, 119, 255, 0.25);
            }

            &.active {
              background: #1677FF;
              box-shadow: 0 2px 10px rgba(22, 119, 255, 0.5);

              .leaf-dot {
                background: #FFFFFF;
              }

              .leaf-title {
                color: #FFFFFF;
                font-weight: 600;
              }
            }
          }
        }
      }
    }
  }

  .active-node-panel {
    background: #F8FAFC;
    border: 1px solid #E2E8F0;
    border-radius: 14px;
    padding: 16px 20px;
    display: flex;
    flex-direction: column;
    gap: 10px;

    .active-header {
      display: flex;
      align-items: center;
      justify-content: space-between;

      h4 {
        margin: 0;
        font-size: 15px;
        font-weight: 700;
        color: #0F172A;
      }
    }

    .active-desc {
      margin: 0;
      font-size: 13px;
      color: #64748B;
      line-height: 1.6;
    }

    .active-actions {
      display: flex;
      gap: 10px;
      margin-top: 6px;

      .capsule-btn-action {
        height: 34px;
        padding: 0 16px;
        border-radius: 9999px;
        font-size: 12.5px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s ease;
        border: none;

        &--ai {
          background: #1677FF;
          color: #FFFFFF;
          &:hover { background: #4096FF; }
        }

        &--quiz {
          background: #EEF2FF;
          color: #4F46E5;
          &:hover { background: #E0E7FF; }
        }
      }
    }
  }
}
</style>
