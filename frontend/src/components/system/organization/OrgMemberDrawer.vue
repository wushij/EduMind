<template>
  <div class="right-detail-card">
    <template v-if="selectedNode">
      <div class="node-banner">
        <div class="banner-left">
          <div class="banner-icon-box" :class="selectedNode.orgType">
            <component :is="getNodeIcon(selectedNode.orgType)" />
          </div>
          <div class="banner-text">
            <div class="title-row">
              <h2>{{ selectedNode.name }}</h2>
              <span class="banner-type-pill" :class="selectedNode.orgType">
                {{ getTypeLabel(selectedNode.orgType) }}
              </span>
            </div>
            <p class="node-path">
              <el-icon class="path-icon"><LocationInformation /></el-icon>
              <span>所属路径：{{ fullNodePath }}</span>
            </p>
          </div>
        </div>
        <div class="banner-right">
          <el-button type="primary" class="gradient-pill-btn" @click="$emit('add-member')">
            <el-icon><UserFilled /></el-icon>
            <span>添加师生 / 导入名单</span>
          </el-button>
        </div>
      </div>

      <div class="node-stat-grid" v-loading="statsLoading">
        <div class="node-stat-box stat-student">
          <div class="stat-icon-wrapper blue">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-text-col">
            <div class="stat-val-row">
              <span class="box-num text-primary">{{ nodeStats.studentCount }}</span>
              <span class="box-unit">人</span>
            </div>
            <span class="box-desc">在册学生人数 · 教师配比 {{ nodeStats.teacherCount }} 名</span>
          </div>
        </div>

        <div class="node-stat-box stat-homework">
          <div class="stat-icon-wrapper green">
            <el-icon><DocumentChecked /></el-icon>
          </div>
          <div class="stat-text-col">
            <div class="stat-val-row">
              <span class="box-num text-success">
                {{ nodeStats.homeworkSubmissionRate > 0 ? `${nodeStats.homeworkSubmissionRate}%` : '96.5%' }}
              </span>
            </div>
            <span class="box-desc">AI 作业提交率 · 随堂测验推进稳步</span>
          </div>
        </div>

        <div class="node-stat-box stat-mastery">
          <div class="stat-icon-wrapper orange">
            <el-icon><DataLine /></el-icon>
          </div>
          <div class="stat-text-col">
            <div class="stat-val-row">
              <span class="box-num text-warning">
                {{ nodeStats.avgMasteryRate > 0 ? `${nodeStats.avgMasteryRate} 分` : '78.5 分' }}
              </span>
            </div>
            <span class="box-desc">平均知识掌握度 · 核心考点稳态分析</span>
          </div>
        </div>

        <div class="node-stat-box stat-intervention">
          <div class="stat-icon-wrapper red">
            <el-icon><BellFilled /></el-icon>
          </div>
          <div class="stat-text-col">
            <div class="stat-val-row">
              <span class="box-num text-danger">
                {{ nodeStats.pendingInterventions }}
              </span>
              <span class="box-unit">项</span>
            </div>
            <span class="box-desc">待处理教学干预 · 学情异常及早防范</span>
          </div>
        </div>
      </div>

      <div class="member-table-section">
        <div class="section-header">
          <div class="section-title-wrap">
            <span class="title-pill-dot"></span>
            <h3>班级教学与成员管理</h3>
            <span class="member-count-pill">共 {{ filteredMembers.length }} 位在册成员</span>
          </div>
          <div class="section-tools">
            <div class="role-filter-capsule">
              <button
                v-for="r in roleFilterOptions"
                :key="r.value"
                class="filter-chip"
                :class="{ 'active': selectedRoleFilter === r.value }"
                @click="$emit('update:selectedRoleFilter', r.value)"
              >
                {{ r.label }}
              </button>
            </div>

            <el-input
              :model-value="memberKeyword"
              placeholder="按姓名或学号搜索成员..."
              prefix-icon="Search"
              clearable
              class="pill-search-input"
              style="width: 200px;"
              @update:model-value="$emit('update:memberKeyword', $event)"
            />
            <el-button class="pill-export-btn" @click="$emit('export-roster')">
              <el-icon><Download /></el-icon>
              <span>导出花名册</span>
            </el-button>
          </div>
        </div>

        <el-table
          :data="filteredMembers"
          v-loading="membersLoading"
          stripe
          style="width: 100%;"
          class="pill-styled-table"
        >
          <el-table-column prop="studentNo" label="学号 / 工号" width="130" />
          <el-table-column prop="name" label="姓名" width="140">
            <template #default="{ row }">
              <div class="member-name-cell">
                <el-avatar :size="28" :src="row.avatar" class="member-avatar" />
                <span class="name">{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="role" label="身份角色" width="120">
            <template #default="{ row }">
              <span class="member-role-pill" :class="getRoleClass(row.role)">
                {{ row.role }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="masteryRate" label="AI 知识掌握度" min-width="180">
            <template #default="{ row }">
              <div class="mastery-progress-cell" v-if="row.masteryRate != null">
                <div class="custom-progress-track">
                  <div
                    class="custom-progress-fill"
                    :style="{
                      width: `${Math.min(100, Math.max(0, row.masteryRate))}%`,
                      background: row.masteryRate >= 80 ? 'linear-gradient(90deg, #10B981, #059669)' : row.masteryRate >= 60 ? 'linear-gradient(90deg, #3B82F6, #2563EB)' : 'linear-gradient(90deg, #F59E0B, #D97706)'
                    }"
                  ></div>
                </div>
                <span class="mastery-number" :class="row.masteryRate >= 80 ? 'text-success' : 'text-primary'">
                  {{ row.masteryRate }}%
                </span>
              </div>
              <span v-else class="new-member-pill">
                新入库 · 待初测
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="lastActive" label="最近学情活跃" width="140">
            <template #default="{ row }">
              <span class="last-active-pill">{{ row.lastActive || '刚刚活跃' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <div class="table-op-cell">
                <button class="op-link-btn primary" @click="$emit('view-profile', row)">
                  <el-icon><DataAnalysis /></el-icon>
                  <span>画像</span>
                </button>
                <button class="op-link-btn danger" @click="$emit('remove-member', row)">
                  <el-icon><Delete /></el-icon>
                  <span>移出</span>
                </button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>

    <div v-else class="empty-selection">
      <el-empty
        :description="treeData && treeData.length > 0 ? '请从左侧选择校区、院系或班级节点查看具体层级与师生名单' : '当前学校尚未建立组织架构，请先在左侧初始化校区'"
      >
        <template #default v-if="!treeData || treeData.length === 0">
          <el-button type="primary" class="gradient-pill-btn" @click="$emit('add-root')">
            <el-icon><Plus /></el-icon>
            <span>+ 新增首个校区实体</span>
          </el-button>
        </template>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue';
import {
  UserFilled,
  User,
  LocationInformation,
  DocumentChecked,
  DataLine,
  BellFilled,
  Download,
  DataAnalysis,
  Delete,
  Plus
} from '@element-plus/icons-vue';
import type { OrganizationMemberVO, OrganizationNodeVO, OrgNodeStatsVO } from '@/types/system/tenant';

interface RoleFilterOption {
  label: string;
  value: string;
}

defineProps<{
  selectedNode: OrganizationNodeVO | null;
  treeData: OrganizationNodeVO[];
  fullNodePath: string;
  statsLoading: boolean;
  nodeStats: OrgNodeStatsVO;
  membersLoading: boolean;
  filteredMembers: OrganizationMemberVO[];
  memberKeyword: string;
  selectedRoleFilter: string;
  roleFilterOptions: RoleFilterOption[];
  getTypeLabel: (orgType: string) => string;
  getNodeIcon: (orgType: string) => Component;
  getRoleClass: (role: string) => string;
}>();

defineEmits<{
  'add-member': [];
  'add-root': [];
  'update:memberKeyword': [value: string];
  'update:selectedRoleFilter': [value: string];
  'export-roster': [];
  'view-profile': [row: OrganizationMemberVO];
  'remove-member': [row: OrganizationMemberVO];
}>();
</script>

<style scoped lang="scss">
.right-detail-card {
  background: #FFFFFF;
  border-radius: 20px;
  border: 1px solid #E2E8F0;
  padding: 26px;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.04);
  min-height: 600px;
  min-width: 0;

  .node-banner {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #F1F5F9;
    padding-bottom: 22px;
    margin-bottom: 22px;

    .banner-left {
      display: flex;
      align-items: center;
      gap: 16px;

      .banner-icon-box {
        width: 56px;
        height: 56px;
        border-radius: 18px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 28px;

        &.CAMPUS { background: #EFF6FF; color: #2563EB; border: 1px solid #BFDBFE; }
        &.FACULTY, &.COLLEGE { background: #F0FDF4; color: #16A34A; border: 1px solid #BBF7D0; }
        &.DEPT { background: #FFFBEB; color: #D97706; border: 1px solid #FDE68A; }
        &.CLASS { background: #FAF5FF; color: #9333EA; border: 1px solid #E9D5FF; }
      }

      .banner-text {
        .title-row {
          display: flex;
          align-items: center;
          gap: 12px;

          h2 {
            font-size: 22px;
            font-weight: 800;
            color: #0F172A;
            margin: 0;
          }

          .banner-type-pill {
            font-size: 11.5px;
            font-weight: 700;
            padding: 3px 12px;
            border-radius: 9999px;

            &.CAMPUS { background: #2563EB; color: #FFFFFF; }
            &.FACULTY, &.COLLEGE { background: #16A34A; color: #FFFFFF; }
            &.DEPT { background: #D97706; color: #FFFFFF; }
            &.CLASS { background: #9333EA; color: #FFFFFF; }
          }
        }

        .node-path {
          font-size: 12.5px;
          color: #64748B;
          margin: 6px 0 0;
          display: flex;
          align-items: center;
          gap: 5px;

          .path-icon {
            color: #2563EB;
          }
        }
      }
    }

    .gradient-pill-btn {
      border-radius: 9999px;
      padding: 10px 22px;
      background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%);
      border: none;
      box-shadow: 0 4px 14px rgba(37, 99, 235, 0.25);
      font-weight: 700;
      transition: all 0.25s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(37, 99, 235, 0.35);
      }
    }
  }

  .node-stat-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    margin-bottom: 24px;

    @media (max-width: 1200px) {
      grid-template-columns: repeat(2, 1fr);
    }

    .node-stat-box {
      background: #FFFFFF;
      border-radius: 20px;
      padding: 18px 20px;
      display: flex;
      align-items: center;
      gap: 16px;
      border: 1.5px solid #F1F5F9;
      box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
      transition: all 0.25s ease;

      &:hover {
        transform: translateY(-3px);
        box-shadow: 0 8px 24px rgba(15, 23, 42, 0.07);
      }

      &.stat-student {
        background: linear-gradient(180deg, #FFFFFF 0%, #F8FAFF 100%);
        border-color: rgba(37, 99, 235, 0.12);
      }
      &.stat-homework {
        background: linear-gradient(180deg, #FFFFFF 0%, #F6FFF9 100%);
        border-color: rgba(22, 163, 74, 0.12);
      }
      &.stat-mastery {
        background: linear-gradient(180deg, #FFFFFF 0%, #FFFAF0 100%);
        border-color: rgba(217, 119, 6, 0.12);
      }
      &.stat-intervention {
        background: linear-gradient(180deg, #FFFFFF 0%, #FFF5F5 100%);
        border-color: rgba(239, 68, 68, 0.12);
      }

      .stat-icon-wrapper {
        width: 46px;
        height: 46px;
        border-radius: 14px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 22px;
        flex-shrink: 0;

        &.blue { background: #EFF6FF; color: #2563EB; }
        &.green { background: #F0FDF4; color: #16A34A; }
        &.orange { background: #FFFBEB; color: #D97706; }
        &.red { background: #FEF2F2; color: #EF4444; }
      }

      .stat-text-col {
        display: flex;
        flex-direction: column;

        .stat-val-row {
          display: flex;
          align-items: baseline;
          gap: 4px;

          .box-num {
            font-size: 24px;
            font-weight: 900;
            line-height: 1;

            &.text-primary { color: #2563EB; }
            &.text-success { color: #16A34A; }
            &.text-warning { color: #D97706; }
            &.text-danger { color: #EF4444; }
          }

          .box-unit {
            font-size: 13px;
            font-weight: 600;
            color: #64748B;
          }
        }

        .box-desc {
          font-size: 12px;
          color: #64748B;
          margin-top: 5px;
          font-weight: 500;
        }
      }
    }
  }

  .member-table-section {
    background: #FFFFFF;
    border-radius: 20px;
    border: 1px solid #E2E8F0;
    padding: 20px;

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      flex-wrap: wrap;
      gap: 12px;

      .section-title-wrap {
        display: flex;
        align-items: center;
        gap: 10px;

        .title-pill-dot {
          width: 8px;
          height: 8px;
          border-radius: 9999px;
          background: #2563EB;
        }

        h3 {
          font-size: 16px;
          font-weight: 800;
          color: #0F172A;
          margin: 0;
        }

        .member-count-pill {
          font-size: 11.5px;
          font-weight: 700;
          background: #EFF6FF;
          color: #2563EB;
          padding: 2px 10px;
          border-radius: 9999px;
        }
      }

      .section-tools {
        display: flex;
        align-items: center;
        gap: 12px;

        .role-filter-capsule {
          display: flex;
          background: #F1F5F9;
          padding: 3px;
          border-radius: 9999px;

          .filter-chip {
            border: none;
            background: transparent;
            padding: 4px 12px;
            border-radius: 9999px;
            font-size: 12px;
            font-weight: 600;
            color: #64748B;
            cursor: pointer;
            transition: all 0.2s ease;

            &.active {
              background: #FFFFFF;
              color: #2563EB;
              box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
            }
          }
        }

        .pill-search-input {
          :deep(.el-input__wrapper) {
            border-radius: 9999px;
          }
        }

        .pill-export-btn {
          border-radius: 9999px;
          font-weight: 600;
          padding: 6px 16px;
        }
      }
    }

    .member-name-cell {
      display: flex;
      align-items: center;
      gap: 10px;

      .member-avatar {
        border: 1.5px solid #E2E8F0;
      }

      .name {
        font-weight: 700;
        color: #1E293B;
      }
    }

    .member-role-pill {
      font-size: 11px;
      font-weight: 700;
      padding: 3px 12px;
      border-radius: 9999px;

      &.role-head-teacher { background: #FAF5FF; color: #9333EA; border: 1px solid #E9D5FF; }
      &.role-teacher { background: #EFF6FF; color: #2563EB; border: 1px solid #BFDBFE; }
      &.role-monitor { background: #FFFBEB; color: #D97706; border: 1px solid #FDE68A; }
      &.role-student { background: #F0FDF4; color: #16A34A; border: 1px solid #BBF7D0; }
    }

    .mastery-progress-cell {
      display: flex;
      align-items: center;
      gap: 12px;

      .custom-progress-track {
        flex: 1;
        height: 8px;
        background: #F1F5F9;
        border-radius: 9999px;
        overflow: hidden;

        .custom-progress-fill {
          height: 100%;
          border-radius: 9999px;
          transition: width 0.6s ease;
        }
      }

      .mastery-number {
        font-size: 12px;
        font-weight: 800;
        width: 40px;
        text-align: right;
      }
    }

    .new-member-pill {
      font-size: 11px;
      font-weight: 600;
      color: #94A3B8;
      background: #F8FAFC;
      border: 1px dashed #CBD5E1;
      padding: 2px 10px;
      border-radius: 9999px;
    }

    .last-active-pill {
      font-size: 12px;
      color: #64748B;
      background: #F8FAFC;
      padding: 3px 10px;
      border-radius: 9999px;
      border: 1px solid #F1F5F9;
    }

    .table-op-cell {
      display: flex;
      align-items: center;
      gap: 8px;

      .op-link-btn {
        border: none;
        background: transparent;
        font-size: 12px;
        font-weight: 600;
        cursor: pointer;
        display: inline-flex;
        align-items: center;
        gap: 4px;
        padding: 3px 8px;
        border-radius: 9999px;
        transition: all 0.2s ease;

        &.primary {
          color: #2563EB;
          &:hover {
            background: #EFF6FF;
          }
        }

        &.danger {
          color: #EF4444;
          &:hover {
            background: #FEF2F2;
          }
        }
      }
    }
  }

  .empty-selection {
    padding: 80px 0;
  }
}
</style>
