<template>
  <div class="question-create-page question-module-page">
    <!-- 顶部统一 Hero 背景大卡片 (ModulePageHeroHeader) -->
    <ModulePageHeroHeader
      :icon="EditPen"
      title="录入新试题"
      :badge="targetBankName ? `已关联题库：${targetBankName}` : '智能命题工作台'"
      description="创建单选、多选、判断、填空或主观推导题，全链路支持 LaTeX 公式编辑、AI 智能生成干扰项与深度教学解析，录入完成后将自动入库。"
    >
      <template #nav>
        <div class="module-page-hero__nav-inner">
          <button type="button" class="module-page-back" @click="handleBack">
            <el-icon><ArrowLeft /></el-icon>
            <span>{{ targetBankName ? `返回题库「${targetBankName}」` : '返回试题列表' }}</span>
          </button>
          <el-breadcrumb separator="/" class="module-page-breadcrumb">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item :to="{ path: '/question/list' }">试题管理</el-breadcrumb-item>
            <el-breadcrumb-item>录入新试题</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
      </template>

      <template #actions>
        <button
          type="button"
          class="module-capsule-btn module-capsule-btn--ai"
          :disabled="aiFullAutoLoading"
          @click="triggerAiFullAuto"
        >
          <el-icon><MagicStick /></el-icon>
          <span>{{ aiFullAutoLoading ? 'AI 装配中...' : 'AI 一键全套智能补全' }}</span>
          <span class="pill-bubble">一键出题</span>
        </button>

        <button
          type="button"
          class="module-capsule-btn module-capsule-btn--secondary"
          @click="triggerAiTutor"
        >
          <el-icon><ChatDotRound /></el-icon>
          <span>咨询 AI 命题助教</span>
        </button>
      </template>
    </ModulePageHeroHeader>

    <!-- 主表单：恢复经典左右双栏排版 -->
    <div class="page-body">
      <QuestionForm
        ref="questionFormRef"
        v-model="currentFormData"
        :submitting="submitting"
        @submit="handleFormSubmit"
        @cancel="handleBack"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft, EditPen, MagicStick, ChatDotRound } from '@element-plus/icons-vue';
import ModulePageHeroHeader from '@/components/question/common/ModulePageHeroHeader.vue';
import QuestionForm from '@/components/question/QuestionForm.vue';
import { useQuestion } from '@/composables/question/useQuestion';
import { addQuestionsToBank } from '@/api/question/question-bank';
import type { Question } from '@/types/question/question';

import { useAuthStore } from '@/stores/auth/auth';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const { saveQuestion } = useQuestion();
const submitting = ref(false);
const questionFormRef = ref<any>(null);
const aiFullAutoLoading = ref(false);

const canUseAi = computed(() => {
  return authStore.hasAnyRole(['ADMIN', 'TEACHER']) || authStore.hasPermission(['ai:chat', 'ai:question']);
});

const targetBankId = computed(() => {
  const val = route.query.bankId;
  return val ? Number(val) : null;
});

const targetBankName = computed(() => {
  return route.query.bankName ? decodeURIComponent(String(route.query.bankName)) : '';
});

const targetCourseId = computed(() => {
  const val = route.query.courseId;
  return val ? Number(val) : undefined;
});

function inferCourseName(courseId?: number, bankName?: string): string {
  if (courseId === 102 || courseId === 258) return 'Java面向对象程序设计';
  if (courseId === 101) return '数据结构与算法';
  if (courseId === 103) return '高等数学（上）';
  if (bankName && bankName.includes('Java')) return 'Java面向对象程序设计';
  if (bankName && bankName.includes('数据结构')) return '数据结构与算法';
  if (bankName && bankName.includes('数学')) return '高等数学（上）';
  return 'Java面向对象程序设计';
}

const initialForm = computed<Partial<Question>>(() => ({
  courseId: targetCourseId.value,
  courseName: inferCourseName(targetCourseId.value, targetBankName.value),
  type: 'SINGLE_CHOICE',
  difficulty: 'MEDIUM',
  score: 5,
  stem: ''
}));

const currentFormData = ref<Partial<Question>>({ ...initialForm.value });

const hasStem = computed(() => {
  const stem = currentFormData.value?.stem || questionFormRef.value?.getStem();
  return Boolean(stem && stem.trim().length >= 2);
});

function handleBack() {
  if (targetBankId.value) {
    router.push(`/question/banks/${targetBankId.value}`);
  } else {
    router.push('/question/list');
  }
}

async function triggerAiFullAuto() {
  if (!canUseAi.value) {
    ElMessage.warning('抱歉，当前账号暂无 AI 智能命题权限');
    return;
  }
  const stem = (currentFormData.value?.stem || questionFormRef.value?.getStem() || '').trim();
  if (!stem) {
    ElMessage.warning('题目尚未输入任何信息，请先在下方输入试题题干内容，AI 才能进行全套智能补全！');
    questionFormRef.value?.focusStem?.();
    return;
  }
  if (questionFormRef.value?.triggerFullAuto) {
    aiFullAutoLoading.value = true;
    try {
      await questionFormRef.value.triggerFullAuto();
    } finally {
      aiFullAutoLoading.value = false;
    }
  }
}

function triggerAiTutor() {
  if (!canUseAi.value) {
    ElMessage.warning('抱歉，当前账号暂无 AI 命题助教咨询权限');
    return;
  }
  const stem = (currentFormData.value?.stem || questionFormRef.value?.getStem() || '').trim();
  if (!stem) {
    ElMessage.warning('题目尚未输入任何信息，请先在下方输入试题题干后再咨询 AI 命题助教！');
    questionFormRef.value?.focusStem?.();
    return;
  }
  if (questionFormRef.value?.triggerAiTutor) {
    questionFormRef.value.triggerAiTutor();
  }
}

async function handleFormSubmit(formData: Partial<Question>) {
  submitting.value = true;
  try {
    const newId = await saveQuestion(formData);
    if (targetBankId.value && newId) {
      try {
        await addQuestionsToBank(targetBankId.value, [newId]);
        ElMessage.success(`试题录入入库成功，并已自动加入题库「${targetBankName.value || '当前题库'}」！`);
      } catch (err: any) {
        ElMessage.warning(`试题创建成功，但自动关联题库失败：${err?.message || '请手动添加'}`);
      }
      router.push(`/question/banks/${targetBankId.value}`);
    } else {
      ElMessage.success('试题录入入库成功！');
      router.push('/question/list');
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '保存试题失败，请检查必填项');
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/question/module-page-shell.scss';

.question-create-page {
  display: flex;
  flex-direction: column;
  gap: 20px;

  .page-body {
    width: 100%;
  }
}
</style>
