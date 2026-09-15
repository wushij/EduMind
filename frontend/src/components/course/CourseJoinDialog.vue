<template>
  <!-- 加入课程长圆现代弹窗 -->
  <el-dialog
    :model-value="visible"
    width="600px"
    append-to-body
    destroy-on-close
    class="capsule-course-dialog"
    :show-close="true"
    @update:model-value="emit('update:visible', $event)"
  >
    <template #header>
      <div class="dialog-custom-header">
        <div class="header-icon-badge">
          <svg viewBox="0 0 24 24" class="header-svg" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"></path>
          </svg>
        </div>
        <div class="header-text-group">
          <h3 class="header-main-title">加入课程修读空间</h3>
          <p class="header-sub-desc">输入课程专属代号或速选全校公开课，一键入驻智能学习空间</p>
        </div>
      </div>
    </template>

    <div class="join-dialog-body">
      <!-- 药丸 Segmented Tab 切换器 -->
      <div class="dialog-pill-tabs">
        <button
          type="button"
          class="dialog-pill-tab"
          :class="{ active: joinTab === 'code' }"
          @click="emit('update:joinTab', 'code')"
        >
          <svg viewBox="0 0 24 24" class="tab-svg" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
            <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
          </svg>
          <span>课程代号 / 邀请码选课</span>
        </button>
        <button
          type="button"
          class="dialog-pill-tab"
          :class="{ active: joinTab === 'browse' }"
          @click="emit('update:joinTab', 'browse')"
        >
          <svg viewBox="0 0 24 24" class="tab-svg" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="2" y1="12" x2="22" y2="12"></line>
            <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"></path>
          </svg>
          <span>全校公开课程速选</span>
        </button>
      </div>

      <!-- 模式一：输入课程代号 -->
      <div v-if="joinTab === 'code'" class="tab-pane-code">
        <div class="guide-banner-card">
          <div class="guide-icon">
            <svg viewBox="0 0 24 24" class="info-svg" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="16" x2="12" y2="12"></line>
              <line x1="12" y1="8" x2="12.01" y2="8"></line>
            </svg>
          </div>
          <div class="guide-text">
            请输入教师公布的 <strong>6 位课程代号</strong> 或邀请码（例如 <code>CS201</code>、<code>CS101</code>、<code>MATH101</code>）
          </div>
        </div>

        <!-- 高阶质感大胶囊输入框 -->
        <div class="dialog-capsule-input-box">
          <div class="input-prefix">
            <svg viewBox="0 0 24 24" class="prefix-svg" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="4 17 10 11 4 5"></polyline>
              <line x1="12" y1="19" x2="20" y2="19"></line>
            </svg>
          </div>
          <input
            :value="courseCodeInput"
            type="text"
            class="native-code-input"
            placeholder="输入课程代号，如 CS201..."
            maxlength="20"
            @input="emit('update:courseCodeInput', ($event.target as HTMLInputElement).value)"
            @keyup.enter="emit('join-by-code')"
          />
          <button
            v-if="courseCodeInput"
            type="button"
            class="clear-input-btn"
            title="清空"
            @click="emit('update:courseCodeInput', '')"
          >
            <svg viewBox="0 0 24 24" class="clear-svg" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
          <button
            type="button"
            class="capsule-submit-btn"
            :class="{ 'is-loading': joining }"
            :disabled="!courseCodeInput.trim() || joining"
            @click="emit('join-by-code')"
          >
            <span v-if="joining" class="btn-spinner"></span>
            <span>{{ joining ? '正在加入...' : '立即选课加入' }}</span>
            <svg v-if="!joining" viewBox="0 0 24 24" class="btn-arrow-svg" fill="none" stroke="currentColor" stroke-width="2.5">
              <line x1="5" y1="12" x2="19" y2="12"></line>
              <polyline points="12 5 19 12 12 19"></polyline>
            </svg>
          </button>
        </div>

        <!-- 热门示范课程芯片快速填入 -->
        <div class="quick-code-section">
          <div class="section-label-row">
            <span class="hint-label">热门课程快速填入：</span>
            <span class="hint-sub">点击芯片自动填入代号</span>
          </div>
          <div class="quick-chips-grid">
            <button
              type="button"
              class="quick-chip-item"
              :class="{ active: courseCodeInput === 'CS201' }"
              @click="emit('update:courseCodeInput', 'CS201')"
            >
              <span class="chip-code">CS201</span>
              <span class="chip-name">数据结构与算法</span>
            </button>
            <button
              type="button"
              class="quick-chip-item"
              :class="{ active: courseCodeInput === 'CS101' }"
              @click="emit('update:courseCodeInput', 'CS101')"
            >
              <span class="chip-code">CS101</span>
              <span class="chip-name">Java程序设计</span>
            </button>
            <button
              type="button"
              class="quick-chip-item"
              :class="{ active: courseCodeInput === 'MATH101' }"
              @click="emit('update:courseCodeInput', 'MATH101')"
            >
              <span class="chip-code">MATH101</span>
              <span class="chip-name">高等数学</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 模式二：全校公开课程速选 -->
      <div v-else class="tab-pane-browse">
        <div class="browse-header-row">
          <span class="browse-title">全校精选示范公开课</span>
          <span class="browse-badge">无需邀请码一键加入</span>
        </div>

        <div class="public-course-list">
          <div
            v-for="item in publicCoursesList"
            :key="item.code"
            class="public-course-card"
          >
            <div class="course-avatar-box">
              <span class="avatar-letter">{{ item.title.slice(0, 1) }}</span>
            </div>
            <div class="course-main-meta">
              <div class="title-code-row">
                <span class="course-name">{{ item.title }}</span>
                <span class="course-code-tag">{{ item.code }}</span>
              </div>
              <div class="extra-meta-row">
                <span class="teacher-text">教师：{{ item.teacherName }}</span>
                <span class="dot-sep">·</span>
                <span class="semester-text">{{ item.semester || '2026秋季' }}</span>
                <span class="dot-sep">·</span>
                <span class="student-text">{{ item.studentCount || 10 }} 人在学</span>
              </div>
            </div>
            <button
              type="button"
              class="quick-enroll-btn"
              :disabled="joining || !item.code"
              @click="item.code && emit('quick-join', item.code)"
            >
              <span>一键加入</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import type { PublicCourseItem } from '@/composables/course/useCourseList';

defineProps<{
  visible: boolean;
  joinTab: 'code' | 'browse';
  courseCodeInput: string;
  joining: boolean;
  publicCoursesList: PublicCourseItem[];
}>();

const emit = defineEmits<{
  'update:visible': [value: boolean];
  'update:joinTab': [value: 'code' | 'browse'];
  'update:courseCodeInput': [value: string];
  'join-by-code': [];
  'quick-join': [code: string];
}>();
</script>

<style scoped lang="scss">
// 3. 加入课程长圆现代弹窗内部样式
.join-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 4px 6px 12px;

  // 胶囊 Segmented Tab 切换栏
  .dialog-pill-tabs {
    display: flex;
    background: #F1F5F9;
    padding: 5px;
    border-radius: 9999px;
    gap: 6px;
    border: 1px solid #E2E8F0;

    .dialog-pill-tab {
      flex: 1;
      height: 38px;
      border-radius: 9999px;
      border: none;
      background: transparent;
      font-size: 13.5px;
      font-weight: 500;
      color: #64748B;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 7px;
      transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);

      .tab-svg {
        width: 15px;
        height: 15px;
        stroke-width: 2;
        transition: color 0.2s;
      }

      &:hover:not(.active) {
        color: #1E293B;
      }

      &.active {
        background: #FFFFFF;
        color: #1677FF;
        font-weight: 600;
        box-shadow: 0 3px 10px rgba(15, 23, 42, 0.08);
      }
    }
  }

  // 模式一：代号输入区域
  .tab-pane-code {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .guide-banner-card {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      padding: 12px 16px;
      border-radius: 14px;
      background: #F0F7FF;
      border: 1px solid #BAE0FF;

      .guide-icon {
        color: #1677FF;
        flex-shrink: 0;
        margin-top: 1px;

        .info-svg {
          width: 16px;
          height: 16px;
        }
      }

      .guide-text {
        font-size: 13px;
        color: #1E3A8A;
        line-height: 1.6;

        strong {
          color: #0958D9;
          font-weight: 600;
        }

        code {
          background: rgba(22, 119, 255, 0.12);
          color: #1677FF;
          padding: 2px 6px;
          border-radius: 6px;
          font-family: inherit;
          font-weight: 600;
          font-size: 12.5px;
          margin: 0 2px;
        }
      }
    }

    // 高阶质感大胶囊输入框
    .dialog-capsule-input-box {
      display: flex;
      align-items: center;
      position: relative;
      background: #FFFFFF;
      border: 2px solid #E2E8F0;
      border-radius: 9999px;
      padding: 5px 6px 5px 16px;
      box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
      transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

      &:focus-within {
        border-color: #1677FF;
        box-shadow: 0 0 0 4px rgba(22, 119, 255, 0.12), 0 4px 14px rgba(22, 119, 255, 0.08);
      }

      .input-prefix {
        display: flex;
        align-items: center;
        color: #94A3B8;
        margin-right: 10px;

        .prefix-svg {
          width: 18px;
          height: 18px;
        }
      }

      .native-code-input {
        flex: 1;
        border: none;
        outline: none;
        background: transparent;
        font-size: 15px;
        font-weight: 500;
        color: #0F172A;
        letter-spacing: 0.5px;

        &::placeholder {
          color: #94A3B8;
          font-size: 13.5px;
        }
      }

      .clear-input-btn {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 24px;
        height: 24px;
        border-radius: 50%;
        background: #F1F5F9;
        border: none;
        color: #64748B;
        cursor: pointer;
        margin-right: 8px;
        transition: all 0.2s;

        .clear-svg {
          width: 14px;
          height: 14px;
        }

        &:hover {
          background: #E2E8F0;
          color: #0F172A;
        }
      }

      .capsule-submit-btn {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        height: 42px;
        padding: 0 22px;
        border-radius: 9999px;
        border: none;
        background: linear-gradient(135deg, #1677FF 0%, #3B82F6 100%);
        color: #FFFFFF;
        font-size: 14px;
        font-weight: 600;
        cursor: pointer;
        box-shadow: 0 3px 12px rgba(22, 119, 255, 0.3);
        transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
        white-space: nowrap;

        .btn-arrow-svg {
          width: 15px;
          height: 15px;
          transition: transform 0.2s;
        }

        .btn-spinner {
          width: 14px;
          height: 14px;
          border: 2px solid rgba(255, 255, 255, 0.3);
          border-top-color: #FFFFFF;
          border-radius: 50%;
          animation: spin 0.8s linear infinite;
        }

        &:hover:not(:disabled) {
          background: linear-gradient(135deg, #4096FF 0%, #2563EB 100%);
          transform: translateY(-1px);
          box-shadow: 0 6px 16px rgba(22, 119, 255, 0.4);

          .btn-arrow-svg {
            transform: translateX(2px);
          }
        }

        &:active:not(:disabled) {
          transform: translateY(0);
        }

        &:disabled {
          opacity: 0.55;
          cursor: not-allowed;
          box-shadow: none;
        }
      }
    }

    // 快捷芯片卡片
    .quick-code-section {
      display: flex;
      flex-direction: column;
      gap: 8px;
      margin-top: 4px;

      .section-label-row {
        display: flex;
        align-items: center;
        justify-content: space-between;

        .hint-label {
          font-size: 12.5px;
          font-weight: 600;
          color: #475569;
        }

        .hint-sub {
          font-size: 11.5px;
          color: #94A3B8;
        }
      }

      .quick-chips-grid {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 10px;

        .quick-chip-item {
          display: flex;
          flex-direction: column;
          align-items: flex-start;
          gap: 2px;
          padding: 10px 14px;
          border-radius: 12px;
          background: #F8FAFC;
          border: 1.5px solid #E2E8F0;
          cursor: pointer;
          transition: all 0.2s ease;
          text-align: left;

          .chip-code {
            font-size: 12px;
            font-weight: 700;
            color: #1677FF;
            letter-spacing: 0.4px;
          }

          .chip-name {
            font-size: 12px;
            font-weight: 500;
            color: #334155;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            width: 100%;
          }

          &:hover {
            border-color: #93C5FD;
            background: #EFF6FF;
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(22, 119, 255, 0.08);
          }

          &.active {
            border-color: #1677FF;
            background: #EAF3FF;
            box-shadow: 0 2px 8px rgba(22, 119, 255, 0.15);
          }
        }
      }
    }
  }

  // 模式二：全校公开课列表速选
  .tab-pane-browse {
    display: flex;
    flex-direction: column;
    gap: 14px;

    .browse-header-row {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .browse-title {
        font-size: 13.5px;
        font-weight: 600;
        color: #1E293B;
      }

      .browse-badge {
        font-size: 11px;
        font-weight: 600;
        padding: 3px 8px;
        border-radius: 9999px;
        background: #DCFCE7;
        color: #15803D;
      }
    }

    .public-course-list {
      display: flex;
      flex-direction: column;
      gap: 10px;
      max-height: 280px;
      overflow-y: auto;
      padding-right: 4px;

      .public-course-card {
        display: flex;
        align-items: center;
        gap: 14px;
        padding: 12px 16px;
        border-radius: 14px;
        background: #F8FAFC;
        border: 1px solid #E2E8F0;
        transition: all 0.22s ease;

        &:hover {
          border-color: #BAE0FF;
          background: #F0F7FF;
          transform: translateY(-1px);
          box-shadow: 0 4px 14px rgba(22, 119, 255, 0.06);
        }

        .course-avatar-box {
          width: 40px;
          height: 40px;
          border-radius: 10px;
          background: linear-gradient(135deg, #1677FF 0%, #69B1FF 100%);
          display: flex;
          align-items: center;
          justify-content: center;
          color: #FFFFFF;
          font-weight: 700;
          font-size: 16px;
          flex-shrink: 0;
          box-shadow: 0 2px 6px rgba(22, 119, 255, 0.2);
        }

        .course-main-meta {
          flex: 1;
          min-width: 0;
          display: flex;
          flex-direction: column;
          gap: 3px;

          .title-code-row {
            display: flex;
            align-items: center;
            gap: 8px;

            .course-name {
              font-size: 13.5px;
              font-weight: 600;
              color: #0F172A;
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
            }

            .course-code-tag {
              font-size: 11px;
              font-weight: 600;
              color: #1677FF;
              background: #EAF3FF;
              padding: 1px 6px;
              border-radius: 4px;
            }
          }

          .extra-meta-row {
            font-size: 12px;
            color: #64748B;
            display: flex;
            align-items: center;
            gap: 6px;

            .dot-sep {
              color: #CBD5E1;
            }
          }
        }

        .quick-enroll-btn {
          height: 32px;
          padding: 0 16px;
          border-radius: 9999px;
          border: none;
          background: #1677FF;
          color: #FFFFFF;
          font-size: 12.5px;
          font-weight: 600;
          cursor: pointer;
          transition: all 0.2s ease;
          flex-shrink: 0;

          &:hover:not(:disabled) {
            background: #4096FF;
            transform: translateY(-1px);
            box-shadow: 0 4px 10px rgba(22, 119, 255, 0.25);
          }
        }
      }
    }
  }
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>

<!-- 全局覆盖 Element Plus 对话框外壳样式（确保圆角、投影、头部完全应用） -->
<style lang="scss">
.el-dialog.capsule-course-dialog {
  border-radius: 24px !important;
  overflow: hidden !important;
  box-shadow: 0 24px 60px -12px rgba(15, 23, 42, 0.18), 0 8px 24px -4px rgba(15, 23, 42, 0.08) !important;
  border: 1px solid rgba(255, 255, 255, 0.9) !important;
  background: #FFFFFF !important;

  .el-dialog__header {
    margin: 0 !important;
    padding: 24px 28px 12px !important;
    border-bottom: 1px solid #F1F5F9 !important;
    position: relative;

    .dialog-custom-header {
      display: flex;
      align-items: center;
      gap: 14px;

      .header-icon-badge {
        width: 44px;
        height: 44px;
        border-radius: 12px;
        background: linear-gradient(135deg, #EAF3FF 0%, #D0E2FF 100%);
        border: 1px solid #BAE0FF;
        color: #1677FF;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;

        .header-svg {
          width: 22px;
          height: 22px;
        }
      }

      .header-text-group {
        display: flex;
        flex-direction: column;
        gap: 3px;

        .header-main-title {
          margin: 0;
          font-size: 18px;
          font-weight: 700;
          color: #0F172A;
          letter-spacing: -0.2px;
        }

        .header-sub-desc {
          margin: 0;
          font-size: 12.5px;
          color: #64748B;
        }
      }
    }

    .el-dialog__headerbtn {
      top: 24px !important;
      right: 24px !important;
      width: 32px;
      height: 32px;
      border-radius: 50%;
      transition: all 0.2s;

      &:hover {
        background: #F1F5F9;
      }

      .el-dialog__close {
        color: #64748B;
        font-size: 18px;
      }
    }
  }

  .el-dialog__body {
    padding: 20px 28px 24px !important;
  }
}
</style>
