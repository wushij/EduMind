<template>
  <el-drawer v-model="visible" title="课程公告" size="480px" destroy-on-close>
    <div v-loading="loading" class="announcement-drawer-body">
      <div v-for="item in list" :key="item.id" class="announcement-row">
        <div class="row-head">
          <strong>{{ item.title }}</strong>
          <span v-if="item.pinned" class="pin">置顶</span>
        </div>
        <p class="row-content">{{ item.content }}</p>
        <div class="row-meta">{{ item.publishTime }} · {{ item.publisherName || '教师' }}</div>
        <div v-if="editable" class="row-actions">
          <button type="button" class="table-action-pill table-action-pill--primary" @click="emit('withdraw', item.id)">
            撤回
          </button>
        </div>
      </div>
      <el-empty v-if="!loading && list.length === 0" description="暂无公告" />
    </div>
    <div class="drawer-pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="load"
      />
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { pageCourseAnnouncements } from '@/api/course/overview';
import type { CourseAnnouncementVO } from '@/types/course/overview';

const props = defineProps<{
  modelValue: boolean;
  courseId?: number;
  editable?: boolean;
}>();

const emit = defineEmits<{
  'update:modelValue': [boolean];
  withdraw: [id: number];
}>();

const visible = ref(props.modelValue);
const loading = ref(false);
const list = ref<CourseAnnouncementVO[]>([]);
const page = ref(1);
const pageSize = ref(10);
const total = ref(0);

watch(() => props.modelValue, (v) => {
  visible.value = v;
  if (v && props.courseId) {
    page.value = 1;
    load();
  }
});

watch(visible, (v) => emit('update:modelValue', v));

async function load() {
  if (!props.courseId) return;
  loading.value = true;
  try {
    const res = await pageCourseAnnouncements(props.courseId, {
      page: page.value,
      pageSize: pageSize.value,
      status: 'PUBLISHED'
    });
    list.value = res.data?.list || [];
    total.value = Number(res.data?.total ?? 0);
  } finally {
    loading.value = false;
  }
}

defineExpose({ reload: load });
</script>

<style scoped lang="scss">
@import './course-overview-shared.scss';

.announcement-drawer-body {
  min-height: 200px;
}

.announcement-row {
  padding: 14px 0;
  border-bottom: 1px solid #f1f5f9;

  .row-head {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    color: #0f172a;

    .pin {
      font-size: 10px;
      padding: 0 6px;
      border-radius: 9999px;
      background: #fef3c7;
      color: #d97706;
    }
  }

  .row-content {
    font-size: 13px;
    color: #475569;
    margin: 8px 0;
    line-height: 1.5;
  }

  .row-meta {
    font-size: 11px;
    color: #94a3b8;
  }

  .row-actions {
    margin-top: 8px;
  }
}

.drawer-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-start;
}
</style>
