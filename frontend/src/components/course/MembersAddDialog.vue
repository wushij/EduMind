<template>
  <el-dialog
    :model-value="visible"
    title="录入选课成员至当前班级"
    width="480px"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
  >
    <el-form label-position="top">
      <el-form-item label="学生 / 用户 ID" required>
        <el-input
          :model-value="newUserId != null ? String(newUserId) : ''"
          placeholder="例如：5（系统现有用户 ID）"
          @update:model-value="emit('update:newUserId', $event ? Number($event) : null)"
        />
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
defineProps<{
  visible: boolean;
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
</script>

<style scoped lang="scss">
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
