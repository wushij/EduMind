<template>
  <div class="bank-list-page-container">
    <!-- 顶部操作头区 -->
    <div class="bank-header-dock">
      <div class="header-left">
        <div class="title-with-icon">
          <span class="header-icon">🗂️</span>
          <h1 class="main-title">课程与通用题库中心</h1>
          <span class="capsule-count-tag">共 {{ banks.length }} 个精选题库</span>
        </div>
        <p class="sub-desc">
          归纳整理各课程专项试题集、历年统考与期中期末题库，支持快捷组卷与试题穿梭调度。
        </p>
      </div>

      <div class="header-right-actions">
        <button
          type="button"
          class="capsule-btn capsule-btn--primary"
          @click="showCreateDialog = true"
        >
          <span>➕ 新建题库</span>
        </button>
      </div>
    </div>

    <!-- 课程筛选行 -->
    <div class="filter-capsule-card">
      <div class="filter-row">
        <span class="filter-label">所属课程：</span>
        <div class="pill-tags-track">
          <span
            class="filter-pill-tag"
            :class="{ active: selectedCourseId === null }"
            @click="selectedCourseId = null"
          >
            全部课程
          </span>
          <span
            v-for="c in courses"
            :key="c.id"
            class="filter-pill-tag"
            :class="{ active: selectedCourseId === c.id }"
            @click="selectedCourseId = c.id"
          >
            {{ c.title }}
          </span>
        </div>
      </div>
    </div>

    <!-- 题库卡片网格 -->
    <div v-loading="loading" class="banks-grid-wrapper">
      <div v-if="filteredBanks.length > 0" class="banks-grid">
        <div
          v-for="b in filteredBanks"
          :key="b.id"
          class="bank-card-item"
          @click="router.push(`/question/banks/${b.id}`)"
        >
          <div class="card-top-header">
            <div class="course-chip">{{ b.courseName || getCourseName(b.courseId) }}</div>
            <span class="count-tag">{{ b.questionCount || 0 }} 题</span>
          </div>

          <h3 class="bank-name">{{ b.name }}</h3>
          <p class="bank-desc">{{ b.description || '暂无详细描述信息' }}</p>

          <div class="card-footer">
            <span class="date-text">更新于 {{ b.updateTime ? b.updateTime.slice(0, 10) : '近期' }}</span>
            <el-button type="primary" link size="small">
              进入题库维护 ➔
            </el-button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state-panel">
        <span class="empty-icon">🗂️</span>
        <h3>暂无匹配的题库</h3>
        <p>您可以点击右上角“新建题库”为您的课程创建首个专属试题库。</p>
        <el-button type="primary" @click="showCreateDialog = true">立即新建题库</el-button>
      </div>
    </div>

    <!-- 新建题库对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="新建试题库"
      width="540px"
      destroy-on-close
    >
      <el-form ref="dialogFormRef" :model="newBankForm" :rules="dialogRules" label-position="top">
        <el-form-item label="题库名称" prop="name">
          <el-input v-model="newBankForm.name" placeholder="例如：2026秋季数据结构期末高频冲刺库" />
        </el-form-item>

        <el-form-item label="关联课程" prop="courseId">
          <el-select v-model="newBankForm.courseId" placeholder="请选择对应课程" class="w-full">
            <el-option
              v-for="c in courses"
              :key="c.id"
              :label="c.title"
              :value="c.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="题库说明">
          <el-input
            v-model="newBankForm.description"
            type="textarea"
            :rows="3"
            placeholder="简要说明本题库的收录范围与考察重点..."
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateBank">
          确认创建
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { getQuestionBanks, createQuestionBank } from '@/api/question/question-bank';
import { getCourseList } from '@/api/course/course';
import type { Course } from '@/types/course/course';

const router = useRouter();
const loading = ref(false);
const creating = ref(false);
const showCreateDialog = ref(false);
const dialogFormRef = ref<FormInstance>();

const banks = ref<any[]>([]);
const courses = ref<Course[]>([]);
const selectedCourseId = ref<number | null>(null);

const newBankForm = reactive({
  name: '',
  courseId: undefined as number | undefined,
  description: ''
});

const dialogRules: FormRules = {
  name: [{ required: true, message: '请输入题库名称', trigger: 'blur' }],
  courseId: [{ required: true, message: '请选择关联课程', trigger: 'change' }]
};

const filteredBanks = computed(() => {
  if (selectedCourseId.value === null) {
    return banks.value;
  }
  return banks.value.filter(b => b.courseId === selectedCourseId.value);
});

onMounted(async () => {
  await Promise.all([loadCourses(), loadBanks()]);
});

async function loadCourses() {
  try {
    const res = await getCourseList({ page: 1, pageSize: 50 });
    courses.value = res.data?.list || [];
  } catch (err) {
    console.error('加载课程失败', err);
  }
}

async function loadBanks() {
  loading.value = true;
  try {
    const res = await getQuestionBanks({ page: 1, pageSize: 50 });
    banks.value = res.data?.list || [
      {
        id: 1,
        name: '数据结构核心真题库',
        courseId: 101,
        courseName: '数据结构与算法',
        questionCount: 5,
        description: '涵盖全国统考408与期末高频真题，包括线性表、树与排序算法',
        updateTime: '2026-09-10'
      },
      {
        id: 2,
        name: 'Java面向对象精选题集',
        courseId: 102,
        courseName: 'Java程序设计',
        questionCount: 3,
        description: 'Java基础语法、面向对象、集合框架与异常处理典型题型',
        updateTime: '2026-09-09'
      },
      {
        id: 3,
        name: '高等数学期末测试真题库',
        courseId: 103,
        courseName: '高等数学（上）',
        questionCount: 3,
        description: '极限、连续、导数与微积分计算经典测试题',
        updateTime: '2026-09-08'
      }
    ];
  } catch {
    // 降级演示数据
    banks.value = [
      { id: 1, name: '数据结构核心真题库', courseId: 101, questionCount: 5, description: '涵盖408与期末高频真题', updateTime: '2026-09-10' },
      { id: 2, name: 'Java面向对象精选题集', courseId: 102, questionCount: 3, description: 'Java核心典型题型', updateTime: '2026-09-09' },
      { id: 3, name: '高等数学期末测试真题库', courseId: 103, questionCount: 3, description: '微积分计算经典测试题', updateTime: '2026-09-08' }
    ];
  } finally {
    loading.value = false;
  }
}

function getCourseName(courseId: number): string {
  const c = courses.value.find(item => item.id === courseId);
  return c ? c.title : '专业核心课';
}

async function handleCreateBank() {
  if (!dialogFormRef.value) return;
  await dialogFormRef.value.validate(async (valid) => {
    if (valid) {
      creating.value = true;
      try {
        await createQuestionBank(newBankForm);
        ElMessage.success('题库创建成功！');
        showCreateDialog.value = false;
        newBankForm.name = '';
        newBankForm.description = '';
        await loadBanks();
      } catch (err: any) {
        ElMessage.error(err?.message || '创建题库失败');
      } finally {
        creating.value = false;
      }
    }
  });
}
</script>

<style scoped lang="scss">
.bank-list-page-container {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;

  .bank-header-dock {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 16px;
    padding: 24px 32px;
    margin-bottom: 20px;
    box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);

    .header-left {
      .title-with-icon {
        display: flex;
        align-items: center;
        gap: 12px;

        .header-icon {
          font-size: 24px;
        }

        .main-title {
          font-size: 22px;
          font-weight: 700;
          color: #0f172a;
          margin: 0;
        }

        .capsule-count-tag {
          font-size: 12px;
          color: #2563eb;
          background: #eff6ff;
          padding: 3px 10px;
          border-radius: 9999px;
          font-weight: 600;
        }
      }

      .sub-desc {
        margin: 8px 0 0 0;
        font-size: 14px;
        color: #64748b;
      }
    }

    .capsule-btn--primary {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 10px 22px;
      border-radius: 9999px;
      background: #2563eb;
      color: #ffffff;
      border: none;
      font-weight: 600;
      cursor: pointer;
      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
      transition: all 0.2s;

      &:hover {
        background: #1d4ed8;
        transform: translateY(-1px);
      }
    }
  }

  .filter-capsule-card {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    padding: 14px 24px;
    margin-bottom: 24px;

    .filter-row {
      display: flex;
      align-items: center;
      gap: 12px;

      .filter-label {
        font-size: 13px;
        font-weight: 600;
        color: #475569;
        white-space: nowrap;
      }

      .pill-tags-track {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;

        .filter-pill-tag {
          padding: 4px 14px;
          border-radius: 9999px;
          font-size: 13px;
          color: #64748b;
          background: #f1f5f9;
          cursor: pointer;
          transition: all 0.15s;

          &:hover {
            background: #e2e8f0;
            color: #0f172a;
          }

          &.active {
            background: #2563eb;
            color: #ffffff;
            font-weight: 600;
          }
        }
      }
    }
  }

  .banks-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
    gap: 20px;
  }

  .bank-card-item {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 14px;
    padding: 20px 24px;
    cursor: pointer;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    display: flex;
    flex-direction: column;

    &:hover {
      border-color: #93c5fd;
      transform: translateY(-3px);
      box-shadow: 0 12px 24px -6px rgba(37, 99, 235, 0.08);
    }

    .card-top-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      .course-chip {
        font-size: 12px;
        font-weight: 600;
        color: #475569;
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        padding: 2px 10px;
        border-radius: 6px;
      }

      .count-tag {
        font-size: 12px;
        color: #10b981;
        background: #ecfdf5;
        padding: 2px 8px;
        border-radius: 4px;
        font-weight: 600;
      }
    }

    .bank-name {
      font-size: 17px;
      font-weight: 700;
      color: #0f172a;
      margin: 0 0 8px 0;
      line-height: 1.4;
    }

    .bank-desc {
      font-size: 13px;
      color: #64748b;
      margin: 0 0 18px 0;
      line-height: 1.6;
      flex: 1;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .card-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-top: 1px dashed #f1f5f9;
      padding-top: 14px;

      .date-text {
        font-size: 12px;
        color: #94a3b8;
      }
    }
  }

  .empty-state-panel {
    text-align: center;
    padding: 60px 20px;
    background: #ffffff;
    border-radius: 16px;
    border: 1px dashed #cbd5e1;

    .empty-icon {
      font-size: 44px;
      display: block;
      margin-bottom: 12px;
    }

    h3 {
      font-size: 18px;
      color: #0f172a;
      margin-bottom: 6px;
    }

    p {
      font-size: 14px;
      color: #64748b;
      margin-bottom: 20px;
    }
  }

  .w-full {
    width: 100%;
  }
}
</style>
