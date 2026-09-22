<template>
  <div class="detail-body-grid">
    <!-- 左侧：用户基础档案卡片 -->
    <el-card shadow="never" class="info-card">
      <div class="card-title-line">
        <h3 class="card-title">
          <el-icon class="mr-1 text-blue-600"><User /></el-icon>
          <span>用户基础档案</span>
        </h3>
        <button type="button" class="mini-edit-btn" @click="onOpenEditProfile">
          <el-icon><EditPen /></el-icon>
          <span>编辑修改</span>
        </button>
      </div>

      <div class="fields-list">
        <div class="field-item">
          <div class="field-label-group">
            <el-icon class="field-icon"><UserFilled /></el-icon>
            <span class="label">用户名/账号：</span>
          </div>
          <div class="val font-mono">
            <span class="account-tag">@{{ userInfo?.username || '—' }}</span>
          </div>
        </div>

        <div class="field-item">
          <div class="field-label-group">
            <el-icon class="field-icon"><Avatar /></el-icon>
            <span class="label">真实姓名：</span>
          </div>
          <div class="val font-semibold text-slate-800">
            <span>{{ userInfo?.realName || '—' }}</span>
            <el-tag size="small" type="info" class="ml-2">已认证</el-tag>
          </div>
        </div>

        <div class="field-item">
          <div class="field-label-group">
            <el-icon class="field-icon"><Message /></el-icon>
            <span class="label">电子邮箱：</span>
          </div>
          <div class="val">
            <template v-if="userInfo?.email">
              <span>{{ userInfo.email }}</span>
              <span class="verified-chip verified-chip--green">
                <el-icon><CircleCheck /></el-icon>
                <span>已绑定</span>
              </span>
            </template>
            <span v-else class="text-slate-400">未绑定邮箱</span>
          </div>
        </div>

        <div class="field-item">
          <div class="field-label-group">
            <el-icon class="field-icon"><Iphone /></el-icon>
            <span class="label">联系手机：</span>
          </div>
          <div class="val font-mono">
            <template v-if="userInfo?.phone">
              <span>{{ userInfo.phone }}</span>
              <span class="verified-chip verified-chip--blue">
                <el-icon><CircleCheck /></el-icon>
                <span>已绑定</span>
              </span>
            </template>
            <span v-else class="text-slate-400">未绑定手机</span>
          </div>
        </div>

        <div class="field-item">
          <div class="field-label-group">
            <el-icon class="field-icon"><School /></el-icon>
            <span class="label">归属院系/班级：</span>
          </div>
          <div class="val">
            <span v-if="userInfo?.department" class="dept-badge">{{ userInfo.department }}</span>
            <span v-else class="text-slate-400">未配置院系</span>
          </div>
        </div>

        <div class="field-item">
          <div class="field-label-group">
            <el-icon class="field-icon"><Clock /></el-icon>
            <span class="label">注册加入时间：</span>
          </div>
          <div class="val text-slate-500 font-mono">
            <span>{{ userInfo?.createTime ? formatDateTime(userInfo.createTime) : '—' }}</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 右侧：角色身份与权限分配卡片 -->
    <el-card shadow="never" class="info-card">
      <div class="card-title-line">
        <h3 class="card-title">
          <el-icon class="mr-1 text-purple-600"><Lock /></el-icon>
          <span>角色身份与权限分配</span>
        </h3>
        <button type="button" class="mini-edit-btn mini-edit-btn--purple" @click="editRolesModal = true">
          <el-icon><Stamp /></el-icon>
          <span>调整分配角色</span>
        </button>
      </div>

      <div class="roles-allocated-box">
        <div v-for="r in (userInfo?.roles as string[])" :key="r" class="role-desc-card">
          <div class="role-top">
            <span class="role-badge" :class="getRoleTagClass(r)">{{ r }}</span>
            <span class="role-title">{{ getRoleLabel(r) }}</span>
            <el-tag size="small" effect="plain" type="success" class="ml-auto">已生效</el-tag>
          </div>
          <p class="role-note">{{ getRoleDescription(r) }}</p>

          <!-- 核心权限能力标签展示 -->
          <div class="role-capabilities-flow">
            <span
              v-for="cap in getRoleCapabilities(r)"
              :key="cap"
              class="capability-pill"
            >
              {{ cap }}
            </span>
          </div>
        </div>

        <div v-if="!userInfo?.roles || (userInfo.roles as string[]).length === 0" class="role-empty-tip">
          <span>暂无分配的角色，点击右上角进行配置</span>
        </div>
      </div>
    </el-card>
  </div>

  <!-- 编辑用户基础资料对框 -->
  <el-dialog
    v-model="editProfileModal"
    title="编辑用户基础档案"
    width="520px"
    class="custom-edit-profile-dialog"
    destroy-on-close
    append-to-body
  >
    <el-form :model="editProfileForm" label-position="top" class="edit-profile-form">
      <el-form-item label="真实姓名 *" required>
        <el-input
          v-model="editProfileForm.realName"
          placeholder="请输入用户真实姓名"
          size="large"
          clearable
        >
          <template #prefix>
            <el-icon><Avatar /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item label="电子邮箱">
        <el-input
          v-model="editProfileForm.email"
          placeholder="例如：user@edumind.edu.cn"
          size="large"
          clearable
        >
          <template #prefix>
            <el-icon><Message /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item label="联系手机号">
        <el-input
          v-model="editProfileForm.phone"
          placeholder="请输入 11 位手机号码"
          size="large"
          clearable
        >
          <template #prefix>
            <el-icon><Iphone /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item label="归属院系/专业/班级">
        <el-input
          v-model="editProfileForm.department"
          placeholder="例如：计算机科学与技术学院 · 软件工程系"
          size="large"
          clearable
        >
          <template #prefix>
            <el-icon><School /></el-icon>
          </template>
        </el-input>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-actions-row">
        <el-button @click="editProfileModal = false">取消</el-button>
        <el-button
          type="primary"
          :loading="editProfileLoading"
          @click="onSubmitEditProfile"
        >
          保存档案修改
        </el-button>
      </div>
    </template>
  </el-dialog>

  <!-- 调整分配角色对话框 -->
  <el-dialog
    v-model="editRolesModal"
    title="调整用户系统角色分配"
    width="540px"
    class="custom-edit-roles-dialog"
    destroy-on-close
    append-to-body
  >
    <p class="role-dialog-tip">请勾选该用户所具有的系统权限角色，支持赋予多个交叉角色组合：</p>
    <el-checkbox-group v-model="selectedRoles" class="role-checkboxes">
      <div
        v-for="role in systemRolesList"
        :key="role.code"
        class="role-option-card"
        :class="{ checked: selectedRoles.includes(role.code) }"
      >
        <el-checkbox :label="role.code" size="large">
          <div class="checkbox-role-content">
            <div class="checkbox-role-title">
              <span class="role-name-text">{{ role.name }}</span>
              <span class="role-code-badge">{{ role.code }}</span>
            </div>
            <p class="checkbox-role-desc">{{ role.description }}</p>
          </div>
        </el-checkbox>
      </div>
    </el-checkbox-group>

    <template #footer>
      <div class="dialog-actions-row">
        <el-button @click="editRolesModal = false">取消</el-button>
        <el-button type="primary" @click="onSaveRoles">确认保存分配</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import {
  User,
  Lock,
  EditPen,
  Stamp,
  UserFilled,
  Avatar,
  Message,
  Iphone,
  School,
  Clock,
  CircleCheck
} from '@element-plus/icons-vue';
import { getRoleLabel, getRoleDescription } from '@/composables/system/useUserDetail';

const editRolesModal = defineModel<boolean>('editRolesModal', { required: true });
const selectedRoles = defineModel<string[]>('selectedRoles', { required: true });
const editProfileModal = defineModel<boolean>('editProfileModal', { required: true });

defineProps<{
  userInfo: Record<string, any> | null;
  systemRolesList: Array<{ code: string; name: string; description: string }>;
  editProfileForm: { realName: string; email: string; phone: string; department: string };
  editProfileLoading: boolean;
  onOpenEditProfile: () => void;
  onSubmitEditProfile: () => void;
  onSaveRoles: () => void;
}>();

function getRoleTagClass(role: string): string {
  if (role.includes('ADMIN')) return 'role-badge--red';
  if (role.includes('TEACHER')) return 'role-badge--blue';
  if (role.includes('STUDENT')) return 'role-badge--green';
  return 'role-badge--gray';
}

function formatDateTime(val: unknown): string {
  if (!val) return '—';
  return String(val).replace('T', ' ').slice(0, 19);
}

function getRoleCapabilities(role: string): string[] {
  if (role.includes('ADMIN')) {
    return ['全局权限配置', '用户账号管控', '日志审计溯源', 'AI模型与配额', '租户与校区管理'];
  }
  if (role.includes('TEACHER')) {
    return ['教学大纲制定', '试题录入与维护', 'AI智能组卷', '作业AI批改', '班级学情诊断'];
  }
  if (role.includes('STUDENT')) {
    return ['在线课程修读', '课后测验作答', 'AI学伴答疑', '错题专项攻关'];
  }
  return ['通用功能访问'];
}
</script>

<style scoped lang="scss">
.detail-body-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;

  @media (max-width: 1024px) {
    grid-template-columns: 1fr;
  }

  .info-card {
    background: #ffffff;
    border-radius: 18px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 2px 12px rgba(15, 23, 42, 0.03);
    padding: 18px 22px;
    transition: all 0.2s ease;

    &:hover {
      box-shadow: 0 4px 18px rgba(15, 23, 42, 0.06);
    }

    .card-title-line {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 18px;
      padding-bottom: 12px;
      border-bottom: 1px solid #f1f5f9;

      .card-title {
        font-size: 16px;
        font-weight: 700;
        color: #0f172a;
        margin: 0;
        display: flex;
        align-items: center;
      }

      .mini-edit-btn {
        display: inline-flex;
        align-items: center;
        gap: 5px;
        padding: 4px 12px;
        border-radius: 9999px;
        background: #eff6ff;
        border: 1px solid #bfdbfe;
        color: #2563eb;
        font-size: 12px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.15s ease;

        &:hover {
          background: #2563eb;
          color: #ffffff;
        }

        &--purple {
          background: #f5f3ff;
          border-color: #ddd6fe;
          color: #7c3aed;

          &:hover {
            background: #7c3aed;
            color: #ffffff;
          }
        }
      }
    }

    .fields-list {
      display: flex;
      flex-direction: column;
      gap: 12px;

      .field-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 10px 14px;
        border-radius: 10px;
        background: #f8fafc;
        border: 1px solid #f1f5f9;
        font-size: 13px;
        transition: background 0.15s ease;

        &:hover {
          background: #f1f5f9;
        }

        .field-label-group {
          display: flex;
          align-items: center;
          gap: 8px;
          color: #64748b;
          font-weight: 500;

          .field-icon {
            font-size: 15px;
            color: #94a3b8;
          }
        }

        .val {
          display: flex;
          align-items: center;
          gap: 8px;
          color: #1e293b;
          font-weight: 500;

          .account-tag {
            background: #e2e8f0;
            padding: 2px 8px;
            border-radius: 6px;
            font-size: 12.5px;
          }

          .dept-badge {
            background: #e0f2fe;
            color: #0369a1;
            padding: 2px 10px;
            border-radius: 9999px;
            font-size: 12px;
            font-weight: 600;
          }

          .verified-chip {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            padding: 1px 7px;
            border-radius: 9999px;
            font-size: 11px;
            font-weight: 600;

            &--green {
              background: #ecfdf5;
              color: #059669;
              border: 1px solid #a7f3d0;
            }

            &--blue {
              background: #eff6ff;
              color: #2563eb;
              border: 1px solid #bfdbfe;
            }
          }
        }
      }
    }

    .roles-allocated-box {
      display: flex;
      flex-direction: column;
      gap: 14px;

      .role-desc-card {
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 12px;
        padding: 14px 16px;
        transition: all 0.2s ease;

        &:hover {
          border-color: #cbd5e1;
          box-shadow: 0 4px 12px rgba(15, 23, 42, 0.04);
        }

        .role-top {
          display: flex;
          align-items: center;
          gap: 10px;
          margin-bottom: 8px;

          .role-badge {
            font-family: monospace;
            font-size: 11px;
            font-weight: 700;
            padding: 2px 8px;
            border-radius: 6px;

            &--red {
              background: #fee2e2;
              color: #dc2626;
              border: 1px solid #fecaca;
            }

            &--blue {
              background: #eff6ff;
              color: #2563eb;
              border: 1px solid #bfdbfe;
            }

            &--green {
              background: #ecfdf5;
              color: #059669;
              border: 1px solid #a7f3d0;
            }

            &--gray {
              background: #f1f5f9;
              color: #475569;
              border: 1px solid #e2e8f0;
            }
          }

          .role-title {
            font-size: 14px;
            font-weight: 700;
            color: #0f172a;
          }
        }

        .role-note {
          font-size: 12.5px;
          color: #64748b;
          margin: 0 0 10px;
          line-height: 1.5;
        }

        .role-capabilities-flow {
          display: flex;
          flex-wrap: wrap;
          gap: 6px;
          padding-top: 8px;
          border-top: 1px dashed #e2e8f0;

          .capability-pill {
            font-size: 11px;
            background: #ffffff;
            color: #475569;
            border: 1px solid #e2e8f0;
            padding: 2px 8px;
            border-radius: 9999px;
            font-weight: 500;
          }
        }
      }

      .role-empty-tip {
        padding: 24px;
        text-align: center;
        color: #94a3b8;
        font-size: 13px;
        background: #f8fafc;
        border-radius: 12px;
        border: 1px dashed #cbd5e1;
      }
    }
  }
}

.role-dialog-tip {
  font-size: 13px;
  color: #64748b;
  margin-top: 0;
  margin-bottom: 16px;
}

.role-checkboxes {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .role-option-card {
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    padding: 12px 16px;
    background: #ffffff;
    transition: all 0.2s ease;

    &:hover {
      border-color: #93c5fd;
      background: #f8fafc;
    }

    &.checked {
      border-color: #3b82f6;
      background: #eff6ff;
    }

    :deep(.el-checkbox) {
      width: 100%;
      height: auto;
      align-items: flex-start;

      .el-checkbox__label {
        width: 100%;
        white-space: normal;
      }
    }

    .checkbox-role-content {
      display: flex;
      flex-direction: column;
      gap: 4px;
      margin-left: 6px;

      .checkbox-role-title {
        display: flex;
        align-items: center;
        gap: 8px;

        .role-name-text {
          font-weight: 700;
          color: #0f172a;
          font-size: 14px;
        }

        .role-code-badge {
          font-size: 11px;
          font-family: monospace;
          color: #64748b;
          background: #f1f5f9;
          padding: 1px 6px;
          border-radius: 4px;
        }
      }

      .checkbox-role-desc {
        font-size: 12px;
        color: #64748b;
        margin: 0;
        line-height: 1.4;
      }
    }
  }
}

.dialog-actions-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}
</style>
