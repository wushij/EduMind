<template>
  <el-dialog
    :model-value="visible"
    title="广播详情"
    width="560px"
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
  >
    <template v-if="broadcast">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="标题">{{ broadcast.title }}</el-descriptions-item>
        <el-descriptions-item label="受众">
          {{ audienceLabel(broadcast.targetType, broadcast.targetPayload) }}
        </el-descriptions-item>
        <el-descriptions-item label="优先级">
          {{ priorityLabel(broadcast.priority) }}
        </el-descriptions-item>
        <el-descriptions-item label="触达 / 已读">
          {{ broadcast.readCount }} / {{ broadcast.totalCount }}
          （{{ calcRate(broadcast.readCount, broadcast.totalCount) }}%）
        </el-descriptions-item>
        <el-descriptions-item label="发送人">{{ broadcast.senderName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发送时间">
          {{ broadcast.createTime ? formatDateTime(broadcast.createTime) : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="正文">
          <div class="content-block">{{ broadcast.content }}</div>
        </el-descriptions-item>
      </el-descriptions>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { formatDateTime } from '@/utils/format/date';
import type { NotificationBroadcastVO } from '@/types/notification/broadcast';

defineProps<{
  visible: boolean;
  broadcast: NotificationBroadcastVO | null;
}>();

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void;
}>();

function priorityLabel(p: number) {
  if (p === 1) return '弹窗强提醒';
  if (p === 2) return '顶部跑马灯';
  return '普通站内信';
}

function audienceLabel(type: string, payload?: string) {
  if (type === 'all') return '全体用户';
  if (type === 'role') {
    const map: Record<string, string> = {
      ADMIN: '系统管理员',
      TEACHER: '教师',
      STUDENT: '学生'
    };
    return map[payload || ''] || payload || '指定角色';
  }
  return type;
}

function calcRate(read: number, total: number) {
  if (total <= 0) return 0;
  return Math.min(Math.round((read / total) * 100), 100);
}
</script>

<style scoped lang="scss">
.content-block {
  white-space: pre-wrap;
  line-height: 1.6;
  max-height: 240px;
  overflow-y: auto;
}
</style>
