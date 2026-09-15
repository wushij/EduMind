<template>
  <div class="left-tree-card">
    <div class="tree-header">
      <div class="header-title">
        <el-icon class="title-icon"><Connection /></el-icon>
        <span>组织架构层级</span>
      </div>
      <el-button size="small" type="primary" class="pill-btn add-root-btn" @click="$emit('add-root')">
        <el-icon><Plus /></el-icon>
        <span>新增校区</span>
      </el-button>
    </div>

    <div class="tree-search-bar">
      <el-input
        :model-value="treeSearchKeyword"
        placeholder="搜索校区、院系、年级或班级..."
        prefix-icon="Search"
        clearable
        class="pill-search-input"
        @update:model-value="$emit('update:treeSearchKeyword', $event)"
      />
    </div>

    <div class="tree-wrapper">
      <el-tree
        v-if="treeData && treeData.length > 0"
        :ref="treeRef"
        :data="treeData"
        node-key="id"
        :props="{ label: 'name', children: 'children' }"
        default-expand-all
        highlight-current
        :filter-node-method="filterTreeNode"
        @node-click="(data: OrganizationNodeVO) => $emit('node-click', data)"
      >
        <template #default="{ data }">
          <div class="custom-tree-node" :class="{ 'is-selected': selectedNode?.id === data.id }">
            <div class="node-left">
              <el-icon class="node-icon" :class="data.orgType">
                <OfficeBuilding v-if="data.orgType === 'CAMPUS'" />
                <School v-else-if="data.orgType === 'FACULTY' || data.orgType === 'COLLEGE'" />
                <Folder v-else-if="data.orgType === 'DEPT'" />
                <UserFilled v-else />
              </el-icon>
              <span class="node-label">{{ data.name }}</span>
              <span class="node-pill-tag" :class="data.orgType">
                {{ getTypeLabel(data.orgType) }}
              </span>
            </div>
            <div class="node-actions" @click.stop>
              <el-dropdown trigger="click" @command="(cmd: string) => $emit('tree-command', cmd, data)">
                <button class="node-more-btn">
                  <el-icon><MoreFilled /></el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu class="pill-dropdown-menu">
                    <el-dropdown-item command="addChild" v-if="data.orgType !== 'CLASS'">
                      <el-icon><Plus /></el-icon>添加下级部门/班级
                    </el-dropdown-item>
                    <el-dropdown-item command="edit">
                      <el-icon><Edit /></el-icon>重命名 / 编辑
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" divided style="color: #EF4444;">
                      <el-icon><Delete /></el-icon>删除该节点
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </template>
      </el-tree>
      <div v-else class="empty-tree-state">
        <el-empty
          :image-size="80"
          description="暂无组织架构数据"
        >
          <template #default>
            <p class="empty-tree-desc">
              当前学校尚未建立任何校区与教学组织
            </p>
            <el-button type="primary" size="small" class="gradient-pill-btn" @click="$emit('add-root')">
              <el-icon><Plus /></el-icon>
              <span>初始化新建校区</span>
            </el-button>
          </template>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Ref } from 'vue';
import {
  Connection,
  Plus,
  OfficeBuilding,
  School,
  Folder,
  UserFilled,
  MoreFilled,
  Edit,
  Delete
} from '@element-plus/icons-vue';
import type { OrganizationNodeVO } from '@/types/system/tenant';

defineProps<{
  treeRef: Ref | undefined;
  treeData: OrganizationNodeVO[];
  treeSearchKeyword: string;
  selectedNode: OrganizationNodeVO | null;
  filterTreeNode: (value: string, data: OrganizationNodeVO) => boolean;
  getTypeLabel: (orgType: string) => string;
}>();

defineEmits<{
  'update:treeSearchKeyword': [value: string];
  'add-root': [];
  'node-click': [data: OrganizationNodeVO];
  'tree-command': [command: string, data: OrganizationNodeVO];
}>();
</script>

<style scoped lang="scss">
.left-tree-card {
  background: #FFFFFF;
  border-radius: 20px;
  border: 1px solid #E2E8F0;
  padding: 20px;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);

  .tree-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .header-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 700;
      font-size: 16px;
      color: #0F172A;

      .title-icon { color: #2563EB; font-size: 19px; }
    }

    .add-root-btn {
      border-radius: 9999px;
      font-weight: 600;
      padding: 6px 14px;
    }
  }

  .tree-search-bar {
    margin-bottom: 14px;

    .pill-search-input {
      :deep(.el-input__wrapper) {
        border-radius: 9999px;
        background: #F8FAFC;
        box-shadow: none;
        border: 1px solid #E2E8F0;
        padding: 2px 14px;

        &.is-focus {
          background: #FFFFFF;
          border-color: #2563EB;
          box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.12);
        }
      }
    }
  }

  .tree-wrapper {
    max-height: 640px;
    overflow-y: auto;
    padding-right: 4px;

    :deep(.el-tree-node__content) {
      height: 40px;
      border-radius: 12px;
      margin-bottom: 4px;
      transition: all 0.2s ease;

      &:hover {
        background: #F1F5F9;
      }
    }

    :deep(.el-tree-node.is-current > .el-tree-node__content) {
      background: #EFF6FF !important;
      border-left: 3px solid #2563EB;
    }

    .custom-tree-node {
      display: flex;
      justify-content: space-between;
      align-items: center;
      width: 100%;
      padding-right: 8px;

      .node-left {
        display: flex;
        align-items: center;
        gap: 7px;

        .node-icon {
          font-size: 16px;
          &.CAMPUS { color: #2563EB; }
          &.FACULTY, &.COLLEGE { color: #16A34A; }
          &.DEPT { color: #D97706; }
          &.CLASS { color: #7C3AED; }
        }

        .node-label {
          font-size: 13px;
          color: #1E293B;
          font-weight: 600;
        }

        .node-pill-tag {
          font-size: 10.5px;
          font-weight: 700;
          padding: 1px 8px;
          border-radius: 9999px;

          &.CAMPUS { background: #EFF6FF; color: #2563EB; }
          &.FACULTY, &.COLLEGE { background: #F0FDF4; color: #16A34A; }
          &.DEPT { background: #FFFBEB; color: #D97706; }
          &.CLASS { background: #FAF5FF; color: #9333EA; }
        }
      }

      .node-actions {
        .node-more-btn {
          width: 26px;
          height: 26px;
          border-radius: 9999px;
          border: none;
          background: transparent;
          color: #94A3B8;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;

          &:hover {
            background: #E2E8F0;
            color: #0F172A;
          }
        }
      }
    }

    .empty-tree-state {
      padding: 36px 12px;
      text-align: center;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;

      .empty-tree-desc {
        font-size: 13px;
        color: #64748B;
        margin: 6px 0 16px;
        line-height: 1.5;
      }
    }
  }
}
</style>
