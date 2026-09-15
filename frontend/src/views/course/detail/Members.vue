<template>
  <div class="course-members-page">
    <!-- 1. 顶部班级学情概览卡片 (长圆指标美学) -->
    <div class="members-stat-overview">
      <div class="stat-capsule-card">
        <div class="stat-icon-circle stat-icon-circle--blue">
          <el-icon><User /></el-icon>
        </div>
        <div class="stat-meta">
          <span class="stat-num">{{ members.length }}</span>
          <span class="stat-desc">选课班级总人数</span>
        </div>
      </div>

      <div class="stat-capsule-card">
        <div class="stat-icon-circle stat-icon-circle--emerald">
          <el-icon><School /></el-icon>
        </div>
        <div class="stat-meta">
          <span class="stat-num">{{ studentMembers.length }}</span>
          <span class="stat-desc">在读选课学生</span>
        </div>
      </div>

      <div class="stat-capsule-card">
        <div class="stat-icon-circle stat-icon-circle--purple">
          <el-icon><Avatar /></el-icon>
        </div>
        <div class="stat-meta">
          <span class="stat-num">{{ staffMembers.length }}</span>
          <span class="stat-desc">主讲与助教团队</span>
        </div>
      </div>

      <div class="stat-capsule-card">
        <div class="stat-icon-circle stat-icon-circle--amber">
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="stat-meta">
          <span class="stat-num">{{ averageProgress }}%</span>
          <span class="stat-desc">班级平均章节达成度</span>
        </div>
      </div>
    </div>

    <!-- 2. 长圆跑道工具栏与角色筛选 -->
    <div class="members-toolbar-panel">
      <!-- 角色药丸分类选择器 -->
      <div class="pill-filter-tabs">
        <button
          v-for="tab in roleTabs"
          :key="tab.value"
          type="button"
          class="pill-filter-btn"
          :class="{ active: currentRoleTab === tab.value }"
          @click="currentRoleTab = tab.value"
        >
          <span>{{ tab.label }}</span>
          <span class="tab-count-bubble">{{ tab.count }}</span>
        </button>
      </div>

      <!-- 右侧搜索与添加成员按钮 -->
      <div class="toolbar-right-actions">
        <div class="capsule-search-container">
          <span class="search-icon">
            <svg viewBox="0 0 24 24" class="svg-icon" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
          </span>
          <input
            v-model="searchKeyword"
            type="text"
            class="capsule-input-native"
            placeholder="搜索姓名、学号或用户名..."
          />
          <el-icon v-if="searchKeyword" class="clear-btn" @click="searchKeyword = ''"><CircleClose /></el-icon>
        </div>

        <button
          type="button"
          class="capsule-primary-btn"
          @click="showAddDialog = true"
        >
          <el-icon><Plus /></el-icon>
          <span>添加选课成员</span>
        </button>
      </div>
    </div>

    <!-- 3. 选课成员列表 (长圆美学卡片列表) -->
    <div v-loading="loading" class="members-list-container">
      <div v-if="filteredMembers.length > 0" class="members-grid">
        <div
          v-for="m in filteredMembers"
          :key="m.userId"
          class="member-card"
        >
          <!-- 头像与基本信息 -->
          <div class="member-profile">
            <div class="member-avatar" :class="getAvatarClass(m.memberRole)">
              {{ (m.realName || m.username || '学').slice(0, 1) }}
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

          <!-- 学习进度胶囊条 -->
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

          <!-- 加入时间与学情状态 -->
          <div class="member-meta-col">
            <span class="join-date">加入时间：{{ m.joinTime || '2025-09-01' }}</span>
            <span class="status-pill status-pill--active">
              <span class="dot"></span>
              在读修读中
            </span>
          </div>

          <!-- 操作栏 -->
          <div class="member-actions-col">
            <button
              type="button"
              class="action-pill-btn action-pill-btn--portrait"
              title="查看该学生学情诊断画像"
              @click="handleViewPortrait(m)"
            >
              <el-icon><TrendCharts /></el-icon>
              <span>学情画像</span>
            </button>
            <el-popconfirm
              v-if="m.memberRole !== 'TEACHER'"
              title="确定将该成员移出此课程空间吗？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleRemove(m.userId)"
            >
              <template #reference>
                <button type="button" class="action-pill-btn action-pill-btn--del" title="移出课程">
                  <el-icon><Delete /></el-icon>
                </button>
              </template>
            </el-popconfirm>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-members-box">
        <div class="empty-icon-circle">
          <el-icon><User /></el-icon>
        </div>
        <h4>暂无符合条件的选课成员</h4>
        <p>可点击右上角“添加选课成员”或提供课程代码让学生自主选课加入。</p>
      </div>
    </div>

    <!-- 4. 添加选课成员弹窗 -->
    <el-dialog
      v-model="showAddDialog"
      title="录入选课成员至当前班级"
      width="480px"
      append-to-body
      destroy-on-close
    >
      <el-form label-position="top">
        <el-form-item label="学生 / 用户 ID" required>
          <el-input v-model.number="newUserId" placeholder="例如：5（系统现有用户 ID）" />
        </el-form-item>
        <el-form-item label="成员修读角色" required>
          <el-select v-model="newMemberRole" class="w-full">
            <el-option label="在读选课学生 (STUDENT)" value="STUDENT" />
            <el-option label="主讲授课教师 (TEACHER)" value="TEACHER" />
            <el-option label="随堂助教 (ASSISTANT)" value="ASSISTANT" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <button type="button" class="capsule-modal-btn" @click="showAddDialog = false">取消</button>
        <button
          type="button"
          class="capsule-modal-btn capsule-modal-btn--primary"
          :disabled="!newUserId || adding"
          @click="handleAddSubmit"
        >
          {{ adding ? '添加中...' : '确认录入班级' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  User,
  School,
  Avatar,
  TrendCharts,
  Plus,
  Delete,
  CircleClose
} from '@element-plus/icons-vue';
import { useCourseMember } from '@/composables/course/useCourseMember';

const route = useRoute();
const router = useRouter();
const courseId = Number(route.params.id) || 101;

const {
  members,
  loading,
  fetchMembers,
  addMember,
  removeMember
} = useCourseMember(courseId);

const searchKeyword = ref('');
const currentRoleTab = ref('ALL');

const showAddDialog = ref(false);
const newUserId = ref<number | null>(null);
const newMemberRole = ref('STUDENT');
const adding = ref(false);

const studentMembers = computed(() => members.value.filter(m => m.memberRole === 'STUDENT'));
const staffMembers = computed(() => members.value.filter(m => m.memberRole === 'TEACHER' || m.memberRole === 'ASSISTANT'));

const averageProgress = computed(() => {
  if (studentMembers.value.length === 0) return 100;
  const sum = studentMembers.value.reduce((acc, cur) => acc + (cur.progress ?? 65), 0);
  return Math.round(sum / studentMembers.value.length);
});

const roleTabs = computed(() => [
  { label: '全部成员', value: 'ALL', count: members.value.length },
  { label: '选课学生', value: 'STUDENT', count: studentMembers.value.length },
  { label: '教学团队', value: 'STAFF', count: staffMembers.value.length }
]);

const filteredMembers = computed(() => {
  return members.value.filter(m => {
    if (currentRoleTab.value === 'STUDENT' && m.memberRole !== 'STUDENT') return false;
    if (currentRoleTab.value === 'STAFF' && m.memberRole !== 'TEACHER' && m.memberRole !== 'ASSISTANT') return false;

    if (searchKeyword.value.trim()) {
      const kw = searchKeyword.value.trim().toLowerCase();
      const inName = (m.realName || '').toLowerCase().includes(kw);
      const inUsername = (m.username || '').toLowerCase().includes(kw);
      const inId = String(m.userId).includes(kw);
      if (!inName && !inUsername && !inId) return false;
    }
    return true;
  });
});

function getRoleLabel(role: string) {
  const map: Record<string, string> = {
    TEACHER: '主讲教师',
    ASSISTANT: '课程助教',
    STUDENT: '选课学生'
  };
  return map[role] || '在读学生';
}

function getRoleClass(role: string) {
  if (role === 'TEACHER') return 'role-badge--teacher';
  if (role === 'ASSISTANT') return 'role-badge--ta';
  return 'role-badge--student';
}

function getAvatarClass(role: string) {
  if (role === 'TEACHER') return 'avatar--teacher';
  if (role === 'ASSISTANT') return 'avatar--ta';
  return 'avatar--student';
}

function handleViewPortrait(member: any) {
  router.push({
    path: '/analytics/learning',
    query: { studentId: member.userId, courseId }
  });
}

async function handleRemove(userId: number) {
  try {
    await removeMember(userId);
    ElMessage.success('成员已成功移出课程班级');
  } catch (err: any) {
    ElMessage.error(err?.message || '移出成员失败');
  }
}

async function handleAddSubmit() {
  if (!newUserId.value) {
    ElMessage.warning('请输入用户 ID');
    return;
  }
  adding.value = true;
  try {
    await addMember(newUserId.value, newMemberRole.value);
    ElMessage.success('成功录入选课成员！');
    showAddDialog.value = false;
    newUserId.value = null;
  } catch (err: any) {
    ElMessage.error(err?.message || '录入成员失败');
  } finally {
    adding.value = false;
  }
}

onMounted(() => {
  fetchMembers();
});
</script>

<style scoped lang="scss">
.course-members-page {
  display: flex;
  flex-direction: column;
  gap: 18px;

  // 1. 顶部指标概览
  .members-stat-overview {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 14px;

    .stat-capsule-card {
      background: #FFFFFF;
      border-radius: 16px;
      border: 1px solid #EBF1F7;
      padding: 16px 20px;
      display: flex;
      align-items: center;
      gap: 14px;
      box-shadow: 0 4px 16px rgba(30, 80, 150, 0.04);
      transition: all 0.2s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 24px rgba(30, 80, 150, 0.08);
      }

      .stat-icon-circle {
        width: 44px;
        height: 44px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;

        &--blue { background: #EFF6FF; color: #1677FF; }
        &--emerald { background: #ECFDF5; color: #10B981; }
        &--purple { background: #F5F3FF; color: #7C3AED; }
        &--amber { background: #FEF3C7; color: #D97706; }
      }

      .stat-meta {
        display: flex;
        flex-direction: column;

        .stat-num {
          font-size: 20px;
          font-weight: 700;
          color: #0F172A;
          line-height: 1.2;
        }

        .stat-desc {
          font-size: 12px;
          color: #64748B;
          margin-top: 2px;
        }
      }
    }
  }

  // 2. 长圆工具栏
  .members-toolbar-panel {
    background: #FFFFFF;
    border-radius: 16px;
    padding: 12px 20px;
    border: 1px solid #EBF1F7;
    box-shadow: 0 2px 12px rgba(30, 80, 150, 0.04);
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 14px;

    .pill-filter-tabs {
      display: flex;
      align-items: center;
      gap: 6px;
      background: #F1F5F9;
      padding: 4px;
      border-radius: 9999px;

      .pill-filter-btn {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        height: 32px;
        padding: 0 14px;
        border-radius: 9999px;
        border: none;
        background: transparent;
        color: #64748B;
        font-size: 13px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s ease;

        .tab-count-bubble {
          font-size: 11px;
          padding: 1px 6px;
          border-radius: 9999px;
          background: rgba(148, 163, 184, 0.2);
          color: #475569;
        }

        &.active {
          background: #FFFFFF;
          color: #1677FF;
          font-weight: 600;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

          .tab-count-bubble {
            background: #EAF3FF;
            color: #1677FF;
          }
        }
      }
    }

    .toolbar-right-actions {
      display: flex;
      align-items: center;
      gap: 12px;

      .capsule-search-container {
        display: flex;
        align-items: center;
        width: 260px;
        height: 38px;
        background: #FFFFFF;
        border: 1px solid #E2E8F0;
        border-radius: 9999px;
        padding: 0 14px;
        box-sizing: border-box;
        transition: all 0.2s ease;

        &:focus-within {
          border-color: #1677FF;
          box-shadow: 0 0 0 2.5px rgba(22, 119, 255, 0.15);
        }

        .search-icon {
          display: flex;
          align-items: center;
          color: #94A3B8;
          margin-right: 6px;

          .svg-icon {
            width: 15px;
            height: 15px;
          }
        }

        .capsule-input-native {
          flex: 1;
          height: 100%;
          border: none;
          outline: none;
          background: transparent;
          font-size: 13px;
          color: #1E293B;

          &::placeholder {
            color: #94A3B8;
            font-size: 12.5px;
          }
        }

        .clear-btn {
          cursor: pointer;
          color: #94A3B8;
          font-size: 12px;
          &:hover { color: #64748B; }
        }
      }

      .capsule-primary-btn {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        height: 38px;
        padding: 0 18px;
        border-radius: 9999px;
        background: #1677FF;
        color: #FFFFFF;
        border: none;
        font-size: 13px;
        font-weight: 600;
        cursor: pointer;
        box-shadow: 0 2px 8px rgba(22, 119, 255, 0.25);
        transition: all 0.2s ease;

        &:hover {
          background: #4096FF;
          transform: translateY(-1px);
        }
      }
    }
  }

  // 3. 成员列表卡片
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

  // 4. 弹窗按钮样式
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
}

// 响应式
@media (max-width: 900px) {
  .course-members-page {
    .members-list-container .members-grid .member-card {
      grid-template-columns: 1fr;
      gap: 12px;
    }
  }
}
</style>
