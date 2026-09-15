<template>
  <el-dialog
    v-model="visible"
    title="确认执行国密 KMS 密钥版本递增轮换"
    width="520px"
    destroy-on-close
  >
    <div class="rotate-dialog-body">
      <div class="warning-hero-card">
        <el-icon class="warning-icon"><WarningFilled /></el-icon>
        <div class="warning-text">
          <p class="title">密钥轮换将自动淘汰老版本并派生全新版本</p>
          <p class="desc">
            系统将把当前活跃版本标记为 <code>DEPRECATED</code>，并在租户沙箱中递增生成全新 <code>ACTIVE</code> 密钥。
            历史业务数据仍可按历史版本号平滑解密，新写入数据将即刻采用全新密钥加密。
          </p>
        </div>
      </div>

      <el-form label-position="top" class="mt-3">
        <el-form-item label="待轮换的目标密钥资产别名">
          <el-select v-model="selectedRotateAlias" style="width: 100%;">
            <el-option label="edumind-data-key (教学沙箱数据主密钥)" value="edumind-data-key" />
            <el-option label="edumind-model-key (AI模型中枢凭证密钥)" value="edumind-model-key" />
          </el-select>
        </el-form-item>
        <div class="version-preview-row">
          <span class="vp-label">版本演进预估：</span>
          <span class="font-mono text-muted">v{{ getActiveVerByAlias(selectedRotateAlias) }}</span>
          <el-icon class="vp-arrow"><ArrowRight v-if="false" /></el-icon>
          <span class="vp-symbol">➔</span>
          <span class="font-mono text-success font-bold">v{{ getActiveVerByAlias(selectedRotateAlias) + 1 }} (全新 ACTIVE)</span>
        </div>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="rotating" class="gradient-btn" @click="executeRotate">
        确认立即轮换
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { WarningFilled, ArrowRight } from '@element-plus/icons-vue';

const visible = defineModel<boolean>('visible', { default: false });
const selectedRotateAlias = defineModel<string>('selectedRotateAlias', { default: 'edumind-data-key' });

defineProps<{
  rotating: boolean;
  getActiveVerByAlias: (alias?: string) => number;
  executeRotate: () => void | Promise<void>;
}>();
</script>
