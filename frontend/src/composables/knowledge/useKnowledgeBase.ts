import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import type { UploadFile, UploadFiles } from 'element-plus';
import {
  getKnowledgeBases,
  getKnowledgeBaseDetail,
  createKnowledgeBase,
  updateKnowledgeBase,
  deleteKnowledgeBase
} from '@/api/knowledge/knowledge-base';
import { uploadDocument } from '@/api/knowledge/document';
import { getCourseList } from '@/api/course/course';
import { batchUploadMessage, emptyBatchUploadResult } from '@/utils/upload/coalesce-upload-files';
import { KnowledgeBase } from '@/types/knowledge/knowledge-base';
import type { Course } from '@/types/course/course';
import { mapKnowledgeBaseVectorStatus } from '@/utils/knowledge/knowledge-base-status';

export function useKnowledgeBase() {
  const knowledgeBases = ref<KnowledgeBase[]>([]);
  const currentKnowledgeBase = ref<KnowledgeBase | null>(null);
  const loading = ref(false);
  const total = ref(0);

  async function fetchKnowledgeBases(params: Record<string, any> = {}) {
    loading.value = true;
    try {
      const res = await getKnowledgeBases(params);
      knowledgeBases.value = (res.data || []).map(normalizeKnowledgeBase);
      total.value = knowledgeBases.value.length;
    } catch (err) {
      knowledgeBases.value = [];
      total.value = 0;
      throw err;
    } finally {
      loading.value = false;
    }
  }

  async function fetchKnowledgeBaseDetail(id: number) {
    loading.value = true;
    try {
      const res = await getKnowledgeBaseDetail(id);
      currentKnowledgeBase.value = res.data ? normalizeKnowledgeBase(res.data as Record<string, any>) : null;
    } catch (err) {
      currentKnowledgeBase.value = null;
      throw err;
    } finally {
      loading.value = false;
    }
    return currentKnowledgeBase.value;
  }

  async function create(data: Record<string, any>) {
    const res = await createKnowledgeBase(data);
    return res.data;
  }

  async function update(id: number, data: Record<string, any>) {
    await updateKnowledgeBase(id, data);
    await fetchKnowledgeBaseDetail(id);
  }

  async function remove(id: number) {
    await deleteKnowledgeBase(id);
    knowledgeBases.value = knowledgeBases.value.filter(k => k.id !== id);
    total.value = knowledgeBases.value.length;
  }

  return {
    knowledgeBases,
    currentKnowledgeBase,
    loading,
    total,
    fetchKnowledgeBases,
    fetchKnowledgeBaseDetail,
    create,
    update,
    remove
  };
}

export function useKnowledgeBaseCreate() {
  const router = useRouter();
  const formRef = ref<FormInstance>();
  const submitting = ref(false);
  const courses = ref<Course[]>([]);
  const initialUploadFiles = ref<File[]>([]);

  const formData = reactive({
    name: '',
    // 课程由「第一门真实课程」回填（见 loadCourses），不再写死某个课程 id
    courseId: undefined as number | undefined,
    description: '',
    embeddingModel: 'bge-large-zh-v1.5',
    chunkStrategy: 'PARAGRAPH',
    chunkSize: 500,
    chunkOverlap: 50
  });

  const rules = reactive<FormRules>({
    name: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }],
    courseId: [{ required: true, message: '请选择所属课程', trigger: 'change' }]
  });

  async function loadCourses() {
    try {
      const res = await getCourseList({ page: 1, pageSize: 50 });
      const rawList = res.data?.list || [];
      courses.value = rawList.map((c: any) => ({
        ...c,
        title: c.name || c.title || `课程 #${c.id}`,
        name: c.name || c.title || `课程 #${c.id}`
      }));
      // 默认选中第一门真实课程，避免表单停留在写死的课程 id 上
      if (!formData.courseId && courses.value.length > 0) {
        formData.courseId = Number(courses.value[0].id);
      }
    } catch {
      // 课程接口不可用时不再兜底到写死的课程（原 FALLBACK_COURSES 含 101/102/103），
      // 避免把知识库误绑到并不存在的课程上。
      courses.value = [];
    }
  }

  function syncInitialUploadFiles(uploadFiles: UploadFiles) {
    const files: File[] = [];
    for (const item of uploadFiles) {
      if (item.raw) {
        files.push(item.raw);
      }
    }
    initialUploadFiles.value = files;
  }

  function handleInitialUploadChange(_uploadFile: UploadFile, uploadFiles: UploadFiles) {
    syncInitialUploadFiles(uploadFiles);
  }

  function handleInitialUploadRemove(_uploadFile: UploadFile, uploadFiles: UploadFiles) {
    syncInitialUploadFiles(uploadFiles);
  }

  function resolveCreatedKnowledgeBaseId(data: unknown): number | undefined {
    if (typeof data === 'number' && Number.isFinite(data)) {
      return data;
    }
    if (data && typeof data === 'object' && 'id' in data) {
      const id = Number((data as { id?: unknown }).id);
      return Number.isFinite(id) ? id : undefined;
    }
    return undefined;
  }

  async function uploadInitialDocuments(knowledgeBaseId: number) {
    if (initialUploadFiles.value.length === 0) {
      return emptyBatchUploadResult();
    }
    const result = { ...emptyBatchUploadResult() };
    for (const file of initialUploadFiles.value) {
      try {
        await uploadDocument(knowledgeBaseId, file);
        result.succeeded += 1;
      } catch {
        result.failed += 1;
        result.failedNames.push(file.name);
      }
    }
    return result;
  }

  async function handleSubmit() {
    if (!formRef.value) return;
    await formRef.value.validate(async (valid) => {
      if (!valid) return;
      submitting.value = true;
      try {
        const res = await createKnowledgeBase(formData);
        const newId = resolveCreatedKnowledgeBaseId(res.data) ?? 1;
        const uploadResult = await uploadInitialDocuments(newId);
        if (uploadResult.succeeded + uploadResult.failed > 0) {
          const { level, text } = batchUploadMessage(uploadResult, '初始文档');
          if (level === 'success') {
            ElMessage.success(`知识库已创建，${text}`);
          } else if (level === 'warning') {
            ElMessage.warning(`知识库已创建，${text}`);
          } else {
            ElMessage.warning(`知识库已创建，但${text}`);
          }
        } else {
          ElMessage.success('知识库已成功创建！正在为您跳转到文档维护详情页...');
        }
        setTimeout(() => {
          router.push(`/knowledge/${newId}`);
        }, 600);
      } catch (err) {
        console.error(err);
        ElMessage.error('知识库创建失败，请稍后重试');
      } finally {
        submitting.value = false;
      }
    });
  }

  onMounted(loadCourses);

  return {
    router,
    formRef,
    submitting,
    courses,
    formData,
    rules,
    initialUploadFiles,
    handleInitialUploadChange,
    handleInitialUploadRemove,
    loadCourses,
    handleSubmit
  };
}

function normalizeKnowledgeBase(raw: Record<string, any>): KnowledgeBase {
  const indexMapped = mapKnowledgeBaseVectorStatus(
    raw.vectorStatus || raw.indexStatus
  );
  return {
    id: Number(raw.id),
    name: raw.name || '',
    description: raw.description || '',
    category: raw.category || 'MAJOR',
    categoryLabel: raw.categoryLabel || '专业核心',
    documentCount: Number(raw.docCount ?? raw.documentCount ?? 0),
    chunkCount: Number(raw.chunkCount ?? 0),
    vectorStatus: indexMapped.vectorStatus,
    vectorStatusLabel: indexMapped.vectorStatusLabel,
    vectorProgress: raw.vectorProgress ?? 0,
    embeddingModel: raw.embeddingModel || '',
    updatedAt: raw.updateTime || raw.updatedAt || '',
    courseId: raw.courseId,
    courseName: raw.courseName
  };
}
