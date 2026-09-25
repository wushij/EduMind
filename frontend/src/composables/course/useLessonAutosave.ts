import { ref, watch, onUnmounted, type Ref } from 'vue';
import { onBeforeRouteLeave } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import { useAuthStore } from '@/stores/auth/auth';

export function useLessonAutosave(options: {
  courseId: number;
  lessonId: number;
  isDirty: Ref<boolean>;
  save: (showToast?: boolean) => Promise<boolean>;
}) {
  const saveStatusText = ref('就绪');
  const authStore = useAuthStore();
  let autoTimer: ReturnType<typeof setInterval> | null = null;

  const backupKey = `edumind_lesson_draft_${authStore.currentUser?.id ?? 'guest'}_${options.courseId}_${options.lessonId}`;

  function writeLocalBackup(payload: unknown) {
    try {
      localStorage.setItem(backupKey, JSON.stringify({ savedAt: Date.now(), payload }));
    } catch {
      /* ignore */
    }
  }

  function readLocalBackup(): { savedAt: number; payload: unknown } | null {
    try {
      const raw = localStorage.getItem(backupKey);
      if (!raw) return null;
      return JSON.parse(raw);
    } catch {
      return null;
    }
  }

  function clearLocalBackup() {
    localStorage.removeItem(backupKey);
  }

  async function runSave(showToast = false) {
    saveStatusText.value = '保存中...';
    const ok = await options.save(showToast);
    if (ok) {
      const now = new Date();
      saveStatusText.value = `已保存于 ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
      options.isDirty.value = false;
      clearLocalBackup();
    } else {
      saveStatusText.value = '保存失败';
    }
    return ok;
  }

  watch(
    options.isDirty,
    dirty => {
      if (dirty) {
        saveStatusText.value = '有未保存更改';
      }
    }
  );

  autoTimer = setInterval(() => {
    if (options.isDirty.value) {
      void runSave(false);
    }
  }, 30000);

  /** 确认框是否已打开：导航在弹窗未关闭期间可能被重复触发，需拦回以免叠加多个确认框 */
  let leaving = false;

  onBeforeRouteLeave((_to, _from, next) => {
    if (!options.isDirty.value) {
      next();
      return;
    }
    if (leaving) {
      next(false);
      return;
    }
    leaving = true;
    ElMessageBox.confirm('课节内容有未保存的修改，确定离开吗？', '提示', {
      type: 'warning',
      confirmButtonText: '离开',
      cancelButtonText: '继续编辑'
    })
      .then(() => next())
      .catch(() => next(false))
      .finally(() => {
        leaving = false;
      });
  });

  onUnmounted(() => {
    if (autoTimer) clearInterval(autoTimer);
  });

  return {
    saveStatusText,
    runSave,
    writeLocalBackup,
    readLocalBackup,
    clearLocalBackup
  };
}
