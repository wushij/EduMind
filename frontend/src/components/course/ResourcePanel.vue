<template>
  <div class="course-resources-page">
    <div class="resources-panel-card">
      <!-- 1. 顶部指标概览栏 -->
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

      <div class="resources-panel-divider" aria-hidden="true" />

      <!-- 2. 筛选工具栏 -->
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
          v-if="editable"
          type="button"
          class="capsule-primary-btn"
          @click="showUploadDialog = true"
        >
          <el-icon><Upload /></el-icon>
          <span>上传教学课件</span>
        </button>
      </div>
      </div>

      <div class="resources-panel-divider" aria-hidden="true" />

      <!-- 3. 资源列表 -->
      <div v-loading="resourceLoading" class="resources-list-container">
        <div v-if="pagedResources.length > 0" class="resources-grid">
          <div
            v-for="item in pagedResources"
            :key="item.id"
            class="resource-card"
          >
          <div class="card-left-badge" :class="`badge-type--${itemResourceType(item).toLowerCase()}`">
            <span class="ext-label">{{ resourceTypeShortLabel(itemResourceType(item), item.title) }}</span>
          </div>

          <h4 class="resource-title" :title="item.title" @click="handlePreview(item)">
            {{ item.title }}
          </h4>

          <div class="resource-meta-row">
            <span class="pill-sub-tag pill-sub-tag--time">
              {{ formatResourceListedTime(item.createTime) }}
            </span>
            <span v-if="item.size" class="pill-sub-tag">
              {{ item.size }}
            </span>
            <span v-if="item.documentId" class="pill-sub-tag pill-sub-tag--rag">
              <el-icon><Check /></el-icon>
              已入 RAG
            </span>
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
              v-if="editable"
              type="button"
              class="action-pill-btn action-pill-btn--del"
              title="删除课件资料"
              aria-label="删除课件资料"
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

        <AppPagination
          v-if="filteredTotal > 0"
          v-model:page-num="pageNum"
          v-model:page-size="pageSize"
          :total="filteredTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
        />
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
          multiple
          :auto-upload="false"
          :show-file-list="false"
          :file-list="uploadFileList"
          :on-change="handleFileChange"
          accept=".pdf,.ppt,.pptx,.doc,.docx,.mp4,.md,.txt"
          class="modern-dropzone"
        >
          <div class="dropzone-idle">
            <div class="drop-icon-cloud">
              <el-icon><Upload /></el-icon>
            </div>
            <div class="drop-text">
              <strong>点击选择本地课件（可多选），或直接拖拽至此区域</strong>
              <p>支持 PDF、PPT、Word、MP4、Markdown 等格式，单文件最大支持 200MB</p>
            </div>
          </div>
        </el-upload>
        <div v-if="selectedFiles.length > 0" class="pending-upload-files">
          <div class="pending-upload-files__head">
            <span class="pending-upload-files__count">已选择 {{ selectedFiles.length }} 个文件</span>
            <button type="button" class="pending-upload-files__clear" @click="confirmClearSelectedFiles">
              清空列表
            </button>
          </div>
          <ul class="pending-upload-files__list">
            <li
              v-for="(file, index) in selectedFiles"
              :key="`${file.name}-${file.size}-${index}`"
              class="pending-upload-files__item"
            >
              <span class="pending-upload-files__file-icon" aria-hidden="true">
                <el-icon><Document /></el-icon>
              </span>
              <span class="pending-upload-files__name" :title="file.name">{{ file.name }}</span>
              <span class="pending-upload-files__size">{{ formatUploadFileSize(file.size) }}</span>
              <button
                type="button"
                class="pending-upload-files__remove"
                title="移除此文件"
                aria-label="移除此文件"
                @click="confirmRemoveSelectedFile(index)"
              >
                <el-icon :size="14"><Close /></el-icon>
              </button>
            </li>
          </ul>
        </div>
      </div>

      <el-form label-position="top" class="upload-detail-form">
        <el-form-item v-if="selectedFiles.length <= 1" label="课件资料显示标题" required>
          <el-input
            v-model="newResourceTitle"
            placeholder="例如：数据结构第三章-树与二叉树精讲课件.pdf"
            maxlength="80"
            show-word-limit
          />
          <p v-if="selectedFiles.length === 1" class="detected-type-hint">
            格式已根据所选文件自动识别为
            <span class="detected-type-hint__badge">
              {{ resourceTypeShortLabel(inferResourceTypeFromFileName(selectedFiles[0].name)) }}
            </span>
            ，无需手动选择
          </p>
        </el-form-item>

        <p v-else class="batch-upload-hint">批量上传时将按各文件名自动识别格式并分别入库，无需单独填写标题。</p>

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
            :disabled="!canConfirmUpload || uploading"
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
      size="82%"
      destroy-on-close
      class="course-resource-preview-drawer"
    >
      <div class="doc-preview-wrapper">
        <div class="doc-preview-meta">
          <span class="preview-badge">
            {{
              currentPreviewItem
                ? `${resourceTypeShortLabel(itemResourceType(currentPreviewItem), currentPreviewItem.title)} 格式`
                : '—'
            }}
          </span>
          <span v-if="currentPreviewItem?.size" class="preview-badge">
            文档大小：{{ currentPreviewItem.size }}
          </span>
          <span v-if="currentPreviewItem?.documentId" class="preview-badge is-rag">已关联知识库文档</span>
        </div>

        <div v-loading="previewLoading" class="doc-preview-reader">
          <div
            v-if="previewHtml"
            ref="previewBodyRef"
            class="reader-content-body markdown-body chat-md-content"
            v-html="previewHtml"
          />
          <div v-else-if="previewPdfUrl" class="reader-pdf-frame">
            <iframe :src="previewPdfUrl" title="PDF 预览" />
          </div>
          <div v-else class="reader-empty">
            <el-icon class="reader-empty__icon"><Document /></el-icon>
            <p class="reader-empty__title">暂无预览内容</p>
          </div>
        </div>

        <div class="preview-bottom-actions">
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
import { ref, computed, onMounted, watch, nextTick } from 'vue';
import { useRoute } from 'vue-router';
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
  CircleClose,
  Check,
  Loading,
  Close
} from '@element-plus/icons-vue';
import type { UploadFile, UploadFiles } from 'element-plus';
import type { Course } from '@/types/course/course';
import type { CourseResourceItem } from '@/types/course/resource';
import { useCourseMember } from '@/composables/course/useCourseMember';
import { useCourse } from '@/composables/course/useCourse';
import {
  defaultResourceTitleFromFileName,
  formatUploadFileSize,
  inferResourceTypeFromFileName,
  isMarkdownLikeResourceType,
  resolveResourceType,
  resourceTypeShortLabel
} from '@/utils/course/infer-resource-type';
import { formatDateTime } from '@/utils/format/date';
import { bindMarkdownCodeCopy, renderChatMarkdown } from '@/utils/ai/chat-markdown';
import { batchUploadMessage, emptyBatchUploadResult } from '@/utils/upload/coalesce-upload-files';
import { downloadByApiPath, fetchStorageBlob, fetchStorageText } from '@/utils/download/blob-download';
import AppPagination from '@/components/common/AppPagination.vue';
import { usePagination } from '@/composables/common/usePagination';

withDefaults(
  defineProps<{
    course?: Course | null;
    editable?: boolean;
  }>(),
  { editable: false }
);

const route = useRoute();
const courseId = Number(route.params.id) || 0;

const {
  resources,
  resourceLoading,
  fetchResources,
  uploadResource,
  deleteResource
} = useCourseMember(courseId);

const { chapters, fetchChapters } = useCourse();

const searchKeyword = ref('');
const currentTypeTab = ref('ALL');
const { pageNum, pageSize } = usePagination(10);

const showUploadDialog = ref(false);
const newResourceTitle = ref('');
const selectedChapterId = ref<number | undefined>(undefined);
const autoSyncRag = ref(true);
const autoExtractSummary = ref(true);
const uploading = ref(false);
const uploadProgress = ref(0);
const uploadProgressText = ref('');

const uploadFileList = ref<UploadFile[]>([]);
const selectedFiles = ref<File[]>([]);

const showPreviewDrawer = ref(false);
const currentPreviewItem = ref<CourseResourceItem | null>(null);
const previewLoading = ref(false);
const previewMarkdownText = ref('');
const previewPdfUrl = ref('');
const previewBodyRef = ref<HTMLElement | null>(null);
const localPreviewByResourceId = new Map<number, string>();
const localResourceTypeById = new Map<number, string>();

function itemResourceType(item: CourseResourceItem): string {
  const cachedType = localResourceTypeById.get(item.id);
  if (cachedType) return cachedType;
  if (localPreviewByResourceId.has(item.id)) return 'MD';
  return resolveResourceType(item.resourceType, item.title);
}

const previewHtml = computed(() =>
  previewMarkdownText.value ? renderChatMarkdown(previewMarkdownText.value) : ''
);

const canConfirmUpload = computed(() => {
  if (selectedFiles.value.length === 0) return false;
  if (selectedFiles.value.length === 1) {
    return newResourceTitle.value.trim().length > 0;
  }
  return true;
});

function handleFileChange(_file: UploadFile, fileList: UploadFiles) {
  uploadFileList.value = [...fileList];
  syncSelectedFilesFromUploadList();
}

function filesFromUploadList(fileList: UploadFiles): File[] {
  const files: File[] = [];
  for (const item of fileList) {
    if (item.raw) {
      files.push(item.raw);
    }
  }
  return files;
}

function syncSelectedFilesFromUploadList() {
  selectedFiles.value = filesFromUploadList(uploadFileList.value);

  if (selectedFiles.value.length === 1) {
    const raw = selectedFiles.value[0];
    newResourceTitle.value = defaultResourceTitleFromFileName(raw.name) || raw.name;
  }
}

function removeSelectedFile(index: number) {
  uploadFileList.value = uploadFileList.value.filter((_, i) => i !== index);
  syncSelectedFilesFromUploadList();
  if (selectedFiles.value.length === 0) {
    newResourceTitle.value = '';
  }
}

function clearSelectedFiles() {
  uploadFileList.value = [];
  selectedFiles.value = [];
  newResourceTitle.value = '';
}

async function confirmRemoveSelectedFile(index: number) {
  const file = selectedFiles.value[index];
  if (!file) return;
  try {
    await ElMessageBox.confirm(
      `确定从待上传列表中移除「${file.name}」吗？移除后需重新选择文件。`,
      '移除确认',
      {
        type: 'warning',
        confirmButtonText: '确认移除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger',
        lockScroll: false
      }
    );
    removeSelectedFile(index);
  } catch {
    // 用户取消
  }
}

async function confirmClearSelectedFiles() {
  const count = selectedFiles.value.length;
  if (count === 0) return;
  try {
    await ElMessageBox.confirm(
      `确定清空已选择的 ${count} 个文件吗？清空后需重新选择或拖拽上传。`,
      '清空确认',
      {
        type: 'warning',
        confirmButtonText: '确认清空',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger',
        lockScroll: false
      }
    );
    clearSelectedFiles();
  } catch {
    // 用户取消
  }
}

function formatResourceListedTime(value?: string): string {
  if (!value) return '上传时间未知';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '上传时间未知';
  return formatDateTime(d);
}

const pdfCount = computed(() => resources.value.filter(r => (r.resourceType || '').toUpperCase() === 'PDF').length);
const pptCount = computed(() => resources.value.filter(r => (r.resourceType || '').toUpperCase() === 'PPT').length);
const ragIndexedCount = computed(
  () =>
    resources.value.filter(
      (r) =>
        r.documentId != null &&
        (r.knowledgeParseStatus === 'SUCCESS' || (r.knowledgeChunkCount != null && r.knowledgeChunkCount > 0))
    ).length
);

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

const filteredTotal = computed(() => filteredResources.value.length);

const pagedResources = computed(() => {
  const start = (pageNum.value - 1) * pageSize.value;
  return filteredResources.value.slice(start, start + pageSize.value);
});

watch([currentTypeTab, searchKeyword], () => {
  pageNum.value = 1;
});

watch(filteredTotal, (count) => {
  const maxPage = Math.max(1, Math.ceil(count / pageSize.value) || 1);
  if (pageNum.value > maxPage) {
    pageNum.value = maxPage;
  }
});

function handlePreview(item: CourseResourceItem) {
  currentPreviewItem.value = item;
  showPreviewDrawer.value = true;
}

const previewPdfObjectUrl = ref('');

async function loadPreviewContent(item: CourseResourceItem) {
  previewLoading.value = true;
  previewMarkdownText.value = '';
  if (previewPdfObjectUrl.value) {
    URL.revokeObjectURL(previewPdfObjectUrl.value);
    previewPdfObjectUrl.value = '';
  }
  previewPdfUrl.value = '';
  try {
    const cached = localPreviewByResourceId.get(item.id);
    if (cached) {
      previewMarkdownText.value = cached;
      return;
    }

    if (!item.downloadUrl) {
      return;
    }

    const displayType = itemResourceType(item);

    if (displayType === 'PDF') {
      const blob = await fetchStorageBlob(item.downloadUrl);
      previewPdfObjectUrl.value = URL.createObjectURL(blob);
      previewPdfUrl.value = previewPdfObjectUrl.value;
      return;
    }

    if (isMarkdownLikeResourceType(displayType, item.title)) {
      previewMarkdownText.value = await fetchStorageText(item.downloadUrl);
      return;
    }

    if (displayType !== 'PDF') {
      previewMarkdownText.value = await fetchStorageText(item.downloadUrl);
    }
  } catch {
    ElMessage.warning('预览内容加载失败，请尝试下载后本地打开');
  } finally {
    previewLoading.value = false;
  }
}

watch(showPreviewDrawer, async (open) => {
  if (!open) {
    previewMarkdownText.value = '';
    previewPdfUrl.value = '';
    if (previewPdfObjectUrl.value) {
      URL.revokeObjectURL(previewPdfObjectUrl.value);
      previewPdfObjectUrl.value = '';
    }
    return;
  }
  const item = currentPreviewItem.value;
  if (!item) return;
  await loadPreviewContent(item);
  await nextTick();
  if (previewBodyRef.value) {
    bindMarkdownCodeCopy(previewBodyRef.value);
  }
});

async function handleDownload(item: CourseResourceItem | null) {
  if (!item) return;
  if (!item.downloadUrl) {
    ElMessage.info('暂无可下载的文件');
    return;
  }
  try {
    const ext = inferResourceTypeFromFileName(item.title).toLowerCase();
    const suffix = ext === 'md' ? '.md' : ext === 'pdf' ? '.pdf' : '';
    await downloadByApiPath(item.downloadUrl, `${item.title || 'course-resource'}${suffix}`);
  } catch {
    ElMessage.error('下载失败，请稍后重试');
  }
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
  const files = selectedFiles.value;
  if (files.length === 0) {
    ElMessage.warning('请选择要上传的课件文件');
    return;
  }
  if (files.length === 1 && !newResourceTitle.value.trim()) {
    ElMessage.warning('请输入课件资料标题');
    return;
  }

  uploading.value = true;
  uploadProgress.value = 0;
  uploadProgressText.value = '正在上传课件文件至课程专属存储空间...';

  const result = { ...emptyBatchUploadResult() };

  try {
    for (let i = 0; i < files.length; i += 1) {
      const file = files[i];
      const title =
        files.length === 1
          ? newResourceTitle.value.trim()
          : defaultResourceTitleFromFileName(file.name) || file.name;
      const resourceType = inferResourceTypeFromFileName(file.name);

      uploadProgress.value = Math.max(10, Math.round(((i + 0.5) / files.length) * 90));
      uploadProgressText.value =
        files.length === 1
          ? autoSyncRag.value
            ? '正在切片并构建 RAG 向量索引切片...'
            : '正在入库归档...'
          : `正在入库第 ${i + 1}/${files.length} 个课件：${file.name}`;

      await uploadResource(file, {
        title,
        resourceType,
        chapterId: selectedChapterId.value,
        syncToKnowledgeBase: autoSyncRag.value
      });
      result.succeeded += 1;

      uploadProgress.value = Math.round(((i + 1) / files.length) * 100);
    }

    uploadProgressText.value = '完成！';
    await new Promise((r) => setTimeout(r, 150));

    const { level, text } = batchUploadMessage(result, '课件');
    if (level === 'success') {
      ElMessage.success(
        files.length === 1 && autoSyncRag.value ? `${text}，并已同步构建 RAG 向量切片！` : text
      );
    } else if (level === 'warning') {
      ElMessage.warning(text);
    } else {
      ElMessage.error(text);
    }

    showUploadDialog.value = false;
    newResourceTitle.value = '';
    uploadFileList.value = [];
    selectedFiles.value = [];
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

  .resources-panel-card {
    background: #ffffff;
    border: 1px solid #ebf1f7;
    border-radius: 16px;
    box-shadow: 0 2px 12px rgba(30, 80, 150, 0.04);
    overflow: hidden;
  }

  .resources-panel-divider {
    height: 1px;
    background: #ebf1f7;
  }

  // 1. 顶部指标概览
  .resources-stat-overview {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 12px;
    padding: 16px 20px 12px;

    .stat-capsule-card {
      background: #f8fafc;
      border-radius: 14px;
      border: 1px solid #eef2f7;
      padding: 14px 18px;
      display: flex;
      align-items: center;
      gap: 14px;
      transition: background 0.2s ease;

      &:hover {
        background: #f1f5f9;
        transform: none;
        box-shadow: none;
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
    background: transparent;
    border-radius: 0;
    padding: 12px 20px;
    border: none;
    box-shadow: none;
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
    min-height: 200px;
    padding: 8px 20px 16px;

    :deep(.pagination-bar) {
      display: flex;
      justify-content: flex-start;
      align-items: center;
      margin-top: 12px;
      padding-top: 12px;
      border-top: 1px solid #ebf1f7;

      .el-pagination {
        justify-content: flex-start;
        width: 100%;
      }
    }

    .resources-grid {
      display: flex;
      flex-direction: column;
      gap: 12px;

      .resource-card {
        background: #FFFFFF;
        border: 1px solid #EBF1F7;
        border-radius: 14px;
        padding: 12px 16px;
        display: flex;
        flex-direction: row;
        align-items: center;
        gap: 12px;
        transition: all 0.2s ease;
        box-shadow: 0 2px 8px rgba(30, 80, 150, 0.03);

        &:hover {
          border-color: #DBEAFE;
          box-shadow: 0 6px 20px rgba(22, 119, 255, 0.08);
        }

        .card-left-badge {
          width: 44px;
          height: 44px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 11px;
          font-weight: 700;
          letter-spacing: 0.02em;
          flex-shrink: 0;
          text-align: center;
          line-height: 1.1;

          &.badge-type--pdf { background: #FFF1F2; color: #E11D48; }
          &.badge-type--ppt { background: #FEF3C7; color: #D97706; }
          &.badge-type--word { background: #EFF6FF; color: #2563EB; }
          &.badge-type--video { background: #F3E8FF; color: #9333EA; }
          &.badge-type--md { background: #ECFDF5; color: #047857; }
          &.badge-type--txt { background: #F8FAFC; color: #475569; }
          &.badge-type--doc,
          &.badge-type--document { background: #EFF6FF; color: #2563EB; }
        }

        .resource-title {
          margin: 0;
          flex: 1 1 120px;
          min-width: 0;
          font-size: 15px;
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
          gap: 6px;
          flex: 0 0 auto;
          flex-wrap: nowrap;
          white-space: nowrap;

          .pill-sub-tag {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            font-size: 12px;
            color: #64748B;
            background: #F8FAFC;
            padding: 3px 10px;
            border-radius: 9999px;
            border: 1px solid #eef2f7;

            &--time {
              color: #475569;
            }

            &--rag {
              background: #ECFDF5;
              color: #059669;
              font-weight: 500;
              border-color: #bbf7d0;
            }
          }
        }

        .card-actions-row {
          display: flex;
          align-items: center;
          gap: 8px;
          flex: 0 0 auto;
          flex-shrink: 0;
          margin-left: 4px;

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
            font-family: inherit;
            outline: none;

            &:hover {
              border-color: #CBD5E1;
              background: #F8FAFC;
            }

            &:focus-visible {
              box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.25);
            }

            &--preview {
              color: #1677FF;
              border-color: #BFDBFE;
              background: #EFF6FF;
              &:hover { background: #DBEAFE; }
            }

            &--download {
              color: #0F766E;
              border-color: #99F6E4;
              background: #F0FDFA;
              &:hover { background: #CCFBF1; }
            }

            &--del {
              width: 30px;
              padding: 0;
              justify-content: center;
              color: #EF4444;
              border-color: #FECACA;
              background: #FFF5F5;
              &:hover {
                background: #FEE2E2;
                border-color: #FCA5A5;
              }
            }
          }
        }
      }
    }

    .empty-resources-box {
      padding: 48px 0 32px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: transparent;
      border-radius: 0;
      border: none;

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

  .pending-upload-files {
    margin-top: 12px;
    padding: 12px 14px 10px;
    border-radius: 14px;
    border: 1px solid #dbeafe;
    background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);

    &__head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 10px;
      margin-bottom: 10px;
    }

    &__count {
      font-size: 12px;
      font-weight: 600;
      color: #334155;
    }

    &__clear {
      height: 28px;
      padding: 0 12px;
      border: none;
      border-radius: 9999px;
      background: transparent;
      color: #64748b;
      font-size: 12px;
      font-weight: 500;
      cursor: pointer;
      transition: color 0.2s ease, background 0.2s ease;
      font-family: inherit;

      &:hover {
        color: #dc2626;
        background: #fef2f2;
      }

      &:focus-visible {
        outline: none;
        box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.28);
      }
    }

    &__list {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 8px;
      max-height: 168px;
      overflow-y: auto;
    }

    &__item {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 8px 10px;
      border-radius: 10px;
      background: #ffffff;
      border: 1px solid #e2e8f0;
      transition: border-color 0.2s ease, box-shadow 0.2s ease;

      &:hover {
        border-color: #bfdbfe;
        box-shadow: 0 2px 8px rgba(37, 99, 235, 0.06);
      }
    }

    &__file-icon {
      width: 32px;
      height: 32px;
      border-radius: 8px;
      background: #eff6ff;
      color: #2563eb;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    &__name {
      flex: 1;
      min-width: 0;
      font-size: 13px;
      font-weight: 500;
      color: #0f172a;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    &__size {
      flex-shrink: 0;
      height: 22px;
      padding: 0 8px;
      border-radius: 9999px;
      background: #f1f5f9;
      font-size: 11px;
      font-weight: 500;
      color: #64748b;
      line-height: 22px;
    }

    &__remove {
      width: 28px;
      height: 28px;
      padding: 0;
      border: none;
      border-radius: 9999px;
      background: transparent;
      color: #94a3b8;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      transition: color 0.2s ease, background 0.2s ease;
      font-family: inherit;

      &:hover {
        color: #dc2626;
        background: #fef2f2;
      }

      &:focus-visible {
        outline: none;
        box-shadow: 0 0 0 2px rgba(220, 38, 38, 0.25);
      }
    }
  }
}

.upload-detail-form {
  .detected-type-hint {
    margin: 8px 0 0;
    font-size: 12px;
    color: #64748b;
    line-height: 1.5;

    &__badge {
      display: inline-flex;
      align-items: center;
      height: 22px;
      padding: 0 8px;
      margin: 0 4px;
      border-radius: 9999px;
      background: #eff6ff;
      border: 1px solid #bfdbfe;
      color: #2563eb;
      font-size: 11px;
      font-weight: 700;
    }
  }

  .batch-upload-hint {
    margin: 0 0 12px;
    padding: 10px 12px;
    border-radius: 10px;
    background: #eff6ff;
    border: 1px solid #bfdbfe;
    font-size: 12px;
    color: #1d4ed8;
    line-height: 1.5;
  }

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

<style lang="scss">
/* 预览抽屉 teleport 到 body，不可放在 .course-resources-page scoped 内 */
.course-resource-preview-drawer {
  .el-drawer__body {
    display: flex;
    flex-direction: column;
    overflow: hidden;
    padding: 12px 20px 20px;
  }

  .doc-preview-wrapper {
    display: flex;
    flex-direction: column;
    gap: 12px;
    flex: 1;
    min-height: 0;
    height: 100%;

    .doc-preview-meta {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;

      .preview-badge {
        padding: 3px 10px;
        border-radius: 9999px;
        background: #f1f5f9;
        font-size: 12px;
        color: #475569;

        &.is-rag {
          background: #ecfdf5;
          color: #059669;
          font-weight: 600;
        }
      }
    }

    .doc-preview-reader {
      flex: 1;
      min-height: 0;
      display: flex;
      flex-direction: column;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 14px;
      padding: 12px;
      overflow: hidden;

      .reader-content-body {
        flex: 1;
        min-height: min(72vh, 780px);
        overflow-y: auto;
        padding: 20px 24px;
        background: #ffffff;
        border-radius: 12px;
        border: 1px solid #e2e8f0;
      }

      .reader-pdf-frame {
        flex: 1;
        min-height: min(72vh, 780px);
        border-radius: 12px;
        overflow: hidden;
        border: 1px solid #e2e8f0;
        background: #fff;

        iframe {
          width: 100%;
          height: 100%;
          min-height: min(72vh, 780px);
          border: none;
        }
      }

      .reader-empty {
        flex: 1;
        min-height: min(52vh, 520px);
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        text-align: center;
        padding: 32px 24px;
        background: #ffffff;
        border-radius: 12px;
        border: 1px dashed #cbd5e1;

        .reader-empty__icon {
          font-size: 40px;
          color: #94a3b8;
          margin-bottom: 12px;
        }

        .reader-empty__title {
          margin: 0;
          font-size: 15px;
          font-weight: 600;
          color: #334155;
        }
      }
    }

    .preview-bottom-actions {
      display: flex;
      align-items: center;
      justify-content: flex-end;
      gap: 12px;
      flex-shrink: 0;

      .capsule-secondary-btn {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        height: 38px;
        padding: 0 18px;
        border-radius: 9999px;
        background: #f1f5f9;
        color: #334155;
        border: 1px solid #e2e8f0;
        font-size: 13px;
        font-weight: 500;
        cursor: pointer;
        font-family: inherit;

        &:hover {
          background: #e2e8f0;
        }
      }
    }
  }
}
</style>
