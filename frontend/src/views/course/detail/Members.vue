<template>
  <div class="course-members-page">
    <MembersStatOverview
      :total-count="members.length"
      :student-count="studentMembers.length"
      :staff-count="staffMembers.length"
      :average-progress="averageProgress"
    />

    <MembersToolbar
      v-model:current-role-tab="currentRoleTab"
      v-model:search-keyword="searchKeyword"
      :role-tabs="roleTabs"
      @add="showAddDialog = true"
    />

    <MembersTable
      :members="filteredMembers"
      :loading="loading"
      :broken-avatars="brokenAvatars"
      :resolve-member-avatar="resolveMemberAvatar"
      :mark-avatar-broken="markAvatarBroken"
      :get-role-label="getRoleLabel"
      :get-role-class="getRoleClass"
      :get-avatar-class="getAvatarClass"
      @view-portrait="handleViewPortrait"
      @remove="handleRemove"
    />

    <MembersAddDialog
      v-model:visible="showAddDialog"
      v-model:new-user-id="newUserId"
      v-model:new-member-role="newMemberRole"
      :adding="adding"
      @submit="handleAddSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router';
import MembersStatOverview from '@/components/course/MembersStatOverview.vue';
import MembersToolbar from '@/components/course/MembersToolbar.vue';
import MembersTable from '@/components/course/MembersTable.vue';
import MembersAddDialog from '@/components/course/MembersAddDialog.vue';
import { useCourseMembersPage } from '@/composables/course/useCourseMembersPage';

const route = useRoute();
const courseId = Number(route.params.id) || 101;

const {
  members,
  loading,
  searchKeyword,
  currentRoleTab,
  showAddDialog,
  newUserId,
  newMemberRole,
  adding,
  brokenAvatars,
  studentMembers,
  staffMembers,
  averageProgress,
  roleTabs,
  filteredMembers,
  resolveMemberAvatar,
  markAvatarBroken,
  getRoleLabel,
  getRoleClass,
  getAvatarClass,
  handleViewPortrait,
  handleRemove,
  handleAddSubmit
} = useCourseMembersPage(courseId);
</script>

<style scoped lang="scss">
.course-members-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
</style>
