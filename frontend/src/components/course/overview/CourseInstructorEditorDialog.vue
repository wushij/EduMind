<template>
  <el-dialog
    v-model="visible"
    title="编辑教学团队展示"
    width="680px"
    destroy-on-close
    class="course-instructor-editor-dialog"
  >
    <div class="dialog-body-wrap">
      <!-- 顶部信息说明栏与同步操作 -->
      <div class="dialog-banner">
        <div class="banner-text">
          <el-icon class="banner-icon"><InfoFilled /></el-icon>
          <div class="banner-content">
            <span class="banner-title">教学团队展示说明</span>
            <p class="banner-desc">
              团队成员将展示在课程概览右侧栏，供学生了解主讲与助教团队并查看答疑接待排班（Office Hours）。
            </p>
          </div>
        </div>
        <button
          type="button"
          class="sync-pill-btn"
          :class="{ 'is-loading': syncing || saving }"
          :disabled="syncing || saving"
          @click="handleSync"
        >
          <el-icon :class="{ 'spin-icon': syncing }"><Refresh /></el-icon>
          <span>{{ syncing ? '正在同步...' : '从课程成员同步' }}</span>
        </button>
      </div>

      <!-- 空状态 -->
      <div v-if="rows.length === 0" class="empty-instructor-block">
        <div class="empty-icon-wrap">
          <el-icon><UserFilled /></el-icon>
        </div>
        <h4 class="empty-title">暂未配置团队展示成员</h4>
        <p class="empty-desc">
          系统支持一键将「课程成员」中已分配的教师与助教拉取至展示卡片，无需重复添加。
        </p>
        <button
          type="button"
          class="empty-sync-btn"
          :disabled="syncing || saving"
          @click="handleSync"
        >
          <el-icon :class="{ 'spin-icon': syncing }"><Refresh /></el-icon>
          <span>立即从课程成员同步</span>
        </button>
      </div>

      <!-- 教师卡片列表 -->
      <div v-else class="instructor-cards-list">
        <div
          v-for="(row, index) in rows"
          :key="row.userId"
          class="instructor-card"
          :class="{ 'is-primary': row.primary }"
        >
          <!-- 卡片头部：头像 + 姓名 + 身份徽章 -->
          <div class="instructor-card__head">
            <div class="user-meta">
              <el-avatar
                :size="44"
                :src="row.avatarSrc"
                class="user-avatar"
                :class="{ 'primary-avatar': row.primary }"
              >
                {{ row.avatarLetter }}
              </el-avatar>
              <div class="user-info">
                <div class="name-row">
                  <span class="real-name">{{ row.name }}</span>
                  <el-tag
                    size="small"
                    :type="row.primary ? 'warning' : 'primary'"
                    effect="light"
                    class="role-badge"
                  >
                    <el-icon class="badge-icon">
                      <StarFilled v-if="row.primary" />
                      <User v-else />
                    </el-icon>
                    {{ row.primary ? '主讲教师' : (row.roleLabel || '助教') }}
                  </el-tag>
                  <span v-if="row.username" class="username-code">@{{ row.username }}</span>
                </div>
                <div class="role-desc">
                  <span>{{ row.primary ? '课程创建负责人，主持教学进度与讲授' : '课程教学辅助与答疑管理' }}</span>
                </div>
              </div>
            </div>

            <div class="head-right">
              <span class="sort-seq">#{{ index + 1 }}</span>
            </div>
          </div>

          <!-- 卡片输入字段区域 -->
          <div class="instructor-card__body">
            <!-- 教师简介 -->
            <div class="form-field">
              <label class="field-label">
                <el-icon class="label-icon"><User /></el-icon>
                <span>教师简介 / 教学特长</span>
              </label>
              <el-input
                v-model="row.intro"
                type="textarea"
                :autosize="{ minRows: 2, maxRows: 4 }"
                placeholder="介绍教学背景、主讲方向或研究特色，帮助学生更快熟悉（可选）"
                maxlength="500"
                show-word-limit
                class="field-textarea"
              />
            </div>

            <!-- 答疑时间与地点 -->
            <div class="form-field">
              <div class="field-label-row">
                <label class="field-label">
                  <el-icon class="label-icon"><Clock /></el-icon>
                  <span>答疑接待时间与地点 (Office Hours)</span>
                </label>
                <span class="field-hint">快捷填入常用模板：</span>
              </div>
              <el-input
                v-model="row.officeHours"
                placeholder="如：每周二 14:00-16:00 (教3-201 / 腾讯会议)"
                maxlength="150"
                class="field-input"
              >
                <template #prefix>
                  <el-icon class="input-inner-icon"><Location /></el-icon>
                </template>
              </el-input>

              <!-- 快捷模板标签 -->
              <div class="quick-preset-chips">
                <button
                  v-for="preset in QUICK_OFFICE_HOURS_PRESETS"
                  :key="preset"
                  type="button"
                  class="preset-chip"
                  @click="applyPresetHours(row, preset)"
                >
                  {{ preset }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部按钮操作栏 -->
    <template #footer>
      <div class="dialog-footer-row">
        <span class="footer-count">共 {{ rows.length }} 位团队成员</span>
        <div class="footer-btns">
          <el-button @click="visible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submit">保存展示配置</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import {
  InfoFilled,
  Refresh,
  UserFilled,
  User,
  StarFilled,
  Clock,
  Location
} from '@element-plus/icons-vue';
import type { CourseInstructorCardVO } from '@/types/course/overview';
import { normalizeAvatarUrl } from '@/utils/format/file';

interface InstructorEditRow {
  userId: number;
  name: string;
  username?: string;
  roleLabel: string;
  avatarSrc?: string;
  avatarLetter: string;
  intro: string;
  officeHours: string;
  sortOrder?: number;
  primary?: boolean;
}

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
const rows = ref<InstructorEditRow[]>([]);
const syncing = ref(false);

const QUICK_OFFICE_HOURS_PRESETS = [
  '每周二 14:00-16:00',
  '每周四 19:00-21:00 (线上会议)',
  '工作日课后 16:30-17:30',
  '预约制答疑 (站内私信联系)'
];

function transformInstructors(list: CourseInstructorCardVO[]): InstructorEditRow[] {
  return list.map(i => {
    const displayName = i.realName || i.username || `用户${i.userId}`;
    const avatar = normalizeAvatarUrl(i.avatar);
    return {
      userId: i.userId,
      name: displayName,
      username: i.username,
      roleLabel: i.roleLabel || (i.primary ? '主讲教师' : '教师'),
      avatarSrc: avatar,
      avatarLetter: displayName.trim().slice(0, 1).toUpperCase(),
      intro: i.intro || '',
      officeHours: i.officeHours || '',
      sortOrder: i.sortOrder,
      primary: i.primary
    };
  });
}

function syncFromProps(newList: CourseInstructorCardVO[]) {
  const currentMap = new Map<number, { intro: string; officeHours: string }>();
  for (const r of rows.value) {
    currentMap.set(r.userId, { intro: r.intro, officeHours: r.officeHours });
  }

  rows.value = newList.map(i => {
    const existing = currentMap.get(i.userId);
    const displayName = i.realName || i.username || `用户${i.userId}`;
    const avatar = normalizeAvatarUrl(i.avatar);
    return {
      userId: i.userId,
      name: displayName,
      username: i.username,
      roleLabel: i.roleLabel || (i.primary ? '主讲教师' : '教师'),
      avatarSrc: avatar,
      avatarLetter: displayName.trim().slice(0, 1).toUpperCase(),
      intro: existing ? existing.intro : (i.intro || ''),
      officeHours: existing ? existing.officeHours : (i.officeHours || ''),
      sortOrder: i.sortOrder,
      primary: i.primary
    };
  });
}

watch(() => props.modelValue, (v) => {
  visible.value = v;
  if (v) {
    rows.value = transformInstructors(props.instructors || []);
  }
});

watch(
  () => props.instructors,
  (newList) => {
    if (visible.value && newList) {
      syncFromProps(newList);
      syncing.value = false;
    }
  },
  { deep: true }
);

watch(visible, (v) => emit('update:modelValue', v));

function handleSync() {
  syncing.value = true;
  emit('sync');
  // 兜底超时关闭动画
  setTimeout(() => {
    syncing.value = false;
  }, 3000);
}

function applyPresetHours(row: InstructorEditRow, preset: string) {
  if (!row.officeHours.trim()) {
    row.officeHours = preset;
  } else if (!row.officeHours.includes(preset)) {
    row.officeHours = `${row.officeHours} / ${preset}`;
  }
}

function submit() {
  emit('save', rows.value.map(r => ({
    userId: r.userId,
    intro: r.intro.trim(),
    officeHours: r.officeHours.trim(),
    sortOrder: r.sortOrder,
    primary: r.primary
  })));
}
</script>

<style scoped lang="scss">
.course-instructor-editor-dialog {
  :deep(.el-dialog__body) {
    padding: 16px 22px 20px;
  }
}

.dialog-body-wrap {
  min-height: 180px;
}

/* 顶部 Banner 说明区 */
.dialog-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 16px;
  margin-bottom: 18px;
  border-radius: 12px;
  background: linear-gradient(135deg, #f0f7ff 0%, #f5f3ff 100%);
  border: 1px solid #dbeafe;

  .banner-text {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    min-width: 0;
  }

  .banner-icon {
    font-size: 18px;
    color: #2563eb;
    margin-top: 2px;
    flex-shrink: 0;
  }

  .banner-content {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .banner-title {
    font-size: 13px;
    font-weight: 700;
    color: #1e3a8a;
  }

  .banner-desc {
    margin: 0;
    font-size: 12px;
    color: #475569;
    line-height: 1.45;
  }
}

.sync-pill-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 16px;
  border-radius: 9999px;
  border: 1px solid #bfdbfe;
  background: #ffffff;
  color: #2563eb;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  box-shadow: 0 1px 3px rgba(37, 99, 235, 0.08);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover:not(:disabled) {
    background: #eff6ff;
    border-color: #93c5fd;
    box-shadow: 0 2px 8px rgba(37, 99, 235, 0.16);
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.65;
    cursor: not-allowed;
    transform: none;
  }

  .spin-icon {
    animation: rotate 1s linear infinite;
  }
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 空状态卡片 */
.empty-instructor-block {
  text-align: center;
  padding: 36px 20px;
  border-radius: 14px;
  background: #fafbfc;
  border: 1px dashed #cbd5e1;

  .empty-icon-wrap {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 52px;
    height: 52px;
    border-radius: 50%;
    background: #e2e8f0;
    color: #64748b;
    font-size: 26px;
    margin-bottom: 12px;
  }

  .empty-title {
    margin: 0 0 6px;
    font-size: 15px;
    font-weight: 700;
    color: #0f172a;
  }

  .empty-desc {
    margin: 0 auto 18px;
    max-width: 440px;
    font-size: 13px;
    color: #64748b;
    line-height: 1.5;
  }

  .empty-sync-btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    height: 38px;
    padding: 0 22px;
    border-radius: 9999px;
    border: none;
    background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
    color: #ffffff;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    box-shadow: 0 3px 10px rgba(37, 99, 235, 0.25);
    transition: all 0.2s ease;

    &:hover:not(:disabled) {
      box-shadow: 0 4px 14px rgba(37, 99, 235, 0.35);
      transform: translateY(-1px);
    }
  }
}

/* 教师卡片列表 */
.instructor-cards-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: min(60vh, 520px);
  overflow-y: auto;
  padding-right: 4px;
}

.instructor-card {
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
  padding: 16px 18px 18px;
  transition: all 0.2s ease;

  &:hover {
    border-color: #cbd5e1;
    box-shadow: 0 4px 14px rgba(15, 23, 42, 0.06);
  }

  &.is-primary {
    border-color: #bfdbfe;
    background: linear-gradient(180deg, #fcfdff 0%, #ffffff 100%);
  }

  &__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding-bottom: 12px;
    margin-bottom: 14px;
    border-bottom: 1px solid #f1f5f9;

    .user-meta {
      display: flex;
      align-items: center;
      gap: 12px;
      min-width: 0;
    }

    .user-avatar {
      background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
      color: #fff;
      font-size: 18px;
      font-weight: 700;
      box-shadow: 0 2px 6px rgba(37, 99, 235, 0.2);
      flex-shrink: 0;

      &.primary-avatar {
        background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
        box-shadow: 0 2px 6px rgba(217, 119, 6, 0.25);
      }
    }

    .user-info {
      display: flex;
      flex-direction: column;
      gap: 4px;
      min-width: 0;
    }

    .name-row {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
    }

    .real-name {
      font-size: 15px;
      font-weight: 700;
      color: #0f172a;
    }

    .role-badge {
      display: inline-flex;
      align-items: center;
      gap: 3px;
      font-weight: 600;
      border-radius: 6px;

      .badge-icon {
        font-size: 12px;
      }
    }

    .username-code {
      font-size: 12px;
      color: #94a3b8;
      font-family: monospace;
    }

    .role-desc {
      font-size: 12px;
      color: #64748b;
    }

    .head-right {
      flex-shrink: 0;
      .sort-seq {
        font-size: 12px;
        font-weight: 700;
        color: #94a3b8;
        background: #f1f5f9;
        padding: 2px 8px;
        border-radius: 9999px;
      }
    }
  }

  &__body {
    display: flex;
    flex-direction: column;
    gap: 14px;
  }
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;

  .field-label {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12.5px;
    font-weight: 600;
    color: #334155;

    .label-icon {
      font-size: 14px;
      color: #64748b;
    }
  }

  .field-label-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    flex-wrap: wrap;

    .field-hint {
      font-size: 11.5px;
      color: #94a3b8;
    }
  }

  .field-textarea {
    :deep(.el-textarea__inner) {
      border-radius: 10px;
      box-shadow: 0 0 0 1px #e2e8f0 inset;
      background: #fafbfc;
      font-size: 13px;
      line-height: 1.6;
      padding: 10px 12px 28px;
      transition: all 0.2s ease;

      &:focus {
        background: #ffffff;
        box-shadow: 0 0 0 1.5px #3b82f6 inset, 0 2px 8px rgba(59, 130, 246, 0.1);
      }
    }

    :deep(.el-input__count) {
      background: transparent;
      bottom: 6px;
      right: 10px;
      font-size: 11px;
    }
  }

  .field-input {
    :deep(.el-input__wrapper) {
      border-radius: 10px;
      box-shadow: 0 0 0 1px #e2e8f0 inset;
      background: #fafbfc;
      transition: all 0.2s ease;

      &.is-focus {
        background: #ffffff;
        box-shadow: 0 0 0 1.5px #3b82f6 inset, 0 2px 8px rgba(59, 130, 246, 0.1);
      }
    }

    .input-inner-icon {
      color: #94a3b8;
      font-size: 14px;
    }
  }
}

/* 快捷预设标签 */
.quick-preset-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;

  .preset-chip {
    padding: 3px 10px;
    border-radius: 9999px;
    border: 1px solid #e2e8f0;
    background: #f8fafc;
    color: #475569;
    font-size: 11.5px;
    cursor: pointer;
    transition: all 0.15s ease;

    &:hover {
      background: #eff6ff;
      border-color: #bfdbfe;
      color: #2563eb;
    }
  }
}

/* 底部操作区 */
.dialog-footer-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;

  .footer-count {
    font-size: 12.5px;
    color: #64748b;
  }

  .footer-btns {
    display: flex;
    align-items: center;
    gap: 10px;
  }
}
</style>
