<template>
  <el-drawer
    :model-value="visible"
    title="发起系统广播推送"
    size="720px"
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="消息标题" prop="title">
        <el-input v-model="form.title" placeholder="如：【系统通知】期末考试安排说明" maxlength="128" show-word-limit />
      </el-form-item>

      <el-form-item label="推送对象" prop="targetType">
        <el-select v-model="form.targetType" style="width: 100%" @change="onAudienceChange">
          <el-option value="all" label="全体用户" />
          <el-option value="role" label="按角色推送" />
        </el-select>
      </el-form-item>

      <el-form-item v-if="form.targetType === 'role'" label="选择角色" prop="targetPayload">
        <el-select v-model="form.targetPayload" placeholder="请选择角色" style="width: 100%" @change="updateEstimate">
          <el-option label="系统管理员" value="ADMIN" />
          <el-option label="教师" value="TEACHER" />
          <el-option label="学生" value="STUDENT" />
        </el-select>
      </el-form-item>

      <div v-loading="estimating" class="estimate-pill">
        <el-icon><User /></el-icon>
        <span>{{ estimateDesc }}</span>
      </div>

      <el-form-item label="提醒优先级">
        <el-select v-model="form.priority" style="width: 100%">
          <el-option label="普通站内信" :value="0" />
          <el-option label="弹窗强提醒" :value="1" />
          <el-option label="顶部跑马灯（紧急）" :value="2" />
        </el-select>
      </el-form-item>

      <el-form-item label="推送正文" prop="content">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="8"
          placeholder="请输入广播正文内容..."
          maxlength="2000"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="drawer-footer">
        <el-button @click="emit('update:visible', false)">取消</el-button>
        <el-button type="primary" :loading="sending" @click="handleSend">
          <el-icon><Promotion /></el-icon>
          立即广播
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { Promotion, User } from '@element-plus/icons-vue';
import { createBroadcast, estimateBroadcastAudience } from '@/api/notification/broadcast';
import type { BroadcastCreateRequest, BroadcastTargetType } from '@/types/notification/broadcast';

const props = defineProps<{
  visible: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void;
  (e: 'sent'): void;
}>();

const formRef = ref<FormInstance>();
const sending = ref(false);
const estimating = ref(false);
const estimateDesc = ref('计算受众中...');

const form = reactive<BroadcastCreateRequest>({
  title: '',
  content: '',
  targetType: 'all',
  targetPayload: '',
  priority: 0
});

const rules: FormRules = {
  title: [{ required: true, message: '请输入消息标题', trigger: 'blur' }],
  targetType: [{ required: true, message: '请选择推送对象', trigger: 'change' }],
  content: [{ required: true, message: '请输入推送正文', trigger: 'blur' }],
  targetPayload: [
    {
      validator: (_rule, _value, callback) => {
        if (form.targetType === 'role' && !form.targetPayload) {
          callback(new Error('请选择角色'));
          return;
        }
        callback();
      },
      trigger: 'change'
    }
  ]
};

async function updateEstimate() {
  estimating.value = true;
  try {
    const res = await estimateBroadcastAudience({
      targetType: form.targetType,
      targetPayload: form.targetPayload || undefined
    });
    estimateDesc.value = res.data?.formattedDesc || `预计触达 ${res.data?.estimatedCount ?? 0} 人`;
  } catch {
    estimateDesc.value = '暂无法预估受众规模';
  } finally {
    estimating.value = false;
  }
}

function onAudienceChange() {
  if (form.targetType === 'all') {
    form.targetPayload = '';
  } else {
    form.targetPayload = 'TEACHER';
  }
  updateEstimate();
}

watch(
  () => props.visible,
  (open) => {
    if (!open) return;
    form.title = '';
    form.content = '';
    form.targetType = 'all';
    form.targetPayload = '';
    form.priority = 0;
    updateEstimate();
  }
);

async function handleSend() {
  if (!formRef.value) return;
  await formRef.value.validate();

  try {
    await ElMessageBox.confirm(
      `确认向「${estimateDesc.value}」发起广播？发送后将实时推送，且不可撤回。`,
      '广播确认',
      { type: 'warning', confirmButtonText: '确认发送', cancelButtonText: '取消' }
    );
    sending.value = true;
    await createBroadcast({
      title: form.title.trim(),
      content: form.content.trim(),
      targetType: form.targetType as BroadcastTargetType,
      targetPayload: form.targetType === 'role' ? form.targetPayload : undefined,
      priority: form.priority
    });
    ElMessage.success('广播任务已提交，系统正在批量派发');
    emit('update:visible', false);
    emit('sent');
  } catch (err: unknown) {
    if (err !== 'cancel' && err instanceof Error && err.message) {
      /* axios 已提示 */
    }
  } finally {
    sending.value = false;
  }
}
</script>

<style scoped lang="scss">
.estimate-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  margin-bottom: 16px;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.08);
  color: #4f46e5;
  font-size: 13px;
  font-weight: 600;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
