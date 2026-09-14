import { App } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import 'element-plus/theme-chalk/dark/css-vars.css';
import { router } from '@/router';
import { setupDirectives } from '@/directives';
import { setupAppErrorHandlers, logAppError } from '@/core/http/error-handler';
import { useAuthStore } from '@/stores/auth/auth';
import { usePreferenceStore } from '@/stores/user/preference';
import { tokenUtil } from '@/core/auth/token';
import '@/styles/index.scss';
import 'katex/dist/katex.min.css';

export async function bootstrap(app: App) {
  const pinia = createPinia();
  app.use(pinia);
  app.use(router);
  app.use(ElementPlus);
  setupDirectives(app);
  setupAppErrorHandlers(app);

  const authStore = useAuthStore();
  const onAuthEntry =
    window.location.pathname.startsWith('/auth/login')
    || window.location.pathname.startsWith('/auth/register')
    || window.location.pathname.startsWith('/auth/forgot');
  if (tokenUtil.get() && !onAuthEntry) {
    try {
      await authStore.fetchUserInfo();
    } catch (err) {
      logAppError('AuthBootstrap', err);
      authStore.logout();
    }
  } else if (tokenUtil.get() && onAuthEntry) {
    // 登录页残留过期 Token：仅清本地态，避免无意义拉用户信息 + 401 弹窗
    authStore.logout();
  }

  // 初始化用户偏好（未登录只读本地缓存；已登录再同步云端）
  const preferenceStore = usePreferenceStore();
  await preferenceStore.loadPreferences();
}
