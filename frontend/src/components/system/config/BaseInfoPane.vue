<template>
  <div class="base-info-pane">
    <div class="config-card max-width-card">
      <div class="card-header-bar">
        <div class="header-title-group">
          <el-icon class="card-icon"><Monitor /></el-icon>
          <h2 class="card-title">智教云站点与品牌展示信息</h2>
        </div>
      </div>

      <el-form label-position="top" class="config-form" autocomplete="off">
        <el-form-item label="平台中文标题">
          <input
            v-model="baseInfo.platformName"
            type="text"
            name="platform_title_name"
            autocomplete="off"
            class="capsule-input"
            placeholder="例如：智教云 · EduMind"
          />
        </el-form-item>

        <el-form-item label="平台副标题 / 定位">
          <input
            v-model="baseInfo.subTitle"
            type="text"
            name="platform_sub_title"
            autocomplete="off"
            class="capsule-input"
            placeholder="例如：AI 智能教学赋能平台"
          />
        </el-form-item>

        <el-form-item label="网站备案号">
          <input
            v-model="baseInfo.icp"
            type="text"
            name="platform_icp_code"
            autocomplete="off"
            class="capsule-input"
            placeholder="例如：京ICP备20260001号-1"
          />
        </el-form-item>

        <el-form-item label="底部版权声明">
          <input
            v-model="baseInfo.copyright"
            type="text"
            name="platform_copyright_text"
            autocomplete="off"
            class="capsule-input"
            placeholder="例如：© 2026 EduMind. All rights reserved."
          />
        </el-form-item>

        <button type="button" class="btn-save-main" @click="handleSaveBaseInfo">
          <el-icon class="btn-icon"><Check /></el-icon>
          <span>保存基础信息</span>
        </button>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Monitor, Check } from '@element-plus/icons-vue';
import { fetchBaseInfo } from '@/composables/system/useSystemConfig';

const baseInfo = reactive({
  platformName: '智教云 · EduMind',
  subTitle: 'AI 智能教学赋能平台',
  copyright: '© 2026 EduMind. All rights reserved.',
  icp: '京ICP备20260001号-1'
});

async function loadBaseInfo() {
  try {
    const res = await fetchBaseInfo();
    if (res?.data) {
      Object.assign(baseInfo, res.data);
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '获取基础信息失败');
  }
}

function handleSaveBaseInfo() {
  ElMessage.success('平台基础展示信息已保存');
}

defineExpose({
  loadBaseInfo
});

onMounted(() => {
  loadBaseInfo();
});
</script>

<style scoped lang="scss">
.base-info-pane {
  .config-card {
    background: #FFFFFF;
    border-radius: 18px;
    border: 1px solid #E2E8F0;
    padding: 24px 28px;
    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);

    &.max-width-card {
      max-width: 680px;
    }

    .card-header-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 22px;
      padding-bottom: 14px;
      border-bottom: 1px solid #F1F5F9;

      .header-title-group {
        display: flex;
        align-items: center;
        gap: 10px;

        .card-icon {
          font-size: 18px;
          color: #1677FF;
        }

        .card-title {
          margin: 0;
          font-size: 16px;
          font-weight: 700;
          color: #0F172A;
        }
      }
    }

    .capsule-input {
      width: 100%;
      height: 42px;
      border-radius: 9999px;
      border: 1px solid #E2E8F0;
      background: #FFFFFF;
      padding: 0 18px;
      font-size: 13.5px;
      color: #1E293B;
      outline: none;
      box-sizing: border-box;
      transition: all 0.2s;

      &:-webkit-autofill,
      &:-webkit-autofill:hover,
      &:-webkit-autofill:focus,
      &:-webkit-autofill:active {
        -webkit-box-shadow: 0 0 0 1000px #FFFFFF inset !important;
        -webkit-text-fill-color: #1E293B !important;
        transition: background-color 50000s ease-in-out 0s;
      }

      &:focus {
        border-color: #1677FF;
        box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.16);
      }
    }

    .btn-save-main {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 9px 24px;
      margin-top: 10px;
      border-radius: 9999px;
      background: #1677FF;
      border: none;
      color: #FFFFFF;
      font-size: 13.5px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;
      box-shadow: 0 4px 12px rgba(22, 119, 255, 0.25);

      &:hover {
        background: #0958D9;
        transform: translateY(-1px);
      }
    }
  }
}
</style>
