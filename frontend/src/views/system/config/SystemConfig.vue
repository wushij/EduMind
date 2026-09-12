<template>
  <div class="system-config-page">
    <!-- é¡¶é¨æ é¢å¡ç -->
    <div class="config-header-card">
      <div class="header-left">
        <div class="header-icon-box">
          <el-icon :size="24" class="icon-cog"><Setting /></el-icon>
        </div>
        <div class="header-text-block">
          <h1 class="page-title">å¹³å°å¨å±è¿è¡ä¸æå¡åæ°éç½®</h1>
          <p class="page-subtitle">
            éä¸­ç®¡çæºæäºÂ·EduMind å¤å¼ææä»¶å­å¨ä¸­æ¢ãå¨é¾è·¯å½å¯ä¸é²çç ´å®å¨ç­ç¥ãSMTP é®ä»¶åä¿¡åç«ç¹åºç¡åæ°
          </p>
        </div>
      </div>
      <div class="header-right">
        <button type="button" class="action-refresh-btn" @click="handleRefreshAll">
          <el-icon><Refresh /></el-icon>
          <span>éæ°å è½½</span>
        </button>
      </div>
    </div>

    <!-- éé¡¹å¡åæ¢å¯¼èª -->
    <div class="config-tabs-nav">
      <div
        class="tab-item"
        :class="{ active: currentTab === 'storage' }"
        @click="currentTab = 'storage'"
      >
        <el-icon><Coin /></el-icon>
        <span>æä»¶å­å¨ä¸­æ¢ (Storage)</span>
      </div>
      <div
        class="tab-item"
        :class="{ active: currentTab === 'security' }"
        @click="currentTab = 'security'"
      >
        <el-icon><Lock /></el-icon>
        <span>å®å¨ä¸é²çç ´ç­ç¥ (Security)</span>
      </div>
      <div
        class="tab-item"
        :class="{ active: currentTab === 'mail' }"
        @click="currentTab = 'mail'"
      >
        <el-icon><Message /></el-icon>
        <span>é®ä»¶åä¿¡æå¡ (SMTP)</span>
      </div>
      <div
        class="tab-item"
        :class="{ active: currentTab === 'base' }"
        @click="currentTab = 'base'"
      >
        <el-icon><Monitor /></el-icon>
        <span>å¹³å°åºç¡ä¿¡æ¯</span>
      </div>
    </div>

    <!-- éé¡¹å¡åå®¹é¢æ¿ -->
    <div class="config-content-body">
      <StorageConfigPane v-if="currentTab === 'storage'" ref="storagePaneRef" />
      <SecurityConfigPane v-if="currentTab === 'security'" ref="securityPaneRef" />
      <MailConfigPane v-if="currentTab === 'mail'" ref="mailPaneRef" />
      <BaseInfoPane v-if="currentTab === 'base'" ref="basePaneRef" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { Setting, Refresh, Coin, Lock, Message, Monitor } from '@element-plus/icons-vue';
import StorageConfigPane from '@/components/system/config/StorageConfigPane.vue';
import SecurityConfigPane from '@/components/system/config/SecurityConfigPane.vue';
import MailConfigPane from '@/components/system/config/MailConfigPane.vue';
import BaseInfoPane from '@/components/system/config/BaseInfoPane.vue';

const currentTab = ref<'storage' | 'security' | 'mail' | 'base'>('storage');

const storagePaneRef = ref<InstanceType<typeof StorageConfigPane> | null>(null);
const securityPaneRef = ref<InstanceType<typeof SecurityConfigPane> | null>(null);
const mailPaneRef = ref<InstanceType<typeof MailConfigPane> | null>(null);
const basePaneRef = ref<InstanceType<typeof BaseInfoPane> | null>(null);

function handleRefreshAll() {
  storagePaneRef.value?.loadStorageConfig();
  securityPaneRef.value?.loadSecurityConfig();
  mailPaneRef.value?.loadMailConfig();
  basePaneRef.value?.loadBaseInfo();
}
</script>

<style scoped lang="scss">
.system-config-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;

  // 1. é¡¶é¨å¡ç
  .config-header-card {
    background: #FFFFFF;
    border-radius: 18px;
    padding: 24px 28px;
    border: 1px solid #E2E8F0;
    box-shadow: 0 4px 18px rgba(30, 80, 150, 0.04);
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    flex-wrap: wrap;

    .header-left {
      display: flex;
      align-items: center;
      gap: 16px;

      .header-icon-box {
        width: 52px;
        height: 52px;
        border-radius: 14px;
        background: #EFF6FF;
        color: #1677FF;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .header-text-block {
        .page-title {
          margin: 0 0 4px;
          font-size: 20px;
          font-weight: 700;
          color: #0F172A;
        }

        .page-subtitle {
          margin: 0;
          font-size: 13px;
          color: #64748B;
        }
      }
    }

    .header-right {
      .action-refresh-btn {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 8px 18px;
        border-radius: 9999px;
        background: #F8FAFC;
        border: 1px solid #CBD5E1;
        color: #475569;
        font-size: 13px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          background: #FFFFFF;
          border-color: #1677FF;
          color: #1677FF;
        }
      }
    }
  }

  // 2. éé¡¹å¡åæ¢å¯¼èª
  .config-tabs-nav {
    display: flex;
    align-items: center;
    gap: 10px;
    background: #FFFFFF;
    padding: 6px;
    border-radius: 14px;
    border: 1px solid #E2E8F0;
    width: fit-content;

    .tab-item {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 9px 20px;
      border-radius: 10px;
      font-size: 13.5px;
      font-weight: 600;
      color: #64748B;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        color: #1677FF;
        background: #F8FAFC;
      }

      &.active {
        background: #1677FF;
        color: #FFFFFF;
        box-shadow: 0 2px 8px rgba(22, 119, 255, 0.25);
      }
    }
  }

  .config-content-body {
    width: 100%;
  }
}
</style>
