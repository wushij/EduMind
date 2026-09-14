<template>
  <el-drawer
    :model-value="visible"
    size="680px"
    destroy-on-close
    class="broadcast-drawer"
    @update:model-value="emit('update:visible', $event)"
  >
    <template #header>
      <div class="drawer-header-custom">
        <div class="drawer-icon-box">
          <el-icon :size="20"><Promotion /></el-icon>
        </div>
        <div class="drawer-header-titles">
          <span class="drawer-title">发起全校系统广播</span>
          <span class="drawer-subtitle">定向下发至目标群体，支持普通广播、弹窗提醒与顶部紧急公告</span>
        </div>
      </div>
    </template>

    <div class="drawer-body-wrap">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <!-- 消息标题 -->
        <el-form-item label="广播标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="例如：【教务通知】2026学年春季学期教学实训与考务安排"
            maxlength="128"
            show-word-limit
          />
        </el-form-item>

        <!-- 推送受众 -->
        <div class="form-row-two">
          <el-form-item label="推送对象" prop="targetType" class="flex-1">
            <el-select v-model="form.targetType" style="width: 100%" @change="onAudienceChange">
              <el-option value="all" label="全校全体成员（所有活跃用户）" />
              <el-option value="role" label="按指定身份角色定向推送" />
            </el-select>
          </el-form-item>

          <el-form-item
            v-if="form.targetType === 'role'"
            label="选择指定角色"
            prop="targetPayload"
            class="flex-1"
          >
            <el-select
              v-model="form.targetPayload"
              placeholder="请选择受众角色"
              style="width: 100%"
              @change="updateEstimate"
            >
              <el-option label="系统管理员 (ADMIN)" value="ADMIN" />
              <el-option label="任课教师 (TEACHER)" value="TEACHER" />
              <el-option label="在籍学生 (STUDENT)" value="STUDENT" />
            </el-select>
          </el-form-item>
        </div>

        <!-- 受众预估胶囊卡片 -->
        <div v-loading="estimating" class="audience-estimate-card">
          <div class="estimate-icon">
            <el-icon :size="16"><UserFilled /></el-icon>
          </div>
          <div class="estimate-text">
            <span class="estimate-title">受众预估覆盖</span>
            <span class="estimate-desc">{{ estimateDesc }}</span>
          </div>
        </div>

        <!-- 提醒优先级与形式 -->
        <el-form-item label="提醒级别与交互形式" class="priority-form-item">
          <div class="priority-radio-grid">
            <div
              class="priority-card-opt"
              :class="{ active: form.priority === 0 }"
              @click="form.priority = 0"
            >
              <div class="opt-header">
                <span class="opt-badge opt-info">普通广播</span>
                <span class="opt-hint">站内信</span>
              </div>
              <p class="opt-desc">静默送达用户站内消息盒子，适合常规事务、温馨提醒与课务通知。</p>
            </div>

            <div
              class="priority-card-opt"
              :class="{ active: form.priority === 1 }"
              @click="form.priority = 1"
            >
              <div class="opt-header">
                <span class="opt-badge opt-warning">重要提醒</span>
                <span class="opt-hint">登录弹窗</span>
              </div>
              <p class="opt-desc">用户在线或登录系统时居中强弹窗展示，必须手动确认关闭。</p>
            </div>

            <div
              class="priority-card-opt"
              :class="{ active: form.priority === 2 }"
              @click="form.priority = 2"
            >
              <div class="opt-header">
                <span class="opt-badge opt-danger">紧急公告</span>
                <span class="opt-hint">全站置顶横幅</span>
              </div>
              <p class="opt-desc">全站各页面顶部高亮常驻滚动横幅，特急通知与重大突发事项首选。</p>
            </div>
          </div>
        </el-form-item>

        <!-- 广播正文 -->
        <el-form-item label="广播正文详情" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            placeholder="请输入广播正文内容，支持换行与详细说明..."
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>

        <!-- 实时推送效果模拟预览 -->
        <div class="preview-section">
          <div class="preview-header">
            <span class="preview-title">
              <el-icon class="mr-1"><View /></el-icon> 客户端效果模拟预览
            </span>
            <span class="preview-type-pill">
              {{ form.priority === 2 ? '全站顶部常驻横幅' : form.priority === 1 ? '居中强制确认弹窗' : '通知抽屉卡片' }}
            </span>
          </div>

          <!-- 紧急公告横幅模拟 -->
          <div v-if="form.priority === 2" class="mock-banner">
            <div class="mock-banner-left">
              <span class="mock-dot" />
              <el-icon><Notification /></el-icon>
              <span class="mock-tag">紧急公告</span>
            </div>
            <div class="mock-banner-text">
              <strong>{{ form.title || '【紧急公告标题示例】' }}：</strong>
              <span>{{ form.content || '请在此输入紧急广播正文内容...' }}</span>
            </div>
            <span class="mock-close">✕</span>
          </div>

          <!-- 重要弹窗模拟 -->
          <div v-else-if="form.priority === 1" class="mock-modal">
            <div class="mock-modal-box">
              <div class="mock-modal-icon">
                <el-icon :size="20"><WarningFilled /></el-icon>
              </div>
              <div class="mock-modal-content">
                <h4>{{ form.title || '重要系统通知标题' }}</h4>
                <p>{{ form.content || '此处为弹窗正文预览内容，用户必须知晓并确认。' }}</p>
                <div class="mock-modal-btn">我知道了</div>
              </div>
            </div>
          </div>

          <!-- 普通消息卡片模拟 -->
          <div v-else class="mock-notify-card">
            <div class="mock-notify-icon">
              <el-icon><ChatDotSquare /></el-icon>
            </div>
            <div class="mock-notify-body">
              <span class="mock-notify-title">{{ form.title || '普通站内通知标题' }}</span>
              <span class="mock-notify-desc">{{ form.content || '用户将在通知中心收到此条广播消息。' }}</span>
              <span class="mock-notify-time">刚刚 · 来自系统广播</span>
            </div>
          </div>
        </div>
      </el-form>
    </div>

    <template #footer>
      <div class="drawer-footer-custom">
        <el-button round class="footer-btn-cancel" @click="emit('update:visible', false)">
          取消
        </el-button>
        <el-button
          type="primary"
          round
          :loading="sending"
          :icon="Promotion"
          class="footer-btn-send"
          @click="handleSend"
        >
          立即广播发布
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import {
  Promotion,
  UserFilled,
  View,
  Notification,
  WarningFilled,
  ChatDotSquare
} from '@element-plus/icons-vue';
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
const estimateDesc = ref('正在评估目标受众...');

const form = reactive<BroadcastCreateRequest>({
  title: '',
  content: '',
  targetType: 'all',
  targetPayload: '',
  priority: 0
});

const rules: FormRules = {
  title: [{ required: true, message: '请输入广播标题', trigger: 'blur' }],
  targetType: [{ required: true, message: '请选择推送对象', trigger: 'change' }],
  content: [{ required: true, message: '请输入广播正文内容', trigger: 'blur' }],
  targetPayload: [
    {
      validator: (_rule, _value, callback) => {
        if (form.targetType === 'role' && !form.targetPayload) {
          callback(new Error('请选择指定角色'));
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
    const priorityDesc =
      form.priority === 2 ? '【紧急公告（全站顶部置顶横幅）】' : form.priority === 1 ? '【重要提醒（全屏弹窗）】' : '【普通广播（站内信）】';
    await ElMessageBox.confirm(
      `确认向「${estimateDesc.value}」发起${priorityDesc}？发送后将实时派发至全校师生客户端，不可撤回。`,
      '发布广播确认',
      {
        type: form.priority === 2 ? 'error' : 'warning',
        confirmButtonText: '确认发送',
        cancelButtonText: '再想想'
      }
    );
    sending.value = true;
    await createBroadcast({
      title: form.title.trim(),
      content: form.content.trim(),
      targetType: form.targetType as BroadcastTargetType,
      targetPayload: form.targetType === 'role' ? form.targetPayload : undefined,
      priority: form.priority
    });
    ElMessage.success('广播任务已提交，系统正在批量派发全员通知');
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
.drawer-header-custom {
  display: flex;
  align-items: center;
  gap: 12px;
}

.drawer-icon-box {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

.drawer-header-titles {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.drawer-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.drawer-subtitle {
  font-size: 12px;
  color: #64748b;
}

.drawer-body-wrap {
  padding: 4px 6px;
}

.form-row-two {
  display: flex;
  gap: 16px;

  .flex-1 {
    flex: 1;
  }
}

.audience-estimate-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  margin-bottom: 20px;
  border-radius: 12px;
  background: #f0f7ff;
  border: 1px solid #bfdbfe;

  .estimate-icon {
    width: 32px;
    height: 32px;
    border-radius: 8px;
    background: #2563eb;
    color: #ffffff;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .estimate-text {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .estimate-title {
    font-size: 11px;
    color: #1e40af;
    font-weight: 600;
  }

  .estimate-desc {
    font-size: 13px;
    font-weight: 600;
    color: #1d4ed8;
  }
}

/* 优先级选择卡片网格 */
.priority-radio-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  width: 100%;
}

.priority-card-opt {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
  cursor: pointer;
  background: #ffffff;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  gap: 6px;

  &:hover {
    border-color: #93c5fd;
    background: #f8fafc;
  }

  &.active {
    border-color: #2563eb;
    background: #eff6ff;
    box-shadow: 0 0 0 1px #2563eb;
  }
}

.opt-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.opt-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 9999px;

  &.opt-info {
    background: #f1f5f9;
    color: #475569;
  }
  &.opt-warning {
    background: #fef3c7;
    color: #d97706;
  }
  &.opt-danger {
    background: #fee2e2;
    color: #dc2626;
  }
}

.opt-hint {
  font-size: 11px;
  color: #94a3b8;
}

.opt-desc {
  margin: 0;
  font-size: 11px;
  color: #64748b;
  line-height: 1.4;
}

/* 实时模拟预览区 */
.preview-section {
  margin-top: 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;

  .preview-title {
    font-size: 12px;
    font-weight: 600;
    color: #475569;
    display: flex;
    align-items: center;
  }

  .preview-type-pill {
    font-size: 11px;
    padding: 2px 8px;
    border-radius: 9999px;
    background: #e2e8f0;
    color: #475569;
  }
}

/* 顶部横幅模拟 */
.mock-banner {
  background: linear-gradient(90deg, #ef4444 0%, #f97316 50%, #ea580c 100%);
  color: #fff;
  border-radius: 8px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.25);

  .mock-banner-left {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-shrink: 0;
  }

  .mock-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #fff;
  }

  .mock-tag {
    font-weight: 700;
    font-size: 11px;
    padding: 1px 6px;
    background: rgba(255, 255, 255, 0.25);
    border-radius: 9999px;
  }

  .mock-banner-text {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .mock-close {
    font-size: 12px;
    opacity: 0.8;
  }
}

/* 居中弹窗模拟 */
.mock-modal {
  display: flex;
  justify-content: center;
  padding: 10px 0;

  .mock-modal-box {
    width: 320px;
    background: #ffffff;
    border: 1px solid #fed7aa;
    border-radius: 12px;
    padding: 14px;
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
    display: flex;
    gap: 12px;
  }

  .mock-modal-icon {
    color: #f59e0b;
    flex-shrink: 0;
  }

  .mock-modal-content {
    h4 {
      margin: 0 0 4px;
      font-size: 13px;
      color: #0f172a;
    }
    p {
      margin: 0 0 10px;
      font-size: 12px;
      color: #64748b;
      line-height: 1.4;
    }
  }

  .mock-modal-btn {
    display: inline-block;
    background: #2563eb;
    color: #fff;
    padding: 3px 12px;
    border-radius: 9999px;
    font-size: 11px;
    font-weight: 500;
  }
}

/* 普通通知卡片模拟 */
.mock-notify-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px 14px;
  display: flex;
  gap: 10px;
  align-items: flex-start;

  .mock-notify-icon {
    width: 28px;
    height: 28px;
    border-radius: 8px;
    background: #eff6ff;
    color: #2563eb;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .mock-notify-body {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .mock-notify-title {
    font-size: 13px;
    font-weight: 600;
    color: #1e293b;
  }

  .mock-notify-desc {
    font-size: 12px;
    color: #64748b;
  }

  .mock-notify-time {
    font-size: 11px;
    color: #94a3b8;
    margin-top: 2px;
  }
}

/* 底部按钮长圆药丸化 */
.drawer-footer-custom {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 8px 0;
}

.footer-btn-cancel {
  border-radius: 9999px !important;
  padding: 9px 20px;
}

.footer-btn-send {
  border-radius: 9999px !important;
  padding: 9px 22px;
  font-weight: 600;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%) !important;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}
</style>
