<template>
  <div class="system-page-shell menu-management-page gb-fade-in">
    <ProfilePageHero
      title="系统菜单与动态路由配置"
      subtitle="已同步最新 9 大核心业务模块架构（含学习中心 / AI 运维）"
    >
      <template #footer>
        <p class="hero-meta-line">
          共 <strong>{{ moduleCount }}</strong> 个顶层模块 · <strong>{{ totalMenuCount }}</strong> 个节点
          <span class="type-stats">({{ typeStats.dirs }} 目录 / {{ typeStats.menus }} 菜单 / {{ typeStats.btns }} 权限)</span>
        </p>
      </template>
      <template #actions>
        <div class="hero-action-row">
          <button type="button" class="hero-pill-btn is-primary" @click="handleCreate()">
            <el-icon><Plus /></el-icon>
            新增顶级菜单
          </button>
        </div>
      </template>
    </ProfilePageHero>

    <div class="system-filter-card filter-toolbar">
      <el-input
        v-model="searchKeyword"
        clearable
        placeholder="搜索菜单名称 / 路由路径 / 权限标识..."
        class="search-input"
        :prefix-icon="Search"
        @input="handleSearch"
        @clear="handleSearch"
      />

      <el-button round class="action-btn" @click="toggleExpandAll">
        <el-icon><Operation /></el-icon>
        {{ expandAll ? '折叠全部' : '展开全部' }}
      </el-button>

      <el-button round class="btn-refresh" :icon="Refresh" :loading="loading" @click="fetchData">
        刷新
      </el-button>

      <el-popconfirm
        title="确定将菜单配置重置为官方 8 大业务模块预设吗？"
        confirm-button-text="确定重置"
        cancel-button-text="取消"
        icon="Warning"
        icon-color="#f59e0b"
        @confirm="handleResetDefault"
      >
        <template #reference>
          <el-button round class="action-btn text-amber">
            恢复预设
          </el-button>
        </template>
      </el-popconfirm>
    </div>

    <div class="system-table-card table-card">
      <el-table
        :key="tableKey"
        ref="tableRef"
        v-loading="loading"
        :data="tableData"
        row-key="id"
        :default-expand-all="expandAll"
        :tree-props="{ children: 'children' }"
        style="width: 100%"
        class="gb-modern-table menu-tree-table"
        @row-click="onRowClick"
      >
        <!-- 菜单名称列 (严格对齐：占位符与展开箭头等宽，图标与文本同列垂直对齐，彻底消除纵向文字折行) -->
        <el-table-column prop="name" label="菜单名称" min-width="230" class-name="menu-name-col">
          <template #default="{ row }">
            <div
              class="menu-name-cell"
              :class="{ 'is-parent': row.children && row.children.length > 0 }"
              @click.stop="toggleRow(row)"
            >
              <el-icon v-if="row.icon" size="17" class="name-icon" color="#1677ff">
                <component :is="getIconComponent(row.icon)" />
              </el-icon>
              <el-icon v-else size="17" class="name-icon" color="#94a3b8">
                <component :is="row.type === 3 ? Pointer : Document" />
              </el-icon>
              <strong class="menu-name-text">{{ row.name }}</strong>
              <span v-if="row.children && row.children.length > 0" class="child-count-pill">
                {{ row.children.length }} 项
              </span>
            </div>
          </template>
        </el-table-column>

        <!-- 图标展示 -->
        <el-table-column prop="icon" label="图标" width="120" align="center">
          <template #default="{ row }">
            <div v-if="row.icon" class="icon-preview-box">
              <el-icon size="16" color="#1677ff">
                <component :is="getIconComponent(row.icon)" />
              </el-icon>
              <span class="icon-code-text">{{ row.icon }}</span>
            </div>
            <span v-else class="text-placeholder">-</span>
          </template>
        </el-table-column>

        <!-- 类型标识 -->
        <el-table-column prop="type" label="类型" width="95" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.type === 1 ? 'primary' : row.type === 2 ? 'success' : 'warning'"
              :effect="row.type === 1 ? 'dark' : 'light'"
              size="small"
              round
              class="type-tag"
            >
              {{ row.type === 1 ? '目录' : row.type === 2 ? '菜单' : '按钮' }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 路由路径 -->
        <el-table-column prop="path" label="路由路径" min-width="180">
          <template #default="{ row }">
            <span v-if="row.path" class="code-pill">{{ row.path }}</span>
            <span v-else class="text-placeholder">-</span>
          </template>
        </el-table-column>

        <!-- 前端组件路径 -->
        <el-table-column prop="component" label="前端组件" min-width="210">
          <template #default="{ row }">
            <span v-if="row.component" class="component-text" :title="row.component">{{ row.component }}</span>
            <span v-else class="text-placeholder">-</span>
          </template>
        </el-table-column>

        <!-- 权限标识 -->
        <el-table-column prop="permission" label="权限标识" min-width="160" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.permission" type="danger" size="small" round font-mono class="perm-tag">
              {{ row.permission }}
            </el-tag>
            <span v-else class="text-placeholder">-</span>
          </template>
        </el-table-column>

        <!-- 排序 -->
        <el-table-column prop="sort" label="排序" width="75" align="center">
          <template #default="{ row }">
            <span class="sort-badge">{{ row.sort }}</span>
          </template>
        </el-table-column>

        <!-- 状态 -->
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 1 ? 'success' : 'info'"
              size="small"
              round
            >
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 操作列 -->
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-pill-group" @click.stop>
              <button type="button" class="table-action-pill table-action-pill--primary" @click="handleEdit(row)">
                编辑
              </button>
              <button
                v-if="row.type !== 3"
                type="button"
                class="table-action-pill table-action-pill--primary"
                @click="handleCreate(row.id, row.type)"
              >
                添加子项
              </button>
              <button type="button" class="table-action-pill table-action-pill--danger" @click="handleDelete(row)">
                删除
              </button>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <div style="padding: 36px 0">
            <el-empty description="未找到相关菜单配置" />
          </div>
        </template>
      </el-table>
    </div>

    <!-- 菜单编辑 / 新增对话框 (深度适配 EduMind dialog-shell 与长圆胶囊体系) -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑菜单与权限' : '新增菜单项'"
      width="600px"
      append-to-body
      destroy-on-close
      class="menu-dialog"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="96px"
        class="menu-form"
      >
        <!-- 1. 上级菜单 -->
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="parentTreeOptions"
            node-key="id"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="请选择上级菜单（留空则为根目录顶级模块）"
            check-strictly
            default-expand-all
            clearable
            style="width: 100%"
          />
        </el-form-item>

        <!-- 2. 菜单类型 -->
        <el-form-item label="菜单类型" required>
          <el-radio-group v-model="form.type" @change="handleTypeChange">
            <el-radio :value="1">
              <span class="radio-label"><strong>目录</strong> (顶级/二级业务模块)</span>
            </el-radio>
            <el-radio :value="2">
              <span class="radio-label"><strong>菜单</strong> (路由页面与组件)</span>
            </el-radio>
            <el-radio :value="3">
              <span class="radio-label"><strong>按钮</strong> (细粒度操作权限)</span>
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 3. 菜单名称 -->
        <el-form-item label="菜单名称" prop="name">
          <el-input
            v-model="form.name"
            placeholder="例如：课程中心、AI出题、新增用户"
            clearable
          />
        </el-form-item>

        <!-- 4. 路由路径 (目录与菜单需要) -->
        <el-form-item v-if="form.type !== 3" label="路由路径" prop="path">
          <el-input
            v-model="form.path"
            placeholder="例如：/course 或 /ai/question/generate"
            clearable
          >
            <template #prepend>path</template>
          </el-input>
        </el-form-item>

        <!-- 5. 组件路径 (仅菜单页面需要) -->
        <el-form-item v-if="form.type === 2" label="前端组件">
          <el-autocomplete
            v-model="form.component"
            :fetch-suggestions="queryComponentSuggestions"
            placeholder="例如：views/course/CourseList.vue"
            clearable
            style="width: 100%"
          >
            <template #prepend>src/</template>
          </el-autocomplete>
        </el-form-item>

        <!-- 6. 图标选择 (目录与菜单需要) -->
        <el-form-item v-if="form.type !== 3" label="图标选择">
          <IconPicker v-model="form.icon" />
        </el-form-item>

        <!-- 7. 权限标识 -->
        <el-form-item label="权限标识">
          <el-input
            v-model="form.permission"
            placeholder="例如：course:create 或 ai:chat"
            clearable
          />
        </el-form-item>

        <!-- 8. 排序号与显示状态 -->
        <div class="form-row-grid">
          <el-form-item label="排序权重">
            <el-input-number
              v-model="form.sort"
              :min="1"
              :max="999"
              controls-position="right"
              style="width: 140px"
            />
          </el-form-item>

          <el-form-item label="状态" label-width="60px">
            <el-switch
              v-model="form.status"
              :active-value="1"
              :inactive-value="0"
              active-text="启用"
              inactive-text="停用"
            />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button round @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" round :loading="saving" @click="saveMenu">
            保存配置
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { Operation, Plus, Search, Refresh, Pointer, Document } from '@element-plus/icons-vue';
import ProfilePageHero from '@/components/profile/ProfilePageHero.vue';
import IconPicker from '@/components/system/menu/IconPicker.vue';
import { useMenu } from '@/composables/system/useMenu';

const {
  loading,
  saving,
  tableData,
  tableRef,
  searchKeyword,
  expandAll,
  tableKey,
  dialogVisible,
  isEdit,
  formRef,
  form,
  formRules,
  moduleCount,
  totalMenuCount,
  typeStats,
  parentTreeOptions,
  fetchData,
  handleSearch,
  toggleExpandAll,
  toggleRow,
  onRowClick,
  handleTypeChange,
  handleCreate,
  handleEdit,
  saveMenu,
  handleDelete,
  handleResetDefault,
  getIconComponent,
  queryComponentSuggestions
} = useMenu();
</script>

<style scoped lang="scss">
@use '@/styles/system-page-shell.scss';

.menu-management-page {
  .hero-meta-line {
    margin: 0;
    font-size: 13px;
    color: #64748b;

    strong {
      color: #0f172a;
      font-weight: 700;
    }

    .type-stats {
      margin-left: 4px;
      color: #94a3b8;
      font-size: 12px;
    }
  }

  .filter-toolbar {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;

    .search-input {
      width: 320px;

      :deep(.el-input__wrapper) {
        border-radius: 999px;
        box-shadow: 0 0 0 1px #e2e8f0 inset;
        background: #ffffff;

        &:hover {
          box-shadow: 0 0 0 1px #93c5fd inset;
        }
      }
    }

    .action-btn {
      border-radius: 999px;
      font-weight: 500;
      font-size: 13px;
    }

    .text-amber {
      color: #d97706;
      border-color: #fde68a;
      background: #fffbeb;

      &:hover {
        background: #fef3c7;
        border-color: #fcd34d;
      }
    }
  }

  .table-card {
    overflow: hidden;
    padding: 0;
  }
}

/* ==========================================================
   核心：精确消除第一列占位符与箭头宽度差异，文本永不竖向折行
   1:1 复刻 Code Compass menu-tree-table 机制
   ========================================================== */
.menu-tree-table :deep(.el-table__body td.menu-name-col .cell),
.menu-tree-table :deep(.el-table__header th.menu-name-col .cell) {
  display: flex !important;
  align-items: center !important;
  text-align: left !important;
  justify-content: flex-start !important;
  white-space: nowrap !important;
}

.menu-tree-table :deep(.el-table__expand-icon) {
  width: 20px !important;
  height: 20px !important;
  margin-right: 6px !important;
  cursor: pointer;
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.menu-tree-table :deep(.el-table__placeholder) {
  width: 20px !important;
  height: 20px !important;
  margin-right: 6px !important;
  display: inline-block !important;
  flex-shrink: 0;
}

.menu-name-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  user-select: none;
  white-space: nowrap;

  &.is-parent {
    cursor: pointer;

    &:hover .menu-name-text {
      color: #1677ff;
    }
  }
}

.name-icon {
  flex-shrink: 0;
}

.menu-name-text {
  font-size: 13.5px;
  font-weight: 600;
  color: #0f172a;
  transition: color 0.15s ease;
  white-space: nowrap;
}

.child-count-pill {
  font-size: 11px;
  color: #64748b;
  background: rgba(15, 23, 42, 0.05);
  padding: 1px 7px;
  border-radius: 999px;
  font-weight: 600;
}

.icon-preview-box {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}

.icon-code-text {
  font-size: 11.5px;
  color: #64748b;
  font-family: monospace;
}

.type-tag {
  font-weight: 600;
  padding: 2px 8px;
}

.code-pill {
  font-family: monospace;
  font-size: 12px;
  background: rgba(15, 23, 42, 0.04);
  padding: 3px 8px;
  border-radius: 6px;
  color: #334155;
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.component-text {
  font-family: monospace;
  font-size: 11.5px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: inline-block;
  max-width: 200px;
}

.perm-tag {
  font-size: 11px;
}

.sort-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.05);
  font-size: 11.5px;
  font-weight: 700;
  color: #64748b;
}

.text-placeholder {
  color: #94a3b8;
}

.action-btns {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;

  .el-button {
    font-weight: 600;
  }
}

/* 对话框内表单美化 */
.menu-dialog {
  :deep(.el-dialog__body) {
    padding: 16px 24px;
  }
}

.menu-form {
  .radio-label {
    font-size: 13px;

    strong {
      color: #0f172a;
    }
  }

  .form-row-grid {
    display: flex;
    align-items: center;
    gap: 24px;
  }

  :deep(.el-input__wrapper) {
    border-radius: 999px;
  }
}

.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;

  .el-button {
    min-width: 84px;
    font-weight: 600;
  }
}
</style>
