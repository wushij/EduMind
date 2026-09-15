<template>
  <div class="tool-edit-page gb-fade-in">
    <!-- 顶部导航与操作栏 -->
    <div class="edit-header-card">
      <div class="header-left">
        <el-button round class="back-pill-btn" :icon="ArrowLeft" @click="router.push('/system/tools')">
          返回工具中枢
        </el-button>
        <div class="header-titles">
          <div class="eyebrow-row">
            <span class="eyebrow-tag">EduMind AI Compute</span>
            <span class="eyebrow-slash">/</span>
            <span class="eyebrow-text">{{ isEdit ? '编辑教学工具' : '新建教学工具' }}</span>
          </div>
          <h1 class="page-title">{{ isEdit ? `编辑：${form.name || '教学工具'}` : '新建 AI 教学工具' }}</h1>
          <p class="page-subtitle">配置工具的基础信息、适用分类、大模型及跳转路由，右侧可实时同步预览卡片效果</p>
        </div>
      </div>

      <div class="header-actions">
        <el-button round class="action-btn-save" :loading="saving" @click="handleSave(false)">
          保存配置
        </el-button>
        <el-button type="primary" round class="action-btn-publish" :loading="saving" @click="handleSave(true)">
          <el-icon><Check /></el-icon>
          {{ form.status === 1 ? '保存并保持上架' : '保存并发布到广场' }}
        </el-button>
      </div>
    </div>

    <!-- 主表单与右侧实时预览分栏 -->
    <div v-loading="loading" class="edit-layout">
      <!-- 左侧表单主体 -->
      <div class="form-panel">
        <el-card shadow="never" class="form-card">
          <ToolFormFields
            ref="formRef"
            v-model="form"
            :is-edit="isEdit"
            :show-status="true"
          />
        </el-card>
      </div>

      <!-- 右侧实时预览 -->
      <div class="preview-panel">
        <div class="preview-sticky-wrap">
          <el-card shadow="never" class="preview-card">
            <template #header>
              <div class="preview-card-header">
                <div class="preview-title-wrap">
                  <span class="preview-badge-dot" />
                  <span class="preview-title">广场卡片实时预览</span>
                </div>
                <div class="preview-status-pill" :class="form.status === 1 ? 'online' : 'offline'">
                  <span class="status-pulse-dot" />
                  <span>{{ form.status === 1 ? '上架公开' : '下架隐藏' }}</span>
                </div>
              </div>
            </template>

            <!-- 卡片渲染舞台 -->
            <div class="preview-stage">
              <AIToolCard
                v-if="previewTool"
                :tool="previewTool"
                @open-detail="() => {}"
              />
              <div v-else class="preview-empty">
                暂无可预览数据
              </div>
            </div>

            <!-- 参数详情预览面板 -->
            <div v-if="previewTool" class="detail-preview-panel">
              <div class="panel-section-title">
                <span>详情介绍摘要</span>
                <span class="live-tag">LIVE SYNC</span>
              </div>
              <p class="intro-preview-text">
                {{ previewTool.detailedIntro || previewTool.description || '暂无详细介绍，可在左侧表单补充...' }}
              </p>

              <div class="meta-pills-list">
                <div class="meta-pill-item">
                  <span class="meta-k">模式</span>
                  <span class="meta-v">{{ form.executionMode }}</span>
                </div>
                <div class="meta-pill-item">
                  <span class="meta-k">路由</span>
                  <span class="meta-v text-ellipsis" :title="form.route || '-'">
                    {{ form.route || '-' }}
                  </span>
                </div>
                <div class="meta-pill-item">
                  <span class="meta-k">模型</span>
                  <span class="meta-v">{{ form.modelId || '平台默认' }}</span>
                </div>
                <div class="meta-pill-item">
                  <span class="meta-k">排序权重</span>
                  <span class="meta-v">{{ form.sortOrder ?? 0 }}</span>
                </div>
              </div>

              <div class="sync-tip-box">
                <el-icon color="#3b82f6"><InfoFilled /></el-icon>
                <span>左侧编辑任何字段时，右侧卡片与详情摘要将实时双向响应更新。</span>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowLeft, Check, InfoFilled } from '@element-plus/icons-vue';
import ToolFormFields from '@/components/system/ai-tool/ToolFormFields.vue';
import AIToolCard from '@/components/ai/AIToolCard.vue';
import { useAIToolManage } from '@/composables/system/useAIToolManage';
import type { AIToolSaveRequest } from '@/types/system/tool';

const route = useRoute();
const router = useRouter();
const formRef = ref<InstanceType<typeof ToolFormFields>>();
const toolId = computed(() => route.params.id as string | undefined);
const isEdit = computed(() => Boolean(toolId.value));

const {
  loading,
  saving,
  previewTool,
  currentTool,
  loadTool,
  handleCreate,
  handleUpdate,
  handlePublish,
  setPreviewFromForm
} = useAIToolManage();

const defaultForm = (): Partial<AIToolSaveRequest> => ({
  id: '',
  name: '',
  description: '',
  detailedIntro: '',
  category: 'GENERAL',
  icon: 'MagicStick',
  modelId: '',
  route: '',
  executionMode: 'ROUTE',
  tags: '',
  isRecommended: false,
  isHot: false,
  sortOrder: 0,
  status: 0
});

const form = ref<Partial<AIToolSaveRequest>>(defaultForm());

watch(form, () => setPreviewFromForm(form.value as any), { deep: true });

async function load() {
  if (!toolId.value) {
    form.value = defaultForm();
    setPreviewFromForm(form.value as any);
    return;
  }
  await loadTool(toolId.value);
  const admin = currentTool.value;
  if (!admin) return;
  form.value = {
    id: admin.id,
    name: admin.name,
    description: admin.description,
    detailedIntro: admin.detailedIntro,
    category: admin.category,
    icon: admin.icon || 'MagicStick',
    modelId: admin.modelId,
    route: admin.route,
    executionMode: admin.executionMode,
    tags: admin.tags || '',
    isRecommended: admin.isRecommended,
    isHot: admin.isHot,
    sortOrder: admin.sortOrder,
    status: admin.status
  };
  setPreviewFromForm({ ...form.value, useCount: admin.useCount });
}

async function handleSave(publishAfter = false) {
  const valid = await formRef.value?.validate();
  if (!valid) return;

  const payload = {
    name: form.value.name!,
    description: form.value.description,
    detailedIntro: form.value.detailedIntro,
    category: form.value.category!,
    icon: form.value.icon,
    modelId: form.value.modelId || undefined,
    route: form.value.route,
    executionMode: form.value.executionMode,
    tags: form.value.tags,
    isRecommended: form.value.isRecommended,
    isHot: form.value.isHot,
    sortOrder: form.value.sortOrder,
    status: publishAfter ? 1 : form.value.status
  };

  if (isEdit.value && toolId.value) {
    const ok = await handleUpdate(toolId.value, payload);
    if (!ok) return;
    if (publishAfter) {
      await handlePublish(toolId.value);
    }
    router.push('/system/tools');
    return;
  }

  const id = await handleCreate({
    id: form.value.id!,
    ...payload
  });
  if (!id) return;
  if (publishAfter) {
    await handlePublish(id);
  }
  router.push('/system/tools');
}

onMounted(load);
</script>

<style scoped lang="scss">
.tool-edit-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 20px;
  background: #f8fafc;
  min-height: calc(100vh - 64px);
}

/* 顶部导航与操作栏卡片 */
.edit-header-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 18px 24px;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
  flex-wrap: wrap;
}

.header-left {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
}

.back-pill-btn {
  height: 34px !important;
  padding: 6px 14px !important;
  border-radius: 9999px !important;
  border: 1px solid #e2e8f0 !important;
  background: #ffffff !important;
  color: #475569 !important;
  font-weight: 500 !important;
  font-size: 13px !important;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03) !important;
  transition: all 0.2s ease !important;

  &:hover {
    border-color: #93c5fd !important;
    color: #2563eb !important;
    background: #eff6ff !important;
    transform: translateY(-1px);
  }
}

.header-titles {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.eyebrow-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;

  .eyebrow-tag {
    font-weight: 600;
    color: #2563eb;
    background: #eff6ff;
    padding: 2px 8px;
    border-radius: 999px;
  }

  .eyebrow-slash {
    color: #cbd5e1;
  }

  .eyebrow-text {
    color: #64748b;
  }
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.3px;
}

.page-subtitle {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-btn-save {
  height: 36px !important;
  padding: 8px 18px !important;
  border-radius: 9999px !important;
  font-weight: 500 !important;
  color: #475569 !important;
  border: 1px solid #cbd5e1 !important;
  background: #ffffff !important;

  &:hover {
    color: #2563eb !important;
    border-color: #93c5fd !important;
    background: #eff6ff !important;
  }
}

.action-btn-publish {
  height: 36px !important;
  padding: 8px 20px !important;
  border-radius: 9999px !important;
  font-weight: 600 !important;
  background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%) !important;
  border: none !important;
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.28) !important;
  transition: all 0.2s ease !important;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 16px rgba(22, 119, 255, 0.35) !important;
  }
}

/* 分栏布局 */
.edit-layout {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 20px;
  align-items: start;
}

.form-card {
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);

  :deep(.el-card__body) {
    padding: 24px;
  }
}

/* 右侧悬浮预览卡片 */
.preview-sticky-wrap {
  position: sticky;
  top: 20px;
}

.preview-card {
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  background: #ffffff;
  overflow: hidden;

  :deep(.el-card__header) {
    padding: 14px 20px;
    border-bottom: 1px solid #f1f5f9;
  }

  :deep(.el-card__body) {
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
}

.preview-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.preview-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;

  .preview-badge-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #2563eb;
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.2);
  }

  .preview-title {
    font-size: 14px;
    font-weight: 700;
    color: #0f172a;
  }
}

.preview-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;

  &.online {
    color: #059669;
    background: #ecfdf5;
    border: 1px solid #a7f3d0;

    .status-pulse-dot {
      background: #10b981;
      box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.3);
    }
  }

  &.offline {
    color: #64748b;
    background: #f1f5f9;
    border: 1px solid #e2e8f0;

    .status-pulse-dot {
      background: #94a3b8;
    }
  }

  .status-pulse-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
  }
}

/* 预览舞台：仿广场卡片真实容器 */
.preview-stage {
  padding: 16px;
  border-radius: 14px;
  background: linear-gradient(180deg, #f8fafc 0%, #f1f5f9 100%);
  border: 1px dashed #cbd5e1;
  display: flex;
  justify-content: center;

  :deep(.ai-tool-card) {
    width: 100%;
    max-width: 100%;
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.06);
    background: #ffffff;
  }
}

.preview-empty {
  padding: 32px 0;
  color: #94a3b8;
  font-size: 13px;
  text-align: center;
}

/* 参数与摘要面板 */
.detail-preview-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;

  .panel-section-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 13px;
    font-weight: 700;
    color: #1e293b;

    .live-tag {
      font-size: 10px;
      font-weight: 700;
      color: #2563eb;
      background: #eff6ff;
      padding: 1px 6px;
      border-radius: 4px;
      letter-spacing: 0.5px;
    }
  }

  .intro-preview-text {
    margin: 0;
    font-size: 12.5px;
    line-height: 1.6;
    color: #64748b;
    background: #f8fafc;
    padding: 10px 12px;
    border-radius: 8px;
    border: 1px solid #e2e8f0;
  }
}

.meta-pills-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.meta-pill-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  font-size: 11.5px;

  .meta-k {
    color: #94a3b8;
  }

  .meta-v {
    color: #1e293b;
    font-weight: 600;
    max-width: 100px;
  }
}

.text-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sync-tip-box {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  font-size: 11.5px;
  color: #1d4ed8;
  line-height: 1.45;

  .el-icon {
    margin-top: 2px;
    flex-shrink: 0;
  }
}

@media (max-width: 1200px) {
  .edit-layout {
    grid-template-columns: 1fr;
  }

  .preview-sticky-wrap {
    position: static;
  }
}
</style>
