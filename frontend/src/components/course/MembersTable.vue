<template>
  <div v-loading="loading" class="members-list-container">
    <div v-if="members.length > 0" class="members-grid">
      <div
        v-for="m in members"
        :key="m.userId"
        class="member-card"
      >
        <div class="member-profile">
          <div class="member-avatar" :class="getAvatarClass(m.memberRole)">
            <img
              v-if="resolveMemberAvatar(m) && !brokenAvatars[m.userId]"
              :src="resolveMemberAvatar(m)"
              class="member-avatar-img"
              alt=""
              @error="markAvatarBroken(m.userId)"
            />
            <span v-else>{{ (m.realName || m.username || '学').slice(0, 1) }}</span>
          </div>
          <div class="member-name-col">
            <div class="name-row">
              <span class="real-name">{{ m.realName || m.username || `用户${m.userId}` }}</span>
              <span class="role-pill-badge" :class="getRoleClass(m.memberRole)">
                {{ getRoleLabel(m.memberRole) }}
              </span>
            </div>
            <span class="username-tag font-mono">ID: {{ m.userId }} · {{ m.username || 'student' }}</span>
          </div>
        </div>

        <div class="member-progress-col">
          <div class="progress-info-row">
            <span class="progress-title">章节学习达成度</span>
            <span class="progress-val">{{ m.progress ?? (m.memberRole === 'TEACHER' ? 100 : 65) }}%</span>
          </div>
          <div class="capsule-track">
            <div
              class="capsule-fill"
              :style="{ width: `${m.progress ?? (m.memberRole === 'TEACHER' ? 100 : 65)}%` }"
            ></div>
          </div>
        </div>

        <div class="member-meta-col">
          <span class="join-date">加入时间：{{ m.joinTime || '2025-09-01' }}</span>
          <span class="status-pill status-pill--active">
            <span class="dot"></span>
            在读修读中
          </span>
        </div>

        <div class="member-actions-col">
          <!--
            入口按角色收敛：教师/管理员可查看任意成员；
            学生仅能在自己那一行进入本人画像，避免从成员列表跳到班级整体学情分析。
          -->
          <button
            v-if="canViewPortrait(m)"
            type="button"
            class="action-pill-btn action-pill-btn--portrait"
            :title="portraitTitle(m)"
            @click="emit('view-portrait', m)"
          >
            <el-icon><TrendCharts /></el-icon>
            <span>{{ portraitLabel(m) }}</span>
          </button>
          <button
            v-if="manageable && m.memberRole !== 'TEACHER'"
            type="button"
            class="action-pill-btn action-pill-btn--del"
            title="移出课程"
            @click="emit('remove', m.userId)"
          >
            <el-icon><Delete /></el-icon>
          </button>
        </div>
      </div>
    </div>

    <div v-else class="empty-members-box">
      <div class="empty-icon-circle">
        <el-icon><User /></el-icon>
      </div>
      <h4>暂无符合条件的选课成员</h4>
      <p>可点击右上角“添加选课成员”或提供课程代码让学生自主选课加入。</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { User, TrendCharts, Delete } from '@element-plus/icons-vue';
import type { CourseMemberItem } from '@/types/course/member';

const props = withDefaults(
  defineProps<{
    members: CourseMemberItem[];
    loading: boolean;
    brokenAvatars: Record<number, boolean>;
    resolveMemberAvatar: (member: CourseMemberItem) => string | undefined;
    markAvatarBroken: (userId: number) => void;
    getRoleLabel: (role: string) => string;
    getRoleClass: (role: string) => string;
    getAvatarClass: (role: string) => string;
    manageable?: boolean;
    /** 当前查看者是否为教师/管理员 */
    viewerIsStaff?: boolean;
    /** 当前查看者用户 ID（学生视角用于限定只能看自己） */
    viewerUserId?: number | null;
  }>(),
  { manageable: false, viewerIsStaff: false, viewerUserId: null }
);

const emit = defineEmits<{
  'view-portrait': [member: CourseMemberItem];
  remove: [userId: number];
}>();

/** 教师/管理员可查看任意成员；学生只允许在自己那一行进入本人画像 */
function canViewPortrait(member: CourseMemberItem): boolean {
  if (props.viewerIsStaff) {
    return true;
  }
  return props.viewerUserId != null && member.userId === props.viewerUserId;
}

function portraitLabel(member: CourseMemberItem): string {
  if (props.viewerIsStaff) {
    return member.memberRole === 'TEACHER' ? '课程学情' : '学情画像';
  }
  return '我的学情画像';
}

function portraitTitle(member: CourseMemberItem): string {
  if (props.viewerIsStaff) {
    return member.memberRole === 'TEACHER' ? '查看课程整体学情分析' : '查看该学生学情诊断画像';
  }
  return '查看本人学情诊断画像';
}
</script>

<style scoped lang="scss">
.members-list-container {
  min-height: 380px;

  .members-grid {
    display: flex;
    flex-direction: column;
    gap: 12px;

    .member-card {
      background: #FFFFFF;
      border: 1px solid #EBF1F7;
      border-radius: 14px;
      padding: 16px 20px;
      display: grid;
      grid-template-columns: 240px 1fr 180px 140px;
      align-items: center;
      gap: 20px;
      transition: all 0.2s ease;
      box-shadow: 0 2px 8px rgba(30, 80, 150, 0.03);

      &:hover {
        border-color: #DBEAFE;
        transform: translateY(-1px);
        box-shadow: 0 6px 20px rgba(22, 119, 255, 0.08);
      }

      .member-profile {
        display: flex;
        align-items: center;
        gap: 12px;
        min-width: 0;

        .member-avatar {
          width: 42px;
          height: 42px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 15px;
          font-weight: 700;
          color: #FFFFFF;
          flex-shrink: 0;
          overflow: hidden;

          .member-avatar-img {
            width: 100%;
            height: 100%;
            object-fit: cover;
            display: block;
          }

          &.avatar--teacher {
            background: linear-gradient(135deg, #1677FF 0%, #722ED1 100%);
          }

          &.avatar--ta {
            background: linear-gradient(135deg, #059669 0%, #10B981 100%);
          }

          &.avatar--student {
            background: linear-gradient(135deg, #3B82F6 0%, #0284C7 100%);
          }
        }

        .member-name-col {
          display: flex;
          flex-direction: column;
          gap: 3px;
          min-width: 0;

          .name-row {
            display: flex;
            align-items: center;
            gap: 8px;

            .real-name {
              font-size: 14.5px;
              font-weight: 600;
              color: #0F172A;
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
            }

            .role-pill-badge {
              padding: 1px 8px;
              border-radius: 9999px;
              font-size: 11px;
              font-weight: 600;

              &.role-badge--teacher {
                background: #F5F3FF;
                color: #7C3AED;
              }

              &.role-badge--ta {
                background: #ECFDF5;
                color: #059669;
              }

              &.role-badge--student {
                background: #EFF6FF;
                color: #2563EB;
              }
            }
          }

          .username-tag {
            font-size: 11.5px;
            color: #94A3B8;
          }
        }
      }

      .member-progress-col {
        display: flex;
        flex-direction: column;
        gap: 6px;

        .progress-info-row {
          display: flex;
          justify-content: space-between;
          font-size: 12px;
          color: #64748B;

          .progress-val {
            font-weight: 600;
            color: #1677FF;
          }
        }

        .capsule-track {
          width: 100%;
          height: 6px;
          background: #F1F5F9;
          border-radius: 9999px;
          overflow: hidden;

          .capsule-fill {
            height: 100%;
            border-radius: 9999px;
            background: linear-gradient(90deg, #3B82F6 0%, #1677FF 100%);
            transition: width 0.4s ease;
          }
        }
      }

      .member-meta-col {
        display: flex;
        flex-direction: column;
        gap: 4px;
        font-size: 12px;
        color: #64748B;

        .status-pill {
          display: inline-flex;
          align-items: center;
          gap: 5px;
          font-size: 11.5px;
          color: #059669;

          .dot {
            width: 6px;
            height: 6px;
            border-radius: 50%;
            background: #10B981;
          }
        }
      }

      .member-actions-col {
        display: flex;
        align-items: center;
        justify-content: flex-end;
        gap: 8px;

        .action-pill-btn {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          height: 30px;
          padding: 0 12px;
          border-radius: 9999px;
          border: 1px solid #E2E8F0;
          background: #FFFFFF;
          font-size: 12px;
          font-weight: 500;
          color: #475569;
          cursor: pointer;
          transition: all 0.2s ease;

          &--portrait {
            color: #1677FF;
            border-color: #BFDBFE;
            background: #EFF6FF;

            &:hover {
              background: #DBEAFE;
            }
          }

          &--del {
            padding: 0 8px;
            color: #EF4444;

            &:hover {
              background: #FEF2F2;
              border-color: #FECACA;
            }
          }
        }
      }
    }
  }

  .empty-members-box {
    padding: 60px 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: #FFFFFF;
    border-radius: 16px;
    border: 1px solid #EBF1F7;

    .empty-icon-circle {
      width: 56px;
      height: 56px;
      border-radius: 50%;
      background: #F1F5F9;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 26px;
      color: #94A3B8;
      margin-bottom: 12px;
    }

    h4 {
      margin: 0 0 6px 0;
      font-size: 15px;
      color: #1E293B;
    }

    p {
      margin: 0;
      font-size: 13px;
      color: #94A3B8;
    }
  }
}

@media (max-width: 900px) {
  .members-list-container .members-grid .member-card {
    grid-template-columns: 1fr;
    gap: 12px;
  }
}
</style>
