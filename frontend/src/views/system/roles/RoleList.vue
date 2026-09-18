<template>
  <div class="system-page-shell role-management-page gb-fade-in">
    <ProfilePageHero
      title="系统角色与权限授权"
      :subtitle="`共 ${roles.length} 个系统角色，支持细粒度菜单与功能授权`"
    >
      <template #actions>
        <div class="hero-action-row">
          <button type="button" class="hero-pill-btn is-primary" @click="openCreateDialog">
            <el-icon><Plus /></el-icon>
            新增角色
          </button>
        </div>
      </template>
    </ProfilePageHero>

    <div class="system-filter-card filter-toolbar">
      <el-input
        v-model="searchKeyword"
        clearable
        placeholder="搜索角色名称或编码..."
        class="search-input"
        :prefix-icon="Search"
      />
    </div>

    <div class="system-table-card table-card">
      <el-table
        v-loading="loading"
        :data="filteredRoles"
        style="width: 100%"
        class="gb-modern-table role-table"
      >
        <el-table-column prop="id" label="ID" width="70" align="center" />

        <el-table-column prop="roleName" label="角色名称" min-width="160">
          <template #default="{ row }">
            <div class="role-name-cell">
              <strong class="role-name-text">{{ row.roleName }}</strong>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="roleCode" label="角色标识" min-width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="getRoleTagType(row.roleCode)" size="small" round font-mono>
              {{ row.roleCode }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="已赋权限数" width="130" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small" round class="perm-count-badge">
              {{ row.permissions?.length || 0 }} 项权限
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="role-desc-text">{{ row.description || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-pill-group">
              <button type="button" class="table-action-pill table-action-pill--primary" @click="openEditDialog(row)">
                编辑
              </button>
              <button type="button" class="table-action-pill table-action-pill--primary" @click="openPermissionDrawer(row)">
                分配权限
              </button>
              <button
                type="button"
                class="table-action-pill table-action-pill--danger"
                :disabled="row.roleCode === 'ADMIN'"
                @click="handleDelete(row)"
              >
                删除
              </button>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <div style="padding: 36px 0">
            <el-empty description="暂无角色数据" />
          </div>
        </template>
      </el-table>
    </div>

    <!-- 角色新增 / 编辑弹窗 -->
    <el-dialog
      v-model="formVisible"
      :title="formMode === 'create' ? '新增角色' : '编辑角色'"
      width="480px"
      append-to-body
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input
            v-model="form.roleCode"
            :disabled="formMode === 'edit'"
            placeholder="例如：TEACHER / ASSISTANT"
          />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="例如：任课骨干教师" />
        </el-form-item>
        <el-form-item label="说明备注">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="例如：负责课程教学、作业批改与题库管理"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button round @click="formVisible = false">取消</el-button>
        <el-button type="primary" round :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配菜单权限抽屉 (1:1 模仿 Code Compass permDrawer) -->
    <el-drawer
      v-model="permDrawer"
      title="分配菜单权限"
      size="520px"
      append-to-body
      class="perm-drawer"
    >
      <div class="perm-drawer-body">
        <p class="drawer-subtitle">
          当前角色：<strong>{{ currentRole?.roleName }}</strong> ({{ currentRole?.roleCode }})
        </p>

        <!-- 快捷操作栏 -->
        <div class="drawer-toolbar">
          <el-input
            v-model="treeFilterText"
            placeholder="过滤菜单或权限..."
            clearable
            size="small"
            :prefix-icon="Search"
            style="width: 180px"
          />
          <div class="toolbar-btns">
            <el-button size="small" link type="primary" @click="checkAllNodes(true)">全选</el-button>
            <el-button size="small" link @click="checkAllNodes(false)">清空</el-button>
            <el-button size="small" link @click="toggleTreeExpand">
              {{ isTreeExpanded ? '折叠全部' : '展开全部' }}
            </el-button>
          </div>
        </div>

        <!-- 侧边栏对齐的菜单权限树 -->
        <div v-loading="permissionLoading" class="tree-box">
          <el-tree
            ref="treeRef"
            :data="menuTreeData"
            show-checkbox
            node-key="id"
            :default-expand-all="isTreeExpanded"
            :filter-node-method="filterTreeNode"
            :props="{ label: 'name', children: 'children' }"
          >
            <template #default="{ data }">
              <div class="tree-node-item">
                <el-icon v-if="data.icon" class="node-icon" color="#6366f1">
                  <component :is="getIconComponent(data.icon)" />
                </el-icon>
                <span class="node-title">{{ data.name }}</span>
                <span v-if="data.children?.length" class="node-count">({{ data.children.length }})</span>
                <el-tag
                  v-if="data.permission"
                  size="small"
                  type="danger"
                  round
                  class="node-perm-tag font-mono"
                >
                  {{ data.permission }}
                </el-tag>
              </div>
            </template>
          </el-tree>
        </div>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <el-button round @click="permDrawer = false">取消</el-button>
          <el-button type="primary" round :loading="submitting" @click="savePermissions">保存授权</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { Plus, Search } from '@element-plus/icons-vue';
import ProfilePageHero from '@/components/profile/ProfilePageHero.vue';
import { useRole } from '@/composables/system/useRole';

const {
  roles,
  loading,
  permissionLoading,
  submitting,
  searchKeyword,
  formVisible,
  formMode,
  formRef,
  permDrawer,
  currentRole,
  treeRef,
  treeFilterText,
  isTreeExpanded,
  form,
  rules,
  menuTreeData,
  filteredRoles,
  openCreateDialog,
  openEditDialog,
  openPermissionDrawer,
  checkAllNodes,
  toggleTreeExpand,
  submitForm,
  savePermissions,
  handleDelete,
  getRoleTagType,
  getIconComponent,
  filterTreeNode
} = useRole();
</script>

<style scoped lang="scss">
@use '@/styles/system-page-shell.scss';

.role-management-page {
  .filter-toolbar {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;

    .search-input {
      width: 320px;

      :deep(.el-input__wrapper) {
        border-radius: 999px;
      }
    }
  }

  .table-card {
    overflow: hidden;
    padding: 0;
  }
}

.role-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 700;
  height: 48px;
}

.role-name-text {
  font-weight: 600;
  color: #0f172a;
}

.role-desc-text {
  font-size: 13px;
  color: #64748b;
}

.perm-count-badge {
  font-weight: 600;
}

.action-btns {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

/* 分配菜单权限抽屉 (对齐 Code Compass 抽屉规范) */
.perm-drawer-body {
  padding: 10px 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.drawer-subtitle {
  font-size: 13.5px;
  color: #64748b;
  margin: 0;
}

.drawer-subtitle strong {
  color: #0f172a;
}

.drawer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #ffffff 0%, #f8faff 100%);
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  box-shadow: 0 4px 16px rgba(30, 80, 150, 0.05);
}

.toolbar-btns {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tree-box {
  max-height: calc(100vh - 230px);
  overflow-y: auto;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  padding: 16px 18px;
  background: #ffffff;
  box-shadow: 0 4px 20px rgba(30, 80, 150, 0.04);
}

.tree-node-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
  width: 100%;
}

.node-icon {
  font-size: 15px;
  flex-shrink: 0;
}

.node-title {
  font-weight: 500;
  color: #1e293b;
}

.node-count {
  font-size: 11px;
  color: #94a3b8;
}

.node-perm-tag {
  margin-left: auto;
  font-size: 11px;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
