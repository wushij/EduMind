<template>
  <el-dialog v-model="visible" title="编辑教学团队展示" width="600px" destroy-on-close>
    <div class="toolbar">
      <button type="button" class="table-action-pill table-action-pill--primary" @click="emit('sync')">
        从成员同步
      </button>
    </div>
    <div v-for="row in rows" :key="row.userId" class="instructor-row">
      <div class="instructor-name">{{ row.label }}</div>
      <el-input v-model="row.intro" type="textarea" :rows="2" placeholder="本课程教师简介" maxlength="1000" />
      <el-input v-model="row.officeHours" placeholder="答疑时间，如：每周二 14:00-16:00" maxlength="200" />
    </div>
    <el-empty v-if="rows.length === 0" description="请先同步教学团队成员" />
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import type { CourseInstructorCardVO } from '@/types/course/overview';

const props = defineProps<{
  modelValue: boolean;
  instructors: CourseInstructorCardVO[];
  saving?: boolean;
}>();

const emit = defineEmits<{
  'update:modelValue': [boolean];
  save: [items: Array<{ userId: number; intro?: string; officeHours?: string; sortOrder?: number; primary?: boolean }>];
  sync: [];
}>();

const visible = ref(props.modelValue);
const rows = ref<Array<{
  userId: number;
  label: string;
  intro?: string;
  officeHours?: string;
  sortOrder?: number;
  primary?: boolean;
}>>([]);

watch(() => props.modelValue, (v) => {
  visible.value = v;
  if (v) {
    rows.value = props.instructors.map(i => ({
      userId: i.userId,
      label: `${i.realName || i.username || i.userId}（${i.roleLabel || '教师'}）`,
      intro: i.intro || '',
      officeHours: i.officeHours || '',
      sortOrder: i.sortOrder,
      primary: i.primary
    }));
  }
});

watch(visible, (v) => emit('update:modelValue', v));

function submit() {
  emit('save', rows.value.map(r => ({
    userId: r.userId,
    intro: r.intro,
    officeHours: r.officeHours,
    sortOrder: r.sortOrder,
    primary: r.primary
  })));
}
</script>

<style scoped lang="scss">
@import './course-overview-shared.scss';

.toolbar {
  margin-bottom: 12px;
}

.instructor-row {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  flex-direction: column;
  gap: 8px;

  .instructor-name {
    font-size: 14px;
    font-weight: 600;
    color: #0f172a;
  }
}
</style>
