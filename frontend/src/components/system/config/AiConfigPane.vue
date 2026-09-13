<template>
  <div class="ai-config-tab">
    <el-form :model="draft" label-width="120px" class="config-form" autocomplete="off" @submit.prevent>
      <el-form-item label="AI 助手">
        <div class="switch-with-tip">
          <el-switch v-model="draft.assistantEnabled" :disabled="!canEdit" />
          <span class="form-item-tip">关闭后，PC 端与移动端的悬浮 AI 助手小窗将不再显示</span>
        </div>
      </el-form-item>

      <el-form-item label="回答边界">
        <el-radio-group v-model="draft.answerScope" :disabled="!canEdit">
          <el-radio label="focus">聚焦本系统</el-radio>
          <el-radio label="open">开放问答</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>

    <div class="token-quota-card">
      <div class="card-header">
        <div class="title-group">
          <el-icon class="title-icon"><Coin /></el-icon>
          <span class="title-text">Token 每日配额设置</span>
        </div>
        <span class="subtitle-text">限制每位用户或不同角色每天可消耗的最大 AI Token 数量</span>
      </div>

      <div class="card-body">
        <!-- 默认每用户每日配额 -->
        <div class="quota-item default-quota-item">
          <div class="item-info">
            <div class="item-title">每用户默认每日配额</div>
            <div class="item-desc">未匹配单独角色配额规则的用户，将统一按此配额限制</div>
          </div>
          <div class="item-control">
            <el-input-number
              v-model="draft.tokensPerUserDaily"
              :min="0"
              :max="10000000"
              :step="10000"
              controls-position="right"
              :disabled="!canEdit"
              class="quota-input-number"
            />
            <span class="unit-text">Tokens / 天</span>
          </div>
        </div>

        <el-divider class="quota-divider" />

        <!-- 角色配额 -->
        <div class="quota-item role-quota-item">
          <div class="role-header-row">
            <div class="item-info">
              <div class="item-title">角色差异化配额</div>
              <div class="item-desc">可为特定角色单独指定 Token 配额（如管理员、高级用户等）</div>
            </div>
            <el-button
              type="primary"
              plain
              size="small"
              :icon="Plus"
              :disabled="!canEdit || (draft.roleTokenQuotas && draft.roleTokenQuotas.length >= 50)"
              @click="addRow"
            >
              添加角色配额
            </el-button>
          </div>

          <div class="role-table-container">
            <el-table
              :data="draft.roleTokenQuotas"
              size="default"
              stripe
              border
              class="role-quota-table"
            >
              <el-table-column label="目标角色" min-width="220">
                <template #default="{ row }">
                  <el-select
                    v-model="row.roleId"
                    :disabled="!canEdit"
                    placeholder="请选择角色"
                    style="width: 100%"
                    @change="() => onRoleSelectChange(row)"
                  >
                    <el-option
                      v-for="role in roleOptions"
                      :key="role.id"
                      :label="`${role.name}（${role.code}）`"
                      :value="role.id"
                      :disabled="isRoleUsed(role.id, row.roleId)"
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="每日 Token 配额" min-width="200">
                <template #default="{ row }">
                  <el-input-number
                    v-model="row.tokensDaily"
                    :min="0"
                    :max="10000000"
                    :step="10000"
                    controls-position="right"
                    :disabled="!canEdit"
                    style="width: 100%"
                    @change="() => onTokensChange(row)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="生效规则说明" width="130" align="center">
                <template #default="{ row }">
                  <el-tag v-if="!row.tokensDaily || row.tokensDaily === 0" type="success" size="small" effect="light">无限制</el-tag>
                  <el-tag v-else type="info" size="small" effect="plain">{{ row.tokensDaily?.toLocaleString() }} Tokens</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="90" align="center">
                <template #default="{ $index }">
                  <el-button
                    type="danger"
                    plain
                    size="small"
                    :disabled="!canEdit"
                    @click="removeRow($index)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
              <template #empty>
                <div class="empty-holder">
                  <el-empty description="暂未配置角色差异化配额，全员使用默认配额" :image-size="48" />
                </div>
              </template>
            </el-table>
          </div>

          <div class="quota-rules-hint">
            <el-icon class="hint-icon"><InfoFilled /></el-icon>
            <span>多角色用户自动按最高配额生效；配额设为 0 表示该角色不限制使用 Token。</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Coin, Plus, InfoFilled } from '@element-plus/icons-vue';
import { ElMessageBox } from 'element-plus';
import type { AiConfig, RoleOption } from '@/types/system/config';

const props = withDefaults(
  defineProps<{
    draft: AiConfig;
    roleOptions: RoleOption[];
    canEdit?: boolean;
  }>(),
  {
    canEdit: true
  }
);

function onRoleSelectChange(row: any) {
  const match = props.roleOptions.find((r) => r.id === row.roleId);
  if (match) {
    row.roleCode = match.code;
  }
}

function onTokensChange(row: any) {
  row.maxTokensDaily = row.tokensDaily;
}

/** 已被其它行选用的角色置灰（当前行已选值除外），避免重复配置同一角色 */
function isRoleUsed(roleId: number, currentRoleId?: number): boolean {
  if (roleId === currentRoleId) return false;
  if (!props.draft.roleTokenQuotas) return false;
  return props.draft.roleTokenQuotas.some((q) => q.roleId === roleId);
}

function addRow() {
  if (!props.draft.roleTokenQuotas) {
    props.draft.roleTokenQuotas = [];
  }
  props.draft.roleTokenQuotas.push({
    roleId: undefined,
    roleCode: '',
    tokensDaily: 200000,
    maxTokensDaily: 200000
  });
}

async function removeRow(index: number) {
  try {
    await ElMessageBox.confirm('确定要删除该角色的 Token 配额规则吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    props.draft.roleTokenQuotas.splice(index, 1);
  } catch {
    // 用户取消
  }
}
</script>

<style scoped>
.ai-config-tab {
  max-width: 760px;
  padding-top: 8px;
}

.config-form {
  margin-bottom: 20px;
}

.switch-with-tip {
  display: flex;
  align-items: center;
  gap: 12px;
}

.form-item-tip {
  font-size: 13px;
  color: #909399;
  white-space: nowrap;
}

.token-quota-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background-color: #ffffff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.card-header {
  padding: 16px 20px;
  background-color: #f8f9fa;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.title-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  font-size: 18px;
  color: #1677ff;
}

.title-text {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.subtitle-text {
  font-size: 12px;
  color: #909399;
}

.card-body {
  padding: 20px;
}

.quota-item {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.default-quota-item {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
}

.item-info {
  flex: 1;
}

.item-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.item-desc {
  font-size: 12px;
  color: #909399;
}

.item-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quota-input-number {
  width: 220px;
}

.unit-text {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}

.quota-divider {
  margin: 20px 0;
}

.role-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.role-table-container {
  margin-bottom: 12px;
  border-radius: 6px;
  overflow: hidden;
}

.role-quota-table {
  width: 100%;
}

.quota-rules-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
  background-color: #fafafa;
  padding: 8px 12px;
  border-radius: 4px;
  border: 1px solid #f2f3f5;
}

.hint-icon {
  font-size: 14px;
  color: #909399;
}

.empty-holder {
  padding: 16px 0;
}
</style>
