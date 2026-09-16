<template>
  <div class="user-list-container">
    <div class="page-header-card">
      <div class="header-left">
        <h2>用户管理</h2>
        <p>管理系统账号、角色授权与账号状态</p>
      </div>
      <div class="header-right">
        <el-button type="primary" :icon="Plus" @click="showCreateDialog = true">
          新增系统用户
        </el-button>
      </div>
    </div>

    <!-- 筛选过滤行 -->
    <div class="filter-capsule-card">
      <div class="filter-left">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户名、姓名、邮箱..."
          clearable
          class="search-input"
          :prefix-icon="Search"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="selectedRole" placeholder="系统角色" clearable class="filter-select" @change="handleSearch">
          <el-option label="全部角色" value="" />
          <el-option label="系统管理员" value="ADMIN" />
          <el-option label="任课教师" value="TEACHER" />
          <el-option label="在读学生" value="STUDENT" />
        </el-select>
        <el-select v-model="selectedStatus" placeholder="账号状态" clearable class="filter-select" @change="handleSearch">
          <el-option label="全部状态" value="" />
          <el-option label="正常活跃" value="ENABLE" />
          <el-option label="冻结停用" value="DISABLED" />
        </el-select>

        <div class="filter-actions-group">
          <el-button
            type="primary"
            round
            :icon="Search"
            class="action-pill-btn primary"
            :disabled="loading"
            @click="handleSearch"
          >
            查询
          </el-button>
          <el-button
            round
            class="btn-refresh"
            :icon="Refresh"
            :loading="loading"
            @click="handleReset"
          >
            重置
          </el-button>
        </div>
      </div>
    </div>

    <!-- 用户列表表格卡片 -->
    <div
      v-loading="loading"
      element-loading-text="正在检索系统用户信息..."
      class="user-table-card"
    >
      <el-table :data="users" stripe class="main-table">
        <el-table-column label="用户信息" min-width="220">
          <template #default="{ row }">
            <div class="em-user-cell">
              <el-avatar
                :size="40"
                :src="(!avatarBroken[row.id] && normalizeAvatarUrl(row.avatar)) ? normalizeAvatarUrl(row.avatar) : undefined"
                class="em-user-avatar-sm"
                @error="onAvatarError(row.id)"
              >
                {{ (row.realName || row.username || 'U').charAt(0).toUpperCase() }}
              </el-avatar>
              <div class="user-meta-info">
                <div class="user-real-name">{{ row.realName || row.username }}</div>
                <div class="user-account-sub">@{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="所属院系 / 班级" prop="department" min-width="180">
          <template #default="{ row }">
            <span class="text-slate-600 text-xs">{{ row.department || '计算机学院' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="授权角色" min-width="200">
          <template #default="{ row }">
            <div class="role-tags-cell">
              <el-tag
                v-for="r in row.roles"
                :key="r"
                size="small"
                :type="getRoleTagType(r)"
              >
                {{ getRoleLabel(r) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="账号状态" width="120">
          <template #default="{ row }">
            <el-tag :type="isUserEnabled(row) ? 'success' : 'danger'" size="small">
              {{ isUserEnabled(row) ? '正常活跃' : '已冻结' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <div class="table-action-group">
              <button
                type="button"
                class="table-action-pill table-action-pill--primary"
                @click="router.push(`/system/users/${row.id}`)"
              >
                档案与权限
              </button>
              <button
                type="button"
                class="table-action-pill"
                :class="isUserEnabled(row) ? 'table-action-pill--warning' : 'table-action-pill--success'"
                @click="toggleUserStatus(row)"
              >
                {{ isUserEnabled(row) ? '冻结' : '解冻' }}
              </button>
              <button
                type="button"
                class="table-action-pill table-action-pill--danger"
                :disabled="!canDeleteUser(row)"
                @click="handleDeleteUser(row)"
              >
                删除
              </button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <AppPagination
        v-model:page-num="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        @change="loadUsers"
      />
    </div>

    <!-- 新增用户对话框 -->
    <el-dialog v-model="showCreateDialog" title="新增系统用户" width="520px" destroy-on-close>
      <el-form ref="dialogFormRef" :model="newUserForm" :rules="dialogRules" label-position="top">
        <el-form-item label="用户登录名" prop="username">
          <el-input v-model="newUserForm.username" placeholder="英文字母或数字，例如 teacher_wang" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="newUserForm.realName" placeholder="例如：王建国" />
        </el-form-item>
        <el-form-item label="初始角色身份" prop="role">
          <el-select v-model="newUserForm.role" placeholder="请选择角色" class="w-full">
            <el-option label="任课教师" value="TEACHER" />
            <el-option label="在读学生" value="STUDENT" />
            <el-option label="系统管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="初始密码">
          <el-input v-model="newUserForm.password" placeholder="默认 123456" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateUser">确认创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { Plus, Search, Refresh } from '@element-plus/icons-vue';
import AppPagination from '@/components/common/AppPagination.vue';
import { normalizeAvatarUrl } from '@/utils/format/file';
import { useUser } from '@/composables/system/useUser';

const router = useRouter();

const {
  loading,
  creating,
  showCreateDialog,
  dialogFormRef,
  searchKeyword,
  selectedRole,
  selectedStatus,
  users,
  pageNum,
  pageSize,
  total,
  avatarBroken,
  newUserForm,
  dialogRules,
  onAvatarError,
  loadUsers,
  handleSearch,
  handleReset,
  getRoleLabel,
  getRoleTagType,
  isUserEnabled,
  canDeleteUser,
  toggleUserStatus,
  handleDeleteUser,
  handleCreateUser
} = useUser();
</script>

<style scoped lang="scss">
.user-list-container {
  padding: 24px;
  background: #f8fafc;

  .page-header-card {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;
    padding: 20px 24px;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 14px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.02);

    .header-left {
      h2 {
        margin: 0;
        font-size: 20px;
        font-weight: 700;
        color: #0f172a;
      }

      p {
        margin: 6px 0 0;
        font-size: 14px;
        color: #64748b;
      }
    }
  }

  .filter-capsule-card {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    padding: 14px 20px;
    margin-bottom: 20px;

    .filter-left {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;

      .search-input {
        width: 320px;
      }

      .filter-select {
        width: 150px;
      }

      .filter-actions-group {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-left: 4px;
      }
    }
  }

  .user-table-card {
    background: #ffffff;
    border-radius: 14px;
    border: 1px solid #e2e8f0;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
    padding: 16px 20px;

    .em-user-cell {
      display: flex;
      align-items: center;
      gap: 12px;

      .em-user-avatar-sm {
        flex-shrink: 0;
        background: linear-gradient(135deg, #dbeafe, #eff6ff);
        color: #2563eb;
        font-weight: 700;
      }

      .user-meta-info {
        min-width: 0;

        .user-real-name {
          font-size: 14px;
          font-weight: 600;
          color: #0f172a;
          line-height: 1.3;
        }

        .user-account-sub {
          font-size: 12px;
          color: #64748B;
          font-family: monospace;
          line-height: 1.2;
        }
      }
    }

    .role-tags-cell {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
    }

  }
}
</style>
