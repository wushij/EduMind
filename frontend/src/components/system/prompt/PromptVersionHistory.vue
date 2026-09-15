<template>
  <div v-if="promptId && versionHistory.length" class="section-card version-card">
    <div class="section-card-header">
      <div class="sch-left">
        <span class="sec-icon purple"><el-icon><Clock /></el-icon></span>
        <div class="sec-title-wrap">
          <div class="title-with-pill">
            <span class="sec-title">版本历史管理</span>
            <span class="ver-count-pill">{{ versionHistory.length }} 个快照</span>
          </div>
          <span class="sec-sub">支持查看完整历史快照，对生产版本进行一键无缝回滚</span>
        </div>
      </div>
    </div>

    <div class="version-timeline-list">
      <div
        v-for="item in versionHistory"
        :key="item.id"
        class="v-card-item"
        :class="{ 'is-current': isCurrentVersion(item.version) }"
      >
        <div class="v-card-main">
          <div class="v-top-bar">
            <div class="v-pill-cluster">
              <span class="v-tag-pill" :class="{ active: isCurrentVersion(item.version) }">
                v{{ item.version }}.0
              </span>
              <span v-if="isCurrentVersion(item.version)" class="v-status-badge current">
                <span class="pulse-dot"></span>当前运行
              </span>
              <span v-else class="v-status-badge archived">历史快照</span>
            </div>

            <div class="v-timestamp">
              <el-icon class="time-ic"><Timer /></el-icon>
              <span>{{ formatVersionDate(item.createTime) }}</span>
            </div>
          </div>

          <div class="v-change-summary-row">
            <span class="diff-tag-pill" :class="getVersionDiffInfo(item).tagType">
              <el-icon v-if="getVersionDiffInfo(item).tagType === 'base'"><Flag /></el-icon>
              <el-icon v-else-if="getVersionDiffInfo(item).tagType === 'identical'"><InfoFilled /></el-icon>
              <el-icon v-else><EditPen /></el-icon>
              {{ getVersionDiffInfo(item).tag }}
            </span>
            <span class="diff-desc-text" :title="getVersionDiffInfo(item).summary">
              {{ getVersionDiffInfo(item).summary }}
            </span>
          </div>

          <div class="v-bottom-bar">
            <div class="v-meta-tags">
              <span v-if="item.variables" class="v-meta-tag">
                <el-icon><CollectionTag /></el-icon>
                {{ getVariablesList(item.variables).length }} 个插槽
              </span>
              <span v-if="item.systemPrompt" class="v-meta-tag system">
                <el-icon><Cpu /></el-icon>
                含角色设定
              </span>
            </div>

            <div class="v-action-group">
              <el-button
                class="capsule-action-btn detail-btn"
                size="small"
                @click="onOpenDetail(item)"
              >
                <el-icon><View /></el-icon>
                <span>查看详情</span>
              </el-button>

              <el-button
                v-if="!isCurrentVersion(item.version)"
                class="capsule-action-btn rollback-btn"
                size="small"
                :loading="rollingBack"
                @click="onRollback(item.version)"
              >
                <el-icon><RefreshLeft /></el-icon>
                <span>回滚至此</span>
              </el-button>

              <span v-else class="active-running-pill">
                <el-icon><Check /></el-icon>
                <span>生效中</span>
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="versionDetailVisible"
      :title="`版本快照详情 · v${selectedVersion?.version}.0`"
      width="780px"
      top="7vh"
      class="version-detail-dialog"
      destroy-on-close
      append-to-body
    >
      <div v-if="selectedVersion" class="version-detail-content">
        <div class="vd-meta-strip">
          <div class="meta-block">
            <span class="m-label">提示词模板</span>
            <span class="m-val">{{ templateName }}</span>
          </div>
          <div class="meta-block">
            <span class="m-label">模板标识</span>
            <span class="m-val mono">{{ templateCode }}</span>
          </div>
          <div class="meta-block">
            <span class="m-label">发布状态</span>
            <div class="m-val">
              <span
                v-if="isCurrentVersion(selectedVersion.version)"
                class="vd-status-pill online"
              >
                <span class="pulse-dot"></span>当前在线生产版
              </span>
              <span v-else class="vd-status-pill archived">历史归档快照</span>
            </div>
          </div>
          <div class="meta-block">
            <span class="m-label">发布时间</span>
            <span class="m-val">{{ formatVersionDate(selectedVersion.createTime) }}</span>
          </div>
        </div>

        <div class="vd-change-analysis-card" :class="selectedVersionDiffInfo.tagType">
          <div class="ca-header">
            <div class="ca-title-group">
              <span class="ca-pill" :class="selectedVersionDiffInfo.tagType">
                {{ selectedVersionDiffInfo.tag }}
              </span>
              <span class="ca-main-title">版本变更说明与差异分析</span>
            </div>
            <span v-if="selectedVersionDiffInfo.prevVerNum" class="ca-compare-target">
              对比基准：v{{ selectedVersionDiffInfo.prevVerNum }}.0
            </span>
          </div>

          <div class="ca-body">
            <p class="ca-summary-p">{{ selectedVersionDiffInfo.summary }}</p>

            <div v-if="!selectedVersionDiffInfo.isBase" class="ca-diff-details-grid">
              <div
                class="diff-col"
                :class="{ modified: selectedVersionDiffInfo.sysDiff.includes('已调优') }"
              >
                <span class="col-lbl">System 设定：</span>
                <span class="col-val">{{ selectedVersionDiffInfo.sysDiff }}</span>
              </div>
              <div
                class="diff-col"
                :class="{ modified: selectedVersionDiffInfo.userDiff.includes('已调整') }"
              >
                <span class="col-lbl">User 模板：</span>
                <span class="col-val">{{ selectedVersionDiffInfo.userDiff }}</span>
              </div>
              <div
                class="diff-col"
                :class="{
                  modified:
                    selectedVersionDiffInfo.varsDiff.includes('新增') ||
                    selectedVersionDiffInfo.varsDiff.includes('移除')
                }"
              >
                <span class="col-lbl">参数插槽：</span>
                <span class="col-val">{{ selectedVersionDiffInfo.varsDiff }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="vd-section">
          <div class="vd-sec-header">
            <div class="sec-h-left">
              <span class="h-icon purple"><el-icon><Cpu /></el-icon></span>
              <span class="h-title">System Prompt（系统角色设定指令快照）</span>
            </div>
            <el-button
              v-if="selectedVersion.systemPrompt"
              link
              size="small"
              class="copy-pill-btn"
              @click="onCopy(selectedVersion.systemPrompt)"
            >
              <el-icon><CopyDocument /></el-icon>
              <span>复制系统指令</span>
            </el-button>
          </div>
          <div class="code-terminal-box">
            <pre v-if="selectedVersion.systemPrompt">{{ selectedVersion.systemPrompt }}</pre>
            <div v-else class="empty-code-hint">
              （此历史版本未指定独立 System Prompt，使用系统或大模型默认角色设定）
            </div>
          </div>
        </div>

        <div class="vd-section">
          <div class="vd-sec-header">
            <div class="sec-h-left">
              <span class="h-icon blue"><el-icon><ChatDotRound /></el-icon></span>
              <span class="h-title">User Prompt 模板（用户输入指令与插槽快照）</span>
            </div>
            <el-button
              link
              size="small"
              class="copy-pill-btn"
              @click="onCopy(selectedVersion.content)"
            >
              <el-icon><CopyDocument /></el-icon>
              <span>复制用户指令</span>
            </el-button>
          </div>
          <div class="code-terminal-box">
            <pre>{{ selectedVersion.content }}</pre>
          </div>
        </div>

        <div class="vd-section">
          <div class="vd-sec-header">
            <div class="sec-h-left">
              <span class="h-icon amber"><el-icon><CollectionTag /></el-icon></span>
              <span class="h-title">包含的动态插槽变量 ({{ selectedVersionVariables.length }})</span>
            </div>
          </div>
          <div class="variables-capsule-list">
            <span
              v-for="varName in selectedVersionVariables"
              :key="varName"
              class="var-pill"
            >
              <code>&#123;&#123; {{ varName }} &#125;&#125;</code>
            </span>
            <span v-if="!selectedVersionVariables.length" class="empty-var-hint">无独立动态参数</span>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="vd-dialog-footer">
          <div class="footer-left">
            <span class="hint-text">回滚将把当前在线生产运行的配置完全替换为此快照。</span>
          </div>
          <div class="footer-right">
            <el-button class="pill-btn cancel-btn" @click="versionDetailVisible = false">
              关闭
            </el-button>
            <el-button
              v-if="!isCurrentVersion(selectedVersion?.version)"
              type="warning"
              class="pill-btn rollback-btn-confirm"
              :loading="rollingBack"
              @click="onDetailRollback"
            >
              <el-icon><RefreshLeft /></el-icon>
              <span>回滚至此版本 (v{{ selectedVersion?.version }}.0)</span>
            </el-button>
            <span v-else class="vd-current-running-label">
              <el-icon><Check /></el-icon> 当前在线运行中
            </span>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {
  Clock,
  Timer,
  Flag,
  InfoFilled,
  EditPen,
  CollectionTag,
  Cpu,
  View,
  RefreshLeft,
  Check,
  CopyDocument,
  ChatDotRound
} from '@element-plus/icons-vue';
import type { PromptVersionItem } from '@/types/system/prompt';
import type { VersionDiffInfo } from '@/composables/system/usePromptEditor';
import { getVariablesList } from '@/composables/system/usePromptEditor';

defineProps<{
  promptId: number | null;
  templateName: string;
  templateCode: string;
  versionHistory: PromptVersionItem[];
  rollingBack: boolean;
  selectedVersion: PromptVersionItem | null;
  selectedVersionVariables: string[];
  selectedVersionDiffInfo: VersionDiffInfo;
  isCurrentVersion: (ver?: number) => boolean;
  getVersionDiffInfo: (item: PromptVersionItem) => VersionDiffInfo;
  formatVersionDate: (dateStr?: string) => string;
  onOpenDetail: (item: PromptVersionItem) => void;
  onRollback: (version: number) => void;
  onDetailRollback: () => void;
  onCopy: (text: string, msg?: string) => void;
}>();

const versionDetailVisible = defineModel<boolean>('versionDetailVisible', { required: true });
</script>

<style scoped lang="scss">
.section-card {
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 14px;
  padding: 18px 22px;
  margin-bottom: 18px;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.02);

  .section-card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 12px;
    margin-bottom: 16px;
    border-bottom: 1px solid #F1F5F9;

    .sch-left {
      display: flex;
      align-items: center;
      gap: 10px;

      .sec-icon {
        width: 32px;
        height: 32px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 16px;

        &.purple { background: #FAF5FF; color: #9333EA; }
      }

      .sec-title-wrap {
        display: flex;
        flex-direction: column;
        gap: 2px;

        .title-with-pill {
          display: flex;
          align-items: center;
          gap: 8px;

          .sec-title {
            font-size: 14.5px;
            font-weight: 700;
            color: #0F172A;
          }

          .ver-count-pill {
            font-size: 11px;
            font-weight: 600;
            color: #7C3AED;
            background: #F3E8FF;
            padding: 1px 8px;
            border-radius: 999px !important;
          }
        }

        .sec-sub {
          font-size: 11.5px;
          color: #64748B;
        }
      }
    }
  }
}

.version-card {
  .version-timeline-list {
    display: flex;
    flex-direction: column;
    gap: 10px;

    .v-card-item {
      background: linear-gradient(180deg, #FFFFFF 0%, #F8FAFC 100%);
      border: 1px solid #E2E8F0;
      border-radius: 12px;
      padding: 12px 14px;
      transition: border-color 0.2s ease, box-shadow 0.2s ease;

      &:hover {
        border-color: #CBD5E1;
        box-shadow: 0 4px 14px rgba(15, 23, 42, 0.05);
      }

      &.is-current {
        background: linear-gradient(180deg, #F0FDF4 0%, #FFFFFF 100%);
        border: 1px solid #BBF7D0;
        border-left: 3.5px solid #10B981;
      }

      .v-card-main {
        display: flex;
        flex-direction: column;
        gap: 10px;

        .v-top-bar {
          display: flex;
          justify-content: space-between;
          align-items: center;
          flex-wrap: wrap;
          gap: 8px;

          .v-pill-cluster {
            display: flex;
            align-items: center;
            gap: 6px;

            .v-tag-pill {
              font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
              font-size: 12px;
              font-weight: 700;
              color: #2563EB;
              background: #EFF6FF;
              border: 1px solid #BFDBFE;
              padding: 2px 9px;
              border-radius: 999px !important;

              &.active {
                color: #059669;
                background: #ECFDF5;
                border-color: #A7F3D0;
              }
            }

            .v-status-badge {
              font-size: 11px;
              font-weight: 600;
              padding: 2px 8px;
              border-radius: 999px !important;
              display: inline-flex;
              align-items: center;
              gap: 4px;

              &.current {
                color: #059669;
                background: #DCFCE7;
                border: 1px solid #86EFAC;

                .pulse-dot {
                  width: 6px;
                  height: 6px;
                  border-radius: 50%;
                  background: #10B981;
                }
              }

              &.archived {
                color: #64748B;
                background: #F1F5F9;
                border: 1px solid #E2E8F0;
              }
            }
          }

          .v-timestamp {
            display: flex;
            align-items: center;
            gap: 4px;
            font-size: 12px;
            color: #64748B;

            .time-ic {
              font-size: 13px;
              color: #94A3B8;
            }
          }
        }

        .v-change-summary-row {
          display: flex;
          align-items: center;
          gap: 8px;
          background: #F8FAFC;
          border-radius: 8px;
          padding: 6px 10px;
          border: 1px dashed #E2E8F0;

          .diff-tag-pill {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            font-size: 11px;
            font-weight: 600;
            padding: 1px 8px;
            border-radius: 999px !important;
            white-space: nowrap;

            &.base {
              background: #F1F5F9;
              color: #475569;
              border: 1px solid #CBD5E1;
            }

            &.identical {
              background: #F1F5F9;
              color: #64748B;
              border: 1px solid #E2E8F0;
            }

            &.updated {
              background: #EFF6FF;
              color: #2563EB;
              border: 1px solid #BFDBFE;
            }
          }

          .diff-desc-text {
            font-size: 11.5px;
            color: #475569;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }

        .v-bottom-bar {
          display: flex;
          justify-content: space-between;
          align-items: center;
          flex-wrap: wrap;
          gap: 8px;
          padding-top: 8px;
          border-top: 1px dashed #E2E8F0;

          .v-meta-tags {
            display: flex;
            align-items: center;
            gap: 6px;

            .v-meta-tag {
              display: inline-flex;
              align-items: center;
              gap: 4px;
              font-size: 11px;
              color: #475569;
              background: #F1F5F9;
              padding: 2px 8px;
              border-radius: 999px !important;
              border: 1px solid #E2E8F0;

              &.system {
                color: #7C3AED;
                background: #F5F3FF;
                border-color: #DDD6FE;
              }
            }
          }

          .v-action-group {
            display: flex;
            align-items: center;
            gap: 6px;

            .capsule-action-btn {
              border-radius: 999px !important;
              font-size: 12px;
              font-weight: 500;
              padding: 5px 12px;
              height: 28px;
              display: inline-flex;
              align-items: center;
              gap: 4px;

              &.detail-btn {
                background: #F8FAFC;
                color: #334155;
                border: 1px solid #CBD5E1;
              }

              &.rollback-btn {
                background: #FFFBEB;
                color: #D97706;
                border: 1px solid #FDE68A;
              }
            }

            .active-running-pill {
              display: inline-flex;
              align-items: center;
              gap: 4px;
              font-size: 11.5px;
              font-weight: 600;
              color: #059669;
              background: #ECFDF5;
              border: 1px solid #A7F3D0;
              padding: 3px 10px;
              border-radius: 999px !important;
            }
          }
        }
      }
    }
  }
}
</style>

<style lang="scss">
.version-detail-dialog {
  border-radius: 16px !important;
  overflow: hidden;

  .el-dialog__header {
    margin-right: 0;
    padding: 18px 24px 14px;
    border-bottom: 1px solid #F1F5F9;

    .el-dialog__title {
      font-size: 16px;
      font-weight: 800;
      color: #0F172A;
    }
  }

  .el-dialog__body {
    padding: 20px 24px;
  }

  .el-dialog__footer {
    padding: 14px 24px;
    border-top: 1px solid #F1F5F9;
    background: #FAFAFA;
  }

  .version-detail-content {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .vd-meta-strip {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 12px;
      background: #F8FAFC;
      border: 1px solid #E2E8F0;
      border-radius: 10px;
      padding: 12px 16px;

      .meta-block {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .m-label {
          font-size: 11.5px;
          color: #64748B;
          font-weight: 500;
        }

        .m-val {
          font-size: 13px;
          font-weight: 600;
          color: #0F172A;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;

          &.mono {
            font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
            color: #2563EB;
          }

          .vd-status-pill {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            font-size: 11px;
            font-weight: 600;
            padding: 2px 8px;
            border-radius: 999px !important;

            &.online {
              background: #ECFDF5;
              color: #059669;
              border: 1px solid #A7F3D0;

              .pulse-dot {
                width: 6px;
                height: 6px;
                border-radius: 50%;
                background: #10B981;
              }
            }

            &.archived {
              background: #F1F5F9;
              color: #64748B;
              border: 1px solid #E2E8F0;
            }
          }
        }
      }
    }

    .vd-change-analysis-card {
      border-radius: 12px;
      padding: 14px 16px;
      border: 1px solid #E2E8F0;

      &.updated {
        background: #EFF6FF;
        border-color: #BFDBFE;
      }

      .ca-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;

        .ca-title-group {
          display: flex;
          align-items: center;
          gap: 8px;

          .ca-pill {
            font-size: 11px;
            font-weight: 700;
            padding: 2px 9px;
            border-radius: 999px !important;

            &.updated {
              background: #DBEAFE;
              color: #1D4ED8;
            }
          }

          .ca-main-title {
            font-size: 13px;
            font-weight: 700;
            color: #0F172A;
          }
        }

        .ca-compare-target {
          font-size: 11.5px;
          color: #64748B;
          font-family: ui-monospace, monospace;
        }
      }

      .ca-body {
        .ca-summary-p {
          margin: 0 0 8px 0;
          font-size: 12.5px;
          color: #334155;
          line-height: 1.5;
        }

        .ca-diff-details-grid {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: 8px;

          .diff-col {
            background: #FFFFFF;
            border: 1px solid #E2E8F0;
            border-radius: 8px;
            padding: 8px 10px;
            display: flex;
            flex-direction: column;
            gap: 3px;

            &.modified {
              border-color: #93C5FD;
              background: #F0F7FF;

              .col-val {
                color: #2563EB;
                font-weight: 600;
              }
            }

            .col-lbl {
              font-size: 11px;
              color: #64748B;
            }

            .col-val {
              font-size: 12px;
              color: #1E293B;
            }
          }
        }
      }
    }

    .vd-section {
      display: flex;
      flex-direction: column;
      gap: 8px;

      .vd-sec-header {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .sec-h-left {
          display: flex;
          align-items: center;
          gap: 6px;

          .h-icon {
            width: 22px;
            height: 22px;
            border-radius: 6px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 12px;

            &.purple { background: #F5F3FF; color: #7C3AED; }
            &.blue { background: #EFF6FF; color: #2563EB; }
            &.amber { background: #FFFBEB; color: #D97706; }
          }

          .h-title {
            font-size: 13px;
            font-weight: 700;
            color: #1E293B;
          }
        }

        .copy-pill-btn {
          font-size: 12px;
          color: #2563EB;
          display: inline-flex;
          align-items: center;
          gap: 4px;
        }
      }

      .code-terminal-box {
        background: #0F172A;
        border-radius: 10px;
        padding: 14px 16px;
        border: 1px solid #1E293B;

        pre {
          margin: 0;
          font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
          font-size: 12.5px;
          color: #E2E8F0;
          line-height: 1.6;
          white-space: pre-wrap;
          word-break: break-word;
          max-height: 180px;
          overflow-y: auto;
        }

        .empty-code-hint {
          color: #64748B;
          font-size: 12px;
          font-style: italic;
        }
      }

      .variables-capsule-list {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        padding: 10px 14px;
        background: #F8FAFC;
        border: 1px solid #E2E8F0;
        border-radius: 8px;

        .var-pill {
          display: inline-flex;
          align-items: center;
          background: #EFF6FF;
          border: 1px solid #BFDBFE;
          padding: 3px 10px;
          border-radius: 999px !important;

          code {
            font-family: ui-monospace, monospace;
            font-size: 12px;
            color: #1D4ED8;
            font-weight: 600;
          }
        }

        .empty-var-hint {
          font-size: 12px;
          color: #94A3B8;
        }
      }
    }
  }

  .vd-dialog-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;
    gap: 12px;

    .footer-left .hint-text {
      font-size: 12px;
      color: #64748B;
    }

    .footer-right {
      display: flex;
      align-items: center;
      gap: 10px;

      .pill-btn {
        border-radius: 999px !important;
        font-weight: 600;
        padding: 8px 18px;

        &.rollback-btn-confirm {
          background: #F59E0B;
          border: none;
          color: #FFFFFF;
        }
      }

      .vd-current-running-label {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        font-size: 12.5px;
        font-weight: 600;
        color: #059669;
        background: #ECFDF5;
        border: 1px solid #A7F3D0;
        padding: 6px 14px;
        border-radius: 999px !important;
      }
    }
  }
}
</style>
