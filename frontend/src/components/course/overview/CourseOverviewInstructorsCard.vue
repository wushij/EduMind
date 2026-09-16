<template>
  <div class="content-card teacher-card">
    <div class="card-header-row">
      <div class="card-header-left">
        <el-icon class="card-icon card-icon--purple"><User /></el-icon>
        <h3 class="card-title">教学团队</h3>
      </div>
      <button
        v-if="editable"
        type="button"
        class="table-action-pill table-action-pill--primary"
        @click="emit('edit')"
      >
        编辑展示
      </button>
    </div>

    <template v-if="primaryInstructor">
      <div class="teacher-header">
        <div class="teacher-large-avatar">
          <img
            v-if="avatarUrl(primaryInstructor) && !brokenAvatar"
            :src="avatarUrl(primaryInstructor)"
            alt=""
            class="avatar-img"
            @error="brokenAvatar = true"
          />
          <span v-else>{{ displayName(primaryInstructor).slice(0, 1) }}</span>
        </div>
        <div class="teacher-title-meta">
          <h4 class="teacher-name">{{ displayName(primaryInstructor) }}</h4>
          <span class="teacher-dept">{{ primaryInstructor.roleLabel || '主讲教师' }}</span>
        </div>
      </div>
      <p v-if="primaryInstructor.intro" class="teacher-bio">{{ primaryInstructor.intro }}</p>
      <p v-else class="teacher-bio teacher-bio--muted">
        {{ editable ? '可补充本课程教师简介与答疑说明。' : '教师尚未填写课程答疑说明。' }}
      </p>
      <div v-if="primaryInstructor.officeHours" class="office-hour">
        <span class="oh-label">答疑时间：</span>
        <span>{{ primaryInstructor.officeHours }}</span>
      </div>
    </template>
    <div v-else class="empty-block">
      <el-icon class="empty-icon"><User /></el-icon>
      <p class="empty-text">暂无教学团队信息</p>
    </div>

    <div v-if="assistants.length > 0" class="assistant-list">
      <div v-for="a in assistants" :key="a.userId" class="assistant-row">
        <span class="assistant-name">{{ displayName(a) }}</span>
        <span class="assistant-role">{{ a.roleLabel }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { User } from '@element-plus/icons-vue';
import type { CourseInstructorCardVO } from '@/types/course/overview';
import { normalizeAvatarUrl } from '@/utils/format/file';

const props = defineProps<{
  instructors: CourseInstructorCardVO[];
  editable?: boolean;
}>();

const emit = defineEmits<{ edit: [] }>();

const brokenAvatar = ref(false);

const primaryInstructor = computed(() =>
  props.instructors.find(i => i.primary) || props.instructors[0]
);

const assistants = computed(() =>
  props.instructors.filter(i => i.userId !== primaryInstructor.value?.userId)
);

function displayName(i: CourseInstructorCardVO) {
  return i.realName || i.username || `用户${i.userId}`;
}

function avatarUrl(i: CourseInstructorCardVO) {
  return normalizeAvatarUrl(i.avatar);
}
</script>

<style scoped lang="scss">
@import './course-overview-shared.scss';

.teacher-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;

  .teacher-large-avatar {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    background: #1677ff;
    color: #fff;
    font-size: 20px;
    font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;

    .avatar-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .teacher-name {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    color: #0f172a;
  }

  .teacher-dept {
    font-size: 12px;
    color: #64748b;
  }
}

.teacher-bio {
  font-size: 13px;
  color: #475569;
  line-height: 1.6;
  margin: 0 0 14px;

  &--muted { color: #94a3b8; }
}

.office-hour {
  font-size: 12px;
  color: #64748b;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;

  .oh-label {
    font-weight: 600;
    color: #334155;
  }
}

.assistant-list {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e2e8f0;

  .assistant-row {
    display: flex;
    justify-content: space-between;
    font-size: 12.5px;
    padding: 6px 0;
    color: #475569;

    .assistant-role {
      color: #94a3b8;
    }
  }
}
</style>
