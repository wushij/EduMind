<template>
  <el-dialog v-model="visible" :title="isEdit ? '编辑公告' : '发布课程公告'" width="560px" destroy-on-close>
    <el-form label-position="top">
      <el-form-item label="标题" required>
        <el-input v-model="form.title" maxlength="200" show-word-limit placeholder="公告标题" />
      </el-form-item>
      <el-form-item label="正文" required>
        <el-input v-model="form.content" type="textarea" :rows="5" placeholder="通知内容" />
      </el-form-item>
      <el-form-item>
        <el-checkbox v-model="form.pinned">置顶显示</el-checkbox>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">发布</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import type { CourseAnnouncementVO } from '@/types/course/overview';

const props = defineProps<{
  modelValue: boolean;
  saving?: boolean;
  editing?: CourseAnnouncementVO | null;
}>();

const emit = defineEmits<{
  'update:modelValue': [boolean];
  publish: [payload: { title: string; content: string; pinned?: boolean }];
}>();

const visible = ref(props.modelValue);
const isEdit = ref(false);
const form = reactive({ title: '', content: '', pinned: false });

watch(() => props.modelValue, (v) => {
  visible.value = v;
  if (v) {
    isEdit.value = Boolean(props.editing);
    form.title = props.editing?.title || '';
    form.content = props.editing?.content || '';
    form.pinned = Boolean(props.editing?.pinned);
  }
});

watch(visible, (v) => emit('update:modelValue', v));

function submit() {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('请填写标题和正文');
    return;
  }
  emit('publish', {
    title: form.title.trim(),
    content: form.content.trim(),
    pinned: form.pinned
  });
}
</script>
