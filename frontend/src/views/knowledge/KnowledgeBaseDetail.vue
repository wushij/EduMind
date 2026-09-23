<template>
  <div class="kb-detail-container">
    <div v-loading="loading" class="kb-hero-header">
      <div class="glow-orb glow-orb--left"></div>
      <div class="glow-orb glow-orb--right"></div>

      <div class="hero-top-toolbar">
        <div class="header-nav-bar">
          <button type="button" class="back-btn" @click="router.push('/knowledge')">
            <el-icon><ArrowLeft /></el-icon>
            <span>返回知识库列表</span>
          </button>
          <el-divider direction="vertical" class="nav-divider" />
          <el-breadcrumb separator="/" class="header-breadcrumb">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item :to="{ path: '/knowledge' }">知识库中心</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentKnowledgeBase?.name || '知识库详情' }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="toolbar-right-actions">
          <button type="button" class="capsule-btn capsule-btn--primary" @click="scrollToUpload">
            <svg viewBox="0 0 24 24" class="btn-icon-svg" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
              <polyline points="17 8 12 3 7 8"></polyline>
              <line x1="12" y1="3" x2="12" y2="15"></line>
            </svg>
            <span>上传课件文档</span>
          </button>
        </div>
      </div>

      <div class="hero-main-row">
        <div class="main-info-col">
          <div class="title-status-line">
            <h1 v-if="currentKnowledgeBase" class="hero-kb-title">{{ currentKnowledgeBase.name }}</h1>
            <div v-else-if="loading" class="hero-title-skeleton"></div>
            <h1 v-else class="hero-kb-title hero-kb-title--muted">知识库详情</h1>

            <span v-if="currentKnowledgeBase?.categoryLabel" class="category-badge">
              {{ currentKnowledgeBase.categoryLabel }}
            </span>
            <span class="status-pill-badge" :class="statusClass">
              <span class="pulse-dot"></span>
              <span>{{ statusText }}</span>
            </span>

            <!-- 切换课程/知识库快捷下拉菜单 -->
            <el-dropdown
              v-if="kbOptions.length > 1"
              trigger="click"
              class="course-switch-dropdown"
              :max-height="340"
              popper-class="kb-switch-popper"
              @command="handleSwitchKb"
            >
              <button type="button" class="course-switch-pill" title="快速切换到其他课程知识库">
                <el-icon><Switch /></el-icon>
                <span>切换课程</span>
                <el-icon class="arrow-down"><ArrowDown /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu class="course-dropdown-menu">
                  <div class="dropdown-header-tip">切换至其他课程知识库</div>
                  <el-dropdown-item
                    v-for="kb in sortedKbOptions"
                    :key="kb.id"
                    :command="kb.id"
                    :class="{ 'is-selected': Number(kb.id) === Number(kbId) }"
                  >
                    <div class="kb-switch-item">
                      <div class="kb-switch-main">
                        <span class="kb-title-text">{{ kb.name }}</span>
                        <span v-if="kb.courseName" class="kb-course-pill">关联课程：{{ kb.courseName }}</span>
                      </div>
                      <span v-if="Number(kb.id) === Number(kbId)" class="current-check">当前</span>
                    </div>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>

          <p class="hero-description">
            {{ currentKnowledgeBase?.description || '暂无描述，可在知识库设置中补充说明。' }}
          </p>

          <div class="hero-meta-badges">
            <div v-if="currentKnowledgeBase?.courseName" class="meta-badge-item">
              <svg viewBox="0 0 24 24" class="meta-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
              </svg>
              <span class="meta-label">关联课程：</span>
              <strong class="meta-value">{{ currentKnowledgeBase.courseName }}</strong>
            </div>

            <div class="meta-badge-item">
              <svg viewBox="0 0 24 24" class="meta-svg" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
              </svg>
              <span class="meta-label">入库文档：</span>
              <strong class="meta-value">{{ currentKnowledgeBase?.documentCount ?? 0 }} 篇</strong>
            </div>

            <div class="meta-badge-item">
              <svg viewBox="0 0 24 24" class="meta-svg" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="7" height="7"></rect>
                <rect x="14" y="3" width="7" height="7"></rect>
                <rect x="14" y="14" width="7" height="7"></rect>
                <rect x="3" y="14" width="7" height="7"></rect>
              </svg>
              <span class="meta-label">向量切片：</span>
              <strong class="meta-value">{{ currentKnowledgeBase?.chunkCount ?? 0 }} 个</strong>
            </div>
          </div>
        </div>
      </div>

      <div class="hero-stats-row">
        <div class="hero-stat-card">
          <span class="stat-num text-primary">{{ currentKnowledgeBase?.documentCount ?? 0 }}</span>
          <span class="stat-label">文档总数</span>
        </div>
        <div class="hero-stat-card">
          <span class="stat-num text-success">{{ currentKnowledgeBase?.chunkCount ?? 0 }}</span>
          <span class="stat-label">切片总量</span>
        </div>
        <div class="hero-stat-card">
          <span class="stat-num text-info">{{ currentKnowledgeBase?.vectorStatusLabel || '待处理' }}</span>
          <span class="stat-label">向量索引状态</span>
        </div>
        <div class="hero-stat-card">
          <span class="stat-num text-warning" :title="currentKnowledgeBase?.embeddingModel || '未配置向量模型'">
            {{ currentKnowledgeBase?.embeddingModel || '未配置' }}
          </span>
          <span class="stat-label">Embedding 模型</span>
        </div>
      </div>
    </div>

    <!--
      未接入真实向量模型时必须显式提示：
      否则「已向量化」会被误读成语义检索可用，而实际向量是本地哈希生成的伪向量。
    -->
    <el-alert
      v-if="currentKnowledgeBase?.embeddingMocked"
      class="embedding-mock-alert"
      type="warning"
      effect="light"
      show-icon
      :closable="false"
      title="当前向量为 Mock 伪向量，检索结果不可信"
    >
      <div class="embedding-mock-body">
        <span>
          平台没有可用的 Embedding 模型配置，切片向量由本地哈希生成、不具备语义（当前标识：
          {{ currentKnowledgeBase?.embeddingModel || '未知' }}）。请配置向量模型后重新执行「解析 → 切片 → 向量化」。
        </span>
        <router-link class="embedding-mock-link" to="/system/models">前往配置向量模型</router-link>
      </div>
    </el-alert>

    <div class="kb-subview-content">
      <router-view v-if="currentKnowledgeBase" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft, ArrowDown, Switch } from '@element-plus/icons-vue';
import { getKnowledgeBases } from '@/api/knowledge/knowledge-base';
import type { KnowledgeBase } from '@/types/knowledge/knowledge-base';
import { useKnowledgeBase } from '@/composables/knowledge/useKnowledgeBase';
import { setStoredKnowledgeBaseId, clearStoredKnowledgeBaseId, useKnowledgeRoute } from '@/composables/knowledge/useKnowledgeRoute';

const route = useRoute();
const router = useRouter();
const { kbId } = useKnowledgeRoute();
const { currentKnowledgeBase, loading, fetchKnowledgeBaseDetail } = useKnowledgeBase();

const kbOptions = ref<KnowledgeBase[]>([]);

async function loadKbOptions() {
  try {
    const res = await getKnowledgeBases();
    kbOptions.value = Array.isArray(res?.data) ? res.data : [];
  } catch {
    kbOptions.value = [];
  }
}

/**
 * 下拉列表顺序：当前所在知识库置顶，其余按名称（中文拼音）升序。
 * 直接用后端返回顺序会出现「当前项埋在末尾、其余看似随机」的观感。
 */
const sortedKbOptions = computed(() => {
  const currentId = Number(kbId.value);
  return [...kbOptions.value].sort((a, b) => {
    const aCurrent = Number(a.id) === currentId;
    const bCurrent = Number(b.id) === currentId;
    if (aCurrent !== bCurrent) return aCurrent ? -1 : 1;
    return (a.name || '').localeCompare(b.name || '', 'zh-Hans-CN');
  });
});

function handleSwitchKb(targetKbId: number) {
  if (!targetKbId || targetKbId === kbId.value) return;
  setStoredKnowledgeBaseId(targetKbId);
  const currentSubPath = route.path.replace(/^\/knowledge\/\d+/, '');
  const targetPath = `/knowledge/${targetKbId}${currentSubPath || '/documents'}`;
  router.push({ path: targetPath, query: route.query });
}

const statusClass = computed(() => {
  const status = currentKnowledgeBase.value?.vectorStatus;
  if (status === 'SYNCED') return 'status-pill-badge--success';
  if (status === 'PARSING') return 'status-pill-badge--warning';
  return 'status-pill-badge--default';
});

const statusText = computed(() => {
  const kb = currentKnowledgeBase.value;
  if (!kb) return '加载中';
  const label = kb.vectorStatusLabel || '待处理';
  return `${label} · ${kb.documentCount} 个文档`;
});

async function loadKnowledgeBase() {
  const id = kbId.value;
  if (!id) {
    router.replace('/knowledge');
    return;
  }
  try {
    const kb = await fetchKnowledgeBaseDetail(id);
    if (!kb) {
      clearStoredKnowledgeBaseId();
      ElMessage.warning('知识库不存在或已被移除，正在返回知识库列表...');
      router.replace('/knowledge');
      return;
    }
    setStoredKnowledgeBaseId(id);
  } catch {
    clearStoredKnowledgeBaseId();
    ElMessage.warning('知识库不存在或已被移除，正在返回知识库列表...');
    router.replace('/knowledge');
  }
}

function scrollToUpload() {
  const anchor = document.getElementById('kb-doc-upload');
  if (anchor) {
    anchor.scrollIntoView({ behavior: 'smooth', block: 'center' });
    return;
  }
  const id = kbId.value;
  if (id) {
    router.push({ path: `/knowledge/${id}/documents`, query: { action: 'upload' } });
  }
}

onMounted(() => {
  loadKnowledgeBase();
  loadKbOptions();
});

watch(kbId, (id) => {
  if (id) {
    loadKnowledgeBase();
  }
});
</script>

<style scoped lang="scss">
.kb-detail-container {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.kb-hero-header {
  position: relative;
  overflow: hidden;
  border-radius: 16px;
  padding: 18px 24px 16px;
  background: linear-gradient(135deg, #f8fbff 0%, #eef6ff 45%, #f5f3ff 100%);
  border: 1px solid rgba(191, 219, 254, 0.65);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);

  .glow-orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(40px);
    pointer-events: none;

    &--left {
      width: 180px;
      height: 180px;
      top: -60px;
      left: -40px;
      background: rgba(59, 130, 246, 0.18);
    }

    &--right {
      width: 160px;
      height: 160px;
      top: -30px;
      right: -20px;
      background: rgba(139, 92, 246, 0.12);
    }
  }

  .hero-top-toolbar {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding-bottom: 14px;
    border-bottom: 1px solid rgba(226, 232, 240, 0.7);
    margin-bottom: 18px;

    .header-nav-bar {
      display: flex;
      align-items: center;
      gap: 12px;
      flex: 1;

      .back-btn {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 0;
        border: none;
        background: transparent;
        font-size: 13.5px;
        font-weight: 600;
        color: #2563eb;
        cursor: pointer;
        white-space: nowrap;
        transition: color 0.15s ease;

        .el-icon {
          font-size: 14px;
        }

        &:hover {
          color: #1d4ed8;
        }
      }

      .nav-divider {
        margin: 0 4px;
        height: 14px;
        border-color: #cbd5e1;
      }

      .header-breadcrumb {
        :deep(.el-breadcrumb__inner) {
          font-size: 13px;
          color: #64748b;
        }

        :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
          color: #1e293b;
          font-weight: 600;
        }
      }
    }
  }

  .hero-main-row {
    position: relative;
    z-index: 1;
  }

  .title-status-line {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
    margin-bottom: 10px;
  }

  .hero-kb-title {
    margin: 0;
    font-size: 26px;
    font-weight: 700;
    color: #0f172a;
    letter-spacing: -0.02em;

    &--muted {
      color: #64748b;
    }
  }

  .hero-title-skeleton {
    width: 280px;
    height: 32px;
    border-radius: 8px;
    background: linear-gradient(90deg, #e2e8f0 25%, #f1f5f9 50%, #e2e8f0 75%);
    background-size: 200% 100%;
    animation: shimmer 1.2s infinite;
  }

  .category-badge {
    height: 26px;
    padding: 0 10px;
    border-radius: 9999px;
    background: rgba(255, 255, 255, 0.9);
    border: 1px solid #dbeafe;
    color: #2563eb;
    font-size: 12px;
    font-weight: 600;
  }

  .status-pill-badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    height: 26px;
    padding: 0 12px;
    border-radius: 9999px;
    font-size: 12px;
    font-weight: 600;
    border: 1px solid transparent;

    .pulse-dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: currentColor;
    }

    &--success {
      background: #ecfdf5;
      border-color: #a7f3d0;
      color: #059669;
    }

    &--warning {
      background: #fffbeb;
      border-color: #fde68a;
      color: #d97706;
    }

    &--default {
      background: #f8fafc;
      border-color: #e2e8f0;
      color: #64748b;
    }
  }

  .course-switch-pill {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    height: 26px;
    padding: 0 12px;
    border-radius: 9999px;
    font-size: 12px;
    font-weight: 600;
    color: #2563eb;
    background: #eff6ff;
    border: 1px solid #bfdbfe;
    cursor: pointer;
    transition: all 0.2s ease;
    box-shadow: 0 2px 6px rgba(37, 99, 235, 0.08);

    &:hover {
      background: #2563eb;
      color: #ffffff;
      border-color: #2563eb;
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.22);
    }

    .arrow-down {
      font-size: 11px;
      transition: transform 0.2s ease;
    }
  }

  .hero-description {
    margin: 0 0 12px;
    font-size: 14px;
    line-height: 1.6;
    color: #64748b;
    max-width: 820px;
  }

  .hero-meta-badges {
    display: flex;
    align-items: center;
    gap: 16px;
    flex-wrap: wrap;

    .meta-badge-item {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      font-size: 13px;
      color: #64748b;

      .meta-svg {
        width: 14px;
        height: 14px;
        color: #94a3b8;
      }

      .meta-value {
        color: #1e293b;
        font-weight: 600;
      }
    }
  }

  .hero-stats-row {
    position: relative;
    z-index: 1;
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 12px;
    margin-top: 16px;
    padding-top: 14px;
    border-top: 1px solid rgba(226, 232, 240, 0.8);
  }

  .hero-stat-card {
    background: rgba(255, 255, 255, 0.82);
    border: 1px solid rgba(226, 232, 240, 0.9);
    border-radius: 12px;
    padding: 10px 14px;
    display: flex;
    flex-direction: column;
    gap: 4px;

    .stat-num {
      font-size: 18px;
      font-weight: 700;
      line-height: 1.2;

      &.text-primary { color: #2563eb; }
      &.text-success { color: #059669; }
      &.text-info { color: #0891b2; }
      &.text-warning { color: #d97706; }
    }

    .stat-label {
      font-size: 12px;
      color: #64748b;
    }
  }
}

.capsule-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 22px;
  border-radius: 9999px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
  white-space: nowrap;

  .btn-icon-svg {
    width: 16px;
    height: 16px;
    flex-shrink: 0;
  }

  &--primary {
    background: #1677ff;
    color: #ffffff;
    border: none;
    box-shadow: 0 2px 8px rgba(22, 119, 255, 0.25);

    &:hover {
      background: #4096ff;
      transform: translateY(-1px);
      box-shadow: 0 6px 16px rgba(22, 119, 255, 0.35);
    }
  }

  &--default {
    height: 34px;
    padding: 0 16px;
    font-size: 13px;
    background: rgba(255, 255, 255, 0.88);
    backdrop-filter: blur(8px);
    color: #334155;
    border: 1px solid rgba(226, 232, 240, 0.9);
    box-shadow: 0 2px 6px rgba(15, 23, 42, 0.03);

    &:hover {
      border-color: #bfdbfe;
      color: #1677ff;
      background: #ffffff;
      transform: translateY(-1px);
    }
  }
}

.kb-subview-content {
  min-height: 400px;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

@media (max-width: 960px) {
  .kb-hero-header .hero-stats-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

/* Mock 伪向量警示条：与 hero 卡片保持同宽、同圆角语言 */
.embedding-mock-alert {
  margin-top: 12px;
  border-radius: 12px;

  .embedding-mock-body {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
    font-size: 12.5px;
    line-height: 1.6;
  }

  .embedding-mock-link {
    color: #b45309;
    font-weight: 600;
    text-decoration: underline;
    white-space: nowrap;
  }
}

</style>

<style lang="scss">
/**
 * 课程切换下拉：弹层被 teleport 到 body，scoped 的 :deep() 编译后要求存在带 scope-id 的祖先，
 * 在 body 下永远匹配不到，因此下面这些样式此前从未生效（标题与关联课程挤成一行、当前标记被迫换行、悬停色丢失）。
 * 改为全局样式，并用 popper-class 限定作用域，避免影响同样使用 .course-dropdown-menu 的其他下拉。
 */
.kb-switch-popper.el-popper {
  border-radius: 12px;
  border: 1px solid #E2E8F0;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.12);
  // 内边距放在弹层上，让每个条目成为带圆角的独立块
  padding: 6px;
}

.kb-switch-popper .course-dropdown-menu {
  min-width: 320px;
  // 高度由 el-dropdown 的 max-height（内置 el-scrollbar）控制，此处不再自建 overflow
  padding: 0;
}

.kb-switch-popper .dropdown-header-tip {
  font-size: 11px;
  font-weight: 600;
  color: #94A3B8;
  letter-spacing: 0.02em;
  padding: 4px 10px 6px;
}

.kb-switch-popper .el-dropdown-menu__item {
  border-radius: 8px;
  padding: 8px 10px;
  margin-bottom: 2px;
  line-height: 1.45;

  &:last-child {
    margin-bottom: 0;
  }

  &.is-selected {
    background: #EFF6FF;

    .kb-title-text {
      color: #2563EB;
      font-weight: 600;
    }
  }

  &:hover,
  &:focus {
    background: #F1F5F9;
  }
}

.kb-switch-popper .kb-switch-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.kb-switch-popper .kb-switch-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.kb-switch-popper .kb-title-text {
  font-size: 13px;
  color: #1E293B;
  line-height: 1.3;
}

.kb-switch-popper .kb-course-pill {
  font-size: 11px;
  color: #64748B;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kb-switch-popper .current-check {
  flex-shrink: 0;
  font-size: 11px;
  color: #2563EB;
  background: #DBEAFE;
  padding: 1px 6px;
  border-radius: 4px;
  white-space: nowrap;
}
</style>
