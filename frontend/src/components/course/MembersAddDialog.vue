<template>
  <el-dialog
    :model-value="visible"
    title="录入选课成员至当前班级"
    width="480px"
    append-to-body
    destroy-on-close
    @update:model-value="handleVisibleChange"
  >
    <el-form label-position="top">
      <el-form-item label="选择用户" required>
        <el-select
          :model-value="newUserId"
          class="w-full member-user-select"
          filterable
          remote
          reserve-keyword
          clearable
          placeholder="输入用户名或昵称搜索"
          :remote-method="handleRemoteSearch"
          :loading="searching"
          @update:model-value="emit('update:newUserId', $event ?? null)"
        >
          <el-option
            v-for="item in userOptions"
            :key="item.userId"
            :label="formatUserOptionLabel(item)"
            :value="item.userId"
          >
            <div class="user-option-row">
              <span class="user-option-name">{{ item.realName || item.username || `用户${item.userId}` }}</span>
              <span v-if="item.username" class="user-option-username">@{{ item.username }}</span>
            </div>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="成员修读角色" required>
        <el-select :model-value="newMemberRole" class="w-full" @update:model-value="emit('update:newMemberRole', $event)">
          <el-option label="在读选课学生 (STUDENT)" value="STUDENT" />
          <el-option label="主讲授课教师 (TEACHER)" value="TEACHER" />
          <el-option label="随堂助教 (ASSISTANT)" value="ASSISTANT" />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <button type="button" class="capsule-modal-btn" @click="emit('update:visible', false)">取消</button>
      <button
        type="button"
        class="capsule-modal-btn capsule-modal-btn--primary"
        :disabled="!newUserId || adding"
        @click="emit('submit')"
      >
        {{ adding ? '添加中...' : '确认录入班级' }}
      </button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { searchCourseMemberCandidates } from '@/api/course/member';
import type { CourseMemberCandidate } from '@/types/course/member';

const props = defineProps<{
  visible: boolean;
  courseId: number;
  newUserId: number | null;
  newMemberRole: string;
  adding: boolean;
}>();

const emit = defineEmits<{
  'update:visible': [value: boolean];
  'update:newUserId': [value: number | null];
  'update:newMemberRole': [value: string];
  submit: [];
}>();

const userOptions = ref<CourseMemberCandidate[]>([]);
const searching = ref(false);
let searchTimer: ReturnType<typeof setTimeout> | undefined;

function formatUserOptionLabel(item: CourseMemberCandidate): string {
  const display = item.realName || item.username || `用户${item.userId}`;
  return item.username && item.realName ? `${item.realName} (@${item.username})` : display;
}

function handleVisibleChange(open: boolean) {
  emit('update:visible', open);
  if (!open) {
    userOptions.value = [];
  }
}

async function fetchCandidates(keyword: string) {
  if (!props.courseId || !keyword.trim()) {
    userOptions.value = [];
    return;
  }
  searching.value = true;
  try {
    const res = await searchCourseMemberCandidates(props.courseId, keyword.trim());
    userOptions.value = res?.data ?? [];
  } catch {
    userOptions.value = [];
  } finally {
    searching.value = false;
  }
}

function handleRemoteSearch(query: string) {
  if (searchTimer) {
    clearTimeout(searchTimer);
  }
  if (!query.trim()) {
    userOptions.value = [];
    return;
  }
  searchTimer = setTimeout(() => {
    fetchCandidates(query);
  }, 300);
}

watch(
  () => props.visible,
  (open) => {
    if (!open) {
      userOptions.value = [];
    }
  }
);
</script>

<style scoped lang="scss">
.w-full {
  width: 100%;
}

.member-user-select {
  width: 100%;
}

.user-option-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.user-option-name {
  font-size: 13px;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-option-username {
  font-size: 12px;
  color: #94a3b8;
  flex-shrink: 0;
}

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
</style>
