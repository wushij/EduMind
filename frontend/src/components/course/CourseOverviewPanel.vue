<template>
  <div v-loading="loading" class="course-overview-page">
    <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" class="overview-error" />

    <div v-if="overview" class="overview-grid">
      <div class="overview-main-col">
        <CourseOverviewIntroCard
          :course="overview.course"
          :capability-tags="overview.capabilityTags"
          :editable="overview.editable"
          @edit-profile="showEditDrawer = true"
        />
        <CourseOverviewObjectivesCard
          :objectives="overview.objectives"
          :editable="overview.editable"
          @manage="showObjectiveDialog = true"
          @ai-lesson="goAiLessonPlan"
        />
        <CourseOverviewAnnouncementsCard
          :announcements="overview.announcementsPreview"
          :total="overview.announcementTotal"
          :editable="overview.editable"
          @publish="showAnnouncementDialog = true"
          @view-all="showAnnouncementDrawer = true"
        />
      </div>

      <div class="overview-side-col">
        <CourseOverviewStatsSidebar
          :course="overview.course"
          :editable="overview.editable"
          @edit-profile="showEditDrawer = true"
          @knowledge-base="handleKnowledgeBaseNavigate"
        />
        <CourseOverviewInstructorsCard
          :instructors="overview.instructors"
          :editable="overview.editable"
          @edit="showInstructorDialog = true"
        />
      </div>
    </div>

    <CourseEditDrawer
      v-model="showEditDrawer"
      :course="overview?.course || course || null"
      @saved="handleProfileSaved"
    />

    <CourseObjectiveEditorDialog
      v-model="showObjectiveDialog"
      :course-id="courseId"
      :objectives="overview?.objectives || []"
      :saving="actionSaving"
      @save="handleSaveObjectives"
    />

    <CourseAnnouncementEditorDialog
      v-model="showAnnouncementDialog"
      :saving="actionSaving"
      @publish="handlePublishAnnouncement"
    />

    <CourseAnnouncementListDrawer
      ref="announcementDrawerRef"
      v-model="showAnnouncementDrawer"
      :course-id="courseId"
      :editable="overview?.editable"
      @withdraw="handleWithdrawAnnouncement"
    />

    <CourseInstructorEditorDialog
      v-model="showInstructorDialog"
      :instructors="overview?.instructors || []"
      :saving="actionSaving"
      @sync="handleSyncInstructors"
      @save="handleSaveInstructors"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { Course } from '@/types/course/course';
import CourseEditDrawer from '@/components/course/CourseEditDrawer.vue';
import CourseOverviewIntroCard from '@/components/course/overview/CourseOverviewIntroCard.vue';
import CourseOverviewObjectivesCard from '@/components/course/overview/CourseOverviewObjectivesCard.vue';
import CourseOverviewAnnouncementsCard from '@/components/course/overview/CourseOverviewAnnouncementsCard.vue';
import CourseOverviewInstructorsCard from '@/components/course/overview/CourseOverviewInstructorsCard.vue';
import CourseOverviewStatsSidebar from '@/components/course/overview/CourseOverviewStatsSidebar.vue';
import CourseObjectiveEditorDialog from '@/components/course/overview/CourseObjectiveEditorDialog.vue';
import CourseAnnouncementEditorDialog from '@/components/course/overview/CourseAnnouncementEditorDialog.vue';
import CourseAnnouncementListDrawer from '@/components/course/overview/CourseAnnouncementListDrawer.vue';
import CourseInstructorEditorDialog from '@/components/course/overview/CourseInstructorEditorDialog.vue';
import { useCourseOverview } from '@/composables/course/useCourseOverview';

const props = defineProps<{
  course?: Course | null;
}>();

const router = useRouter();
const courseId = computed(() => props.course?.id);

const {
  overview,
  loading,
  error,
  fetchOverview,
  saveObjectives,
  publishAnnouncement,
  withdrawAnnouncement,
  saveInstructors,
  syncInstructors
} = useCourseOverview(courseId);

const showEditDrawer = ref(false);
const showObjectiveDialog = ref(false);
const showAnnouncementDialog = ref(false);
const showAnnouncementDrawer = ref(false);
const announcementDrawerRef = ref<{ reload: () => Promise<void> } | null>(null);
const showInstructorDialog = ref(false);
const actionSaving = ref(false);

function goAiLessonPlan() {
  const id = courseId.value;
  if (!id) return;
  const name = overview.value?.course?.name ?? props.course?.name ?? '';
  router.push({
    path: '/ai/lesson',
    query: { courseId: String(id), topic: name }
  });
}

function handleKnowledgeBaseNavigate() {
  const kbId = overview.value?.course?.knowledgeBaseId ?? props.course?.knowledgeBaseId;
  if (kbId && kbId > 0) {
    router.push(`/knowledge/${kbId}/documents`);
    return;
  }
  ElMessage.info('当前课程尚未绑定知识库，请先在课程档案中完成关联');
  router.push('/knowledge');
}

async function handleProfileSaved() {
  await fetchOverview();
}

async function handleSaveObjectives(items: Array<{ title: string; description?: string }>) {
  actionSaving.value = true;
  try {
    await saveObjectives({ objectives: items });
    showObjectiveDialog.value = false;
    ElMessage.success('教学目标已保存');
  } catch {
    ElMessage.error('保存失败');
  } finally {
    actionSaving.value = false;
  }
}

async function handlePublishAnnouncement(payload: { title: string; content: string; pinned?: boolean }) {
  actionSaving.value = true;
  try {
    await publishAnnouncement(payload);
    showAnnouncementDialog.value = false;
    ElMessage.success('公告已发布，已通知课程成员');
  } catch {
    ElMessage.error('发布失败');
  } finally {
    actionSaving.value = false;
  }
}

async function handleWithdrawAnnouncement(id: number) {
  try {
    await ElMessageBox.confirm('确定撤回该公告吗？', '撤回确认', { type: 'warning' });
    await withdrawAnnouncement(id);
    await announcementDrawerRef.value?.reload();
    ElMessage.success('公告已撤回');
  } catch {
    // cancelled
  }
}

async function handleSaveInstructors(
  items: Array<{ userId: number; intro?: string; officeHours?: string; sortOrder?: number; primary?: boolean }>
) {
  actionSaving.value = true;
  try {
    await saveInstructors({ instructors: items });
    showInstructorDialog.value = false;
    ElMessage.success('教学团队信息已保存');
  } catch {
    ElMessage.error('保存失败');
  } finally {
    actionSaving.value = false;
  }
}

async function handleSyncInstructors() {
  actionSaving.value = true;
  try {
    await syncInstructors();
    ElMessage.success('已从课程成员同步教学团队');
  } catch {
    ElMessage.error('同步失败');
  } finally {
    actionSaving.value = false;
  }
}
</script>

<style scoped lang="scss">
.course-overview-page {
  .overview-error {
    margin-bottom: 16px;
  }

  .overview-grid {
    display: grid;
    grid-template-columns: 1fr 340px;
    gap: 20px;
    align-items: start;
  }
}

@media (max-width: 960px) {
  .course-overview-page .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
