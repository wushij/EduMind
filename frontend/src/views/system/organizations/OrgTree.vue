<template>
  <div class="org-tree-page" v-loading="loading">
    <PageHeroBanner
      title="组织与班级架构 · 多级教学实体编排"
      subtitle="清晰编排集团校、校区、院系/年级、教研组与行政教学班级，实现师生权限精准继承与班级化 AI 赋能"
      background-variant="system"
    >
      <template #extra>
        <OrgTreeHeroStats :tenant-stats="tenantStats" />
      </template>
    </PageHeroBanner>

    <div class="main-content-layout">
      <div class="split-workbench">
        <OrgTreePanel
          :tree-ref="treeRef"
          :tree-data="treeData"
          v-model:tree-search-keyword="treeSearchKeyword"
          :selected-node="selectedNode"
          :filter-tree-node="filterTreeNode"
          :get-type-label="getTypeLabel"
          @add-root="openAddRootDialog"
          @node-click="handleNodeClick"
          @tree-command="handleTreeCommand"
        />

        <OrgMemberDrawer
          :selected-node="selectedNode"
          :tree-data="treeData"
          :full-node-path="fullNodePath"
          :stats-loading="statsLoading"
          :node-stats="nodeStats"
          :members-loading="membersLoading"
          :filtered-members="filteredMembers"
          v-model:member-keyword="memberKeyword"
          v-model:selected-role-filter="selectedRoleFilter"
          :role-filter-options="roleFilterOptions"
          :get-type-label="getTypeLabel"
          :get-node-icon="getNodeIcon"
          :get-role-class="getRoleClass"
          @add-member="openAddMemberDialog"
          @add-root="openAddRootDialog"
          @export-roster="exportRoster"
          @view-profile="viewStudentProfile"
          @remove-member="removeMember"
        />
      </div>
    </div>

    <OrgNodeEditDialog
      v-model="nodeDialogVisible"
      :mode="nodeFormMode"
      :parent-node="activeParentNode"
      :edit-node="activeEditNode"
      @success="loadTree"
    />

    <MemberAssignDialog
      v-model="memberDialogVisible"
      :org-node="selectedNode"
      @success="handleMemberAssignSuccess"
    />

    <StudentProfileDrawer
      v-model="profileDrawerVisible"
      :student="activeProfileStudent"
    />
  </div>
</template>

<script setup lang="ts">
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import OrgTreeHeroStats from '@/components/system/organization/OrgTreeHeroStats.vue';
import OrgTreePanel from '@/components/system/organization/OrgTreePanel.vue';
import OrgMemberDrawer from '@/components/system/organization/OrgMemberDrawer.vue';
import OrgNodeEditDialog from '@/components/system/organization/OrgNodeEditDialog.vue';
import MemberAssignDialog from '@/components/system/organization/MemberAssignDialog.vue';
import StudentProfileDrawer from '@/components/system/organization/StudentProfileDrawer.vue';
import { useOrganization } from '@/composables/system/useOrganization';

const {
  loading,
  membersLoading,
  statsLoading,
  treeSearchKeyword,
  treeRef,
  treeData,
  selectedNode,
  memberKeyword,
  selectedRoleFilter,
  roleFilterOptions,
  tenantStats,
  nodeStats,
  nodeDialogVisible,
  nodeFormMode,
  activeParentNode,
  activeEditNode,
  memberDialogVisible,
  profileDrawerVisible,
  activeProfileStudent,
  filteredMembers,
  fullNodePath,
  filterTreeNode,
  getTypeLabel,
  getNodeIcon,
  getRoleClass,
  loadTree,
  handleNodeClick,
  openAddRootDialog,
  handleTreeCommand,
  openAddMemberDialog,
  handleMemberAssignSuccess,
  viewStudentProfile,
  removeMember,
  exportRoster
} = useOrganization();
</script>

<style scoped lang="scss">
.org-tree-page {
  padding-bottom: 40px;

  .main-content-layout {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .split-workbench {
    display: grid;
    grid-template-columns: 360px minmax(0, 1fr);
    gap: 22px;
    align-items: start;
    width: 100%;

    @media (max-width: 1024px) {
      grid-template-columns: 1fr;
    }
  }
}
</style>
