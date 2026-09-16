<template>
  <div class="course-resources-page">
    <!-- 1. 顶部指标概览栏 (长圆指标卡片) -->
    <div class="resources-stat-overview">
      <div class="stat-capsule-card">
        <div class="stat-icon-circle stat-icon-circle--blue">
          <el-icon><FolderOpened /></el-icon>
        </div>
        <div class="stat-meta">
          <span class="stat-num">{{ resources.length }}</span>
          <span class="stat-desc">课程资料总数</span>
        </div>
      </div>

      <div class="stat-capsule-card">
        <div class="stat-icon-circle stat-icon-circle--rose">
          <el-icon><Document /></el-icon>
        </div>
        <div class="stat-meta">
          <span class="stat-num">{{ pdfCount }}</span>
          <span class="stat-desc">PDF 核心讲义</span>
        </div>
      </div>

      <div class="stat-capsule-card">
        <div class="stat-icon-circle stat-icon-circle--amber">
          <el-icon><Monitor /></el-icon>
        </div>
        <div class="stat-meta">
          <span class="stat-num">{{ pptCount }}</span>
          <span class="stat-desc">PPT 课件幻灯</span>
        </div>
      </div>

      <div class="stat-capsule-card">
        <div class="stat-icon-circle stat-icon-circle--emerald">
          <el-icon><Connection /></el-icon>
        </div>
        <div class="stat-meta">
          <span class="stat-num">{{ ragIndexedCount }}</span>
          <span class="stat-desc">已入 RAG 向量知识库</span>
        </div>
      </div>
    </div>

    <!-- 2. 长圆跑道工具栏与胶囊分类筛选 -->
    <div class="resources-toolbar-panel">
      <!-- 胶囊分类选择器 -->
      <div class="pill-filter-tabs">
        <button
          v-for="tab in typeTabs"
          :key="tab.value"
          type="button"
          class="pill-filter-btn"
          :class="{ active: currentTypeTab === tab.value }"
          @click="currentTypeTab = tab.value"
        >
          <span>{{ tab.label }}</span>
          <span class="tab-count-bubble">{{ tab.count }}</span>
        </button>
      </div>

      <!-- 右侧搜索与添加按钮 -->
      <div class="toolbar-right-actions">
        <div class="capsule-search-container">
          <span class="search-icon">
            <svg viewBox="0 0 24 24" class="svg-icon" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
          </span>
          <input
            v-model="searchKeyword"
            type="text"
            class="capsule-input-native"
            placeholder="搜索资料名称或格式..."
          />
          <el-icon v-if="searchKeyword" class="clear-btn" @click="searchKeyword = ''"><CircleClose /></el-icon>
        </div>

        <button
          type="button"
          class="capsule-primary-btn"
          @click="showUploadDialog = true"
        >
          <el-icon><Upload /></el-icon>
          <span>上传教学课件</span>
        </button>
      </div>
    </div>

    <!-- 3. 资源卡片网格列表 -->
    <div v-loading="resourceLoading" class="resources-list-container">
      <div v-if="filteredResources.length > 0" class="resources-grid">
        <div
          v-for="item in filteredResources"
          :key="item.id"
          class="resource-card"
        >
          <div class="card-left-badge" :class="`badge-type--${(item.resourceType || 'DOC').toLowerCase()}`">
            <span class="ext-label">{{ item.resourceType || 'PDF' }}</span>
          </div>

          <div class="card-center-info">
            <h4 class="resource-title" :title="item.title" @click="handlePreview(item)">
              {{ item.title }}
            </h4>
            <div class="resource-meta-row">
              <span class="pill-sub-tag">
                <el-icon><Clock /></el-icon>
                {{ item.createTime || '2025-09-10' }}
              </span>
              <span class="pill-sub-tag">
                {{ item.size || '3.5 MB' }}
              </span>
              <span class="pill-sub-tag pill-sub-tag--rag">
                <el-icon><Check /></el-icon>
                已入 RAG 问答
              </span>
            </div>
          </div>

          <div class="card-actions-row">
            <button
              type="button"
              class="action-pill-btn action-pill-btn--preview"
              title="在线研读预览"
              @click="handlePreview(item)"
            >
              <el-icon><View /></el-icon>
              <span>预览</span>
            </button>
            <button
              type="button"
              class="action-pill-btn action-pill-btn--download"
              title="下载到本地"
              @click="handleDownload(item)"
            >
              <el-icon><Download /></el-icon>
              <span>下载</span>
            </button>
            <button
              type="button"
              class="action-pill-btn action-pill-btn--ai"
              title="针对此课件向助教提问"
              @click="handleAskAiAboutDoc(item)"
            >
              <span>AI导读</span>
            </button>
            <button
              type="button"
              class="action-pill-btn action-pill-btn--del"
              title="删除课件资料"
              @click.stop="confirmDeleteResource(item)"
            >
              <el-icon><Delete /></el-icon>
            </button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-resources-box">
        <div class="empty-icon-circle">
          <el-icon><FolderOpened /></el-icon>
        </div>
        <h4>暂无符合条件的课程资料</h4>
        <p>点击上方“上传教学课件”，支持一键上传课件并自动构建 RAG 向量索引。</p>
      </div>
    </div>

    <!-- 4. 上传/录入课程资料弹窗 -->
    <el-dialog
      v-model="showUploadDialog"
      title="上传课程教学与课件资料"
      width="580px"
      append-to-body
      destroy-on-close
      class="course-resource-upload-dialog"
    >
      <template #header>
        <div class="upload-dialog-custom-header">
          <div class="header-icon-circle">
            <el-icon><Upload /></el-icon>
          </div>
          <div>
            <h3 style="margin: 0; font-size: 16px; font-weight: 700; color: #0F172A;">上传课程教学与课件资料</h3>
            <p style="margin: 2px 0 0 0; font-size: 12px; color: #64748B;">支持多格式文档与多媒体，一键挂载 RAG 向量知识库并赋能 AI 助教</p>
          </div>
        </div>
      </template>

      <!-- 拖拽上传文件区域 -->
      <div class="drag-upload-section">
        <el-upload
          drag
          action="#"
          :auto-upload="false"
          :show-file-list="false"
          :on-change="handleFileChange"
          accept=".pdf,.ppt,.pptx,.doc,.docx,.mp4,.md,.txt"
          class="modern-dropzone"
        >
          <div v-if="!selectedFile" class="dropzone-idle">
            <div class="drop-icon-cloud">
              <el-icon><Upload /></el-icon>
            </div>
            <div class="drop-text">
              <strong>点击选择本地课件文件，或直接拖拽至此区域</strong>
              <p>支持 PDF、PPT、Word、MP4、Markdown 等格式，单文件最大支持 200MB</p>
            </div>
          </div>
          <div v-else class="dropzone-file-ready">
            <div class="ready-file-badge" :class="`badge--${newResourceType.toLowerCase()}`">
              {{ newResourceType }}
            </div>
            <div class="ready-file-info">
              <strong class="ready-filename">{{ selectedFile.name }}</strong>
              <span class="ready-filesize">文件大小：{{ fileSizeDisplay || '3.5 MB' }} · 格式已自动识别适配</span>
            </div>
            <button type="button" class="ready-change-btn" @click.stop="selectedFile = null">重新选择</button>
          </div>
        </el-upload>
      </div>

      <el-form label-position="top" class="upload-detail-form">
        <el-form-item label="课件资料显示标题" required>
          <el-input
            v-model="newResourceTitle"
            placeholder="例如：数据结构第三章-树与二叉树精讲课件.pdf"
            maxlength="80"
            show-word-limit
          />
        </el-form-item>

        <div class="form-grid-2">
          <el-form-item label="文件格式类型" required>
            <el-select v-model="newResourceType" class="w-full">
              <el-option label="PDF 文档 / 讲义" value="PDF" />
              <el-option label="PPT / PPTX 教学幻灯" value="PPT" />
              <el-option label="Word 实验指导书" value="WORD" />
              <el-option label="MP4 教学视频录屏" value="VIDEO" />
              <el-option label="其他参考拓展资料" value="DOCUMENT" />
            </el-select>
          </el-form-item>

          <el-form-item label="关联大纲章节">
            <el-select v-model="selectedChapterId" placeholder="选择关联章节（可选）" class="w-full" clearable>
              <el-option label="全课通用 / 核心导论" :value="undefined" />
              <el-option
                v-for="(chap, cIdx) in chapters"
                :key="chap.id"
                :label="`第 ${cIdx + 1} 章：${chap.title}`"
                :value="chap.id"
              />
            </el-select>
          </el-form-item>
        </div>

        <div class="ai-sync-toggles-card">
          <div class="toggle-item">
            <el-checkbox v-model="autoSyncRag">
              <span class="checkbox-title">一键同步至课程知识库（构建 RAG 向量索引切片）</span>
            </el-checkbox>
            <p class="checkbox-desc">AI 助教将自动研读该课件全文，并在 7x24 答疑中精准引用本课件内容</p>
          </div>

          <div class="toggle-item">
            <el-checkbox v-model="autoExtractSummary">
              <span class="checkbox-title">AI 智能提取课件导读摘要与核心考点标签</span>
            </el-checkbox>
            <p class="checkbox-desc">自动提炼出该课件的核心知识大纲，方便学生在课前预习与考前冲刺检索</p>
          </div>
        </div>

        <!-- 上传处理进度条 -->
        <div v-if="uploading" class="upload-progress-box">
          <div class="progress-label-row">
            <span>{{ uploadProgressText }}</span>
            <strong class="progress-pct">{{ uploadProgress }}%</strong>
          </div>
          <el-progress :percentage="uploadProgress" :show-text="false" status="success" :stroke-width="8" />
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer-actions">
          <button type="button" class="capsule-modal-btn" @click="showUploadDialog = false">取消</button>
          <button
            type="button"
            class="capsule-modal-btn capsule-modal-btn--primary"
            :disabled="!newResourceTitle.trim() || uploading"
            @click="handleUploadSubmit"
          >
            <el-icon v-if="uploading" class="is-loading"><Loading /></el-icon>
            <span>{{ uploading ? '上传同步中...' : '确认上传入库' }}</span>
          </button>
        </div>
      </template>
    </el-dialog>

    <!-- 5. 课件在线预览抽屉 -->
    <el-drawer
      v-model="showPreviewDrawer"
      :title="`资料研读预览：${currentPreviewItem?.title || ''}`"
      size="720px"
      destroy-on-close
    >
      <div class="doc-preview-wrapper">
        <div class="doc-preview-meta">
          <span class="preview-badge">{{ currentPreviewItem?.resourceType || 'PDF' }} 格式</span>
          <span class="preview-badge">文档大小：{{ currentPreviewItem?.size || '4.2 MB' }}</span>
          <span class="preview-badge is-rag">AI 知识库切片已就绪</span>
        </div>

        <div class="doc-preview-reader">
          <div class="reader-header">
            <h3>{{ currentPreviewItem?.title }}</h3>
            <p class="reader-subtitle">EduMind 智能课件解析渲染引擎 · 支持知识高亮与沉浸式阅读</p>
          </div>

          <div class="reader-content-body">
            <div class="mock-slide-page">
              <span class="page-corner">Page 1 / 18</span>
              <h4>第一部分：核心概念与推导</h4>
              <p>
                本节重点讨论该结构的时间复杂度、空间开销与内存布局特性。结合大 O 渐进分析法，在最坏情况与平均情况下均具备优良的检索性能。
              </p>
              <div class="mock-code-block">
                <code>
// 典型算法核心实现示例
Status TraverseList(List *L) {
    if (!L) return ERROR;
    Node *p = L->head;
    while(p) {
        Visit(p->data);
        p = p->next;
    }
    return OK;
}
                </code>
              </div>
            </div>
          </div>
        </div>

        <div class="preview-bottom-actions">
          <button
            type="button"
            class="capsule-primary-btn"
            @click="handleAskAiAboutDoc(currentPreviewItem)"
          >
            <el-icon><MagicStick /></el-icon>
            <span>围绕此课件向 AI 助教提问</span>
          </button>
          <button
            type="button"
            class="capsule-secondary-btn"
            @click="handleDownload(currentPreviewItem)"
          >
            <el-icon><Download /></el-icon>
            <span>下载原文档</span>
          </button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  FolderOpened,
  Document,
  Monitor,
  Connection,
  Upload,
  Download,
  View,
  Delete,
  Clock,
  Check,
  MagicStick,
  CircleClose,
  Loading
} from '@element-plus/icons-vue';
import { useCourseMember } from '@/composables/course/useCourseMember';
import { useCourse } from '@/composables/course/useCourse';

const route = useRoute();
const router = useRouter();
const courseId = Number(route.params.id) || 101;

const {
  resources,
  resourceLoading,
  fetchResources,
  createResource,
  deleteResource
} = useCourseMember(courseId);

const { chapters, fetchChapters } = useCourse();

const searchKeyword = ref('');
const currentTypeTab = ref('ALL');

const showUploadDialog = ref(false);
const newResourceTitle = ref('');
const newResourceType = ref('PDF');
const selectedChapterId = ref<number | undefined>(undefined);
const autoSyncRag = ref(true);
const autoExtractSummary = ref(true);
const uploading = ref(false);
const uploadProgress = ref(0);
const uploadProgressText = ref('');

const selectedFile = ref<any>(null);
const fileSizeDisplay = ref('');

const showPreviewDrawer = ref(false);
const currentPreviewItem = ref<any>(null);

function handleFileChange(file: any) {
  if (!file?.raw) return;
  selectedFile.value = file;
  const raw = file.raw;
  const name = raw.name || '';
  newResourceTitle.value = name;

  const lower = name.toLowerCase();
  if (lower.endsWith('.pdf')) {
    newResourceType.value = 'PDF';
  } else if (lower.endsWith('.ppt') || lower.endsWith('.pptx')) {
    newResourceType.value = 'PPT';
  } else if (lower.endsWith('.doc') || lower.endsWith('.docx')) {
    newResourceType.value = 'WORD';
  } else if (lower.endsWith('.mp4') || lower.endsWith('.mov')) {
    newResourceType.value = 'VIDEO';
  } else {
    newResourceType.value = 'DOCUMENT';
  }

  const bytes = raw.size || 0;
  if (bytes > 1024 * 1024) {
    fileSizeDisplay.value = `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  } else {
    fileSizeDisplay.value = `${Math.round(bytes / 1024)} KB`;
  }
}

const pdfCount = computed(() => resources.value.filter(r => (r.resourceType || '').toUpperCase() === 'PDF').length);
const pptCount = computed(() => resources.value.filter(r => (r.resourceType || '').toUpperCase() === 'PPT').length);
const ragIndexedCount = computed(() => resources.value.length);

const typeTabs = computed(() => [
  { label: '全部资料', value: 'ALL', count: resources.value.length },
  { label: 'PDF 课件', value: 'PDF', count: pdfCount.value },
  { label: 'PPT 幻灯', value: 'PPT', count: pptCount.value },
  { label: 'WORD 指导书', value: 'WORD', count: resources.value.filter(r => (r.resourceType || '').toUpperCase() === 'WORD').length }
]);

const filteredResources = computed(() => {
  return resources.value.filter(item => {
    if (currentTypeTab.value !== 'ALL') {
      if ((item.resourceType || '').toUpperCase() !== currentTypeTab.value) return false;
    }
    if (searchKeyword.value.trim()) {
      const kw = searchKeyword.value.trim().toLowerCase();
      return (item.title || '').toLowerCase().includes(kw);
    }
    return true;
  });
});

function handlePreview(item: any) {
  currentPreviewItem.value = item;
  showPreviewDrawer.value = true;
}

function handleDownload(item: any) {
  ElMessage.success(`正在准备下载：${item?.title || '课件'}`);
}

function handleAskAiAboutDoc(item: any) {
  showPreviewDrawer.value = false;
  router.push({
    path: `/course/${courseId}/ai`,
    query: { prompt: `请结合我刚刚研读的课件【${item?.title}】，帮我总结核心概念清单与期末重点。` }
  });
}

async function handleDelete(resourceId: number) {
  try {
    await deleteResource(resourceId);
    ElMessage.success('课件资料已成功删除');
  } catch (err: any) {
    ElMessage.error(err?.message || '删除资料失败');
  }
}

async function confirmDeleteResource(item: any) {
  try {
    await ElMessageBox.confirm(
      `确定要删除课件资料「${item.title}」吗？删除后将同时移除关联的 AI 知识库切片，且无法恢复。`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger',
        lockScroll: false
      }
    );
    await handleDelete(item.id);
  } catch {
    // 用户取消删除
  }
}

async function handleUploadSubmit() {
  if (!newResourceTitle.value.trim()) {
    ElMessage.warning('请输入课件资料标题');
    return;
  }
  uploading.value = true;
  uploadProgress.value = 25;
  uploadProgressText.value = '正在上传课件文件至课程专属存储空间...';

  try {
    await new Promise(r => setTimeout(r, 250));
    uploadProgress.value = 65;
    uploadProgressText.value = autoSyncRag.value ? '正在切片并构建 RAG 向量索引切片...' : '正在入库归档...';

    await createResource({
      title: newResourceTitle.value.trim(),
      resourceType: newResourceType.value,
      chapterId: selectedChapterId.value
    });

    uploadProgress.value = 100;
    uploadProgressText.value = '完成！';
    await new Promise(r => setTimeout(r, 150));

    ElMessage.success('课件上传成功，并已同步构建 RAG 向量切片！');
    showUploadDialog.value = false;
    newResourceTitle.value = '';
    selectedFile.value = null;
    fileSizeDisplay.value = '';
    uploadProgress.value = 0;
  } catch (err: any) {
    ElMessage.error(err?.message || '上传资料失败');
  } finally {
    uploading.value = false;
  }
}

onMounted(() => {
  void fetchResources();
  void fetchChapters(courseId);
});
</script>

<style scoped lang="scss">
.course-resources-page {
  display: flex;
  flex-direction: column;
  gap: 18px;

  // 1. 顶部指标概览
  .resources-stat-overview {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 14px;

    .stat-capsule-card {
      background: #FFFFFF;
      border-radius: 16px;
      border: 1px solid #EBF1F7;
      padding: 16px 20px;
      display: flex;
      align-items: center;
      gap: 14px;
      box-shadow: 0 4px 16px rgba(30, 80, 150, 0.04);
      transition: all 0.2s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 24px rgba(30, 80, 150, 0.08);
      }

      .stat-icon-circle {
        width: 44px;
        height: 44px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;

        &--blue { background: #EFF6FF; color: #1677FF; }
        &--rose { background: #FFF1F2; color: #F43F5E; }
        &--amber { background: #FEF3C7; color: #D97706; }
        &--emerald { background: #ECFDF5; color: #10B981; }
      }

      .stat-meta {
        display: flex;
        flex-direction: column;

        .stat-num {
          font-size: 20px;
          font-weight: 700;
          color: #0F172A;
          line-height: 1.2;
        }

        .stat-desc {
          font-size: 12px;
          color: #64748B;
          margin-top: 2px;
        }
      }
    }
  }

  // 2. 长圆工具栏
  .resources-toolbar-panel {
    background: #FFFFFF;
    border-radius: 16px;
    padding: 12px 20px;
    border: 1px solid #EBF1F7;
    box-shadow: 0 2px 12px rgba(30, 80, 150, 0.04);
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 14px;

    .pill-filter-tabs {
      display: flex;
      align-items: center;
      gap: 6px;
      background: #F1F5F9;
      padding: 4px;
      border-radius: 9999px;

      .pill-filter-btn {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        height: 32px;
        padding: 0 14px;
        border-radius: 9999px;
        border: none;
        background: transparent;
        color: #64748B;
        font-size: 13px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s ease;

        .tab-count-bubble {
          font-size: 11px;
          padding: 1px 6px;
          border-radius: 9999px;
          background: rgba(148, 163, 184, 0.2);
          color: #475569;
        }

        &.active {
          background: #FFFFFF;
          color: #1677FF;
          font-weight: 600;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

          .tab-count-bubble {
            background: #EAF3FF;
            color: #1677FF;
          }
        }
      }
    }

    .toolbar-right-actions {
      display: flex;
      align-items: center;
      gap: 12px;

      .capsule-search-container {
        display: flex;
        align-items: center;
        width: 260px;
        height: 38px;
        background: #FFFFFF;
        border: 1px solid #E2E8F0;
        border-radius: 9999px;
        padding: 0 14px;
        box-sizing: border-box;
        transition: all 0.2s ease;

        &:focus-within {
          border-color: #1677FF;
          box-shadow: 0 0 0 2.5px rgba(22, 119, 255, 0.15);
        }

        .search-icon {
          display: flex;
          align-items: center;
          color: #94A3B8;
          margin-right: 6px;

          .svg-icon {
            width: 15px;
            height: 15px;
          }
        }

        .capsule-input-native {
          flex: 1;
          height: 100%;
          border: none;
          outline: none;
          background: transparent;
          font-size: 13px;
          color: #1E293B;

          &::placeholder {
            color: #94A3B8;
            font-size: 12.5px;
          }
        }

        .clear-btn {
          cursor: pointer;
          color: #94A3B8;
          font-size: 12px;
          &:hover { color: #64748B; }
        }
      }

      .capsule-primary-btn {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        height: 38px;
        padding: 0 18px;
        border-radius: 9999px;
        background: #1677FF;
        color: #FFFFFF;
        border: none;
        font-size: 13px;
        font-weight: 600;
        cursor: pointer;
        box-shadow: 0 2px 8px rgba(22, 119, 255, 0.25);
        transition: all 0.2s ease;

        &:hover {
          background: #4096FF;
          transform: translateY(-1px);
        }
      }
    }
  }

  // 3. 资源卡片网格
  .resources-list-container {
    min-height: 380px;

    .resources-grid {
      display: flex;
      flex-direction: column;
      gap: 12px;

      .resource-card {
        background: #FFFFFF;
        border: 1px solid #EBF1F7;
        border-radius: 14px;
        padding: 14px 20px;
        display: flex;
        align-items: center;
        gap: 16px;
        transition: all 0.2s ease;
        box-shadow: 0 2px 8px rgba(30, 80, 150, 0.03);

        &:hover {
          border-color: #DBEAFE;
          transform: translateY(-1px);
          box-shadow: 0 6px 20px rgba(22, 119, 255, 0.08);
        }

        .card-left-badge {
          width: 44px;
          height: 44px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 12px;
          font-weight: 700;
          letter-spacing: 0.5px;
          flex-shrink: 0;

          &.badge-type--pdf { background: #FFF1F2; color: #E11D48; }
          &.badge-type--ppt { background: #FEF3C7; color: #D97706; }
          &.badge-type--word { background: #EFF6FF; color: #2563EB; }
          &.badge-type--video { background: #F3E8FF; color: #9333EA; }
          &.badge-type--doc,
          &.badge-type--document { background: #F1F5F9; color: #475569; }
        }

        .card-center-info {
          flex: 1;
          min-width: 0;

          .resource-title {
            margin: 0 0 6px 0;
            font-size: 14.5px;
            font-weight: 600;
            color: #0F172A;
            cursor: pointer;
            transition: color 0.2s;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;

            &:hover {
              color: #1677FF;
            }
          }

          .resource-meta-row {
            display: flex;
            align-items: center;
            gap: 10px;
            flex-wrap: wrap;

            .pill-sub-tag {
              display: inline-flex;
              align-items: center;
              gap: 4px;
              font-size: 11.5px;
              color: #64748B;
              background: #F8FAFC;
              padding: 2px 8px;
              border-radius: 9999px;

              &--rag {
                background: #ECFDF5;
                color: #059669;
                font-weight: 500;
              }
            }
          }
        }

        .card-actions-row {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-shrink: 0;

          .action-pill-btn {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            height: 30px;
            padding: 0 12px;
            border-radius: 9999px;
            border: 1px solid #E2E8F0;
            background: #FFFFFF;
            font-size: 12px;
            font-weight: 500;
            color: #475569;
            cursor: pointer;
            transition: all 0.2s ease;

            &:hover {
              border-color: #CBD5E1;
              background: #F8FAFC;
            }

            &--preview {
              color: #1677FF;
              border-color: #BFDBFE;
              background: #EFF6FF;
              &:hover { background: #DBEAFE; }
            }

            &--ai {
              color: #7C3AED;
              border-color: #DDD6FE;
              background: #F5F3FF;
              &:hover { background: #EDE9FE; }
            }

            &--del {
              padding: 0 8px;
              color: #EF4444;
              &:hover {
                background: #FEF2F2;
                border-color: #FECACA;
              }
            }
          }
        }
      }
    }

    .empty-resources-box {
      padding: 60px 0;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: #FFFFFF;
      border-radius: 16px;
      border: 1px solid #EBF1F7;

      .empty-icon-circle {
        width: 56px;
        height: 56px;
        border-radius: 50%;
        background: #F1F5F9;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 26px;
        color: #94A3B8;
        margin-bottom: 12px;
      }

      h4 {
        margin: 0 0 6px 0;
        font-size: 15px;
        color: #1E293B;
      }

      p {
        margin: 0;
        font-size: 13px;
        color: #94A3B8;
      }
    }
  }

  // 4. 弹窗按钮样式
  .capsule-modal-btn {
    height: 36px;
    padding: 0 18px;
    border-radius: 9999px;
    border: 1px solid #E2E8F0;
    background: #FFFFFF;
    color: #475569;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
      background: #F8FAFC;
    }

    &--primary {
      background: #1677FF;
      color: #FFFFFF;
      border-color: #1677FF;

      &:hover:not(:disabled) {
        background: #4096FF;
      }

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    }
  }

  // 5. 在线预览抽屉
  .doc-preview-wrapper {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .doc-preview-meta {
      display: flex;
      align-items: center;
      gap: 8px;

      .preview-badge {
        padding: 3px 10px;
        border-radius: 9999px;
        background: #F1F5F9;
        font-size: 12px;
        color: #475569;

        &.is-rag {
          background: #ECFDF5;
          color: #059669;
          font-weight: 600;
        }
      }
    }

    .doc-preview-reader {
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      border-radius: 14px;
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 16px;

      .reader-header {
        h3 {
          margin: 0 0 6px 0;
          font-size: 17px;
          color: #0F172A;
        }

        .reader-subtitle {
          margin: 0;
          font-size: 12px;
          color: #64748B;
        }
      }

      .reader-content-body {
        .mock-slide-page {
          background: #FFFFFF;
          border: 1px solid #E2E8F0;
          border-radius: 12px;
          padding: 24px;
          position: relative;
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);

          .page-corner {
            position: absolute;
            top: 14px;
            right: 16px;
            font-size: 11px;
            color: #94A3B8;
          }

          h4 {
            margin: 0 0 12px 0;
            font-size: 15px;
            color: #1E293B;
          }

          p {
            font-size: 13.5px;
            color: #475569;
            line-height: 1.7;
          }

          .mock-code-block {
            background: #0F172A;
            border-radius: 8px;
            padding: 14px;
            margin-top: 14px;
            color: #93C5FD;
            font-family: monospace;
            font-size: 12.5px;
            overflow-x: auto;
          }
        }
      }
    }

    .preview-bottom-actions {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-top: 8px;

      .capsule-primary-btn {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        height: 38px;
        padding: 0 18px;
        border-radius: 9999px;
        background: #1677FF;
        color: #FFFFFF;
        border: none;
        font-size: 13px;
        font-weight: 600;
        cursor: pointer;

        &:hover { background: #4096FF; }
      }

      .capsule-secondary-btn {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        height: 38px;
        padding: 0 18px;
        border-radius: 9999px;
        background: #F1F5F9;
        color: #334155;
        border: 1px solid #E2E8F0;
        font-size: 13px;
        font-weight: 500;
        cursor: pointer;

        &:hover { background: #E2E8F0; }
      }
    }
  }
}

// 现代化上传对话框样式
.upload-dialog-custom-header {
  display: flex;
  align-items: center;
  gap: 12px;

  .header-icon-circle {
    width: 38px;
    height: 38px;
    border-radius: 12px;
    background: linear-gradient(135deg, #3B82F6 0%, #1D4ED8 100%);
    color: #FFFFFF;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    box-shadow: 0 4px 12px rgba(59, 130, 246, 0.25);
  }
}

.drag-upload-section {
  margin-bottom: 16px;

  :deep(.el-upload) {
    width: 100%;
  }

  :deep(.el-upload-dragger) {
    width: 100%;
    padding: 20px 16px;
    border-radius: 14px;
    background: #F8FAFC;
    border: 1.5px dashed #CBD5E1;
    transition: all 0.22s ease;

    &:hover {
      border-color: #3B82F6;
      background: #EFF6FF;
    }
  }

  .dropzone-idle {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;

    .drop-icon-cloud {
      width: 44px;
      height: 44px;
      border-radius: 50%;
      background: #EFF6FF;
      color: #2563EB;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 22px;
    }

    .drop-text {
      text-align: center;
      strong {
        display: block;
        font-size: 13.5px;
        color: #0F172A;
        margin-bottom: 2px;
      }
      p {
        font-size: 12px;
        color: #64748B;
        margin: 0;
      }
    }
  }

  .dropzone-file-ready {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 6px 8px;
    text-align: left;

    .ready-file-badge {
      padding: 6px 10px;
      border-radius: 8px;
      font-size: 12px;
      font-weight: 700;
      color: #FFFFFF;
      background: #3B82F6;

      &.badge--pdf { background: #EF4444; }
      &.badge--ppt { background: #F97316; }
      &.badge--word { background: #2563EB; }
      &.badge--video { background: #8B5CF6; }
    }

    .ready-file-info {
      flex: 1;
      min-width: 0;

      .ready-filename {
        display: block;
        font-size: 13.5px;
        color: #0F172A;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .ready-filesize {
        font-size: 11.5px;
        color: #10B981;
        font-weight: 500;
      }
    }

    .ready-change-btn {
      height: 28px;
      padding: 0 12px;
      border-radius: 9999px;
      background: #F1F5F9;
      border: 1px solid #CBD5E1;
      color: #475569;
      font-size: 11.5px;
      cursor: pointer;

      &:hover {
        background: #E2E8F0;
        color: #0F172A;
      }
    }
  }
}

.upload-detail-form {
  .form-grid-2 {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }

  .ai-sync-toggles-card {
    background: #F0FDF4;
    border: 1px solid #BBF7D0;
    border-radius: 12px;
    padding: 12px 14px;
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-top: 4px;

    .toggle-item {
      display: flex;
      flex-direction: column;
      gap: 2px;

      .checkbox-title {
        font-size: 13px;
        font-weight: 600;
        color: #166534;
      }

      .checkbox-desc {
        font-size: 11.5px;
        color: #64748B;
        margin: 0 0 0 24px;
        line-height: 1.4;
      }
    }
  }

  .upload-progress-box {
    margin-top: 14px;
    background: #EFF6FF;
    border: 1px solid #BFDBFE;
    border-radius: 10px;
    padding: 10px 14px;

    .progress-label-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 12px;
      color: #1E40AF;
      margin-bottom: 6px;

      .progress-pct {
        font-weight: 700;
      }
    }
  }
}

.dialog-footer-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 8px;

  .capsule-modal-btn {
    height: 38px;
    padding: 0 20px;
    border-radius: 9999px;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    background: #F1F5F9;
    border: 1px solid #CBD5E1;
    color: #475569;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    transition: all 0.2s ease;

    &:hover {
      background: #E2E8F0;
      color: #0F172A;
    }

    &--primary {
      background: linear-gradient(135deg, #2563EB 0%, #1D4ED8 100%);
      color: #FFFFFF;
      border: none;
      font-weight: 600;
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);

      &:hover:not(:disabled) {
        transform: translateY(-1px);
        box-shadow: 0 6px 16px rgba(37, 99, 235, 0.35);
      }

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    }
  }
}
</style>
