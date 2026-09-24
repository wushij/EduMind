<template>
  <div class="intervention-center-page" v-loading="loading">
    <PageHeroBanner
      title="教学干预决策工作台 · 循证教学与精准干预"
      subtitle="AI 诊断引擎持续捕捉班级学情异动与薄弱断层，自动生成教学干预策略提案，支持教师一键审核、微课定向分发与效果闭环跟踪"
      background-variant="analytics"
    >
      <template #extra>
        <div class="hero-stats-row">
          <div class="hero-stat-card">
            <span class="stat-num text-danger">{{ pendingCount }}</span>
            <span class="stat-label">待审核干预提案</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-primary">{{ totalAffectedStudents }}</span>
            <span class="stat-label">覆盖预警学生</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-success">+14.8%</span>
            <span class="stat-label">干预后掌握度提升</span>
          </div>
          <div class="hero-stat-card">
            <span class="stat-num text-info">96.2%</span>
            <span class="stat-label">闭环完成率</span>
          </div>
        </div>
      </template>
    </PageHeroBanner>

    <div class="main-content-layout">
      <!-- 过滤与工具控制栏 -->
      <div class="filter-card">
        <div class="filter-left">
          <el-select v-model="courseFilter" placeholder="关联课程" clearable filterable style="width: 220px;">
            <el-option label="全部课程" :value="undefined" />
            <el-option v-for="c in courseOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>

          <el-select v-model="triggerFilter" placeholder="预警触发动因" clearable style="width: 180px;">
            <el-option label="全部动因" :value="undefined" />
            <el-option label="考试失分率偏高 (EXAM_WEAK)" value="EXAM_WEAK" />
            <el-option label="学习活跃度骤降 (ACTIVITY_DROP)" value="ACTIVITY_DROP" />
            <el-option label="作业多次逾期 (HOMEWORK_DELAY)" value="HOMEWORK_DELAY" />
          </el-select>

          <el-select v-model="statusFilter" placeholder="干预状态" clearable style="width: 140px;">
            <el-option label="全部状态" :value="undefined" />
            <el-option label="待审核 (PENDING)" value="PENDING" />
            <el-option label="已批准 (APPROVED)" value="APPROVED" />
            <el-option label="已分发 (DISPATCHED)" value="DISPATCHED" />
          </el-select>
        </div>

        <div class="filter-right">
          <el-button type="primary" class="gradient-btn" @click="openCreateDialog">
            <el-icon><Plus /></el-icon>
            <span>新建干预提案</span>
          </el-button>
          <el-button @click="openTriggerSimulator">
            <el-icon><MagicStick /></el-icon>
            <span>诊断巡检</span>
          </el-button>
        </div>
      </div>

      <!-- 干预决策流水卡片列表 -->
      <div class="interventions-list" v-if="filteredInterventions.length > 0">
        <div
          v-for="item in filteredInterventions"
          :key="item.id"
          class="intervention-card"
          :class="item.status.toLowerCase()"
        >
          <div class="card-left-indicator" :class="item.triggerType"></div>

          <div class="card-main">
            <div class="card-top-header">
              <div class="title-meta-left">
                <el-tag :type="getTriggerTagType(item.triggerType)" effect="light">
                  {{ getTriggerLabel(item.triggerType) }}
                </el-tag>
                <el-tag type="info" size="small">{{ item.courseName }}</el-tag>
                <span class="create-time">{{ item.createTime || '4小时前' }}</span>
              </div>
              <div class="status-meta-right">
                <el-tag :type="getStatusTagType(item.status)" effect="dark" round>
                  {{ getStatusLabel(item.status) }}
                </el-tag>
              </div>
            </div>

            <h3 class="intervention-title">{{ item.title }}</h3>

            <div class="proposal-box">
              <div class="proposal-icon"><el-icon><Opportunity /></el-icon></div>
              <div class="proposal-content">
                <span class="proposal-label">AI 循证干预方案：</span>
                <p>{{ item.proposalText }}</p>
              </div>
            </div>

            <div class="student-impact-bar">
              <div class="impact-left">
                <span class="impact-label">受影响学生群组 ({{ item.affectedStudentCount }} 人)：</span>
                <div class="avatar-pile">
                  <el-avatar :size="24" src="https://api.dicebear.com/7.x/avataaars/svg?seed=Felix" />
                  <el-avatar :size="24" src="https://api.dicebear.com/7.x/avataaars/svg?seed=Lily" />
                  <el-avatar :size="24" src="https://api.dicebear.com/7.x/avataaars/svg?seed=Jack" />
                  <span class="more-count" v-if="item.affectedStudentCount > 3">+{{ item.affectedStudentCount - 3 }}</span>
                </div>
              </div>
              <div class="impact-right">
                <span v-if="item.approvedBy" class="approver-info">审核人：{{ item.approvedBy }}</span>
              </div>
            </div>

            <div class="card-action-bar">
              <div class="action-left">
                <el-button link size="small" type="primary" @click="viewEfficacyTrace(item)">
                  <el-icon><DataLine /></el-icon> 查看归因画像与微课预览
                </el-button>
              </div>
              <div class="action-right">
                <template v-if="item.status === 'PENDING'">
                  <el-button size="small" @click="handleReject(item)">驳回建议</el-button>
                  <el-button size="small" type="primary" plain @click="handleCustomize(item)">调整微课与习题</el-button>
                  <el-button size="small" type="primary" class="gradient-btn" @click="handleApprove(item)">
                    <el-icon><Check /></el-icon> 审核通过并下发
                  </el-button>
                </template>
                <template v-else-if="item.status === 'APPROVED'">
                  <el-button size="small" type="success" @click="handleDispatch(item)">
                    <el-icon><Promotion /></el-icon> 立即推送至学生任务中心
                  </el-button>
                </template>
                <template v-else>
                  <el-tag size="small" type="success" effect="plain">已分发完成 · 正在收集反馈</el-tag>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-else description="当前没有任何待处理的教学干预提案" />
    </div>

    <!-- 归因画像与微课预览抽屉 -->
    <el-drawer v-model="drawerVisible" title="教学干预详情与学情归因画像" size="560px">
      <div class="drawer-content" v-if="activeIntervention">
        <div class="drawer-section">
          <h4>干预预警动因</h4>
          <p class="section-text">{{ activeIntervention.title }}</p>
        </div>

        <div class="drawer-section">
          <h4>AI 靶向攻坚微课与变式题推荐清单</h4>
          <div class="resource-card">
            <div class="res-title">微课：《导数极值与罗尔中值定理图解通关》 (8分20秒)</div>
            <div class="res-desc">精讲洛必达法则适用场景与极值点前后符号判断易错陷阱。</div>
          </div>
          <div class="resource-card mt-2">
            <div class="res-title">靶向变式题组：3 道等价代换选择题 + 1 道导数压轴解答题</div>
            <div class="res-desc">基于 Bloom 认知层级自动匹配该群组能力区间。</div>
          </div>
        </div>

        <div class="drawer-section">
          <h4>干预实施预期收益</h4>
          <div class="efficacy-box">
            <span class="efficacy-num">+15 ~ +22%</span>
            <span class="efficacy-desc">预计完成该干预包后，学生在下次章节测验的同类考点得分率</span>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- 新建干预提案对话框 -->
    <el-dialog v-model="createDialogVisible" title="新建教学干预提案" width="560px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" label-position="top">
        <el-form-item label="关联课程">
          <el-select v-model="createForm.courseId" placeholder="选择关联课程" filterable style="width: 100%" @change="onCourseChange">
            <el-option v-for="c in courseOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="触发动因类型">
          <el-select v-model="createForm.triggerType" placeholder="选择触发动因" style="width: 100%">
            <el-option label="考试薄弱断层 (EXAM_WEAK)" value="EXAM_WEAK" />
            <el-option label="学习活跃度骤降 (ACTIVITY_DROP)" value="ACTIVITY_DROP" />
            <el-option label="作业多次逾期 (HOMEWORK_DELAY)" value="HOMEWORK_DELAY" />
          </el-select>
        </el-form-item>

        <el-form-item label="干预提案标题" required>
          <el-input v-model="createForm.title" placeholder="如：第3章导数与极限掌握度预警..." maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="循证干预方案描述" required>
          <el-input
            v-model="createForm.proposalText"
            type="textarea"
            :rows="3"
            placeholder="说明 AI 诊断发现的问题及具体干预措施建议..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="预警影响学生数">
          <el-input-number v-model="createForm.affectedStudentCount" :min="1" :max="500" style="width: 100%" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="submitCreateIntervention">确定创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { MagicStick, Opportunity, DataLine, Check, Promotion, Plus } from '@element-plus/icons-vue';
import PageHeroBanner from '@/components/common/PageHeroBanner.vue';
import { useIntervention } from '@/composables/analytics/useIntervention';

const {
  loading,
  courseOptions,
  courseFilter,
  triggerFilter,
  statusFilter,
  drawerVisible,
  activeIntervention,
  createDialogVisible,
  createSubmitting,
  createForm,
  pendingCount,
  totalAffectedStudents,
  filteredInterventions,
  openCreateDialog,
  onCourseChange,
  submitCreateIntervention,
  handleApprove,
  handleDispatch,
  handleReject,
  handleCustomize,
  viewEfficacyTrace,
  openTriggerSimulator,
  getTriggerLabel,
  getTriggerTagType,
  getStatusLabel,
  getStatusTagType
} = useIntervention();
</script>

<style scoped lang="scss">
.intervention-center-page {
  padding-bottom: 40px;

  .hero-stats-row {
    display: flex;
    gap: 14px;
    margin-top: 4px;
    flex-wrap: wrap;

    .hero-stat-card {
      background: rgba(255, 255, 255, 0.92);
      backdrop-filter: blur(8px);
      padding: 6px 20px;
      border-radius: 9999px;
      border: 1.5px solid rgba(37, 99, 235, 0.12);
      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
      display: flex;
      align-items: center;
      gap: 10px;
      transition: all 0.25s ease;

      &:hover {
        background: #FFFFFF;
        border-color: #2563EB;
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(37, 99, 235, 0.1);
      }

      .stat-num {
        font-size: 18px;
        font-weight: 800;
        line-height: 1;

        &.text-primary { color: #2563EB; }
        &.text-success { color: #16A34A; }
        &.text-warning { color: #D97706; }
        &.text-danger { color: #DC2626; }
        &.text-info { color: #0284C7; }
      }

      .stat-label {
        font-size: 12.5px;
        font-weight: 500;
        color: #475569;
        margin-top: 0;
        white-space: nowrap;
      }
    }
  }

  .main-content-layout {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .filter-card {
    background: #FFFFFF;
    border-radius: 14px;
    padding: 16px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
    margin-bottom: 22px;

    .filter-left {
      display: flex;
      gap: 14px;
      align-items: center;
    }

    .gradient-btn {
      background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%);
      border: none;
      border-radius: 10px;
    }
  }

  .interventions-list {
    display: flex;
    flex-direction: column;
    gap: 18px;

    .intervention-card {
      background: #FFFFFF;
      border-radius: 16px;
      border: 1px solid #E2E8F0;
      box-shadow: 0 4px 16px rgba(15, 23, 42, 0.03);
      display: flex;
      overflow: hidden;
      transition: all 0.2s ease;

      &:hover {
        box-shadow: 0 8px 24px rgba(37, 99, 235, 0.08);
        border-color: #BFDBFE;
      }

      .card-left-indicator {
        width: 6px;
        flex-shrink: 0;

        &.EXAM_WEAK { background: #EF4444; }
        &.ACTIVITY_DROP { background: #F59E0B; }
        &.HOMEWORK_DELAY { background: #64748B; }
      }

      .card-main {
        flex: 1;
        padding: 20px 24px;

        .card-top-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 12px;

          .title-meta-left {
            display: flex;
            align-items: center;
            gap: 10px;

            .create-time {
              font-size: 12px;
              color: #94A3B8;
            }
          }
        }

        .intervention-title {
          font-size: 16px;
          font-weight: 600;
          color: #0F172A;
          margin: 0 0 14px;
        }

        .proposal-box {
          background: #F8FAFC;
          border-left: 3px solid #2563EB;
          border-radius: 8px;
          padding: 12px 16px;
          display: flex;
          gap: 12px;
          margin-bottom: 16px;

          .proposal-icon {
            color: #2563EB;
            font-size: 20px;
            flex-shrink: 0;
            margin-top: 2px;
          }

          .proposal-content {
            .proposal-label {
              font-size: 12px;
              font-weight: 600;
              color: #1E40AF;
              margin-bottom: 4px;
              display: block;
            }

            p {
              font-size: 13px;
              color: #334155;
              line-height: 1.6;
              margin: 0;
            }
          }
        }

        .student-impact-bar {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 16px;
          padding-bottom: 14px;
          border-bottom: 1px solid #F1F5F9;

          .impact-left {
            display: flex;
            align-items: center;
            gap: 10px;

            .impact-label {
              font-size: 13px;
              color: #475569;
            }

            .avatar-pile {
              display: flex;
              align-items: center;
              margin-left: 4px;

              :deep(.el-avatar) {
                border: 2px solid #FFFFFF;
                margin-left: -6px;
                &:first-child { margin-left: 0; }
              }

              .more-count {
                font-size: 11px;
                color: #64748B;
                margin-left: 8px;
              }
            }
          }

          .approver-info {
            font-size: 12px;
            color: #64748B;
          }
        }

        .card-action-bar {
          display: flex;
          justify-content: space-between;
          align-items: center;

          .action-right {
            display: flex;
            gap: 10px;

            .gradient-btn {
              background: linear-gradient(135deg, #2563EB 0%, #4F46E5 100%);
              border: none;
            }
          }
        }
      }
    }
  }

  .drawer-content {
    .drawer-section {
      margin-bottom: 22px;

      h4 {
        font-size: 14px;
        font-weight: 600;
        color: #0F172A;
        margin: 0 0 10px;
      }

      .section-text {
        font-size: 13px;
        color: #334155;
        line-height: 1.6;
        background: #F8FAFC;
        padding: 10px 14px;
        border-radius: 8px;
      }

      .resource-card {
        background: #EFF6FF;
        border: 1px solid #DBEAFE;
        border-radius: 10px;
        padding: 12px 14px;

        .res-title {
          font-weight: 600;
          font-size: 13px;
          color: #1E40AF;
          margin-bottom: 4px;
        }

        .res-desc {
          font-size: 12px;
          color: #4B5563;
        }

        &.mt-2 { margin-top: 10px; }
      }

      .efficacy-box {
        background: #ECFDF5;
        border: 1px solid #A7F3D0;
        border-radius: 12px;
        padding: 16px;
        text-align: center;

        .efficacy-num {
          font-size: 24px;
          font-weight: 700;
          color: #059669;
          display: block;
          margin-bottom: 6px;
        }

        .efficacy-desc {
          font-size: 12px;
          color: #065F46;
        }
      }
    }
  }
}
</style>
